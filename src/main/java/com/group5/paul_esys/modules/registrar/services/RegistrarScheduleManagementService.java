package com.group5.paul_esys.modules.registrar.services;

import com.group5.paul_esys.modules.enums.DayOfWeek;
import com.group5.paul_esys.modules.faculty.model.Faculty;
import com.group5.paul_esys.modules.registrar.model.ScheduleGenerationOfferingCandidate;
import com.group5.paul_esys.modules.registrar.model.ScheduleGenerationPlanRow;
import com.group5.paul_esys.modules.registrar.model.ScheduleGenerationRequest;
import com.group5.paul_esys.modules.registrar.model.ScheduleGenerationResult;
import com.group5.paul_esys.modules.registrar.model.ScheduleGenerationRoomCandidate;
import com.group5.paul_esys.modules.registrar.model.ScheduleGenerationSectionOption;
import com.group5.paul_esys.modules.registrar.model.ScheduleGenerationTemplateBlock;
import com.group5.paul_esys.modules.registrar.model.ScheduleLookupOption;
import com.group5.paul_esys.modules.registrar.model.ScheduleManagementRow;
import com.group5.paul_esys.modules.registrar.model.ScheduleOfferingOption;
import com.group5.paul_esys.modules.registrar.model.ScheduleSaveResult;
import com.group5.paul_esys.modules.registrar.model.ScheduleUpsertRequest;
import com.group5.paul_esys.modules.subjects.model.SubjectSchedulePattern;
import com.group5.paul_esys.modules.users.models.enums.Role;
import com.group5.paul_esys.modules.users.models.user.UserInformation;
import com.group5.paul_esys.modules.users.services.ConnectionService;
import com.group5.paul_esys.modules.users.services.UserSession;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegistrarScheduleManagementService {

  private static final int DEFAULT_ESTIMATED_TIME_MINUTES = 90;
  private static final int DEFAULT_SLOT_MINUTES = 30;
  private static final LocalTime DEFAULT_MIN_START_TIME = LocalTime.of(7, 30);
  private static final LocalTime DEFAULT_MAX_END_TIME = LocalTime.of(18, 0);
  private static final String DEFAULT_SCHEDULE_PATTERN = SubjectSchedulePattern.LECTURE_ONLY.name();
  private static final List<String> GENERATION_DAY_ORDER = List.of("MON", "TUE", "WED", "THU", "FRI", "SAT");
  private static final Set<String> LECTURE_ROOM_TYPES = Set.of("LECTURE", "SEMINAR", "AUDITORIUM", "OTHER");
  private static final Set<String> LAB_ROOM_TYPES = Set.of("LAB");
  private static final Set<String> PE_ROOM_TYPES = Set.of("OTHER", "AUDITORIUM");
  private static final Map<String, String> COMPLEMENTARY_WEEKDAY_MAP = Map.of(
      "MON", "WED",
      "WED", "MON",
      "TUE", "THU",
      "THU", "TUE"
  );

  private static final RegistrarScheduleManagementService INSTANCE = new RegistrarScheduleManagementService();
  private static final Logger logger = LoggerFactory.getLogger(RegistrarScheduleManagementService.class);

  private RegistrarScheduleManagementService() {
  }

  public static RegistrarScheduleManagementService getInstance() {
    return INSTANCE;
  }

  private Long getScopedDepartmentId() {
    UserInformation<?> userInformation = UserSession.getInstance().getUserInformation();
    if (userInformation == null || userInformation.getRole() != Role.FACULTY) {
      return null;
    }

    Object user = userInformation.getUser();
    if (user instanceof Faculty faculty && faculty.isDepartmentHead()) {
      return faculty.getDepartmentId();
    }

    return null;
  }

  public List<ScheduleManagementRow> getScheduleRows() {
    Long filterDepartmentId = getScopedDepartmentId();
    StringBuilder sqlBuilder = new StringBuilder("""
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
        """);

    if (filterDepartmentId != null) {
      sqlBuilder.append(" WHERE sub.department_id = ? ");
    }

    sqlBuilder.append("""
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
        """);

    List<ScheduleManagementRow> rows = new ArrayList<>();
    try (
        Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement(sqlBuilder.toString())
    ) {
      if (filterDepartmentId != null) {
        ps.setLong(1, filterDepartmentId);
      }

      try (ResultSet rs = ps.executeQuery()) {
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
              false,
              false
          ));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: {}", e.getMessage(), e);
      return List.of();
    }

    return applyConflictFlags(rows);
  }

  public List<ScheduleLookupOption> getEnrollmentPeriodOptions() {
    Long filterDepartmentId = getScopedDepartmentId();
    StringBuilder sqlBuilder = new StringBuilder("""
        SELECT DISTINCT
          ep.id,
          ep.school_year,
          ep.semester,
          ep.created_at
        FROM offerings o
        INNER JOIN enrollment_period ep ON ep.id = o.enrollment_period_id
        """);

    if (filterDepartmentId != null) {
      sqlBuilder.append("""
        INNER JOIN subjects sub ON sub.id = o.subject_id
        WHERE sub.department_id = ?
        """);
    }

    sqlBuilder.append("""
        ORDER BY ep.created_at DESC, ep.id DESC
        """);

    List<ScheduleLookupOption> options = new ArrayList<>();
    try (
        Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement(sqlBuilder.toString())
    ) {
      if (filterDepartmentId != null) {
        ps.setLong(1, filterDepartmentId);
      }

      try (ResultSet rs = ps.executeQuery()) {
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
      }
    } catch (SQLException e) {
      logger.error("ERROR: {}", e.getMessage(), e);
      return List.of();
    }

    return options;
  }

  public List<ScheduleOfferingOption> getOfferingOptions() {
    Long filterDepartmentId = getScopedDepartmentId();

    StringBuilder sqlBuilder = new StringBuilder("""
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
        """);
    
    if (filterDepartmentId != null) {
        sqlBuilder.append(" WHERE sub.department_id = ? ");
    }

    sqlBuilder.append("""
        ORDER BY
          ep.created_at DESC,
          sec.section_code,
          sub.subject_code,
          o.id
        """);

    List<ScheduleOfferingOption> options = new ArrayList<>();
    Map<Long, String> prerequisiteLabelBySubjectId = new HashMap<>();
    try (
        Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement(sqlBuilder.toString())
    ) {
      if (filterDepartmentId != null) {
          ps.setLong(1, filterDepartmentId);
      }
      
      try (ResultSet rs = ps.executeQuery()) {
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
    Long filterDepartmentId = getScopedDepartmentId();
    StringBuilder sqlBuilder = new StringBuilder("SELECT id, first_name, last_name FROM faculty");
    if (filterDepartmentId != null) {
      sqlBuilder.append(" WHERE department_id = ?");
    }
    sqlBuilder.append(" ORDER BY last_name, first_name");

    List<ScheduleLookupOption> options = new ArrayList<>();

    try (
        Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement(sqlBuilder.toString())
    ) {
      if (filterDepartmentId != null) {
        ps.setLong(1, filterDepartmentId);
      }

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          options.add(new ScheduleLookupOption(
              rsGetLong(rs, "id"),
              safeText(
                  buildFacultyDisplayName(rs.getString("first_name"), rs.getString("last_name")),
                  "N/A"
              )
          ));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: {}", e.getMessage(), e);
      return List.of();
    }

    return options;
  }

  public List<ScheduleGenerationSectionOption> getSectionGenerationOptions() {
    Long filterDepartmentId = getScopedDepartmentId();
    StringBuilder sqlBuilder = new StringBuilder("""
        SELECT
          sec.id AS section_id,
          sec.section_code,
          ep.id AS enrollment_period_id,
          ep.school_year,
          ep.semester,
          COALESCE(MAX(o.capacity), sec.capacity) AS effective_capacity,
          ep.created_at
        FROM offerings o
        INNER JOIN sections sec ON sec.id = o.section_id
        INNER JOIN enrollment_period ep ON ep.id = o.enrollment_period_id
        """);

    if (filterDepartmentId != null) {
      sqlBuilder.append("""
        INNER JOIN subjects sub ON sub.id = o.subject_id
        WHERE sub.department_id = ?
        """);
    }

    sqlBuilder.append("""
        GROUP BY
          sec.id,
          sec.section_code,
          ep.id,
          ep.school_year,
          ep.semester,
          sec.capacity,
          ep.created_at
        ORDER BY
          ep.created_at DESC,
          sec.section_code,
          sec.id
        """);

    List<ScheduleGenerationSectionOption> options = new ArrayList<>();
    try (
        Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement(sqlBuilder.toString())
    ) {
      if (filterDepartmentId != null) {
        ps.setLong(1, filterDepartmentId);
      }

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Long enrollmentPeriodId = rsGetLong(rs, "enrollment_period_id");
          options.add(new ScheduleGenerationSectionOption(
              rsGetLong(rs, "section_id"),
              safeText(rs.getString("section_code"), "N/A"),
              enrollmentPeriodId,
              buildEnrollmentPeriodLabel(
                  enrollmentPeriodId,
                  rs.getString("school_year"),
                  rs.getString("semester")
              ),
              rs.getObject("effective_capacity", Integer.class)
          ));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: {}", e.getMessage(), e);
      return List.of();
    }

    return options;
  }

  public ScheduleGenerationResult previewSectionScheduleGeneration(ScheduleGenerationRequest request) {
    return buildSectionScheduleGenerationResult(request, false);
  }

  public ScheduleGenerationResult generateSectionSchedules(ScheduleGenerationRequest request) {
    return buildSectionScheduleGenerationResult(request, true);
  }

  private ScheduleGenerationResult buildSectionScheduleGenerationResult(
      ScheduleGenerationRequest request,
      boolean persist
  ) {
    String validationError = validateGenerationRequest(request);
    if (validationError != null) {
      return new ScheduleGenerationResult(false, 0, 0, 0, 0, List.of(), validationError);
    }

    LocalTime minStartTime = normalizeMinStartTime(request.minStartTime());
    LocalTime maxEndTime = normalizeMaxEndTime(request.maxEndTime());
    int slotMinutes = normalizeSlotMinutes(request.slotMinutes());

    Connection conn = null;
    try {
      conn = ConnectionService.getConnection();

      List<ScheduleGenerationOfferingCandidate> candidates = loadOfferingCandidates(
          conn,
          request.sectionId(),
          request.enrollmentPeriodId()
      );

      if (candidates.isEmpty()) {
        return new ScheduleGenerationResult(
            true,
            0,
            0,
            0,
            0,
            List.of(),
            "No offerings found for the selected section and enrollment period."
        );
      }

      List<ScheduleGenerationRoomCandidate> roomCandidates = loadRoomCandidates(conn);
      List<ScheduleGenerationPlanRow> planRows = new ArrayList<>();
      List<ScheduleGenerationPlanRow> plannedRows = new ArrayList<>();

      for (ScheduleGenerationOfferingCandidate candidate : candidates) {
        String schedulePattern = normalizeSchedulePattern(candidate.schedulePattern());

        if (hasExistingScheduleForOffering(conn, candidate.offeringId())) {
          planRows.add(new ScheduleGenerationPlanRow(
              candidate.offeringId(),
              candidate.subjectCode(),
              candidate.subjectName(),
              schedulePattern,
              "All Blocks",
              normalizeEstimatedMinutes(candidate.estimatedMinutes()),
              null,
              null,
              null,
              null,
              null,
              "EXISTING",
              "Skipped because this offering already has at least one schedule row."
          ));
          continue;
        }

        List<ScheduleGenerationTemplateBlock> templateBlocks = resolveTemplateBlocks(candidate);
        boolean placementFailed = false;

        for (int blockIndex = 0; blockIndex < templateBlocks.size(); blockIndex++) {
          ScheduleGenerationTemplateBlock block = templateBlocks.get(blockIndex);

          if (maxEndTime.minusMinutes(block.minutes()).isBefore(minStartTime)) {
            planRows.add(new ScheduleGenerationPlanRow(
                candidate.offeringId(),
                candidate.subjectCode(),
                candidate.subjectName(),
                schedulePattern,
                block.blockLabel(),
                block.minutes(),
                null,
                null,
                null,
                null,
                null,
                "SKIPPED",
                "Skipped because block duration exceeds the selected time window."
            ));
            placementFailed = true;
          } else if (!hasCompatibleRoomForBlock(candidate, block, roomCandidates)) {
            planRows.add(new ScheduleGenerationPlanRow(
                candidate.offeringId(),
                candidate.subjectCode(),
                candidate.subjectName(),
                schedulePattern,
                block.blockLabel(),
                block.minutes(),
                null,
                null,
                null,
                null,
                null,
                "SKIPPED",
                "Skipped because no available room matches required capacity and room type."
            ));
            placementFailed = true;
          } else {
            ScheduleGenerationPlanRow placement = findPlacement(
                conn,
                candidate,
                block,
                roomCandidates,
                plannedRows,
                minStartTime,
                maxEndTime,
                slotMinutes
            );

            if (placement == null) {
              planRows.add(new ScheduleGenerationPlanRow(
                  candidate.offeringId(),
                  candidate.subjectCode(),
                  candidate.subjectName(),
                  schedulePattern,
                  block.blockLabel(),
                  block.minutes(),
                  null,
                  null,
                  null,
                  null,
                  null,
                  "SKIPPED",
                  "Skipped because no conflict-free slot was found within the selected window."
              ));
              placementFailed = true;
            } else {
              planRows.add(placement);
              plannedRows.add(placement);
            }
          }

          if (placementFailed) {
            for (int remainingIndex = blockIndex + 1; remainingIndex < templateBlocks.size(); remainingIndex++) {
              ScheduleGenerationTemplateBlock remainingBlock = templateBlocks.get(remainingIndex);
              planRows.add(new ScheduleGenerationPlanRow(
                  candidate.offeringId(),
                  candidate.subjectCode(),
                  candidate.subjectName(),
                  schedulePattern,
                  remainingBlock.blockLabel(),
                  remainingBlock.minutes(),
                  null,
                  null,
                  null,
                  null,
                  null,
                  "SKIPPED",
                  "Skipped because a required block for this pattern could not be placed."
              ));
            }
            break;
          }
        }
      }

      int readyCount = (int) planRows.stream().filter(row -> "READY".equals(row.status())).count();
      int skippedCount = planRows.size() - readyCount;

      if (!persist) {
        return new ScheduleGenerationResult(
            true,
            planRows.size(),
            readyCount,
            0,
            skippedCount,
            List.copyOf(planRows),
            "Preview complete. " + readyCount + " offering(s) ready and " + skippedCount + " skipped."
        );
      }

      conn.setAutoCommit(false);
      String insertSql = "INSERT INTO schedules (offering_id, room_id, faculty_id, day, start_time, end_time) VALUES (?, ?, ?, ?, ?, ?)";
      int createdCount = 0;
      List<ScheduleGenerationPlanRow> persistedRows = new ArrayList<>(planRows.size());

      try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
        for (ScheduleGenerationPlanRow row : planRows) {
          if (!"READY".equals(row.status())) {
            persistedRows.add(row);
            continue;
          }

          ScheduleUpsertRequest upsertRequest = new ScheduleUpsertRequest(
              null,
              row.offeringId(),
              row.roomId(),
              null,
              DayOfWeek.valueOf(row.day()),
              row.startTime(),
              row.endTime()
          );

          ScheduleSaveResult conflictValidation = validateConflicts(conn, upsertRequest, false);
          if (!conflictValidation.successful()) {
            persistedRows.add(withPlanStatus(row, "FAILED", conflictValidation.message()));
            continue;
          }

          ps.setLong(1, row.offeringId());
          setNullableLong(ps, 2, row.roomId());
          ps.setNull(3, Types.BIGINT);
          ps.setString(4, row.day());
          ps.setTime(5, Time.valueOf(row.startTime()));
          ps.setTime(6, Time.valueOf(row.endTime()));

          try {
            int affected = ps.executeUpdate();
            if (affected > 0) {
              createdCount++;
              persistedRows.add(withPlanStatus(row, "CREATED", "Schedule generated successfully."));
            } else {
              persistedRows.add(withPlanStatus(row, "FAILED", "No row was inserted for this offering."));
            }
          } catch (SQLException e) {
            String message = isUniqueConstraintError(e)
                ? "Skipped because a conflicting schedule now exists for the selected slot."
                : "Failed to insert generated schedule.";
            persistedRows.add(withPlanStatus(row, "FAILED", message));
          }
        }
      }

      conn.commit();
      int persistedSkippedCount = persistedRows.size() - createdCount;
      return new ScheduleGenerationResult(
          true,
          persistedRows.size(),
          readyCount,
          createdCount,
          persistedSkippedCount,
          List.copyOf(persistedRows),
          "Generation completed. " + createdCount + " schedule(s) created."
      );
    } catch (SQLException e) {
      if (conn != null) {
        try {
          if (!conn.isClosed()) {
            conn.rollback();
          }
        } catch (SQLException rollbackException) {
          logger.error("ERROR: {}", rollbackException.getMessage(), rollbackException);
        }
      }

      logger.error("ERROR: {}", e.getMessage(), e);
      return new ScheduleGenerationResult(
          false,
          0,
          0,
          0,
          0,
          List.of(),
          "Failed to generate schedules. Please try again."
      );
    } finally {
      if (conn != null) {
        try {
          conn.setAutoCommit(true);
          conn.close();
        } catch (SQLException closeException) {
          logger.error("ERROR: {}", closeException.getMessage(), closeException);
        }
      }
    }
  }

  private ScheduleGenerationPlanRow findPlacement(
      Connection conn,
      ScheduleGenerationOfferingCandidate candidate,
      ScheduleGenerationTemplateBlock block,
      List<ScheduleGenerationRoomCandidate> roomCandidates,
      List<ScheduleGenerationPlanRow> plannedRows,
      LocalTime minStartTime,
      LocalTime maxEndTime,
      int slotMinutes
  ) throws SQLException {
    int estimatedMinutes = block.minutes();
    LocalTime latestStartTime = maxEndTime.minusMinutes(estimatedMinutes);

    for (String dayCode : resolveGenerationDayOrder(candidate, block, plannedRows)) {
      LocalTime startTime = minStartTime;
      while (!startTime.isAfter(latestStartTime)) {
        LocalTime endTime = startTime.plusMinutes(estimatedMinutes);

        for (ScheduleGenerationRoomCandidate room : roomCandidates) {
          if (!isRoomCompatible(candidate, block, room)) {
            continue;
          }

          if (block.requiresDifferentDayFromOffering() && isOfferingDayAlreadyPlanned(plannedRows, candidate.offeringId(), dayCode)) {
            continue;
          }

          if (isPlannedSectionConflict(plannedRows, dayCode, startTime, endTime)) {
            continue;
          }

          if (isPlannedRoomConflict(plannedRows, room.roomId(), dayCode, startTime, endTime)) {
            continue;
          }

          ScheduleUpsertRequest upsertRequest = new ScheduleUpsertRequest(
              null,
              candidate.offeringId(),
              room.roomId(),
              null,
              DayOfWeek.valueOf(dayCode),
              startTime,
              endTime
          );

          if (hasSectionConflict(conn, upsertRequest, false)) {
            continue;
          }

          if (hasRoomConflict(conn, upsertRequest, false)) {
            continue;
          }

          return new ScheduleGenerationPlanRow(
              candidate.offeringId(),
              candidate.subjectCode(),
              candidate.subjectName(),
              normalizeSchedulePattern(candidate.schedulePattern()),
              block.blockLabel(),
              estimatedMinutes,
              dayCode,
              startTime,
              endTime,
              room.roomId(),
              room.roomLabel(),
              "READY",
              "Ready to generate."
          );
        }

        startTime = startTime.plusMinutes(slotMinutes);
      }
    }

    return null;
  }

  private List<ScheduleGenerationOfferingCandidate> loadOfferingCandidates(
      Connection conn,
      Long sectionId,
      Long enrollmentPeriodId
  ) throws SQLException {
    Long filterDepartmentId = getScopedDepartmentId();
    String estimatedMinutesExpression = hasSubjectEstimatedTimeColumn(conn)
        ? "COALESCE(sub.estimated_time, " + DEFAULT_ESTIMATED_TIME_MINUTES + ")"
        : String.valueOf(DEFAULT_ESTIMATED_TIME_MINUTES);

    String schedulePatternExpression = hasSubjectSchedulePatternColumn(conn)
        ? "COALESCE(sub.schedule_pattern, '" + DEFAULT_SCHEDULE_PATTERN + "')"
        : "'" + DEFAULT_SCHEDULE_PATTERN + "'";

    StringBuilder sqlBuilder = new StringBuilder("""
        SELECT
          o.id AS offering_id,
          sub.subject_code,
          sub.subject_name,
          %s AS schedule_pattern,
          %s AS estimated_minutes,
          COALESCE(o.capacity, sec.capacity) AS required_capacity
        FROM offerings o
        INNER JOIN subjects sub ON sub.id = o.subject_id
        INNER JOIN sections sec ON sec.id = o.section_id
        WHERE o.section_id = ?
          AND o.enrollment_period_id = ?
        """);

    if (filterDepartmentId != null) {
      sqlBuilder.append(" AND sub.department_id = ? ");
    }

    sqlBuilder.append("""
        ORDER BY sub.subject_code, o.id
        """);

    String sql = sqlBuilder.toString().formatted(schedulePatternExpression, estimatedMinutesExpression);

    List<ScheduleGenerationOfferingCandidate> candidates = new ArrayList<>();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, sectionId);
      ps.setLong(2, enrollmentPeriodId);
      if (filterDepartmentId != null) {
        ps.setLong(3, filterDepartmentId);
      }

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          candidates.add(new ScheduleGenerationOfferingCandidate(
              rsGetLong(rs, "offering_id"),
              safeText(rs.getString("subject_code"), "N/A"),
              safeText(rs.getString("subject_name"), "N/A"),
              normalizeSchedulePattern(rs.getString("schedule_pattern")),
              rs.getObject("estimated_minutes", Integer.class),
              rs.getObject("required_capacity", Integer.class)
          ));
        }
      }
    }

    return candidates;
  }

  private List<ScheduleGenerationRoomCandidate> loadRoomCandidates(Connection conn) throws SQLException {
    String sql = "SELECT id, building, room, room_type, status, capacity FROM rooms ORDER BY building, room";
    List<ScheduleGenerationRoomCandidate> rooms = new ArrayList<>();

    try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        String status = safeText(rs.getString("status"), "AVAILABLE").toUpperCase();
        if (!"AVAILABLE".equals(status)) {
          continue;
        }

        String building = safeText(rs.getString("building"), "N/A");
        String roomName = safeText(rs.getString("room"), "N/A");
        String roomLabel = building + " - " + roomName;
        String roomType = safeText(rs.getString("room_type"), "OTHER").toUpperCase();

        rooms.add(new ScheduleGenerationRoomCandidate(
            rsGetLong(rs, "id"),
            roomLabel,
            roomType,
            rs.getObject("capacity", Integer.class)
        ));
      }
    }

    return rooms;
  }

  private boolean hasExistingScheduleForOffering(Connection conn, Long offeringId) throws SQLException {
    String sql = "SELECT 1 FROM schedules WHERE offering_id = ? FETCH FIRST 1 ROWS ONLY";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, offeringId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    }
  }

  private boolean hasCompatibleRoomForBlock(
      ScheduleGenerationOfferingCandidate candidate,
      ScheduleGenerationTemplateBlock block,
      List<ScheduleGenerationRoomCandidate> roomCandidates
  ) {
    for (ScheduleGenerationRoomCandidate room : roomCandidates) {
      if (isRoomCompatible(candidate, block, room)) {
        return true;
      }
    }

    return false;
  }

  private boolean isRoomCompatible(
      ScheduleGenerationOfferingCandidate candidate,
      ScheduleGenerationTemplateBlock block,
      ScheduleGenerationRoomCandidate room
  ) {
    Set<String> allowedRoomTypes = resolveAllowedRoomTypes(candidate, block);
    if (!allowedRoomTypes.contains(safeText(room.roomType(), "OTHER").toUpperCase())) {
      return false;
    }

    Integer requiredCapacity = candidate.requiredCapacity();
    Integer roomCapacity = room.capacity();
    if (requiredCapacity == null || requiredCapacity <= 0 || roomCapacity == null) {
      return true;
    }

    return roomCapacity >= requiredCapacity;
  }

  private Set<String> resolveAllowedRoomTypes(
      ScheduleGenerationOfferingCandidate candidate,
      ScheduleGenerationTemplateBlock block
  ) {
    if (block.allowedRoomTypes() != null && !block.allowedRoomTypes().isEmpty()) {
      return block.allowedRoomTypes();
    }

    SubjectSchedulePattern schedulePattern = SubjectSchedulePattern.fromValue(candidate.schedulePattern());
    String blockCode = safeText(block.blockCode(), "").toUpperCase();

    if (schedulePattern == SubjectSchedulePattern.LECTURE_LAB && "LAB".equals(blockCode)) {
      return LAB_ROOM_TYPES;
    }

    if (schedulePattern == SubjectSchedulePattern.PE_PAIRED) {
      return PE_ROOM_TYPES;
    }

    return resolveHeuristicAllowedRoomTypes(candidate.subjectCode(), candidate.subjectName());
  }

  private Set<String> resolveHeuristicAllowedRoomTypes(String subjectCode, String subjectName) {
    String descriptor = (safeText(subjectCode, "") + " " + safeText(subjectName, "")).toUpperCase();

    if (
        descriptor.contains("LAB")
            || descriptor.contains("PRACTICUM")
            || descriptor.contains("WORKSHOP")
    ) {
      return LAB_ROOM_TYPES;
    }

    if (descriptor.contains("SEMINAR")) {
      return LECTURE_ROOM_TYPES;
    }

    return LECTURE_ROOM_TYPES;
  }

  private List<ScheduleGenerationTemplateBlock> resolveTemplateBlocks(ScheduleGenerationOfferingCandidate candidate) {
    SubjectSchedulePattern schedulePattern = SubjectSchedulePattern.fromValue(candidate.schedulePattern());
    int fallbackMinutes = normalizeEstimatedMinutes(candidate.estimatedMinutes());

    return switch (schedulePattern) {
      case LECTURE_LAB -> List.of(
          new ScheduleGenerationTemplateBlock(
              "LECTURE",
              "Lecture",
              120,
              List.of("MON", "TUE", "WED", "THU", "FRI"),
              LECTURE_ROOM_TYPES,
              false
          ),
          new ScheduleGenerationTemplateBlock(
              "LAB",
              "Laboratory",
              180,
              List.of("TUE", "THU", "WED", "MON", "FRI", "SAT"),
              LAB_ROOM_TYPES,
              true
          )
      );
      case GE_PAIRED -> List.of(
          new ScheduleGenerationTemplateBlock(
              "SESSION_1",
              "Session 1",
              90,
              List.of("MON", "TUE", "WED", "THU", "FRI"),
              LECTURE_ROOM_TYPES,
              false
          ),
          new ScheduleGenerationTemplateBlock(
              "SESSION_2",
              "Session 2",
              90,
              List.of("WED", "THU", "MON", "TUE", "FRI"),
              LECTURE_ROOM_TYPES,
              true
          )
      );
      case PE_PAIRED -> List.of(
          new ScheduleGenerationTemplateBlock(
              "SESSION_1",
              "Session 1",
              60,
              List.of("TUE", "THU", "MON", "WED", "FRI"),
              PE_ROOM_TYPES,
              false
          ),
          new ScheduleGenerationTemplateBlock(
              "SESSION_2",
              "Session 2",
              60,
              List.of("THU", "TUE", "WED", "MON", "FRI"),
              PE_ROOM_TYPES,
              true
          )
      );
      case NSTP_BLOCK -> List.of(
          new ScheduleGenerationTemplateBlock(
              "MAIN",
              "Main Session",
              Math.max(180, fallbackMinutes),
              List.of("SAT"),
              LECTURE_ROOM_TYPES,
              false
          )
      );
      case LECTURE_ONLY -> List.of(
          new ScheduleGenerationTemplateBlock(
              "MAIN",
              "Main Session",
              fallbackMinutes,
              List.of("MON", "TUE", "WED", "THU", "FRI", "SAT"),
              LECTURE_ROOM_TYPES,
              false
          )
      );
    };
  }

  private List<String> resolveGenerationDayOrder(
      ScheduleGenerationOfferingCandidate candidate,
      ScheduleGenerationTemplateBlock block,
      List<ScheduleGenerationPlanRow> plannedRows
  ) {
    SubjectSchedulePattern schedulePattern = SubjectSchedulePattern.fromValue(candidate.schedulePattern());
    if (schedulePattern == SubjectSchedulePattern.NSTP_BLOCK) {
      return List.of("SAT");
    }

    LinkedHashSet<String> dayOrder = new LinkedHashSet<>();
    String plannedDay = findPrimaryPlannedDayForOffering(plannedRows, candidate.offeringId());
    String blockCode = safeText(block.blockCode(), "").toUpperCase();

    boolean isFollowupBlock = "LAB".equals(blockCode) || "SESSION_2".equals(blockCode);
    if (plannedDay != null && isFollowupBlock) {
      String complementaryDay = COMPLEMENTARY_WEEKDAY_MAP.get(plannedDay);
      if (complementaryDay != null) {
        dayOrder.add(complementaryDay);
      }
    }

    if (block.preferredDays() != null) {
      dayOrder.addAll(block.preferredDays());
    }

    dayOrder.addAll(GENERATION_DAY_ORDER);
    return List.copyOf(dayOrder);
  }

  private String findPrimaryPlannedDayForOffering(List<ScheduleGenerationPlanRow> plannedRows, Long offeringId) {
    for (ScheduleGenerationPlanRow row : plannedRows) {
      if (offeringId.equals(row.offeringId()) && row.day() != null) {
        return row.day();
      }
    }

    return null;
  }

  private boolean isOfferingDayAlreadyPlanned(List<ScheduleGenerationPlanRow> plannedRows, Long offeringId, String day) {
    for (ScheduleGenerationPlanRow row : plannedRows) {
      if (offeringId.equals(row.offeringId()) && day.equals(row.day())) {
        return true;
      }
    }

    return false;
  }

  private String normalizeSchedulePattern(String schedulePattern) {
    return SubjectSchedulePattern.fromValue(schedulePattern).name();
  }

  private boolean isPlannedSectionConflict(
      List<ScheduleGenerationPlanRow> plannedRows,
      String day,
      LocalTime start,
      LocalTime end
  ) {
    for (ScheduleGenerationPlanRow row : plannedRows) {
      if (row.day() == null || !day.equals(row.day())) {
        continue;
      }

      if (row.startTime() == null || row.endTime() == null) {
        continue;
      }

      if (isOverlapping(start, end, row.startTime(), row.endTime())) {
        return true;
      }
    }

    return false;
  }

  private boolean isPlannedRoomConflict(
      List<ScheduleGenerationPlanRow> plannedRows,
      Long roomId,
      String day,
      LocalTime start,
      LocalTime end
  ) {
    if (roomId == null) {
      return false;
    }

    for (ScheduleGenerationPlanRow row : plannedRows) {
      if (row.roomId() == null || !roomId.equals(row.roomId()) || row.day() == null || !day.equals(row.day())) {
        continue;
      }

      if (row.startTime() == null || row.endTime() == null) {
        continue;
      }

      if (isOverlapping(start, end, row.startTime(), row.endTime())) {
        return true;
      }
    }

    return false;
  }

  private ScheduleGenerationPlanRow withPlanStatus(ScheduleGenerationPlanRow row, String status, String details) {
    return new ScheduleGenerationPlanRow(
        row.offeringId(),
        row.subjectCode(),
        row.subjectName(),
        row.schedulePattern(),
        row.blockLabel(),
        row.estimatedMinutes(),
        row.day(),
        row.startTime(),
        row.endTime(),
        row.roomId(),
        row.roomLabel(),
        status,
        details
    );
  }

  private String validateGenerationRequest(ScheduleGenerationRequest request) {
    if (request == null) {
      return "Schedule generation request is required.";
    }

    if (request.sectionId() == null) {
      return "Section selection is required.";
    }

    if (request.enrollmentPeriodId() == null) {
      return "Enrollment period selection is required.";
    }

    LocalTime minStartTime = normalizeMinStartTime(request.minStartTime());
    LocalTime maxEndTime = normalizeMaxEndTime(request.maxEndTime());
    if (!minStartTime.isBefore(maxEndTime)) {
      return "Minimum start time must be earlier than maximum end time.";
    }

    return null;
  }

  private int normalizeEstimatedMinutes(Integer minutes) {
    if (minutes == null || minutes <= 0) {
      return DEFAULT_ESTIMATED_TIME_MINUTES;
    }

    return minutes;
  }

  private int normalizeSlotMinutes(Integer slotMinutes) {
    if (slotMinutes == null || slotMinutes <= 0) {
      return DEFAULT_SLOT_MINUTES;
    }

    return slotMinutes;
  }

  private LocalTime normalizeMinStartTime(LocalTime minStartTime) {
    return minStartTime == null ? DEFAULT_MIN_START_TIME : minStartTime;
  }

  private LocalTime normalizeMaxEndTime(LocalTime maxEndTime) {
    return maxEndTime == null ? DEFAULT_MAX_END_TIME : maxEndTime;
  }

  private boolean hasSubjectEstimatedTimeColumn(Connection conn) {
    try {
      try (ResultSet rs = conn.getMetaData().getColumns(null, null, "SUBJECTS", "ESTIMATED_TIME")) {
        if (rs.next()) {
          return true;
        }
      }

      try (ResultSet rs = conn.getMetaData().getColumns(null, null, "subjects", "estimated_time")) {
        return rs.next();
      }
    } catch (SQLException e) {
      logger.error("ERROR: {}", e.getMessage(), e);
      return false;
    }
  }

  private boolean hasSubjectSchedulePatternColumn(Connection conn) {
    try {
      try (ResultSet rs = conn.getMetaData().getColumns(null, null, "SUBJECTS", "SCHEDULE_PATTERN")) {
        if (rs.next()) {
          return true;
        }
      }

      try (ResultSet rs = conn.getMetaData().getColumns(null, null, "subjects", "schedule_pattern")) {
        return rs.next();
      }
    } catch (SQLException e) {
      logger.error("ERROR: {}", e.getMessage(), e);
      return false;
    }
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

  public ScheduleSaveResult deleteSchedules(List<Long> scheduleIds) {
    if (scheduleIds == null || scheduleIds.isEmpty()) {
      return new ScheduleSaveResult(false, "Please select at least one schedule to delete.");
    }

    String sql = "DELETE FROM schedules WHERE id = ?";
    int deletedCount = 0;

    try (
        Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)
    ) {
      conn.setAutoCommit(false);
      try {
        for (Long id : scheduleIds) {
          ps.setLong(1, id);
          int result = ps.executeUpdate();
          if (result > 0) {
            deletedCount++;
            logger.info("AUDIT: Deleted schedule ID: {}", id);
          }
        }
        conn.commit();
        return new ScheduleSaveResult(true, deletedCount + " schedule(s) deleted successfully.");
      } catch (SQLException e) {
        conn.rollback();
        logger.error("ERROR during batch delete: {}", e.getMessage(), e);
        return new ScheduleSaveResult(false, "Failed to delete schedules: " + e.getMessage());
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      logger.error("ERROR establishing connection for batch delete: {}", e.getMessage(), e);
      return new ScheduleSaveResult(false, "Database connection error.");
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

    if (isTBATime(request.startTime())) {
      return new ScheduleSaveResult(false, "Start time cannot be TBA (00:00). Please set a specific start time.");
    }

    if (isTBATime(request.endTime())) {
      return new ScheduleSaveResult(false, "End time cannot be TBA (00:00). Please set a specific end time.");
    }

    if (!request.startTime().isBefore(request.endTime())) {
      return new ScheduleSaveResult(false, "Start time must be earlier than end time.");
    }

    return new ScheduleSaveResult(true, "OK");
  }

  private boolean isTBATime(LocalTime time) {
    return time != null && time.getHour() == 0 && time.getMinute() == 0;
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