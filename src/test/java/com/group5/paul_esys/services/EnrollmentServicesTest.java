package com.group5.paul_esys.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.group5.paul_esys.modules.enums.EnrollmentDetailStatus;
import com.group5.paul_esys.modules.enums.EnrollmentStatus;
import com.group5.paul_esys.modules.enrollments.model.Enrollment;
import com.group5.paul_esys.modules.enrollments.model.EnrollmentDetail;
import com.group5.paul_esys.modules.enrollments.services.EnrollmentDetailService;
import com.group5.paul_esys.modules.enrollments.services.EnrollmentService;
import com.group5.paul_esys.modules.enrollments.services.StudentAcademicPromotionService;
import com.group5.paul_esys.modules.enrollments.services.StudentEnrollmentEligibilityService;
import com.group5.paul_esys.modules.enrollments.services.StudentEnrolledSubjectService;
import com.group5.paul_esys.modules.enrollments.services.StudentSemesterProgressService;
import com.group5.paul_esys.modules.students.model.Student;
import com.group5.paul_esys.modules.students.model.StudentStatus;
import com.group5.paul_esys.modules.students.services.StudentService;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import com.group5.paul_esys.testsupport.ServiceTestSupport;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnrollmentServicesTest extends ServiceTestSupport {

  @Test
  void studentServiceReturnsEmptyOptionalWhenStudentMissing() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertTrue(StudentService.getInstance().get("20250001").isEmpty()));
  }

  @Test
  void studentServiceInsertSucceedsWhenRowsAreInserted() throws Exception {
    Student student = new Student()
        .setStudentId("20250001")
        .setUserId(1L)
        .setFirstName("Ada")
        .setLastName("Lovelace")
        .setMiddleName("B")
        .setBirthdate(Date.valueOf(LocalDate.of(2000, 1, 1)))
        .setStudentStatus(StudentStatus.REGULAR)
        .setCourseId(1L)
        .setCurriculumId(1L)
        .setYearLevel(1L);

    withConnection(mockUpdateConnectionOnly(1), () ->
        assertTrue(StudentService.getInstance().insert(student).isPresent()));
  }

  @Test
  void studentServiceUpdateSucceedsWhenRowsAreUpdated() throws Exception {
    Student student = new Student()
        .setStudentId("20250001")
        .setUserId(1L)
        .setFirstName("Ada")
        .setLastName("Lovelace")
        .setMiddleName("B")
        .setBirthdate(Date.valueOf(LocalDate.of(2000, 1, 1)))
        .setStudentStatus(StudentStatus.REGULAR)
        .setCourseId(1L)
        .setCurriculumId(1L)
        .setYearLevel(2L);

    withConnection(mockUpdateConnectionOnly(1), () ->
        assertTrue(StudentService.getInstance().update(student).isPresent()));
  }

  @Test
  void studentServiceRejectsBlankAvailabilityChecks() {
    assertFalse(StudentService.getInstance().isEmailAvailable(" "));
    assertFalse(StudentService.getInstance().isStudentIdAvailable(null));
  }

  @Test
  void studentServiceReturnsEmptyListWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertTrue(StudentService.getInstance().list().isEmpty()));
  }

  @Test
  void enrollmentServiceRejectsNullEnrollment() {
    assertFalse(EnrollmentService.getInstance().createEnrollment(null));
  }

  @Test
  void enrollmentServiceRejectsSubmittedEnrollmentBelowMinimumUnitsOnCreate() {
    Enrollment enrollment = new Enrollment()
        .setStudentId("20250001")
        .setEnrollmentPeriodId(1L)
        .setStatus(EnrollmentStatus.SUBMITTED)
        .setMaxUnits(24.0f)
        .setTotalUnits(17.0f);

    assertFalse(EnrollmentService.getInstance().createEnrollment(enrollment));
  }

  @Test
  void enrollmentServiceRejectsSubmittedEnrollmentBelowMinimumUnitsOnUpdate() {
    Enrollment enrollment = new Enrollment()
        .setId(1L)
        .setStudentId("20250001")
        .setEnrollmentPeriodId(1L)
        .setStatus(EnrollmentStatus.SUBMITTED)
        .setMaxUnits(24.0f)
        .setTotalUnits(17.0f);

    assertFalse(EnrollmentService.getInstance().updateEnrollment(enrollment));
  }

  @Test
  void enrollmentServiceAllowsSubmittedEnrollmentAtMinimumUnits() throws Exception {
    Enrollment enrollment = new Enrollment()
        .setId(1L)
        .setStudentId("20250001")
        .setEnrollmentPeriodId(1L)
        .setStatus(EnrollmentStatus.SUBMITTED)
        .setMaxUnits(24.0f)
        .setTotalUnits(18.0f);

    withConnection(mockUpdateConnectionOnly(1), () ->
        assertTrue(EnrollmentService.getInstance().updateEnrollment(enrollment)));
  }

  @Test
  void enrollmentServiceAllowsSubmittedEnrollmentAboveMinimumUnits() throws Exception {
    Enrollment enrollment = new Enrollment()
        .setId(1L)
        .setStudentId("20250001")
        .setEnrollmentPeriodId(1L)
        .setStatus(EnrollmentStatus.SUBMITTED)
        .setMaxUnits(24.0f)
        .setTotalUnits(21.0f);

    withConnection(mockUpdateConnectionOnly(1), () ->
        assertTrue(EnrollmentService.getInstance().updateEnrollment(enrollment)));
  }

  @Test
  void enrollmentServiceAllowsCreateWhenAnotherOpenPeriodExists() throws Exception {
    JdbcMock jdbc = mockUpdateConnection(1);
    when(jdbc.resultSet().next()).thenReturn(true, false);
    when(jdbc.resultSet().getLong("id")).thenReturn(1L);
    when(jdbc.resultSet().getString("school_year")).thenReturn("2025-2026");
    when(jdbc.resultSet().getString("semester")).thenReturn("First Semester");
    when(jdbc.resultSet().getString("description")).thenReturn("Open period");
    when(jdbc.resultSet().getTimestamp("start_date")).thenReturn(Timestamp.valueOf(LocalDateTime.now().minusDays(1)));
    when(jdbc.resultSet().getTimestamp("end_date")).thenReturn(Timestamp.valueOf(LocalDateTime.now().plusDays(1)));
    when(jdbc.resultSet().getTimestamp("updated_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
    when(jdbc.resultSet().getTimestamp("created_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now().minusDays(2)));

    Enrollment enrollment = new Enrollment()
        .setStudentId("20250001")
        .setEnrollmentPeriodId(1L)
        .setStatus(EnrollmentStatus.DRAFT)
        .setMaxUnits(24.0f)
        .setTotalUnits(0.0f);

    withConnection(jdbc.connection(), () ->
        assertTrue(EnrollmentService.getInstance().createEnrollment(enrollment)));
  }

  @Test
  void enrollmentServiceReturnsEmptyListWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertTrue(EnrollmentService.getInstance().getAllEnrollments().isEmpty()));
  }

  @Test
  void enrollmentServiceUpdateStatusSucceedsWithoutAStudentLink() throws Exception {
    withConnection(mockUpdateConnectionOnly(1), () ->
        assertTrue(EnrollmentService.getInstance().updateEnrollmentStatus(1L, EnrollmentStatus.APPROVED)));
  }

  @Test
  void enrollmentServiceDeleteSucceedsWithoutAStudentLink() throws Exception {
    withConnection(mockUpdateConnectionOnly(1), () ->
        assertTrue(EnrollmentService.getInstance().deleteEnrollment(1L)));
  }

  @Test
  void enrollmentServiceBackfillReturnsZeroWhenNoRowsAreUpdated() throws Exception {
    withConnection(mockUpdateConnectionOnly(0), () ->
        assertEquals(0, EnrollmentService.getInstance().backfillCompletedEnrollments()));
  }

  @Test
  void enrollmentDetailServiceRejectsIncompleteCreateRequest() {
    EnrollmentDetail detail = new EnrollmentDetail()
        .setEnrollmentId(null)
        .setOfferingId(null)
        .setUnits(3.0f)
        .setStatus(EnrollmentDetailStatus.SELECTED);

    assertFalse(EnrollmentDetailService.getInstance().createEnrollmentDetail(detail));
  }

  @Test
  void enrollmentDetailServiceReturnsZeroWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertEquals(0L, EnrollmentDetailService.getInstance().countSelectedByOffering(1L)));
  }

  @Test
  void enrollmentDetailServiceReturnsEmptyListWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertTrue(EnrollmentDetailService.getInstance().getEnrollmentDetailsByEnrollment(1L).isEmpty()));
  }

  @Test
  void studentEnrolledSubjectServiceReturnsEmptyListWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertTrue(StudentEnrolledSubjectService.getInstance().getByStudent("20250001").isEmpty()));
  }

  @Test
  void studentEnrolledSubjectServiceReturnsEmptyOptionalWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertTrue(StudentEnrolledSubjectService.getInstance().getByStudentAndOffering("20250001", 1L).isEmpty()));
  }

  @Test
  void studentEnrolledSubjectServiceRejectsInvalidDeleteRequest() {
    assertFalse(StudentEnrolledSubjectService.getInstance().delete(" ", 1L));
  }

  @Test
  void studentSemesterProgressServiceRejectsBlankStudentId() {
    assertFalse(StudentSemesterProgressService.getInstance().syncStudentProgress(" "));
  }

  @Test
  void studentSemesterProgressServiceRejectsInvalidInitialization() throws Exception {
    assertFalse(StudentSemesterProgressService.getInstance().initializeInitialSemesterProgress(null, null, null));
  }

  @Test
  void studentSemesterProgressServiceReturnsEmptyListWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertTrue(StudentSemesterProgressService.getInstance().getProgressByStudent("20250001").isEmpty()));
  }

  @Test
  void studentAcademicPromotionServiceRejectsInvalidInput() throws Exception {
    assertFalse(StudentAcademicPromotionService.getInstance().promoteIfYearCompleted(null, "20250001"));
    var jdbc = mockEmptyQueryConnection();
    withConnection(jdbc.connection(), () ->
        assertFalse(StudentAcademicPromotionService.getInstance().promoteIfYearCompleted(jdbc.connection(), "20250001")));
  }

  @Test
  void studentEnrollmentEligibilityServiceRejectsBlankStudentId() {
    assertTrue(StudentEnrollmentEligibilityService.getInstance().getEligibleSemesterSubjectIds(" ", null, null).isEmpty());
    assertEquals(0.0f, StudentEnrollmentEligibilityService.getInstance().getCurrentSemesterUnitLimit(" ", null, null));
  }

  @Test
  void studentEnrollmentEligibilityServiceResolvesCurrentSemesterUnitLimitFromCurriculum() throws Exception {
    Connection connection = mock(Connection.class);
    PreparedStatement studentStatement = mock(PreparedStatement.class);
    PreparedStatement semesterStatement = mock(PreparedStatement.class);
    PreparedStatement unitLimitStatement = mock(PreparedStatement.class);
    ResultSet studentResultSet = mock(ResultSet.class);
    ResultSet semesterResultSet = mock(ResultSet.class);
    ResultSet unitLimitResultSet = mock(ResultSet.class);
    DatabaseMetaData metadata = mock(DatabaseMetaData.class);
    ResultSet yearLevelColumnResultSet = mock(ResultSet.class);

    when(connection.prepareStatement(anyString()))
        .thenReturn(studentStatement, semesterStatement, unitLimitStatement);
    when(connection.getMetaData()).thenReturn(metadata);
    when(metadata.getColumns(null, null, "SEMESTER", "YEAR_LEVEL")).thenReturn(yearLevelColumnResultSet);
    when(yearLevelColumnResultSet.next()).thenReturn(true);

    when(studentStatement.executeQuery()).thenReturn(studentResultSet);
    when(studentResultSet.next()).thenReturn(true, false);
    when(studentResultSet.getLong("curriculum_id")).thenReturn(10L);
    when(studentResultSet.wasNull()).thenReturn(false);

    when(semesterStatement.executeQuery()).thenReturn(semesterResultSet);
    when(semesterResultSet.next()).thenReturn(true, false);
    when(semesterResultSet.getLong("id")).thenReturn(100L);
    when(semesterResultSet.getString("semester")).thenReturn("First Semester");
    when(semesterResultSet.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2025, 1, 1, 0, 0)));
    when(semesterResultSet.getInt("year_level")).thenReturn(1);
    when(semesterResultSet.wasNull()).thenReturn(false);

    when(unitLimitStatement.executeQuery()).thenReturn(unitLimitResultSet);
    when(unitLimitResultSet.next()).thenReturn(true, false);
    when(unitLimitResultSet.getFloat("total_units")).thenReturn(20.0f);

    withConnection(connection, () -> assertEquals(
        20.0f,
        StudentEnrollmentEligibilityService.getInstance()
            .getCurrentSemesterUnitLimit("20250001", "First Semester", 1L)));
  }
}