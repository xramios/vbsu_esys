package com.group5.paul_esys.modules.offerings.model;

import java.sql.Timestamp;

public class Offering {

  private Long id;
  private Long subjectId;
  private Long sectionId;
  private Long enrollmentPeriodId;
  private Long semesterSubjectId;
  private Integer capacity;
  private Timestamp createdAt;

  public Offering() {
  }

  public Offering(
      Long id,
      Long subjectId,
      Long sectionId,
      Long enrollmentPeriodId,
      Long semesterSubjectId,
      Integer capacity,
      Timestamp createdAt
  ) {
    this.id = id;
    this.subjectId = subjectId;
    this.sectionId = sectionId;
    this.enrollmentPeriodId = enrollmentPeriodId;
    this.semesterSubjectId = semesterSubjectId;
    this.capacity = capacity;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public Offering setId(Long id) {
    this.id = id;
    return this;
  }

  public Long getSubjectId() {
    return subjectId;
  }

  public Offering setSubjectId(Long subjectId) {
    this.subjectId = subjectId;
    return this;
  }

  public Long getSectionId() {
    return sectionId;
  }

  public Offering setSectionId(Long sectionId) {
    this.sectionId = sectionId;
    return this;
  }

  public Long getEnrollmentPeriodId() {
    return enrollmentPeriodId;
  }

  public Offering setEnrollmentPeriodId(Long enrollmentPeriodId) {
    this.enrollmentPeriodId = enrollmentPeriodId;
    return this;
  }

  public Long getSemesterSubjectId() {
    return semesterSubjectId;
  }

  public Offering setSemesterSubjectId(Long semesterSubjectId) {
    this.semesterSubjectId = semesterSubjectId;
    return this;
  }

  public Integer getCapacity() {
    return capacity;
  }

  public Offering setCapacity(Integer capacity) {
    this.capacity = capacity;
    return this;
  }

  public Timestamp getCreatedAt() {
    return createdAt;
  }

  public Offering setCreatedAt(Timestamp createdAt) {
    this.createdAt = createdAt;
    return this;
  }
}
