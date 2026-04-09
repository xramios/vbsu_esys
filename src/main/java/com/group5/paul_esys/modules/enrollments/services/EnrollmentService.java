package com.group5.paul_esys.modules.enrollments.services;

import com.group5.paul_esys.modules.enums.EnrollmentStatus;
import com.group5.paul_esys.modules.enrollments.model.Enrollment;
import com.group5.paul_esys.modules.enrollments.utils.EnrollmentUtils;
import com.group5.paul_esys.modules.users.services.ConnectionService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO enrollments (student_id, enrollment_period_id, status, max_units, total_units, submitted_at) VALUES (?, ?, ?, ?, ?, ?)"
        )) {
      ps.setString(1, enrollment.getStudentId());
      ps.setLong(2, enrollment.getEnrollmentPeriodId());
      ps.setString(3, enrollment.getStatus().name());
      ps.setFloat(4, enrollment.getMaxUnits());
      ps.setFloat(5, enrollment.getTotalUnits());
      ps.setTimestamp(6, new java.sql.Timestamp(enrollment.getSubmittedAt().getTime()));

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
      ps.setString(1, enrollment.getStudentId());
      ps.setLong(2, enrollment.getEnrollmentPeriodId());
      ps.setString(3, enrollment.getStatus().name());
      ps.setFloat(4, enrollment.getMaxUnits());
      ps.setFloat(5, enrollment.getTotalUnits());
      ps.setTimestamp(6, new java.sql.Timestamp(enrollment.getSubmittedAt().getTime()));
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
}
