package com.group5.paul_esys.modules.enrollments.services;

import com.group5.paul_esys.modules.enums.EnrollmentStatus;
import com.group5.paul_esys.modules.enrollments.model.Enrollment;
import com.group5.paul_esys.modules.enrollments.utils.EnrollmentUtils;
import com.group5.paul_esys.modules.users.services.ConnectionService;

import java.sql.Connection;
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

public class EnrollmentService {

  private static final EnrollmentService INSTANCE = new EnrollmentService();
  private static final Logger logger = LoggerFactory.getLogger(EnrollmentService.class);

  private EnrollmentService() {
  }

  public static EnrollmentService getInstance() {
    return INSTANCE;
  }

  public List<Enrollment> getAllEnrollments() {
    List<Enrollment> enrollments = new ArrayList<>();
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM enrollments ORDER BY created_at DESC");
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        enrollments.add(EnrollmentUtils.mapResultSetToEnrollment(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return enrollments;
  }

  public Optional<Enrollment> getEnrollmentById(Long id) {
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM enrollments WHERE id = ?")) {
      ps.setLong(1, id);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(EnrollmentUtils.mapResultSetToEnrollment(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return Optional.empty();
  }

  public List<Enrollment> getEnrollmentsByStudent(String studentId) {
    List<Enrollment> enrollments = new ArrayList<>();
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM enrollments WHERE student_id = ? ORDER BY created_at DESC")) {
      ps.setString(1, studentId);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          enrollments.add(EnrollmentUtils.mapResultSetToEnrollment(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return enrollments;
  }

  public List<Enrollment> getEnrollmentsByStatus(EnrollmentStatus status) {
    List<Enrollment> enrollments = new ArrayList<>();
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM enrollments WHERE status = ? ORDER BY created_at DESC")) {
      ps.setString(1, status.name());

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          enrollments.add(EnrollmentUtils.mapResultSetToEnrollment(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return enrollments;
  }

  public boolean createEnrollment(Enrollment enrollment) {
    if (enrollment == null) {
      logger.warn("Unable to create enrollment: request is null");
      return false;
    }

    if (enrollment.getStudentId() == null || enrollment.getStudentId().isBlank()) {
      logger.warn("Unable to create enrollment: student ID is required");
      return false;
    }

    if (enrollment.getStatus() == null) {
      logger.warn("Unable to create enrollment: status is required");
      return false;
    }

    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO enrollments (student_id, enrollment_period_id, status, max_units, total_units, submitted_at) VALUES (?, ?, ?, ?, ?, ?)"
        )) {
      Long enrollmentPeriodId = resolveEnrollmentPeriodId(conn, enrollment.getEnrollmentPeriodId());
      if (enrollmentPeriodId == null) {
        logger.warn("Unable to create enrollment: no enrollment period available");
        return false;
      }

      ps.setString(1, enrollment.getStudentId());
      ps.setLong(2, enrollmentPeriodId);
      ps.setString(3, enrollment.getStatus().name());
      if (enrollment.getMaxUnits() == null) {
        ps.setNull(4, Types.FLOAT);
      } else {
        ps.setFloat(4, enrollment.getMaxUnits());
      }

      if (enrollment.getTotalUnits() == null) {
        ps.setNull(5, Types.FLOAT);
      } else {
        ps.setFloat(5, enrollment.getTotalUnits());
      }

      Timestamp submittedAt = enrollment.getSubmittedAt() == null
          ? new Timestamp(System.currentTimeMillis())
          : new Timestamp(enrollment.getSubmittedAt().getTime());
      ps.setTimestamp(6, submittedAt);

      boolean created = ps.executeUpdate() > 0;
      if (created) {
        StudentSemesterProgressService.getInstance().syncStudentProgress(enrollment.getStudentId());
      }
      return created;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean updateEnrollment(Enrollment enrollment) {
    try (Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(
        "UPDATE enrollments SET student_id = ?, enrollment_period_id = ?, status = ?, max_units = ?, total_units = ?, submitted_at = ? WHERE id = ?"
      )) {
      Long enrollmentPeriodId = resolveEnrollmentPeriodId(conn, enrollment.getEnrollmentPeriodId());
      if (enrollmentPeriodId == null) {
        logger.warn("Unable to update enrollment {}: no enrollment period available", enrollment.getId());
        return false;
      }

      ps.setString(1, enrollment.getStudentId());
      ps.setLong(2, enrollmentPeriodId);
      ps.setString(3, enrollment.getStatus().name());
      if (enrollment.getMaxUnits() == null) {
        ps.setNull(4, Types.FLOAT);
      } else {
        ps.setFloat(4, enrollment.getMaxUnits());
      }

      if (enrollment.getTotalUnits() == null) {
        ps.setNull(5, Types.FLOAT);
      } else {
        ps.setFloat(5, enrollment.getTotalUnits());
      }

      if (enrollment.getSubmittedAt() == null) {
        ps.setNull(6, Types.TIMESTAMP);
      } else {
        ps.setTimestamp(6, new Timestamp(enrollment.getSubmittedAt().getTime()));
      }
      ps.setLong(7, enrollment.getId());

      boolean updated = ps.executeUpdate() > 0;
      if (updated) {
        StudentSemesterProgressService.getInstance().syncStudentProgress(enrollment.getStudentId());
      }
      return updated;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean updateEnrollmentStatus(Long id, EnrollmentStatus status) {
    Optional<String> studentId = getStudentIdByEnrollmentId(id);

    try (Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(
        "UPDATE enrollments SET status = ? WHERE id = ?"
      )) {
      ps.setString(1, status.name());
      ps.setLong(2, id);

      boolean updated = ps.executeUpdate() > 0;
      if (updated && studentId.isPresent()) {
        StudentSemesterProgressService.getInstance().syncStudentProgress(studentId.get());
      }
      return updated;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean deleteEnrollment(Long id) {
    Optional<String> studentId = getStudentIdByEnrollmentId(id);

    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("DELETE FROM enrollments WHERE id = ?")) {
      ps.setLong(1, id);

      boolean deleted = ps.executeUpdate() > 0;
      if (deleted && studentId.isPresent()) {
        StudentSemesterProgressService.getInstance().syncStudentProgress(studentId.get());
      }
      return deleted;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public int backfillCompletedEnrollments() {
    try (Connection conn = ConnectionService.getConnection()) {
      return markCompletedEnrollments(conn, null);
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return 0;
    }
  }

  public int markCompletedEnrollmentsForStudent(Connection conn, String studentId) throws SQLException {
    if (conn == null || studentId == null || studentId.isBlank()) {
      return 0;
    }

    return markCompletedEnrollments(conn, studentId);
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

  private int markCompletedEnrollments(Connection conn, String studentId) throws SQLException {
    StringBuilder sql = new StringBuilder(
        "UPDATE enrollments "
            + "SET status = 'COMPLETED', updated_at = CURRENT_TIMESTAMP "
            + "WHERE status = 'ENROLLED' ");

    if (studentId != null) {
      sql.append("AND student_id = ? ");
    }

    sql.append(
        "AND EXISTS ("
            + "SELECT 1 "
            + "FROM enrollments_details ed "
            + "WHERE ed.enrollment_id = enrollments.id "
            + "AND ed.status = 'SELECTED'"
            + ") "
            + "AND NOT EXISTS ("
            + "SELECT 1 "
        + "FROM enrollments_details ed "
            + "WHERE ed.enrollment_id = enrollments.id "
            + "AND ed.status = 'SELECTED' "
        + "AND NOT EXISTS ("
        + "SELECT 1 "
        + "FROM student_enrolled_subjects ses "
        + "WHERE ses.enrollment_id = ed.enrollment_id "
        + "AND ses.offering_id = ed.offering_id "
        + "AND ses.status = 'COMPLETED'"
        + ")"
            + ")");

    try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
      if (studentId != null) {
        ps.setString(1, studentId);
      }

      return ps.executeUpdate();
    }
  }

  private Long resolveEnrollmentPeriodId(Connection conn, Long requestedEnrollmentPeriodId) throws SQLException {
    if (requestedEnrollmentPeriodId != null) {
      return requestedEnrollmentPeriodId;
    }

    Long currentPeriodId = getEnrollmentPeriodId(conn,
        "SELECT id FROM enrollment_period WHERE start_date <= CURRENT_TIMESTAMP AND end_date >= CURRENT_TIMESTAMP ORDER BY start_date DESC FETCH FIRST 1 ROWS ONLY");
    if (currentPeriodId != null) {
      return currentPeriodId;
    }

    return getEnrollmentPeriodId(conn,
        "SELECT id FROM enrollment_period ORDER BY created_at DESC FETCH FIRST 1 ROWS ONLY");
  }

  private Long getEnrollmentPeriodId(Connection conn, String sql) throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      if (rs.next()) {
        return rs.getLong("id");
      }
    }

    return null;
  }
}
