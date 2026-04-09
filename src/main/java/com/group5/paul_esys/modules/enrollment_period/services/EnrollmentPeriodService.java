package com.group5.paul_esys.modules.enrollment.services;

import com.group5.paul_esys.modules.enrollment.model.EnrollmentPeriod;
import com.group5.paul_esys.modules.enrollment.utils.EnrollmentPeriodUtils;
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

public class EnrollmentPeriodService {

  private static final EnrollmentPeriodService INSTANCE = new EnrollmentPeriodService();
  private static final Logger logger = LoggerFactory.getLogger(EnrollmentPeriodService.class);

  private EnrollmentPeriodService() {
  }

  public static EnrollmentPeriodService getInstance() {
    return INSTANCE;
  }

  public List<EnrollmentPeriod> getAllEnrollmentPeriods() {
    List<EnrollmentPeriod> periods = new ArrayList<>();
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM enrollment_period ORDER BY created_at DESC");
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        periods.add(EnrollmentPeriodUtils.mapResultSetToEnrollmentPeriod(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return periods;
  }

  public Optional<EnrollmentPeriod> getEnrollmentPeriodById(Long id) {
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM enrollment_period WHERE id = ?")) {
      ps.setLong(1, id);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(EnrollmentPeriodUtils.mapResultSetToEnrollmentPeriod(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return Optional.empty();
  }

  public Optional<EnrollmentPeriod> getCurrentEnrollmentPeriod() {
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement(
            "SELECT * FROM enrollment_period WHERE start_date <= CURRENT_TIMESTAMP AND end_date >= CURRENT_TIMESTAMP"
        );
        ResultSet rs = ps.executeQuery()) {
      if (rs.next()) {
        return Optional.of(EnrollmentPeriodUtils.mapResultSetToEnrollmentPeriod(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return Optional.empty();
  }

  public boolean createEnrollmentPeriod(EnrollmentPeriod period) {
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO enrollment_period (school_year, semester, start_date, end_date) VALUES (?, ?, ?, ?)"
        )) {
      ps.setString(1, period.getSchoolYear());
      ps.setString(2, period.getSemester());
      ps.setTimestamp(3, new java.sql.Timestamp(period.getStartDate().getTime()));
      ps.setTimestamp(4, new java.sql.Timestamp(period.getEndDate().getTime()));
      
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean updateEnrollmentPeriod(EnrollmentPeriod period) {
    try (Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(
        "UPDATE enrollment_period SET school_year = ?, semester = ?, start_date = ?, end_date = ? WHERE id = ?"
      )) {
      ps.setString(1, period.getSchoolYear());
      ps.setString(2, period.getSemester());
      ps.setTimestamp(3, new java.sql.Timestamp(period.getStartDate().getTime()));
      ps.setTimestamp(4, new java.sql.Timestamp(period.getEndDate().getTime()));
      ps.setLong(5, period.getId());
      
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean deleteEnrollmentPeriod(Long id) {
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("DELETE FROM enrollment_period WHERE id = ?")) {
      ps.setLong(1, id);
      
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }
}
