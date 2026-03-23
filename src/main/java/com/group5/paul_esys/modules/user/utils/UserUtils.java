package com.group5.paul_esys.modules.user.utils;

import com.group5.paul_esys.modules.user.models.user.UserInformation;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserUtils {

  public static UserInformation mapResultSetToUserInformation(ResultSet rs) throws SQLException {
    return new UserInformation(
        rs.getInt("user_id"),
        rs.getString("email"),
        rs.getString("password"),
        rs.getString("role")
    );
  }
}
