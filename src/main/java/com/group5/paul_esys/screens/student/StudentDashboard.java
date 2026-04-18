/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.group5.paul_esys.screens.student;

import com.group5.paul_esys.modules.courses.model.Course;
import com.group5.paul_esys.modules.courses.services.CourseService;
import com.group5.paul_esys.modules.enrollment_period.model.EnrollmentPeriod;
import com.group5.paul_esys.modules.enrollment_period.services.EnrollmentPeriodService;
import com.group5.paul_esys.modules.enrollments.model.Enrollment;
import com.group5.paul_esys.modules.enrollments.model.EnrollmentDetail;
import com.group5.paul_esys.modules.enrollments.model.StudentEnrolledSubject;
import com.group5.paul_esys.modules.enrollments.model.StudentSemesterProgress;
import com.group5.paul_esys.modules.enrollments.services.EnrollmentDetailService;
import com.group5.paul_esys.modules.enrollments.services.EnrollmentService;
import com.group5.paul_esys.modules.enrollments.services.StudentEnrolledSubjectService;
import com.group5.paul_esys.modules.enrollments.services.StudentEnrollmentEligibilityService;
import com.group5.paul_esys.modules.enrollments.services.StudentSemesterProgressService;
import com.group5.paul_esys.modules.enums.EnrollmentDetailStatus;
import com.group5.paul_esys.modules.enums.EnrollmentStatus;
import com.group5.paul_esys.modules.enums.SemesterProgressStatus;
import com.group5.paul_esys.modules.enums.StudentEnrolledSubjectStatus;
import com.group5.paul_esys.modules.offerings.model.Offering;
import com.group5.paul_esys.modules.offerings.services.OfferingService;
import com.group5.paul_esys.modules.rooms.model.Room;
import com.group5.paul_esys.modules.rooms.services.RoomService;
import com.group5.paul_esys.modules.schedules.model.Schedule;
import com.group5.paul_esys.modules.schedules.services.ScheduleService;
import com.group5.paul_esys.modules.sections.model.Section;
import com.group5.paul_esys.modules.sections.services.SectionService;
import com.group5.paul_esys.modules.semester.model.Semester;
import com.group5.paul_esys.modules.semester.services.SemesterService;
import com.group5.paul_esys.modules.semester_subjects.model.SemesterSubject;
import com.group5.paul_esys.modules.semester_subjects.services.SemesterSubjectService;
import com.group5.paul_esys.modules.students.model.Student;
import com.group5.paul_esys.modules.subjects.model.Subject;
import com.group5.paul_esys.modules.subjects.services.SubjectService;
import com.group5.paul_esys.modules.users.services.UserSession;
import com.group5.paul_esys.screens.sign_in.SignIn;
import com.group5.paul_esys.screens.student.models.*;
import com.group5.paul_esys.utils.ThemeManager;
import java.awt.*;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

/**
 *
 * @author Shan
 */
public class StudentDashboard extends javax.swing.JFrame {

	private static final int CATALOG_COL_SELECTED = 0;
	private static final int CATALOG_COL_CODE = 1;
	private static final int CATALOG_COL_SUBJECT_NAME = 2;
	private static final int CATALOG_COL_UNITS = 3;
	private static final int CATALOG_COL_SECTION = 4;
	private static final int CATALOG_COL_SCHEDULE = 5;
	private static final int CATALOG_COL_SLOTS = 6;
	private static final int CATALOG_COL_OFFERING_ID = 7;
	private static final int CATALOG_COL_SUBJECT_ID = 8;
	private static final float MIN_SUBMISSION_UNITS = 18.0f;
	private static final float MAX_ENROLLMENT_UNITS = 24.0f;
	private static final DateTimeFormatter PROGRESS_TIMESTAMP_FORMATTER = DateTimeFormatter
			.ofPattern("MMM d, yyyy h:mm a");

	private Student currentStudent;

	private final StudentEnrolledSubjectService studentEnrolledSubjectService = StudentEnrolledSubjectService
			.getInstance();
	private final StudentSemesterProgressService semesterProgressService = StudentSemesterProgressService.getInstance();
	private final SemesterService semesterService = SemesterService.getInstance();
	private final SemesterSubjectService semesterSubjectService = SemesterSubjectService.getInstance();
	private final DefaultTableModel semesterProgressModel = new DefaultTableModel(
			new Object[][] {},
			new String[] { "Semester", "Year Level", "Status", "Started At", "Completed At" }) {
		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return String.class;
		}
	};
	private final DefaultTableModel completedSubjectsModel = new DefaultTableModel(
			new Object[][] {},
			new String[] { "Code", "Subject", "Units", "Semester", "Completed At" }) {
		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return String.class;
		}
	};
	private boolean hasActiveEnrollmentPeriod;
	private int activeBackgroundTasks;
	private float selectedCatalogUnits;
	private final Set<Long> transientSelectedOfferingIds = new HashSet<>();
	private final Map<Long, Long> selectedSubjectIdsByOfferingId = new HashMap<>();
	private final Set<Long> duplicateSelectedSubjectIds = new HashSet<>();

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
		ThemeManager.applySavedTheme();

		if (UserSession.getInstance().getUserInformation() == null
				|| !(UserSession.getInstance().getUserInformation().getUser() instanceof Student)) {
			JOptionPane.showMessageDialog(
					null,
					"Your session has expired. Please sign in again.",
					"Session Required",
					JOptionPane.WARNING_MESSAGE);
			SwingUtilities.invokeLater(() -> new SignIn().setVisible(true));
			dispose();
			return;
		}

		Student student = (Student) UserSession.getInstance()
				.getUserInformation()
				.getUser();
		String fullName = String.format(
				"%s, %s %s",
				student.getLastName(),
				student.getFirstName(),
				(student.getMiddleName() != null) ? student.getMiddleName() : "");

		this.setUndecorated(true);
		initComponents();
		this.setLocationRelativeTo(null);
		this.windowBar1.setTitle("Welcome " + fullName);
		this.currentStudent = student;
		initializeDashboardUi();
		initStudentData(student);
		reloadStudentDashboardData();
	}

	private void initializeDashboardUi() {
		configureLogoutAction();
		configureSubjectCatalogTable();
		configureScheduleTable();
		configureSelectedSubjectsPanel();
	}

	private void reloadStudentDashboardData() {
		initStudentData(currentStudent);
		loadSubjectCatalogAsync(txtSearch.getText());
		loadMySchedule();
		loadSemesterProgress();
		loadCompletedSubjects();
	}

	private void beginBackgroundTask() {
		activeBackgroundTasks++;
		updateBackgroundState();
	}

	private void endBackgroundTask() {
		activeBackgroundTasks = Math.max(0, activeBackgroundTasks - 1);
		updateBackgroundState();
	}

	private void updateBackgroundState() {
		boolean busy = activeBackgroundTasks > 0;
		setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
		txtSearch.setEnabled(!busy);
		btnSearchSubject.setEnabled(!busy);
		btnSubmitSchedule.setEnabled(!busy && hasActiveEnrollmentPeriod && hasMinimumSubmissionUnits(selectedCatalogUnits));
		btnSaveDraft.setEnabled(!busy && hasActiveEnrollmentPeriod);
	}

	private void configureLogoutAction() {
		JButton logoutButton = new JButton("Logout");
		logoutButton.setFont(new Font("Poppins", Font.PLAIN, 13));
		logoutButton.putClientProperty("JButton.buttonType", "roundRect");
		logoutButton.putClientProperty("JComponent.minimumWidth", 120);
		logoutButton.addActionListener(evt -> logoutCurrentUser());

		JPanel trailingPanel = new JPanel(new BorderLayout());
		trailingPanel.setOpaque(false);
		trailingPanel.setBorder(BorderFactory.createEmptyBorder(10, 8, 12, 8));
		trailingPanel.add(logoutButton, BorderLayout.SOUTH);

		tabbedPane.putClientProperty("JTabbedPane.trailingComponent", trailingPanel);
	}

	private void logoutCurrentUser() {
		int confirm = JOptionPane.showConfirmDialog(
				this,
				"Are you sure you want to logout?",
				"Confirm Logout",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE);

		if (confirm != JOptionPane.YES_OPTION) {
			return;
		}

		UserSession.getInstance().logout();
		this.dispose();
		SwingUtilities.invokeLater(() -> new SignIn().setVisible(true));
	}

	private <T> void executeDatabaseTask(
			Supplier<T> taskSupplier,
			Consumer<T> successHandler,
			String defaultErrorMessage) {
		beginBackgroundTask();
		SwingWorker<T, Void> worker = new SwingWorker<>() {
			@Override
			protected T doInBackground() {
				return taskSupplier.get();
			}

			@Override
			protected void done() {
				try {
					successHandler.accept(get());
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
					JOptionPane.showMessageDialog(
							StudentDashboard.this,
							defaultErrorMessage,
							"Error",
							JOptionPane.ERROR_MESSAGE);
				} catch (ExecutionException ex) {
					Throwable cause = ex.getCause() == null ? ex : ex.getCause();
					String detailedMessage = cause.getMessage() == null || cause.getMessage().trim().isEmpty()
							? defaultErrorMessage
							: defaultErrorMessage + "\n" + cause.getMessage().trim();
					JOptionPane.showMessageDialog(
							StudentDashboard.this,
							detailedMessage,
							"Error",
							JOptionPane.ERROR_MESSAGE);
				} finally {
					endBackgroundTask();
				}
			}
		};
		worker.execute();
	}

	private void configureSubjectCatalogTable() {
		DefaultTableModel model = new DefaultTableModel(
				new Object[][] {},
				new String[] { "Selected", "Code", "Subject Name", "Units", "Section", "Schedule", "Slots", "Offering ID",
						"Subject ID" }) {
			@Override
			public Class<?> getColumnClass(int columnIndex) {
				return switch (columnIndex) {
					case CATALOG_COL_SELECTED -> Boolean.class;
					case CATALOG_COL_UNITS -> Float.class;
					case CATALOG_COL_OFFERING_ID, CATALOG_COL_SUBJECT_ID -> Long.class;
					default -> String.class;
				};
			}

			@Override
			public boolean isCellEditable(int row, int column) {
				return column == CATALOG_COL_SELECTED;
			}
		};

		tblSubjectCatalog.setModel(model);
		tblSubjectCatalog.setRowHeight(26);
		tblSubjectCatalog.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tblSubjectCatalog.setAutoCreateRowSorter(false);
		tblSubjectCatalog.getTableHeader().setReorderingAllowed(false);
		tblSubjectCatalog.getTableHeader().setResizingAllowed(false);
		configureSubjectCatalogColumns();
		hideTableColumn(tblSubjectCatalog, CATALOG_COL_OFFERING_ID);
		hideTableColumn(tblSubjectCatalog, CATALOG_COL_SUBJECT_ID);
		model.addTableModelListener(evt -> refreshSelectedSubjectsPreview());
	}

	private void configureSubjectCatalogColumns() {
		TableColumn selectedColumn = tblSubjectCatalog.getColumnModel().getColumn(CATALOG_COL_SELECTED);
		selectedColumn.setMinWidth(80);
		selectedColumn.setPreferredWidth(80);
		selectedColumn.setMaxWidth(80);

		tblSubjectCatalog.getColumnModel().getColumn(CATALOG_COL_CODE).setPreferredWidth(110);
		tblSubjectCatalog.getColumnModel().getColumn(CATALOG_COL_SUBJECT_NAME).setPreferredWidth(220);
		tblSubjectCatalog.getColumnModel().getColumn(CATALOG_COL_UNITS).setPreferredWidth(70);
		tblSubjectCatalog.getColumnModel().getColumn(CATALOG_COL_SECTION).setPreferredWidth(90);
		tblSubjectCatalog.getColumnModel().getColumn(CATALOG_COL_SCHEDULE).setPreferredWidth(180);
		tblSubjectCatalog.getColumnModel().getColumn(CATALOG_COL_SLOTS).setPreferredWidth(80);
	}

	private void configureScheduleTable() {
		DefaultTableModel model = new DefaultTableModel(
				new Object[][] {},
				new String[] { "Code", "Course Name", "Instructor", "Schedule", "Room", "Credits" }) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		tableSchedules.setModel(model);
		tableSchedules.setRowHeight(26);
	}

	private final DefaultTableModel selectedSubjectsTableModel = new javax.swing.table.DefaultTableModel(
			new Object[][] {},
			new String[] { "Subject Code", "Name", "Section", "Instructor", "Schedule", "Room", "Credits", "Offering ID" }) {
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return columnIndex == 7 ? Long.class : String.class;
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}
	};

	private void configureSelectedSubjectsPanel() {
		tblSelectedSubjects = new javax.swing.JTable(selectedSubjectsTableModel);
		tblSelectedSubjects.setRowHeight(26);
		tblSelectedSubjects.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tblSelectedSubjects.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tblSelectedSubjects.getTableHeader().setReorderingAllowed(false);
		tblSelectedSubjects.getTableHeader().setResizingAllowed(false);
		configureSelectedSubjectsRenderer();

		// Hide offering ID
		tblSelectedSubjects.getColumnModel().getColumn(7).setMinWidth(0);
		tblSelectedSubjects.getColumnModel().getColumn(7).setMaxWidth(0);
		tblSelectedSubjects.getColumnModel().getColumn(7).setWidth(0);

		jScrollPane3.setViewportView(tblSelectedSubjects);
		refreshSelectedSubjectsPreview();
	}

	private void loadSemesterProgress() {
		executeDatabaseTask(
				this::fetchSemesterProgressSnapshot,
				this::applySemesterProgressSnapshot,
				"Failed to load semester progress.");
	}

	private void loadCompletedSubjects() {
		executeDatabaseTask(
				this::fetchCompletedSubjectsSnapshot,
				this::applyCompletedSubjectsSnapshot,
				"Failed to load completed subjects.");
	}

	private SemesterProgressSnapshot fetchSemesterProgressSnapshot() {
		List<Object[]> rows = new ArrayList<>();
		if (currentStudent == null || currentStudent.getStudentId() == null || currentStudent.getStudentId().isBlank()) {
			return new SemesterProgressSnapshot("No student profile is available.", rows);
		}

		List<StudentSemesterProgress> progressRecords = semesterProgressService
				.getProgressByStudent(currentStudent.getStudentId());
		if (progressRecords.isEmpty()) {
			return new SemesterProgressSnapshot("No semester progress has been recorded yet.", rows);
		}

		Map<Long, Semester> semesterById = new HashMap<>();
		if (currentStudent.getCurriculumId() != null) {
			for (Semester semester : semesterService.getSemestersByCurriculum(currentStudent.getCurriculumId())) {
				if (semester.getId() != null) {
					semesterById.put(semester.getId(), semester);
				}
			}
		}

		int completedCount = 0;
		int inProgressCount = 0;
		int notStartedCount = 0;

		for (StudentSemesterProgress progress : progressRecords) {
			Semester semester = null;
			if (progress.getSemesterId() != null) {
				semester = semesterById.get(progress.getSemesterId());
				if (semester == null) {
					semester = semesterService.getSemesterById(progress.getSemesterId()).orElse(null);
				}
			}

			String semesterLabel = buildSemesterLabel(semester, progress.getSemesterId());
			String yearLevelLabel = semester == null || semester.getYearLevel() == null
					? "N/A"
					: String.valueOf(semester.getYearLevel());
			String statusLabel = progress.getStatus() == null
					? SemesterProgressStatus.NOT_STARTED.name().replace('_', ' ')
					: progress.getStatus().name().replace('_', ' ');

			if (progress.getStatus() == SemesterProgressStatus.COMPLETED) {
				completedCount++;
			} else if (progress.getStatus() == SemesterProgressStatus.IN_PROGRESS) {
				inProgressCount++;
			} else {
				notStartedCount++;
			}

			rows.add(new Object[] {
					semesterLabel,
					yearLevelLabel,
					statusLabel,
					formatTimestamp(progress.getStartedAt()),
					formatTimestamp(progress.getCompletedAt())
			});
		}

		String summary = "Completed: " + completedCount
				+ " | In Progress: " + inProgressCount
				+ " | Not Started: " + notStartedCount;
		return new SemesterProgressSnapshot(summary, rows);
	}

	private void applySemesterProgressSnapshot(SemesterProgressSnapshot snapshot) {
		semesterProgressModel.setRowCount(0);
		for (Object[] row : snapshot.rows()) {
			semesterProgressModel.addRow(row);
		}

		if (lblSemesterProgressSummary != null) {
			lblSemesterProgressSummary.setText(snapshot.summaryText());
		}
	}

	private CompletedSubjectsSnapshot fetchCompletedSubjectsSnapshot() {
		List<Object[]> rows = new ArrayList<>();
		if (currentStudent == null || currentStudent.getStudentId() == null || currentStudent.getStudentId().isBlank()) {
			return new CompletedSubjectsSnapshot("No student profile is available.", rows);
		}

		List<StudentEnrolledSubject> completedSubjects = studentEnrolledSubjectService
				.getByStudent(currentStudent.getStudentId())
				.stream()
				.filter(subject -> subject.getStatus() == StudentEnrolledSubjectStatus.COMPLETED)
				.toList();

		if (completedSubjects.isEmpty()) {
			return new CompletedSubjectsSnapshot("No completed subjects have been recorded yet.", rows);
		}

		Map<Long, Semester> semesterById = new HashMap<>();
		if (currentStudent.getCurriculumId() != null) {
			for (Semester semester : semesterService.getSemestersByCurriculum(currentStudent.getCurriculumId())) {
				if (semester.getId() != null) {
					semesterById.put(semester.getId(), semester);
				}
			}
		}

		Map<Long, SemesterSubject> semesterSubjectCache = new HashMap<>();
		Map<Long, Subject> subjectCache = new HashMap<>();

		for (StudentEnrolledSubject completedSubject : completedSubjects) {
			SemesterSubject semesterSubject = null;
			if (completedSubject.getSemesterSubjectId() != null) {
				semesterSubject = semesterSubjectCache.computeIfAbsent(
						completedSubject.getSemesterSubjectId(),
						id -> semesterSubjectService.getSemesterSubjectById(id).orElse(null));
			}

			Semester semester = null;
			if (semesterSubject != null && semesterSubject.getSemesterId() != null) {
				semester = semesterById.get(semesterSubject.getSemesterId());
				if (semester == null) {
					semester = semesterService.getSemesterById(semesterSubject.getSemesterId()).orElse(null);
				}
			}

			Subject subject = null;
			if (semesterSubject != null && semesterSubject.getSubjectId() != null) {
				subject = subjectCache.computeIfAbsent(
						semesterSubject.getSubjectId(),
						id -> SubjectService.getInstance().getSubjectById(id).orElse(null));
			}

			rows.add(buildCompletedSubjectRow(completedSubject, semesterSubject, semester, subject));
		}

		String summary = "Completed subjects: " + rows.size();
		return new CompletedSubjectsSnapshot(summary, rows);
	}

	private void applyCompletedSubjectsSnapshot(CompletedSubjectsSnapshot snapshot) {
		completedSubjectsModel.setRowCount(0);
		for (Object[] row : snapshot.rows()) {
			completedSubjectsModel.addRow(row);
		}

		if (lblCompletedSubjectsSummary != null) {
			lblCompletedSubjectsSummary.setText(snapshot.summaryText());
		}
	}

	private Object[] buildCompletedSubjectRow(
			StudentEnrolledSubject completedSubject,
			SemesterSubject semesterSubject,
			Semester semester,
			Subject subject) {
		String subjectCode = subject == null ? "N/A" : safeText(subject.getSubjectCode(), "N/A");
		String subjectName = subject == null ? "N/A" : safeText(subject.getSubjectName(), "N/A");
		String units = subject == null ? formatUnits(0.0f)
				: formatUnits(subject.getUnits() == null ? 0.0f : subject.getUnits());
		Long semesterId = semesterSubject == null ? null : semesterSubject.getSemesterId();
		String semesterLabel = buildSemesterLabel(semester, semesterId);
		Timestamp completedAt = completedSubject.getUpdatedAt() != null ? completedSubject.getUpdatedAt()
				: completedSubject.getCreatedAt();

		return new Object[] {
				subjectCode,
				subjectName,
				units,
				semesterLabel,
				formatTimestamp(completedAt)
		};
	}

	private String buildSemesterLabel(Semester semester, Long semesterId) {
		if (semester == null) {
			return semesterId == null ? "N/A" : "Semester ID " + semesterId;
		}

		String semesterName = safeText(semester.getSemester(), "N/A");
		String yearLevel = semester.getYearLevel() == null ? "N/A" : String.valueOf(semester.getYearLevel());
		return semesterName + " | Year " + yearLevel;
	}

	private String formatTimestamp(Timestamp timestamp) {
		if (timestamp == null) {
			return "N/A";
		}

		return PROGRESS_TIMESTAMP_FORMATTER.format(timestamp.toLocalDateTime());
	}

	private void updateSemesterLabel(Enrollment enrollment) {
		if (enrollment == null || enrollment.getEnrollmentPeriodId() == null) {
			txtSemester.setText(resolveCurrentOrLatestSemesterLabel());
			return;
		}

		EnrollmentPeriodService.getInstance()
				.getEnrollmentPeriodById(enrollment.getEnrollmentPeriodId())
				.ifPresentOrElse(
						period -> txtSemester.setText(buildEnrollmentPeriodLabel(period)),
						() -> txtSemester.setText(resolveCurrentOrLatestSemesterLabel()));
	}

	private String resolveCurrentOrLatestSemesterLabel() {
		return EnrollmentPeriodService.getInstance()
				.getCurrentOrLatestEnrollmentPeriod()
				.map(this::buildEnrollmentPeriodLabel)
				.orElse("N/A");
	}

	private void hideTableColumn(JTable table, int columnIndex) {
		TableColumn column = table.getColumnModel().getColumn(columnIndex);
		column.setMinWidth(0);
		column.setMaxWidth(0);
		column.setPreferredWidth(0);
	}

	private void refreshSelectedSubjectsPreview() {
		selectedSubjectsTableModel.setRowCount(0);
		selectedSubjectIdsByOfferingId.clear();
		duplicateSelectedSubjectIds.clear();
		float totalUnits = 0.0f;
		Map<Long, Integer> subjectSelectionCounts = new HashMap<>();

		DefaultTableModel catalogModel = (DefaultTableModel) tblSubjectCatalog.getModel();
		for (int modelRow = 0; modelRow < catalogModel.getRowCount(); modelRow++) {
			Long offeringIdStr = (Long) catalogModel.getValueAt(modelRow, CATALOG_COL_OFFERING_ID);
			if (isCatalogRowChecked(catalogModel, modelRow)) {
				transientSelectedOfferingIds.add(offeringIdStr);
			} else {
				transientSelectedOfferingIds.remove(offeringIdStr);
			}
			if (!isCatalogRowChecked(catalogModel, modelRow)) {
				continue;
			}

			String code = String.valueOf(catalogModel.getValueAt(modelRow, CATALOG_COL_CODE));
			String subjectName = String.valueOf(catalogModel.getValueAt(modelRow, CATALOG_COL_SUBJECT_NAME));
			float units = parseUnitsCell(catalogModel.getValueAt(modelRow, CATALOG_COL_UNITS));
			Long subjectId = parseLongCell(catalogModel.getValueAt(modelRow, CATALOG_COL_SUBJECT_ID));
			if (subjectId != null) {
				selectedSubjectIdsByOfferingId.put(offeringIdStr, subjectId);
				subjectSelectionCounts.merge(subjectId, 1, Integer::sum);
			}

			String schedule = String.valueOf(catalogModel.getValueAt(modelRow, 5));
			String section = String.valueOf(catalogModel.getValueAt(modelRow, 4));

			List<Schedule> schedules = ScheduleService.getInstance().getSchedulesByOffering(offeringIdStr);
			StringBuilder facultyString = new StringBuilder();
			StringBuilder roomString = new StringBuilder();
			for (Schedule sched : schedules) {
				com.group5.paul_esys.modules.faculty.services.FacultyService.getInstance().getFacultyById(sched.getFacultyId())
						.ifPresent(f -> facultyString.append(f.getLastName()).append(", ").append(f.getFirstName()).append(" "));
				com.group5.paul_esys.modules.rooms.services.RoomService.getInstance().getRoomById(sched.getRoomId())
						.ifPresent(r -> roomString.append(r.getRoom()).append(" "));
			}
			String facultyValue = facultyString.length() == 0 ? "TBA" : facultyString.toString().trim();
			String roomValue = roomString.length() == 0 ? "TBA" : roomString.toString().trim();

			selectedSubjectsTableModel.addRow(new Object[] {
					code,
					subjectName,
					section,
					facultyValue,
					schedule,
					roomValue,
					formatUnits(units),
					offeringIdStr
			});
			totalUnits += units;
		}

		for (Map.Entry<Long, Integer> entry : subjectSelectionCounts.entrySet()) {
			if (entry.getValue() != null && entry.getValue() > 1) {
				duplicateSelectedSubjectIds.add(entry.getKey());
			}
		}

		selectedCatalogUnits = totalUnits;
		jLabel17.setText(formatUnits(totalUnits) + " / " + formatUnits(MAX_ENROLLMENT_UNITS) + " units");
		tblSelectedSubjects.repaint();
		updateBackgroundState();
	}

	private void configureSelectedSubjectsRenderer() {
		DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(
					JTable table,
					Object value,
					boolean isSelected,
					boolean hasFocus,
					int row,
					int column) {
				Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				int modelRow = table.convertRowIndexToModel(row);
				Long offeringId = parseLongCell(table.getModel().getValueAt(modelRow, 7));
				Long subjectId = offeringId == null ? null : selectedSubjectIdsByOfferingId.get(offeringId);
				boolean duplicate = subjectId != null && duplicateSelectedSubjectIds.contains(subjectId);

				if (duplicate) {
					component.setBackground(isSelected ? new Color(255, 220, 220) : new Color(255, 240, 240));
					component.setForeground(new Color(170, 0, 0));
					component.setFont(component.getFont().deriveFont(Font.BOLD));
				} else if (isSelected) {
					component.setBackground(table.getSelectionBackground());
					component.setForeground(table.getSelectionForeground());
					component.setFont(component.getFont().deriveFont(Font.PLAIN));
				} else {
					component.setBackground(table.getBackground());
					component.setForeground(table.getForeground());
					component.setFont(component.getFont().deriveFont(Font.PLAIN));
				}

				return component;
			}
		};

		tblSelectedSubjects.setDefaultRenderer(String.class, renderer);
		tblSelectedSubjects.setDefaultRenderer(Long.class, renderer);
		tblSelectedSubjects.setDefaultRenderer(Object.class, renderer);
	}

	private boolean hasMinimumSubmissionUnits(float units) {
		return units + 0.0001f >= MIN_SUBMISSION_UNITS;
	}

	private void loadSubjectCatalogAsync(String keyword) {
		executeDatabaseTask(
				() -> fetchSubjectCatalogSnapshot(keyword),
				this::applySubjectCatalogSnapshot,
				"Failed to load subject catalog.");
	}

	private SubjectCatalogSnapshot fetchSubjectCatalogSnapshot(String keyword) {
		Optional<EnrollmentPeriod> activeEnrollmentPeriod = EnrollmentPeriodService.getInstance()
				.getCurrentEnrollmentPeriod();
		Optional<EnrollmentPeriod> catalogEnrollmentPeriod = activeEnrollmentPeriod.isPresent()
				? activeEnrollmentPeriod
				: EnrollmentPeriodService.getInstance().getAllEnrollmentPeriods().stream().findFirst();

		boolean activePeriod = activeEnrollmentPeriod.isPresent();
		if (catalogEnrollmentPeriod.isEmpty()) {
			return new SubjectCatalogSnapshot(
					activePeriod,
					"",
					"Subject Catalog - No enrollment period configured",
					List.of());
		}

		EnrollmentPeriod enrollmentPeriod = catalogEnrollmentPeriod.get();
		String periodLabel = buildEnrollmentPeriodLabel(enrollmentPeriod);
		String announcement = activePeriod ? "Enrollment Period is open" : "Enrollment Period has ended";
		String catalogLabel = activePeriod
				? "Subject Catalog - " + periodLabel
				: "Subject Catalog - " + periodLabel + " (Preview only)";

		List<Offering> offerings = OfferingService.getInstance()
				.getScheduledOfferingsByEnrollmentPeriod(enrollmentPeriod.getId());
		if (offerings.isEmpty()) {
			return new SubjectCatalogSnapshot(activePeriod, announcement, catalogLabel, List.of());
		}

		Set<Long> allowedSemesterSubjectIds = StudentEnrollmentEligibilityService.getInstance()
				.getEligibleSemesterSubjectIds(
						currentStudent.getStudentId(),
						enrollmentPeriod.getSemester(),
						currentStudent.getYearLevel());
		if (allowedSemesterSubjectIds.isEmpty()) {
			return new SubjectCatalogSnapshot(
					activePeriod,
					announcement,
					"Subject Catalog - " + periodLabel + " (No eligible subjects for current semester progress)",
					List.of());
		}

		Map<Long, Subject> subjectById = new HashMap<>();
		for (Subject subject : SubjectService.getInstance().getAllSubjects()) {
			subjectById.put(subject.getId(), subject);
		}

		Map<Long, Section> sectionById = new HashMap<>();
		for (Section section : SectionService.getInstance().getAllSections()) {
			sectionById.put(section.getId(), section);
		}

		Map<Long, Long> selectedOfferingBySubject = getSelectedOfferingBySubjectForPeriod(enrollmentPeriod.getId());
		Set<Long> selectedOfferingIds = new HashSet<>(selectedOfferingBySubject.values());
		Set<Long> completedSubjectIds = getCompletedSubjectIds();
		String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
		List<Object[]> rows = new ArrayList<>();
		for (Offering offering : offerings) {
			if (offering.getSemesterSubjectId() == null
					|| !allowedSemesterSubjectIds.contains(offering.getSemesterSubjectId())) {
				continue;
			}

			Subject subject = subjectById.get(offering.getSubjectId());
			Section section = sectionById.get(offering.getSectionId());
			if (subject == null || section == null) {
				continue;
			}

			if (completedSubjectIds.contains(subject.getId())) {
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

			boolean isSelected = selectedOfferingIds.contains(offering.getId())
					|| transientSelectedOfferingIds.contains(offering.getId());
			rows.add(buildSubjectCatalogRow(subject, section, offering, isSelected));
		}

		return new SubjectCatalogSnapshot(activePeriod, announcement, catalogLabel, rows);
	}

	private Set<Long> getCompletedSubjectIds() {
		Set<Long> completedSubjectIds = new HashSet<>();
		if (currentStudent == null || currentStudent.getStudentId() == null || currentStudent.getStudentId().isBlank()) {
			return completedSubjectIds;
		}

		Map<Long, SemesterSubject> semesterSubjectCache = new HashMap<>();
		for (StudentEnrolledSubject completedSubject : studentEnrolledSubjectService
				.getByStudent(currentStudent.getStudentId())) {
			if (completedSubject == null || completedSubject.getStatus() != StudentEnrolledSubjectStatus.COMPLETED) {
				continue;
			}

			Long semesterSubjectId = completedSubject.getSemesterSubjectId();
			if (semesterSubjectId == null) {
				continue;
			}

			SemesterSubject semesterSubject = semesterSubjectCache.computeIfAbsent(
					semesterSubjectId,
					id -> semesterSubjectService.getSemesterSubjectById(id).orElse(null));
			if (semesterSubject != null && semesterSubject.getSubjectId() != null) {
				completedSubjectIds.add(semesterSubject.getSubjectId());
			}
		}

		return completedSubjectIds;
	}

	private void applySubjectCatalogSnapshot(SubjectCatalogSnapshot snapshot) {
		hasActiveEnrollmentPeriod = snapshot.activeEnrollmentPeriod();
		labelAnnouncement.setText(snapshot.announcementText());
		jLabel14.setText(snapshot.catalogLabel());

		DefaultTableModel model = (DefaultTableModel) tblSubjectCatalog.getModel();
		model.setRowCount(0);
		for (Object[] row : snapshot.rows()) {
			model.addRow(row);
		}

		refreshSelectedSubjectsPreview();
		updateBackgroundState();
	}

	private String buildEnrollmentPeriodLabel(EnrollmentPeriod period) {
		String schoolYear = safeText(period.getSchoolYear(), "N/A");
		String semester = safeText(period.getSemester(), "N/A");
		return schoolYear + " - " + semester;
	}

	private Map<Long, Long> getSelectedOfferingBySubjectForPeriod(Long enrollmentPeriodId) {
		Map<Long, Long> selectedOfferingBySubject = new HashMap<>();

		Optional<Enrollment> enrollment = findEnrollmentByPeriod(enrollmentPeriodId);
		if (enrollment.isEmpty()) {
			return selectedOfferingBySubject;
		}

		List<EnrollmentDetail> details = EnrollmentDetailService.getInstance()
				.getEnrollmentDetailsByEnrollment(enrollment.get().getId());
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

	private Optional<Enrollment> resolveEnrollmentForSchedule(List<Enrollment> enrollments) {
		if (enrollments == null || enrollments.isEmpty()) {
			return Optional.empty();
		}

		Optional<Enrollment> activeEnrollment = resolveEnrollmentForActivePeriod(enrollments);
		if (activeEnrollment.isEmpty()) {
			return Optional.empty();
		}

		if (hasSelectedEnrollmentDetails(activeEnrollment.get())) {
			return activeEnrollment;
		}

		return activeEnrollment;
	}

	private Optional<Enrollment> resolveEnrollmentForActivePeriod(List<Enrollment> enrollments) {
		if (enrollments == null || enrollments.isEmpty()) {
			return Optional.empty();
		}

		Optional<Long> activeEnrollmentPeriodId = EnrollmentPeriodService.getInstance()
				.getCurrentEnrollmentPeriod()
				.map(EnrollmentPeriod::getId);
		if (activeEnrollmentPeriodId.isEmpty()) {
			return Optional.empty();
		}

		Long periodId = activeEnrollmentPeriodId.get();
		return enrollments.stream()
				.filter(enrollment -> periodId.equals(enrollment.getEnrollmentPeriodId()))
				.findFirst();
	}

	private boolean hasSelectedEnrollmentDetails(Enrollment enrollment) {
		if (enrollment == null || enrollment.getId() == null) {
			return false;
		}

		List<EnrollmentDetail> details = EnrollmentDetailService.getInstance()
				.getEnrollmentDetailsByEnrollment(enrollment.getId());
		for (EnrollmentDetail detail : details) {
			if (detail.getStatus() == EnrollmentDetailStatus.SELECTED) {
				return true;
			}
		}

		return false;
	}

	private Object[] buildSubjectCatalogRow(
			Subject subject,
			Section section,
			Offering offering,
			boolean selected) {
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

		return new Object[] {
				selected,
				safeText(subject.getSubjectCode(), "N/A"),
				safeText(subject.getSubjectName(), "N/A"),
				units,
				safeText(section.getSectionCode(), "N/A"),
				scheduleDisplay,
				slotsDisplay,
				offering.getId(),
				subject.getId()
		};
	}

	private boolean isCatalogRowChecked(DefaultTableModel catalogModel, int modelRow) {
		Object value = catalogModel.getValueAt(modelRow, CATALOG_COL_SELECTED);
		return value instanceof Boolean checked && checked;
	}

	private List<Integer> getCheckedCatalogModelRows(DefaultTableModel catalogModel) {
		List<Integer> checkedRows = new ArrayList<>();
		for (int modelRow = 0; modelRow < catalogModel.getRowCount(); modelRow++) {
			if (isCatalogRowChecked(catalogModel, modelRow)) {
				checkedRows.add(modelRow);
			}
		}

		return checkedRows;
	}

	private void configureTextBoxes(Student student) {
		txtStudentID.setText(student.getStudentId());
		txtFirstName.setText(student.getFirstName());
		txtLastName.setText(student.getLastName());
		txtMiddleName.setText(student.getMiddleName() != null ? student.getMiddleName() : "");
		txtEmailAddress.setText(UserSession.getInstance().getUserInformation().getEmail());
		txtBirthDate.setText(student.getBirthdate() != null ? student.getBirthdate().toString() : "");
		txtStudentStatus.setText(student.getStudentStatus().toString());
		txtYearLevel.setText(student.getYearLevel() != null ? student.getYearLevel().toString() : "");
		txtSemester.setText(resolveCurrentOrLatestSemesterLabel());
		txtCourse.setText("N/A");
	}

	private void initStudentData(Student student) {
		configureTextBoxes(student);

		Optional<Course> course = CourseService.getInstance().getCourseById(student.getCourseId());
		course.ifPresent(c -> txtCourse.setText(c.getCourseName()));

		List<Enrollment> enrollments = EnrollmentService.getInstance().getEnrollmentsByStudent(student.getStudentId());
		Optional<Enrollment> activeEnrollment = resolveEnrollmentForActivePeriod(enrollments);
		if (activeEnrollment.isPresent()) {
			Enrollment enrollment = activeEnrollment.get();
			updateSemesterLabel(enrollment);
			txtEnrollmentStatus
					.setText(safeText(enrollment.getStatus() == null ? null : enrollment.getStatus().name(), "NOT ENROLLED"));
			txtTotalUnits.setText(formatUnits(enrollment.getTotalUnits() == null ? 0.0f : enrollment.getTotalUnits()));

			List<EnrollmentDetail> details = EnrollmentDetailService.getInstance()
					.getEnrollmentDetailsByEnrollment(enrollment.getId());
			long selectedCount = details.stream().filter(detail -> detail.getStatus() == EnrollmentDetailStatus.SELECTED)
					.count();
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
			updateEnrollmentStatusPresentation(enrollment);
		} else {
			updateSemesterLabel(null);
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
			case COMPLETED -> 100;
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
			return Float.parseFloat(value.toString().trim().replace(",", "."));
		} catch (NumberFormatException ex) {
			return 0.0f;
		}
	}

	private String formatUnits(float units) {
		return String.format(Locale.US, "%.1f", units);
	}

	/**
	 * This method is called from within the constructor to initialize the
	 * form. WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc="Generated
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
                txtSemester = new javax.swing.JTextField();
                jLabel31 = new javax.swing.JLabel();
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
                tblSelectedSubjects = new javax.swing.JTable();
                btnSaveDraft = new javax.swing.JButton();
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
                panelSemesterProgress = new javax.swing.JPanel();
                panelSemesterProgressHeader = new javax.swing.JPanel();
                lblSemesterProgressTitle = new javax.swing.JLabel();
                lblSemesterProgressSubtitle = new javax.swing.JLabel();
                lblSemesterProgressSummary = new javax.swing.JLabel();
                semesterProgressScrollPane = new javax.swing.JScrollPane();
                tableSemesterProgress = new javax.swing.JTable();
                panelCompletedSubjects = new javax.swing.JPanel();
                panelCompletedSubjectsHeader = new javax.swing.JPanel();
                lblCompletedSubjectsTitle = new javax.swing.JLabel();
                lblCompletedSubjectsSubtitle = new javax.swing.JLabel();
                lblCompletedSubjectsSummary = new javax.swing.JLabel();
                completedSubjectsScrollPane = new javax.swing.JScrollPane();
                tableCompletedSubjects = new javax.swing.JTable();

                setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
                setPreferredSize(new java.awt.Dimension(1280, 720));
                setResizable(false);

                windowBar1.setTitle("Student Dashboard");

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

                txtSemester.setEditable(false);
                txtSemester.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtSemester.setText("1st Semester");
                txtSemester.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                jLabel31.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel31.setText("Semester");

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
                                        .addGroup(panelAcademicOverviewLayout.createSequentialGroup()
                                                .addGap(0, 0, Short.MAX_VALUE)
                                                .addComponent(jLabel25, javax.swing.GroupLayout.PREFERRED_SIZE, 205, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(809, 809, 809))
                                        .addGroup(panelAcademicOverviewLayout.createSequentialGroup()
                                                .addComponent(txtCourse, javax.swing.GroupLayout.PREFERRED_SIZE, 290, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel26, javax.swing.GroupLayout.PREFERRED_SIZE, 198, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(txtYearLevel, javax.swing.GroupLayout.PREFERRED_SIZE, 334, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel31, javax.swing.GroupLayout.PREFERRED_SIZE, 198, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addGroup(panelAcademicOverviewLayout.createSequentialGroup()
                                                                .addComponent(txtSemester)
                                                                .addGap(36, 36, 36))))
                                        .addGroup(panelAcademicOverviewLayout.createSequentialGroup()
                                                .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(panelAcademicOverviewLayout.createSequentialGroup()
                                                                .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                                        .addComponent(jLabel23, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                        .addComponent(txtBirthDate, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 501, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                        .addComponent(jLabel24, javax.swing.GroupLayout.PREFERRED_SIZE, 471, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                        .addComponent(txtStudentStatus)))
                                                        .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelAcademicOverviewLayout.createSequentialGroup()
                                                                        .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                                                                .addGroup(panelAcademicOverviewLayout.createSequentialGroup()
                                                                                        .addComponent(jLabel30, javax.swing.GroupLayout.PREFERRED_SIZE, 296, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                        .addGap(203, 203, 203))
                                                                                .addComponent(txtSections))
                                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                        .addComponent(txtEnrollmentStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 502, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                .addGroup(panelAcademicOverviewLayout.createSequentialGroup()
                                                                        .addComponent(txtTotalSubjects, javax.swing.GroupLayout.PREFERRED_SIZE, 500, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                        .addGap(9, 9, 9)
                                                                        .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                                                .addComponent(jLabel29, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addComponent(jLabel28, javax.swing.GroupLayout.DEFAULT_SIZE, 499, Short.MAX_VALUE)
                                                                                .addComponent(txtTotalUnits)))))
                                                .addGap(36, 36, 36))))
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
                                .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addGroup(panelAcademicOverviewLayout.createSequentialGroup()
                                                .addComponent(jLabel23)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtBirthDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(jLabel25)
                                                        .addComponent(jLabel26))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(panelAcademicOverviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(txtCourse, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(txtYearLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                        .addGroup(panelAcademicOverviewLayout.createSequentialGroup()
                                                .addComponent(jLabel24)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtStudentStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(jLabel31)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtSemester, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
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
                                        .addComponent(txtEnrollmentStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(40, Short.MAX_VALUE))
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
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 595, Short.MAX_VALUE)
                                        .addComponent(jLabel14, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
                );
                jPanel2Layout.setVerticalGroup(
                        jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(13, 13, 13)
                                .addComponent(jLabel14)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 548, Short.MAX_VALUE)
                                .addContainerGap())
                );

                btnSearchSubject.setBackground(new java.awt.Color(119, 0, 0));
                btnSearchSubject.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                btnSearchSubject.setForeground(new java.awt.Color(255, 255, 255));
                btnSearchSubject.setText("Search");
                btnSearchSubject.addActionListener(this::btnSearchSubjectActionPerformed);

                jPanel8.setBackground(new java.awt.Color(255, 255, 255));
                jPanel8.setBorder(new com.group5.paul_esys.ui.RoundShadowBorder());

                jLabel15.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel15.setText("Selected Subjects");

                jLabel16.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel16.setText("Selected");

                jLabel17.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel17.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel17.setText("0 / n units");

                btnSubmitSchedule.setBackground(new java.awt.Color(119, 0, 0));
                btnSubmitSchedule.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                btnSubmitSchedule.setForeground(new java.awt.Color(255, 255, 255));
                btnSubmitSchedule.setText("Submit Schedule");
                btnSubmitSchedule.addActionListener(this::btnSubmitScheduleActionPerformed);

                tblSelectedSubjects.setAutoCreateRowSorter(true);
                tblSelectedSubjects.setModel(new javax.swing.table.DefaultTableModel(
                        new Object [][] {

                        },
                        new String [] {
                                "Subject Code", "Name", "Section", "Instructor", "Schedule", "Room", "Credits", "Offering ID"
                        }
                ) {
                        boolean[] canEdit = new boolean [] {
                                false, false, false, false, false, false, false, false
                        };

                        public boolean isCellEditable(int rowIndex, int columnIndex) {
                                return canEdit [columnIndex];
                        }
                });
                tblSelectedSubjects.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
                tblSelectedSubjects.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                tblSelectedSubjects.setShowGrid(true);
                jScrollPane3.setViewportView(tblSelectedSubjects);

                btnSaveDraft.setBackground(new java.awt.Color(119, 0, 0));
                btnSaveDraft.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                btnSaveDraft.setForeground(new java.awt.Color(255, 255, 255));
                btnSaveDraft.setText("Save as Draft");
                btnSaveDraft.addActionListener(this::btnSaveDraftActionPerformed);

                javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
                jPanel8.setLayout(jPanel8Layout);
                jPanel8Layout.setHorizontalGroup(
                        jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel8Layout.createSequentialGroup()
                                .addGap(121, 121, 121)
                                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(btnSubmitSchedule, javax.swing.GroupLayout.PREFERRED_SIZE, 219, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnSaveDraft, javax.swing.GroupLayout.PREFERRED_SIZE, 219, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGroup(jPanel8Layout.createSequentialGroup()
                                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addGroup(jPanel8Layout.createSequentialGroup()
                                                .addContainerGap()
                                                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jSeparator2)
                                                        .addComponent(jLabel17, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addComponent(jLabel16, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                        .addComponent(jLabel15, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                                .addContainerGap())
                );
                jPanel8Layout.setVerticalGroup(
                        jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel8Layout.createSequentialGroup()
                                .addGap(16, 16, 16)
                                .addComponent(jLabel15)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 364, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel16)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel17)
                                .addGap(18, 18, 18)
                                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(btnSaveDraft)
                                .addGap(14, 14, 14)
                                .addComponent(btnSubmitSchedule)
                                .addGap(26, 26, 26))
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
                                                .addGroup(panelCourseRegistrationLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                        .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addGroup(panelCourseRegistrationLayout.createSequentialGroup()
                                                                .addComponent(btnSearchSubject, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                .addGap(2, 2, 2)))
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
                                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 1075, Short.MAX_VALUE)
                                        .addComponent(jPanel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
                );
                jPanel9Layout.setVerticalGroup(
                        jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel9Layout.createSequentialGroup()
                                .addGap(16, 16, 16)
                                .addComponent(jPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 565, Short.MAX_VALUE)
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

                panelSemesterProgress.setBackground(new java.awt.Color(255, 255, 255));
                panelSemesterProgress.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(225, 225, 225)));
                panelSemesterProgress.setLayout(new java.awt.BorderLayout());

                panelSemesterProgressHeader.setOpaque(false);
                panelSemesterProgressHeader.setLayout(new java.awt.BorderLayout());

                lblSemesterProgressTitle.setFont(new java.awt.Font("Poppins", 1, 22)); // NOI18N
                lblSemesterProgressTitle.setText("Semester Progress");
                panelSemesterProgressHeader.add(lblSemesterProgressTitle, java.awt.BorderLayout.NORTH);

                lblSemesterProgressSubtitle.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
                lblSemesterProgressSubtitle.setForeground(new java.awt.Color(120, 120, 120));
                lblSemesterProgressSubtitle.setText("Track derived semester status from enrollment and completion records.");
                panelSemesterProgressHeader.add(lblSemesterProgressSubtitle, java.awt.BorderLayout.CENTER);

                lblSemesterProgressSummary.setFont(new java.awt.Font("Poppins", 0, 13)); // NOI18N
                lblSemesterProgressSummary.setForeground(new java.awt.Color(95, 95, 95));
                lblSemesterProgressSummary.setText("Loading semester progress...");
                panelSemesterProgressHeader.add(lblSemesterProgressSummary, java.awt.BorderLayout.SOUTH);

                panelSemesterProgress.add(panelSemesterProgressHeader, java.awt.BorderLayout.NORTH);

                tableSemesterProgress.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                tableSemesterProgress.setModel(new javax.swing.table.DefaultTableModel(
                        new Object [][] {
                                {null, null, null, null, null},
                                {null, null, null, null, null},
                                {null, null, null, null, null},
                                {null, null, null, null, null}
                        },
                        new String [] {
                                "Semester", "Year Level", "Status", "Started At", "Completed At"
                        }
                ));
                semesterProgressScrollPane.setViewportView(tableSemesterProgress);

                panelSemesterProgress.add(semesterProgressScrollPane, java.awt.BorderLayout.CENTER);

                tabbedPane.addTab("Semester Progress", panelSemesterProgress);

                panelCompletedSubjects.setBackground(new java.awt.Color(255, 255, 255));
                panelCompletedSubjects.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(225, 225, 225)));
                panelCompletedSubjects.setLayout(new java.awt.BorderLayout());

                panelCompletedSubjectsHeader.setOpaque(false);
                panelCompletedSubjectsHeader.setLayout(new java.awt.BorderLayout());

                lblCompletedSubjectsTitle.setFont(new java.awt.Font("Poppins", 1, 22)); // NOI18N
                lblCompletedSubjectsTitle.setText("Completed Subjects");
                panelCompletedSubjectsHeader.add(lblCompletedSubjectsTitle, java.awt.BorderLayout.NORTH);

                lblCompletedSubjectsSubtitle.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
                lblCompletedSubjectsSubtitle.setForeground(new java.awt.Color(120, 120, 120));
                lblCompletedSubjectsSubtitle.setText("Subjects marked completed from the student's enrollment history.");
                panelCompletedSubjectsHeader.add(lblCompletedSubjectsSubtitle, java.awt.BorderLayout.CENTER);

                lblCompletedSubjectsSummary.setFont(new java.awt.Font("Poppins", 0, 13)); // NOI18N
                lblCompletedSubjectsSummary.setForeground(new java.awt.Color(95, 95, 95));
                lblCompletedSubjectsSummary.setText("Loading completed subjects...");
                panelCompletedSubjectsHeader.add(lblCompletedSubjectsSummary, java.awt.BorderLayout.SOUTH);

                panelCompletedSubjects.add(panelCompletedSubjectsHeader, java.awt.BorderLayout.NORTH);

                tableCompletedSubjects.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                tableCompletedSubjects.setModel(new javax.swing.table.DefaultTableModel(
                        new Object [][] {
                                {null, null, null, null, null},
                                {null, null, null, null, null},
                                {null, null, null, null, null},
                                {null, null, null, null, null}
                        },
                        new String [] {
                                "Code", "Subject", "Units", "Semester", "Completed At"
                        }
                ));
                completedSubjectsScrollPane.setViewportView(tableCompletedSubjects);

                panelCompletedSubjects.add(completedSubjectsScrollPane, java.awt.BorderLayout.CENTER);

                tabbedPane.addTab("Completed Subjects", panelCompletedSubjects);

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
                getContentPane().setLayout(layout);
                layout.setHorizontalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(windowBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 1283, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(tabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 1283, javax.swing.GroupLayout.PREFERRED_SIZE)
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(windowBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(tabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 682, javax.swing.GroupLayout.PREFERRED_SIZE))
                );

                pack();
        }// </editor-fold>//GEN-END:initComponents

	private void txtSearchActionPerformed(java.awt.event.ActionEvent evt) {
		// GEN-FIRST:event_txtSearchActionPerformed
		loadSubjectCatalogAsync(txtSearch.getText());
	}// GEN-LAST:event_txtSearchActionPerformed

	private void btnSearchSubjectActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnSearchSubjectActionPerformed
		loadSubjectCatalogAsync(txtSearch.getText());
	}// GEN-LAST:event_btnSearchSubjectActionPerformed

	private void btnSubmitScheduleActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnSubmitScheduleActionPerformed
		persistSelectedSchedule(EnrollmentStatus.SUBMITTED);
	}// GEN-LAST:event_btnSubmitScheduleActionPerformed

	private void btnSaveDraftActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnSaveDraftActionPerformed
		persistSelectedSchedule(EnrollmentStatus.DRAFT);
	}// GEN-LAST:event_btnSaveDraftActionPerformed

	private void persistSelectedSchedule(EnrollmentStatus targetStatus) {
		if (!hasActiveEnrollmentPeriod) {
			String actionLabel = targetStatus == EnrollmentStatus.SUBMITTED ? "submission" : "saving a draft";
			JOptionPane.showMessageDialog(
					this,
					"Enrollment is closed. You can preview offerings, but " + actionLabel
							+ " requires an active enrollment period.",
					"Enrollment Closed",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		DefaultTableModel catalogModel = (DefaultTableModel) tblSubjectCatalog.getModel();
		List<Integer> selectedRows = getCheckedCatalogModelRows(catalogModel);
		if (selectedRows.isEmpty()) {
			String actionLabel = targetStatus == EnrollmentStatus.SUBMITTED ? "submit" : "save as draft";
			JOptionPane.showMessageDialog(this, "Please select offerings to " + actionLabel + ".", "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		Optional<EnrollmentPeriod> activeEnrollmentPeriod = EnrollmentPeriodService.getInstance()
				.getCurrentEnrollmentPeriod();
		if (activeEnrollmentPeriod.isEmpty()) {
			showValidationErrorAndRefresh("Validation Error", "No active enrollment period is available.");
			return;
		}

		EnrollmentPeriod enrollmentPeriod = activeEnrollmentPeriod.get();
		Long activeEnrollmentPeriodId = enrollmentPeriod.getId();
		Set<Long> allowedSemesterSubjectIds = StudentEnrollmentEligibilityService.getInstance()
				.getEligibleSemesterSubjectIds(
						currentStudent.getStudentId(),
						enrollmentPeriod.getSemester(),
						currentStudent.getYearLevel());
		if (allowedSemesterSubjectIds.isEmpty()) {
			showValidationErrorAndRefresh(
					"Validation Error",
					"No eligible subjects are available for your current semester progress.");
			return;
		}

		Optional<Enrollment> existingEnrollment = findEnrollmentByPeriod(activeEnrollmentPeriodId);
		if (existingEnrollment.isPresent() && isFinalizedEnrollment(existingEnrollment.get())) {
			JOptionPane.showMessageDialog(
					this,
					"Your enrollment for the active period is already finalized.",
					"Enrollment Locked",
					JOptionPane.WARNING_MESSAGE);
			return;
		}

		Long existingEnrollmentId = existingEnrollment.map(Enrollment::getId).orElse(null);

		List<Schedule> selectedSchedules = new ArrayList<>();
		List<OfferingSelection> selectedOfferings = new ArrayList<>();
		Map<Long, Section> selectedSectionsById = new HashMap<>();
		Map<Long, Integer> selectedSectionRoomCapacity = new HashMap<>();
		Set<Long> selectedSubjectIds = new HashSet<>();
		for (Integer modelRow : selectedRows) {
			Long offeringId = parseLongCell(catalogModel.getValueAt(modelRow, CATALOG_COL_OFFERING_ID));
			if (offeringId == null) {
				showValidationErrorAndRefresh("Validation Error", "A selected row has an invalid offering reference.");
				return;
			}

			Optional<Offering> offeringOpt = OfferingService.getInstance().getOfferingById(offeringId);
			if (offeringOpt.isEmpty()) {
				showValidationErrorAndRefresh("Validation Error",
						"Offering #" + offeringId + " no longer exists. Please reselect your subjects.");
				return;
			}

			Offering offering = offeringOpt.get();
			if (!activeEnrollmentPeriodId.equals(offering.getEnrollmentPeriodId())) {
				showValidationErrorAndRefresh(
						"Validation Conflict",
						"Offering #" + offering.getId()
								+ " belongs to a different enrollment period. Your catalog has been refreshed.");
				return;
			}

			if (offering.getSemesterSubjectId() == null
					|| !allowedSemesterSubjectIds.contains(offering.getSemesterSubjectId())) {
				showValidationErrorAndRefresh(
						"Validation Conflict",
						"Offering #" + offering.getId()
								+ " is outside your eligible semester subjects (current semester + backtracking only).");
				return;
			}

			Long subjectIdFromTable = parseLongCell(catalogModel.getValueAt(modelRow, CATALOG_COL_SUBJECT_ID));
			if (subjectIdFromTable == null || !subjectIdFromTable.equals(offering.getSubjectId())) {
				showValidationErrorAndRefresh(
						"Validation Conflict",
						"Subject data for offering #" + offering.getId()
								+ " is stale or invalid. Please review the updated catalog.");
				return;
			}

			Optional<Subject> subjectOpt = SubjectService.getInstance().getSubjectById(offering.getSubjectId());
			if (subjectOpt.isEmpty()) {
				showValidationErrorAndRefresh(
						"Validation Error",
						"Subject details for offering #" + offering.getId() + " are missing.");
				return;
			}

			Subject subject = subjectOpt.get();
			if (!selectedSubjectIds.add(subject.getId())) {
				showValidationErrorAndRefresh(
						"Validation Conflict",
						"Duplicate subject selection detected for "
								+ safeText(subject.getSubjectCode(), "Unknown Subject")
								+ ".");
				return;
			}

			Optional<Section> sectionOpt = SectionService.getInstance().getSectionById(offering.getSectionId());
			if (sectionOpt.isEmpty()) {
				showValidationErrorAndRefresh(
						"Validation Error",
						"Section details for offering #" + offering.getId() + " are missing.");
				return;
			}

			Section section = sectionOpt.get();
			selectedSectionsById.put(section.getId(), section);
			String sectionLabel = safeText(section.getSectionCode(), "Section #" + section.getId());

			List<Schedule> schedules = ScheduleService.getInstance().getSchedulesByOffering(offering.getId());
			if (schedules.isEmpty()) {
				showValidationErrorAndRefresh(
						"Validation Error",
						"Offering for " + sectionLabel + " has no schedule record.");
				return;
			}

			for (Schedule newSchedule : schedules) {
				if (newSchedule.getDay() == null || newSchedule.getStartTime() == null || newSchedule.getEndTime() == null) {
					showValidationErrorAndRefresh(
							"Validation Error",
							"Offering for " + sectionLabel + " has an incomplete schedule definition.");
					return;
				}

				if (newSchedule.getRoomId() == null) {
					showValidationErrorAndRefresh(
							"Validation Error",
							"Offering for " + sectionLabel + " has no room assigned.");
					return;
				}

				Optional<Room> roomOpt = RoomService.getInstance().getRoomById(newSchedule.getRoomId());
				if (roomOpt.isEmpty()) {
					showValidationErrorAndRefresh(
							"Validation Error",
							"Room #" + newSchedule.getRoomId() + " for " + sectionLabel + " no longer exists.");
					return;
				}

				int roomCapacity = normalizeCapacity(roomOpt.get().getCapacity());
				if (roomCapacity <= 0) {
					showValidationErrorAndRefresh(
							"Validation Error",
							"Room " + safeText(roomOpt.get().getRoom(), "#" + roomOpt.get().getId())
									+ " for " + sectionLabel + " has no valid capacity.");
					return;
				}
				selectedSectionRoomCapacity.merge(section.getId(), roomCapacity, Math::min);

				for (Schedule existingSchedule : selectedSchedules) {
					if (existingSchedule.getStartTime() == null || existingSchedule.getEndTime() == null
							|| existingSchedule.getDay() == null) {
						continue;
					}

					if (existingSchedule.getOfferingId() != null
							&& existingSchedule.getOfferingId().equals(newSchedule.getOfferingId())) {
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
						showValidationErrorAndRefresh(
								"Schedule Conflict",
								"Schedule conflict in " + sectionLabel + " on " + newSchedule.getDay()
										+ ". Time " + newSchedule.getStartTime().toString().substring(0, 5)
										+ "-" + newSchedule.getEndTime().toString().substring(0, 5)
										+ " overlaps with another selected subject.");
						return;
					}
				}
			}

			selectedSchedules.addAll(schedules);
			float units = parseUnitsCell(catalogModel.getValueAt(modelRow, CATALOG_COL_UNITS));
			float authoritativeUnits = subject.getUnits() == null ? units : subject.getUnits();
			if (subject.getUnits() != null && Math.abs(units - authoritativeUnits) > 0.001f) {
				showValidationErrorAndRefresh(
						"Validation Conflict",
						"Units for " + safeText(subject.getSubjectCode(), "the selected subject")
								+ " changed from " + formatUnits(units) + " to " + formatUnits(authoritativeUnits)
								+ ". Please review your selection.");
				return;
			}

			selectedOfferings.add(new OfferingSelection(offering, authoritativeUnits));
		}

		for (Map.Entry<Long, Section> sectionEntry : selectedSectionsById.entrySet()) {
			Long sectionId = sectionEntry.getKey();
			Section section = sectionEntry.getValue();

			int roomCapacity = selectedSectionRoomCapacity.getOrDefault(sectionId, 0);
			if (roomCapacity <= 0) {
				showValidationErrorAndRefresh(
						"Validation Error",
						"Section " + safeText(section.getSectionCode(), "#" + sectionId)
								+ " has no valid room capacity configured.");
				return;
			}

			int sectionCapacity = normalizeCapacity(section.getCapacity());
			int effectiveCapacity = sectionCapacity > 0 ? Math.min(sectionCapacity, roomCapacity) : roomCapacity;
			long reservedByOthers = SectionService.getInstance()
					.countReservedStudentsBySection(sectionId, activeEnrollmentPeriodId, existingEnrollmentId);
			long projectedReserved = reservedByOthers + 1;

			if (projectedReserved > effectiveCapacity) {
				showValidationErrorAndRefresh(
						"Capacity Conflict",
						"Section " + safeText(section.getSectionCode(), "#" + sectionId)
								+ " exceeds capacity. Current reserved: " + reservedByOthers
								+ ", projected: " + projectedReserved
								+ ", allowed: " + effectiveCapacity
								+ " (based on configured room/section capacity).");
				return;
			}
		}

		float totalSelectedUnits = 0.0f;
		for (OfferingSelection selection : selectedOfferings) {
			totalSelectedUnits += selection.units;
		}

		if (targetStatus == EnrollmentStatus.SUBMITTED && !hasMinimumSubmissionUnits(totalSelectedUnits)) {
			showValidationErrorAndRefresh(
					"Validation Error",
					"You must enroll at least 18 units before submitting your schedule.");
			return;
		}

		Enrollment activeEnrollment = resolveOrCreateEnrollment(activeEnrollmentPeriodId, targetStatus);
		if (activeEnrollment == null) {
			return;
		}

		Map<Long, EnrollmentDetail> existingDetailsByOfferingId = new HashMap<>();
		for (EnrollmentDetail detail : EnrollmentDetailService.getInstance()
				.getEnrollmentDetailsByEnrollment(activeEnrollment.getId())) {
			existingDetailsByOfferingId.put(detail.getOfferingId(), detail);
		}

		Set<Long> selectedOfferingIds = new HashSet<>();
		for (OfferingSelection selection : selectedOfferings) {
			selectedOfferingIds.add(selection.offering.getId());
		}

		for (EnrollmentDetail existingDetail : existingDetailsByOfferingId.values()) {
			if (selectedOfferingIds.contains(existingDetail.getOfferingId())) {
				continue;
			}

			if (existingDetail.getStatus() == EnrollmentDetailStatus.DROPPED) {
				continue;
			}

			existingDetail.setStatus(EnrollmentDetailStatus.DROPPED);
			if (!EnrollmentDetailService.getInstance().updateEnrollmentDetail(existingDetail)) {
				JOptionPane.showMessageDialog(this, "Failed to update dropped enrollment detail.", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			Optional<Offering> droppedOffering = OfferingService.getInstance()
					.getOfferingById(existingDetail.getOfferingId());
			if (droppedOffering.isPresent() && droppedOffering.get().getSemesterSubjectId() != null) {
				StudentEnrolledSubjectService.getInstance().upsertStatus(
						currentStudent.getStudentId(),
						activeEnrollment.getId(),
						droppedOffering.get().getId(),
						droppedOffering.get().getSemesterSubjectId(),
						StudentEnrolledSubjectStatus.DROPPED,
						false);
			}
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
						StudentEnrolledSubjectStatus.ENROLLED,
						true);
			}
		}

		activeEnrollment.setStatus(targetStatus);
		activeEnrollment.setMaxUnits(MAX_ENROLLMENT_UNITS);
		activeEnrollment.setTotalUnits(sumSelectedUnits(activeEnrollment.getId()));
		if (targetStatus == EnrollmentStatus.SUBMITTED) {
			if (activeEnrollment.getSubmittedAt() == null) {
				activeEnrollment.setSubmittedAt(new Date());
			}
		} else {
			activeEnrollment.setSubmittedAt(null);
		}

		if (!EnrollmentService.getInstance().updateEnrollment(activeEnrollment)) {
			JOptionPane.showMessageDialog(this, "Failed to update enrollment summary.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		String successMessage = targetStatus == EnrollmentStatus.SUBMITTED
				? "Enrollment submitted successfully."
				: "Draft saved successfully.";
		JOptionPane.showMessageDialog(this, successMessage, "Success", JOptionPane.INFORMATION_MESSAGE);
		reloadStudentDashboardData();
	}

	private void showValidationErrorAndRefresh(String title, String message) {
		reloadStudentDashboardData();
		JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
	}

	private Optional<Enrollment> findEnrollmentByPeriod(Long enrollmentPeriodId) {
		List<Enrollment> studentEnrollments = EnrollmentService.getInstance()
				.getEnrollmentsByStudent(currentStudent.getStudentId());
		for (Enrollment enrollment : studentEnrollments) {
			if (enrollmentPeriodId.equals(enrollment.getEnrollmentPeriodId())) {
				return Optional.of(enrollment);
			}
		}

		return Optional.empty();
	}

	private boolean isFinalizedEnrollment(Enrollment enrollment) {
		return enrollment.getStatus() == EnrollmentStatus.APPROVED
				|| enrollment.getStatus() == EnrollmentStatus.ENROLLED
				|| enrollment.getStatus() == EnrollmentStatus.COMPLETED;
	}

	private void loadMySchedule() {
		DefaultTableModel model = (DefaultTableModel) tableSchedules.getModel();
		model.setRowCount(0);

		List<Enrollment> studentEr = EnrollmentService.getInstance().getEnrollmentsByStudent(currentStudent.getStudentId());
		Optional<Enrollment> scheduleEnrollment = resolveEnrollmentForSchedule(studentEr);
		if (scheduleEnrollment.isEmpty()) {
			updateEnrollmentStatusPresentation(null);
			return;
		}

		Enrollment active = scheduleEnrollment.get();
		updateEnrollmentStatusPresentation(active);
		List<EnrollmentDetail> details = EnrollmentDetailService.getInstance()
				.getEnrollmentDetailsByEnrollment(active.getId());

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

				model.addRow(new Object[] {
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

	private Enrollment resolveOrCreateEnrollment(Long enrollmentPeriodId, EnrollmentStatus initialStatus) {
		List<Enrollment> studentEnrollments = EnrollmentService.getInstance()
				.getEnrollmentsByStudent(currentStudent.getStudentId());
		for (Enrollment enrollment : studentEnrollments) {
			if (!enrollmentPeriodId.equals(enrollment.getEnrollmentPeriodId())) {
				continue;
			}

			if (isFinalizedEnrollment(enrollment)) {
				JOptionPane.showMessageDialog(
						this,
						"Your enrollment for the active period is already finalized.",
						"Enrollment Locked",
						JOptionPane.WARNING_MESSAGE);
				return null;
			}

			return enrollment;
		}

		Enrollment enrollment = new Enrollment();
		enrollment.setStudentId(currentStudent.getStudentId());
		enrollment.setEnrollmentPeriodId(enrollmentPeriodId);
		enrollment.setStatus(initialStatus);
		enrollment.setMaxUnits(MAX_ENROLLMENT_UNITS);
		enrollment.setTotalUnits(0.0f);
		if (initialStatus == EnrollmentStatus.SUBMITTED) {
			enrollment.setSubmittedAt(new Date());
		} else {
			enrollment.setSubmittedAt(null);
		}

		if (!EnrollmentService.getInstance().createEnrollment(enrollment)) {
			JOptionPane.showMessageDialog(this, "Failed to create enrollment.", "Error", JOptionPane.ERROR_MESSAGE);
			return null;
		}

		List<Enrollment> reloadedEnrollments = EnrollmentService.getInstance()
				.getEnrollmentsByStudent(currentStudent.getStudentId());
		for (Enrollment reloaded : reloadedEnrollments) {
			if (enrollmentPeriodId.equals(reloaded.getEnrollmentPeriodId())) {
				return reloaded;
			}
		}

		JOptionPane.showMessageDialog(this, "Enrollment was created but could not be reloaded.", "Error",
				JOptionPane.ERROR_MESSAGE);
		return null;
	}

	private float sumSelectedUnits(Long enrollmentId) {
		float total = 0.0f;
		List<EnrollmentDetail> details = EnrollmentDetailService.getInstance()
				.getEnrollmentDetailsByEnrollment(enrollmentId);
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
		java.awt.EventQueue.invokeLater(() -> new StudentDashboard().setVisible(true));
	}

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton btnSaveDraft;
        private javax.swing.JButton btnSearchSubject;
        private javax.swing.JButton btnSubmitSchedule;
        private javax.swing.JScrollPane completedSubjectsScrollPane;
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
        private javax.swing.JLabel jLabel31;
        private javax.swing.JLabel jLabel4;
        private javax.swing.JLabel jLabel5;
        private javax.swing.JLabel jLabel6;
        private javax.swing.JLabel jLabel7;
        private javax.swing.JLabel jLabel8;
        private javax.swing.JLabel jLabel9;
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
        private javax.swing.JLabel lblCompletedSubjectsSubtitle;
        private javax.swing.JLabel lblCompletedSubjectsSummary;
        private javax.swing.JLabel lblCompletedSubjectsTitle;
        private javax.swing.JLabel lblSemesterProgressSubtitle;
        private javax.swing.JLabel lblSemesterProgressSummary;
        private javax.swing.JLabel lblSemesterProgressTitle;
        private javax.swing.JProgressBar pBarRegistration;
        private javax.swing.JPanel panelAcademicOverview;
        private javax.swing.JPanel panelCompletedSubjects;
        private javax.swing.JPanel panelCompletedSubjectsHeader;
        private javax.swing.JPanel panelCourseRegistration;
        private javax.swing.JPanel panelDashboard;
        private javax.swing.JPanel panelEnrollmentProgress;
        private javax.swing.JPanel panelMySchedule;
        private javax.swing.JPanel panelSemesterProgress;
        private javax.swing.JPanel panelSemesterProgressHeader;
        private javax.swing.JScrollPane semesterProgressScrollPane;
        private javax.swing.JTabbedPane tabbedPane;
        private javax.swing.JTable tableCompletedSubjects;
        private javax.swing.JTable tableSchedules;
        private javax.swing.JTable tableSemesterProgress;
        private javax.swing.JTable tblSelectedSubjects;
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
        private javax.swing.JTextField txtSemester;
        private javax.swing.JTextField txtStudentID;
        private javax.swing.JTextField txtStudentStatus;
        private javax.swing.JTextField txtTotalSubjects;
        private javax.swing.JTextField txtTotalUnits;
        private javax.swing.JTextField txtYearLevel;
        private com.group5.paul_esys.components.WindowBar windowBar1;
        // End of variables declaration//GEN-END:variables
}
