package com.group5.paul_esys.modules.schedules.utils;

import com.group5.paul_esys.modules.enums.DayOfWeek;
import com.group5.paul_esys.modules.schedules.model.Schedule;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ScheduleUtils {

  public static Schedule mapResultSetToSchedule(ResultSet rs) throws SQLException {
    return new Schedule(
        rs.getLong("id"),
        rs.getLong("offering_id"),
        rs.getLong("room_id"),
        rs.getLong("faculty_id"),
        DayOfWeek.valueOf(rs.getString("day")),
        rs.getString("start_time") != null ? java.sql.Time.valueOf(rs.getString("start_time")) : null,
        rs.getString("end_time") != null ? java.sql.Time.valueOf(rs.getString("end_time")) : null,
        rs.getTimestamp("updated_at"),
        rs.getTimestamp("created_at")
    );
  }
}
