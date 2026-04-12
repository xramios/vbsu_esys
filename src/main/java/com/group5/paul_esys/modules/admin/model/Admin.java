package com.group5.paul_esys.modules.admin.model;

import java.sql.Timestamp;

public class Admin {

  private Long id;
  private Long userId;
  private String firstName;
  private String lastName;
  private String contactNumber;
  private Timestamp updatedAt;
  private Timestamp createdAt;

  public Admin() {}

  public Admin(
    Long id,
    Long userId,
    String firstName,
    String lastName,
    String contactNumber,
    Timestamp updatedAt,
    Timestamp createdAt
  ) {
    this.id = id;
    this.userId = userId;
    this.firstName = firstName;
    this.lastName = lastName;
    this.contactNumber = contactNumber;
    this.updatedAt = updatedAt;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public Admin setId(Long id) {
    this.id = id;
    return this;
  }

  public Long getUserId() {
    return userId;
  }

  public Admin setUserId(Long userId) {
    this.userId = userId;
    return this;
  }

  public String getFirstName() {
    return firstName;
  }

  public Admin setFirstName(String firstName) {
    this.firstName = firstName;
    return this;
  }

  public String getLastName() {
    return lastName;
  }

  public Admin setLastName(String lastName) {
    this.lastName = lastName;
    return this;
  }

  public String getContactNumber() {
    return contactNumber;
  }

  public Admin setContactNumber(String contactNumber) {
    this.contactNumber = contactNumber;
    return this;
  }

  public Timestamp getUpdatedAt() {
    return updatedAt;
  }

  public Admin setUpdatedAt(Timestamp updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public Timestamp getCreatedAt() {
    return createdAt;
  }

  public Admin setCreatedAt(Timestamp createdAt) {
    this.createdAt = createdAt;
    return this;
  }
}
