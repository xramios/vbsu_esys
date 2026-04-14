package com.group5.paul_esys.modules.registrar.services;

import com.group5.paul_esys.modules.enums.DropRequestStatus;
import com.group5.paul_esys.modules.enums.EnrollmentDetailStatus;
import com.group5.paul_esys.modules.enums.StudentEnrolledSubjectStatus;
import com.group5.paul_esys.modules.enrollments.services.StudentSemesterProgressService;
import com.group5.paul_esys.modules.registrar.model.DropActionResult;
import com.group5.paul_esys.modules.registrar.model.FacultyStudentDropRequest;
import com.group5.paul_esys.modules.registrar.model.StudentDropCandidate;
import com.group5.paul_esys.modules.registrar.model.StudentDropTargetOption;
import com.group5.paul_esys.modules.registrar.utils.RegistrarDropRequestUtils;
import com.group5.paul_esys.modules.users.services.ConnectionService;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegistrarDropRequestService {

  private static final RegistrarDropRequestService INSTANCE = new RegistrarDropRequestService();
  private static final Logger logger = LoggerFactory.getLogger(RegistrarDropRequestService.class);

  private RegistrarDropRequestService() {
  }

  public static RegistrarDropRequestService getInstance() {
    return INSTANCE;
  }

  public List<FacultyStudentDropRequest> getDropRequests(DropRequestStatus status) {
    List<FacultyStudentDropRequest> requests = new ArrayList<>();

    StringBuilder sql = new StringBuilder(
        "SELECT "
            + "dr.id, dr.faculty_id, dr.student_id, dr.offering_id, dr.reason, dr.status, dr.created_at, dr.updated_at, "
            + "fac.first_name AS faculty_first_name, fac.last_name AS faculty_last_name, "
            + "stu.first_name AS student_first_name, stu.last_name AS student_last_name, "
            + "sub.subject_code, sub.subject_name, sec.section_code, ep.school_year, ep.semester, "
            + "e.id AS enrollment_id, ed.status AS enrollment_detail_status "
            + "FROM faculty_student_drop_requests dr "
            + "LEFT JOIN faculty fac ON fac.id = dr.faculty_id "
            + "LEFT JOIN students stu ON stu.student_id = dr.student_id "
            + "LEFT JOIN offerings o ON o.id = dr.offering_id "
            + "LEFT JOIN subjects sub ON sub.id = o.subject_id "
            + "LEFT JOIN sections sec ON sec.id = o.section_id "
            + "LEFT JOIN enrollment_period ep ON ep.id = o.enrollment_period_id "
            + "LEFT JOIN enrollments e ON e.student_id = dr.student_id AND e.enrollment_period_id = o.enrollment_period_id "
            + "LEFT JOIN enrollments_details ed ON ed.enrollment_id = e.id AND ed.offering_id = dr.offering_id "
    );

    if (status != null) {
      sql.append("WHERE dr.status = ? ");
    }

    sql.append(
        "ORDER BY CASE dr.status "
            + "WHEN 'PENDING' THEN 0 "
            + "WHEN 'APPROVED' THEN 1 "
            + "ELSE 2 END, "
            + "dr.created_at DESC"
    );

    try (
        Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql.toString())
    ) {
      if (status != null) {
        ps.setString(1, status.name());
      }

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          requests.add(RegistrarDropRequestUtils.mapResultSetToDropRequest(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: {}", e.getMessage(), e);
    }

    return requests;
  }

  public boolean approveDropRequest(Long requestId) {
    if (requestId == null) {
      return false;
    }

    try (Connection conn = ConnectionService.getConnection()) {
      conn.setAutoCommit(false);
      DropActionResult dropActionResult;

      try {
        FacultyStudentDropRequest request = getDropRequestById(conn, requestId);
        if (request == null || request.getStatus() != DropRequestStatus.PENDING) {
          conn.rollback();
          return false;
        }

        dropActionResult = dropStudentFromOfferingInternal(conn, request.getStudentId(), request.getOfferingId());
        if (!dropActionResult.isSuccess()) {
          conn.rollback();
          return false;
        }

        if (!updateDropRequestStatus(conn, requestId, DropRequestStatus.APPROVED)) {
          conn.rollback();
          return false;
        }

        conn.commit();
      } catch (SQLException e) {
        conn.rollback();
        logger.error("ERROR: {}", e.getMessage(), e);
        return false;
      } finally {
        conn.setAutoCommit(true);
      }

      if (dropActionResult.getEnrollmentId() != null) {
        StudentSemesterProgressService.getInstance().syncByEnrollmentId(dropActionResult.getEnrollmentId());
      }

      return true;
    } catch (SQLException e) {
      logger.error("ERROR: {}", e.getMessage(), e);
      return false;
    }
  }

  public boolean rejectDropRequest(Long requestId) {
    if (requestId == null) {
      return false;
    }

    String sql = "UPDATE faculty_student_drop_requests SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND status = ?";

    try (
        Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)
    ) {
      ps.setString(1, DropRequestStatus.REJECTED.name());
      ps.setLong(2, requestId);
      ps.setString(3, DropRequestStatus.PENDING.name());
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: {}", e.getMessage(), e);
      return false;
    }
  }

  public boolean dropStudentFromOffering(String studentId, Long offeringId, String reason) {
    if (studentId == null || studentId.isBlank() || offeringId == null) {
      return false;
    }

    try (Connection conn = ConnectionService.getConnection()) {
      conn.setAutoCommit(false);
      DropActionResult dropActionResult;

      try {
        dropActionResult = dropStudentFromOfferingInternal(conn, studentId, offeringId);
        if (!dropActionResult.isSuccess()) {
          conn.rollback();
          return false;
        }

        conn.commit();
      } catch (SQLException e) {
        conn.rollback();
        logger.error("ERROR: {}", e.getMessage(), e);
        return false;
      } finally {
        conn.setAutoCommit(true);
      }

      if (dropActionResult.getEnrollmentId() != null) {
        StudentSemesterProgressService.getInstance().syncByEnrollmentId(dropActionResult.getEnrollmentId());
      }

      logger.info(
          "Manual drop applied for studentId={}, offeringId={}, reason={}",
          studentId,
          offeringId,
          reason == null ? "" : reason.trim()
      );
      return true;
    } catch (SQLException e) {
      logger.error("ERROR: {}", e.getMessage(), e);
      return false;
    }
  }

  public List<StudentDropCandidate> getStudentsWithDroppableOfferings() {
    List<StudentDropCandidate> candidates = new ArrayList<>();
    String sql =
        "SELECT DISTINCT s.student_id, s.first_name, s.last_name "
            + "FROM enrollments_details ed "
            + "INNER JOIN enrollments e ON e.id = ed.enrollment_id "
            + "INNER JOIN students s ON s.student_id = e.student_id "
            + "WHERE ed.status = ? "
            + "AND e.status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'ENROLLED') "
            + "ORDER BY s.last_name, s.first_name";

    try (
        Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)
    ) {
      ps.setString(1, EnrollmentDetailStatus.SELECTED.name());
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String studentId = rs.getString("student_id");
          String firstName = safeText(rs.getString("first_name"), "");
          String lastName = safeText(rs.getString("last_name"), "");
          String studentName = (lastName + ", " + firstName).trim();

          candidates.add(new StudentDropCandidate()
              .setStudentId(studentId)
              .setStudentName(studentName)
          );
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: {}", e.getMessage(), e);
    }

    return candidates;
  }

  public List<StudentDropTargetOption> getDroppableOfferingsByStudent(String studentId) {
    if (studentId == null || studentId.isBlank()) {
      return List.of();
    }

    List<StudentDropTargetOption> options = new ArrayList<>();
    String sql =
        "SELECT e.id AS enrollment_id, e.status AS enrollment_status, "
            + "o.id AS offering_id, o.semester_subject_id, ed.units, "
            + "sub.subject_code, sub.subject_name, sec.section_code, "
            + "ep.school_year, ep.semester "
            + "FROM enrollments_details ed "
            + "INNER JOIN enrollments e ON e.id = ed.enrollment_id "
            + "INNER JOIN offerings o ON o.id = ed.offering_id "
            + "INNER JOIN subjects sub ON sub.id = o.subject_id "
            + "INNER JOIN sections sec ON sec.id = o.section_id "
            + "INNER JOIN enrollment_period ep ON ep.id = o.enrollment_period_id "
            + "WHERE e.student_id = ? "
            + "AND ed.status = ? "
            + "AND e.status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'ENROLLED') "
            + "ORDER BY ep.created_at DESC, sub.subject_code, sec.section_code";

    try (
        Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)
    ) {
      ps.setString(1, studentId);
      ps.setString(2, EnrollmentDetailStatus.SELECTED.name());

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String schoolYear = safeText(rs.getString("school_year"), "N/A");
          String semester = safeText(rs.getString("semester"), "N/A");
          String enrollmentPeriodLabel = schoolYear + " | " + semester;

          options.add(new StudentDropTargetOption()
              .setEnrollmentId(rs.getLong("enrollment_id"))
              .setStudentId(studentId)
              .setOfferingId(rs.getLong("offering_id"))
              .setSemesterSubjectId(rs.getObject("semester_subject_id", Long.class))
              .setUnits(rs.getObject("units", Float.class))
              .setEnrollmentStatus(rs.getString("enrollment_status"))
              .setSubjectCode(safeText(rs.getString("subject_code"), "N/A"))
              .setSubjectName(safeText(rs.getString("subject_name"), "N/A"))
              .setSectionCode(safeText(rs.getString("section_code"), "N/A"))
              .setEnrollmentPeriodLabel(enrollmentPeriodLabel)
          );
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: {}", e.getMessage(), e);
    }

    return options;
  }

  private FacultyStudentDropRequest getDropRequestById(Connection conn, Long requestId) throws SQLException {
    String sql = "SELECT id, faculty_id, student_id, offering_id, reason, status, created_at, updated_at FROM faculty_student_drop_requests WHERE id = ?";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, requestId);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return null;
        }

        return new FacultyStudentDropRequest()
            .setId(rs.getLong("id"))
            .setFacultyId(rs.getObject("faculty_id", Long.class))
            .setStudentId(rs.getString("student_id"))
            .setOfferingId(rs.getObject("offering_id", Long.class))
            .setReason(rs.getString("reason"))
            .setStatus(DropRequestStatus.fromValue(rs.getString("status")))
            .setCreatedAt(rs.getTimestamp("created_at"))
            .setUpdatedAt(rs.getTimestamp("updated_at"));
      }
    }
  }

  private boolean updateDropRequestStatus(Connection conn, Long requestId, DropRequestStatus status) throws SQLException {
    String sql = "UPDATE faculty_student_drop_requests SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, status.name());
      ps.setLong(2, requestId);
      return ps.executeUpdate() > 0;
    }
  }

  private DropActionResult dropStudentFromOfferingInternal(Connection conn, String studentId, Long offeringId)
      throws SQLException {
    StudentDropTargetOption selectedTarget = getSelectedDropTarget(conn, studentId, offeringId);

    if (selectedTarget == null) {
      Long existingEnrollmentId = getExistingDroppedEnrollmentId(conn, studentId, offeringId);
      if (existingEnrollmentId != null) {
        return new DropActionResult(true, existingEnrollmentId);
      }

      return new DropActionResult(false, null);
    }

    String updateDetailSql =
        "UPDATE enrollments_details "
            + "SET status = ?, updated_at = CURRENT_TIMESTAMP "
            + "WHERE enrollment_id = ? AND offering_id = ? AND status = ?";

    try (PreparedStatement ps = conn.prepareStatement(updateDetailSql)) {
      ps.setString(1, EnrollmentDetailStatus.DROPPED.name());
      ps.setLong(2, selectedTarget.getEnrollmentId());
      ps.setLong(3, selectedTarget.getOfferingId());
      ps.setString(4, EnrollmentDetailStatus.SELECTED.name());

      int updatedRows = ps.executeUpdate();
      if (updatedRows <= 0) {
        return new DropActionResult(false, null);
      }
    }

    upsertStudentEnrolledSubjectDropped(conn, selectedTarget);
    updateEnrollmentTotalUnits(conn, selectedTarget.getEnrollmentId());

    return new DropActionResult(true, selectedTarget.getEnrollmentId());
  }

  private StudentDropTargetOption getSelectedDropTarget(Connection conn, String studentId, Long offeringId)
      throws SQLException {
    String sql =
        "SELECT ed.enrollment_id, ed.offering_id, o.semester_subject_id, ed.units "
            + "FROM enrollments_details ed "
            + "INNER JOIN enrollments e ON e.id = ed.enrollment_id "
            + "INNER JOIN offerings o ON o.id = ed.offering_id "
            + "WHERE e.student_id = ? "
            + "AND ed.offering_id = ? "
            + "AND ed.status = ? "
            + "ORDER BY e.created_at DESC "
            + "FETCH FIRST 1 ROWS ONLY";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, studentId);
      ps.setLong(2, offeringId);
      ps.setString(3, EnrollmentDetailStatus.SELECTED.name());

      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return null;
        }

        return new StudentDropTargetOption()
            .setStudentId(studentId)
            .setEnrollmentId(rs.getLong("enrollment_id"))
            .setOfferingId(rs.getLong("offering_id"))
            .setSemesterSubjectId(rs.getObject("semester_subject_id", Long.class))
            .setUnits(rs.getObject("units", Float.class));
      }
    }
  }

  private Long getExistingDroppedEnrollmentId(Connection conn, String studentId, Long offeringId) throws SQLException {
    String sql =
        "SELECT ed.enrollment_id "
            + "FROM enrollments_details ed "
            + "INNER JOIN enrollments e ON e.id = ed.enrollment_id "
            + "WHERE e.student_id = ? "
            + "AND ed.offering_id = ? "
            + "AND ed.status = ? "
            + "ORDER BY e.created_at DESC "
            + "FETCH FIRST 1 ROWS ONLY";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, studentId);
      ps.setLong(2, offeringId);
      ps.setString(3, EnrollmentDetailStatus.DROPPED.name());

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getLong("enrollment_id");
        }
      }
    }

    return null;
  }

  private void upsertStudentEnrolledSubjectDropped(Connection conn, StudentDropTargetOption target) throws SQLException {
    boolean hasSelectedColumn = hasSelectedColumn(conn);
    String updateSql = hasSelectedColumn
        ? "UPDATE student_enrolled_subjects SET status = ?, is_selected = ?, updated_at = CURRENT_TIMESTAMP WHERE student_id = ? AND offering_id = ?"
        : "UPDATE student_enrolled_subjects SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE student_id = ? AND offering_id = ?";

    try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
      int index = 1;
      updatePs.setString(index++, StudentEnrolledSubjectStatus.DROPPED.name());
      if (hasSelectedColumn) {
        updatePs.setBoolean(index++, false);
      }
      updatePs.setString(index++, target.getStudentId());
      updatePs.setLong(index, target.getOfferingId());

      int updatedRows = updatePs.executeUpdate();
      if (updatedRows > 0 || target.getSemesterSubjectId() == null) {
        return;
      }
    }

    String insertSql = hasSelectedColumn
        ? "INSERT INTO student_enrolled_subjects (student_id, enrollment_id, offering_id, semester_subject_id, status, is_selected) VALUES (?, ?, ?, ?, ?, ?)"
        : "INSERT INTO student_enrolled_subjects (student_id, enrollment_id, offering_id, semester_subject_id, status) VALUES (?, ?, ?, ?, ?)";

    try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
      insertPs.setString(1, target.getStudentId());
      insertPs.setLong(2, target.getEnrollmentId());
      insertPs.setLong(3, target.getOfferingId());
      insertPs.setLong(4, target.getSemesterSubjectId());
      insertPs.setString(5, StudentEnrolledSubjectStatus.DROPPED.name());

      if (hasSelectedColumn) {
        insertPs.setBoolean(6, false);
      }

      insertPs.executeUpdate();
    }
  }

  private void updateEnrollmentTotalUnits(Connection conn, Long enrollmentId) throws SQLException {
    Float totalUnits = 0f;

    String sumSql = "SELECT COALESCE(SUM(units), 0) AS total_units FROM enrollments_details WHERE enrollment_id = ? AND status = ?";
    try (PreparedStatement sumPs = conn.prepareStatement(sumSql)) {
      sumPs.setLong(1, enrollmentId);
      sumPs.setString(2, EnrollmentDetailStatus.SELECTED.name());

      try (ResultSet rs = sumPs.executeQuery()) {
        if (rs.next()) {
          totalUnits = rs.getFloat("total_units");
        }
      }
    }

    String updateEnrollmentSql = "UPDATE enrollments SET total_units = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
    try (PreparedStatement updatePs = conn.prepareStatement(updateEnrollmentSql)) {
      updatePs.setFloat(1, totalUnits == null ? 0f : totalUnits);
      updatePs.setLong(2, enrollmentId);
      updatePs.executeUpdate();
    }
  }

  private boolean hasSelectedColumn(Connection conn) {
    try {
      DatabaseMetaData metadata = conn.getMetaData();
      try (ResultSet rs = metadata.getColumns(null, null, "STUDENT_ENROLLED_SUBJECTS", "IS_SELECTED")) {
        if (rs.next()) {
          return true;
        }
      }

      try (ResultSet rs = metadata.getColumns(null, null, "student_enrolled_subjects", "is_selected")) {
        return rs.next();
      }
    } catch (SQLException e) {
      logger.error("ERROR: {}", e.getMessage(), e);
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