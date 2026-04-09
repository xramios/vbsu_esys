package com.group5.paul_esys.modules.sections.utils;

import com.group5.paul_esys.modules.sections.model.Section;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class SectionUtils {

  public static Section mapResultSetToSection(ResultSet rs) throws SQLException {
    return new Section(
        rs.getLong("id"),
        rs.getString("section_name"),
        rs.getString("section_code"),
        rs.getInt("capacity"),
        getOptionalColumnValue(rs, "status", "OPEN"),
        rs.getTimestamp("updated_at"),
        rs.getTimestamp("created_at")
    );
  }

  private static String getOptionalColumnValue(ResultSet rs, String columnName, String fallback) throws SQLException {
    if (!hasColumn(rs, columnName)) {
      return fallback;
    }

    String value = rs.getString(columnName);
    return value == null || value.trim().isEmpty() ? fallback : value.trim();
  }

  private static boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
    ResultSetMetaData metadata = rs.getMetaData();
    for (int index = 1; index <= metadata.getColumnCount(); index++) {
      String label = metadata.getColumnLabel(index);
      String name = metadata.getColumnName(index);

      if (columnName.equalsIgnoreCase(label) || columnName.equalsIgnoreCase(name)) {
        return true;
      }
    }

    return false;
  }
}
