package com.group5.paul_esys.modules.students.utils;

import com.group5.paul_esys.modules.students.model.Student;
import com.group5.paul_esys.modules.students.model.StudentStatus;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StudentMapper {

  public static Student mapToStudent(ResultSet rs) throws SQLException {
    return new Student(
      rs.getString("student_id"),
      rs.getLong("user_id"),
      rs.getString("first_name"),
      rs.getString("last_name"),
      rs.getString("middle_name"),
      rs.getDate("birthdate"),
      StudentStatus.valueOf(rs.getString("student_status")),
      rs.getLong("course_id"),
      rs.getObject("curriculum_id", Long.class),
      rs.getLong("year_level"),
      rs.getTimestamp("created_at")
    );
  }
}
