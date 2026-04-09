package com.group5.paul_esys.modules.offerings.services;

import com.group5.paul_esys.modules.offerings.model.Offering;
import com.group5.paul_esys.modules.offerings.utils.OfferingUtils;
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

public class OfferingService {

  private static final OfferingService INSTANCE = new OfferingService();
  private static final Logger logger = LoggerFactory.getLogger(OfferingService.class);

  private OfferingService() {
  }

  public static OfferingService getInstance() {
    return INSTANCE;
  }

  public List<Offering> getAllOfferings() {
    List<Offering> offerings = new ArrayList<>();
    String sql = "SELECT * FROM offerings ORDER BY enrollment_period_id, section_id, subject_id";

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql);
      ResultSet rs = ps.executeQuery()
    ) {
      while (rs.next()) {
        offerings.add(OfferingUtils.mapResultSetToOffering(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }

    return offerings;
  }

  public Optional<Offering> getOfferingById(Long id) {
    String sql = "SELECT * FROM offerings WHERE id = ?";

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql)
    ) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(OfferingUtils.mapResultSetToOffering(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }

    return Optional.empty();
  }

  public List<Offering> getOfferingsByEnrollmentPeriod(Long enrollmentPeriodId) {
    List<Offering> offerings = new ArrayList<>();
    String sql = "SELECT * FROM offerings WHERE enrollment_period_id = ? ORDER BY section_id, subject_id";

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql)
    ) {
      ps.setLong(1, enrollmentPeriodId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          offerings.add(OfferingUtils.mapResultSetToOffering(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }

    return offerings;
  }

  public List<Offering> getOfferingsBySection(Long sectionId) {
    List<Offering> offerings = new ArrayList<>();
    String sql = "SELECT * FROM offerings WHERE section_id = ? ORDER BY enrollment_period_id DESC, subject_id";

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql)
    ) {
      ps.setLong(1, sectionId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          offerings.add(OfferingUtils.mapResultSetToOffering(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }

    return offerings;
  }

  public List<Offering> getOfferingsBySubject(Long subjectId) {
    List<Offering> offerings = new ArrayList<>();
    String sql = "SELECT * FROM offerings WHERE subject_id = ? ORDER BY enrollment_period_id DESC, section_id";

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql)
    ) {
      ps.setLong(1, subjectId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          offerings.add(OfferingUtils.mapResultSetToOffering(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }

    return offerings;
  }

  public boolean createOffering(Offering offering) {
    String sql = "INSERT INTO offerings (subject_id, section_id, enrollment_period_id, semester_subject_id, capacity) VALUES (?, ?, ?, ?, ?)";

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql)
    ) {
      ps.setLong(1, offering.getSubjectId());
      ps.setLong(2, offering.getSectionId());
      ps.setLong(3, offering.getEnrollmentPeriodId());
      ps.setObject(4, offering.getSemesterSubjectId());
      ps.setObject(5, offering.getCapacity());

      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean updateOffering(Offering offering) {
    String sql = "UPDATE offerings SET subject_id = ?, section_id = ?, enrollment_period_id = ?, semester_subject_id = ?, capacity = ? WHERE id = ?";

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql)
    ) {
      ps.setLong(1, offering.getSubjectId());
      ps.setLong(2, offering.getSectionId());
      ps.setLong(3, offering.getEnrollmentPeriodId());
      ps.setObject(4, offering.getSemesterSubjectId());
      ps.setObject(5, offering.getCapacity());
      ps.setLong(6, offering.getId());

      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean deleteOffering(Long id) {
    String sql = "DELETE FROM offerings WHERE id = ?";

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql)
    ) {
      ps.setLong(1, id);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }
}
