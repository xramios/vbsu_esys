package com.group5.paul_esys.modules.enrollments.model;

public class StudentEnrolledSubject {

  private String studentId;
  private Long subjectId;

  public StudentEnrolledSubject() {
  }

  public StudentEnrolledSubject(String studentId, Long subjectId) {
    this.studentId = studentId;
    this.subjectId = subjectId;
  }

  public String getStudentId() {
    return studentId;
  }

  public StudentEnrolledSubject setStudentId(String studentId) {
    this.studentId = studentId;
    return this;
  }

  public Long getSubjectId() {
    return subjectId;
  }

  public StudentEnrolledSubject setSubjectId(Long subjectId) {
    this.subjectId = subjectId;
    return this;
  }
}
