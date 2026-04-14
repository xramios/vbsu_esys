package com.group5.paul_esys.modules.registrar.model;

import com.group5.paul_esys.modules.enums.DropRequestStatus;
import java.sql.Timestamp;

public class FacultyStudentDropRequest {

  private Long id;
  private Long facultyId;
  private String studentId;
  private Long offeringId;
  private String reason;
  private DropRequestStatus status;
  private Timestamp createdAt;
  private Timestamp updatedAt;

  private String studentName;
  private String facultyName;
  private String subjectCode;
  private String subjectName;
  private String sectionCode;
  private String enrollmentPeriodLabel;
  private Long enrollmentId;
  private String enrollmentDetailStatus;

  public Long getId() {
    return id;
  }

  public FacultyStudentDropRequest setId(Long id) {
    this.id = id;
    return this;
  }

  public Long getFacultyId() {
    return facultyId;
  }

  public FacultyStudentDropRequest setFacultyId(Long facultyId) {
    this.facultyId = facultyId;
    return this;
  }

  public String getStudentId() {
    return studentId;
  }

  public FacultyStudentDropRequest setStudentId(String studentId) {
    this.studentId = studentId;
    return this;
  }

  public Long getOfferingId() {
    return offeringId;
  }

  public FacultyStudentDropRequest setOfferingId(Long offeringId) {
    this.offeringId = offeringId;
    return this;
  }

  public String getReason() {
    return reason;
  }

  public FacultyStudentDropRequest setReason(String reason) {
    this.reason = reason;
    return this;
  }

  public DropRequestStatus getStatus() {
    return status;
  }

  public FacultyStudentDropRequest setStatus(DropRequestStatus status) {
    this.status = status;
    return this;
  }

  public Timestamp getCreatedAt() {
    return createdAt;
  }

  public FacultyStudentDropRequest setCreatedAt(Timestamp createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Timestamp getUpdatedAt() {
    return updatedAt;
  }

  public FacultyStudentDropRequest setUpdatedAt(Timestamp updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public String getStudentName() {
    return studentName;
  }

  public FacultyStudentDropRequest setStudentName(String studentName) {
    this.studentName = studentName;
    return this;
  }

  public String getFacultyName() {
    return facultyName;
  }

  public FacultyStudentDropRequest setFacultyName(String facultyName) {
    this.facultyName = facultyName;
    return this;
  }

  public String getSubjectCode() {
    return subjectCode;
  }

  public FacultyStudentDropRequest setSubjectCode(String subjectCode) {
    this.subjectCode = subjectCode;
    return this;
  }

  public String getSubjectName() {
    return subjectName;
  }

  public FacultyStudentDropRequest setSubjectName(String subjectName) {
    this.subjectName = subjectName;
    return this;
  }

  public String getSectionCode() {
    return sectionCode;
  }

  public FacultyStudentDropRequest setSectionCode(String sectionCode) {
    this.sectionCode = sectionCode;
    return this;
  }

  public String getEnrollmentPeriodLabel() {
    return enrollmentPeriodLabel;
  }

  public FacultyStudentDropRequest setEnrollmentPeriodLabel(String enrollmentPeriodLabel) {
    this.enrollmentPeriodLabel = enrollmentPeriodLabel;
    return this;
  }

  public Long getEnrollmentId() {
    return enrollmentId;
  }

  public FacultyStudentDropRequest setEnrollmentId(Long enrollmentId) {
    this.enrollmentId = enrollmentId;
    return this;
  }

  public String getEnrollmentDetailStatus() {
    return enrollmentDetailStatus;
  }

  public FacultyStudentDropRequest setEnrollmentDetailStatus(String enrollmentDetailStatus) {
    this.enrollmentDetailStatus = enrollmentDetailStatus;
    return this;
  }
}