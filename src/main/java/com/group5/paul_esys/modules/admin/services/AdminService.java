package com.group5.paul_esys.modules.admin.services;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.group5.paul_esys.modules.admin.model.Admin;
import com.group5.paul_esys.modules.admin.utils.AdminUtils;
import com.group5.paul_esys.modules.users.services.ConnectionService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminService {

  private static final AdminService INSTANCE = new AdminService();
  private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
  private static final String ADMIN_COLUMNS =
    "id, user_id, first_name, last_name, contact_number, updated_at, created_at";

  private AdminService() {}

  public static AdminService getInstance() {
    return INSTANCE;
  }

  public List<Admin> getAllAdmins() {
    List<Admin> admins = new ArrayList<>();
    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(
        "SELECT " + ADMIN_COLUMNS + " FROM admins ORDER BY last_name, first_name"
      );
      ResultSet rs = ps.executeQuery()
    ) {
      while (rs.next()) {
        admins.add(AdminUtils.mapResultSetToAdmin(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return admins;
  }

  public Optional<Admin> getAdminById(Long id) {
    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(
        "SELECT " + ADMIN_COLUMNS + " FROM admins WHERE id = ?"
      )
    ) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(AdminUtils.mapResultSetToAdmin(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }

    return Optional.empty();
  }

  public Optional<Admin> getAdminByUserId(Long userId) {
    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(
        "SELECT " + ADMIN_COLUMNS + " FROM admins WHERE user_id = ?"
      )
    ) {
      ps.setLong(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(AdminUtils.mapResultSetToAdmin(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }

    return Optional.empty();
  }

  public boolean createAdmin(Admin admin) {
    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(
        "INSERT INTO admins (user_id, first_name, last_name, contact_number) VALUES (?, ?, ?, ?)"
      )
    ) {
      ps.setObject(1, admin.getUserId());
      ps.setString(2, admin.getFirstName());
      ps.setString(3, admin.getLastName());
      ps.setString(4, admin.getContactNumber());

      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public Optional<Admin> registerAdmin(String email, String plainPassword, Admin admin) {
    String userSql = "INSERT INTO users (email, password, role) VALUES (?, ?, ?)";
    String adminSql =
      "INSERT INTO admins (user_id, first_name, last_name, contact_number) VALUES (?, ?, ?, ?)";

    String hashedPassword = BCrypt
      .withDefaults()
      .hashToString(12, plainPassword.toCharArray());

    try (Connection conn = ConnectionService.getConnection()) {
      conn.setAutoCommit(false);

      try (
        PreparedStatement userStmt = conn.prepareStatement(
          userSql,
          Statement.RETURN_GENERATED_KEYS
        );
        PreparedStatement adminStmt = conn.prepareStatement(adminSql)
      ) {
        userStmt.setString(1, email);
        userStmt.setString(2, hashedPassword);
        userStmt.setString(3, "ADMIN");

        if (userStmt.executeUpdate() <= 0) {
          conn.rollback();
          return Optional.empty();
        }

        try (ResultSet generatedKeys = userStmt.getGeneratedKeys()) {
          if (!generatedKeys.next()) {
            conn.rollback();
            return Optional.empty();
          }
          admin.setUserId(generatedKeys.getLong(1));
        }

        adminStmt.setLong(1, admin.getUserId());
        adminStmt.setString(2, admin.getFirstName());
        adminStmt.setString(3, admin.getLastName());
        adminStmt.setString(4, admin.getContactNumber());

        if (adminStmt.executeUpdate() <= 0) {
          conn.rollback();
          return Optional.empty();
        }

        conn.commit();
        return Optional.of(admin);
      } catch (SQLException e) {
        conn.rollback();
        logger.error("ERROR: " + e.getMessage(), e);
        return Optional.empty();
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return Optional.empty();
    }
  }

  public Optional<String> getUserEmailByUserId(Long userId) {
    if (userId == null) {
      return Optional.empty();
    }

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(
        "SELECT email FROM users WHERE id = ?"
      )
    ) {
      ps.setLong(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.ofNullable(rs.getString("email"));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }

    return Optional.empty();
  }

  public boolean updateAdminWithEmail(Admin admin, String email, String newPlainPassword) {
    if (admin == null || admin.getId() == null || admin.getUserId() == null) {
      return false;
    }

    String adminSql =
      "UPDATE admins SET first_name = ?, last_name = ?, contact_number = ? WHERE id = ?";
    String userSqlWithPassword =
      "UPDATE users SET email = ?, password = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
    String userSqlWithoutPassword =
      "UPDATE users SET email = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

    try (Connection conn = ConnectionService.getConnection()) {
      conn.setAutoCommit(false);

      try (PreparedStatement adminStmt = conn.prepareStatement(adminSql)) {
        adminStmt.setString(1, admin.getFirstName());
        adminStmt.setString(2, admin.getLastName());
        adminStmt.setString(3, admin.getContactNumber());
        adminStmt.setLong(4, admin.getId());

        if (adminStmt.executeUpdate() <= 0) {
          conn.rollback();
          return false;
        }
      }

      boolean changePassword =
        newPlainPassword != null && !newPlainPassword.trim().isEmpty();

      if (changePassword) {
        String hashedPassword = BCrypt
          .withDefaults()
          .hashToString(12, newPlainPassword.toCharArray());

        try (PreparedStatement userStmt = conn.prepareStatement(userSqlWithPassword)) {
          userStmt.setString(1, email);
          userStmt.setString(2, hashedPassword);
          userStmt.setLong(3, admin.getUserId());

          if (userStmt.executeUpdate() <= 0) {
            conn.rollback();
            return false;
          }
        }
      } else {
        try (PreparedStatement userStmt = conn.prepareStatement(userSqlWithoutPassword)) {
          userStmt.setString(1, email);
          userStmt.setLong(2, admin.getUserId());

          if (userStmt.executeUpdate() <= 0) {
            conn.rollback();
            return false;
          }
        }
      }

      conn.commit();
      return true;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean deleteAdmin(Long id) {
    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement("DELETE FROM admins WHERE id = ?")
    ) {
      ps.setLong(1, id);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }
}
