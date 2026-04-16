package com.group5.paul_esys.modules.schedules.model;

import java.sql.Time;
import java.util.List;

/**
 * Represents a schedule conflict between two or more student enrollments.
 * Contains details about overlapping times, affected subjects, and sections.
 */
public class ScheduleConflict {
  private final List<Long> conflictingScheduleIds;
  private final List<ConflictSubjectInfo> conflictingSubjects;
  private final String conflictDayOfWeek;
  private final Time conflictStartTime;
  private final Time conflictEndTime;

  public ScheduleConflict(
      List<Long> conflictingScheduleIds,
      List<ConflictSubjectInfo> conflictingSubjects,
      String conflictDayOfWeek,
      Time conflictStartTime,
      Time conflictEndTime) {
    this.conflictingScheduleIds = conflictingScheduleIds;
    this.conflictingSubjects = conflictingSubjects;
    this.conflictDayOfWeek = conflictDayOfWeek;
    this.conflictStartTime = conflictStartTime;
    this.conflictEndTime = conflictEndTime;
  }

  public List<Long> getConflictingScheduleIds() {
    return conflictingScheduleIds;
  }

  public List<ConflictSubjectInfo> getConflictingSubjects() {
    return conflictingSubjects;
  }

  public String getConflictDayOfWeek() {
    return conflictDayOfWeek;
  }

  public Time getConflictStartTime() {
    return conflictStartTime;
  }

  public Time getConflictEndTime() {
    return conflictEndTime;
  }

  public String getFormattedConflictTime() {
    if (conflictStartTime == null || conflictEndTime == null) {
      return "TBA";
    }
    return conflictStartTime.toString().substring(0, 5) + "-" + conflictEndTime.toString().substring(0, 5);
  }

  public String getConflictSummary() {
    StringBuilder summary = new StringBuilder();
    summary.append(conflictDayOfWeek).append(" ");
    summary.append(getFormattedConflictTime()).append(" - ");
    
    if (conflictingSubjects != null) {
      for (int i = 0; i < conflictingSubjects.size(); i++) {
        if (i > 0) summary.append(" & ");
        summary.append(conflictingSubjects.get(i).getSubjectCode());
      }
    }
    
    return summary.toString();
  }

  /**
   * Nested class to hold subject information for a conflict.
   */
  public static class ConflictSubjectInfo {
    private final Long offeringId;
    private final String subjectCode;
    private final String subjectName;
    private final String sectionCode;

    public ConflictSubjectInfo(Long offeringId, String subjectCode, String subjectName, String sectionCode) {
      this.offeringId = offeringId;
      this.subjectCode = subjectCode;
      this.subjectName = subjectName;
      this.sectionCode = sectionCode;
    }

    public Long getOfferingId() {
      return offeringId;
    }

    public String getSubjectCode() {
      return subjectCode;
    }

    public String getSubjectName() {
      return subjectName;
    }

    public String getSectionCode() {
      return sectionCode;
    }
  }
}
