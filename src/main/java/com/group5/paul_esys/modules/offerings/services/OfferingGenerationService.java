package com.group5.paul_esys.modules.offerings.services;

import com.group5.paul_esys.modules.offerings.model.OfferingGenerationPlanRow;
import com.group5.paul_esys.modules.offerings.model.OfferingGenerationResult;
import com.group5.paul_esys.modules.users.services.ConnectionService;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OfferingGenerationService {

  private static final OfferingGenerationService INSTANCE = new OfferingGenerationService();
  private static final Logger logger = LoggerFactory.getLogger(OfferingGenerationService.class);

  private OfferingGenerationService() {
  }

  public static OfferingGenerationService getInstance() {
    return INSTANCE;
  }

  public List<String> getDistinctSemesterNames() {
    List<String> semesterNames = new ArrayList<>();

    String sql = "SELECT DISTINCT semester FROM semester WHERE semester IS NOT NULL AND TRIM(semester) <> '' ORDER BY semester";

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql);
      ResultSet rs = ps.executeQuery()
    ) {
      while (rs.next()) {
        semesterNames.add(safeText(rs.getString("semester"), ""));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }

    return semesterNames;
  }

  public List<OfferingGenerationPlanRow> previewGenerationPlan(
      Long enrollmentPeriodId,
      Long curriculumId,
      Integer yearLevel,
      String semesterName,
      boolean onlyActiveSections,
      boolean includeWaitlistSections
  ) {
    List<OfferingGenerationPlanRow> planRows = new ArrayList<>();
    if (enrollmentPeriodId == null
        || curriculumId == null
        || yearLevel == null
        || semesterName == null
        || semesterName.trim().isEmpty()) {
      return planRows;
    }

    try (Connection conn = ConnectionService.getConnection()) {
      boolean hasSectionStatus = hasSectionStatusColumn(conn);
      String sql = buildPreviewSql(hasSectionStatus, onlyActiveSections, includeWaitlistSections);

      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setLong(1, enrollmentPeriodId);
        ps.setLong(2, curriculumId);
        ps.setInt(3, yearLevel);
        ps.setString(4, semesterName.trim());

        try (ResultSet rs = ps.executeQuery()) {
          while (rs.next()) {
            planRows.add(
                new OfferingGenerationPlanRow(
                    rs.getLong("subject_id"),
                    safeText(rs.getString("subject_code"), "N/A"),
                    safeText(rs.getString("subject_name"), "N/A"),
                    rs.getLong("section_id"),
                    safeText(rs.getString("section_code"), "N/A"),
                    rs.getObject("section_capacity", Integer.class),
                    rs.getObject("semester_subject_id", Long.class),
                    rs.getInt("already_exists") > 0
                )
            );
          }
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }

    return planRows;
  }

  public OfferingGenerationResult generateOfferings(
      Long enrollmentPeriodId,
      Long curriculumId,
      Integer yearLevel,
      String semesterName,
      boolean onlyActiveSections,
      boolean includeWaitlistSections
  ) {
    List<OfferingGenerationPlanRow> planRows = previewGenerationPlan(
        enrollmentPeriodId,
        curriculumId,
        yearLevel,
        semesterName,
        onlyActiveSections,
        includeWaitlistSections
    );

    if (planRows.isEmpty()) {
      return new OfferingGenerationResult(
          true,
          0,
          0,
          0,
          0,
          "No matching subject-section combinations were found for the selected filters."
      );
    }

    int existingCount = 0;
    for (OfferingGenerationPlanRow row : planRows) {
      if (row.alreadyExists()) {
        existingCount++;
      }
    }

    String insertSql = """
      INSERT INTO offerings (subject_id, section_id, enrollment_period_id, semester_subject_id, capacity)
      VALUES (?, ?, ?, ?, ?)
      """;

    int createdCount = 0;

    Connection conn = null;
    try {
      conn = ConnectionService.getConnection();
      conn.setAutoCommit(false);

      try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
        for (OfferingGenerationPlanRow row : planRows) {
          ps.setLong(1, row.subjectId());
          ps.setLong(2, row.sectionId());
          ps.setLong(3, enrollmentPeriodId);
          if (row.semesterSubjectId() == null) {
            ps.setNull(4, Types.BIGINT);
          } else {
            ps.setLong(4, row.semesterSubjectId());
          }

          if (row.sectionCapacity() == null) {
            ps.setNull(5, Types.INTEGER);
          } else {
            ps.setInt(5, row.sectionCapacity());
          }

          try {
            int affected = ps.executeUpdate();
            if (affected > 0) {
              createdCount++;
            }
          } catch (SQLIntegrityConstraintViolationException duplicateException) {
            logger.warn("Skipping duplicate offering for subject {} section {} period {}",
                row.subjectId(), row.sectionId(), enrollmentPeriodId);
          }
        }
      }

      conn.commit();
    } catch (SQLException e) {
      if (conn != null) {
        try {
          if (!conn.isClosed()) {
            conn.rollback();
          }
        } catch (SQLException rollbackException) {
          logger.error("ERROR: " + rollbackException.getMessage(), rollbackException);
        }
      }

      logger.error("ERROR: " + e.getMessage(), e);
      return new OfferingGenerationResult(
          false,
          planRows.size(),
          existingCount,
          createdCount,
          planRows.size() - createdCount,
          "Failed to generate offerings. Please check logs and try again."
      );
    } finally {
      if (conn != null) {
        try {
          conn.close();
        } catch (SQLException closeException) {
          logger.error("ERROR: " + closeException.getMessage(), closeException);
        }
      }
    }

    int skippedCount = planRows.size() - createdCount;
    return new OfferingGenerationResult(
        true,
        planRows.size(),
        existingCount,
        createdCount,
        skippedCount,
        "Offerings generation completed."
    );
  }

  private String buildPreviewSql(boolean hasSectionStatus, boolean onlyActiveSections, boolean includeWaitlistSections) {
    StringBuilder sql = new StringBuilder(
        """
        SELECT
          sub.id AS subject_id,
          sub.subject_code,
          sub.subject_name,
          sec.id AS section_id,
          sec.section_code,
          sec.capacity AS section_capacity,
          MIN(ss.id) AS semester_subject_id,
          MAX(CASE WHEN o.id IS NULL THEN 0 ELSE 1 END) AS already_exists
        FROM semester_subjects ss
        INNER JOIN semester sem ON sem.id = ss.semester_id
        INNER JOIN subjects sub ON sub.id = ss.subject_id
        CROSS JOIN sections sec
        LEFT JOIN offerings o
          ON o.subject_id = sub.id
         AND o.section_id = sec.id
         AND o.enrollment_period_id = ?
        WHERE sem.curriculum_id = ?
          AND sem.year_level = ?
          AND UPPER(TRIM(sem.semester)) = UPPER(TRIM(?))
        """
    );

    if (hasSectionStatus && onlyActiveSections) {
      if (includeWaitlistSections) {
        sql.append(" AND UPPER(TRIM(sec.status)) IN ('OPEN', 'WAITLIST') ");
      } else {
        sql.append(" AND UPPER(TRIM(sec.status)) = 'OPEN' ");
      }
    }

    sql.append(
        """
        GROUP BY
          sub.id,
          sub.subject_code,
          sub.subject_name,
          sec.id,
          sec.section_code,
          sec.capacity
        ORDER BY
          sub.subject_code,
          sec.section_code
        """
    );

    return sql.toString();
  }

  private boolean hasSectionStatusColumn(Connection conn) {
    try {
      DatabaseMetaData metadata = conn.getMetaData();

      try (ResultSet rs = metadata.getColumns(null, null, "SECTIONS", "STATUS")) {
        if (rs.next()) {
          return true;
        }
      }

      try (ResultSet rs = metadata.getColumns(null, null, "sections", "status")) {
        return rs.next();
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  private String safeText(String value, String fallback) {
    if (value == null) {
      return fallback;
    }

    String normalized = value.trim();
    return normalized.isEmpty() ? fallback : normalized;
  }
}
