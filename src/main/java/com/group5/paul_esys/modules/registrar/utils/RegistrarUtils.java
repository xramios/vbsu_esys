package com.group5.paul_esys.modules.registrar.utils;

import com.group5.paul_esys.modules.registrar.model.Registrar;

import java.sql.ResultSet;
import java.sql.SQLException;

public class RegistrarUtils {

  public static Registrar mapResultSetToRegistrar(ResultSet rs) throws SQLException {
    return new Registrar(
        rs.getLong("id"),
        rs.getObject("user_id", Long.class),
        rs.getString("employee_id"),
        rs.getString("first_name"),
        rs.getString("last_name"),
        rs.getString("contact_number"),
        rs.getTimestamp("updated_at"),
        rs.getTimestamp("created_at")
    );
  }
}
