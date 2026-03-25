package com.group5.paul_esys.modules.students.model;

import java.sql.Timestamp;
import java.util.Date;

public class Student {

  private String studentId;
  private Long userId;
  private String firstName;
  private String lastName;
  private String middleName;
  private Date birthdate;
  private StudentStatus studentStatus;
  private Long courseId;
  private Long yearLevel;
  private Timestamp createdAt;

  public Student() {

  }

  public Student(String studentId, Long userId, String firstName, String lastName, String middleName, Date birthdate, StudentStatus studentStatus, Long courseId, Long yearLevel, Timestamp createdAt) {
    this.studentId = studentId;
    this.userId = userId;
    this.firstName = firstName;
    this.lastName = lastName;
    this.middleName = middleName;
    this.birthdate = birthdate;
    this.studentStatus = studentStatus;
    this.courseId = courseId;
    this.yearLevel = yearLevel;
    this.createdAt = createdAt;
  }

  public String getStudentId() {
    return studentId;
  }

  public Student setStudentId(String studentId) {
    this.studentId = studentId;
    return this;
  }

  public Long getUserId() {
    return userId;
  }

  public Student setUserId(Long userId) {
    this.userId = userId;
    return this;
  }

  public String getFirstName() {
    return firstName;
  }

  public Student setFirstName(String firstName) {
    this.firstName = firstName;
    return this;
  }

  public String getLastName() {
    return lastName;
  }

  public Student setLastName(String lastName) {
    this.lastName = lastName;
    return this;
  }

  public String getMiddleName() {
    return middleName;
  }

  public Student setMiddleName(String middleName) {
    this.middleName = middleName;
    return this;
  }

  public Date getBirthdate() {
    return birthdate;
  }

  public Student setBirthdate(Date birthdate) {
    this.birthdate = birthdate;
    return this;
  }

  public StudentStatus getStudentStatus() {
    return studentStatus;
  }

  public Student setStudentStatus(StudentStatus studentStatus) {
    this.studentStatus = studentStatus;
    return this;
  }

  public Long getCourseId() {
    return courseId;
  }

  public Student setCourseId(Long courseId) {
    this.courseId = courseId;
    return this;
  }

  public Long getYearLevel() {
    return yearLevel;
  }

  public Student setYearLevel(Long yearLevel) {
    this.yearLevel = yearLevel;
    return this;
  }

  public Timestamp getCreatedAt() {
    return createdAt;
  }

  public Student setCreatedAt(Timestamp createdAt) {
    this.createdAt = createdAt;
    return this;
  }
}
