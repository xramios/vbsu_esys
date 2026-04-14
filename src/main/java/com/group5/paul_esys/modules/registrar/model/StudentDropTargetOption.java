package com.group5.paul_esys.modules.registrar.model;

public class StudentDropTargetOption {

  private Long enrollmentId;
  private String studentId;
  private Long offeringId;
  private Long semesterSubjectId;
  private Float units;
  private String enrollmentStatus;
  private String subjectCode;
  private String subjectName;
  private String sectionCode;
  private String enrollmentPeriodLabel;

  public Long getEnrollmentId() {
    return enrollmentId;
  }

  public StudentDropTargetOption setEnrollmentId(Long enrollmentId) {
    this.enrollmentId = enrollmentId;
    return this;
  }

  public String getStudentId() {
    return studentId;
  }

  public StudentDropTargetOption setStudentId(String studentId) {
    this.studentId = studentId;
    return this;
  }

  public Long getOfferingId() {
    return offeringId;
  }

  public StudentDropTargetOption setOfferingId(Long offeringId) {
    this.offeringId = offeringId;
    return this;
  }

  public Long getSemesterSubjectId() {
    return semesterSubjectId;
  }

  public StudentDropTargetOption setSemesterSubjectId(Long semesterSubjectId) {
    this.semesterSubjectId = semesterSubjectId;
    return this;
  }

  public Float getUnits() {
    return units;
  }

  public StudentDropTargetOption setUnits(Float units) {
    this.units = units;
    return this;
  }

  public String getEnrollmentStatus() {
    return enrollmentStatus;
  }

  public StudentDropTargetOption setEnrollmentStatus(String enrollmentStatus) {
    this.enrollmentStatus = enrollmentStatus;
    return this;
  }

  public String getSubjectCode() {
    return subjectCode;
  }

  public StudentDropTargetOption setSubjectCode(String subjectCode) {
    this.subjectCode = subjectCode;
    return this;
  }

  public String getSubjectName() {
    return subjectName;
  }

  public StudentDropTargetOption setSubjectName(String subjectName) {
    this.subjectName = subjectName;
    return this;
  }

  public String getSectionCode() {
    return sectionCode;
  }

  public StudentDropTargetOption setSectionCode(String sectionCode) {
    this.sectionCode = sectionCode;
    return this;
  }

  public String getEnrollmentPeriodLabel() {
    return enrollmentPeriodLabel;
  }

  public StudentDropTargetOption setEnrollmentPeriodLabel(String enrollmentPeriodLabel) {
    this.enrollmentPeriodLabel = enrollmentPeriodLabel;
    return this;
  }
}