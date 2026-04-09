package com.group5.paul_esys.modules.enrollments.utils;

import com.group5.paul_esys.modules.enums.SemesterProgressStatus;
import com.group5.paul_esys.modules.enrollments.model.StudentSemesterProgress;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StudentSemesterProgressUtils {

  public static StudentSemesterProgress mapResultSetToStudentSemesterProgress(ResultSet rs) throws SQLException {
    return new StudentSemesterProgress(
        rs.getLong("id"),
        rs.getString("student_id"),
        rs.getLong("curriculum_id"),
        rs.getLong("semester_id"),
        SemesterProgressStatus.valueOf(rs.getString("status")),
        rs.getTimestamp("started_at"),
        rs.getTimestamp("completed_at"),
        rs.getTimestamp("created_at"),
        rs.getTimestamp("updated_at")
    );
  }
}