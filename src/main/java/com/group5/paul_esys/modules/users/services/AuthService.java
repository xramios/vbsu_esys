package com.group5.paul_esys.modules.users.services;

import com.group5.paul_esys.modules.faculty.model.Faculty;
import com.group5.paul_esys.modules.faculty.services.FacultyService;
import com.group5.paul_esys.modules.registrar.model.Registrar;
import com.group5.paul_esys.modules.registrar.services.RegistrarService;
import com.group5.paul_esys.modules.students.model.Student;
import com.group5.paul_esys.modules.students.services.StudentService;
import com.group5.paul_esys.modules.users.models.enums.Role;
import com.group5.paul_esys.modules.users.models.user.LoginData;
import com.group5.paul_esys.modules.users.models.user.UserInformation;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class AuthService {

  private static final AuthService INSTANCE = new AuthService();
  private static final java.util.logging.Logger logger =
    java.util.logging.Logger.getLogger(AuthService.class.getName());

  // Mga services
  private final StudentService studentService = StudentService.getInstance();
  private final FacultyService facultyService = FacultyService.getInstance();
  private final RegistrarService registrarService =
    RegistrarService.getInstance();

  private AuthService() {}

  public static AuthService getInstance() {
    return INSTANCE;
  }

  private UserInformation<Registrar> getRegistrarInformation(
    ResultSet rs,
    LoginData loginData
  ) throws SQLException {
    logger.info("Fetching registrar information for user: " + rs.getLong("id"));
    Optional<Registrar> registrar = registrarService.getRegistrarByUserId(
      rs.getLong("id")
    );

    if (registrar.isEmpty()) {
      throw new IllegalArgumentException(
        "The user account is not connected to any information"
      );
    }

    UserInformation<Registrar> userInformation = new UserInformation<>(
      rs.getLong("id"),
      loginData.getEmail(),
      loginData.getPassword(),
      Role.REGISTRAR
    );

    userInformation.setUser(registrar.get());
    return userInformation;
  }

  private UserInformation<Student> getStudentInformation(
    ResultSet rs,
    LoginData loginData
  ) throws SQLException {
    logger.info("Fetching student information for user: " + rs.getLong("id"));
    Optional<Student> student = studentService.getStudentByUserId(
      rs.getLong("id")
    );

    if (student.isEmpty()) {
      throw new IllegalArgumentException(
        "The user account is not connected to any information"
      );
    }

    UserInformation<Student> userInformation = new UserInformation<>(
      student.get().getUserId(),
      loginData.getEmail(),
      loginData.getPassword(),
      Role.STUDENT
    );

    userInformation.setUser(student.get());
    return userInformation;
  }

  private UserInformation<Faculty> getFacultyInformation(
    ResultSet rs,
    LoginData loginData
  ) throws SQLException {
    logger.info("Fetching faculty information for user: " + rs.getLong("id"));
    Optional<Faculty> faculty = facultyService.getFacultyByUserId(
      rs.getLong("id")
    );

    if (faculty.isEmpty()) {
      throw new IllegalArgumentException(
        "The user account is not connected to any information"
      );
    }

    UserInformation<Faculty> userInformation = new UserInformation<>(
      rs.getLong("id"),
      loginData.getEmail(),
      loginData.getPassword(),
      Role.FACULTY
    );

    userInformation.setUser(faculty.get());
    return userInformation;
  }

  public Optional<UserInformation<?>> login(LoginData loginData)
    throws IllegalArgumentException {
    if (!loginData.isValid()) {
      throw new IllegalArgumentException("Invalid login data");
    }

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(
        "SELECT * FROM users WHERE email = ?"
      )
    ) {
      ps.setString(1, loginData.getEmail());

      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return Optional.empty();
        }

        // Bago imap out, need ko muna tignan kung anong type of user tapos get yung information.
        switch (rs.getString("role")) {
          case "REGISTRAR" -> {
            return Optional.of(getRegistrarInformation(rs, loginData));
          }
          case "STUDENT" -> {
            return Optional.of(getStudentInformation(rs, loginData));
          }
          case "FACULTY" -> {
            return Optional.of(getFacultyInformation(rs, loginData));
          }
        }
      }
    } catch (SQLException e) {
      e.printStackTrace();
      return Optional.empty();
    }
    return null;
  }
}
