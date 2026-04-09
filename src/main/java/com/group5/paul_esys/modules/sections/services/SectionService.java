package com.group5.paul_esys.modules.sections.services;

import com.group5.paul_esys.modules.sections.model.Section;
import com.group5.paul_esys.modules.sections.utils.SectionUtils;
import com.group5.paul_esys.modules.users.services.ConnectionService;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SectionService {

  private static final String DEFAULT_STATUS = "OPEN";

  private static final SectionService INSTANCE = new SectionService();
  private static final Logger logger = LoggerFactory.getLogger(SectionService.class);

  private SectionService() {
  }

  public static SectionService getInstance() {
    return INSTANCE;
  }

  public List<Section> getAllSections() {
    List<Section> sections = new ArrayList<>();
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM sections ORDER BY section_code");
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        sections.add(SectionUtils.mapResultSetToSection(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return sections;
  }

  public Optional<Section> getSectionById(Long id) {
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM sections WHERE id = ?")) {
      ps.setLong(1, id);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(SectionUtils.mapResultSetToSection(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return Optional.empty();
  }

  public boolean createSection(Section section) {
    if (section == null) {
      return false;
    }

    try (Connection conn = ConnectionService.getConnection()) {
      boolean hasStatusColumn = hasStatusColumn(conn);
      String sql = hasStatusColumn
          ? "INSERT INTO sections (section_name, section_code, capacity, status) VALUES (?, ?, ?, ?)"
          : "INSERT INTO sections (section_name, section_code, capacity) VALUES (?, ?, ?)";

      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, normalizeText(section.getSectionName()));
        ps.setString(2, normalizeText(section.getSectionCode()));
        ps.setInt(3, normalizeCapacity(section.getCapacity()));
        if (hasStatusColumn) {
          ps.setString(4, normalizeStatus(section.getStatus()));
        }

        return ps.executeUpdate() > 0;
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean updateSection(Section section) {
    if (section == null || section.getId() == null) {
      return false;
    }

    try (Connection conn = ConnectionService.getConnection()) {
      boolean hasStatusColumn = hasStatusColumn(conn);
      String sql = hasStatusColumn
          ? "UPDATE sections SET section_name = ?, section_code = ?, capacity = ?, status = ? WHERE id = ?"
          : "UPDATE sections SET section_name = ?, section_code = ?, capacity = ? WHERE id = ?";

      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, normalizeText(section.getSectionName()));
        ps.setString(2, normalizeText(section.getSectionCode()));
        ps.setInt(3, normalizeCapacity(section.getCapacity()));
        if (hasStatusColumn) {
          ps.setString(4, normalizeStatus(section.getStatus()));
          ps.setLong(5, section.getId());
        } else {
          ps.setLong(4, section.getId());
        }

        return ps.executeUpdate() > 0;
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public Map<Long, Integer> getSelectedEnrollmentCountBySectionId() {
    Map<Long, Integer> enrolledCountBySectionId = new HashMap<>();

    String sql = "SELECT o.section_id, COUNT(*) AS enrolled_count "
      + "FROM enrollments_details ed "
      + "INNER JOIN offerings o ON o.id = ed.offering_id "
      + "WHERE ed.status = ? "
      + "GROUP BY o.section_id";

    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, "SELECTED");

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          enrolledCountBySectionId.put(rs.getLong("section_id"), rs.getInt("enrolled_count"));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }

    return enrolledCountBySectionId;
  }

  public boolean deleteSection(Long id) {
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("DELETE FROM sections WHERE id = ?")) {
      ps.setLong(1, id);
      
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  private boolean hasStatusColumn(Connection conn) {
    return hasColumn(conn, "status");
  }

  private boolean hasColumn(Connection conn, String columnName) {
    try {
      DatabaseMetaData metadata = conn.getMetaData();
      try (ResultSet rs = metadata.getColumns(null, null, "SECTIONS", columnName.toUpperCase())) {
        if (rs.next()) {
          return true;
        }
      }

      try (ResultSet rs = metadata.getColumns(null, null, "sections", columnName.toLowerCase())) {
        return rs.next();
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  private String normalizeText(String value) {
    if (value == null) {
      return "";
    }

    return value.trim();
  }

  private int normalizeCapacity(Integer capacity) {
    return capacity == null ? 0 : capacity;
  }

  private String normalizeStatus(String status) {
    if (status == null || status.trim().isEmpty()) {
      return DEFAULT_STATUS;
    }

    String normalized = status.trim().toUpperCase();
    return switch (normalized) {
      case "OPEN", "CLOSED", "WAITLIST", "DISSOLVED" -> normalized;
      default -> DEFAULT_STATUS;
    };
  }
}
