package com.group5.paul_esys.modules.enrollment.model;

import java.sql.Timestamp;
import java.util.Date;

public class EnrollmentPeriod {

  private Long id;
  private String schoolYear;
  private String semester;
  private Date startDate;
  private Date endDate;
  private Timestamp updatedAt;
  private Timestamp createdAt;

  public EnrollmentPeriod() {
  }

  public EnrollmentPeriod(Long id, String schoolYear, String semester, Date startDate, Date endDate, Timestamp updatedAt, Timestamp createdAt) {
    this.id = id;
    this.schoolYear = schoolYear;
    this.semester = semester;
    this.startDate = startDate;
    this.endDate = endDate;
    this.updatedAt = updatedAt;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public EnrollmentPeriod setId(Long id) {
    this.id = id;
    return this;
  }

  public String getSchoolYear() {
    return schoolYear;
  }

  public EnrollmentPeriod setSchoolYear(String schoolYear) {
    this.schoolYear = schoolYear;
    return this;
  }

  public String getSemester() {
    return semester;
  }

  public EnrollmentPeriod setSemester(String semester) {
    this.semester = semester;
    return this;
  }

  public Date getStartDate() {
    return startDate;
  }

  public EnrollmentPeriod setStartDate(Date startDate) {
    this.startDate = startDate;
    return this;
  }

  public Date getEndDate() {
    return endDate;
  }

  public EnrollmentPeriod setEndDate(Date endDate) {
    this.endDate = endDate;
    return this;
  }

  public Timestamp getUpdatedAt() {
    return updatedAt;
  }

  public EnrollmentPeriod setUpdatedAt(Timestamp updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public Timestamp getCreatedAt() {
    return createdAt;
  }

  public EnrollmentPeriod setCreatedAt(Timestamp createdAt) {
    this.createdAt = createdAt;
    return this;
  }
}
