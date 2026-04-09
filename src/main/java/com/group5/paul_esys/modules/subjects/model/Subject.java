package com.group5.paul_esys.modules.subjects.model;

import java.sql.Timestamp;

public class Subject {

  private Long id;
  private String subjectName;
  private String subjectCode;
  private Float units;
  private String description;
  private Long departmentId;
  private Timestamp updatedAt;
  private Timestamp createdAt;

  public Subject() {
  }

  public Subject(Long id, String subjectName, String subjectCode, Float units, String description, Long departmentId, Timestamp updatedAt, Timestamp createdAt) {
    this.id = id;
    this.subjectName = subjectName;
    this.subjectCode = subjectCode;
    this.units = units;
    this.description = description;
    this.departmentId = departmentId;
    this.updatedAt = updatedAt;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public Subject setId(Long id) {
    this.id = id;
    return this;
  }

  public String getSubjectName() {
    return subjectName;
  }

  public Subject setSubjectName(String subjectName) {
    this.subjectName = subjectName;
    return this;
  }

  public String getSubjectCode() {
    return subjectCode;
  }

  public Subject setSubjectCode(String subjectCode) {
    this.subjectCode = subjectCode;
    return this;
  }

  public Float getUnits() {
    return units;
  }

  public Subject setUnits(Float units) {
    this.units = units;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public Subject setDescription(String description) {
    this.description = description;
    return this;
  }

  public Long getDepartmentId() {
    return departmentId;
  }

  public Subject setDepartmentId(Long departmentId) {
    this.departmentId = departmentId;
    return this;
  }

  public Timestamp getUpdatedAt() {
    return updatedAt;
  }

  public Subject setUpdatedAt(Timestamp updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public Timestamp getCreatedAt() {
    return createdAt;
  }

  public Subject setCreatedAt(Timestamp createdAt) {
    this.createdAt = createdAt;
    return this;
  }
}
