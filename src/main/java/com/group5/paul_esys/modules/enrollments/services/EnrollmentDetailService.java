package com.group5.paul_esys.modules.enrollments.services;

import com.group5.paul_esys.modules.enums.EnrollmentDetailStatus;
import com.group5.paul_esys.modules.enrollments.model.EnrollmentDetail;
import com.group5.paul_esys.modules.enrollments.utils.EnrollmentDetailUtils;
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

public class EnrollmentDetailService {

  private static final EnrollmentDetailService INSTANCE = new EnrollmentDetailService();
  private static final Logger logger = LoggerFactory.getLogger(EnrollmentDetailService.class);

  private EnrollmentDetailService() {
  }

  public static EnrollmentDetailService getInstance() {
    return INSTANCE;
  }

  public List<EnrollmentDetail> getAllEnrollmentDetails() {
    List<EnrollmentDetail> details = new ArrayList<>();
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM enrollments_details ORDER BY created_at DESC");
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        details.add(EnrollmentDetailUtils.mapResultSetToEnrollmentDetail(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return details;
  }

  public Optional<EnrollmentDetail> getEnrollmentDetailById(Long id) {
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM enrollments_details WHERE id = ?")) {
      ps.setLong(1, id);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(EnrollmentDetailUtils.mapResultSetToEnrollmentDetail(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return Optional.empty();
  }

  public List<EnrollmentDetail> getEnrollmentDetailsByEnrollment(Long enrollmentId) {
    List<EnrollmentDetail> details = new ArrayList<>();
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM enrollments_details WHERE enrollment_id = ? ORDER BY created_at")) {
      ps.setLong(1, enrollmentId);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          details.add(EnrollmentDetailUtils.mapResultSetToEnrollmentDetail(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return details;
  }

  public List<EnrollmentDetail> getEnrollmentDetailsBySection(Long sectionId) {
    List<EnrollmentDetail> details = new ArrayList<>();
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM enrollments_details WHERE section_id = ? ORDER BY created_at")) {
      ps.setLong(1, sectionId);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          details.add(EnrollmentDetailUtils.mapResultSetToEnrollmentDetail(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return details;
  }

  public boolean createEnrollmentDetail(EnrollmentDetail detail) {
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO enrollments_details (enrollment_id, section_id, subject_id, units, status) VALUES (?, ?, ?, ?, ?)"
        )) {
      ps.setLong(1, detail.getEnrollmentId());
      ps.setLong(2, detail.getSectionId());
      ps.setLong(3, detail.getSubjectId());
      ps.setFloat(4, detail.getUnits());
      ps.setString(5, detail.getStatus().name());

      boolean created = ps.executeUpdate() > 0;
      if (created) {
        StudentSemesterProgressService.getInstance().syncByEnrollmentId(detail.getEnrollmentId());
      }
      return created;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean updateEnrollmentDetail(EnrollmentDetail detail) {
    try (Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(
        "UPDATE enrollments_details SET enrollment_id = ?, section_id = ?, subject_id = ?, units = ?, status = ? WHERE id = ?"
      )) {
      ps.setLong(1, detail.getEnrollmentId());
      ps.setLong(2, detail.getSectionId());
      ps.setLong(3, detail.getSubjectId());
      ps.setFloat(4, detail.getUnits());
      ps.setString(5, detail.getStatus().name());
      ps.setLong(6, detail.getId());

      boolean updated = ps.executeUpdate() > 0;
      if (updated) {
        StudentSemesterProgressService.getInstance().syncByEnrollmentId(detail.getEnrollmentId());
      }
      return updated;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean updateEnrollmentDetailStatus(Long id, EnrollmentDetailStatus status) {
    Optional<Long> enrollmentId = getEnrollmentIdByDetailId(id);

    try (Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(
        "UPDATE enrollments_details SET status = ? WHERE id = ?"
      )) {
      ps.setString(1, status.name());
      ps.setLong(2, id);

      boolean updated = ps.executeUpdate() > 0;
      if (updated && enrollmentId.isPresent()) {
        StudentSemesterProgressService.getInstance().syncByEnrollmentId(enrollmentId.get());
      }
      return updated;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean deleteEnrollmentDetail(Long id) {
    Optional<Long> enrollmentId = getEnrollmentIdByDetailId(id);

    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("DELETE FROM enrollments_details WHERE id = ?")) {
      ps.setLong(1, id);

      boolean deleted = ps.executeUpdate() > 0;
      if (deleted && enrollmentId.isPresent()) {
        StudentSemesterProgressService.getInstance().syncByEnrollmentId(enrollmentId.get());
      }
      return deleted;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  private Optional<Long> getEnrollmentIdByDetailId(Long detailId) {
    String sql = "SELECT enrollment_id FROM enrollments_details WHERE id = ?";

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql)
    ) {
      ps.setLong(1, detailId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(rs.getLong("enrollment_id"));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }

    return Optional.empty();
  }
}
