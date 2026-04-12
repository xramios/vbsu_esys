package com.group5.paul_esys.modules.registrar.services;

import com.group5.paul_esys.modules.registrar.model.ScheduleLookupOption;
import com.group5.paul_esys.modules.registrar.model.ScheduleManagementRow;
import com.group5.paul_esys.modules.registrar.model.ScheduleOfferingOption;
import com.group5.paul_esys.modules.registrar.model.ScheduleSaveResult;
import com.group5.paul_esys.modules.registrar.model.ScheduleUpsertRequest;
import com.group5.paul_esys.modules.users.services.ConnectionService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegistrarScheduleManagementService {

  private static final RegistrarScheduleManagementService INSTANCE = new RegistrarScheduleManagementService();
  private static final Logger logger = LoggerFactory.getLogger(RegistrarScheduleManagementService.class);

  private RegistrarScheduleManagementService() {
  }

  public static RegistrarScheduleManagementService getInstance() {
    return INSTANCE;
  }

  public List<ScheduleManagementRow> getScheduleRows() {
    String sql = """
        SELECT
          s.id AS schedule_id,
          s.offering_id,
          s.day,
          s.start_time,
          s.end_time,
          o.section_id,
          o.enrollment_period_id,
          ep.school_year,
          ep.semester,
          sec.section_code,
          sub.subject_code,
          sub.subject_name,
          rm.id AS room_id,
          rm.room AS room_name,
          fac.id AS faculty_id,
          fac.first_name AS faculty_first_name,
          fac.last_name AS faculty_last_name
        FROM schedules s
        INNER JOIN offerings o ON o.id = s.offering_id
        INNER JOIN enrollment_period ep ON ep.id = o.enrollment_period_id
        INNER JOIN sections sec ON sec.id = o.section_id
        INNER JOIN subjects sub ON sub.id = o.subject_id
        LEFT JOIN rooms rm ON rm.id = s.room_id
        LEFT JOIN faculty fac ON fac.id = s.faculty_id
        ORDER BY
          ep.created_at DESC,
          sec.section_code,
          sub.subject_code,
          CASE s.day
            WHEN 'MON' THEN 1
            WHEN 'TUE' THEN 2
            WHEN 'WED' THEN 3
            WHEN 'THU' THEN 4
            WHEN 'FRI' THEN 5
            WHEN 'SAT' THEN 6
            WHEN 'SUN' THEN 7
            ELSE 8
          END,
          s.start_time,
          s.id
        """;

    List<ScheduleManagementRow> rows = new ArrayList<>();
    try (
        Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()
    ) {
      while (rs.next()) {
        Long enrollmentPeriodId = rsGetLong(rs, "enrollment_period_id");

        rows.add(new ScheduleManagementRow(
            rsGetLong(rs, "schedule_id"),
            rsGetLong(rs, "offering_id"),
            enrollmentPeriodId,
            buildEnrollmentPeriodLabel(
                enrollmentPeriodId,
                rs.getString("school_year"),
                rs.getString("semester")
            ),
            rsGetLong(rs, "section_id"),
            safeText(rs.getString("section_code"), "N/A"),
            safeText(rs.getString("subject_code"), "N/A"),
            safeText(rs.getString("subject_name"), "N/A"),
            safeText(rs.getString("day"), "N/A"),
            rsGetLocalTime(rs, "start_time"),
            rsGetLocalTime(rs, "end_time"),
            rsGetLong(rs, "room_id"),
            rs.getString("room_name"),
            rsGetLong(rs, "faculty_id"),
            buildFacultyDisplayName(
                rs.getString("faculty_first_name"),
                rs.getString("faculty_last_name")
            ),
            false,
            false,
            false
        ));
      }
    } catch (SQLException e) {
      logger.error("ERROR: {}", e.getMessage(), e);
      return List.of();
    }

    return applyConflictFlags(rows);
  }

  public List<ScheduleLookupOption> getEnrollmentPeriodOptions() {
    String sql = """
        SELECT DISTINCT
          ep.id,
          ep.school_year,
          ep.semester,
          ep.created_at
        FROM offerings o
        INNER JOIN enrollment_period ep ON ep.id = o.enrollment_period_id
        ORDER BY ep.created_at DESC, ep.id DESC
        """;

    List<ScheduleLookupOption> options = new ArrayList<>();
    try (
        Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()
    ) {
      while (rs.next()) {
        Long enrollmentPeriodId = rsGetLong(rs, "id");
        options.add(new ScheduleLookupOption(
            enrollmentPeriodId,
            buildEnrollmentPeriodLabel(
                enrollmentPeriodId,
                rs.getString("school_year"),
                rs.getString("semester")
            )
        ));
      }
    } catch (SQLException e) {
      logger.error("ERROR: {}", e.getMessage(), e);
      return List.of();
    }

    return options;
  }

  public List<ScheduleOfferingOption> getOfferingOptions() {
    String sql = """
        SELECT
          o.id AS offering_id,
          o.subject_id,
          o.enrollment_period_id,
          ep.school_year,
          ep.semester,
          sec.section_code,
          sub.subject_code,
          sub.subject_name,
          COALESCE(o.capacity, sec.capacity) AS effective_capacity
        FROM offerings o
        INNER JOIN enrollment_period ep ON ep.id = o.enrollment_period_id
        INNER JOIN sections sec ON sec.id = o.section_id
        INNER JOIN subjects sub ON sub.id = o.subject_id
        ORDER BY
          ep.created_at DESC,
          sec.section_code,
          sub.subject_code,
          o.id
        """;

    List<ScheduleOfferingOption> options = new ArrayList<>();
  Map<Long, String> prerequisiteLabelBySubjectId = new HashMap<>();
    try (
        Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()
    ) {
      while (rs.next()) {
        Long enrollmentPeriodId = rsGetLong(rs, "enrollment_period_id");
        String periodLabel = buildEnrollmentPeriodLabel(
            enrollmentPeriodId,
            rs.getString("school_year"),
            rs.getString("semester")
        );

        Integer capacity = rs.getObject("effective_capacity", Integer.class);
        String capacityLabel = capacity == null || capacity <= 0 ? "Open" : String.valueOf(capacity);
        String sectionCode = safeText(rs.getString("section_code"), "N/A");
        String subjectCode = safeText(rs.getString("subject_code"), "N/A");
        String subjectName = safeText(rs.getString("subject_name"), "N/A");
        Long subjectId = rsGetLong(rs, "subject_id");

        String prerequisiteLabel = "None";
        if (subjectId != null) {
          if (prerequisiteLabelBySubjectId.containsKey(subjectId)) {
            prerequisiteLabel = prerequisiteLabelBySubjectId.get(subjectId);
          } else {
            prerequisiteLabel = resolvePrerequisiteLabel(conn, subjectId);
            prerequisiteLabelBySubjectId.put(subjectId, prerequisiteLabel);
          }
        }

        String label = sectionCode
            + " | " + subjectCode + " - " + subjectName
            + " | " + periodLabel
            + " | Cap " + capacityLabel;

        options.add(new ScheduleOfferingOption(
            rsGetLong(rs, "offering_id"),
            enrollmentPeriodId,
            periodLabel,
            sectionCode,
            subjectCode,
            subjectName,
            prerequisiteLabel,
            label
        ));
      }
    } catch (SQLException e) {
      logger.error("ERROR: {}", e.getMessage(), e);
      return List.of();
    }

    return options;
  }

  private String resolvePrerequisiteLabel(Connection conn, Long subjectId) throws SQLException {
    String sql = """
        SELECT
          pre.subject_code,
          pre.subject_name
        FROM prerequisites p
        INNER JOIN subjects pre ON pre.id = p.pre_subject_id
        WHERE p.subject_id = ?
        ORDER BY pre.subject_code, pre.subject_name
        """;

    List<String> prerequisiteParts = new ArrayList<>();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, subjectId);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String code = safeText(rs.getString("subject_code"), "N/A");
          String name = safeText(rs.getString("subject_name"), "N/A");
          prerequisiteParts.add(code + " - " + name);
        }
      }
    }

    if (prerequisiteParts.isEmpty()) {
      return "None";
    }

    return String.join(", ", prerequisiteParts);
  }

  public List<ScheduleLookupOption> getRoomOptions() {
    String sql = "SELECT id, building, room, status FROM rooms ORDER BY building, room";
    List<ScheduleLookupOption> options = new ArrayList<>();

    try (
        Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()
    ) {
      while (rs.next()) {
        String building = safeText(rs.getString("building"), "N/A");
        String room = safeText(rs.getString("room"), "N/A");
        String status = safeText(rs.getString("status"), "AVAILABLE");

        options.add(new ScheduleLookupOption(
            rsGetLong(rs, "id"),
            building + " - " + room + " (" + status + ")"
        ));
      }
    } catch (SQLException e) {
      logger.error("ERROR: {}", e.getMessage(), e);
      return List.of();
    }

    return options;
  }

  public List<ScheduleLookupOption> getFacultyOptions() {
    String sql = "SELECT id, first_name, last_name FROM faculty ORDER BY last_name, first_name";
    List<ScheduleLookupOption> options = new ArrayList<>();

    try (
        Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()
    ) {
      while (rs.next()) {
        options.add(new ScheduleLookupOption(
            rsGetLong(rs, "id"),
            safeText(
                buildFacultyDisplayName(rs.getString("first_name"), rs.getString("last_name")),
                "N/A"
            )
        ));
      }
    } catch (SQLException e) {
      logger.error("ERROR: {}", e.getMessage(), e);
      return List.of();
    }

    return options;
  }

  public ScheduleSaveResult createSchedule(ScheduleUpsertRequest request) {
    ScheduleSaveResult validation = validateRequest(request, false);
    if (!validation.successful()) {
      return validation;
    }

    String sql = "INSERT INTO schedules (offering_id, room_id, faculty_id, day, start_time, end_time) VALUES (?, ?, ?, ?, ?, ?)";

    try (
        Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)
    ) {
      if (!offeringExists(conn, request.offeringId())) {
        return new ScheduleSaveResult(false, "Selected offering is no longer available.");
      }

      ScheduleSaveResult conflictValidation = validateConflicts(conn, request, false);
      if (!conflictValidation.successful()) {
        return conflictValidation;
      }

      ps.setLong(1, request.offeringId());
      setNullableLong(ps, 2, request.roomId());
      setNullableLong(ps, 3, request.facultyId());
      ps.setString(4, request.day().name());
      ps.setTime(5, Time.valueOf(request.startTime()));
      ps.setTime(6, Time.valueOf(request.endTime()));

      int updated = ps.executeUpdate();
      if (updated <= 0) {
        return new ScheduleSaveResult(false, "Failed to create schedule.");
      }

      return new ScheduleSaveResult(true, "Schedule created successfully.");
    } catch (SQLException e) {
      logger.error("ERROR: {}", e.getMessage(), e);
      if (isUniqueConstraintError(e)) {
        return new ScheduleSaveResult(false, "A schedule with the same offering, day, and start time already exists.");
      }

      return new ScheduleSaveResult(false, "Failed to create schedule. Please try again.");
    }
  }

  public ScheduleSaveResult updateSchedule(ScheduleUpsertRequest request) {
    ScheduleSaveResult validation = validateRequest(request, true);
    if (!validation.successful()) {
      return validation;
    }

    String sql = """
        UPDATE schedules
        SET offering_id = ?,
            room_id = ?,
            faculty_id = ?,
            day = ?,
            start_time = ?,
            end_time = ?,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
        """;

    try (
        Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)
    ) {
      if (!scheduleExists(conn, request.scheduleId())) {
        return new ScheduleSaveResult(false, "Selected schedule no longer exists.");
      }

      if (!offeringExists(conn, request.offeringId())) {
        return new ScheduleSaveResult(false, "Selected offering is no longer available.");
      }

      ScheduleSaveResult conflictValidation = validateConflicts(conn, request, true);
      if (!conflictValidation.successful()) {
        return conflictValidation;
      }

      ps.setLong(1, request.offeringId());
      setNullableLong(ps, 2, request.roomId());
      setNullableLong(ps, 3, request.facultyId());
      ps.setString(4, request.day().name());
      ps.setTime(5, Time.valueOf(request.startTime()));
      ps.setTime(6, Time.valueOf(request.endTime()));
      ps.setLong(7, request.scheduleId());

      int updated = ps.executeUpdate();
      if (updated <= 0) {
        return new ScheduleSaveResult(false, "Failed to update schedule.");
      }

      return new ScheduleSaveResult(true, "Schedule updated successfully.");
    } catch (SQLException e) {
      logger.error("ERROR: {}", e.getMessage(), e);
      if (isUniqueConstraintError(e)) {
        return new ScheduleSaveResult(false, "A schedule with the same offering, day, and start time already exists.");
      }

      return new ScheduleSaveResult(false, "Failed to update schedule. Please try again.");
    }
  }

  public ScheduleSaveResult deleteSchedule(Long scheduleId) {
    if (scheduleId == null) {
      return new ScheduleSaveResult(false, "Please select a schedule to delete.");
    }

    String sql = "DELETE FROM schedules WHERE id = ?";
    try (
        Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)
    ) {
      ps.setLong(1, scheduleId);
      int deleted = ps.executeUpdate();
      if (deleted <= 0) {
        return new ScheduleSaveResult(false, "Selected schedule no longer exists.");
      }

      return new ScheduleSaveResult(true, "Schedule deleted successfully.");
    } catch (SQLException e) {
      logger.error("ERROR: {}", e.getMessage(), e);
      return new ScheduleSaveResult(false, "Failed to delete schedule. Please try again.");
    }
  }

  private ScheduleSaveResult validateRequest(ScheduleUpsertRequest request, boolean requireId) {
    if (request == null) {
      return new ScheduleSaveResult(false, "Schedule form data is required.");
    }

    if (requireId && request.scheduleId() == null) {
      return new ScheduleSaveResult(false, "A schedule ID is required for update.");
    }

    if (request.offeringId() == null) {
      return new ScheduleSaveResult(false, "Offering selection is required.");
    }

    if (request.day() == null) {
      return new ScheduleSaveResult(false, "Day selection is required.");
    }

    if (request.startTime() == null || request.endTime() == null) {
      return new ScheduleSaveResult(false, "Start and end time are required.");
    }

    if (!request.startTime().isBefore(request.endTime())) {
      return new ScheduleSaveResult(false, "Start time must be earlier than end time.");
    }

    return new ScheduleSaveResult(true, "OK");
  }

  private ScheduleSaveResult validateConflicts(Connection conn, ScheduleUpsertRequest request, boolean isUpdate)
      throws SQLException {
    if (hasOfferingDayStartDuplicate(conn, request, isUpdate)) {
      return new ScheduleSaveResult(false, "A schedule with the same offering, day, and start time already exists.");
    }

    if (hasSectionConflict(conn, request, isUpdate)) {
      return new ScheduleSaveResult(
          false,
          "Section conflict detected. The selected section already has a class in the selected day and time range."
      );
    }

    if (request.roomId() != null && hasRoomConflict(conn, request, isUpdate)) {
      return new ScheduleSaveResult(
          false,
          "Room conflict detected. Another schedule already uses the room in the selected day and time range."
      );
    }

    if (request.facultyId() != null && hasFacultyConflict(conn, request, isUpdate)) {
      return new ScheduleSaveResult(
          false,
          "Faculty conflict detected. The selected faculty already has a class in the selected day and time range."
      );
    }

    return new ScheduleSaveResult(true, "OK");
  }

  private boolean hasOfferingDayStartDuplicate(Connection conn, ScheduleUpsertRequest request, boolean isUpdate)
      throws SQLException {
    String sql = isUpdate
        ? "SELECT 1 FROM schedules WHERE offering_id = ? AND day = ? AND start_time = ? AND id <> ? FETCH FIRST 1 ROWS ONLY"
        : "SELECT 1 FROM schedules WHERE offering_id = ? AND day = ? AND start_time = ? FETCH FIRST 1 ROWS ONLY";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, request.offeringId());
      ps.setString(2, request.day().name());
      ps.setTime(3, Time.valueOf(request.startTime()));

      if (isUpdate) {
        ps.setLong(4, request.scheduleId());
      }

      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    }
  }

  private boolean hasRoomConflict(Connection conn, ScheduleUpsertRequest request, boolean isUpdate) throws SQLException {
    String sql = isUpdate
        ? """
          SELECT 1
          FROM schedules s
          INNER JOIN offerings existing_o ON existing_o.id = s.offering_id
          INNER JOIN offerings target_o ON target_o.id = ?
          WHERE s.room_id = ?
            AND s.day = ?
            AND existing_o.enrollment_period_id = target_o.enrollment_period_id
            AND s.id <> ?
            AND s.start_time < ?
            AND s.end_time > ?
          FETCH FIRST 1 ROWS ONLY
          """
        : """
          SELECT 1
          FROM schedules s
          INNER JOIN offerings existing_o ON existing_o.id = s.offering_id
          INNER JOIN offerings target_o ON target_o.id = ?
          WHERE s.room_id = ?
            AND s.day = ?
            AND existing_o.enrollment_period_id = target_o.enrollment_period_id
            AND s.start_time < ?
            AND s.end_time > ?
          FETCH FIRST 1 ROWS ONLY
          """;

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, request.offeringId());
      ps.setLong(2, request.roomId());
      ps.setString(3, request.day().name());

      int parameterIndex = 4;
      if (isUpdate) {
        ps.setLong(parameterIndex++, request.scheduleId());
      }

      ps.setTime(parameterIndex++, Time.valueOf(request.endTime()));
      ps.setTime(parameterIndex, Time.valueOf(request.startTime()));

      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    }
  }

  private boolean hasFacultyConflict(Connection conn, ScheduleUpsertRequest request, boolean isUpdate) throws SQLException {
    String sql = isUpdate
        ? """
          SELECT 1
          FROM schedules s
          INNER JOIN offerings existing_o ON existing_o.id = s.offering_id
          INNER JOIN offerings target_o ON target_o.id = ?
          WHERE s.faculty_id = ?
            AND s.day = ?
            AND existing_o.enrollment_period_id = target_o.enrollment_period_id
            AND s.id <> ?
            AND s.start_time < ?
            AND s.end_time > ?
          FETCH FIRST 1 ROWS ONLY
          """
        : """
          SELECT 1
          FROM schedules s
          INNER JOIN offerings existing_o ON existing_o.id = s.offering_id
          INNER JOIN offerings target_o ON target_o.id = ?
          WHERE s.faculty_id = ?
            AND s.day = ?
            AND existing_o.enrollment_period_id = target_o.enrollment_period_id
            AND s.start_time < ?
            AND s.end_time > ?
          FETCH FIRST 1 ROWS ONLY
          """;

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, request.offeringId());
      ps.setLong(2, request.facultyId());
      ps.setString(3, request.day().name());

      int parameterIndex = 4;
      if (isUpdate) {
        ps.setLong(parameterIndex++, request.scheduleId());
      }

      ps.setTime(parameterIndex++, Time.valueOf(request.endTime()));
      ps.setTime(parameterIndex, Time.valueOf(request.startTime()));

      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    }
  }

  private boolean hasSectionConflict(Connection conn, ScheduleUpsertRequest request, boolean isUpdate) throws SQLException {
    String sql = isUpdate
        ? """
          SELECT 1
          FROM schedules s
          INNER JOIN offerings existing_o ON existing_o.id = s.offering_id
          INNER JOIN offerings target_o ON target_o.id = ?
          WHERE existing_o.section_id = target_o.section_id
            AND existing_o.enrollment_period_id = target_o.enrollment_period_id
            AND s.day = ?
            AND s.id <> ?
            AND s.start_time < ?
            AND s.end_time > ?
          FETCH FIRST 1 ROWS ONLY
          """
        : """
          SELECT 1
          FROM schedules s
          INNER JOIN offerings existing_o ON existing_o.id = s.offering_id
          INNER JOIN offerings target_o ON target_o.id = ?
          WHERE existing_o.section_id = target_o.section_id
            AND existing_o.enrollment_period_id = target_o.enrollment_period_id
            AND s.day = ?
            AND s.start_time < ?
            AND s.end_time > ?
          FETCH FIRST 1 ROWS ONLY
          """;

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, request.offeringId());
      ps.setString(2, request.day().name());

      int parameterIndex = 3;
      if (isUpdate) {
        ps.setLong(parameterIndex++, request.scheduleId());
      }

      ps.setTime(parameterIndex++, Time.valueOf(request.endTime()));
      ps.setTime(parameterIndex, Time.valueOf(request.startTime()));

      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    }
  }

  private List<ScheduleManagementRow> applyConflictFlags(List<ScheduleManagementRow> rows) {
    if (rows.isEmpty()) {
      return rows;
    }

    Map<Long, Boolean> roomConflicts = resolveOwnerConflicts(rows, true);
    Map<Long, Boolean> facultyConflicts = resolveOwnerConflicts(rows, false);
    Map<Long, Boolean> sectionConflicts = resolveSectionConflicts(rows);

    List<ScheduleManagementRow> resolvedRows = new ArrayList<>(rows.size());
    for (ScheduleManagementRow row : rows) {
      resolvedRows.add(row.withConflictFlags(
          roomConflicts.getOrDefault(row.scheduleId(), false),
          facultyConflicts.getOrDefault(row.scheduleId(), false),
          sectionConflicts.getOrDefault(row.scheduleId(), false)
      ));
    }

    return resolvedRows;
  }

  private Map<Long, Boolean> resolveOwnerConflicts(List<ScheduleManagementRow> rows, boolean resolveRoomConflicts) {
    Map<Long, Boolean> conflictsByScheduleId = new HashMap<>();
    Map<String, List<ScheduleManagementRow>> groups = new LinkedHashMap<>();

    for (ScheduleManagementRow row : rows) {
      Long ownerId = resolveRoomConflicts ? row.roomId() : row.facultyId();

      if (
          ownerId == null
              || row.enrollmentPeriodId() == null
              || row.day() == null
              || row.startTime() == null
              || row.endTime() == null
      ) {
        continue;
      }

      String key = row.day() + '#' + row.enrollmentPeriodId() + '#' + ownerId;
      groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
    }

    for (List<ScheduleManagementRow> groupedRows : groups.values()) {
      groupedRows.sort(Comparator.comparing(ScheduleManagementRow::startTime));

      for (int index = 0; index < groupedRows.size(); index++) {
        ScheduleManagementRow current = groupedRows.get(index);

        for (int nextIndex = index + 1; nextIndex < groupedRows.size(); nextIndex++) {
          ScheduleManagementRow candidate = groupedRows.get(nextIndex);

          if (!current.endTime().isAfter(candidate.startTime())) {
            break;
          }

          if (isOverlapping(current.startTime(), current.endTime(), candidate.startTime(), candidate.endTime())) {
            conflictsByScheduleId.put(current.scheduleId(), true);
            conflictsByScheduleId.put(candidate.scheduleId(), true);
          }
        }
      }
    }

    return conflictsByScheduleId;
  }

  private Map<Long, Boolean> resolveSectionConflicts(List<ScheduleManagementRow> rows) {
    Map<Long, Boolean> conflictsByScheduleId = new HashMap<>();
    Map<String, List<ScheduleManagementRow>> groups = new LinkedHashMap<>();

    for (ScheduleManagementRow row : rows) {
      if (
          row.sectionId() == null
              || row.enrollmentPeriodId() == null
              || row.day() == null
              || row.startTime() == null
              || row.endTime() == null
      ) {
        continue;
      }

      String key = row.day() + '#' + row.enrollmentPeriodId() + '#' + row.sectionId();
      groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
    }

    for (List<ScheduleManagementRow> groupedRows : groups.values()) {
      groupedRows.sort(Comparator.comparing(ScheduleManagementRow::startTime));

      for (int index = 0; index < groupedRows.size(); index++) {
        ScheduleManagementRow current = groupedRows.get(index);

        for (int nextIndex = index + 1; nextIndex < groupedRows.size(); nextIndex++) {
          ScheduleManagementRow candidate = groupedRows.get(nextIndex);

          if (!current.endTime().isAfter(candidate.startTime())) {
            break;
          }

          if (isOverlapping(current.startTime(), current.endTime(), candidate.startTime(), candidate.endTime())) {
            conflictsByScheduleId.put(current.scheduleId(), true);
            conflictsByScheduleId.put(candidate.scheduleId(), true);
          }
        }
      }
    }

    return conflictsByScheduleId;
  }

  private boolean offeringExists(Connection conn, Long offeringId) throws SQLException {
    String sql = "SELECT 1 FROM offerings WHERE id = ? FETCH FIRST 1 ROWS ONLY";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, offeringId);

      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    }
  }

  private boolean scheduleExists(Connection conn, Long scheduleId) throws SQLException {
    String sql = "SELECT 1 FROM schedules WHERE id = ? FETCH FIRST 1 ROWS ONLY";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, scheduleId);

      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    }
  }

  private boolean isOverlapping(LocalTime startA, LocalTime endA, LocalTime startB, LocalTime endB) {
    return startA.isBefore(endB) && startB.isBefore(endA);
  }

  private String buildEnrollmentPeriodLabel(Long id, String schoolYear, String semester) {
    String resolvedSchoolYear = safeText(schoolYear, "N/A");
    String resolvedSemester = safeText(semester, "N/A");
    String idLabel = id == null ? "N/A" : String.valueOf(id);

    return resolvedSchoolYear + " | " + resolvedSemester + " | ID " + idLabel;
  }

  private String buildFacultyDisplayName(String firstName, String lastName) {
    String safeLastName = safeText(lastName, "");
    String safeFirstName = safeText(firstName, "");

    if (safeLastName.isBlank() && safeFirstName.isBlank()) {
      return "";
    }

    if (safeLastName.isBlank()) {
      return safeFirstName;
    }

    if (safeFirstName.isBlank()) {
      return safeLastName;
    }

    return safeLastName + ", " + safeFirstName;
  }

  private String safeText(String text, String fallback) {
    if (text == null || text.isBlank()) {
      return fallback;
    }

    return text.trim();
  }

  private LocalTime rsGetLocalTime(ResultSet rs, String column) {
    try {
      Time value = rs.getTime(column);
      return value == null ? null : value.toLocalTime();
    } catch (SQLException e) {
      return null;
    }
  }

  private Long rsGetLong(ResultSet rs, String column) {
    try {
      long value = rs.getLong(column);
      return rs.wasNull() ? null : value;
    } catch (SQLException e) {
      return null;
    }
  }

  private void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
    if (value == null) {
      ps.setNull(index, Types.BIGINT);
      return;
    }

    ps.setLong(index, value);
  }

  private boolean isUniqueConstraintError(SQLException e) {
    return "23505".equals(e.getSQLState());
  }
}