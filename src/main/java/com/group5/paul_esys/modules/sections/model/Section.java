package com.group5.paul_esys.modules.sections.model;

import java.sql.Timestamp;

public class Section {

  private Long id;
  private String sectionCode;
  private Integer capacity;
  private String status;
  private Timestamp updatedAt;
  private Timestamp createdAt;

  public Section() {
  }

  public Section(Long id, String sectionCode, Integer capacity, Timestamp updatedAt, Timestamp createdAt) {
    this(id, sectionCode, capacity, "OPEN", updatedAt, createdAt);
  }

  public Section(Long id, String sectionCode, Integer capacity, String status, Timestamp updatedAt, Timestamp createdAt) {
    this.id = id;
    this.sectionCode = sectionCode;
    this.capacity = capacity;
    this.status = status;
    this.updatedAt = updatedAt;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public Section setId(Long id) {
    this.id = id;
    return this;
  }

  public String getSectionCode() {
    return sectionCode;
  }

  public Section setSectionCode(String sectionCode) {
    this.sectionCode = sectionCode;
    return this;
  }

  public Integer getCapacity() {
    return capacity;
  }

  public Section setCapacity(Integer capacity) {
    this.capacity = capacity;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public Section setStatus(String status) {
    this.status = status;
    return this;
  }

  public Timestamp getUpdatedAt() {
    return updatedAt;
  }

  public Section setUpdatedAt(Timestamp updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public Timestamp getCreatedAt() {
    return createdAt;
  }

  public Section setCreatedAt(Timestamp createdAt) {
    this.createdAt = createdAt;
    return this;
  }
}
