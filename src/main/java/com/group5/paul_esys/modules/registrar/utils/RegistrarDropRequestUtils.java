package com.group5.paul_esys.modules.registrar.utils;

import com.group5.paul_esys.modules.enums.DropRequestStatus;
import com.group5.paul_esys.modules.registrar.model.FacultyStudentDropRequest;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RegistrarDropRequestUtils {

  private RegistrarDropRequestUtils() {
  }

  public static FacultyStudentDropRequest mapResultSetToDropRequest(ResultSet rs) throws SQLException {
    return new FacultyStudentDropRequest()
        .setId(rs.getLong("id"))
        .setFacultyId(rs.getObject("faculty_id", Long.class))
        .setStudentId(rs.getString("student_id"))
        .setOfferingId(rs.getObject("offering_id", Long.class))
        .setReason(rs.getString("reason"))
        .setStatus(DropRequestStatus.fromValue(rs.getString("status")))
        .setCreatedAt(rs.getTimestamp("created_at"))
        .setUpdatedAt(rs.getTimestamp("updated_at"))
        .setFacultyName(buildPersonName(rs.getString("faculty_first_name"), rs.getString("faculty_last_name"), "N/A"))
        .setStudentName(buildPersonName(rs.getString("student_first_name"), rs.getString("student_last_name"), "N/A"))
        .setSubjectCode(safeText(rs.getString("subject_code"), "N/A"))
        .setSubjectName(safeText(rs.getString("subject_name"), "N/A"))
        .setSectionCode(safeText(rs.getString("section_code"), "N/A"))
        .setEnrollmentPeriodLabel(buildEnrollmentPeriodLabel(rs.getString("school_year"), rs.getString("semester")))
        .setEnrollmentId(rs.getObject("enrollment_id", Long.class))
        .setEnrollmentDetailStatus(safeText(rs.getString("enrollment_detail_status"), "N/A"));
  }

  private static String buildPersonName(String firstName, String lastName, String fallback) {
    String first = safeText(firstName, "");
    String last = safeText(lastName, "");

    if (first.isEmpty() && last.isEmpty()) {
      return fallback;
    }

    if (first.isEmpty()) {
      return last;
    }

    if (last.isEmpty()) {
      return first;
    }

    return last + ", " + first;
  }

  private static String buildEnrollmentPeriodLabel(String schoolYear, String semester) {
    String year = safeText(schoolYear, "N/A");
    String sem = safeText(semester, "N/A");
    return year + " | " + sem;
  }

  private static String safeText(String value, String fallback) {
    if (value == null) {
      return fallback;
    }

    String normalized = value.trim();
    return normalized.isEmpty() ? fallback : normalized;
  }
}