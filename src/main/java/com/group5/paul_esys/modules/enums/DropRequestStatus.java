package com.group5.paul_esys.modules.enums;

public enum DropRequestStatus {
  PENDING,
  APPROVED,
  REJECTED;

  public static DropRequestStatus fromValue(String value) {
    if (value == null || value.isBlank()) {
      return PENDING;
    }

    try {
      return DropRequestStatus.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      return PENDING;
    }
  }
}