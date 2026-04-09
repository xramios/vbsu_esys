package com.group5.paul_esys.modules.subjects.services;

import com.group5.paul_esys.modules.subjects.model.Subject;
import com.group5.paul_esys.modules.subjects.utils.SubjectUtils;
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

public class SubjectService {

  private static final SubjectService INSTANCE = new SubjectService();
  private static final Logger logger = LoggerFactory.getLogger(SubjectService.class);

  private SubjectService() {
  }

  public static SubjectService getInstance() {
    return INSTANCE;
  }

  public List<Subject> getAllSubjects() {
    List<Subject> subjects = new ArrayList<>();
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM subjects ORDER BY subject_code");
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        subjects.add(SubjectUtils.mapResultSetToSubject(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return subjects;
  }

  public Optional<Subject> getSubjectById(Long id) {
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM subjects WHERE id = ?")) {
      ps.setLong(1, id);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(SubjectUtils.mapResultSetToSubject(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return Optional.empty();
  }

  public Optional<Subject> getSubjectByCode(String subjectCode) {
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM subjects WHERE subject_code = ?")) {
      ps.setString(1, subjectCode);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(SubjectUtils.mapResultSetToSubject(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return Optional.empty();
  }

  public List<Subject> getSubjectsByDepartment(Long departmentId) {
    List<Subject> subjects = new ArrayList<>();
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM subjects WHERE department_id = ? ORDER BY subject_code")) {
      ps.setLong(1, departmentId);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          subjects.add(SubjectUtils.mapResultSetToSubject(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return subjects;
  }

  public List<Subject> getSubjectsByCurriculum(Long curriculumId) {
    // subjects table no longer stores curriculum_id; keep this method for compatibility.
    return getAllSubjects();
  }

  public boolean createSubject(Subject subject) {
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO subjects (subject_name, subject_code, units, description, department_id) VALUES (?, ?, ?, ?, ?)"
        )) {
      ps.setString(1, subject.getSubjectName());
      ps.setString(2, subject.getSubjectCode());
      ps.setFloat(3, subject.getUnits());
      ps.setString(4, subject.getDescription());
      ps.setLong(5, subject.getDepartmentId());
      
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean updateSubject(Subject subject) {
    try (Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(
        "UPDATE subjects SET subject_name = ?, subject_code = ?, units = ?, description = ?, department_id = ? WHERE id = ?"
      )) {
      ps.setString(1, subject.getSubjectName());
      ps.setString(2, subject.getSubjectCode());
      ps.setFloat(3, subject.getUnits());
      ps.setString(4, subject.getDescription());
      ps.setLong(5, subject.getDepartmentId());
      ps.setLong(6, subject.getId());
      
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean deleteSubject(Long id) {
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("DELETE FROM subjects WHERE id = ?")) {
      ps.setLong(1, id);
      
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }
}
