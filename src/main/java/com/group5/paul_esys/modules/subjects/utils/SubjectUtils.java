package com.group5.paul_esys.modules.subjects.utils;

import com.group5.paul_esys.modules.subjects.model.Subject;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SubjectUtils {

  public static Subject mapResultSetToSubject(ResultSet rs) throws SQLException {
    return new Subject(
        rs.getLong("id"),
        rs.getString("subject_name"),
        rs.getString("subject_code"),
        rs.getFloat("units"),
        rs.getString("description"),
        rs.getLong("department_id"),
        rs.getTimestamp("updated_at"),
        rs.getTimestamp("created_at")
    );
  }
}
