package com.group5.paul_esys.modules.rooms.services;

import com.group5.paul_esys.modules.rooms.model.Room;
import com.group5.paul_esys.modules.rooms.utils.RoomUtils;
import com.group5.paul_esys.modules.users.services.ConnectionService;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoomService {

  private static final String DEFAULT_BUILDING = "MAIN";
  private static final String DEFAULT_ROOM_TYPE = "OTHER";
  private static final String DEFAULT_STATUS = "AVAILABLE";

  private static final RoomService INSTANCE = new RoomService();
  private static final Logger logger = LoggerFactory.getLogger(RoomService.class);

  private RoomService() {
  }

  public static RoomService getInstance() {
    return INSTANCE;
  }

  public List<String> getDistinctBuildings() {
    List<String> buildings = new ArrayList<>();
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT DISTINCT building FROM rooms ORDER BY building");
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        String building = rs.getString("building");
        if (building != null && !building.trim().isEmpty()) {
          buildings.add(building.trim());
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }

    return buildings;
  }

  public List<Room> getAllRooms() {
    List<Room> rooms = new ArrayList<>();
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM rooms ORDER BY room");
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        rooms.add(RoomUtils.mapResultSetToRoom(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return rooms;
  }

  public Optional<Room> getRoomById(Long id) {
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM rooms WHERE id = ?")) {
      ps.setLong(1, id);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(RoomUtils.mapResultSetToRoom(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return Optional.empty();
  }

  public Optional<Room> getRoomByName(String roomName) {
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM rooms WHERE room = ?")) {
      ps.setString(1, roomName);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(RoomUtils.mapResultSetToRoom(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return Optional.empty();
  }

  public List<Room> getRoomsByCapacity(int minCapacity) {
    List<Room> rooms = new ArrayList<>();
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM rooms WHERE capacity >= ? ORDER BY room")) {
      ps.setInt(1, minCapacity);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          rooms.add(RoomUtils.mapResultSetToRoom(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return rooms;
  }

  public boolean createRoom(Room room) {
    if (room == null) {
      return false;
    }

    try (Connection conn = ConnectionService.getConnection()) {
      boolean hasRoomMetadataColumns = hasRoomMetadataColumns(conn);
      String sql = hasRoomMetadataColumns
          ? "INSERT INTO rooms (building, room_type, status, room, capacity) VALUES (?, ?, ?, ?, ?)"
          : "INSERT INTO rooms (room, capacity) VALUES (?, ?)";

      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        if (hasRoomMetadataColumns) {
          ps.setString(1, normalizeBuilding(room.getBuilding()));
          ps.setString(2, normalizeRoomType(room.getRoomType()));
          ps.setString(3, normalizeStatus(room.getStatus()));
          ps.setString(4, normalizeRoomName(room.getRoom()));
          ps.setInt(5, normalizeCapacity(room.getCapacity()));
        } else {
          ps.setString(1, normalizeRoomName(room.getRoom()));
          ps.setInt(2, normalizeCapacity(room.getCapacity()));
        }

        return ps.executeUpdate() > 0;
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean updateRoom(Room room) {
    if (room == null || room.getId() == null) {
      return false;
    }

    try (Connection conn = ConnectionService.getConnection()) {
      boolean hasRoomMetadataColumns = hasRoomMetadataColumns(conn);
      String sql = hasRoomMetadataColumns
          ? "UPDATE rooms SET building = ?, room_type = ?, status = ?, room = ?, capacity = ? WHERE id = ?"
          : "UPDATE rooms SET room = ?, capacity = ? WHERE id = ?";

      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        if (hasRoomMetadataColumns) {
          ps.setString(1, normalizeBuilding(room.getBuilding()));
          ps.setString(2, normalizeRoomType(room.getRoomType()));
          ps.setString(3, normalizeStatus(room.getStatus()));
          ps.setString(4, normalizeRoomName(room.getRoom()));
          ps.setInt(5, normalizeCapacity(room.getCapacity()));
          ps.setLong(6, room.getId());
        } else {
          ps.setString(1, normalizeRoomName(room.getRoom()));
          ps.setInt(2, normalizeCapacity(room.getCapacity()));
          ps.setLong(3, room.getId());
        }

        return ps.executeUpdate() > 0;
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean deleteRoom(Long id) {
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("DELETE FROM rooms WHERE id = ?")) {
      ps.setLong(1, id);

      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  private boolean hasRoomMetadataColumns(Connection conn) {
    return hasColumn(conn, "building")
        && hasColumn(conn, "room_type")
        && hasColumn(conn, "status");
  }

  private boolean hasColumn(Connection conn, String columnName) {
    try {
      DatabaseMetaData metadata = conn.getMetaData();
      try (ResultSet rs = metadata.getColumns(null, null, "ROOMS", columnName.toUpperCase())) {
        if (rs.next()) {
          return true;
        }
      }

      try (ResultSet rs = metadata.getColumns(null, null, "rooms", columnName.toLowerCase())) {
        return rs.next();
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  private String normalizeRoomName(String roomName) {
    return roomName == null ? "" : roomName.trim();
  }

  private int normalizeCapacity(Integer capacity) {
    return capacity == null ? 0 : capacity;
  }

  private String normalizeBuilding(String building) {
    if (building == null || building.trim().isEmpty()) {
      return DEFAULT_BUILDING;
    }

    return building.trim();
  }

  private String normalizeRoomType(String roomType) {
    if (roomType == null || roomType.trim().isEmpty()) {
      return DEFAULT_ROOM_TYPE;
    }

    String normalized = roomType.trim().toUpperCase();
    return switch (normalized) {
      case "LECTURE", "LAB", "SEMINAR", "AUDITORIUM", "OTHER" -> normalized;
      default -> DEFAULT_ROOM_TYPE;
    };
  }

  private String normalizeStatus(String status) {
    if (status == null || status.trim().isEmpty()) {
      return DEFAULT_STATUS;
    }

    String normalized = status.trim().toUpperCase();
    return switch (normalized) {
      case "AVAILABLE", "UNAVAILABLE", "MAINTENANCE" -> normalized;
      default -> DEFAULT_STATUS;
    };
  }
}
