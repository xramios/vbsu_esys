package com.group5.paul_esys.modules.enrollments.utils;

import com.group5.paul_esys.modules.enums.EnrollmentStatus;
import com.group5.paul_esys.modules.enrollments.model.Enrollment;

import java.sql.ResultSet;
import java.sql.SQLException;

public class EnrollmentUtils {

  public static Enrollment mapResultSetToEnrollment(ResultSet rs) throws SQLException {
    return new Enrollment(
        rs.getLong("id"),
        rs.getString("student_id"),
        rs.getString("school_year"),
        rs.getInt("semester"),
        EnrollmentStatus.valueOf(rs.getString("status")),
        rs.getFloat("max_units"),
        rs.getFloat("total_units"),
        rs.getTimestamp("submitted_at"),
        rs.getTimestamp("updated_at"),
        rs.getTimestamp("created_at")
    );
  }
}
