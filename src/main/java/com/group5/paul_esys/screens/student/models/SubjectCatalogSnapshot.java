
package com.group5.paul_esys.screens.student.models;

import java.util.List;

public record SubjectCatalogSnapshot(
    boolean activeEnrollmentPeriod,
    String announcementText,
    String catalogLabel,
    List<Object[]> rows
) {}
