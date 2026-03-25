package com.group5.paul_esys.modules.students.services;

import com.group5.paul_esys.modules.students.model.Student;
import com.group5.paul_esys.modules.students.utils.StudentMapper;
import com.group5.paul_esys.modules.users.services.ConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class StudentService {

  private final Connection conn = ConnectionService.getConnection();
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  public Optional<Student> get(String studentId) {
    String sql = "SELECT * FROM students WHERE student_id = ?";
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      ResultSet rs = stmt.executeQuery();

      if (rs.next()) {
        if (rs.getRow() > 0) {
          return Optional.of(StudentMapper.mapToStudent(rs));
        } else {
          return Optional.empty();
        }
      }
    } catch (SQLException e) {
      logger.error(e.getMessage());
    }
  }

  public Optional<Student> insert(Student student) {
    String sql = "INSERT INTO students (" +
        "student_id" +
        "user_id" +
        "first_name" +
        "last_name" +
        "middle_name" +
        "birthdate" +
        "student_status" +
        "course_id" +
        "year_level" +
        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, student.getStudentId());
      stmt.setInt(2, student.getUserId());
      stmt.setString(3, student.getFirstName());
      stmt.setString(4, student.getLastName());
      stmt.setString(5, student.getMiddleName());
      stmt.setDate(6, new java.sql.Date(student.getBirthdate().getTime()));
      stmt.setString(7, student.getStudentStatus().name());
      stmt.setInt(8, student.getCourseId());
      stmt.setInt(9, student.getYearLevel());

      ResultSet rs = stmt.executeQuery();
      if (rs.next()) {
        return Optional.of(StudentMapper.mapToStudent(rs));
      } else {
        return Optional.empty();
      }
    } catch (SQLException e ) {
      logger.error(e.getMessage());
    }

    return Optional.empty();
  }

  public void delete(String studentId) {
    String sql = "DELETE FROM students WHERE student_id = ?";

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1,  studentId);
      int rowsDeleted = stmt.executeUpdate();

      if (rs.next()) {
        logger.info("Student with ID %s was successfully deleted!", rs);
      }
    } catch (SQLException e) {
      logger.error(e.getMessage());
    }
  }

}
