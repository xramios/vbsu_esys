package com.group5.paul_esys.modules.schedules.services;

import com.group5.paul_esys.modules.courses.services.CourseService;
import com.group5.paul_esys.modules.offerings.model.Offering;
import com.group5.paul_esys.modules.offerings.services.OfferingService;
import com.group5.paul_esys.modules.schedules.model.Schedule;
import com.group5.paul_esys.modules.schedules.model.ScheduleConflict;
import com.group5.paul_esys.modules.sections.model.Section;
import com.group5.paul_esys.modules.sections.services.SectionService;
import com.group5.paul_esys.modules.subjects.model.Subject;
import com.group5.paul_esys.modules.subjects.services.SubjectService;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for detecting and analyzing schedule conflicts in student enrollments.
 * Identifies overlapping schedules and provides conflict information for UI highlighting.
 */
public class ScheduleConflictAnalyzer {

  private static final ScheduleConflictAnalyzer INSTANCE = new ScheduleConflictAnalyzer();
  private static final Logger logger = LoggerFactory.getLogger(ScheduleConflictAnalyzer.class);

  private ScheduleConflictAnalyzer() {
  }

  public static ScheduleConflictAnalyzer getInstance() {
    return INSTANCE;
  }

  /**
   * Analyzes a list of schedules grouped by offering and returns conflict information.
   * 
   * @param schedulesPerOffering Map of offering ID to list of schedules
   * @return Map of schedule ID to list of conflicts involving that schedule
   */
  public Map<Long, List<ScheduleConflict>> analyzeConflicts(
      Map<Long, List<Schedule>> schedulesPerOffering) {
    
    Map<Long, List<ScheduleConflict>> conflictsBySchedule = new HashMap<>();
    
    if (schedulesPerOffering == null || schedulesPerOffering.isEmpty()) {
      return conflictsBySchedule;
    }

    List<ScheduleWithOffering> allSchedules = new ArrayList<>();
    
    // Build flat list with offering info
    for (Map.Entry<Long, List<Schedule>> entry : schedulesPerOffering.entrySet()) {
      Long offeringId = entry.getKey();
      for (Schedule schedule : entry.getValue()) {
        if (schedule != null && schedule.getStartTime() != null && schedule.getEndTime() != null) {
          allSchedules.add(new ScheduleWithOffering(schedule, offeringId));
        }
      }
    }

    // Compare all pairs of schedules
    for (int i = 0; i < allSchedules.size(); i++) {
      for (int j = i + 1; j < allSchedules.size(); j++) {
        ScheduleWithOffering sched1 = allSchedules.get(i);
        ScheduleWithOffering sched2 = allSchedules.get(j);

        // Skip if same offering (multiple sessions of same subject are allowed)
        if (sched1.offeringId.equals(sched2.offeringId)) {
          continue;
        }

        // Check for day and time overlap
        if (schedulesOverlap(sched1.schedule, sched2.schedule)) {
          ScheduleConflict conflict1 = buildConflict(sched1, sched2);
          ScheduleConflict conflict2 = buildConflict(sched2, sched1);

          conflictsBySchedule.computeIfAbsent(sched1.schedule.getId(), k -> new ArrayList<>()).add(conflict1);
          conflictsBySchedule.computeIfAbsent(sched2.schedule.getId(), k -> new ArrayList<>()).add(conflict2);

          logger.debug("Conflict detected: Schedule {} vs {}", sched1.schedule.getId(), sched2.schedule.getId());
        }
      }
    }

    return conflictsBySchedule;
  }

  /**
   * Checks if two schedules overlap in time on the same day of week.
   */
  private boolean schedulesOverlap(Schedule schedule1, Schedule schedule2) {
    // Must be same day
    if (!schedule1.getDay().equals(schedule2.getDay())) {
      return false;
    }

    // Time overlap check: start1 < end2 AND end1 > start2
    java.sql.Time start1 = schedule1.getStartTime();
    java.sql.Time end1 = schedule1.getEndTime();
    java.sql.Time start2 = schedule2.getStartTime();
    java.sql.Time end2 = schedule2.getEndTime();

    return start1.getTime() < end2.getTime() && end1.getTime() > start2.getTime();
  }

  /**
   * Builds conflict information for a schedule in conflict with another.
   */
  private ScheduleConflict buildConflict(ScheduleWithOffering primary, ScheduleWithOffering conflict) {
    Schedule primarySched = primary.schedule;
    Schedule conflictingSched = conflict.schedule;

    // Fetch offering and subject info for the conflicting subject
    Optional<Offering> conflictingOffering = OfferingService.getInstance()
        .getOfferingById(conflict.offeringId);
    
    List<ScheduleConflict.ConflictSubjectInfo> conflictingSubjects = new ArrayList<>();
    
    if (conflictingOffering.isPresent()) {
      Offering offering = conflictingOffering.get();
      Optional<Subject> subject = SubjectService.getInstance()
          .getSubjectById(offering.getSubjectId());
      Optional<Section> section = SectionService.getInstance()
          .getSectionById(offering.getSectionId());

      if (subject.isPresent() && section.isPresent()) {
        conflictingSubjects.add(
            new ScheduleConflict.ConflictSubjectInfo(
                conflict.offeringId,
                subject.get().getSubjectCode(),
                subject.get().getSubjectName(),
                section.get().getSectionCode()
            )
        );
      }
    }

    return new ScheduleConflict(
        Arrays.asList(conflictingSched.getId()),
        conflictingSubjects,
        primarySched.getDay().toString(),
        primarySched.getStartTime(),
        primarySched.getEndTime()
    );
  }

  /**
   * Gets conflict information for a specific offering's schedules.
   */
  public List<ScheduleConflict> getConflictsForOffering(
      Long offeringId,
      Map<Long, List<Schedule>> schedulesPerOffering,
      Map<Long, List<ScheduleConflict>> allConflicts) {
    
    List<ScheduleConflict> conflicts = new ArrayList<>();

    List<Schedule> offeringSchedules = schedulesPerOffering.get(offeringId);
    if (offeringSchedules == null) {
      return conflicts;
    }

    for (Schedule schedule : offeringSchedules) {
      List<ScheduleConflict> scheduleConflicts = allConflicts.get(schedule.getId());
      if (scheduleConflicts != null) {
        conflicts.addAll(scheduleConflicts);
      }
    }

    return conflicts;
  }

  /**
   * Helper class to associate schedule with its offering.
   */
  private static class ScheduleWithOffering {
    final Schedule schedule;
    final Long offeringId;

    ScheduleWithOffering(Schedule schedule, Long offeringId) {
      this.schedule = schedule;
      this.offeringId = offeringId;
    }
  }
}
