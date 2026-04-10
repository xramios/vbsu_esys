package com.group5.paul_esys.screens.registrar.panels;

import com.group5.paul_esys.modules.registrar.model.ScheduleLookupOption;
import com.group5.paul_esys.modules.registrar.model.ScheduleManagementRow;
import com.group5.paul_esys.modules.registrar.model.ScheduleOfferingOption;
import com.group5.paul_esys.modules.registrar.model.ScheduleSaveResult;
import com.group5.paul_esys.modules.registrar.model.ScheduleUpsertRequest;
import com.group5.paul_esys.modules.registrar.services.RegistrarScheduleManagementService;
import com.group5.paul_esys.screens.registrar.forms.ScheduleEntryDialog;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class RegistrarSchedulesManagement extends javax.swing.JPanel {

  private static final String FILTER_ALL = "ALL";

  private final RegistrarScheduleManagementService scheduleManagementService =
      RegistrarScheduleManagementService.getInstance();

  private final Map<String, Long> enrollmentPeriodIdByLabel = new LinkedHashMap<>();

  private final List<ScheduleManagementRow> scheduleRows = new ArrayList<>();
  private final List<ScheduleManagementRow> filteredScheduleRows = new ArrayList<>();

  private DefaultTableModel tableModel;

  public RegistrarSchedulesManagement() {
    initComponents();
    initializeSchedulePanel();
  }

  private void configureScheduleTableComponent() {
    tableModel = new DefaultTableModel(
        new Object[][]{},
        new String[]{
            "Subject Code",
            "Subject Name",
            "Section",
            "Day",
            "Time",
            "Room",
            "Faculty",
            "Enrollment Period",
            "Conflict"
        }
    ) {
      @Override
      public boolean isCellEditable(int row, int column) {
        return false;
      }
    };

    tableSchedules.setModel(tableModel);
    tableSchedules.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    tableSchedules.setRowHeight(28);
    tableSchedules.setAutoCreateRowSorter(false);
  }

  private void initializeSchedulePanel() {
    configureScheduleTableComponent();
    applyStaticComponentStyles();
    configureConflictRenderer();

    initializeDayFilterOptions();
    reloadSchedules();
  }

  private void applyStaticComponentStyles() {
    txtSearch.setFont(new java.awt.Font("Poppins", 0, 12));
    cbxDay.setFont(new java.awt.Font("Poppins", 0, 12));
    cbxEnrollmentPeriod.setFont(new java.awt.Font("Poppins", 0, 12));

    btnNewSchedule.setBackground(new Color(119, 0, 0));
    btnNewSchedule.setForeground(Color.WHITE);
    btnEditSchedule.setBackground(new Color(119, 0, 0));
    btnEditSchedule.setForeground(Color.WHITE);
    btnDeleteSchedule.setBackground(new Color(119, 0, 0));
    btnDeleteSchedule.setForeground(Color.WHITE);
    btnRefresh.setBackground(new Color(119, 0, 0));
    btnRefresh.setForeground(Color.WHITE);
    btnClearFilter.setBackground(new Color(245, 245, 245));

    lblTableSummary.setFont(new java.awt.Font("Poppins", 0, 12));
    lblConflictWarning.setFont(new java.awt.Font("Poppins", 0, 12));
    panelConflictWarning.setVisible(false);
  }

  private void configureConflictRenderer() {
    DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
      @Override
      public Component getTableCellRendererComponent(
          JTable table,
          Object value,
          boolean isSelected,
          boolean hasFocus,
          int row,
          int column
      ) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (!isSelected) {
          ScheduleManagementRow scheduleRow = getRowByViewIndex(row);
          if (scheduleRow != null && scheduleRow.hasConflict()) {
            setBackground(new Color(255, 245, 232));
          } else {
            setBackground(Color.WHITE);
          }
          setForeground(new Color(38, 38, 38));
        }

        if (column == 8) {
          setHorizontalAlignment(SwingConstants.CENTER);
        } else {
          setHorizontalAlignment(SwingConstants.LEFT);
        }

        return this;
      }
    };

    for (int column = 0; column < tableSchedules.getColumnModel().getColumnCount(); column++) {
      tableSchedules.getColumnModel().getColumn(column).setCellRenderer(renderer);
    }
  }

  private void initializeDayFilterOptions() {
    cbxDay.removeAllItems();
    cbxDay.addItem(FILTER_ALL);
    cbxDay.addItem("MON");
    cbxDay.addItem("TUE");
    cbxDay.addItem("WED");
    cbxDay.addItem("THU");
    cbxDay.addItem("FRI");
    cbxDay.addItem("SAT");
    cbxDay.addItem("SUN");
    cbxDay.setSelectedItem(FILTER_ALL);
  }

  private void reloadSchedules() {
    Long selectedScheduleId = getSelectedScheduleId();

    scheduleRows.clear();
    scheduleRows.addAll(scheduleManagementService.getScheduleRows());

    reloadEnrollmentPeriodFilterOptions();
    applyFilters();

    selectScheduleById(selectedScheduleId);
  }

  private void reloadEnrollmentPeriodFilterOptions() {
    String selectedLabel = cbxEnrollmentPeriod.getSelectedItem() == null
        ? FILTER_ALL
        : cbxEnrollmentPeriod.getSelectedItem().toString();

    enrollmentPeriodIdByLabel.clear();
    cbxEnrollmentPeriod.removeAllItems();
    cbxEnrollmentPeriod.addItem(FILTER_ALL);

    for (ScheduleLookupOption option : scheduleManagementService.getEnrollmentPeriodOptions()) {
      if (option.id() == null) {
        continue;
      }

      cbxEnrollmentPeriod.addItem(option.label());
      enrollmentPeriodIdByLabel.put(option.label(), option.id());
    }

    if (FILTER_ALL.equals(selectedLabel) || enrollmentPeriodIdByLabel.containsKey(selectedLabel)) {
      cbxEnrollmentPeriod.setSelectedItem(selectedLabel);
    } else {
      cbxEnrollmentPeriod.setSelectedItem(FILTER_ALL);
    }
  }

  private void applyFilters() {
    String keyword = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase();
    String selectedDay = cbxDay.getSelectedItem() == null ? FILTER_ALL : cbxDay.getSelectedItem().toString();
    String selectedPeriodLabel = cbxEnrollmentPeriod.getSelectedItem() == null
        ? FILTER_ALL
        : cbxEnrollmentPeriod.getSelectedItem().toString();
    Long selectedPeriodId = enrollmentPeriodIdByLabel.get(selectedPeriodLabel);

    filteredScheduleRows.clear();

    for (ScheduleManagementRow row : scheduleRows) {
      if (!matchesSearch(row, keyword)) {
        continue;
      }

      if (!matchesDay(row, selectedDay)) {
        continue;
      }

      if (!matchesEnrollmentPeriod(row, selectedPeriodLabel, selectedPeriodId)) {
        continue;
      }

      filteredScheduleRows.add(row);
    }

    populateTable();
    updateTableSummary();
    updateScheduleSummary();
  }

  private boolean matchesSearch(ScheduleManagementRow row, String keyword) {
    if (keyword.isEmpty()) {
      return true;
    }

    return row.searchableText().contains(keyword);
  }

  private boolean matchesDay(ScheduleManagementRow row, String selectedDay) {
    if (FILTER_ALL.equals(selectedDay)) {
      return true;
    }

    return selectedDay.equalsIgnoreCase(row.day());
  }

  private boolean matchesEnrollmentPeriod(
      ScheduleManagementRow row,
      String selectedPeriodLabel,
      Long selectedPeriodId
  ) {
    if (FILTER_ALL.equals(selectedPeriodLabel)) {
      return true;
    }

    if (selectedPeriodId == null) {
      return false;
    }

    return selectedPeriodId.equals(row.enrollmentPeriodId());
  }

  private void populateTable() {
    tableModel.setRowCount(0);

    for (ScheduleManagementRow row : filteredScheduleRows) {
      tableModel.addRow(new Object[]{
          row.subjectCode(),
          row.subjectName(),
          row.sectionCode(),
          row.day(),
          row.timeRangeLabel(),
          row.roomDisplay(),
          row.facultyDisplay(),
          row.enrollmentPeriodLabel(),
          row.conflictLabel()
      });
    }
  }

  private void updateTableSummary() {
    String selectedDay = cbxDay.getSelectedItem() == null ? FILTER_ALL : cbxDay.getSelectedItem().toString();
    String selectedPeriod = cbxEnrollmentPeriod.getSelectedItem() == null
        ? FILTER_ALL
        : cbxEnrollmentPeriod.getSelectedItem().toString();
    String keyword = txtSearch.getText() == null ? "" : txtSearch.getText().trim();

    StringBuilder filters = new StringBuilder();

    if (!keyword.isEmpty()) {
      filters.append(" search=").append(keyword);
    }

    if (!FILTER_ALL.equals(selectedDay)) {
      filters.append(" day=").append(selectedDay);
    }

    if (!FILTER_ALL.equals(selectedPeriod)) {
      filters.append(" period=").append(selectedPeriod);
    }

    lblTableSummary.setText(
        "Showing " + filteredScheduleRows.size() + " of " + scheduleRows.size() + " schedules"
            + (filters.isEmpty() ? "" : " | Active filters:" + filters)
    );
  }

  private void clearFilters() {
    txtSearch.setText("");
    cbxDay.setSelectedItem(FILTER_ALL);
    cbxEnrollmentPeriod.setSelectedItem(FILTER_ALL);
    applyFilters();
  }

  private void updateScheduleSummary() {
    ScheduleManagementRow selected = getSelectedSchedule();
    if (selected == null) {
      setSummaryValues("N/A", "N/A", "N/A", "N/A", "N/A", "N/A", "N/A", "NONE");
      panelConflictWarning.setVisible(false);
      return;
    }

    setSummaryValues(
        selected.sectionCode(),
        selected.subjectCode() + " - " + selected.subjectName(),
        selected.enrollmentPeriodLabel(),
        selected.day(),
        selected.timeRangeLabel(),
        selected.roomDisplay(),
        selected.facultyDisplay(),
        selected.conflictLabel()
    );

    if (selected.hasConflict()) {
      lblConflictWarning.setText(buildConflictMessage(selected));
      panelConflictWarning.setVisible(true);
    } else {
      panelConflictWarning.setVisible(false);
    }
  }

  private void setSummaryValues(
      String section,
      String subject,
      String period,
      String day,
      String time,
      String room,
      String faculty,
      String conflict
  ) {
    lblValueSection.setText(section);
    lblValueSubject.setText(subject);
    lblValuePeriod.setText(period);
    lblValueDay.setText(day);
    lblValueTime.setText(time);
    lblValueRoom.setText(room);
    lblValueFaculty.setText(faculty);
    lblValueConflict.setText(conflict);
  }

  private String buildConflictMessage(ScheduleManagementRow row) {
    if (row.roomConflict() && row.facultyConflict()) {
      return "Potential room and faculty overlap detected on " + row.day() + " at " + row.timeRangeLabel() + ".";
    }

    if (row.roomConflict()) {
      return "Potential room overlap detected on " + row.day() + " at " + row.timeRangeLabel() + ".";
    }

    return "Potential faculty overlap detected on " + row.day() + " at " + row.timeRangeLabel() + ".";
  }

  private void openCreateScheduleDialog() {
    openScheduleDialog(null);
  }

  private void openUpdateScheduleDialog() {
    ScheduleManagementRow selected = getSelectedSchedule();
    if (selected == null) {
      JOptionPane.showMessageDialog(
          this,
          "Please select a schedule to update.",
          "Update Schedule",
          JOptionPane.WARNING_MESSAGE
      );
      return;
    }

    openScheduleDialog(selected);
  }

  private void openScheduleDialog(ScheduleManagementRow editingSchedule) {
    List<ScheduleOfferingOption> offeringOptions = scheduleManagementService.getOfferingOptions();
    if (offeringOptions.isEmpty()) {
      JOptionPane.showMessageDialog(
          this,
          "No offerings are available yet. Create offerings first before adding schedules.",
          "Schedules Management",
          JOptionPane.WARNING_MESSAGE
      );
      return;
    }

    List<ScheduleLookupOption> roomOptions = scheduleManagementService.getRoomOptions();
    List<ScheduleLookupOption> facultyOptions = scheduleManagementService.getFacultyOptions();

    Frame parentFrame = resolveParentFrame();
    ScheduleEntryDialog dialog = new ScheduleEntryDialog(
        parentFrame,
        offeringOptions,
        roomOptions,
        facultyOptions,
        editingSchedule
    );

    dialog.setVisible(true);
    ScheduleUpsertRequest request = dialog.getSubmission();

    if (request == null) {
      return;
    }

    ScheduleSaveResult result = editingSchedule == null
        ? scheduleManagementService.createSchedule(request)
        : scheduleManagementService.updateSchedule(request);

    JOptionPane.showMessageDialog(
        this,
        result.message(),
        "Schedules Management",
        result.successful() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE
    );

    if (result.successful()) {
      reloadSchedules();
      if (editingSchedule != null) {
        selectScheduleById(editingSchedule.scheduleId());
      }
    }
  }

  private void deleteSelectedSchedule() {
    ScheduleManagementRow selected = getSelectedSchedule();
    if (selected == null) {
      JOptionPane.showMessageDialog(
          this,
          "Please select a schedule to delete.",
          "Delete Schedule",
          JOptionPane.WARNING_MESSAGE
      );
      return;
    }

    int confirm = JOptionPane.showConfirmDialog(
        this,
        "Delete schedule for " + selected.subjectCode() + " / " + selected.sectionCode() + "?",
        "Confirm Delete",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE
    );

    if (confirm != JOptionPane.YES_OPTION) {
      return;
    }

    ScheduleSaveResult result = scheduleManagementService.deleteSchedule(selected.scheduleId());

    JOptionPane.showMessageDialog(
        this,
        result.message(),
        "Delete Schedule",
        result.successful() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE
    );

    if (result.successful()) {
      reloadSchedules();
    }
  }

  private void selectRowFromPointer(MouseEvent evt) {
    if (!evt.isPopupTrigger()) {
      return;
    }

    int row = tableSchedules.rowAtPoint(evt.getPoint());
    if (row >= 0) {
      tableSchedules.setRowSelectionInterval(row, row);
    }
  }

  private ScheduleManagementRow getSelectedSchedule() {
    int selectedRow = tableSchedules.getSelectedRow();
    if (selectedRow < 0) {
      return null;
    }

    int modelRow = tableSchedules.convertRowIndexToModel(selectedRow);
    if (modelRow < 0 || modelRow >= filteredScheduleRows.size()) {
      return null;
    }

    return filteredScheduleRows.get(modelRow);
  }

  private ScheduleManagementRow getRowByViewIndex(int viewRowIndex) {
    if (viewRowIndex < 0) {
      return null;
    }

    int modelRow = tableSchedules.convertRowIndexToModel(viewRowIndex);
    if (modelRow < 0 || modelRow >= filteredScheduleRows.size()) {
      return null;
    }

    return filteredScheduleRows.get(modelRow);
  }

  private Long getSelectedScheduleId() {
    ScheduleManagementRow selected = getSelectedSchedule();
    return selected == null ? null : selected.scheduleId();
  }

  private void selectScheduleById(Long scheduleId) {
    if (scheduleId == null) {
      return;
    }

    for (int modelRow = 0; modelRow < filteredScheduleRows.size(); modelRow++) {
      ScheduleManagementRow row = filteredScheduleRows.get(modelRow);
      if (scheduleId.equals(row.scheduleId())) {
        int viewRow = tableSchedules.convertRowIndexToView(modelRow);
        if (viewRow >= 0) {
          tableSchedules.setRowSelectionInterval(viewRow, viewRow);
        }
        return;
      }
    }
  }

  private Frame resolveParentFrame() {
    Window ancestor = SwingUtilities.getWindowAncestor(this);
    if (ancestor instanceof Frame frame) {
      return frame;
    }

    return null;
  }

        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                popMenuSchedules = new javax.swing.JPopupMenu();
                menuItemEditSchedule = new javax.swing.JMenuItem();
                menuItemDeleteSchedule = new javax.swing.JMenuItem();
                jLabel2 = new javax.swing.JLabel();
                jLabel1 = new javax.swing.JLabel();
                jPanel1 = new javax.swing.JPanel();
                panelFilters = new javax.swing.JPanel();
                lblSearch = new javax.swing.JLabel();
                txtSearch = new javax.swing.JTextField();
                lblDayFilter = new javax.swing.JLabel();
                cbxDay = new javax.swing.JComboBox<>();
                lblEnrollmentPeriodFilter = new javax.swing.JLabel();
                cbxEnrollmentPeriod = new javax.swing.JComboBox<>();
                btnClearFilter = new javax.swing.JButton();
                btnRefresh = new javax.swing.JButton();
                btnNewSchedule = new javax.swing.JButton();
                btnEditSchedule = new javax.swing.JButton();
                btnDeleteSchedule = new javax.swing.JButton();
                panelList = new javax.swing.JPanel();
                jLabel3 = new javax.swing.JLabel();
                scrollSchedules = new javax.swing.JScrollPane();
                tableSchedules = new javax.swing.JTable();
                panelSummary = new javax.swing.JPanel();
                lblSummaryTitle = new javax.swing.JLabel();
                summaryRows = new javax.swing.JPanel();
                lblSection = new javax.swing.JLabel();
                lblValueSection = new javax.swing.JLabel();
                lblSubject = new javax.swing.JLabel();
                lblValueSubject = new javax.swing.JLabel();
                lblEnrollmentPeriod = new javax.swing.JLabel();
                lblValuePeriod = new javax.swing.JLabel();
                lblDay = new javax.swing.JLabel();
                lblValueDay = new javax.swing.JLabel();
                lblTime = new javax.swing.JLabel();
                lblValueTime = new javax.swing.JLabel();
                lblRoom = new javax.swing.JLabel();
                lblValueRoom = new javax.swing.JLabel();
                lblFaculty = new javax.swing.JLabel();
                lblValueFaculty = new javax.swing.JLabel();
                lblConflict = new javax.swing.JLabel();
                lblValueConflict = new javax.swing.JLabel();
                panelConflictWarning = new javax.swing.JPanel();
                lblConflictWarning = new javax.swing.JLabel();
                lblTableSummary = new javax.swing.JLabel();

                menuItemEditSchedule.setText("Edit Schedule");
                menuItemEditSchedule.addActionListener(this::menuItemEditScheduleActionPerformed);
                popMenuSchedules.add(menuItemEditSchedule);

                menuItemDeleteSchedule.setText("Delete Schedule");
                menuItemDeleteSchedule.addActionListener(this::menuItemDeleteScheduleActionPerformed);
                popMenuSchedules.add(menuItemDeleteSchedule);

                setBackground(new java.awt.Color(255, 255, 255));
                setPreferredSize(new java.awt.Dimension(1181, 684));

                jLabel2.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel2.setForeground(new java.awt.Color(153, 153, 153));
                jLabel2.setText("Manage class schedules by offering, room, faculty, and meeting time.");

                jLabel1.setFont(new java.awt.Font("Poppins", 0, 24)); // NOI18N
                jLabel1.setText("Schedules Management");

                jPanel1.setBackground(new java.awt.Color(255, 255, 255));
                jPanel1.setBorder(new com.group5.paul_esys.ui.PanelRoundBorder());

                panelFilters.setOpaque(false);

                lblSearch.setText("Search");

                txtSearch.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());
                txtSearch.setPreferredSize(new java.awt.Dimension(280, 34));
                txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
                        public void keyReleased(java.awt.event.KeyEvent evt) {
                                txtSearchKeyReleased(evt);
                        }
                });

                lblDayFilter.setText("Day");

                cbxDay.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "ALL" }));
                cbxDay.setPreferredSize(new java.awt.Dimension(110, 34));
                cbxDay.addItemListener(this::cbxDayItemStateChanged);

                lblEnrollmentPeriodFilter.setText("Enrollment Period");

                cbxEnrollmentPeriod.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "ALL" }));
                cbxEnrollmentPeriod.setPreferredSize(new java.awt.Dimension(250, 34));
                cbxEnrollmentPeriod.addItemListener(this::cbxEnrollmentPeriodItemStateChanged);

                btnClearFilter.setText("Clear Filter");
                btnClearFilter.addActionListener(this::btnClearFilterActionPerformed);

                btnRefresh.setText("Refresh");
                btnRefresh.addActionListener(this::btnRefreshActionPerformed);

                btnNewSchedule.setText("New Schedule");
                btnNewSchedule.addActionListener(this::btnNewScheduleActionPerformed);

                btnEditSchedule.setText("Edit Schedule");
                btnEditSchedule.addActionListener(this::btnEditScheduleActionPerformed);

                btnDeleteSchedule.setText("Delete Schedule");
                btnDeleteSchedule.addActionListener(this::btnDeleteScheduleActionPerformed);

                javax.swing.GroupLayout panelFiltersLayout = new javax.swing.GroupLayout(panelFilters);
                panelFilters.setLayout(panelFiltersLayout);
                panelFiltersLayout.setHorizontalGroup(
                        panelFiltersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelFiltersLayout.createSequentialGroup()
                                .addComponent(lblSearch)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 260, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblDayFilter)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cbxDay, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblEnrollmentPeriodFilter)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cbxEnrollmentPeriod, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnClearFilter)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnRefresh)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnNewSchedule)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnEditSchedule)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnDeleteSchedule)
                                .addGap(0, 0, Short.MAX_VALUE))
                );
                panelFiltersLayout.setVerticalGroup(
                        panelFiltersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelFiltersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(lblSearch)
                                .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(lblDayFilter)
                                .addComponent(cbxDay, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(lblEnrollmentPeriodFilter)
                                .addComponent(cbxEnrollmentPeriod, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(btnClearFilter)
                                .addComponent(btnRefresh)
                                .addComponent(btnNewSchedule)
                                .addComponent(btnEditSchedule)
                                .addComponent(btnDeleteSchedule))
                );

                panelList.setBorder(new com.group5.paul_esys.ui.PanelRoundBorder());
                panelList.setOpaque(false);

                jLabel3.setFont(new java.awt.Font("Poppins", 0, 18)); // NOI18N
                jLabel3.setText("Schedule List");

                tableSchedules.setModel(new javax.swing.table.DefaultTableModel(
                        new Object [][] {
                                {null, null, null, null, null, null, null, null, null},
                                {null, null, null, null, null, null, null, null, null},
                                {null, null, null, null, null, null, null, null, null},
                                {null, null, null, null, null, null, null, null, null}
                        },
                        new String [] {
                                "Subject Code", "Subject Name", "Section", "Day", "Time", "Room", "Faculty", "Enrollment Period", "Conflict"
                        }
                ) {
                        Class[] types = new Class [] {
                                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
                        };
                        boolean[] canEdit = new boolean [] {
                                false, false, false, false, false, false, false, false, false
                        };

                        public Class getColumnClass(int columnIndex) {
                                return types [columnIndex];
                        }

                        public boolean isCellEditable(int rowIndex, int columnIndex) {
                                return canEdit [columnIndex];
                        }
                });
                tableSchedules.setComponentPopupMenu(popMenuSchedules);
                tableSchedules.addMouseListener(new java.awt.event.MouseAdapter() {
                        public void mouseClicked(java.awt.event.MouseEvent evt) {
                                tableSchedulesMouseClicked(evt);
                        }
                        public void mousePressed(java.awt.event.MouseEvent evt) {
                                tableSchedulesMousePressed(evt);
                        }
                        public void mouseReleased(java.awt.event.MouseEvent evt) {
                                tableSchedulesMouseReleased(evt);
                        }
                });
                scrollSchedules.setViewportView(tableSchedules);

                javax.swing.GroupLayout panelListLayout = new javax.swing.GroupLayout(panelList);
                panelList.setLayout(panelListLayout);
                panelListLayout.setHorizontalGroup(
                        panelListLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel3)
                        .addComponent(scrollSchedules)
                );
                panelListLayout.setVerticalGroup(
                        panelListLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelListLayout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(scrollSchedules, javax.swing.GroupLayout.DEFAULT_SIZE, 479, Short.MAX_VALUE))
                );

                panelSummary.setBackground(new java.awt.Color(255, 255, 255));
                panelSummary.setBorder(new com.group5.paul_esys.ui.PanelRoundBorder());

                lblSummaryTitle.setFont(new java.awt.Font("Poppins", 0, 16)); // NOI18N
                lblSummaryTitle.setText("Selected Schedule Summary");

                summaryRows.setOpaque(false);
                summaryRows.setLayout(new java.awt.GridLayout(8, 2));

                lblSection.setText("Section");
                summaryRows.add(lblSection);

                lblValueSection.setText("N/A");
                summaryRows.add(lblValueSection);

                lblSubject.setText("Subject");
                summaryRows.add(lblSubject);

                lblValueSubject.setText("N/A");
                summaryRows.add(lblValueSubject);

                lblEnrollmentPeriod.setText("Enrollment Period");
                summaryRows.add(lblEnrollmentPeriod);

                lblValuePeriod.setText("N/A");
                summaryRows.add(lblValuePeriod);

                lblDay.setText("Day");
                summaryRows.add(lblDay);

                lblValueDay.setText("N/A");
                summaryRows.add(lblValueDay);

                lblTime.setText("Time");
                summaryRows.add(lblTime);

                lblValueTime.setText("N/A");
                summaryRows.add(lblValueTime);

                lblRoom.setText("Room");
                summaryRows.add(lblRoom);

                lblValueRoom.setText("N/A");
                summaryRows.add(lblValueRoom);

                lblFaculty.setText("Faculty");
                summaryRows.add(lblFaculty);

                lblValueFaculty.setText("N/A");
                summaryRows.add(lblValueFaculty);

                lblConflict.setText("Conflict");
                summaryRows.add(lblConflict);

                lblValueConflict.setText("NONE");
                summaryRows.add(lblValueConflict);

                panelConflictWarning.setBackground(new java.awt.Color(255, 244, 228));
                panelConflictWarning.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

                lblConflictWarning.setForeground(new java.awt.Color(140, 70, 0));
                lblConflictWarning.setText("No conflict detected.");

                javax.swing.GroupLayout panelConflictWarningLayout = new javax.swing.GroupLayout(panelConflictWarning);
                panelConflictWarning.setLayout(panelConflictWarningLayout);
                panelConflictWarningLayout.setHorizontalGroup(
                        panelConflictWarningLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelConflictWarningLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lblConflictWarning, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
                );
                panelConflictWarningLayout.setVerticalGroup(
                        panelConflictWarningLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelConflictWarningLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lblConflictWarning)
                                .addContainerGap())
                );

                javax.swing.GroupLayout panelSummaryLayout = new javax.swing.GroupLayout(panelSummary);
                panelSummary.setLayout(panelSummaryLayout);
                panelSummaryLayout.setHorizontalGroup(
                        panelSummaryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelSummaryLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelSummaryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(lblSummaryTitle)
                                        .addComponent(summaryRows, javax.swing.GroupLayout.DEFAULT_SIZE, 322, Short.MAX_VALUE)
                                        .addComponent(panelConflictWarning, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
                );
                panelSummaryLayout.setVerticalGroup(
                        panelSummaryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelSummaryLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lblSummaryTitle)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(summaryRows, javax.swing.GroupLayout.PREFERRED_SIZE, 245, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(panelConflictWarning, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                );

                lblTableSummary.setForeground(new java.awt.Color(95, 95, 95));
                lblTableSummary.setText("Showing 0 of 0 schedules");

                javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
                jPanel1.setLayout(jPanel1Layout);
                jPanel1Layout.setHorizontalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(panelFilters, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(lblTableSummary, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(panelList, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(panelSummary, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap())
                );
                jPanel1Layout.setVerticalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(panelFilters, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(panelList, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(panelSummary, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblTableSummary)
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
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel1)
                                                        .addComponent(jLabel2))
                                                .addGap(0, 0, Short.MAX_VALUE)))
                                .addContainerGap())
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel1)
                                .addGap(6, 6, 6)
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
                );
        }// </editor-fold>//GEN-END:initComponents

  private void txtSearchKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtSearchKeyReleased
    applyFilters();
  }//GEN-LAST:event_txtSearchKeyReleased

  private void cbxDayItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbxDayItemStateChanged
    if (evt.getStateChange() == ItemEvent.SELECTED) {
      applyFilters();
    }
  }//GEN-LAST:event_cbxDayItemStateChanged

  private void cbxEnrollmentPeriodItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbxEnrollmentPeriodItemStateChanged
    if (evt.getStateChange() == ItemEvent.SELECTED) {
      applyFilters();
    }
  }//GEN-LAST:event_cbxEnrollmentPeriodItemStateChanged

  private void btnClearFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearFilterActionPerformed
    clearFilters();
  }//GEN-LAST:event_btnClearFilterActionPerformed

  private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshActionPerformed
    reloadSchedules();
  }//GEN-LAST:event_btnRefreshActionPerformed

  private void btnNewScheduleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNewScheduleActionPerformed
    openCreateScheduleDialog();
  }//GEN-LAST:event_btnNewScheduleActionPerformed

  private void btnEditScheduleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditScheduleActionPerformed
    openUpdateScheduleDialog();
  }//GEN-LAST:event_btnEditScheduleActionPerformed

  private void btnDeleteScheduleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteScheduleActionPerformed
    deleteSelectedSchedule();
  }//GEN-LAST:event_btnDeleteScheduleActionPerformed

  private void menuItemEditScheduleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemEditScheduleActionPerformed
    openUpdateScheduleDialog();
  }//GEN-LAST:event_menuItemEditScheduleActionPerformed

  private void menuItemDeleteScheduleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemDeleteScheduleActionPerformed
    deleteSelectedSchedule();
  }//GEN-LAST:event_menuItemDeleteScheduleActionPerformed

  private void tableSchedulesMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableSchedulesMouseClicked
    if (SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() == 2) {
      openUpdateScheduleDialog();
    }
  }//GEN-LAST:event_tableSchedulesMouseClicked

  private void tableSchedulesMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableSchedulesMousePressed
    selectRowFromPointer(evt);
  }//GEN-LAST:event_tableSchedulesMousePressed

  private void tableSchedulesMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableSchedulesMouseReleased
    selectRowFromPointer(evt);
  }//GEN-LAST:event_tableSchedulesMouseReleased

  private void tableSchedulesSelectionValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_tableSchedulesSelectionValueChanged
    if (!evt.getValueIsAdjusting()) {
      updateScheduleSummary();
    }
  }//GEN-LAST:event_tableSchedulesSelectionValueChanged

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton btnClearFilter;
        private javax.swing.JButton btnDeleteSchedule;
        private javax.swing.JButton btnEditSchedule;
        private javax.swing.JButton btnNewSchedule;
        private javax.swing.JButton btnRefresh;
        private javax.swing.JComboBox<String> cbxDay;
        private javax.swing.JComboBox<String> cbxEnrollmentPeriod;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JLabel lblConflict;
        private javax.swing.JLabel lblConflictWarning;
        private javax.swing.JLabel lblDay;
        private javax.swing.JLabel lblDayFilter;
        private javax.swing.JLabel lblEnrollmentPeriod;
        private javax.swing.JLabel lblEnrollmentPeriodFilter;
        private javax.swing.JLabel lblFaculty;
        private javax.swing.JLabel lblRoom;
        private javax.swing.JLabel lblSearch;
        private javax.swing.JLabel lblSection;
        private javax.swing.JLabel lblSubject;
        private javax.swing.JLabel lblSummaryTitle;
        private javax.swing.JLabel lblTableSummary;
        private javax.swing.JLabel lblTime;
        private javax.swing.JLabel lblValueConflict;
        private javax.swing.JLabel lblValueDay;
        private javax.swing.JLabel lblValueFaculty;
        private javax.swing.JLabel lblValuePeriod;
        private javax.swing.JLabel lblValueRoom;
        private javax.swing.JLabel lblValueSection;
        private javax.swing.JLabel lblValueSubject;
        private javax.swing.JLabel lblValueTime;
        private javax.swing.JMenuItem menuItemDeleteSchedule;
        private javax.swing.JMenuItem menuItemEditSchedule;
        private javax.swing.JPanel panelConflictWarning;
        private javax.swing.JPanel panelFilters;
        private javax.swing.JPanel panelList;
        private javax.swing.JPanel panelSummary;
        private javax.swing.JPopupMenu popMenuSchedules;
        private javax.swing.JScrollPane scrollSchedules;
        private javax.swing.JPanel summaryRows;
        private javax.swing.JTable tableSchedules;
        private javax.swing.JTextField txtSearch;
        // End of variables declaration//GEN-END:variables
}
