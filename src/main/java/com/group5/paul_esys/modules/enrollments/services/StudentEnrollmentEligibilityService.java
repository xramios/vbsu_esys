package com.group5.paul_esys.modules.enrollments.services;

import com.group5.paul_esys.modules.users.services.ConnectionService;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StudentEnrollmentEligibilityService {

  private static final StudentEnrollmentEligibilityService INSTANCE = new StudentEnrollmentEligibilityService();
  private static final Logger logger = LoggerFactory.getLogger(StudentEnrollmentEligibilityService.class);

  private StudentEnrollmentEligibilityService() {
  }

  public static StudentEnrollmentEligibilityService getInstance() {
    return INSTANCE;
  }

  public Set<Long> getEligibleSemesterSubjectIds(String studentId, String enrollmentSemesterLabel, Long enrollmentYearLevel) {
    if (studentId == null || studentId.isBlank()) {
      return Collections.emptySet();
    }

    try (Connection conn = ConnectionService.getConnection()) {
      Optional<Long> curriculumIdOpt = resolveCurriculumId(conn, studentId);
      if (curriculumIdOpt.isEmpty()) {
        return Collections.emptySet();
      }

      Map<Long, String> semesterLabelById = new HashMap<>();
      Map<Long, Integer> yearLevelBySemesterId = new HashMap<>();
      List<Long> orderedSemesterIds = getOrderedSemesterIdsByCurriculum(conn, curriculumIdOpt.get(), semesterLabelById, yearLevelBySemesterId);
      if (orderedSemesterIds.isEmpty()) {
        return Collections.emptySet();
      }

      Map<Long, Long> requiredCountBySemester = new HashMap<>();
      Map<Long, Long> completedCountBySemester = new HashMap<>();
      Map<Long, Long> activityCountBySemester = new HashMap<>();
      Map<Long, Long> activeLoadCountBySemester = new HashMap<>();

      for (Long semesterId : orderedSemesterIds) {
        long requiredCount = countRequiredSubjects(conn, semesterId);
        long completedCount = countCompletedSubjects(conn, studentId, semesterId);
        long activityCount = countTrackedSubjectActivity(conn, studentId, semesterId)
            + countSelectedEnrollmentActivity(conn, studentId, semesterId);
        long activeLoadCount = countCompletedOrEnrolledLoadSubjects(conn, studentId, semesterId);

        requiredCountBySemester.put(semesterId, requiredCount);
        completedCountBySemester.put(semesterId, completedCount);
        activityCountBySemester.put(semesterId, activityCount);
        activeLoadCountBySemester.put(semesterId, activeLoadCount);
      }

      Integer currentSemesterIndex = findCurrentSemesterIndex(
          orderedSemesterIds,
          semesterLabelById,
          yearLevelBySemesterId,
          enrollmentSemesterLabel,
          enrollmentYearLevel
      );

      if (currentSemesterIndex == null) {
        currentSemesterIndex = findHighestActiveSemesterIndex(
            orderedSemesterIds,
            completedCountBySemester,
            activityCountBySemester
        );
      }

      if (currentSemesterIndex == null) {
        currentSemesterIndex = 0;
      }

      LinkedHashSet<Long> allowedSemesterIds = new LinkedHashSet<>();
      for (int index = 0; index <= currentSemesterIndex && index < orderedSemesterIds.size(); index++) {
        Long semesterId = orderedSemesterIds.get(index);
        long requiredCount = requiredCountBySemester.getOrDefault(semesterId, 0L);
        long completedCount = completedCountBySemester.getOrDefault(semesterId, 0L);

        boolean isCurrentSemester = index == currentSemesterIndex;
        boolean hasBacktrackLoad = completedCount < requiredCount;

        if (isCurrentSemester || hasBacktrackLoad) {
          allowedSemesterIds.add(semesterId);
        }
      }

      // Unlock the immediate next semester in the same year level when the current semester is completed.
      addNextSemesterUnlock(
          allowedSemesterIds,
          orderedSemesterIds,
          yearLevelBySemesterId,
          activeLoadCountBySemester,
          completedCountBySemester,
          currentSemesterIndex
      );

      if (allowedSemesterIds.isEmpty()) {
        allowedSemesterIds.add(orderedSemesterIds.get(currentSemesterIndex));
      }

      Set<Long> semesterSubjectIds = getSemesterSubjectIds(conn, allowedSemesterIds);
      return filterSemesterSubjectsByPrerequisites(conn, studentId, semesterSubjectIds);
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return Collections.emptySet();
    }
  }

  public float getCurrentSemesterUnitLimit(String studentId, String enrollmentSemesterLabel, Long enrollmentYearLevel) {
    if (studentId == null || studentId.isBlank()) {
      return 0.0f;
    }

    try (Connection conn = ConnectionService.getConnection()) {
      Optional<Long> curriculumIdOpt = resolveCurriculumId(conn, studentId);
      if (curriculumIdOpt.isEmpty()) {
        return 0.0f;
      }

      Map<Long, String> semesterLabelById = new HashMap<>();
      Map<Long, Integer> yearLevelBySemesterId = new HashMap<>();
      List<Long> orderedSemesterIds = getOrderedSemesterIdsByCurriculum(conn, curriculumIdOpt.get(), semesterLabelById, yearLevelBySemesterId);
      if (orderedSemesterIds.isEmpty()) {
        return 0.0f;
      }

      Integer currentSemesterIndex = findCurrentSemesterIndex(
          orderedSemesterIds,
          semesterLabelById,
          yearLevelBySemesterId,
          enrollmentSemesterLabel,
          enrollmentYearLevel
      );

      if (currentSemesterIndex == null || currentSemesterIndex < 0 || currentSemesterIndex >= orderedSemesterIds.size()) {
        return 0.0f;
      }

      Long currentSemesterId = orderedSemesterIds.get(currentSemesterIndex);
      return sumSemesterUnits(conn, currentSemesterId);
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return 0.0f;
    }
  }

  private Optional<Long> resolveCurriculumId(Connection conn, String studentId) throws SQLException {
    String studentSql = "SELECT curriculum_id, course_id FROM students WHERE student_id = ?";

    try (PreparedStatement ps = conn.prepareStatement(studentSql)) {
      ps.setString(1, studentId);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return Optional.empty();
        }

        long curriculumId = rs.getLong("curriculum_id");
        if (!rs.wasNull()) {
          return Optional.of(curriculumId);
        }

        long courseId = rs.getLong("course_id");
        if (rs.wasNull()) {
          return Optional.empty();
        }

        return getLatestCurriculumByCourse(conn, courseId);
      }
    }
  }

  private Optional<Long> getLatestCurriculumByCourse(Connection conn, Long courseId) throws SQLException {
    String sql = "SELECT id FROM curriculum WHERE course = ? ORDER BY cur_year DESC, created_at DESC FETCH FIRST 1 ROWS ONLY";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, courseId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(rs.getLong("id"));
        }
      }
    }

    return Optional.empty();
  }

  private List<Long> getOrderedSemesterIdsByCurriculum(
      Connection conn,
      Long curriculumId,
      Map<Long, String> semesterLabelById,
      Map<Long, Integer> yearLevelBySemesterId
  ) throws SQLException {
    boolean hasYearLevel = hasYearLevelColumn(conn);
    String sql = hasYearLevel
        ? "SELECT id, semester, year_level, created_at FROM semester WHERE curriculum_id = ?"
        : "SELECT id, semester, created_at FROM semester WHERE curriculum_id = ?";

    List<Long> semesterIds = new ArrayList<>();
    Map<Long, Timestamp> createdAtBySemesterId = new HashMap<>();

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, curriculumId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Long semesterId = rs.getLong("id");
          semesterIds.add(semesterId);

          semesterLabelById.put(semesterId, rs.getString("semester"));
          createdAtBySemesterId.put(semesterId, rs.getTimestamp("created_at"));

          Integer yearLevel = null;
          if (hasYearLevel) {
            int rawYearLevel = rs.getInt("year_level");
            if (!rs.wasNull()) {
              yearLevel = rawYearLevel;
            }
          }
          yearLevelBySemesterId.put(semesterId, yearLevel);
        }
      }
    }

    semesterIds.sort(Comparator
        .comparing((Long semesterId) -> yearLevelBySemesterId.get(semesterId),
            Comparator.nullsLast(Integer::compareTo))
        .thenComparing(semesterId -> resolveSemesterRank(semesterLabelById.get(semesterId)))
        .thenComparing(semesterId -> createdAtBySemesterId.get(semesterId),
            Comparator.nullsLast(Timestamp::compareTo))
        .thenComparingLong(Long::longValue));

    return semesterIds;
  }

  private boolean hasYearLevelColumn(Connection conn) {
    try {
      DatabaseMetaData metadata = conn.getMetaData();
      try (ResultSet rs = metadata.getColumns(null, null, "SEMESTER", "YEAR_LEVEL")) {
        if (rs.next()) {
          return true;
        }
      }

      try (ResultSet rs = metadata.getColumns(null, null, "semester", "year_level")) {
        return rs.next();
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  private Integer findCurrentSemesterIndex(
      List<Long> orderedSemesterIds,
      Map<Long, String> semesterLabelById,
      Map<Long, Integer> yearLevelBySemesterId,
      String enrollmentSemesterLabel,
      Long enrollmentYearLevel
  ) {
    if (orderedSemesterIds.isEmpty()) {
      return null;
    }

    if (enrollmentSemesterLabel == null || enrollmentSemesterLabel.isBlank()) {
      return null;
    }

    Integer normalizedYearLevel = enrollmentYearLevel == null ? null : enrollmentYearLevel.intValue();

    if (normalizedYearLevel != null) {
      for (int index = 0; index < orderedSemesterIds.size(); index++) {
        Long semesterId = orderedSemesterIds.get(index);
        Integer candidateYearLevel = yearLevelBySemesterId.get(semesterId);
        if (candidateYearLevel == null || !candidateYearLevel.equals(normalizedYearLevel)) {
          continue;
        }

        if (semesterLabelsMatch(semesterLabelById.get(semesterId), enrollmentSemesterLabel)) {
          return index;
        }
      }
    }

    for (int index = 0; index < orderedSemesterIds.size(); index++) {
      Long semesterId = orderedSemesterIds.get(index);
      if (semesterLabelsMatch(semesterLabelById.get(semesterId), enrollmentSemesterLabel)) {
        return index;
      }
    }

    return null;
  }

  private Integer findHighestActiveSemesterIndex(
      List<Long> orderedSemesterIds,
      Map<Long, Long> completedCountBySemester,
      Map<Long, Long> activityCountBySemester
  ) {
    for (int index = orderedSemesterIds.size() - 1; index >= 0; index--) {
      Long semesterId = orderedSemesterIds.get(index);
      long completedCount = completedCountBySemester.getOrDefault(semesterId, 0L);
      long activityCount = activityCountBySemester.getOrDefault(semesterId, 0L);
      if (completedCount > 0 || activityCount > 0) {
        return index;
      }
    }

    return null;
  }

  private void addNextSemesterUnlock(
      LinkedHashSet<Long> allowedSemesterIds,
      List<Long> orderedSemesterIds,
      Map<Long, Integer> yearLevelBySemesterId,
      Map<Long, Long> activeLoadCountBySemester,
      Map<Long, Long> completedCountBySemester,
      Integer currentSemesterIndex
  ) {
    if (currentSemesterIndex == null || currentSemesterIndex < 0 || currentSemesterIndex >= orderedSemesterIds.size()) {
      return;
    }

    Long currentSemesterId = orderedSemesterIds.get(currentSemesterIndex);
    long activeLoadCount = activeLoadCountBySemester.getOrDefault(currentSemesterId, 0L);
    if (activeLoadCount <= 0) {
      return;
    }

    long completedCount = completedCountBySemester.getOrDefault(currentSemesterId, 0L);
    if (completedCount < activeLoadCount) {
      return;
    }

    int nextIndex = currentSemesterIndex + 1;
    if (nextIndex >= orderedSemesterIds.size()) {
      return;
    }

    Long nextSemesterId = orderedSemesterIds.get(nextIndex);
    Integer currentYearLevel = yearLevelBySemesterId.get(currentSemesterId);
    Integer nextYearLevel = yearLevelBySemesterId.get(nextSemesterId);

    if (currentYearLevel != null && nextYearLevel != null && !currentYearLevel.equals(nextYearLevel)) {
      return;
    }

    allowedSemesterIds.add(nextSemesterId);
  }

  private int resolveSemesterRank(String semesterLabel) {
    if (semesterLabel == null || semesterLabel.isBlank()) {
      return Integer.MAX_VALUE;
    }

    String normalized = semesterLabel.trim().toUpperCase(Locale.ROOT);
    if (normalized.contains("1ST") || normalized.contains("FIRST") || normalized.matches(".*\\b1\\b.*")) {
      return 1;
    }

    if (normalized.contains("2ND") || normalized.contains("SECOND") || normalized.matches(".*\\b2\\b.*")) {
      return 2;
    }

    if (normalized.contains("3RD") || normalized.contains("THIRD") || normalized.matches(".*\\b3\\b.*")) {
      return 3;
    }

    if (normalized.contains("SUMMER")) {
      return 9;
    }

    return 99;
  }

  private boolean semesterLabelsMatch(String left, String right) {
    if (left == null || right == null) {
      return false;
    }

    String normalizedLeft = normalizeSemesterToken(left);
    String normalizedRight = normalizeSemesterToken(right);
    if (!normalizedLeft.isEmpty() && normalizedLeft.equals(normalizedRight)) {
      return true;
    }

    int leftRank = resolveSemesterRank(left);
    int rightRank = resolveSemesterRank(right);
    return leftRank < 99 && leftRank == rightRank;
  }

  private String normalizeSemesterToken(String value) {
    return value == null
        ? ""
        : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
  }

  private Set<Long> getSemesterSubjectIds(Connection conn, Set<Long> semesterIds) throws SQLException {
    if (semesterIds == null || semesterIds.isEmpty()) {
      return Collections.emptySet();
    }

    String placeholders = String.join(",", Collections.nCopies(semesterIds.size(), "?"));
    String sql = "SELECT id FROM semester_subjects WHERE semester_id IN (" + placeholders + ") ORDER BY semester_id, id";

    LinkedHashSet<Long> semesterSubjectIds = new LinkedHashSet<>();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      int parameterIndex = 1;
      for (Long semesterId : semesterIds) {
        ps.setLong(parameterIndex++, semesterId);
      }

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          semesterSubjectIds.add(rs.getLong("id"));
        }
      }
    }

    return semesterSubjectIds;
  }

  private Set<Long> filterSemesterSubjectsByPrerequisites(
      Connection conn,
      String studentId,
      Set<Long> semesterSubjectIds
  ) throws SQLException {
    if (semesterSubjectIds == null || semesterSubjectIds.isEmpty()) {
      return Collections.emptySet();
    }

    if (!hasPrerequisitesTable(conn)) {
      return semesterSubjectIds;
    }

    Map<Long, Long> subjectIdBySemesterSubjectId = getSubjectIdBySemesterSubjectId(conn, semesterSubjectIds);
    if (subjectIdBySemesterSubjectId.isEmpty()) {
      return Collections.emptySet();
    }

    Set<Long> subjectIds = new HashSet<>(subjectIdBySemesterSubjectId.values());
    Map<Long, Set<Long>> prerequisiteIdsBySubjectId = getPrerequisiteIdsBySubjectId(conn, subjectIds);
    if (prerequisiteIdsBySubjectId.isEmpty()) {
      return semesterSubjectIds;
    }

    Set<Long> completedSubjectIds = getCompletedSubjectIds(conn, studentId);
    LinkedHashSet<Long> filteredSemesterSubjectIds = new LinkedHashSet<>();

    for (Long semesterSubjectId : semesterSubjectIds) {
      Long subjectId = subjectIdBySemesterSubjectId.get(semesterSubjectId);
      if (subjectId == null) {
        continue;
      }

      Set<Long> prerequisiteIds = prerequisiteIdsBySubjectId.get(subjectId);
      if (prerequisiteIds == null || prerequisiteIds.isEmpty() || completedSubjectIds.containsAll(prerequisiteIds)) {
        filteredSemesterSubjectIds.add(semesterSubjectId);
      }
    }

    return filteredSemesterSubjectIds;
  }

  private boolean hasPrerequisitesTable(Connection conn) {
    try {
      DatabaseMetaData metadata = conn.getMetaData();
      try (ResultSet rs = metadata.getTables(null, null, "PREREQUISITES", null)) {
        if (rs.next()) {
          return true;
        }
      }

      try (ResultSet rs = metadata.getTables(null, null, "prerequisites", null)) {
        return rs.next();
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  private Map<Long, Long> getSubjectIdBySemesterSubjectId(Connection conn, Set<Long> semesterSubjectIds)
      throws SQLException {
    if (semesterSubjectIds.isEmpty()) {
      return Collections.emptyMap();
    }

    String placeholders = String.join(",", Collections.nCopies(semesterSubjectIds.size(), "?"));
    String sql = "SELECT id, subject_id FROM semester_subjects WHERE id IN (" + placeholders + ")";

    Map<Long, Long> subjectIdBySemesterSubjectId = new HashMap<>();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      int parameterIndex = 1;
      for (Long semesterSubjectId : semesterSubjectIds) {
        ps.setLong(parameterIndex++, semesterSubjectId);
      }

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          subjectIdBySemesterSubjectId.put(rs.getLong("id"), rs.getLong("subject_id"));
        }
      }
    }

    return subjectIdBySemesterSubjectId;
  }

  private Map<Long, Set<Long>> getPrerequisiteIdsBySubjectId(Connection conn, Set<Long> subjectIds) throws SQLException {
    if (subjectIds == null || subjectIds.isEmpty()) {
      return Collections.emptyMap();
    }

    String placeholders = String.join(",", Collections.nCopies(subjectIds.size(), "?"));
    String sql = "SELECT subject_id, pre_subject_id FROM prerequisites WHERE subject_id IN (" + placeholders + ")";

    Map<Long, Set<Long>> prerequisiteIdsBySubjectId = new HashMap<>();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      int parameterIndex = 1;
      for (Long subjectId : subjectIds) {
        ps.setLong(parameterIndex++, subjectId);
      }

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Long subjectId = rs.getLong("subject_id");
          Long prerequisiteSubjectId = rs.getLong("pre_subject_id");

          prerequisiteIdsBySubjectId
              .computeIfAbsent(subjectId, key -> new HashSet<>())
              .add(prerequisiteSubjectId);
        }
      }
    }

    return prerequisiteIdsBySubjectId;
  }

  private Set<Long> getCompletedSubjectIds(Connection conn, String studentId) throws SQLException {
    String sql =
        "SELECT DISTINCT ss.subject_id AS subject_id "
            + "FROM student_enrolled_subjects ses "
            + "JOIN semester_subjects ss ON ss.id = ses.semester_subject_id "
            + "WHERE ses.student_id = ? AND ses.status = 'COMPLETED'";

    Set<Long> completedSubjectIds = new HashSet<>();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, studentId);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          completedSubjectIds.add(rs.getLong("subject_id"));
        }
      }
    }

    return completedSubjectIds;
  }

  private long countRequiredSubjects(Connection conn, Long semesterId) throws SQLException {
    String sql = "SELECT COUNT(*) AS total FROM semester_subjects WHERE semester_id = ?";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, semesterId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getLong("total");
        }
      }
    }

    return 0;
  }

  private long countCompletedSubjects(Connection conn, String studentId, Long semesterId) throws SQLException {
    String sql =
        "SELECT COUNT(DISTINCT ses.semester_subject_id) AS total "
            + "FROM student_enrolled_subjects ses "
            + "JOIN semester_subjects ss ON ss.id = ses.semester_subject_id "
            + "WHERE ses.student_id = ? AND ss.semester_id = ? AND ses.status = 'COMPLETED'";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, studentId);
      ps.setLong(2, semesterId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getLong("total");
        }
      }
    }

    return 0;
  }

  private long countCompletedOrEnrolledLoadSubjects(Connection conn, String studentId, Long semesterId) throws SQLException {
    String sql =
        "SELECT COUNT(DISTINCT ses.semester_subject_id) AS total "
            + "FROM student_enrolled_subjects ses "
            + "JOIN semester_subjects ss ON ss.id = ses.semester_subject_id "
            + "WHERE ses.student_id = ? "
            + "AND ss.semester_id = ? "
            + "AND ses.status IN ('ENROLLED', 'COMPLETED')";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, studentId);
      ps.setLong(2, semesterId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getLong("total");
        }
      }
    }

    return 0;
  }

  private long countTrackedSubjectActivity(Connection conn, String studentId, Long semesterId) throws SQLException {
    String sql =
        "SELECT COUNT(*) AS total "
            + "FROM student_enrolled_subjects ses "
            + "JOIN semester_subjects ss ON ss.id = ses.semester_subject_id "
            + "WHERE ses.student_id = ? AND ss.semester_id = ? AND ses.status IN ('ENROLLED', 'COMPLETED')";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, studentId);
      ps.setLong(2, semesterId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getLong("total");
        }
      }
    }

    return 0;
  }

  private long countSelectedEnrollmentActivity(Connection conn, String studentId, Long semesterId) throws SQLException {
    String sql =
        "SELECT COUNT(DISTINCT o.semester_subject_id) AS total "
            + "FROM enrollments_details ed "
            + "JOIN enrollments e ON e.id = ed.enrollment_id "
            + "JOIN offerings o ON o.id = ed.offering_id "
            + "JOIN semester_subjects ss ON ss.id = o.semester_subject_id "
            + "WHERE e.student_id = ? AND ss.semester_id = ? AND ed.status = 'SELECTED'";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, studentId);
      ps.setLong(2, semesterId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getLong("total");
        }
      }
    }

    return 0;
  }

  private float sumSemesterUnits(Connection conn, Long semesterId) throws SQLException {
    String sql =
        "SELECT COALESCE(SUM(COALESCE(s.units, 0)), 0) AS total_units "
            + "FROM semester_subjects ss "
            + "JOIN subjects s ON s.id = ss.subject_id "
            + "WHERE ss.semester_id = ?";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, semesterId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getFloat("total_units");
        }
      }
    }

    return 0.0f;
  }
}
