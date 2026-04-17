package com.group5.paul_esys.screens.registrar.panels;

import com.group5.paul_esys.modules.curriculum.model.Curriculum;
import com.group5.paul_esys.modules.curriculum.services.CurriculumService;
import com.group5.paul_esys.modules.enrollment_period.model.EnrollmentPeriod;
import com.group5.paul_esys.modules.enrollment_period.services.EnrollmentPeriodService;
import com.group5.paul_esys.modules.enrollment_period.utils.EnrollmentPeriodUtils;
import com.group5.paul_esys.modules.offerings.model.OfferingGenerationPlanRow;
import com.group5.paul_esys.modules.offerings.model.OfferingGenerationResult;
import com.group5.paul_esys.modules.offerings.services.OfferingGenerationService;
import com.group5.paul_esys.modules.semester.model.Semester;
import com.group5.paul_esys.modules.semester.services.SemesterService;
import com.group5.paul_esys.screens.registrar.forms.OfferingForm;
import java.awt.Frame;
import java.awt.Window;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author nytri
 */
public class RegistrarOfferingsManagement extends javax.swing.JPanel {

  private static final String ALL_OPTION = "ALL";

  private final EnrollmentPeriodService enrollmentPeriodService = EnrollmentPeriodService.getInstance();
  private final CurriculumService curriculumService = CurriculumService.getInstance();
  private final SemesterService semesterService = SemesterService.getInstance();
  private final OfferingGenerationService offeringGenerationService = OfferingGenerationService.getInstance();

  private final Map<String, Long> enrollmentPeriodIdByLabel = new LinkedHashMap<>();
  private final Map<String, Long> curriculumIdByLabel = new LinkedHashMap<>();
  private final Map<Long, List<Semester>> semestersByCurriculumId = new LinkedHashMap<>();
  private final List<OfferingGenerationPlanRow> currentPlanRows = new ArrayList<>();
  private boolean suppressFilterEvents;

  /**
   * Creates new form RegistrarOfferingsManagement
   */
  public RegistrarOfferingsManagement() {
    initComponents();
    initializePanel();
  }

  private void initializePanel() {
    tablePreview.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
    tablePreview.setRowHeight(28);
    cbxEnrollmentPeriod.setPrototypeDisplayValue("2025-2026 | First Semester | OPEN | ID 999999");
    cbxCurriculum.setPrototypeDisplayValue("BSCS2026 (2026)");
    cbxYearLevel.setPrototypeDisplayValue("6");
    cbxSemester.setPrototypeDisplayValue("Semester 10");
    btnGenerate.setEnabled(false);
    chkIncludeWaitlist.setEnabled(chkOnlyActiveSections.isSelected());
    populateYearLevelOptions();
    reloadFilterOptions();
  }

  private void onActiveSectionsFilterChanged() {
    chkIncludeWaitlist.setEnabled(chkOnlyActiveSections.isSelected());
    if (!chkOnlyActiveSections.isSelected()) {
      chkIncludeWaitlist.setSelected(false);
    }
  }

  private void onCurriculumOrYearFilterChanged() {
    if (suppressFilterEvents) {
      return;
    }

    refreshSemesterOptions();
  }

  private void onSemesterFilterChanged() {
    if (suppressFilterEvents) {
      return;
    }

    clearPreview();
  }

  private void reloadFilterOptions() {
    clearPreview();
    reloadFilterOptionsAsync();
  }

  public void refreshData() {
    reloadFilterOptions();
  }

  private void reloadFilterOptionsAsync() {
    suppressFilterEvents = true;
    try {
      cbxEnrollmentPeriod.removeAllItems();
      enrollmentPeriodIdByLabel.clear();

      cbxCurriculum.removeAllItems();
      curriculumIdByLabel.clear();

      cbxSemester.removeAllItems();
      semestersByCurriculumId.clear();
    } finally {
      suppressFilterEvents = false;
    }

    cbxEnrollmentPeriod.setEnabled(false);
    cbxCurriculum.setEnabled(false);
    cbxYearLevel.setEnabled(false);
    cbxSemester.setEnabled(false);

    new SwingWorker<FilterOptionsResult, Void>() {
      @Override
      protected FilterOptionsResult doInBackground() {
        List<EnrollmentPeriod> periods = enrollmentPeriodService.getAllEnrollmentPeriods();
        List<Curriculum> curriculums = curriculumService.getAllCurriculums();
        Map<Long, List<Semester>> semestersByCurriculum = new LinkedHashMap<>();

        for (Curriculum curriculum : curriculums) {
          if (curriculum.getId() != null) {
            semestersByCurriculum.put(curriculum.getId(), semesterService.getSemestersByCurriculum(curriculum.getId()));
          }
        }

        return new FilterOptionsResult(periods, curriculums, semestersByCurriculum);
      }

      @Override
      protected void done() {
        try {
          FilterOptionsResult result = get();

          suppressFilterEvents = true;
          try {
            for (EnrollmentPeriod period : result.periods) {
              String label = buildEnrollmentPeriodLabel(period);
              cbxEnrollmentPeriod.addItem(label);
              enrollmentPeriodIdByLabel.put(label, period.getId());
            }

            populateCurriculumOptions(result.curriculums);

            semestersByCurriculumId.clear();
            semestersByCurriculumId.putAll(result.semestersByCurriculumId);
            populateYearLevelOptions();
          } finally {
            suppressFilterEvents = false;
          }

          refreshSemesterOptions();
        } catch (InterruptedException | ExecutionException e) {
          JOptionPane.showMessageDialog(
              RegistrarOfferingsManagement.this,
              "Failed to load filter options: " + e.getMessage(),
              "Error",
              JOptionPane.ERROR_MESSAGE
          );
        } finally {
          cbxEnrollmentPeriod.setEnabled(true);
          cbxCurriculum.setEnabled(true);
          cbxYearLevel.setEnabled(true);
          cbxSemester.setEnabled(true);
        }
      }
    }.execute();
  }

  private record FilterOptionsResult(
      List<EnrollmentPeriod> periods,
      List<Curriculum> curriculums,
      Map<Long, List<Semester>> semestersByCurriculumId
  ) {
  }

  private String buildEnrollmentPeriodLabel(EnrollmentPeriod period) {
    String schoolYear = EnrollmentPeriodUtils.safeText(period.getSchoolYear(), "N/A");
    String semester = EnrollmentPeriodUtils.safeText(period.getSemester(), "N/A");
    String status = EnrollmentPeriodUtils.resolveStatus(period);

    return schoolYear + " | " + semester + " | " + status + " | ID " + period.getId();
  }

  private String buildCurriculumLabel(Curriculum curriculum) {
    String name = safeText(curriculum.getName(), "Curriculum");
    if (curriculum.getCurYear() == null) {
      return name;
    }

    return name + " (" + extractYear(curriculum.getCurYear()) + ")";
  }

  private void populateCurriculumOptions(List<Curriculum> curriculums) {
    Object selectedCurriculum = cbxCurriculum.getSelectedItem();
    String selectedCurriculumLabel = selectedCurriculum == null ? null : selectedCurriculum.toString();

    cbxCurriculum.removeAllItems();
    cbxCurriculum.addItem(ALL_OPTION);

    for (Curriculum curriculum : curriculums) {
      if (curriculum.getId() == null) {
        continue;
      }

      String label = buildCurriculumLabel(curriculum);
      cbxCurriculum.addItem(label);
      curriculumIdByLabel.put(label, curriculum.getId());
    }

    if (selectedCurriculumLabel != null && containsComboItem(cbxCurriculum, selectedCurriculumLabel)) {
      cbxCurriculum.setSelectedItem(selectedCurriculumLabel);
    } else {
      cbxCurriculum.setSelectedItem(ALL_OPTION);
    }
  }

  private int extractYear(Date date) {
    if (date instanceof java.sql.Date sqlDate) {
      return sqlDate.toLocalDate().getYear();
    }

    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().getYear();
  }

  private void populateYearLevelOptions() {
    suppressFilterEvents = true;
    try {
      Object selectedYear = cbxYearLevel.getSelectedItem();
      String selectedYearLabel = selectedYear == null ? null : selectedYear.toString();

      cbxYearLevel.removeAllItems();
      cbxYearLevel.addItem(ALL_OPTION);
      for (int yearLevel = 1; yearLevel <= 6; yearLevel++) {
        cbxYearLevel.addItem(String.valueOf(yearLevel));
      }

      if (selectedYearLabel != null && containsComboItem(cbxYearLevel, selectedYearLabel)) {
        cbxYearLevel.setSelectedItem(selectedYearLabel);
      } else if (cbxYearLevel.getItemCount() > 0) {
        cbxYearLevel.setSelectedItem(ALL_OPTION);
      }
    } finally {
      suppressFilterEvents = false;
    }
  }

  private boolean containsComboItem(javax.swing.JComboBox<String> comboBox, String value) {
    for (int index = 0; index < comboBox.getItemCount(); index++) {
      if (value.equals(comboBox.getItemAt(index))) {
        return true;
      }
    }

    return false;
  }

  private void refreshSemesterOptions() {
    Long curriculumId = getSelectedCurriculumId();
    Integer yearLevel = getSelectedYearLevel();
    Object selectedSemester = cbxSemester.getSelectedItem();
    String selectedSemesterLabel = selectedSemester == null ? null : selectedSemester.toString();

    suppressFilterEvents = true;
    try {
      cbxSemester.removeAllItems();
      cbxSemester.addItem(ALL_OPTION);

      for (String semesterName : collectSemesterNames(curriculumId, yearLevel)) {
        cbxSemester.addItem(semesterName);
      }

      if (selectedSemesterLabel != null && containsComboItem(cbxSemester, selectedSemesterLabel)) {
        cbxSemester.setSelectedItem(selectedSemesterLabel);
      } else if (cbxSemester.getItemCount() > 0) {
        cbxSemester.setSelectedItem(ALL_OPTION);
      }
    } finally {
      suppressFilterEvents = false;
    }

    clearPreview();
  }

  private LinkedHashSet<String> collectSemesterNames(Long curriculumId, Integer yearLevel) {
    LinkedHashSet<String> semesterNames = new LinkedHashSet<>();

    if (curriculumId != null) {
      addSemesterNames(semesterNames, semestersByCurriculumId.getOrDefault(curriculumId, List.of()), yearLevel);
      return semesterNames;
    }

    for (List<Semester> semesters : semestersByCurriculumId.values()) {
      addSemesterNames(semesterNames, semesters, yearLevel);
    }

    return semesterNames;
  }

  private void addSemesterNames(
      LinkedHashSet<String> semesterNames,
      List<Semester> semesters,
      Integer yearLevel
  ) {
    for (Semester semester : semesters) {
      if (yearLevel != null && semester.getYearLevel() != null && !semester.getYearLevel().equals(yearLevel)) {
        continue;
      }

      String semesterName = safeText(semester.getSemester(), "");
      if (!semesterName.isEmpty()) {
        semesterNames.add(semesterName);
      }
    }
  }

  private String safeText(String value, String fallback) {
    if (value == null) {
      return fallback;
    }

    String normalized = value.trim();
    return normalized.isEmpty() ? fallback : normalized;
  }

  private boolean isAllSelection(Object selectedItem) {
    if (selectedItem == null) {
      return true;
    }

    String value = selectedItem.toString().trim();
    return value.isEmpty() || ALL_OPTION.equalsIgnoreCase(value);
  }

  private void previewGenerationPlan() {
    Long enrollmentPeriodId = getSelectedEnrollmentPeriodId();
    Long curriculumId = getSelectedCurriculumId();
    Integer yearLevel = getSelectedYearLevel();
    String semesterName = getSelectedSemesterName();

    if (enrollmentPeriodId == null) {
      JOptionPane.showMessageDialog(
          this,
          "Please select an enrollment period.",
          "Preview Offerings",
          JOptionPane.WARNING_MESSAGE
      );
      return;
    }

    setControlsEnabled(false);

    new SwingWorker<List<OfferingGenerationPlanRow>, Void>() {
      @Override
      protected List<OfferingGenerationPlanRow> doInBackground() {
        return offeringGenerationService.previewGenerationPlan(
            enrollmentPeriodId,
            curriculumId,
            yearLevel,
            semesterName,
            chkOnlyActiveSections.isSelected(),
            chkIncludeWaitlist.isSelected()
        );
      }

      @Override
      protected void done() {
        try {
          currentPlanRows.clear();
          currentPlanRows.addAll(get());
          populatePreviewTable();
        } catch (InterruptedException | ExecutionException e) {
          JOptionPane.showMessageDialog(
              RegistrarOfferingsManagement.this,
              "Failed to preview generation plan: " + e.getMessage(),
              "Error",
              JOptionPane.ERROR_MESSAGE
          );
        } finally {
          setControlsEnabled(true);
        }
      }
    }.execute();
  }

  private void setControlsEnabled(boolean enabled) {
    btnPreview.setEnabled(enabled);
    btnGenerate.setEnabled(enabled && getPotentialNewCount() > 0);
    btnCreateOffering.setEnabled(enabled);
    btnRefresh.setEnabled(enabled);
    cbxEnrollmentPeriod.setEnabled(enabled);
    cbxCurriculum.setEnabled(enabled);
    cbxYearLevel.setEnabled(enabled);
    cbxSemester.setEnabled(enabled);
    chkOnlyActiveSections.setEnabled(enabled);
    chkIncludeWaitlist.setEnabled(enabled && chkOnlyActiveSections.isSelected());
  }

  private void generateOfferings() {
    Long enrollmentPeriodId = getSelectedEnrollmentPeriodId();
    Long curriculumId = getSelectedCurriculumId();
    Integer yearLevel = getSelectedYearLevel();
    String semesterName = getSelectedSemesterName();

    if (enrollmentPeriodId == null) {
      previewGenerationPlan();
      return;
    }

    if (currentPlanRows.isEmpty()) {
      previewGenerationPlan();
      return;
    }

    int potentialCount = getPotentialNewCount();
    if (potentialCount <= 0) {
      JOptionPane.showMessageDialog(
          this,
          "No new offerings are available to generate for the selected filters.",
          "Generate Offerings",
          JOptionPane.INFORMATION_MESSAGE
      );
      return;
    }

    int confirm = JOptionPane.showConfirmDialog(
        this,
        "Generate " + potentialCount + " new offerings?",
        "Confirm Generation",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.QUESTION_MESSAGE
    );

    if (confirm != JOptionPane.YES_OPTION) {
      return;
    }

    setControlsEnabled(false);

    new SwingWorker<OfferingGenerationResult, Void>() {
      @Override
      protected OfferingGenerationResult doInBackground() {
        return offeringGenerationService.generateOfferings(
            enrollmentPeriodId,
            curriculumId,
            yearLevel,
            semesterName,
            chkOnlyActiveSections.isSelected(),
            chkIncludeWaitlist.isSelected()
        );
      }

      @Override
      protected void done() {
        try {
          OfferingGenerationResult result = get();

          if (!result.successful()) {
            JOptionPane.showMessageDialog(
                RegistrarOfferingsManagement.this,
                result.message(),
                "Generate Offerings",
                JOptionPane.ERROR_MESSAGE
            );
            return;
          }

          JOptionPane.showMessageDialog(
              RegistrarOfferingsManagement.this,
              "Created: " + result.createdCount()
                  + "\nAlready existing: " + result.existingCount()
                  + "\nSkipped: " + result.skippedCount(),
              "Generate Offerings",
              JOptionPane.INFORMATION_MESSAGE
          );

          previewGenerationPlan();
        } catch (InterruptedException | ExecutionException e) {
          JOptionPane.showMessageDialog(
              RegistrarOfferingsManagement.this,
              "Failed to generate offerings: " + e.getMessage(),
              "Error",
              JOptionPane.ERROR_MESSAGE
          );
          setControlsEnabled(true);
        }
      }
    }.execute();
  }

  private Long getSelectedEnrollmentPeriodId() {
    Object selected = cbxEnrollmentPeriod.getSelectedItem();
    if (selected == null) {
      return null;
    }

    return enrollmentPeriodIdByLabel.get(selected.toString());
  }

  private Long getSelectedCurriculumId() {
    Object selected = cbxCurriculum.getSelectedItem();
    if (isAllSelection(selected)) {
      return null;
    }

    return curriculumIdByLabel.get(selected.toString());
  }

  private Integer getSelectedYearLevel() {
    Object selected = cbxYearLevel.getSelectedItem();
    if (isAllSelection(selected)) {
      return null;
    }

    try {
      return Integer.parseInt(selected.toString().trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private String getSelectedSemesterName() {
    Object selected = cbxSemester.getSelectedItem();
    if (isAllSelection(selected)) {
      return null;
    }

    return selected.toString();
  }

  private void populatePreviewTable() {
    DefaultTableModel model = (DefaultTableModel) tablePreview.getModel();
    model.setRowCount(0);

    for (OfferingGenerationPlanRow row : currentPlanRows) {
      model.addRow(
          new Object[]{
              row.subjectCode(),
              row.subjectName(),
              row.sectionCode(),
              row.sectionCapacity() == null ? "N/A" : row.sectionCapacity(),
              row.semesterSubjectId() == null ? "N/A" : row.semesterSubjectId(),
              row.alreadyExists() ? "YES" : "NO"
          }
      );
    }

    updateSummaryLabels();
  }

  private void clearPreview() {
    currentPlanRows.clear();

    DefaultTableModel model = (DefaultTableModel) tablePreview.getModel();
    model.setRowCount(0);

    updateSummaryLabels();
  }

  private void updateSummaryLabels() {
    int candidates = currentPlanRows.size();
    int existing = 0;

    for (OfferingGenerationPlanRow row : currentPlanRows) {
      if (row.alreadyExists()) {
        existing++;
      }
    }

    int potential = Math.max(0, candidates - existing);

    lblCandidates.setText("Candidates: " + candidates);
    lblExisting.setText("Already Existing: " + existing);
    lblPotential.setText("Potential New: " + potential);

    btnGenerate.setEnabled(potential > 0);
  }

  private int getPotentialNewCount() {
    int existing = 0;
    for (OfferingGenerationPlanRow row : currentPlanRows) {
      if (row.alreadyExists()) {
        existing++;
      }
    }

    return Math.max(0, currentPlanRows.size() - existing);
  }

  private void openCreateOfferingForm() {
    Window window = SwingUtilities.getWindowAncestor(this);
    Frame parentFrame = window instanceof Frame ? (Frame) window : null;

    OfferingForm form = new OfferingForm(
        parentFrame,
        true,
        getSelectedEnrollmentPeriodId(),
        this::refreshAfterManualOfferingCreate
    );
    form.setVisible(true);
  }

  private void refreshAfterManualOfferingCreate() {
    if (getSelectedEnrollmentPeriodId() == null) {
      clearPreview();
      return;
    }

    previewGenerationPlan();
  }

  /**
   * This method is called from within the constructor to initialize the
   * form. WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                jLabel1 = new javax.swing.JLabel();
                jLabel2 = new javax.swing.JLabel();
                jPanel1 = new javax.swing.JPanel();
                jLabel3 = new javax.swing.JLabel();
                cbxEnrollmentPeriod = new javax.swing.JComboBox<>();
                jLabel5 = new javax.swing.JLabel();
                cbxCurriculum = new javax.swing.JComboBox<>();
                jLabel6 = new javax.swing.JLabel();
                cbxYearLevel = new javax.swing.JComboBox<>();
                jLabel4 = new javax.swing.JLabel();
                cbxSemester = new javax.swing.JComboBox<>();
                btnRefresh = new javax.swing.JButton();
                btnPreview = new javax.swing.JButton();
                btnCreateOffering = new javax.swing.JButton();
                btnGenerate = new javax.swing.JButton();
                chkOnlyActiveSections = new javax.swing.JCheckBox();
                chkIncludeWaitlist = new javax.swing.JCheckBox();
                jScrollPane1 = new javax.swing.JScrollPane();
                tablePreview = new javax.swing.JTable();
                lblCandidates = new javax.swing.JLabel();
                lblExisting = new javax.swing.JLabel();
                lblPotential = new javax.swing.JLabel();

                setBackground(new java.awt.Color(255, 255, 255));
                setPreferredSize(new java.awt.Dimension(1181, 684));

                jLabel1.setFont(new java.awt.Font("Poppins", 0, 24)); // NOI18N
                jLabel1.setText("Offerings Management");

                jLabel2.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel2.setForeground(new java.awt.Color(153, 153, 153));
                jLabel2.setText("Generate offerings in bulk for a selected enrollment period, curriculum, year level, and semester.");

                jPanel1.setBackground(new java.awt.Color(255, 255, 255));
                jPanel1.setBorder(new com.group5.paul_esys.ui.PanelRoundBorder());

                jLabel3.setText("Enrollment Period");

                cbxEnrollmentPeriod.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "" }));
                cbxEnrollmentPeriod.addActionListener(this::cbxEnrollmentPeriodActionPerformed);

                jLabel5.setText("Curriculum");

                cbxCurriculum.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "ALL" }));
                cbxCurriculum.addActionListener(this::cbxCurriculumActionPerformed);

                jLabel6.setText("Year Level");

                cbxYearLevel.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "ALL" }));
                cbxYearLevel.addActionListener(this::cbxYearLevelActionPerformed);

                jLabel4.setText("Semester");

                cbxSemester.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "ALL" }));
                cbxSemester.addActionListener(this::cbxSemesterActionPerformed);

                btnRefresh.setBackground(new java.awt.Color(119, 0, 0));
                btnRefresh.setForeground(new java.awt.Color(255, 255, 255));
                btnRefresh.setText("Refresh");
                btnRefresh.addActionListener(this::btnRefreshActionPerformed);

                btnPreview.setBackground(new java.awt.Color(119, 0, 0));
                btnPreview.setForeground(new java.awt.Color(255, 255, 255));
                btnPreview.setText("Preview");
                btnPreview.addActionListener(this::btnPreviewActionPerformed);

                btnCreateOffering.setBackground(new java.awt.Color(119, 0, 0));
                btnCreateOffering.setForeground(new java.awt.Color(255, 255, 255));
                btnCreateOffering.setText("Create Offering");
                btnCreateOffering.addActionListener(this::btnCreateOfferingActionPerformed);

                btnGenerate.setBackground(new java.awt.Color(119, 0, 0));
                btnGenerate.setForeground(new java.awt.Color(255, 255, 255));
                btnGenerate.setText("Generate Offerings");
                btnGenerate.addActionListener(this::btnGenerateActionPerformed);

                chkOnlyActiveSections.setSelected(true);
                chkOnlyActiveSections.setText("Use active sections only");
                chkOnlyActiveSections.addActionListener(this::chkOnlyActiveSectionsActionPerformed);

                chkIncludeWaitlist.setSelected(true);
                chkIncludeWaitlist.setText("Include WAITLIST sections");

                tablePreview.setModel(new javax.swing.table.DefaultTableModel(
                        new Object [][] {
                                {null, null, null, null, null, null},
                                {null, null, null, null, null, null},
                                {null, null, null, null, null, null},
                                {null, null, null, null, null, null}
                        },
                        new String [] {
                                "Subject Code", "Subject Name", "Section", "Capacity", "Semester Subject ID", "Already Exists"
                        }
                ) {
                  Class<?>[] types = new Class<?>[] {
                                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class, java.lang.Object.class, java.lang.String.class
                        };
                        boolean[] canEdit = new boolean [] {
                                false, false, false, false, false, false
                        };

                  public Class<?> getColumnClass(int columnIndex) {
                                return types [columnIndex];
                        }

                        public boolean isCellEditable(int rowIndex, int columnIndex) {
                                return canEdit [columnIndex];
                        }
                });
                jScrollPane1.setViewportView(tablePreview);

                lblCandidates.setText("Candidates: 0");

                lblExisting.setText("Already Existing: 0");

                lblPotential.setText("Potential New: 0");

                javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
                jPanel1.setLayout(jPanel1Layout);
                jPanel1Layout.setHorizontalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(lblCandidates)
                                                .addGap(18, 18, 18)
                                                .addComponent(lblExisting)
                                                .addGap(18, 18, 18)
                                                .addComponent(lblPotential)
                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                                        .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                        .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                                        .addComponent(cbxEnrollmentPeriod, 0, 220, Short.MAX_VALUE)
                                                                        .addComponent(cbxSemester, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                                        .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                        .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                        .addComponent(cbxCurriculum, 0, 219, Short.MAX_VALUE)
                                                                        .addComponent(cbxYearLevel, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                                        .addComponent(chkIncludeWaitlist, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                        .addComponent(chkOnlyActiveSections, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                                                .addGap(18, 18, 18)
                                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                                        .addComponent(btnRefresh, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                        .addComponent(btnPreview, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                                        .addComponent(btnGenerate, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                        .addComponent(btnCreateOffering, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                );
                jPanel1Layout.setVerticalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel3)
                                        .addComponent(cbxEnrollmentPeriod, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel5)
                                        .addComponent(cbxCurriculum, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnRefresh)
                                        .addComponent(chkIncludeWaitlist)
                                        .addComponent(btnCreateOffering, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel4)
                                        .addComponent(cbxSemester, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnPreview)
                                        .addComponent(jLabel6)
                                        .addComponent(cbxYearLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(chkOnlyActiveSections)
                                        .addComponent(btnGenerate))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 502, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblCandidates)
                                        .addComponent(lblExisting)
                                        .addComponent(lblPotential))
                                .addContainerGap())
                );

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
                this.setLayout(layout);
                layout.setHorizontalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabel1)
                                        .addComponent(jLabel2))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
                );
        }// </editor-fold>//GEN-END:initComponents

  private void chkOnlyActiveSectionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkOnlyActiveSectionsActionPerformed
    onActiveSectionsFilterChanged();
  }//GEN-LAST:event_chkOnlyActiveSectionsActionPerformed

  private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshActionPerformed
    reloadFilterOptions();
  }//GEN-LAST:event_btnRefreshActionPerformed

  private void btnPreviewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPreviewActionPerformed
    previewGenerationPlan();
  }//GEN-LAST:event_btnPreviewActionPerformed

  private void btnGenerateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGenerateActionPerformed
    generateOfferings();
  }//GEN-LAST:event_btnGenerateActionPerformed

  private void btnCreateOfferingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCreateOfferingActionPerformed
    openCreateOfferingForm();
  }//GEN-LAST:event_btnCreateOfferingActionPerformed

  private void cbxCurriculumActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbxCurriculumActionPerformed
    onCurriculumOrYearFilterChanged();
  }//GEN-LAST:event_cbxCurriculumActionPerformed

  private void cbxYearLevelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbxYearLevelActionPerformed
    onCurriculumOrYearFilterChanged();
  }//GEN-LAST:event_cbxYearLevelActionPerformed

  private void cbxSemesterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbxSemesterActionPerformed
    onSemesterFilterChanged();
  }//GEN-LAST:event_cbxSemesterActionPerformed

        private void cbxEnrollmentPeriodActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbxEnrollmentPeriodActionPerformed
                // TODO add your handling code here:
        }//GEN-LAST:event_cbxEnrollmentPeriodActionPerformed

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton btnCreateOffering;
        private javax.swing.JButton btnGenerate;
        private javax.swing.JButton btnPreview;
        private javax.swing.JButton btnRefresh;
        private javax.swing.JComboBox<String> cbxCurriculum;
        private javax.swing.JComboBox<String> cbxEnrollmentPeriod;
        private javax.swing.JComboBox<String> cbxSemester;
        private javax.swing.JComboBox<String> cbxYearLevel;
        private javax.swing.JCheckBox chkIncludeWaitlist;
        private javax.swing.JCheckBox chkOnlyActiveSections;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel4;
        private javax.swing.JLabel jLabel5;
        private javax.swing.JLabel jLabel6;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JLabel lblCandidates;
        private javax.swing.JLabel lblExisting;
        private javax.swing.JLabel lblPotential;
        private javax.swing.JTable tablePreview;
        // End of variables declaration//GEN-END:variables
}
