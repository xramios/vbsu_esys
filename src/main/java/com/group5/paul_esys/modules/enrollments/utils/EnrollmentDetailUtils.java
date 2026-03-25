package com.group5.paul_esys.modules.enrollments.utils;

import com.group5.paul_esys.modules.enums.EnrollmentDetailStatus;
import com.group5.paul_esys.modules.enrollments.model.EnrollmentDetail;

import java.sql.ResultSet;
import java.sql.SQLException;

public class EnrollmentDetailUtils {

  public static EnrollmentDetail mapResultSetToEnrollmentDetail(ResultSet rs) throws SQLException {
    return new EnrollmentDetail(
        rs.getLong("id"),
        rs.getLong("enrollment_id"),
        rs.getLong("section_id"),
        rs.getLong("subject_id"),
        rs.getFloat("units"),
        EnrollmentDetailStatus.valueOf(rs.getString("status")),
        rs.getTimestamp("created_at"),
        rs.getTimestamp("updated_at")
    );
  }
}
