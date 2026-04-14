package com.group5.paul_esys.modules.registrar.model;

public class StudentDropCandidate {

  private String studentId;
  private String studentName;

  public String getStudentId() {
    return studentId;
  }

  public StudentDropCandidate setStudentId(String studentId) {
    this.studentId = studentId;
    return this;
  }

  public String getStudentName() {
    return studentName;
  }

  public StudentDropCandidate setStudentName(String studentName) {
    this.studentName = studentName;
    return this;
  }
}