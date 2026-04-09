package com.group5.paul_esys.modules.semester.utils;

import com.group5.paul_esys.modules.semester.model.Semester;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SemesterUtils {

  public static Semester mapResultSetToSemester(ResultSet rs) throws SQLException {
    return new Semester(
      rs.getLong("id"),
      rs.getLong("curriculum_id"),
      rs.getString("semester"),
      rs.getInt("year_level"),
      rs.getTimestamp("created_at"),
      rs.getTimestamp("updated_at")
    );
  }
}
