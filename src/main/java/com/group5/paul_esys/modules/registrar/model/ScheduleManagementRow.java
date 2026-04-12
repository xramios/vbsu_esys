package com.group5.paul_esys.modules.registrar.model;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public record ScheduleManagementRow(
    Long scheduleId,
    Long offeringId,
    Long enrollmentPeriodId,
    String enrollmentPeriodLabel,
    Long sectionId,
    String sectionCode,
    String subjectCode,
    String subjectName,
    String day,
    LocalTime startTime,
    LocalTime endTime,
    Long roomId,
    String roomName,
    Long facultyId,
    String facultyName,
    boolean roomConflict,
    boolean facultyConflict,
    boolean sectionConflict
) {

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  public ScheduleManagementRow withConflictFlags(
      boolean resolvedRoomConflict,
      boolean resolvedFacultyConflict,
      boolean resolvedSectionConflict
  ) {
    return new ScheduleManagementRow(
        scheduleId,
        offeringId,
        enrollmentPeriodId,
        enrollmentPeriodLabel,
        sectionId,
        sectionCode,
        subjectCode,
        subjectName,
        day,
        startTime,
        endTime,
        roomId,
        roomName,
        facultyId,
        facultyName,
        resolvedRoomConflict,
        resolvedFacultyConflict,
        resolvedSectionConflict
    );
  }

  public boolean hasConflict() {
    return roomConflict || facultyConflict || sectionConflict;
  }

  public String timeRangeLabel() {
    if (startTime == null || endTime == null) {
      return "N/A";
    }

    return startTime.format(TIME_FORMATTER) + " - " + endTime.format(TIME_FORMATTER);
  }

  public String roomDisplay() {
    if (roomName == null || roomName.isBlank()) {
      return "TBA";
    }

    return roomName.trim();
  }

  public String facultyDisplay() {
    if (facultyName == null || facultyName.isBlank()) {
      return "TBA";
    }

    return facultyName.trim();
  }

  public String conflictLabel() {
    List<String> labels = new ArrayList<>(3);
    if (roomConflict) {
      labels.add("ROOM");
    }

    if (facultyConflict) {
      labels.add("FACULTY");
    }

    if (sectionConflict) {
      labels.add("SECTION");
    }

    if (labels.isEmpty()) {
      return "NONE";
    }

    return String.join(" + ", labels);
  }

  public String searchableText() {
    return (
        safeText(subjectCode) + ' '
            + safeText(subjectName) + ' '
            + safeText(sectionCode) + ' '
            + safeText(day) + ' '
            + safeText(roomDisplay()) + ' '
            + safeText(facultyDisplay()) + ' '
            + safeText(enrollmentPeriodLabel)
    ).toLowerCase();
  }

  private String safeText(String value) {
    if (value == null || value.isBlank()) {
      return "";
    }

    return value.trim();
  }
}