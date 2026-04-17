
package com.group5.paul_esys.screens.student.models;

import java.util.List;

public record CompletedSubjectsSnapshot(
    String summaryText,
    List<Object[]> rows
) {}
