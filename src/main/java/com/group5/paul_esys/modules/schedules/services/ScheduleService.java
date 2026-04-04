package com.group5.paul_esys.modules.schedules.services;

import com.group5.paul_esys.modules.enums.DayOfWeek;
import com.group5.paul_esys.modules.schedules.model.Schedule;
import com.group5.paul_esys.modules.schedules.utils.ScheduleUtils;
import com.group5.paul_esys.modules.users.services.ConnectionService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScheduleService {

  private static final ScheduleService INSTANCE = new ScheduleService();
  private static final Logger logger = LoggerFactory.getLogger(ScheduleService.class);

  private ScheduleService() {
  }

  public static ScheduleService getInstance() {
    return INSTANCE;
  }

  public List<Schedule> getAllSchedules() {
    List<Schedule> schedules = new ArrayList<>();
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM schedules ORDER BY day, start_time");
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        schedules.add(ScheduleUtils.mapResultSetToSchedule(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return schedules;
  }

  public Optional<Schedule> getScheduleById(Long id) {
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM schedules WHERE id = ?")) {
      ps.setLong(1, id);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(ScheduleUtils.mapResultSetToSchedule(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return Optional.empty();
  }

  public List<Schedule> getSchedulesBySection(Long sectionId) {
    List<Schedule> schedules = new ArrayList<>();
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM schedules WHERE section_id = ? ORDER BY day, start_time")) {
      ps.setLong(1, sectionId);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          schedules.add(ScheduleUtils.mapResultSetToSchedule(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return schedules;
  }

  public List<Schedule> getSchedulesByFaculty(Long facultyId) {
    List<Schedule> schedules = new ArrayList<>();
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM schedules WHERE faculty_id = ? ORDER BY day, start_time")) {
      ps.setLong(1, facultyId);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          schedules.add(ScheduleUtils.mapResultSetToSchedule(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return schedules;
  }

  public List<Schedule> getSchedulesByRoom(Long roomId) {
    List<Schedule> schedules = new ArrayList<>();
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM schedules WHERE room_id = ? ORDER BY day, start_time")) {
      ps.setLong(1, roomId);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          schedules.add(ScheduleUtils.mapResultSetToSchedule(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return schedules;
  }

  public List<Schedule> getSchedulesByDay(DayOfWeek day) {
    List<Schedule> schedules = new ArrayList<>();
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM schedules WHERE day = ? ORDER BY start_time")) {
      ps.setString(1, day.name());

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          schedules.add(ScheduleUtils.mapResultSetToSchedule(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return schedules;
  }

  public boolean createSchedule(Schedule schedule) {
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO schedules (section_id, room_id, faculty_id, day, start_time, end_time, enrollment_period_id) VALUES (?, ?, ?, ?, ?, ?, ?)"
        )) {
      ps.setLong(1, schedule.getSectionId());
      ps.setLong(2, schedule.getRoomId());
      ps.setLong(3, schedule.getFacultyId());
      ps.setString(4, schedule.getDay().name());
      ps.setTime(5, schedule.getStartTime());
      ps.setTime(6, schedule.getEndTime());
      ps.setLong(7, schedule.getEnrollmentPeriodId());
      
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean updateSchedule(Schedule schedule) {
    try (Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(
        "UPDATE schedules SET section_id = ?, room_id = ?, faculty_id = ?, day = ?, start_time = ?, end_time = ?, enrollment_period_id = ? WHERE id = ?"
      )) {
      ps.setLong(1, schedule.getSectionId());
      ps.setLong(2, schedule.getRoomId());
      ps.setLong(3, schedule.getFacultyId());
      ps.setString(4, schedule.getDay().name());
      ps.setTime(5, schedule.getStartTime());
      ps.setTime(6, schedule.getEndTime());
      ps.setLong(7, schedule.getEnrollmentPeriodId());
      ps.setLong(8, schedule.getId());
      
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean deleteSchedule(Long id) {
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("DELETE FROM schedules WHERE id = ?")) {
      ps.setLong(1, id);
      
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }
}
