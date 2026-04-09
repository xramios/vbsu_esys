package com.group5.paul_esys.modules.offerings.utils;

import com.group5.paul_esys.modules.offerings.model.Offering;

import java.sql.ResultSet;
import java.sql.SQLException;

public class OfferingUtils {

  public static Offering mapResultSetToOffering(ResultSet rs) throws SQLException {
    Long semesterSubjectId = rs.getLong("semester_subject_id");
    if (rs.wasNull()) {
      semesterSubjectId = null;
    }

    Integer capacity = rs.getInt("capacity");
    if (rs.wasNull()) {
      capacity = null;
    }

    return new Offering(
        rs.getLong("id"),
        rs.getLong("subject_id"),
        rs.getLong("section_id"),
        rs.getLong("enrollment_period_id"),
        semesterSubjectId,
        capacity,
        rs.getTimestamp("created_at")
    );
  }
}
