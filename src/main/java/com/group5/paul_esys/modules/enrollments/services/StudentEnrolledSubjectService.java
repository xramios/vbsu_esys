package com.group5.paul_esys.modules.enrollments.services;

import com.group5.paul_esys.modules.enums.StudentEnrolledSubjectStatus;
import com.group5.paul_esys.modules.enrollments.model.StudentEnrolledSubject;
import com.group5.paul_esys.modules.enrollments.utils.StudentEnrolledSubjectUtils;
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

public class StudentEnrolledSubjectService {

  private static final StudentEnrolledSubjectService INSTANCE = new StudentEnrolledSubjectService();
  private static final Logger logger = LoggerFactory.getLogger(StudentEnrolledSubjectService.class);

  private StudentEnrolledSubjectService() {
  }

  public static StudentEnrolledSubjectService getInstance() {
    return INSTANCE;
  }

  public List<StudentEnrolledSubject> getByStudent(String studentId) {
    List<StudentEnrolledSubject> subjects = new ArrayList<>();
    String sql = "SELECT * FROM student_enrolled_subjects WHERE student_id = ? ORDER BY created_at DESC";

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql)
    ) {
      ps.setString(1, studentId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          subjects.add(StudentEnrolledSubjectUtils.mapResultSetToStudentEnrolledSubject(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }

    return subjects;
  }

  public Optional<StudentEnrolledSubject> getByStudentAndSemesterSubject(String studentId, Long semesterSubjectId) {
    String sql = "SELECT * FROM student_enrolled_subjects WHERE student_id = ? AND semester_subject_id = ?";

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql)
    ) {
      ps.setString(1, studentId);
      ps.setLong(2, semesterSubjectId);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(StudentEnrolledSubjectUtils.mapResultSetToStudentEnrolledSubject(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }

    return Optional.empty();
  }

  public boolean upsertStatus(String studentId, Long semesterSubjectId, StudentEnrolledSubjectStatus status) {
    String selectSql = "SELECT 1 FROM student_enrolled_subjects WHERE student_id = ? AND semester_subject_id = ?";
    String insertSql = "INSERT INTO student_enrolled_subjects (student_id, semester_subject_id, status) VALUES (?, ?, ?)";
    String updateSql = "UPDATE student_enrolled_subjects SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE student_id = ? AND semester_subject_id = ?";

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement selectPs = conn.prepareStatement(selectSql)
    ) {
      selectPs.setString(1, studentId);
      selectPs.setLong(2, semesterSubjectId);

      boolean updated;
      try (ResultSet rs = selectPs.executeQuery()) {
        if (rs.next()) {
          try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
            updatePs.setString(1, status.name());
            updatePs.setString(2, studentId);
            updatePs.setLong(3, semesterSubjectId);
            updated = updatePs.executeUpdate() > 0;
          }
        } else {
          try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
            insertPs.setString(1, studentId);
            insertPs.setLong(2, semesterSubjectId);
            insertPs.setString(3, status.name());
            updated = insertPs.executeUpdate() > 0;
          }
        }
      }

      if (updated) {
        StudentSemesterProgressService.getInstance().syncStudentProgress(studentId);
      }

      return updated;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean delete(String studentId, Long semesterSubjectId) {
    String deleteSql = "DELETE FROM student_enrolled_subjects WHERE student_id = ? AND semester_subject_id = ?";

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(deleteSql)
    ) {
      ps.setString(1, studentId);
      ps.setLong(2, semesterSubjectId);

      boolean deleted = ps.executeUpdate() > 0;
      if (deleted) {
        StudentSemesterProgressService.getInstance().syncStudentProgress(studentId);
      }
      return deleted;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }
}