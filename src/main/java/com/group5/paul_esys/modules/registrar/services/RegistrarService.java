package com.group5.paul_esys.modules.registrar.services;

import com.group5.paul_esys.modules.registrar.model.Registrar;
import com.group5.paul_esys.modules.registrar.utils.RegistrarUtils;
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
