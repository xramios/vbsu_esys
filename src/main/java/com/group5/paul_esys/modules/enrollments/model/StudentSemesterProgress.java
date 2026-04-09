package com.group5.paul_esys.modules.enrollments.model;

import com.group5.paul_esys.modules.enums.SemesterProgressStatus;
import java.sql.Timestamp;

public class StudentSemesterProgress {

  private Long id;
  private String studentId;
  private Long curriculumId;
  private Long semesterId;
  private SemesterProgressStatus status;
  private Timestamp startedAt;
  private Timestamp completedAt;
  private Timestamp createdAt;
  private Timestamp updatedAt;

  public StudentSemesterProgress() {
  }

  public StudentSemesterProgress(
      Long id,
      String studentId,
      Long curriculumId,
      Long semesterId,
      SemesterProgressStatus status,
      Timestamp startedAt,
      Timestamp completedAt,
      Timestamp createdAt,
      Timestamp updatedAt
  ) {
    this.id = id;
    this.studentId = studentId;
    this.curriculumId = curriculumId;
    this.semesterId = semesterId;
    this.status = status;
    this.startedAt = startedAt;
    this.completedAt = completedAt;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public StudentSemesterProgress setId(Long id) {
    this.id = id;
    return this;
  }

  public String getStudentId() {
    return studentId;
  }

  public StudentSemesterProgress setStudentId(String studentId) {
    this.studentId = studentId;
    return this;
  }

  public Long getCurriculumId() {
    return curriculumId;
  }

  public StudentSemesterProgress setCurriculumId(Long curriculumId) {
    this.curriculumId = curriculumId;
    return this;
  }

  public Long getSemesterId() {
    return semesterId;
  }

  public StudentSemesterProgress setSemesterId(Long semesterId) {
    this.semesterId = semesterId;
    return this;
  }

  public SemesterProgressStatus getStatus() {
    return status;
  }

  public StudentSemesterProgress setStatus(SemesterProgressStatus status) {
    this.status = status;
    return this;
  }

  public Timestamp getStartedAt() {
    return startedAt;
  }

  public StudentSemesterProgress setStartedAt(Timestamp startedAt) {
    this.startedAt = startedAt;
    return this;
  }

  public Timestamp getCompletedAt() {
    return completedAt;
  }

  public StudentSemesterProgress setCompletedAt(Timestamp completedAt) {
    this.completedAt = completedAt;
    return this;
  }

  public Timestamp getCreatedAt() {
    return createdAt;
  }

  public StudentSemesterProgress setCreatedAt(Timestamp createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Timestamp getUpdatedAt() {
    return updatedAt;
  }

  public StudentSemesterProgress setUpdatedAt(Timestamp updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }
}