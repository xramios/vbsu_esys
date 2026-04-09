package com.group5.paul_esys.modules.semester.model;

import java.sql.Timestamp;

public class Semester {

    private Long id;
    private Long curriculumId;
    private String semester;
    private Integer yearLevel;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Semester() {}

    public Semester(
        Long id,
        Long curriculumId,
        String semester,
        Integer yearLevel,
        Timestamp createdAt,
        Timestamp updatedAt
    ) {
        this.id = id;
        this.curriculumId = curriculumId;
        this.semester = semester;
        this.yearLevel = yearLevel;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public Semester setId(Long id) {
        this.id = id;
        return this;
    }

    public Long getCurriculumId() {
        return curriculumId;
    }

    public Semester setCurriculumId(Long curriculumId) {
        this.curriculumId = curriculumId;
        return this;
    }

    public String getSemester() {
        return semester;
    }

    public Semester setSemester(String semester) {
        this.semester = semester;
        return this;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Semester setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public Semester setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public Integer getYearLevel() {
        return yearLevel;
    }

    public void setYearLevel(Integer yearLevel) {
        this.yearLevel = yearLevel;
    }
}
