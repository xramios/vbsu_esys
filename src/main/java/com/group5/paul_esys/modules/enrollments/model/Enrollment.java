package com.group5.paul_esys.modules.enrollments.model;

import com.group5.paul_esys.modules.enums.EnrollmentStatus;
import java.sql.Timestamp;
import java.util.Date;

public class Enrollment {

  private Long id;
  private String studentId;
  private Long enrollmentPeriodId;
  private EnrollmentStatus status;
  private Float maxUnits;
  private Float totalUnits;
  private Date submittedAt;
  private Timestamp updatedAt;
  private Timestamp createdAt;

  public Enrollment() {
  }

  public Enrollment(Long id, String studentId, Long enrollmentPeriodId, EnrollmentStatus status, Float maxUnits, Float totalUnits, Date submittedAt, Timestamp updatedAt, Timestamp createdAt) {
    this.id = id;
    this.studentId = studentId;
    this.enrollmentPeriodId = enrollmentPeriodId;
    this.status = status;
    this.maxUnits = maxUnits;
    this.totalUnits = totalUnits;
    this.submittedAt = submittedAt;
    this.updatedAt = updatedAt;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public Enrollment setId(Long id) {
    this.id = id;
    return this;
  }

  public String getStudentId() {
    return studentId;
  }

  public Enrollment setStudentId(String studentId) {
    this.studentId = studentId;
    return this;
  }

  public Long getEnrollmentPeriodId() {
    return enrollmentPeriodId;
  }

  public Enrollment setEnrollmentPeriodId(Long enrollmentPeriodId) {
    this.enrollmentPeriodId = enrollmentPeriodId;
    return this;
  }

  public EnrollmentStatus getStatus() {
    return status;
  }

  public Enrollment setStatus(EnrollmentStatus status) {
    this.status = status;
    return this;
  }

  public Float getMaxUnits() {
    return maxUnits;
  }

  public Enrollment setMaxUnits(Float maxUnits) {
    this.maxUnits = maxUnits;
    return this;
  }

  public Float getTotalUnits() {
    return totalUnits;
  }

  public Enrollment setTotalUnits(Float totalUnits) {
    this.totalUnits = totalUnits;
    return this;
  }

  public Date getSubmittedAt() {
    return submittedAt;
  }

  public Enrollment setSubmittedAt(Date submittedAt) {
    this.submittedAt = submittedAt;
    return this;
  }

  public Timestamp getUpdatedAt() {
    return updatedAt;
  }

  public Enrollment setUpdatedAt(Timestamp updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public Timestamp getCreatedAt() {
    return createdAt;
  }

  public Enrollment setCreatedAt(Timestamp createdAt) {
    this.createdAt = createdAt;
    return this;
  }
}
