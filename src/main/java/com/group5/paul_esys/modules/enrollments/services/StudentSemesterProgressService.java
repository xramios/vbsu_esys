package com.group5.paul_esys.modules.enrollments.services;

import com.group5.paul_esys.modules.enums.SemesterProgressStatus;
import com.group5.paul_esys.modules.enrollments.model.StudentSemesterProgress;
import com.group5.paul_esys.modules.enrollments.utils.StudentSemesterProgressUtils;
import com.group5.paul_esys.modules.users.services.ConnectionService;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StudentSemesterProgressService {

  private static final StudentSemesterProgressService INSTANCE = new StudentSemesterProgressService();
  private static final Logger logger = LoggerFactory.getLogger(StudentSemesterProgressService.class);

  private StudentSemesterProgressService() {
  }

  public static StudentSemesterProgressService getInstance() {
    return INSTANCE;
  }

  public List<StudentSemesterProgress> getProgressByStudent(String studentId) {
    List<StudentSemesterProgress> progress = new ArrayList<>();
    String sql = "SELECT * FROM student_semester_progress WHERE student_id = ? ORDER BY created_at";

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql)
    ) {
      ps.setString(1, studentId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          progress.add(StudentSemesterProgressUtils.mapResultSetToStudentSemesterProgress(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }

    return progress;
  }

  public boolean syncByEnrollmentId(Long enrollmentId) {
    Optional<String> studentId = getStudentIdByEnrollmentId(enrollmentId);
    if (studentId.isEmpty()) {
      return false;
    }
    return syncStudentProgress(studentId.get());
  }

  public boolean syncStudentProgress(String studentId) {
    if (studentId == null || studentId.isBlank()) {
      return false;
    }

    try (Connection conn = ConnectionService.getConnection()) {
      conn.setAutoCommit(false);
      try {
        Optional<Long> curriculumIdOpt = resolveCurriculumId(conn, studentId);
        if (curriculumIdOpt.isEmpty()) {
          conn.rollback();
          logger.warn("Cannot sync semester progress: no curriculum linked for student {}", studentId);
          return false;
        }

        Long curriculumId = curriculumIdOpt.get();
        List<Long> semesterIds = getSemesterIdsByCurriculum(conn, curriculumId);

        for (Long semesterId : semesterIds) {
          SemesterProgressStatus nextStatus = computeStatus(conn, studentId, semesterId);
          upsertProgress(conn, studentId, curriculumId, semesterId, nextStatus);
        }

        StudentAcademicPromotionService.getInstance().promoteIfYearCompleted(conn, studentId);

        conn.commit();
        return true;
      } catch (SQLException e) {
        conn.rollback();
        logger.error("ERROR: " + e.getMessage(), e);
        return false;
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean initializeInitialSemesterProgress(Connection conn, String studentId, Long curriculumId) throws SQLException {
    if (conn == null || studentId == null || studentId.isBlank() || curriculumId == null) {
      return false;
    }

    Optional<Long> firstSemesterId = resolveFirstSemesterId(conn, curriculumId);
    if (firstSemesterId.isEmpty()) {
      logger.warn(
          "Cannot initialize semester progress: first semester not found for curriculum {}",
          curriculumId
      );
      return false;
    }

    if (progressExists(conn, studentId, firstSemesterId.get())) {
      return true;
    }

    insertProgress(conn, studentId, curriculumId, firstSemesterId.get(), SemesterProgressStatus.NOT_STARTED);
    return true;
  }

  private Optional<String> getStudentIdByEnrollmentId(Long enrollmentId) {
    String sql = "SELECT student_id FROM enrollments WHERE id = ?";

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql)
    ) {
      ps.setLong(1, enrollmentId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.ofNullable(rs.getString("student_id"));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }

    return Optional.empty();
  }

  private Optional<Long> resolveCurriculumId(Connection conn, String studentId) throws SQLException {
    String studentSql = "SELECT curriculum_id, course_id FROM students WHERE student_id = ?";

    try (PreparedStatement ps = conn.prepareStatement(studentSql)) {
      ps.setString(1, studentId);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return Optional.empty();
        }

        long curriculumId = rs.getLong("curriculum_id");
        if (!rs.wasNull()) {
          return Optional.of(curriculumId);
        }

        long courseId = rs.getLong("course_id");
        if (rs.wasNull()) {
          return Optional.empty();
        }

        return getLatestCurriculumByCourse(conn, courseId);
      }
    }
  }

  private Optional<Long> resolveFirstSemesterId(Connection conn, Long curriculumId) throws SQLException {
    boolean hasYearLevel = hasYearLevelColumn(conn);
    String semesterRank =
        "CASE "
            + "WHEN UPPER(TRIM(semester)) LIKE '%1ST%' OR UPPER(TRIM(semester)) LIKE '%FIRST%' OR UPPER(TRIM(semester)) = 'SEMESTER 1' THEN 1 "
            + "WHEN UPPER(TRIM(semester)) LIKE '%2ND%' OR UPPER(TRIM(semester)) LIKE '%SECOND%' OR UPPER(TRIM(semester)) = 'SEMESTER 2' THEN 2 "
            + "WHEN UPPER(TRIM(semester)) LIKE '%3RD%' OR UPPER(TRIM(semester)) LIKE '%THIRD%' OR UPPER(TRIM(semester)) = 'SEMESTER 3' THEN 3 "
            + "WHEN UPPER(TRIM(semester)) LIKE '%SUMMER%' THEN 9 "
            + "ELSE 99 END";

    String sql = hasYearLevel
        ? "SELECT id FROM semester WHERE curriculum_id = ? ORDER BY year_level ASC, " + semesterRank + ", created_at ASC, id ASC FETCH FIRST 1 ROWS ONLY"
        : "SELECT id FROM semester WHERE curriculum_id = ? ORDER BY " + semesterRank + ", created_at ASC, id ASC FETCH FIRST 1 ROWS ONLY";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, curriculumId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(rs.getLong("id"));
        }
      }
    }

    return Optional.empty();
  }

  private boolean hasYearLevelColumn(Connection conn) {
    try {
      DatabaseMetaData metadata = conn.getMetaData();
      try (ResultSet rs = metadata.getColumns(null, null, "SEMESTER", "YEAR_LEVEL")) {
        if (rs.next()) {
          return true;
        }
      }

      try (ResultSet rs = metadata.getColumns(null, null, "semester", "year_level")) {
        return rs.next();
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  private boolean progressExists(Connection conn, String studentId, Long semesterId) throws SQLException {
    String sql = "SELECT 1 FROM student_semester_progress WHERE student_id = ? AND semester_id = ? FETCH FIRST 1 ROWS ONLY";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, studentId);
      ps.setLong(2, semesterId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    }
  }

  private Optional<Long> getLatestCurriculumByCourse(Connection conn, Long courseId) throws SQLException {
    String sql = "SELECT id FROM curriculum WHERE course = ? ORDER BY cur_year DESC, created_at DESC FETCH FIRST 1 ROWS ONLY";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, courseId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(rs.getLong("id"));
        }
      }
    }

    return Optional.empty();
  }

  private List<Long> getSemesterIdsByCurriculum(Connection conn, Long curriculumId) throws SQLException {
    List<Long> semesterIds = new ArrayList<>();
    String sql = "SELECT id FROM semester WHERE curriculum_id = ? ORDER BY created_at";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, curriculumId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          semesterIds.add(rs.getLong("id"));
        }
      }
    }

    return semesterIds;
  }

  private SemesterProgressStatus computeStatus(Connection conn, String studentId, Long semesterId) throws SQLException {
    long requiredCount = countRequiredSubjects(conn, semesterId);
    long completedCount = countCompletedSubjects(conn, studentId, semesterId);

    if (requiredCount > 0 && completedCount >= requiredCount) {
      return SemesterProgressStatus.COMPLETED;
    }

    long studentSubjectActivity = countStudentSubjectActivity(conn, studentId, semesterId);
    long selectedEnrollmentActivity = countSelectedEnrollmentActivity(conn, studentId, semesterId);
    long activeEnrollmentRecords = countActiveEnrollmentRecords(conn, studentId, semesterId);

    if (studentSubjectActivity > 0 || selectedEnrollmentActivity > 0 || activeEnrollmentRecords > 0) {
      return SemesterProgressStatus.IN_PROGRESS;
    }

    return SemesterProgressStatus.NOT_STARTED;
  }

  private long countRequiredSubjects(Connection conn, Long semesterId) throws SQLException {
    String sql = "SELECT COUNT(*) AS total FROM semester_subjects WHERE semester_id = ?";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, semesterId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getLong("total");
        }
      }
    }

    return 0;
  }

  private long countCompletedSubjects(Connection conn, String studentId, Long semesterId) throws SQLException {
    String sql =
        "SELECT COUNT(DISTINCT ses.semester_subject_id) AS total "
            + "FROM student_enrolled_subjects ses "
            + "JOIN semester_subjects ss ON ss.id = ses.semester_subject_id "
            + "WHERE ses.student_id = ? AND ss.semester_id = ? AND ses.status = 'COMPLETED'";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, studentId);
      ps.setLong(2, semesterId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getLong("total");
        }
      }
    }

    return 0;
  }

  private long countStudentSubjectActivity(Connection conn, String studentId, Long semesterId) throws SQLException {
    String sql =
        "SELECT COUNT(*) AS total "
            + "FROM student_enrolled_subjects ses "
            + "JOIN semester_subjects ss ON ss.id = ses.semester_subject_id "
            + "WHERE ses.student_id = ? AND ss.semester_id = ?";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, studentId);
      ps.setLong(2, semesterId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getLong("total");
        }
      }
    }

    return 0;
  }

  private long countSelectedEnrollmentActivity(Connection conn, String studentId, Long semesterId) throws SQLException {
    String sql =
      "SELECT COUNT(DISTINCT o.semester_subject_id) AS total "
            + "FROM enrollments_details ed "
            + "JOIN enrollments e ON e.id = ed.enrollment_id "
        + "JOIN offerings o ON o.id = ed.offering_id "
        + "JOIN semester_subjects ss ON ss.id = o.semester_subject_id "
            + "WHERE e.student_id = ? AND ss.semester_id = ? AND ed.status = 'SELECTED'";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, studentId);
      ps.setLong(2, semesterId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getLong("total");
        }
      }
    }

    return 0;
  }

  private long countActiveEnrollmentRecords(Connection conn, String studentId, Long semesterId) throws SQLException {
    String sql =
        "SELECT COUNT(*) AS total "
            + "FROM enrollments e "
            + "JOIN enrollment_period ep ON ep.id = e.enrollment_period_id "
            + "JOIN semester s ON s.id = ? "
            + "JOIN students st ON st.student_id = e.student_id "
            + "WHERE e.student_id = ? "
            + "AND e.status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'ENROLLED') "
            + "AND UPPER(TRIM(ep.semester)) = UPPER(TRIM(s.semester)) "
            + "AND (st.year_level IS NULL OR st.year_level = s.year_level)";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, semesterId);
      ps.setString(2, studentId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getLong("total");
        }
      }
    }

    return 0;
  }

  private void upsertProgress(
      Connection conn,
      String studentId,
      Long curriculumId,
      Long semesterId,
      SemesterProgressStatus nextStatus
  ) throws SQLException {
    String selectSql = "SELECT id, started_at, completed_at FROM student_semester_progress WHERE student_id = ? AND semester_id = ?";

    try (PreparedStatement selectPs = conn.prepareStatement(selectSql)) {
      selectPs.setString(1, studentId);
      selectPs.setLong(2, semesterId);

      try (ResultSet rs = selectPs.executeQuery()) {
        if (rs.next()) {
          updateProgress(conn, rs.getLong("id"), rs.getTimestamp("started_at"), rs.getTimestamp("completed_at"), nextStatus);
        } else {
          insertProgress(conn, studentId, curriculumId, semesterId, nextStatus);
        }
      }
    }
  }

  private void updateProgress(
      Connection conn,
      Long id,
      Timestamp existingStartedAt,
      Timestamp existingCompletedAt,
      SemesterProgressStatus nextStatus
  ) throws SQLException {
    String sql = "UPDATE student_semester_progress SET status = ?, started_at = ?, completed_at = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
    Timestamp now = new Timestamp(System.currentTimeMillis());

    Timestamp startedAt = existingStartedAt;
    if (nextStatus == SemesterProgressStatus.NOT_STARTED) {
      startedAt = null;
    } else if (startedAt == null) {
      startedAt = now;
    }

    Timestamp completedAt = existingCompletedAt;
    if (nextStatus == SemesterProgressStatus.COMPLETED) {
      if (completedAt == null) {
        completedAt = now;
      }
    } else {
      completedAt = null;
    }

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, nextStatus.name());

      if (startedAt == null) {
        ps.setNull(2, Types.TIMESTAMP);
      } else {
        ps.setTimestamp(2, startedAt);
      }

      if (completedAt == null) {
        ps.setNull(3, Types.TIMESTAMP);
      } else {
        ps.setTimestamp(3, completedAt);
      }

      ps.setLong(4, id);
      ps.executeUpdate();
    }
  }

  private void insertProgress(
      Connection conn,
      String studentId,
      Long curriculumId,
      Long semesterId,
      SemesterProgressStatus nextStatus
  ) throws SQLException {
    String sql =
        "INSERT INTO student_semester_progress (student_id, curriculum_id, semester_id, status, started_at, completed_at) "
            + "VALUES (?, ?, ?, ?, ?, ?)";

    Timestamp now = new Timestamp(System.currentTimeMillis());
    Timestamp startedAt = nextStatus == SemesterProgressStatus.NOT_STARTED ? null : now;
    Timestamp completedAt = nextStatus == SemesterProgressStatus.COMPLETED ? now : null;

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, studentId);
      ps.setLong(2, curriculumId);
      ps.setLong(3, semesterId);
      ps.setString(4, nextStatus.name());

      if (startedAt == null) {
        ps.setNull(5, Types.TIMESTAMP);
      } else {
        ps.setTimestamp(5, startedAt);
      }

      if (completedAt == null) {
        ps.setNull(6, Types.TIMESTAMP);
      } else {
        ps.setTimestamp(6, completedAt);
      }

      ps.executeUpdate();
    }
  }
}