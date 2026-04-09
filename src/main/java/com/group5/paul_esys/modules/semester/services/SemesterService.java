package com.group5.paul_esys.modules.semester.services;

import com.group5.paul_esys.modules.semester.model.Semester;
import com.group5.paul_esys.modules.semester.utils.SemesterUtils;
import com.group5.paul_esys.modules.users.services.ConnectionService;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
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

  private boolean hasYearLevelColumn(Connection conn) {
    try {
      DatabaseMetaData metadata = conn.getMetaData();
      try (
        ResultSet rs = metadata.getColumns(null, null, "SEMESTER", "YEAR_LEVEL")
      ) {
        if (rs.next()) {
          return true;
        }
      }

      try (
        ResultSet rs = metadata.getColumns(null, null, "semester", "year_level")
      ) {
        return rs.next();
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
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
    try (Connection conn = ConnectionService.getConnection()) {
      boolean hasYearLevel = hasYearLevelColumn(conn);
      String sql;
      if (hasYearLevel) {
        sql = "INSERT INTO semester (curriculum_id, semester, year_level) VALUES (?, ?, ?)";
      } else {
        sql = "INSERT INTO semester (curriculum_id, semester) VALUES (?, ?)";
      }

      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setLong(1, semester.getCurriculumId());
        ps.setString(2, semester.getSemester());

        if (hasYearLevel) {
          ps.setInt(3, 1);
        }

        return ps.executeUpdate() > 0;
      }
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
