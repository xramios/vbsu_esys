package com.group5.paul_esys.modules.admin.utils;

import com.group5.paul_esys.modules.admin.model.Admin;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdminUtils {

  public static Admin mapResultSetToAdmin(ResultSet rs) throws SQLException {
    return new Admin(
      rs.getLong("id"),
      rs.getObject("user_id", Long.class),
      rs.getString("first_name"),
      rs.getString("last_name"),
      rs.getString("contact_number"),
      rs.getTimestamp("updated_at"),
      rs.getTimestamp("created_at")
    );
  }
}
