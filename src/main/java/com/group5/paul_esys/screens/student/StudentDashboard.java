/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.group5.paul_esys.screens.student;

import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTGitHubIJTheme;
import com.group5.paul_esys.modules.courses.model.Course;
import com.group5.paul_esys.modules.courses.services.CourseService;
import com.group5.paul_esys.modules.enrollment_period.model.EnrollmentPeriod;
import com.group5.paul_esys.modules.enrollment_period.services.EnrollmentPeriodService;
import com.group5.paul_esys.modules.enrollments.model.Enrollment;
import com.group5.paul_esys.modules.enrollments.model.EnrollmentDetail;
import com.group5.paul_esys.modules.enrollments.services.EnrollmentDetailService;
import com.group5.paul_esys.modules.enrollments.services.EnrollmentService;
import com.group5.paul_esys.modules.enrollments.services.StudentEnrolledSubjectService;
import com.group5.paul_esys.modules.enums.EnrollmentDetailStatus;
import com.group5.paul_esys.modules.enums.EnrollmentStatus;
import com.group5.paul_esys.modules.enums.StudentEnrolledSubjectStatus;
import com.group5.paul_esys.modules.offerings.model.Offering;
import com.group5.paul_esys.modules.offerings.services.OfferingService;
import com.group5.paul_esys.modules.schedules.model.Schedule;
import com.group5.paul_esys.modules.schedules.services.ScheduleService;
import com.group5.paul_esys.modules.sections.model.Section;
import com.group5.paul_esys.modules.sections.services.SectionService;
import com.group5.paul_esys.modules.students.model.Student;
import com.group5.paul_esys.modules.subjects.model.Subject;
import com.group5.paul_esys.modules.subjects.services.SubjectService;
import com.group5.paul_esys.modules.users.services.UserSession;
import java.awt.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

/**
 *
 * @author Shan
 */
public class StudentDashboard extends javax.swing.JFrame {

        private static final int CATALOG_COL_CODE = 0;
        private static final int CATALOG_COL_SUBJECT_NAME = 1;
        private static final int CATALOG_COL_UNITS = 2;
        private static final int CATALOG_COL_OFFERING_ID = 6;
        private static final int CATALOG_COL_SUBJECT_ID = 7;
        private static final float MAX_ENROLLMENT_UNITS = 24.0f;

	private Student currentStudent;
        private final DefaultListModel<String> selectedSubjectsModel = new DefaultListModel<>();
        private boolean hasActiveEnrollmentPeriod;

	/**
	 * Creates new form Dashboard
	 */
	public StudentDashboard() {
		UIManager.put("TabbedPane.tabType", "underline");
		UIManager.put("TabbedPane.arc", 10);
		UIManager.put("TabbedPane.tabHeight", 36);
		UIManager.put("TabbedPane.tabInsets", new Insets(8, 16, 8, 16));

		UIManager.put("TabbedPane.selectedForeground", Color.BLACK);
		UIManager.put("TabbedPane.foreground", Color.GRAY);

		UIManager.put("TabbedPane.underlineColor", new Color(0, 120, 215));
		UIManager.put("TabbedPane.hoverColor", new Color(230, 230, 230));

		UIManager.put("TabbedPane.contentBorderInsets", new Insets(0, 0, 0, 0));
		UIManager.put("TabbedPane.showContentSeparator", false);
		FlatMTGitHubIJTheme.setup();

		Student student = (Student) UserSession.getInstance()
		  .getUserInformation()
		  .getUser();
		String fullName = String.format(
		  "%s, %s %s",
		  student.getLastName(),
		  student.getFirstName(),
		  (student.getMiddleName() != null) ? student.getMiddleName() : ""
		);

		this.setUndecorated(true);
		initComponents();
		this.setLocationRelativeTo(null);
		this.windowBar1.setTitle("Welcome " + fullName);
		this.currentStudent = student;
                initializeDashboardUi();
                reloadStudentDashboardData();
	}

        private void initializeDashboardUi() {
                configureSubjectCatalogTable();
                configureScheduleTable();
                configureSelectedSubjectsPanel();
        }

        private void reloadStudentDashboardData() {
                initStudentData(currentStudent);
                loadSubjectCatalog(txtSearch.getText());
                loadMySchedule();
        }

        private void configureSubjectCatalogTable() {
                DefaultTableModel model = new DefaultTableModel(
                  new Object[][]{},
                  new String[]{"Code", "Subject Name", "Units", "Section", "Schedule", "Slots", "Offering ID", "Subject ID"}
                ) {
                        @Override
                        public Class<?> getColumnClass(int columnIndex) {
                                return switch (columnIndex) {
                                        case CATALOG_COL_UNITS -> Float.class;
                                        case CATALOG_COL_OFFERING_ID, CATALOG_COL_SUBJECT_ID -> Long.class;
                                        default -> String.class;
                                };
                        }

                        @Override
                        public boolean isCellEditable(int row, int column) {
                                return false;
                        }
                };

                tblSubjectCatalog.setModel(model);
                tblSubjectCatalog.setRowHeight(26);
                tblSubjectCatalog.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                hideTableColumn(tblSubjectCatalog, CATALOG_COL_OFFERING_ID);
                hideTableColumn(tblSubjectCatalog, CATALOG_COL_SUBJECT_ID);

                tblSubjectCatalog.getSelectionModel().addListSelectionListener(evt -> {
                        if (!evt.getValueIsAdjusting()) {
                                refreshSelectedSubjectsPreview();
                        }
                });
        }

        private void configureScheduleTable() {
                DefaultTableModel model = new DefaultTableModel(
                  new Object[][]{},
                  new String[]{"Code", "Course Name", "Instructor", "Schedule", "Room", "Credits"}
                ) {
                        @Override
                        public boolean isCellEditable(int row, int column) {
                                return false;
                        }
                };

                tableSchedules.setModel(model);
                tableSchedules.setRowHeight(26);
        }

        private void configureSelectedSubjectsPanel() {
                jList1.setModel(selectedSubjectsModel);
                refreshSelectedSubjectsPreview();
        }

        private void hideTableColumn(JTable table, int columnIndex) {
                TableColumn column = table.getColumnModel().getColumn(columnIndex);
                column.setMinWidth(0);
                column.setMaxWidth(0);
                column.setPreferredWidth(0);
        }

        private void refreshSelectedSubjectsPreview() {
                selectedSubjectsModel.clear();
                float totalUnits = 0.0f;

                int[] selectedRows = tblSubjectCatalog.getSelectedRows();
                for (int selectedRow : selectedRows) {
                        int modelRow = tblSubjectCatalog.convertRowIndexToModel(selectedRow);
                        String code = String.valueOf(tblSubjectCatalog.getModel().getValueAt(modelRow, CATALOG_COL_CODE));
                        String subjectName = String.valueOf(tblSubjectCatalog.getModel().getValueAt(modelRow, CATALOG_COL_SUBJECT_NAME));
                        float units = parseUnitsCell(tblSubjectCatalog.getModel().getValueAt(modelRow, CATALOG_COL_UNITS));

                        selectedSubjectsModel.addElement(code + " - " + subjectName + " (" + formatUnits(units) + ")");
                        totalUnits += units;
                }

                jLabel17.setText(formatUnits(totalUnits) + " / " + formatUnits(MAX_ENROLLMENT_UNITS) + " units");
        }

	private void loadSubjectCatalog(String keyword) {
                DefaultTableModel model = (DefaultTableModel) tblSubjectCatalog.getModel();
		model.setRowCount(0);
                refreshSelectedSubjectsPreview();

                Optional<EnrollmentPeriod> activeEnrollmentPeriod = EnrollmentPeriodService.getInstance().getCurrentEnrollmentPeriod();
                Optional<EnrollmentPeriod> catalogEnrollmentPeriod = activeEnrollmentPeriod.isPresent()
                  ? activeEnrollmentPeriod
                  : EnrollmentPeriodService.getInstance().getAllEnrollmentPeriods().stream().findFirst();

                hasActiveEnrollmentPeriod = activeEnrollmentPeriod.isPresent();
                btnSubmitSchedule.setEnabled(hasActiveEnrollmentPeriod);

                if (catalogEnrollmentPeriod.isEmpty()) {
                        jLabel14.setText("Subject Catalog - No enrollment period configured");
                        return;
                }

                EnrollmentPeriod enrollmentPeriod = catalogEnrollmentPeriod.get();
                String periodLabel = buildEnrollmentPeriodLabel(enrollmentPeriod);
                if (hasActiveEnrollmentPeriod) {
			labelAnnouncement.setText("Enrollment Period is open");
                        jLabel14.setText("Subject Catalog - " + periodLabel);
                } else {
			labelAnnouncement.setText("Enrollment Period has ended");
                        jLabel14.setText("Subject Catalog - " + periodLabel + " (Preview only)");
                }

                List<Offering> offerings = OfferingService.getInstance().getOfferingsByEnrollmentPeriod(enrollmentPeriod.getId());
		if (offerings.isEmpty()) {
			return;
		}

		Map<Long, Subject> subjectById = new HashMap<>();
		for (Subject subject : SubjectService.getInstance().getAllSubjects()) {
			subjectById.put(subject.getId(), subject);
		}

		Map<Long, Section> sectionById = new HashMap<>();
		for (Section section : SectionService.getInstance().getAllSections()) {
			sectionById.put(section.getId(), section);
		}

		Map<Long, Long> selectedOfferingBySubject = getLatestSelectedOfferingBySubject();
		String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();

		for (Offering offering : offerings) {
			Subject subject = subjectById.get(offering.getSubjectId());
			Section section = sectionById.get(offering.getSectionId());
			if (subject == null || section == null) {
				continue;
			}

			if (!normalizedKeyword.isEmpty()) {
                                String subjectName = safeText(subject.getSubjectName(), "").toLowerCase();
                                String subjectCode = safeText(subject.getSubjectCode(), "").toLowerCase();
                                boolean matchesKeyword = subjectName.contains(normalizedKeyword) || subjectCode.contains(normalizedKeyword);
				if (!matchesKeyword) {
					continue;
				}
			}

			Long selectedOfferingId = selectedOfferingBySubject.get(subject.getId());
			if (selectedOfferingId != null && !selectedOfferingId.equals(offering.getId())) {
				continue;
			}

			addSubjectCatalogRow(model, subject, section, offering);
		}

                refreshSelectedSubjectsPreview();
	}

        private String buildEnrollmentPeriodLabel(EnrollmentPeriod period) {
                String schoolYear = safeText(period.getSchoolYear(), "N/A");
                String semester = safeText(period.getSemester(), "N/A");
                return schoolYear + " - " + semester;
        }

        private Map<Long, Long> getLatestSelectedOfferingBySubject() {
                Map<Long, Long> selectedOfferingBySubject = new HashMap<>();

                List<Enrollment> enrollments = EnrollmentService.getInstance().getEnrollmentsByStudent(currentStudent.getStudentId());
                if (enrollments == null || enrollments.isEmpty()) {
                        return selectedOfferingBySubject;
                }

                Enrollment latest = enrollments.get(0);
                List<EnrollmentDetail> details = EnrollmentDetailService.getInstance().getEnrollmentDetailsByEnrollment(latest.getId());
                for (EnrollmentDetail detail : details) {
                        if (detail.getStatus() != EnrollmentDetailStatus.SELECTED) {
                                continue;
                        }

                        Optional<Offering> offeringOpt = OfferingService.getInstance().getOfferingById(detail.getOfferingId());
                        if (offeringOpt.isPresent()) {
                                Offering offering = offeringOpt.get();
                                selectedOfferingBySubject.put(offering.getSubjectId(), offering.getId());
                        }
                }

                return selectedOfferingBySubject;
        }

        private void addSubjectCatalogRow(
                DefaultTableModel model,
                Subject subject,
                Section section,
                Offering offering
        ) {
                List<Schedule> schedules = ScheduleService.getInstance().getSchedulesByOffering(offering.getId());
                StringBuilder schedString = new StringBuilder();
                for (Schedule sched : schedules) {
                        if (sched.getStartTime() == null || sched.getEndTime() == null) {
                                continue;
                        }

                        schedString.append(sched.getDay().toString()).append(" ")
                          .append(sched.getStartTime().toString().substring(0, 5)).append("-")
                          .append(sched.getEndTime().toString().substring(0, 5)).append(" ");
                }

                int capacity = normalizeCapacity(offering.getCapacity() == null ? section.getCapacity() : offering.getCapacity());
                long enrolledCount = EnrollmentDetailService.getInstance().countSelectedByOffering(offering.getId());
                long availableSlots = Math.max(0, capacity - enrolledCount);
                String scheduleDisplay = schedString.length() == 0 ? "TBA" : schedString.toString().trim();
                String slotsDisplay = capacity <= 0 ? "N/A" : availableSlots + "/" + capacity;
                float units = subject.getUnits() == null ? 0.0f : subject.getUnits();

                model.addRow(new Object[]{
                        safeText(subject.getSubjectCode(), "N/A"),
                        safeText(subject.getSubjectName(), "N/A"),
                        units,
                        safeText(section.getSectionCode(), "N/A"),
                        scheduleDisplay,
                        slotsDisplay,
                        offering.getId(),
                        subject.getId()
                });
        }

	private void initStudentData(Student student) {
		txtStudentID.setText(student.getStudentId());
		txtFirstName.setText(student.getFirstName());
		txtLastName.setText(student.getLastName());
		txtMiddleName.setText(student.getMiddleName() != null ? student.getMiddleName() : "");
		txtEmailAddress.setText(UserSession.getInstance().getUserInformation().getEmail());
		txtBirthDate.setText(student.getBirthdate() != null ? student.getBirthdate().toString() : "");
		txtStudentStatus.setText(student.getStudentStatus().toString());
		txtYearLevel.setText(student.getYearLevel() != null ? student.getYearLevel().toString() : "");
                txtCourse.setText("N/A");

		Optional<Course> course = CourseService.getInstance().getCourseById(student.getCourseId());
		course.ifPresent(c -> txtCourse.setText(c.getCourseName()));

		List<Enrollment> enrollments = EnrollmentService.getInstance().getEnrollmentsByStudent(student.getStudentId());
		if (enrollments != null && !enrollments.isEmpty()) {
                        Enrollment latest = enrollments.get(0);
                        txtEnrollmentStatus.setText(safeText(latest.getStatus() == null ? null : latest.getStatus().name(), "NOT ENROLLED"));
                        txtTotalUnits.setText(formatUnits(latest.getTotalUnits() == null ? 0.0f : latest.getTotalUnits()));

			List<EnrollmentDetail> details = EnrollmentDetailService.getInstance().getEnrollmentDetailsByEnrollment(latest.getId());
                        long selectedCount = details.stream().filter(detail -> detail.getStatus() == EnrollmentDetailStatus.SELECTED).count();
                        txtTotalSubjects.setText(String.valueOf(selectedCount));

                        String sectionCode = "None";
                        for (EnrollmentDetail detail : details) {
                                if (detail.getStatus() != EnrollmentDetailStatus.SELECTED) {
                                        continue;
                                }

                                Optional<Offering> offering = OfferingService.getInstance().getOfferingById(detail.getOfferingId());
                                if (offering.isEmpty()) {
                                        continue;
                                }

                                Optional<Section> section = SectionService.getInstance().getSectionById(offering.get().getSectionId());
                                if (section.isPresent()) {
                                        sectionCode = section.get().getSectionCode();
                                        break;
                                }
                        }

                        txtSections.setText(sectionCode);
                        updateEnrollmentStatusPresentation(latest);
		} else {
			txtEnrollmentStatus.setText("NOT ENROLLED");
                        txtTotalUnits.setText(formatUnits(0.0f));
			txtTotalSubjects.setText("0");
			txtSections.setText("None");
                        updateEnrollmentStatusPresentation(null);
		}

		txtStudentID.setEditable(false);
		txtFirstName.setEditable(false);
		txtLastName.setEditable(false);
		txtMiddleName.setEditable(false);
		txtEmailAddress.setEditable(false);
		txtBirthDate.setEditable(false);
		txtStudentStatus.setEditable(false);
		txtYearLevel.setEditable(false);
		txtCourse.setEditable(false);
		txtTotalSubjects.setEditable(false);
		txtTotalUnits.setEditable(false);
		txtEnrollmentStatus.setEditable(false);
		txtSections.setEditable(false);
	}

        private void updateEnrollmentStatusPresentation(Enrollment enrollment) {
                if (enrollment == null || enrollment.getStatus() == null) {
                        jLabel3.setText("Status: Not Enrolled");
                        pBarRegistration.setValue(0);
                        pBarRegistration.setStringPainted(true);
                        pBarRegistration.setString("0%");
                        return;
                }

                int progress = switch (enrollment.getStatus()) {
                        case DRAFT -> 25;
                        case SUBMITTED -> 50;
                        case APPROVED -> 75;
                        case ENROLLED -> 100;
                        case CANCELLED -> 0;
                };

                jLabel3.setText("Status: " + enrollment.getStatus().name());
                pBarRegistration.setValue(progress);
                pBarRegistration.setStringPainted(true);
                pBarRegistration.setString(progress + "%");
        }

        private int normalizeCapacity(Integer capacity) {
                return capacity == null ? 0 : capacity;
        }

        private String safeText(String value, String fallback) {
                if (value == null || value.trim().isEmpty()) {
                        return fallback;
                }

                return value.trim();
        }

        private float parseUnitsCell(Object value) {
                if (value instanceof Number number) {
                        return number.floatValue();
                }

                if (value == null) {
                        return 0.0f;
                }

                try {
                        return Float.parseFloat(value.toString().trim());
                } catch (NumberFormatException ex) {
                        return 0.0f;
                }
        }

        private String formatUnits(float units) {
                return String.format("%.1f", units);
        }

	/**
	 * This method is called from within the constructor to initialize the
	 * form. WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                windowBar1 = new com.group5.paul_esys.components.WindowBar();
                tabbedPane = new javax.swing.JTabbedPane();
                panelDashboard = new javax.swing.JPanel();
                panelAcademicOverview = new javax.swing.JPanel();
                jLabel18 = new javax.swing.JLabel();
                txtStudentID = new javax.swing.JTextField();
                txtEmailAddress = new javax.swing.JTextField();
                jLabel19 = new javax.swing.JLabel();
                jLabel20 = new javax.swing.JLabel();
                txtFirstName = new javax.swing.JTextField();
                txtMiddleName = new javax.swing.JTextField();
                jLabel21 = new javax.swing.JLabel();
                txtLastName = new javax.swing.JTextField();
                jLabel22 = new javax.swing.JLabel();
                txtBirthDate = new javax.swing.JTextField();
                jLabel23 = new javax.swing.JLabel();
                jLabel1 = new javax.swing.JLabel();
                jLabel4 = new javax.swing.JLabel();
                txtStudentStatus = new javax.swing.JTextField();
                jLabel24 = new javax.swing.JLabel();
                txtCourse = new javax.swing.JTextField();
                jLabel25 = new javax.swing.JLabel();
                txtYearLevel = new javax.swing.JTextField();
                jLabel26 = new javax.swing.JLabel();
                jLabel8 = new javax.swing.JLabel();
                jLabel9 = new javax.swing.JLabel();
                jLabel27 = new javax.swing.JLabel();
                txtTotalSubjects = new javax.swing.JTextField();
                txtTotalUnits = new javax.swing.JTextField();
                jLabel28 = new javax.swing.JLabel();
                txtEnrollmentStatus = new javax.swing.JTextField();
                jLabel29 = new javax.swing.JLabel();
                txtSections = new javax.swing.JTextField();
                jLabel30 = new javax.swing.JLabel();
                panelEnrollmentProgress = new javax.swing.JPanel();
                pBarRegistration = new javax.swing.JProgressBar();
                jLabel5 = new javax.swing.JLabel();
                jLabel6 = new javax.swing.JLabel();
                jLabel7 = new javax.swing.JLabel();
                jLabel2 = new javax.swing.JLabel();
                jLabel10 = new javax.swing.JLabel();
                jLabel11 = new javax.swing.JLabel();
                panelCourseRegistration = new javax.swing.JPanel();
                jLabel13 = new javax.swing.JLabel();
                txtSearch = new javax.swing.JTextField();
                jPanel2 = new javax.swing.JPanel();
                jScrollPane1 = new javax.swing.JScrollPane();
                tblSubjectCatalog = new javax.swing.JTable();
                jLabel14 = new javax.swing.JLabel();
                btnSearchSubject = new javax.swing.JButton();
                jPanel8 = new javax.swing.JPanel();
                jLabel15 = new javax.swing.JLabel();
                jSeparator1 = new javax.swing.JSeparator();
                jLabel16 = new javax.swing.JLabel();
                jLabel17 = new javax.swing.JLabel();
                jSeparator2 = new javax.swing.JSeparator();
                btnSubmitSchedule = new javax.swing.JButton();
                jScrollPane3 = new javax.swing.JScrollPane();
                jList1 = new javax.swing.JList<>();
                labelAnnouncement = new javax.swing.JLabel();
                panelMySchedule = new javax.swing.JPanel();
                jPanel9 = new javax.swing.JPanel();
                jPanel10 = new javax.swing.JPanel();
                jLabel3 = new javax.swing.JLabel();
                jScrollPane2 = new javax.swing.JScrollPane();
                tableSchedules = new javax.swing.JTable();
                jLabel12 = new javax.swing.JLabel();
                jButton4 = new javax.swing.JButton();
                jButton5 = new javax.swing.JButton();

                setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
                setPreferredSize(new java.awt.Dimension(1280, 720));
                setResizable(false);
                getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.Y_AXIS));

                windowBar1.setTitle("Student Dashboard");
                getContentPane().add(windowBar1);

                tabbedPane.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(225, 225, 225)));
                tabbedPane.setTabPlacement(javax.swing.JTabbedPane.LEFT);
                tabbedPane.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N

                panelDashboard.setBackground(new java.awt.Color(255, 255, 255));

                panelAcademicOverview.setBackground(new java.awt.Color(255, 255, 255));
                panelAcademicOverview.setBorder(new com.group5.paul_esys.ui.RoundShadowBorder());

                jLabel18.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel18.setText("Student ID");

                txtStudentID.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtStudentID.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                txtEmailAddress.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtEmailAddress.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                jLabel19.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel19.setText("Email Address");

                jLabel20.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel20.setText("First Name");

                txtFirstName.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtFirstName.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                txtMiddleName.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtMiddleName.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                jLabel21.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel21.setText("Middle Name");

                txtLastName.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtLastName.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                jLabel22.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel22.setText("Last Name");

                txtBirthDate.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtBirthDate.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                jLabel23.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel23.setText("Birth Date");

                jLabel1.setFont(new java.awt.Font("Poppins", 0, 18)); // NOI18N
                jLabel1.setForeground(new java.awt.Color(0, 0, 102));
                jLabel1.setText("Student Information");

                jLabel4.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel4.setForeground(new java.awt.Color(153, 153, 153));
                jLabel4.setText("Read your information on the fly");

                txtStudentStatus.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtStudentStatus.setText("REGULAR");
                txtStudentStatus.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                jLabel24.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel24.setText("Student Status");

                txtCourse.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtCourse.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                jLabel25.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel25.setText("Course / Program");

                txtYearLevel.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtYearLevel.setText("REGULAR");
                txtYearLevel.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                jLabel26.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel26.setText("Year Level");

                jLabel8.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel8.setForeground(new java.awt.Color(153, 153, 153));
                jLabel8.setText("See Enrollment Activity");

                jLabel9.setFont(new java.awt.Font("Poppins", 0, 18)); // NOI18N
                jLabel9.setForeground(new java.awt.Color(0, 0, 102));
                jLabel9.setText("Academic Overview");

                jLabel27.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel27.setText("Total Subjects");

                txtTotalSubjects.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtTotalSubjects.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                txtTotalUnits.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtTotalUnits.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                jLabel28.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel28.setText("Total Units");

                txtEnrollmentStatus.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtEnrollmentStatus.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                jLabel29.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel29.setText("Enrollment Status");

                txtSections.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtSections.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                jLabel30.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel30.setText("Current Section");

                javax.swing.GroupLayout panelAcademicOverviewLayout = new javax.swing.GroupLayout(panelAcademicOverview);
                panelAcademicOverview.setLayout(panelAcademicOverviewLayout);
                panelAcademicOverviewLayout.setHorizontalGroup(
                        panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelAcademicOverviewLayout.createSequentialGroup()
                                .addGap(36, 36, 36)
                                .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelAcademicOverviewLayout.createSequentialGroup()
                                                .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel8, javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel9, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 612, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(jLabel27, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 205, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelAcademicOverviewLayout.createSequentialGroup()
                                                .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addGroup(panelAcademicOverviewLayout.createSequentialGroup()
                                                                .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                        .addComponent(jLabel20, javax.swing.GroupLayout.PREFERRED_SIZE, 205, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                        .addComponent(txtFirstName, javax.swing.GroupLayout.PREFERRED_SIZE, 292, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                        .addGroup(panelAcademicOverviewLayout.createSequentialGroup()
                                                                                .addGap(0, 4, Short.MAX_VALUE)
                                                                                .addComponent(txtMiddleName, javax.swing.GroupLayout.PREFERRED_SIZE, 334, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                        .addComponent(jLabel21, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                        .addComponent(jLabel22, javax.swing.GroupLayout.PREFERRED_SIZE, 228, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                        .addComponent(txtLastName, javax.swing.GroupLayout.PREFERRED_SIZE, 370, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                        .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addGroup(panelAcademicOverviewLayout.createSequentialGroup()
                                                                .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                                        .addComponent(txtStudentID)
                                                                        .addComponent(jLabel18, javax.swing.GroupLayout.DEFAULT_SIZE, 500, Short.MAX_VALUE))
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                        .addComponent(jLabel19, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                        .addComponent(txtEmailAddress))))
                                                .addGap(35, 35, 35))
                                        .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                .addGroup(panelAcademicOverviewLayout.createSequentialGroup()
                                                        .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                                .addComponent(jLabel25, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 205, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addComponent(txtCourse)
                                                                .addComponent(jLabel23, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                .addComponent(txtBirthDate, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 501, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                        .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                .addComponent(jLabel26, javax.swing.GroupLayout.PREFERRED_SIZE, 198, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelAcademicOverviewLayout.createSequentialGroup()
                                                                        .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                                                .addComponent(txtYearLevel, javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(jLabel24, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 501, Short.MAX_VALUE)
                                                                                .addComponent(txtStudentStatus, javax.swing.GroupLayout.Alignment.LEADING))
                                                                        .addContainerGap())))
                                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addGroup(panelAcademicOverviewLayout.createSequentialGroup()
                                                                .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                                        .addGroup(panelAcademicOverviewLayout.createSequentialGroup()
                                                                                .addComponent(jLabel30, javax.swing.GroupLayout.PREFERRED_SIZE, 296, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addGap(203, 203, 203))
                                                                        .addComponent(txtSections))
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(txtEnrollmentStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 502, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, panelAcademicOverviewLayout.createSequentialGroup()
                                                                .addComponent(txtTotalSubjects, javax.swing.GroupLayout.PREFERRED_SIZE, 500, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addGap(9, 9, 9)
                                                                .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                                        .addComponent(jLabel29, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                        .addComponent(txtTotalUnits)
                                                                        .addComponent(jLabel28, javax.swing.GroupLayout.DEFAULT_SIZE, 499, Short.MAX_VALUE)))))))
                );
                panelAcademicOverviewLayout.setVerticalGroup(
                        panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelAcademicOverviewLayout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addGroup(panelAcademicOverviewLayout.createSequentialGroup()
                                                .addComponent(jLabel18)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtStudentID, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(panelAcademicOverviewLayout.createSequentialGroup()
                                                .addComponent(jLabel19)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtEmailAddress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel20)
                                        .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(jLabel21)
                                                .addComponent(jLabel22)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(txtLastName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(txtFirstName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(txtMiddleName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelAcademicOverviewLayout.createSequentialGroup()
                                                .addComponent(jLabel23)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtBirthDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addGroup(panelAcademicOverviewLayout.createSequentialGroup()
                                                                .addComponent(jLabel25)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(txtCourse, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                        .addGroup(panelAcademicOverviewLayout.createSequentialGroup()
                                                                .addComponent(jLabel26)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(txtYearLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel9)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel8)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jLabel27)
                                                        .addComponent(jLabel28))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(txtTotalSubjects, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(txtTotalUnits, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jLabel30)
                                                        .addComponent(jLabel29))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(txtSections, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(txtEnrollmentStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                        .addGroup(panelAcademicOverviewLayout.createSequentialGroup()
                                                .addComponent(jLabel24)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtStudentStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap(8, Short.MAX_VALUE))
                );

                panelEnrollmentProgress.setBorder(new com.group5.paul_esys.ui.RoundShadowBorder());

                pBarRegistration.setToolTipText("");
                pBarRegistration.setValue(50);
                pBarRegistration.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

                jLabel5.setText("Registration");

                jLabel6.setText("Approval");

                jLabel7.setText("Enrolled");

                jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
                jLabel2.setForeground(new java.awt.Color(0, 0, 102));
                jLabel2.setText("Enrollment Progress");

                jLabel10.setText("Draft");

                jLabel11.setText("Submitted");

                javax.swing.GroupLayout panelEnrollmentProgressLayout = new javax.swing.GroupLayout(panelEnrollmentProgress);
                panelEnrollmentProgress.setLayout(panelEnrollmentProgressLayout);
                panelEnrollmentProgressLayout.setHorizontalGroup(
                        panelEnrollmentProgressLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelEnrollmentProgressLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelEnrollmentProgressLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelEnrollmentProgressLayout.createSequentialGroup()
                                                .addComponent(jLabel2)
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addComponent(pBarRegistration, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelEnrollmentProgressLayout.createSequentialGroup()
                                                .addComponent(jLabel5)
                                                .addGap(169, 169, 169)
                                                .addComponent(jLabel10)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(jLabel11)
                                                .addGap(218, 218, 218)
                                                .addComponent(jLabel6)
                                                .addGap(214, 214, 214)
                                                .addComponent(jLabel7)))
                                .addContainerGap())
                );
                panelEnrollmentProgressLayout.setVerticalGroup(
                        panelEnrollmentProgressLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelEnrollmentProgressLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(5, 5, 5)
                                .addGroup(panelEnrollmentProgressLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel5)
                                        .addComponent(jLabel6)
                                        .addComponent(jLabel7)
                                        .addComponent(jLabel10)
                                        .addComponent(jLabel11))
                                .addGap(4, 4, 4)
                                .addComponent(pBarRegistration, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
                );

                javax.swing.GroupLayout panelDashboardLayout = new javax.swing.GroupLayout(panelDashboard);
                panelDashboard.setLayout(panelDashboardLayout);
                panelDashboardLayout.setHorizontalGroup(
                        panelDashboardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelDashboardLayout.createSequentialGroup()
                                .addGap(15, 15, 15)
                                .addGroup(panelDashboardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(panelAcademicOverview, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(panelEnrollmentProgress, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
                );
                panelDashboardLayout.setVerticalGroup(
                        panelDashboardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelDashboardLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(panelEnrollmentProgress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(panelAcademicOverview, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
                );

                tabbedPane.addTab("Dashboard", panelDashboard);

                panelCourseRegistration.setBackground(new java.awt.Color(255, 255, 255));
                panelCourseRegistration.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(225, 225, 225)));

                jLabel13.setFont(new java.awt.Font("Poppins", 0, 18)); // NOI18N
                jLabel13.setText("Search Subjects");

                txtSearch.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtSearch.setToolTipText("Search for Subjects");
                txtSearch.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());
                txtSearch.addActionListener(this::txtSearchActionPerformed);

                jPanel2.setBackground(new java.awt.Color(255, 255, 255));
                jPanel2.setBorder(new com.group5.paul_esys.ui.RoundShadowBorder());

                tblSubjectCatalog.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                tblSubjectCatalog.setModel(new javax.swing.table.DefaultTableModel(
                        new Object [][] {
                                {null, null, null, null},
                                {null, null, null, null},
                                {null, null, null, null},
                                {null, null, null, null}
                        },
                        new String [] {
                                "Subject Name", "Code", "Units", "Description"
                        }
                ) {
                        Class[] types = new Class [] {
                                java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class
                        };

                        public Class getColumnClass(int columnIndex) {
                                return types [columnIndex];
                        }
                });
                jScrollPane1.setViewportView(tblSubjectCatalog);

                jLabel14.setText("Subject Catalog");

                javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
                jPanel2.setLayout(jPanel2Layout);
                jPanel2Layout.setHorizontalGroup(
                        jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 688, Short.MAX_VALUE)
                                        .addComponent(jLabel14, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
                );
                jPanel2Layout.setVerticalGroup(
                        jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(13, 13, 13)
                                .addComponent(jLabel14)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 543, Short.MAX_VALUE)
                                .addContainerGap())
                );

                btnSearchSubject.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                btnSearchSubject.setText("Search");
                btnSearchSubject.addActionListener(this::btnSearchSubjectActionPerformed);

                jPanel8.setBackground(new java.awt.Color(255, 255, 255));
                jPanel8.setBorder(new com.group5.paul_esys.ui.RoundShadowBorder());

                jLabel15.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel15.setText("Selected");

                jLabel16.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel16.setText("Selected");

                jLabel17.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel17.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel17.setText("0 / n units");

                btnSubmitSchedule.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                btnSubmitSchedule.setText("Submit Schedule");
                btnSubmitSchedule.addActionListener(this::btnSubmitScheduleActionPerformed);

                jList1.setModel(new javax.swing.AbstractListModel<String>() {
                        String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
                        public int getSize() { return strings.length; }
                        public String getElementAt(int i) { return strings[i]; }
                });
                jScrollPane3.setViewportView(jList1);

                javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
                jPanel8.setLayout(jPanel8Layout);
                jPanel8Layout.setHorizontalGroup(
                        jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel8Layout.createSequentialGroup()
                                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addGroup(jPanel8Layout.createSequentialGroup()
                                                .addGap(157, 157, 157)
                                                .addComponent(jLabel15)
                                                .addGap(0, 150, Short.MAX_VALUE)))
                                .addContainerGap())
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel8Layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(btnSubmitSchedule, javax.swing.GroupLayout.PREFERRED_SIZE, 219, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(67, 67, 67))
                        .addGroup(jPanel8Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jSeparator2)
                                        .addComponent(jLabel17, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(jPanel8Layout.createSequentialGroup()
                                                .addGap(151, 151, 151)
                                                .addComponent(jLabel16)
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.TRAILING))
                                .addContainerGap())
                );
                jPanel8Layout.setVerticalGroup(
                        jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel8Layout.createSequentialGroup()
                                .addGap(11, 11, 11)
                                .addComponent(jLabel15)
                                .addGap(11, 11, 11)
                                .addComponent(jScrollPane3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel16)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel17)
                                .addGap(18, 18, 18)
                                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(32, 32, 32)
                                .addComponent(btnSubmitSchedule)
                                .addGap(54, 54, 54))
                );

                labelAnnouncement.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                labelAnnouncement.setForeground(new java.awt.Color(119, 0, 0));

                javax.swing.GroupLayout panelCourseRegistrationLayout = new javax.swing.GroupLayout(panelCourseRegistration);
                panelCourseRegistration.setLayout(panelCourseRegistrationLayout);
                panelCourseRegistrationLayout.setHorizontalGroup(
                        panelCourseRegistrationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelCourseRegistrationLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelCourseRegistrationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelCourseRegistrationLayout.createSequentialGroup()
                                                .addGroup(panelCourseRegistrationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addGroup(panelCourseRegistrationLayout.createSequentialGroup()
                                                                .addComponent(txtSearch)
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(btnSearchSubject, javax.swing.GroupLayout.PREFERRED_SIZE, 161, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(panelCourseRegistrationLayout.createSequentialGroup()
                                                .addComponent(jLabel13)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(labelAnnouncement, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                .addContainerGap())
                );
                panelCourseRegistrationLayout.setVerticalGroup(
                        panelCourseRegistrationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelCourseRegistrationLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelCourseRegistrationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel13)
                                        .addComponent(labelAnnouncement))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelCourseRegistrationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelCourseRegistrationLayout.createSequentialGroup()
                                                .addGroup(panelCourseRegistrationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(btnSearchSubject))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
                );

                tabbedPane.addTab("Course Registration", panelCourseRegistration);

                panelMySchedule.setBackground(new java.awt.Color(255, 255, 255));
                panelMySchedule.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(225, 225, 225)));

                jPanel9.setBackground(new java.awt.Color(255, 255, 255));
                jPanel9.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

                jPanel10.setBackground(new java.awt.Color(247, 231, 244));
                jPanel10.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
                java.awt.FlowLayout flowLayout1 = new java.awt.FlowLayout(java.awt.FlowLayout.LEFT);
                flowLayout1.setAlignOnBaseline(true);
                jPanel10.setLayout(flowLayout1);

                jLabel3.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel3.setText("Status: Pending Approval");
                jPanel10.add(jLabel3);

                tableSchedules.setModel(new javax.swing.table.DefaultTableModel(
                        new Object [][] {
                                {null, null, null, null, null, null},
                                {null, null, null, null, null, null},
                                {null, null, null, null, null, null},
                                {null, null, null, null, null, null}
                        },
                        new String [] {
                                "Code", "Course Name", "Instructor", "Schedule", "Room", "Credits"
                        }
                ));
                jScrollPane2.setViewportView(tableSchedules);

                javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
                jPanel9.setLayout(jPanel9Layout);
                jPanel9Layout.setHorizontalGroup(
                        jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel9Layout.createSequentialGroup()
                                .addGap(14, 14, 14)
                                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 1078, Short.MAX_VALUE)
                                        .addComponent(jPanel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
                );
                jPanel9Layout.setVerticalGroup(
                        jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel9Layout.createSequentialGroup()
                                .addGap(16, 16, 16)
                                .addComponent(jPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 561, Short.MAX_VALUE)
                                .addContainerGap())
                );

                jLabel12.setFont(new java.awt.Font("Verdana", 0, 12)); // NOI18N
                jLabel12.setText("Class Schedule");

                jButton4.setFont(new java.awt.Font("Trebuchet MS", 0, 12)); // NOI18N
                jButton4.setText("Print Schedule");

                jButton5.setFont(new java.awt.Font("Trebuchet MS", 0, 12)); // NOI18N
                jButton5.setText("Export PDF");

                javax.swing.GroupLayout panelMyScheduleLayout = new javax.swing.GroupLayout(panelMySchedule);
                panelMySchedule.setLayout(panelMyScheduleLayout);
                panelMyScheduleLayout.setHorizontalGroup(
                        panelMyScheduleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelMyScheduleLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelMyScheduleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jPanel9, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelMyScheduleLayout.createSequentialGroup()
                                                .addComponent(jLabel12)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(jButton5)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jButton4)))
                                .addContainerGap())
                );
                panelMyScheduleLayout.setVerticalGroup(
                        panelMyScheduleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelMyScheduleLayout.createSequentialGroup()
                                .addGroup(panelMyScheduleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelMyScheduleLayout.createSequentialGroup()
                                                .addGap(8, 8, 8)
                                                .addGroup(panelMyScheduleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jButton4)
                                                        .addComponent(jButton5))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED))
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelMyScheduleLayout.createSequentialGroup()
                                                .addContainerGap()
                                                .addComponent(jLabel12)
                                                .addGap(3, 3, 3)))
                                .addComponent(jPanel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
                );

                tabbedPane.addTab("My Schedule", panelMySchedule);

                getContentPane().add(tabbedPane);

                pack();
        }// </editor-fold>//GEN-END:initComponents

	private void txtSearchActionPerformed(java.awt.event.ActionEvent evt) {
//GEN-FIRST:event_txtSearchActionPerformed
	  loadSubjectCatalog(txtSearch.getText());
  }//GEN-LAST:event_txtSearchActionPerformed

        private void btnSearchSubjectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchSubjectActionPerformed
		loadSubjectCatalog(txtSearch.getText());
        }//GEN-LAST:event_btnSearchSubjectActionPerformed

        private void btnSubmitScheduleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSubmitScheduleActionPerformed
                if (!hasActiveEnrollmentPeriod) {
                        JOptionPane.showMessageDialog(
                          this,
                          "Enrollment is closed. You can preview offerings, but submission requires an active enrollment period.",
                          "Enrollment Closed",
                          JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                int[] selectedRows = tblSubjectCatalog.getSelectedRows();
                if (selectedRows == null || selectedRows.length == 0) {
                        JOptionPane.showMessageDialog(this, "Please select offerings to enroll.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                }

                Optional<Long> activeEnrollmentPeriodId = EnrollmentPeriodService.getInstance()
                  .getCurrentEnrollmentPeriod()
                  .map(EnrollmentPeriod::getId);
                if (activeEnrollmentPeriodId.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "No active enrollment period.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                }

                DefaultTableModel catalogModel = (DefaultTableModel) tblSubjectCatalog.getModel();

                List<Schedule> selectedSchedules = new ArrayList<>();
                List<OfferingSelection> selectedOfferings = new ArrayList<>();
                for (int selectedRow : selectedRows) {
                        int modelRow = tblSubjectCatalog.convertRowIndexToModel(selectedRow);
                        Long offeringId = parseLongCell(catalogModel.getValueAt(modelRow, CATALOG_COL_OFFERING_ID));
                        if (offeringId == null) {
                                JOptionPane.showMessageDialog(this, "Invalid offering selection.", "Error", JOptionPane.ERROR_MESSAGE);
                                return;
                        }

                        Optional<Offering> offeringOpt = OfferingService.getInstance().getOfferingById(offeringId);
                        if (offeringOpt.isEmpty()) {
                                JOptionPane.showMessageDialog(this, "Selected offering no longer exists.", "Error", JOptionPane.ERROR_MESSAGE);
                                return;
                        }

                        Offering offering = offeringOpt.get();
                        Optional<Section> sectionOpt = SectionService.getInstance().getSectionById(offering.getSectionId());
                        String sectionLabel = sectionOpt.map(Section::getSectionCode).orElse("Section #" + offering.getSectionId());

                        int capacity = normalizeCapacity(
                          offering.getCapacity() != null ? offering.getCapacity() : sectionOpt.map(Section::getCapacity).orElse(0)
                        );
                        long enrolledCount = EnrollmentDetailService.getInstance().countSelectedByOffering(offering.getId());
                        if (capacity > 0 && enrolledCount >= capacity) {
                                JOptionPane.showMessageDialog(
                                  this,
                                  "Offering for " + sectionLabel + " is full. Capacity: " + capacity,
                                  "Capacity Conflict",
                                  JOptionPane.ERROR_MESSAGE
                                );
                                return;
                        }

                        List<Schedule> schedules = ScheduleService.getInstance().getSchedulesByOffering(offering.getId());
                        for (Schedule newSchedule : schedules) {
                                if (newSchedule.getStartTime() == null || newSchedule.getEndTime() == null) {
                                        continue;
                                }

                                for (Schedule existingSchedule : selectedSchedules) {
                                        if (existingSchedule.getStartTime() == null || existingSchedule.getEndTime() == null) {
                                                continue;
                                        }

                                        if (newSchedule.getDay() != existingSchedule.getDay()) {
                                                continue;
                                        }

                                        long start1 = newSchedule.getStartTime().getTime();
                                        long end1 = newSchedule.getEndTime().getTime();
                                        long start2 = existingSchedule.getStartTime().getTime();
                                        long end2 = existingSchedule.getEndTime().getTime();
                                        if (start1 < end2 && end1 > start2) {
                                                JOptionPane.showMessageDialog(
                                                  this,
                                                  "Schedule conflict at " + newSchedule.getDay() + " for " + sectionLabel,
                                                  "Schedule Conflict",
                                                  JOptionPane.ERROR_MESSAGE
                                                );
                                                return;
                                        }
                                }
                        }

                        selectedSchedules.addAll(schedules);
                        float units = parseUnitsCell(catalogModel.getValueAt(modelRow, CATALOG_COL_UNITS));
                        selectedOfferings.add(new OfferingSelection(offering, units));
                }

                Enrollment activeEnrollment = resolveOrCreateEnrollment(activeEnrollmentPeriodId.get());
                if (activeEnrollment == null) {
                        return;
                }

                Map<Long, EnrollmentDetail> existingDetailsByOfferingId = new HashMap<>();
                for (EnrollmentDetail detail : EnrollmentDetailService.getInstance().getEnrollmentDetailsByEnrollment(activeEnrollment.getId())) {
                        existingDetailsByOfferingId.put(detail.getOfferingId(), detail);
                }

                for (OfferingSelection selection : selectedOfferings) {
                        EnrollmentDetail existingDetail = existingDetailsByOfferingId.get(selection.offering.getId());
                        boolean persisted;

                        if (existingDetail == null) {
                                EnrollmentDetail detail = new EnrollmentDetail();
                                detail.setEnrollmentId(activeEnrollment.getId());
                                detail.setOfferingId(selection.offering.getId());
                                detail.setUnits(selection.units);
                                detail.setStatus(EnrollmentDetailStatus.SELECTED);
                                persisted = EnrollmentDetailService.getInstance().createEnrollmentDetail(detail);
                        } else {
                                existingDetail.setUnits(selection.units);
                                existingDetail.setStatus(EnrollmentDetailStatus.SELECTED);
                                persisted = EnrollmentDetailService.getInstance().updateEnrollmentDetail(existingDetail);
                        }

                        if (!persisted) {
                                JOptionPane.showMessageDialog(this, "Failed to persist enrollment detail.", "Error", JOptionPane.ERROR_MESSAGE);
                                return;
                        }

                        if (selection.offering.getSemesterSubjectId() != null) {
                                StudentEnrolledSubjectService.getInstance().upsertStatus(
                                  currentStudent.getStudentId(),
                                  activeEnrollment.getId(),
                                  selection.offering.getId(),
                                  selection.offering.getSemesterSubjectId(),
                                  StudentEnrolledSubjectStatus.ENROLLED
                                );
                        }
                }

                activeEnrollment.setStatus(EnrollmentStatus.SUBMITTED);
                activeEnrollment.setMaxUnits(MAX_ENROLLMENT_UNITS);
                activeEnrollment.setTotalUnits(sumSelectedUnits(activeEnrollment.getId()));
                if (activeEnrollment.getSubmittedAt() == null) {
                        activeEnrollment.setSubmittedAt(new Date());
                }

                if (!EnrollmentService.getInstance().updateEnrollment(activeEnrollment)) {
                        JOptionPane.showMessageDialog(this, "Failed to update enrollment summary.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                }

                JOptionPane.showMessageDialog(this, "Enrollment submitted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                reloadStudentDashboardData();
        }//GEN-LAST:event_btnSubmitScheduleActionPerformed

	private void loadMySchedule() {
                DefaultTableModel model = (DefaultTableModel) tableSchedules.getModel();
		model.setRowCount(0);

                List<Enrollment> studentEr = EnrollmentService.getInstance().getEnrollmentsByStudent(currentStudent.getStudentId());
		if (studentEr == null || studentEr.isEmpty()) {
                        updateEnrollmentStatusPresentation(null);
			return;
		}

                Enrollment active = studentEr.get(0);
                updateEnrollmentStatusPresentation(active);
                List<EnrollmentDetail> details = EnrollmentDetailService.getInstance().getEnrollmentDetailsByEnrollment(active.getId());

		for (EnrollmentDetail ed : details) {
			if (ed.getStatus() != EnrollmentDetailStatus.SELECTED) {
				continue;
			}

                        Optional<Offering> offering = OfferingService.getInstance().getOfferingById(ed.getOfferingId());
                        if (offering.isEmpty()) {
                                continue;
                        }

                        Optional<Subject> subject = SubjectService.getInstance().getSubjectById(offering.get().getSubjectId());
                        Optional<Section> section = SectionService.getInstance().getSectionById(offering.get().getSectionId());
                        List<Schedule> schedules = ScheduleService.getInstance().getSchedulesByOffering(offering.get().getId());

			StringBuilder schedString = new StringBuilder();
			StringBuilder roomString = new StringBuilder();
			StringBuilder facultyString = new StringBuilder();

			for (Schedule sched : schedules) {
                                if (sched.getStartTime() == null || sched.getEndTime() == null) {
                                        continue;
                                }

				schedString.append(sched.getDay().toString()).append(" ")
				  .append(sched.getStartTime().toString().substring(0, 5)).append("-")
				  .append(sched.getEndTime().toString().substring(0, 5)).append(" ");

				com.group5.paul_esys.modules.rooms.services.RoomService.getInstance().getRoomById(sched.getRoomId())
				  .ifPresent(r -> roomString.append(r.getRoom()).append(" "));

				com.group5.paul_esys.modules.faculty.services.FacultyService.getInstance().getFacultyById(sched.getFacultyId())
				  .ifPresent(f -> facultyString.append(f.getLastName()).append(", ").append(f.getFirstName()).append(" "));
			}

			if (subject.isPresent() && section.isPresent()) {
                                String scheduleValue = schedString.length() == 0
                                  ? safeText(section.get().getSectionCode(), "N/A") + " | TBA"
                                  : safeText(section.get().getSectionCode(), "N/A") + " | " + schedString.toString().trim();
                                String roomValue = roomString.length() == 0 ? "TBA" : roomString.toString().trim();
                                String facultyValue = facultyString.length() == 0 ? "TBA" : facultyString.toString().trim();
                                float units = ed.getUnits() == null
                                  ? (subject.get().getUnits() == null ? 0.0f : subject.get().getUnits())
                                  : ed.getUnits();

				model.addRow(new Object[]{
                                        safeText(subject.get().getSubjectCode(), "N/A"),
                                        safeText(subject.get().getSubjectName(), "N/A"),
                                        facultyValue,
                                        scheduleValue,
                                        roomValue,
                                        formatUnits(units)
				});
			}
		}
	}

        private Enrollment resolveOrCreateEnrollment(Long enrollmentPeriodId) {
                List<Enrollment> studentEnrollments = EnrollmentService.getInstance().getEnrollmentsByStudent(currentStudent.getStudentId());
                for (Enrollment enrollment : studentEnrollments) {
                        if (!enrollmentPeriodId.equals(enrollment.getEnrollmentPeriodId())) {
                                continue;
                        }

                        if (enrollment.getStatus() == EnrollmentStatus.APPROVED || enrollment.getStatus() == EnrollmentStatus.ENROLLED) {
                                JOptionPane.showMessageDialog(
                                  this,
                                  "Your enrollment for the active period is already finalized.",
                                  "Enrollment Locked",
                                  JOptionPane.WARNING_MESSAGE
                                );
                                return null;
                        }

                        return enrollment;
                }

                Enrollment enrollment = new Enrollment();
                enrollment.setStudentId(currentStudent.getStudentId());
                enrollment.setEnrollmentPeriodId(enrollmentPeriodId);
                enrollment.setStatus(EnrollmentStatus.SUBMITTED);
                enrollment.setMaxUnits(MAX_ENROLLMENT_UNITS);
                enrollment.setTotalUnits(0.0f);
                enrollment.setSubmittedAt(new Date());

                if (!EnrollmentService.getInstance().createEnrollment(enrollment)) {
                        JOptionPane.showMessageDialog(this, "Failed to create enrollment.", "Error", JOptionPane.ERROR_MESSAGE);
                        return null;
                }

                List<Enrollment> reloadedEnrollments = EnrollmentService.getInstance().getEnrollmentsByStudent(currentStudent.getStudentId());
                for (Enrollment reloaded : reloadedEnrollments) {
                        if (enrollmentPeriodId.equals(reloaded.getEnrollmentPeriodId())) {
                                return reloaded;
                        }
                }

                JOptionPane.showMessageDialog(this, "Enrollment was created but could not be reloaded.", "Error", JOptionPane.ERROR_MESSAGE);
                return null;
        }

        private float sumSelectedUnits(Long enrollmentId) {
                float total = 0.0f;
                List<EnrollmentDetail> details = EnrollmentDetailService.getInstance().getEnrollmentDetailsByEnrollment(enrollmentId);
                for (EnrollmentDetail detail : details) {
                        if (detail.getStatus() == EnrollmentDetailStatus.SELECTED) {
                                total += detail.getUnits() == null ? 0.0f : detail.getUnits();
                        }
                }

                return total;
        }

        private Long parseLongCell(Object value) {
                if (value instanceof Number number) {
                        return number.longValue();
                }

                if (value == null) {
                        return null;
                }

                try {
                        return Long.parseLong(value.toString().trim());
                } catch (NumberFormatException ex) {
                        return null;
                }
        }

        private static final class OfferingSelection {
                private final Offering offering;
                private final float units;

                private OfferingSelection(Offering offering, float units) {
                        this.offering = offering;
                        this.units = units;
                }
        }

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		/* Create and display the form */
		java.awt.EventQueue.invokeLater(()
		  -> new StudentDashboard().setVisible(true)
		);
	}

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton btnSearchSubject;
        private javax.swing.JButton btnSubmitSchedule;
        private javax.swing.JButton jButton4;
        private javax.swing.JButton jButton5;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel10;
        private javax.swing.JLabel jLabel11;
        private javax.swing.JLabel jLabel12;
        private javax.swing.JLabel jLabel13;
        private javax.swing.JLabel jLabel14;
        private javax.swing.JLabel jLabel15;
        private javax.swing.JLabel jLabel16;
        private javax.swing.JLabel jLabel17;
        private javax.swing.JLabel jLabel18;
        private javax.swing.JLabel jLabel19;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel20;
        private javax.swing.JLabel jLabel21;
        private javax.swing.JLabel jLabel22;
        private javax.swing.JLabel jLabel23;
        private javax.swing.JLabel jLabel24;
        private javax.swing.JLabel jLabel25;
        private javax.swing.JLabel jLabel26;
        private javax.swing.JLabel jLabel27;
        private javax.swing.JLabel jLabel28;
        private javax.swing.JLabel jLabel29;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel30;
        private javax.swing.JLabel jLabel4;
        private javax.swing.JLabel jLabel5;
        private javax.swing.JLabel jLabel6;
        private javax.swing.JLabel jLabel7;
        private javax.swing.JLabel jLabel8;
        private javax.swing.JLabel jLabel9;
        private javax.swing.JList<String> jList1;
        private javax.swing.JPanel jPanel10;
        private javax.swing.JPanel jPanel2;
        private javax.swing.JPanel jPanel8;
        private javax.swing.JPanel jPanel9;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JScrollPane jScrollPane2;
        private javax.swing.JScrollPane jScrollPane3;
        private javax.swing.JSeparator jSeparator1;
        private javax.swing.JSeparator jSeparator2;
        private javax.swing.JLabel labelAnnouncement;
        private javax.swing.JProgressBar pBarRegistration;
        private javax.swing.JPanel panelAcademicOverview;
        private javax.swing.JPanel panelCourseRegistration;
        private javax.swing.JPanel panelDashboard;
        private javax.swing.JPanel panelEnrollmentProgress;
        private javax.swing.JPanel panelMySchedule;
        private javax.swing.JTabbedPane tabbedPane;
        private javax.swing.JTable tableSchedules;
        private javax.swing.JTable tblSubjectCatalog;
        private javax.swing.JTextField txtBirthDate;
        private javax.swing.JTextField txtCourse;
        private javax.swing.JTextField txtEmailAddress;
        private javax.swing.JTextField txtEnrollmentStatus;
        private javax.swing.JTextField txtFirstName;
        private javax.swing.JTextField txtLastName;
        private javax.swing.JTextField txtMiddleName;
        private javax.swing.JTextField txtSearch;
        private javax.swing.JTextField txtSections;
        private javax.swing.JTextField txtStudentID;
        private javax.swing.JTextField txtStudentStatus;
        private javax.swing.JTextField txtTotalSubjects;
        private javax.swing.JTextField txtTotalUnits;
        private javax.swing.JTextField txtYearLevel;
        private com.group5.paul_esys.components.WindowBar windowBar1;
        // End of variables declaration//GEN-END:variables
}
