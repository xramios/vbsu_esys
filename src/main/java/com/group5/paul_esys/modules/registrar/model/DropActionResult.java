package com.group5.paul_esys.modules.registrar.model;

public class DropActionResult {

  private final boolean success;
  private final Long enrollmentId;

  public DropActionResult(boolean success, Long enrollmentId) {
    this.success = success;
    this.enrollmentId = enrollmentId;
  }

  public boolean isSuccess() {
    return success;
  }

  public Long getEnrollmentId() {
    return enrollmentId;
  }
}