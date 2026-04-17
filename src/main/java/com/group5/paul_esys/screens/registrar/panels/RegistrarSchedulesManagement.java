package com.group5.paul_esys.screens.registrar.panels;

import com.group5.paul_esys.modules.registrar.model.ScheduleLookupOption;
import com.group5.paul_esys.modules.registrar.model.ScheduleManagementRow;
import com.group5.paul_esys.modules.registrar.model.ScheduleOfferingOption;
import com.group5.paul_esys.modules.registrar.model.ScheduleSaveResult;
import com.group5.paul_esys.modules.registrar.model.ScheduleUpsertRequest;
import com.group5.paul_esys.modules.registrar.services.RegistrarScheduleManagementService;
import com.group5.paul_esys.screens.registrar.forms.ScheduleEntryDialog;
import com.group5.paul_esys.screens.registrar.forms.ScheduleGenerationDialog;
import com.group5.paul_esys.modules.users.models.enums.Role;
import com.group5.paul_esys.modules.users.models.user.UserInformation;
import com.group5.paul_esys.modules.users.services.UserSession;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
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
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
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
    applyRoleRestrictions();
    initializeSchedulePanel();
  }

  private void applyRoleRestrictions() {
    UserInformation<?> userSession = UserSession.getInstance().getUserInformation();
    if (userSession != null && userSession.getRole() == Role.REGISTRAR) {
        btnNewSchedule.setEnabled(false);
        btnEditSchedule.setEnabled(false);
        btnDeleteSchedule.setEnabled(false);
        btnAutoGenerate.setEnabled(false);
        
        btnNewSchedule.setToolTipText("Scheduling is now managed by Department Heads.");
        btnEditSchedule.setToolTipText("Scheduling is now managed by Department Heads.");
        btnDeleteSchedule.setToolTipText("Scheduling is now managed by Department Heads.");
        btnAutoGenerate.setToolTipText("Scheduling is now managed by Department Heads.");
    }
  }

  private void configureScheduleTableComponent() {
    tableModel = new DefaultTableModel(
        new Object[][]{},
        new String[]{
            "Select",
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
      public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == 0 ? Boolean.class : super.getColumnClass(columnIndex);
      }

      @Override
      public boolean isCellEditable(int row, int column) {
        return column == 0;
      }
    };

    tableSchedules.setModel(tableModel);
    tableSchedules.getSelectionModel().addListSelectionListener(this::tableSchedulesSelectionValueChanged);

    tableModel.addTableModelListener(e -> {
      if (e.getType() == javax.swing.event.TableModelEvent.UPDATE && e.getColumn() == 0) {
        int row = e.getFirstRow();
        if (row >= 0 && row < filteredScheduleRows.size()) {
          Boolean selected = (Boolean) tableModel.getValueAt(row, 0);
          ScheduleManagementRow currentRow = filteredScheduleRows.get(row);
          filteredScheduleRows.set(row, currentRow.withSelected(selected != null && selected));

          // Also update the main list
          for (int i = 0; i < scheduleRows.size(); i++) {
            com.group5.paul_esys.modules.registrar.model.ScheduleManagementRow s = scheduleRows.get(i);
            if (java.util.Objects.equals(s.scheduleId(), currentRow.scheduleId()) 
                && java.util.Objects.equals(s.offeringId(), currentRow.offeringId())) {
              scheduleRows.set(i, s.withSelected(selected != null && selected));
              break;
            }
          }
        }
      }
    });

    tableSchedules.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    tableSchedules.setRowHeight(28);
    tableSchedules.setAutoCreateRowSorter(false);
    tableSchedules.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

    int[] preferredWidths = {50, 120, 220, 95, 80, 130, 100, 170, 180, 130};
    for (int column = 0; column < preferredWidths.length; column++) {
      tableSchedules.getColumnModel().getColumn(column).setPreferredWidth(preferredWidths[column]);
    }
  }

  private void initializeSchedulePanel() {
    configureScheduleTableComponent();
    configureResponsiveLayout();
    applyStaticComponentStyles();
    configureConflictRenderer();

    initializeDayFilterOptions();
    reloadSchedules();
  }

  private void configureResponsiveLayout() {
    panelFilters.setMinimumSize(new Dimension(0, panelFilters.getPreferredSize().height));
    panelList.setMinimumSize(new Dimension(0, 0));
    scrollSchedules.setMinimumSize(new Dimension(0, 0));

    panelSummary.setMinimumSize(new Dimension(380, 0));
    panelSummary.setPreferredSize(new Dimension(380, panelSummary.getPreferredSize().height));
    summaryRows.setLayout(new java.awt.GridLayout(8, 2, 10, 6));
  }

  private void applyStaticComponentStyles() {
    txtSearch.setFont(new java.awt.Font("Poppins", 0, 12));
    cbxDay.setFont(new java.awt.Font("Poppins", 0, 12));
    cbxEnrollmentPeriod.setFont(new java.awt.Font("Poppins", 0, 12));

    configureSummaryValueField(lblValueSection);
    configureSummaryValueField(lblValueSubject);
    configureSummaryValueField(lblValuePeriod);
    configureSummaryValueField(lblValueDay);
    configureSummaryValueField(lblValueTime);
    configureSummaryValueField(lblValueRoom);
    configureSummaryValueField(lblValueFaculty);
    configureSummaryValueField(lblValueConflict);

    textAreaConflictWarning.setFont(new java.awt.Font("Poppins", 0, 12));
    textAreaConflictWarning.setEditable(false);
    textAreaConflictWarning.setFocusable(false);
    textAreaConflictWarning.setLineWrap(true);
    textAreaConflictWarning.setWrapStyleWord(true);
    textAreaConflictWarning.setOpaque(false);
    textAreaConflictWarning.setBorder(null);
    textAreaConflictWarning.setForeground(new Color(140, 70, 0));

    btnNewSchedule.setBackground(new Color(119, 0, 0));
    btnNewSchedule.setForeground(Color.WHITE);
    btnEditSchedule.setBackground(new Color(119, 0, 0));
    btnEditSchedule.setForeground(Color.WHITE);
    btnDeleteSchedule.setBackground(new Color(119, 0, 0));
    btnDeleteSchedule.setForeground(Color.WHITE);
    btnRefresh.setBackground(new Color(119, 0, 0));
    btnRefresh.setForeground(Color.WHITE);
    btnAutoGenerate.setBackground(new Color(119, 0, 0));
    btnAutoGenerate.setForeground(Color.WHITE);
    btnClearFilter.setBackground(new Color(245, 245, 245));

    lblTableSummary.setFont(new java.awt.Font("Poppins", 0, 12));
    textAreaConflictWarning.setVisible(false);
  }

  private void configureSummaryValueField(JTextField field) {
    field.setEditable(false);
    field.setEnabled(false);
    field.setFont(new java.awt.Font("Poppins", 0, 12));
    field.setDisabledTextColor(new Color(38, 38, 38));
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

        if (column == 9) {
          setHorizontalAlignment(SwingConstants.CENTER);
        } else {
          setHorizontalAlignment(SwingConstants.LEFT);
        }

        return this;
      }
    };

    tableSchedules.getColumnModel().getColumn(0).setCellRenderer(
        tableSchedules.getDefaultRenderer(Boolean.class)
    );

    for (int column = 1; column < tableSchedules.getColumnModel().getColumnCount(); column++) {
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

    new SwingWorker<List<ScheduleManagementRow>, Void>() {
      @Override
      protected List<ScheduleManagementRow> doInBackground() throws Exception {
        return scheduleManagementService.getScheduleRows();
      }

      @Override
      protected void done() {
        try {
          List<ScheduleManagementRow> rows = get();
          scheduleRows.clear();
          scheduleRows.addAll(rows);
          reloadEnrollmentPeriodFilterOptions();
          applyFilters();
          selectScheduleById(selectedScheduleId);
        } catch (Exception ex) {
          JOptionPane.showMessageDialog(
              RegistrarSchedulesManagement.this,
              "Error loading schedules: " + ex.getMessage(),
              "Schedules Management",
              JOptionPane.ERROR_MESSAGE
          );
        }
      }
    }.execute();
  }

  public void refreshData() {
    reloadSchedules();
  }

  private void reloadEnrollmentPeriodFilterOptions() {
    String selectedLabel = cbxEnrollmentPeriod.getSelectedItem() == null
        ? FILTER_ALL
        : cbxEnrollmentPeriod.getSelectedItem().toString();

    enrollmentPeriodIdByLabel.clear();
    cbxEnrollmentPeriod.removeAllItems();
    cbxEnrollmentPeriod.addItem(FILTER_ALL);

    new SwingWorker<List<ScheduleLookupOption>, Void>() {
      @Override
      protected List<ScheduleLookupOption> doInBackground() throws Exception {
        return scheduleManagementService.getEnrollmentPeriodOptions();
      }

      @Override
      protected void done() {
        try {
          List<ScheduleLookupOption> options = get();
          for (ScheduleLookupOption option : options) {
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
        } catch (Exception ex) {
          JOptionPane.showMessageDialog(
              RegistrarSchedulesManagement.this,
              "Error loading enrollment periods: " + ex.getMessage(),
              "Schedules Management",
              JOptionPane.ERROR_MESSAGE
          );
        }
      }
    }.execute();
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
          row.selected(),
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
      textAreaConflictWarning.setVisible(false);
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
      textAreaConflictWarning.setText(buildConflictMessage(selected));
      textAreaConflictWarning.setVisible(true);
    } else {
      textAreaConflictWarning.setVisible(false);
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
    List<String> conflictTypes = new ArrayList<>();
    if (row.roomConflict()) {
      conflictTypes.add("room");
    }

    if (row.facultyConflict()) {
      conflictTypes.add("faculty");
    }

    if (row.sectionConflict()) {
      conflictTypes.add("section");
    }

    if (conflictTypes.isEmpty()) {
      return "No conflict detected.";
    }

    String conflictSummary;
    if (conflictTypes.size() == 1) {
      conflictSummary = conflictTypes.get(0);
    } else {
      String leading = String.join(", ", conflictTypes.subList(0, conflictTypes.size() - 1));
      String trailing = conflictTypes.get(conflictTypes.size() - 1);
      conflictSummary = leading + " and " + trailing;
    }

    return "Potential " + conflictSummary + " overlap detected on " + row.day() + " at " + row.timeRangeLabel() + ".";
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

    ScheduleSaveResult result = (editingSchedule == null || editingSchedule.scheduleId() == null)
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

  private void openScheduleGeneratorDialog() {
    ScheduleGenerationDialog dialog = new ScheduleGenerationDialog(
        resolveParentFrame(),
        scheduleManagementService,
        this::reloadSchedules
    );
    dialog.setVisible(true);
  }

  private void deleteSelectedSchedule() {
    List<ScheduleManagementRow> selectedItems = filteredScheduleRows.stream()
        .filter(ScheduleManagementRow::selected)
        .toList();

    if (selectedItems.isEmpty()) {
      ScheduleManagementRow singleSelection = getSelectedSchedule();
      if (singleSelection == null) {
        JOptionPane.showMessageDialog(
            this,
            "Please select at least one schedule to delete.",
            "Delete Schedule",
            JOptionPane.WARNING_MESSAGE
        );
        return;
      }
      selectedItems = List.of(singleSelection);
    }

    String message;
    if (selectedItems.size() == 1) {
      ScheduleManagementRow s = selectedItems.get(0);
      message = "Delete schedule for " + s.subjectCode() + " / " + s.sectionCode() + "?";
    } else {
      StringBuilder sb = new StringBuilder("Are you sure you want to delete the following " + selectedItems.size() + " schedules?\n\n");
      for (int i = 0; i < Math.min(selectedItems.size(), 10); i++) {
        ScheduleManagementRow s = selectedItems.get(i);
        sb.append("• ").append(s.subjectCode()).append(" / ").append(s.sectionCode())
            .append(" (").append(s.day()).append(" ").append(s.timeRangeLabel()).append(")\n");
      }
      if (selectedItems.size() > 10) {
        sb.append("... and ").append(selectedItems.size() - 10).append(" more.");
      }
      message = sb.toString();
    }

    int confirm = JOptionPane.showConfirmDialog(
        this,
        message,
        "Confirm Delete",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE
    );

    if (confirm != JOptionPane.YES_OPTION) {
      return;
    }

    final List<Long> idsToDelete = selectedItems.stream()
        .map(ScheduleManagementRow::scheduleId)
        .filter(java.util.Objects::nonNull)
        .toList();

    if (idsToDelete.isEmpty()) {
        JOptionPane.showMessageDialog(
            this,
            "The selected item(s) do not have any schedules to delete.",
            "Delete Schedule",
            JOptionPane.WARNING_MESSAGE
        );
        return;
    }

    new SwingWorker<ScheduleSaveResult, Void>() {
      @Override
      protected ScheduleSaveResult doInBackground() throws Exception {
        return scheduleManagementService.deleteSchedules(idsToDelete);
      }

      @Override
      protected void done() {
        try {
          ScheduleSaveResult result = get();
          JOptionPane.showMessageDialog(
              RegistrarSchedulesManagement.this,
              result.message(),
              "Delete Schedules",
              result.successful() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE
          );

          if (result.successful()) {
            reloadSchedules();
          }
        } catch (Exception ex) {
          JOptionPane.showMessageDialog(
              RegistrarSchedulesManagement.this,
              "Error deleting schedules: " + ex.getMessage(),
              "Delete Schedules",
              JOptionPane.ERROR_MESSAGE
          );
        }
      }
    }.execute();
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
                btnAutoGenerate = new javax.swing.JButton();
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
                lblValueSection = new javax.swing.JTextField();
                lblSubject = new javax.swing.JLabel();
                lblValueSubject = new javax.swing.JTextField();
                lblEnrollmentPeriod = new javax.swing.JLabel();
                lblValuePeriod = new javax.swing.JTextField();
                lblDay = new javax.swing.JLabel();
                lblValueDay = new javax.swing.JTextField();
                lblTime = new javax.swing.JLabel();
                lblValueTime = new javax.swing.JTextField();
                lblRoom = new javax.swing.JLabel();
                lblValueRoom = new javax.swing.JTextField();
                lblFaculty = new javax.swing.JLabel();
                lblValueFaculty = new javax.swing.JTextField();
                lblConflict = new javax.swing.JLabel();
                lblValueConflict = new javax.swing.JTextField();
                jScrollPane1 = new javax.swing.JScrollPane();
                textAreaConflictWarning = new javax.swing.JTextArea();
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

                panelFilters.setMinimumSize(new java.awt.Dimension(0, 72));
                panelFilters.setOpaque(false);

                lblSearch.setText("Search");

                txtSearch.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());
                txtSearch.setPreferredSize(new java.awt.Dimension(220, 34));
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
                cbxEnrollmentPeriod.setPreferredSize(new java.awt.Dimension(200, 34));
                cbxEnrollmentPeriod.addItemListener(this::cbxEnrollmentPeriodItemStateChanged);

                btnClearFilter.setBackground(new java.awt.Color(119, 0, 0));
                btnClearFilter.setForeground(new java.awt.Color(255, 255, 255));
                btnClearFilter.setText("Clear Filter");
                btnClearFilter.addActionListener(this::btnClearFilterActionPerformed);

                btnRefresh.setBackground(new java.awt.Color(119, 0, 0));
                btnRefresh.setForeground(new java.awt.Color(255, 255, 255));
                btnRefresh.setText("Refresh");
                btnRefresh.addActionListener(this::btnRefreshActionPerformed);

                btnAutoGenerate.setText("Auto Generate");
                btnAutoGenerate.addActionListener(this::btnAutoGenerateActionPerformed);

                btnNewSchedule.setBackground(new java.awt.Color(119, 0, 0));
                btnNewSchedule.setForeground(new java.awt.Color(255, 255, 255));
                btnNewSchedule.setText("New Schedule");
                btnNewSchedule.addActionListener(this::btnNewScheduleActionPerformed);

                btnEditSchedule.setBackground(new java.awt.Color(119, 0, 0));
                btnEditSchedule.setForeground(new java.awt.Color(255, 255, 255));
                btnEditSchedule.setText("Edit Schedule");
                btnEditSchedule.addActionListener(this::btnEditScheduleActionPerformed);

                btnDeleteSchedule.setBackground(new java.awt.Color(119, 0, 0));
                btnDeleteSchedule.setForeground(new java.awt.Color(255, 255, 255));
                btnDeleteSchedule.setText("Delete Selected");
                btnDeleteSchedule.addActionListener(this::btnDeleteScheduleActionPerformed);

                javax.swing.GroupLayout panelFiltersLayout = new javax.swing.GroupLayout(panelFilters);
                panelFilters.setLayout(panelFiltersLayout);
                panelFiltersLayout.setHorizontalGroup(
                        panelFiltersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelFiltersLayout.createSequentialGroup()
                                .addGroup(panelFiltersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(panelFiltersLayout.createSequentialGroup()
                                                .addComponent(lblSearch)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtSearch, javax.swing.GroupLayout.DEFAULT_SIZE, 382, Short.MAX_VALUE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(lblDayFilter)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(cbxDay, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(lblEnrollmentPeriodFilter)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(cbxEnrollmentPeriod, javax.swing.GroupLayout.PREFERRED_SIZE, 275, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(panelFiltersLayout.createSequentialGroup()
                                                .addComponent(btnAutoGenerate)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(btnNewSchedule)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnEditSchedule)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelFiltersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addGroup(panelFiltersLayout.createSequentialGroup()
                                                .addComponent(btnClearFilter)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnRefresh))
                                        .addComponent(btnDeleteSchedule, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
                );
                panelFiltersLayout.setVerticalGroup(
                        panelFiltersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelFiltersLayout.createSequentialGroup()
                                .addGroup(panelFiltersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblSearch)
                                        .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(lblDayFilter)
                                        .addComponent(cbxDay, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(lblEnrollmentPeriodFilter)
                                        .addComponent(cbxEnrollmentPeriod, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnClearFilter)
                                        .addComponent(btnRefresh))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelFiltersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(btnAutoGenerate)
                                        .addComponent(btnNewSchedule)
                                        .addComponent(btnEditSchedule)
                                        .addComponent(btnDeleteSchedule)))
                );

                panelList.setBorder(new com.group5.paul_esys.ui.PanelRoundBorder());
                panelList.setMinimumSize(new java.awt.Dimension(0, 0));
                panelList.setOpaque(false);

                jLabel3.setFont(new java.awt.Font("Poppins", 0, 18)); // NOI18N
                jLabel3.setText("Schedule List");

                scrollSchedules.setMinimumSize(new java.awt.Dimension(0, 0));

                tableSchedules.setModel(new javax.swing.table.DefaultTableModel(
                        new Object [][] {
                        {null, null, null, null, null, null, null, null, null, null},
                        {null, null, null, null, null, null, null, null, null, null},
                        {null, null, null, null, null, null, null, null, null, null},
                        {null, null, null, null, null, null, null, null, null, null}
                        },
                        new String [] {
                        "Select", "Subject Code", "Subject Name", "Section", "Day", "Time", "Room", "Faculty", "Enrollment Period", "Conflict"
                        }
                ) {
                        Class<?>[] types = new Class<?> [] {
                        java.lang.Boolean.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
                        };
                        boolean[] canEdit = new boolean [] {
                        true, false, false, false, false, false, false, false, false, false
                        };

                        @Override
                        public Class<?> getColumnClass(int columnIndex) {
                                return types [columnIndex];
                        }

                        @Override
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
                        .addComponent(scrollSchedules, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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
                panelSummary.setMinimumSize(new java.awt.Dimension(380, 0));
                panelSummary.setPreferredSize(new java.awt.Dimension(380, 0));

                lblSummaryTitle.setFont(new java.awt.Font("Poppins", 0, 16)); // NOI18N
                lblSummaryTitle.setText("Selected Schedule Summary");

                summaryRows.setOpaque(false);
                summaryRows.setLayout(new java.awt.GridLayout(8, 2, 0, 8));

                lblSection.setText("Section");
                summaryRows.add(lblSection);

                lblValueSection.setEnabled(false);
                lblValueSection.setText("N/A");
                summaryRows.add(lblValueSection);

                lblSubject.setText("Subject");
                summaryRows.add(lblSubject);

                lblValueSubject.setEnabled(false);
                lblValueSubject.setText("N/A");
                summaryRows.add(lblValueSubject);

                lblEnrollmentPeriod.setText("Enrollment Period");
                summaryRows.add(lblEnrollmentPeriod);

                lblValuePeriod.setEnabled(false);
                lblValuePeriod.setText("N/A");
                summaryRows.add(lblValuePeriod);

                lblDay.setText("Day");
                summaryRows.add(lblDay);

                lblValueDay.setText("N/A");
                lblValueDay.setEnabled(false);
                summaryRows.add(lblValueDay);

                lblTime.setText("Time");
                summaryRows.add(lblTime);

                lblValueTime.setEnabled(false);
                lblValueTime.setText("N/A");
                summaryRows.add(lblValueTime);

                lblRoom.setText("Room");
                summaryRows.add(lblRoom);

                lblValueRoom.setEnabled(false);
                lblValueRoom.setText("N/A");
                summaryRows.add(lblValueRoom);

                lblFaculty.setText("Faculty");
                summaryRows.add(lblFaculty);

                lblValueFaculty.setEnabled(false);
                lblValueFaculty.setText("N/A");
                summaryRows.add(lblValueFaculty);

                lblConflict.setText("Conflict");
                summaryRows.add(lblConflict);

                lblValueConflict.setText("NONE");
                lblValueConflict.setEnabled(false);
                summaryRows.add(lblValueConflict);

                textAreaConflictWarning.setColumns(20);
                textAreaConflictWarning.setRows(5);
                jScrollPane1.setViewportView(textAreaConflictWarning);

                javax.swing.GroupLayout panelSummaryLayout = new javax.swing.GroupLayout(panelSummary);
                panelSummary.setLayout(panelSummaryLayout);
                panelSummaryLayout.setHorizontalGroup(
                        panelSummaryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelSummaryLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelSummaryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(jScrollPane1)
                                        .addComponent(summaryRows, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 362, Short.MAX_VALUE)
                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, panelSummaryLayout.createSequentialGroup()
                                                .addComponent(lblSummaryTitle)
                                                .addGap(0, 0, Short.MAX_VALUE)))
                                .addContainerGap())
                );
                panelSummaryLayout.setVerticalGroup(
                        panelSummaryLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(panelSummaryLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lblSummaryTitle)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(summaryRows, javax.swing.GroupLayout.PREFERRED_SIZE, 337, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1)
                                .addContainerGap())
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
                                        .addComponent(panelSummary, javax.swing.GroupLayout.DEFAULT_SIZE, 490, Short.MAX_VALUE))
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

  private void btnAutoGenerateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAutoGenerateActionPerformed
    openScheduleGeneratorDialog();
  }//GEN-LAST:event_btnAutoGenerateActionPerformed

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
        private javax.swing.JButton btnAutoGenerate;
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
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JLabel lblConflict;
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
        private javax.swing.JTextField lblValueConflict;
        private javax.swing.JTextField lblValueDay;
        private javax.swing.JTextField lblValueFaculty;
        private javax.swing.JTextField lblValuePeriod;
        private javax.swing.JTextField lblValueRoom;
        private javax.swing.JTextField lblValueSection;
        private javax.swing.JTextField lblValueSubject;
        private javax.swing.JTextField lblValueTime;
        private javax.swing.JMenuItem menuItemDeleteSchedule;
        private javax.swing.JMenuItem menuItemEditSchedule;
        private javax.swing.JPanel panelFilters;
        private javax.swing.JPanel panelList;
        private javax.swing.JPanel panelSummary;
        private javax.swing.JPopupMenu popMenuSchedules;
        private javax.swing.JScrollPane scrollSchedules;
        private javax.swing.JPanel summaryRows;
        private javax.swing.JTable tableSchedules;
        private javax.swing.JTextArea textAreaConflictWarning;
        private javax.swing.JTextField txtSearch;
        // End of variables declaration//GEN-END:variables
}
