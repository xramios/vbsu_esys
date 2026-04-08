package com.group5.paul_esys.modules.semester.services;

import com.group5.paul_esys.modules.semester.model.Semester;
import com.group5.paul_esys.modules.semester.utils.SemesterUtils;
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

public class SemesterService {

  private static final SemesterService INSTANCE = new SemesterService();
  private static final Logger logger = LoggerFactory.getLogger(SemesterService.class);

  private SemesterService() {
  }

  public static SemesterService getInstance() {
    return INSTANCE;
  }

  public List<Semester> getAllSemesters() {
    List<Semester> semesters = new ArrayList<>();
    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement("SELECT * FROM semester ORDER BY created_at DESC");
      ResultSet rs = ps.executeQuery()
    ) {
      while (rs.next()) {
        semesters.add(SemesterUtils.mapResultSetToSemester(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return semesters;
  }

  public Optional<Semester> getSemesterById(Long id) {
    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement("SELECT * FROM semester WHERE id = ?")
    ) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(SemesterUtils.mapResultSetToSemester(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }

    return Optional.empty();
  }

  public List<Semester> getSemestersByCurriculum(Long curriculumId) {
    List<Semester> semesters = new ArrayList<>();
    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement("SELECT * FROM semester WHERE curriculum_id = ? ORDER BY created_at DESC")
    ) {
      ps.setLong(1, curriculumId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          semesters.add(SemesterUtils.mapResultSetToSemester(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }

    return semesters;
  }

  public boolean createSemester(Semester semester) {
    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(
        "INSERT INTO semester (curriculum_id, semester) VALUES (?, ?)"
      )
    ) {
      ps.setLong(1, semester.getCurriculumId());
      ps.setString(2, semester.getSemester());

      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean updateSemester(Semester semester) {
    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(
        "UPDATE semester SET curriculum_id = ?, semester = ? WHERE id = ?"
      )
    ) {
      ps.setLong(1, semester.getCurriculumId());
      ps.setString(2, semester.getSemester());
      ps.setLong(3, semester.getId());

      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean deleteSemester(Long id) {
    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement("DELETE FROM semester WHERE id = ?")
    ) {
      ps.setLong(1, id);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }
}
