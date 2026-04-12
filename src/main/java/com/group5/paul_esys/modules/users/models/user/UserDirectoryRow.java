package com.group5.paul_esys.modules.users.models.user;

import com.group5.paul_esys.modules.users.models.enums.Role;

public class UserDirectoryRow {

  private final Long userId;
  private final Long profileId;
  private final String studentId;
  private final String email;
  private final Role role;
  private final String firstName;
  private final String lastName;
  private final String contactNumber;

  public UserDirectoryRow(
    Long userId,
    Long profileId,
    String studentId,
    String email,
    Role role,
    String firstName,
    String lastName,
    String contactNumber
  ) {
    this.userId = userId;
    this.profileId = profileId;
    this.studentId = studentId;
    this.email = email;
    this.role = role;
    this.firstName = firstName;
    this.lastName = lastName;
    this.contactNumber = contactNumber;
  }

  public Long getUserId() {
    return userId;
  }

  public Long getProfileId() {
    return profileId;
  }

  public String getStudentId() {
    return studentId;
  }

  public String getEmail() {
    return email;
  }

  public Role getRole() {
    return role;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getContactNumber() {
    return contactNumber;
  }

  public String getFullName() {
    String first = firstName == null ? "" : firstName.trim();
    String last = lastName == null ? "" : lastName.trim();

    if (first.isEmpty() && last.isEmpty()) {
      return "-";
    }

    if (first.isEmpty()) {
      return last;
    }

    if (last.isEmpty()) {
      return first;
    }

    return last + ", " + first;
  }

  public boolean matchesSearchTerm(String rawSearchTerm) {
    if (rawSearchTerm == null || rawSearchTerm.trim().isEmpty()) {
      return true;
    }

    String searchTerm = rawSearchTerm.trim().toLowerCase();

    return getFullName().toLowerCase().contains(searchTerm)
      || safeLower(email).contains(searchTerm)
      || safeLower(contactNumber).contains(searchTerm)
      || safeLower(studentId).contains(searchTerm)
      || role.name().toLowerCase().contains(searchTerm);
  }

  private String safeLower(String value) {
    return value == null ? "" : value.toLowerCase();
  }
}
