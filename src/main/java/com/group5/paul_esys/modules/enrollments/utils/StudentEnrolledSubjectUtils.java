package com.group5.paul_esys.modules.enrollments.utils;

import com.group5.paul_esys.modules.enums.StudentEnrolledSubjectStatus;
import com.group5.paul_esys.modules.enrollments.model.StudentEnrolledSubject;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StudentEnrolledSubjectUtils {

  public static StudentEnrolledSubject mapResultSetToStudentEnrolledSubject(ResultSet rs) throws SQLException {
    return new StudentEnrolledSubject(
        rs.getString("student_id"),
        rs.getLong("semester_subject_id"),
        StudentEnrolledSubjectStatus.valueOf(rs.getString("status")),
        rs.getTimestamp("created_at"),
        rs.getTimestamp("updated_at")
    );
  }
}