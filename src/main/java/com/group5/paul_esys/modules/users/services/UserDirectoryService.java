package com.group5.paul_esys.modules.users.services;

import com.group5.paul_esys.modules.users.models.enums.Role;
import com.group5.paul_esys.modules.users.models.user.UserDirectoryRow;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserDirectoryService {

  private static final UserDirectoryService INSTANCE = new UserDirectoryService();
  private static final Logger logger = LoggerFactory.getLogger(
    UserDirectoryService.class
  );

  private UserDirectoryService() {}

  public static UserDirectoryService getInstance() {
    return INSTANCE;
  }

  public List<UserDirectoryRow> getAllUsers() {
    String sql =
      "SELECT " +
      "u.id AS user_id, u.email, u.role, " +
      "s.student_id, s.first_name AS student_first_name, s.last_name AS student_last_name, " +
      "f.id AS faculty_id, f.first_name AS faculty_first_name, f.last_name AS faculty_last_name, f.contact_number AS faculty_contact_number, " +
      "r.id AS registrar_id, r.first_name AS registrar_first_name, r.last_name AS registrar_last_name, r.contact_number AS registrar_contact_number, " +
      "a.id AS admin_id, a.first_name AS admin_first_name, a.last_name AS admin_last_name, a.contact_number AS admin_contact_number " +
      "FROM users u " +
      "LEFT JOIN students s ON s.user_id = u.id " +
      "LEFT JOIN faculty f ON f.user_id = u.id " +
      "LEFT JOIN registrar r ON r.user_id = u.id " +
      "LEFT JOIN admins a ON a.user_id = u.id " +
      "ORDER BY u.role, u.email";

    List<UserDirectoryRow> rows = new ArrayList<>();

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql);
      ResultSet rs = ps.executeQuery()
    ) {
      while (rs.next()) {
        rows.add(mapRow(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }

    return rows;
  }

  public boolean deleteUser(UserDirectoryRow userRow) {
    if (userRow == null || userRow.getUserId() == null) {
      return false;
    }

    try (Connection conn = ConnectionService.getConnection()) {
      conn.setAutoCommit(false);

      try {
        boolean roleDeleteSuccess = deleteRoleProfile(conn, userRow);
        if (!roleDeleteSuccess) {
          conn.rollback();
          return false;
        }

        try (
          PreparedStatement deleteUserStmt = conn.prepareStatement(
            "DELETE FROM users WHERE id = ?"
          )
        ) {
          deleteUserStmt.setLong(1, userRow.getUserId());
          if (deleteUserStmt.executeUpdate() <= 0) {
            conn.rollback();
            return false;
          }
        }

        conn.commit();
        return true;
      } catch (SQLException e) {
        conn.rollback();
        logger.error("ERROR: " + e.getMessage(), e);
        return false;
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  private UserDirectoryRow mapRow(ResultSet rs) throws SQLException {
    Role role = Role.valueOf(rs.getString("role").toUpperCase());

    return switch (role) {
      case STUDENT -> new UserDirectoryRow(
        rs.getLong("user_id"),
        null,
        rs.getString("student_id"),
        rs.getString("email"),
        role,
        rs.getString("student_first_name"),
        rs.getString("student_last_name"),
        null
      );
      case FACULTY -> new UserDirectoryRow(
        rs.getLong("user_id"),
        rs.getObject("faculty_id", Long.class),
        null,
        rs.getString("email"),
        role,
        rs.getString("faculty_first_name"),
        rs.getString("faculty_last_name"),
        rs.getString("faculty_contact_number")
      );
      case REGISTRAR -> new UserDirectoryRow(
        rs.getLong("user_id"),
        rs.getObject("registrar_id", Long.class),
        null,
        rs.getString("email"),
        role,
        rs.getString("registrar_first_name"),
        rs.getString("registrar_last_name"),
        rs.getString("registrar_contact_number")
      );
      case ADMIN -> new UserDirectoryRow(
        rs.getLong("user_id"),
        rs.getObject("admin_id", Long.class),
        null,
        rs.getString("email"),
        role,
        rs.getString("admin_first_name"),
        rs.getString("admin_last_name"),
        rs.getString("admin_contact_number")
      );
    };
  }

  private boolean deleteRoleProfile(Connection conn, UserDirectoryRow userRow)
    throws SQLException {
    return switch (userRow.getRole()) {
      case STUDENT -> deleteStudentProfile(conn, userRow);
      case FACULTY -> deleteByNumericId(conn, "faculty", userRow.getProfileId());
      case REGISTRAR -> deleteByNumericId(conn, "registrar", userRow.getProfileId());
      case ADMIN -> deleteByNumericId(conn, "admins", userRow.getProfileId());
    };
  }

  private boolean deleteStudentProfile(Connection conn, UserDirectoryRow userRow)
    throws SQLException {
    if (userRow.getStudentId() != null && !userRow.getStudentId().trim().isEmpty()) {
      try (
        PreparedStatement ps = conn.prepareStatement(
          "DELETE FROM students WHERE student_id = ?"
        )
      ) {
        ps.setString(1, userRow.getStudentId());
        return ps.executeUpdate() > 0;
      }
    }

    try (
      PreparedStatement ps = conn.prepareStatement(
        "DELETE FROM students WHERE user_id = ?"
      )
    ) {
      ps.setLong(1, userRow.getUserId());
      return ps.executeUpdate() > 0;
    }
  }

  private boolean deleteByNumericId(Connection conn, String table, Long id)
    throws SQLException {
    if (id == null) {
      return false;
    }

    try (
      PreparedStatement ps = conn.prepareStatement(
        "DELETE FROM " + table + " WHERE id = ?"
      )
    ) {
      ps.setLong(1, id);
      return ps.executeUpdate() > 0;
    }
  }
}
