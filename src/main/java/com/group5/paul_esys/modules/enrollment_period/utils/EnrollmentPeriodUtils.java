package com.group5.paul_esys.modules.enrollment.utils;

import com.group5.paul_esys.modules.enrollment.model.EnrollmentPeriod;

import java.sql.ResultSet;
import java.sql.SQLException;

public class EnrollmentPeriodUtils {

  public static EnrollmentPeriod mapResultSetToEnrollmentPeriod(ResultSet rs) throws SQLException {
    return new EnrollmentPeriod(
        rs.getLong("id"),
        rs.getString("school_year"),
        rs.getString("semester"),
        rs.getTimestamp("start_date"),
        rs.getTimestamp("end_date"),
        rs.getTimestamp("updated_at"),
        rs.getTimestamp("created_at")
    );
  }
}
