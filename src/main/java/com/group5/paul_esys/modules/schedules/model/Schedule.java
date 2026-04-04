package com.group5.paul_esys.modules.schedules.model;

import com.group5.paul_esys.modules.enums.DayOfWeek;
import java.sql.Timestamp;
import java.sql.Time;

public class Schedule {

  private Long id;
  private Long sectionId;
  private Long roomId;
  private Long facultyId;
  private DayOfWeek day;
  private Time startTime;
  private Time endTime;
  private Long enrollmentPeriodId;
  private Timestamp updatedAt;
  private Timestamp createdAt;

  public Schedule() {
  }

  public Schedule(Long id, Long sectionId, Long roomId, Long facultyId, DayOfWeek day, Time startTime, Time endTime, Long enrollmentPeriodId, Timestamp updatedAt, Timestamp createdAt) {
    this.id = id;
    this.sectionId = sectionId;
    this.roomId = roomId;
    this.facultyId = facultyId;
    this.day = day;
    this.startTime = startTime;
    this.endTime = endTime;
    this.enrollmentPeriodId = enrollmentPeriodId;
    this.updatedAt = updatedAt;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public Schedule setId(Long id) {
    this.id = id;
    return this;
  }

  public Long getSectionId() {
    return sectionId;
  }

  public Schedule setSectionId(Long sectionId) {
    this.sectionId = sectionId;
    return this;
  }

  public Long getRoomId() {
    return roomId;
  }

  public Schedule setRoomId(Long roomId) {
    this.roomId = roomId;
    return this;
  }

  public Long getFacultyId() {
    return facultyId;
  }

  public Schedule setFacultyId(Long facultyId) {
    this.facultyId = facultyId;
    return this;
  }

  public DayOfWeek getDay() {
    return day;
  }

  public Schedule setDay(DayOfWeek day) {
    this.day = day;
    return this;
  }

  public Time getStartTime() {
    return startTime;
  }

  public Schedule setStartTime(Time startTime) {
    this.startTime = startTime;
    return this;
  }

  public Time getEndTime() {
    return endTime;
  }

  public Schedule setEndTime(Time endTime) {
    this.endTime = endTime;
    return this;
  }

  public Long getEnrollmentPeriodId() {
    return enrollmentPeriodId;
  }

  public Schedule setEnrollmentPeriodId(Long enrollmentPeriodId) {
    this.enrollmentPeriodId = enrollmentPeriodId;
    return this;
  }

  public Timestamp getUpdatedAt() {
    return updatedAt;
  }

  public Schedule setUpdatedAt(Timestamp updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public Timestamp getCreatedAt() {
    return createdAt;
  }

  public Schedule setCreatedAt(Timestamp createdAt) {
    this.createdAt = createdAt;
    return this;
  }
}
