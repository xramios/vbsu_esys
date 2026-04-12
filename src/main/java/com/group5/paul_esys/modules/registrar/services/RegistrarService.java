package com.group5.paul_esys.modules.registrar.services;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.group5.paul_esys.modules.registrar.model.Registrar;
import com.group5.paul_esys.modules.registrar.utils.RegistrarUtils;
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

public class RegistrarService {

  private static final RegistrarService INSTANCE = new RegistrarService();
  private static final Logger logger = LoggerFactory.getLogger(RegistrarService.class);

  private RegistrarService() {
  }

  public static RegistrarService getInstance() {
    return INSTANCE;
  }

  public List<Registrar> getAllRegistrars() {
    List<Registrar> registrars = new ArrayList<>();
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM registrar ORDER BY last_name, first_name");
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        registrars.add(RegistrarUtils.mapResultSetToRegistrar(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return registrars;
  }

  public Optional<Registrar> getRegistrarById(Long id) {
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM registrar WHERE id = ?")) {
      ps.setLong(1, id);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(RegistrarUtils.mapResultSetToRegistrar(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return Optional.empty();
  }

  public Optional<Registrar> getRegistrarByUserId(Long userId) {
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM registrar WHERE user_id = ?")) {
      ps.setLong(1, userId);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(RegistrarUtils.mapResultSetToRegistrar(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return Optional.empty();
  }

  public Optional<Registrar> getRegistrarByEmployeeId(String employeeId) {
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM registrar WHERE employee_id = ?")) {
      ps.setString(1, employeeId);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(RegistrarUtils.mapResultSetToRegistrar(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return Optional.empty();
  }

  public boolean createRegistrar(Registrar registrar) {
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO registrar (user_id, employee_id, first_name, last_name, contact_number) VALUES (?, ?, ?, ?, ?)"
        )) {
      ps.setObject(1, registrar.getUserId());
      ps.setString(2, registrar.getEmployeeId());
      ps.setString(3, registrar.getFirstName());
      ps.setString(4, registrar.getLastName());
      ps.setString(5, registrar.getContactNumber());

      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public Optional<Registrar> registerRegistrar(
    String email,
    String plainPassword,
    Registrar registrar
  ) {
    String userSql = "INSERT INTO users (email, password, role) VALUES (?, ?, ?)";
    String registrarSql =
      "INSERT INTO registrar (user_id, employee_id, first_name, last_name, contact_number) VALUES (?, ?, ?, ?, ?)";

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
        PreparedStatement registrarStmt = conn.prepareStatement(registrarSql)
      ) {
        userStmt.setString(1, email);
        userStmt.setString(2, hashedPassword);
        userStmt.setString(3, "REGISTRAR");

        if (userStmt.executeUpdate() <= 0) {
          conn.rollback();
          return Optional.empty();
        }

        try (ResultSet generatedKeys = userStmt.getGeneratedKeys()) {
          if (!generatedKeys.next()) {
            conn.rollback();
            return Optional.empty();
          }

          registrar.setUserId(generatedKeys.getLong(1));
        }

        registrarStmt.setLong(1, registrar.getUserId());
        registrarStmt.setString(2, registrar.getEmployeeId());
        registrarStmt.setString(3, registrar.getFirstName());
        registrarStmt.setString(4, registrar.getLastName());
        registrarStmt.setString(5, registrar.getContactNumber());

        if (registrarStmt.executeUpdate() <= 0) {
          conn.rollback();
          return Optional.empty();
        }

        conn.commit();
        return Optional.of(registrar);
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

  public boolean updateRegistrarWithEmail(
    Registrar registrar,
    String email,
    String newPlainPassword
  ) {
    if (registrar == null || registrar.getId() == null || registrar.getUserId() == null) {
      return false;
    }

    String registrarSql =
      "UPDATE registrar SET employee_id = ?, first_name = ?, last_name = ?, contact_number = ? WHERE id = ?";
    String userSqlWithPassword =
      "UPDATE users SET email = ?, password = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
    String userSqlWithoutPassword =
      "UPDATE users SET email = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

    try (Connection conn = ConnectionService.getConnection()) {
      conn.setAutoCommit(false);

      try (PreparedStatement registrarStmt = conn.prepareStatement(registrarSql)) {
        registrarStmt.setString(1, registrar.getEmployeeId());
        registrarStmt.setString(2, registrar.getFirstName());
        registrarStmt.setString(3, registrar.getLastName());
        registrarStmt.setString(4, registrar.getContactNumber());
        registrarStmt.setLong(5, registrar.getId());

        if (registrarStmt.executeUpdate() <= 0) {
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
          userStmt.setLong(3, registrar.getUserId());

          if (userStmt.executeUpdate() <= 0) {
            conn.rollback();
            return false;
          }
        }
      } else {
        try (PreparedStatement userStmt = conn.prepareStatement(userSqlWithoutPassword)) {
          userStmt.setString(1, email);
          userStmt.setLong(2, registrar.getUserId());

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

  public boolean updateRegistrar(Registrar registrar) {
    try (Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(
        "UPDATE registrar SET user_id = ?, employee_id = ?, first_name = ?, last_name = ?, contact_number = ? WHERE id = ?"
      )) {
      ps.setObject(1, registrar.getUserId());
      ps.setString(2, registrar.getEmployeeId());
      ps.setString(3, registrar.getFirstName());
      ps.setString(4, registrar.getLastName());
      ps.setString(5, registrar.getContactNumber());
      ps.setLong(6, registrar.getId());

      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean deleteRegistrar(Long id) {
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("DELETE FROM registrar WHERE id = ?")) {
      ps.setLong(1, id);

      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }
}
