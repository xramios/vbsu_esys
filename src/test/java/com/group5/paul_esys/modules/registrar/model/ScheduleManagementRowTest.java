package com.group5.paul_esys.modules.registrar.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class ScheduleManagementRowTest {

  @Test
  void conflictLabelReturnsNoneWhenNoFlagsAreSet() {
    ScheduleManagementRow row = buildRow(false, false, false);

    assertEquals("NONE", row.conflictLabel());
    assertFalse(row.hasConflict());
  }

  @Test
  void conflictLabelIncludesSectionOnly() {
    ScheduleManagementRow row = buildRow(false, false, true);

    assertEquals("SECTION", row.conflictLabel());
    assertTrue(row.hasConflict());
  }

  @Test
  void conflictLabelIncludesAllFlagsInExpectedOrder() {
    ScheduleManagementRow row = buildRow(true, true, true);

    assertEquals("ROOM + FACULTY + SECTION", row.conflictLabel());
    assertTrue(row.hasConflict());
  }

  @Test
  void withConflictFlagsAppliesAllConflictTypes() {
    ScheduleManagementRow base = buildRow(false, false, false);

    ScheduleManagementRow updated = base.withConflictFlags(true, false, true);

    assertTrue(updated.roomConflict());
    assertFalse(updated.facultyConflict());
    assertTrue(updated.sectionConflict());
    assertEquals("ROOM + SECTION", updated.conflictLabel());
  }

  private ScheduleManagementRow buildRow(
      boolean roomConflict,
      boolean facultyConflict,
      boolean sectionConflict
  ) {
    return new ScheduleManagementRow(
        1L,
        1L,
        1L,
        "2026-2027 | Semester 1 | ID 1",
        1L,
        "IT2B",
        "CCP1102",
        "Computer Programming 2",
        "MON",
        LocalTime.of(8, 0),
        LocalTime.of(10, 0),
        1L,
        "EN403",
        1L,
        "Lutz, Kathy",
        roomConflict,
        facultyConflict,
        sectionConflict
    );
  }
}
