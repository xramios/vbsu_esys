package com.group5.paul_esys.modules.enrollments.model;

import com.group5.paul_esys.modules.enums.StudentEnrolledSubjectStatus;
import java.sql.Timestamp;

public class StudentEnrolledSubject {

  private String studentId;
  private Long semesterSubjectId;
  private StudentEnrolledSubjectStatus status;
  private Timestamp createdAt;
  private Timestamp updatedAt;

  public StudentEnrolledSubject() {
  }

  public StudentEnrolledSubject(String studentId, Long semesterSubjectId, StudentEnrolledSubjectStatus status, Timestamp createdAt, Timestamp updatedAt) {
    this.studentId = studentId;
    this.semesterSubjectId = semesterSubjectId;
    this.status = status;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public String getStudentId() {
    return studentId;
  }

  public StudentEnrolledSubject setStudentId(String studentId) {
    this.studentId = studentId;
    return this;
  }

  public Long getSemesterSubjectId() {
    return semesterSubjectId;
  }

  public StudentEnrolledSubject setSemesterSubjectId(Long semesterSubjectId) {
    this.semesterSubjectId = semesterSubjectId;
    return this;
  }

  public StudentEnrolledSubjectStatus getStatus() {
    return status;
  }

  public StudentEnrolledSubject setStatus(StudentEnrolledSubjectStatus status) {
    this.status = status;
    return this;
  }

  public Timestamp getCreatedAt() {
    return createdAt;
  }

  public StudentEnrolledSubject setCreatedAt(Timestamp createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Timestamp getUpdatedAt() {
    return updatedAt;
  }

  public StudentEnrolledSubject setUpdatedAt(Timestamp updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }
}
