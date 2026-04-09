/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package com.group5.paul_esys.screens.registrar.panels;

import com.group5.paul_esys.modules.sections.model.Section;
import com.group5.paul_esys.modules.sections.services.SectionService;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author nytri
 */
public class RegistrarSectionsManagement extends javax.swing.JPanel {

        private static final String FILTER_ALL = "ALL";
        private static final String STATUS_OPEN = "OPEN";
        private static final String STATUS_CLOSED = "CLOSED";
        private static final String STATUS_WAITLIST = "WAITLIST";
        private static final String STATUS_DISSOLVED = "DISSOLVED";

        private static final List<String> BASE_STATUS_OPTIONS = List.of(
                STATUS_OPEN,
                STATUS_CLOSED,
                STATUS_WAITLIST,
                STATUS_DISSOLVED
        );

        private final SectionService sectionService = SectionService.getInstance();
        private final javax.swing.Timer autoRefreshTimer = new javax.swing.Timer(4000, evt -> refreshSectionsIfChanged());

        private final JPopupMenu popMenu = new JPopupMenu();
        private final JMenuItem menuItemUpdate = new JMenuItem("Update Section");
        private final JMenuItem menuItemDelete = new JMenuItem("Delete Section");

        private List<Section> sections = new ArrayList<>();
        private List<Section> filteredSections = new ArrayList<>();
        private Map<Long, Integer> enrolledCountBySectionId = new HashMap<>();
        private long sectionsFingerprint = Long.MIN_VALUE;

	/**
	 * Creates new form RegistrarSectionsManagement
	 */
	public RegistrarSectionsManagement() {
		initComponents();
		initializeSectionPanel();
	}

        private void initializeSectionPanel() {
                tableSections.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
                tableSections.setRowHeight(28);

                configureTableModel();
                configurePopupMenu();
                registerFilterListeners();
                registerTableInteractionListeners();
                configureAutoRefresh();

                initializeSections();
        }

        @Override
        public void addNotify() {
                super.addNotify();
                startAutoRefresh();
        }

        @Override
        public void removeNotify() {
                stopAutoRefresh();
                super.removeNotify();
        }

        private void configureAutoRefresh() {
                autoRefreshTimer.setRepeats(true);
                autoRefreshTimer.setCoalesce(true);
                autoRefreshTimer.setInitialDelay(4000);
        }

        private void startAutoRefresh() {
                if (!autoRefreshTimer.isRunning()) {
                        autoRefreshTimer.start();
                }

                refreshSectionsIfChanged();
        }

        private void stopAutoRefresh() {
                if (autoRefreshTimer.isRunning()) {
                        autoRefreshTimer.stop();
                }
        }

        private void configureTableModel() {
                DefaultTableModel model = new DefaultTableModel(
                        new Object[][]{},
                        new String[]{"Section Name", "Code", "Capacity", "Enrolled", "Status"}
                ) {
                        @Override
                        public Class<?> getColumnClass(int columnIndex) {
                                return switch (columnIndex) {
                                        case 2, 3 -> Integer.class;
                                        default -> String.class;
                                };
                        }

                        @Override
                        public boolean isCellEditable(int rowIndex, int columnIndex) {
                                return false;
                        }
                };

                tableSections.setModel(model);
        }

        private void configurePopupMenu() {
                menuItemUpdate.addActionListener(evt -> openUpdateSectionDialog());
                menuItemDelete.addActionListener(evt -> deleteSelectedSection());

                popMenu.add(menuItemUpdate);
                popMenu.add(menuItemDelete);

                tableSections.setComponentPopupMenu(popMenu);
        }

        private void registerFilterListeners() {
                txtSearch.getDocument().addDocumentListener(new DocumentListener() {
                        @Override
                        public void insertUpdate(DocumentEvent e) {
                                applyFilters();
                        }

                        @Override
                        public void removeUpdate(DocumentEvent e) {
                                applyFilters();
                        }

                        @Override
                        public void changedUpdate(DocumentEvent e) {
                                applyFilters();
                        }
                });
        }

        private void registerTableInteractionListeners() {
                tableSections.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mousePressed(MouseEvent evt) {
                                selectRowFromPointer(evt);
                        }

                        @Override
                        public void mouseReleased(MouseEvent evt) {
                                selectRowFromPointer(evt);
                        }

                        @Override
                        public void mouseClicked(MouseEvent evt) {
                                if (SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() == 2) {
                                        openUpdateSectionDialog();
                                }
                        }
                });
        }

        private void selectRowFromPointer(MouseEvent evt) {
                if (!evt.isPopupTrigger()) {
                        return;
                }

                int row = tableSections.rowAtPoint(evt.getPoint());
                if (row >= 0) {
                        tableSections.setRowSelectionInterval(row, row);
                }
        }

        private void initializeSections() {
                List<Section> latestSections = sectionService.getAllSections();
                Map<Long, Integer> latestEnrolledCount = sectionService.getSelectedEnrollmentCountBySectionId();

                applySectionSnapshot(latestSections, latestEnrolledCount, true);
        }

        private void refreshSectionsIfChanged() {
                List<Section> latestSections = sectionService.getAllSections();
                Map<Long, Integer> latestEnrolledCount = sectionService.getSelectedEnrollmentCountBySectionId();

                applySectionSnapshot(latestSections, latestEnrolledCount, false);
        }

        private void applySectionSnapshot(
                List<Section> latestSections,
                Map<Long, Integer> latestEnrolledCount,
                boolean forceRefresh
        ) {
                long latestFingerprint = computeSectionsFingerprint(latestSections, latestEnrolledCount);
                if (!forceRefresh && latestFingerprint == sectionsFingerprint) {
                        return;
                }

                sections = latestSections;
                enrolledCountBySectionId = latestEnrolledCount;
                sectionsFingerprint = latestFingerprint;

                reloadStatusFilterOptions();
                applyFilters();
        }

        private long computeSectionsFingerprint(List<Section> sectionsData, Map<Long, Integer> enrollmentMap) {
                int hash = 1;

                for (Section section : sectionsData) {
                        hash = 31 * hash + Objects.hash(
                                section.getId(),
                                safeText(section.getSectionCode(), ""),
                                safeText(section.getSectionName(), ""),
                                normalizeCapacity(section.getCapacity()),
                                normalizeStatus(section.getStatus())
                        );
                }

                List<Long> sectionIds = new ArrayList<>(enrollmentMap.keySet());
                Collections.sort(sectionIds);
                for (Long sectionId : sectionIds) {
                        hash = 31 * hash + Objects.hash(sectionId, enrollmentMap.getOrDefault(sectionId, 0));
                }

                return hash;
        }

        private void reloadStatusFilterOptions() {
                Object selectedStatus = cbxStatus.getSelectedItem();
                String selectedValue = selectedStatus == null
                        ? FILTER_ALL
                        : selectedStatus.toString();

                Set<String> statusOptions = new LinkedHashSet<>(BASE_STATUS_OPTIONS);
                for (Section section : sections) {
                        statusOptions.add(normalizeStatus(section.getStatus()));
                }

                cbxStatus.removeAllItems();
                cbxStatus.addItem(FILTER_ALL);
                statusOptions.forEach(cbxStatus::addItem);

                if (FILTER_ALL.equals(selectedValue) || statusOptions.contains(selectedValue)) {
                        cbxStatus.setSelectedItem(selectedValue);
                } else {
                        cbxStatus.setSelectedItem(FILTER_ALL);
                }
        }

        private void applyFilters() {
                String searchTerm = txtSearch.getText() == null
                        ? ""
                        : txtSearch.getText().trim().toLowerCase();

                String selectedStatus = cbxStatus.getSelectedItem() == null
                        ? FILTER_ALL
                        : cbxStatus.getSelectedItem().toString();

                filteredSections = sections
                        .stream()
                        .filter(section -> matchesSearch(section, searchTerm))
                        .filter(section -> matchesStatus(section, selectedStatus))
                        .collect(Collectors.toList());

                populateTable(filteredSections);
        }

        private boolean matchesSearch(Section section, String searchTerm) {
                if (searchTerm.isEmpty()) {
                        return true;
                }

                String sectionName = safeText(section.getSectionName(), "").toLowerCase();
                String sectionCode = safeText(section.getSectionCode(), "").toLowerCase();

                return sectionName.contains(searchTerm) || sectionCode.contains(searchTerm);
        }

        private boolean matchesStatus(Section section, String selectedStatus) {
                if (FILTER_ALL.equals(selectedStatus)) {
                        return true;
                }

                return normalizeStatus(section.getStatus()).equals(selectedStatus);
        }

        private void populateTable(List<Section> sectionsToDisplay) {
                DefaultTableModel model = (DefaultTableModel) tableSections.getModel();
                model.setRowCount(0);

                for (Section section : sectionsToDisplay) {
                        int capacity = normalizeCapacity(section.getCapacity());
                        int enrolledCount = enrolledCountBySectionId.getOrDefault(section.getId(), 0);

                        model.addRow(new Object[]{
                                safeText(section.getSectionName(), "N/A"),
                                safeText(section.getSectionCode(), "N/A"),
                                capacity,
                                enrolledCount,
                                deriveDisplayStatus(section, enrolledCount, capacity)
                        });
                }
        }

        private Section getSelectedSection() {
                int selectedRow = tableSections.getSelectedRow();
                if (selectedRow < 0) {
                        return null;
                }

                int modelRow = tableSections.convertRowIndexToModel(selectedRow);
                if (modelRow < 0 || modelRow >= filteredSections.size()) {
                        return null;
                }

                return filteredSections.get(modelRow);
        }

        private void openCreateSectionDialog() {
                openSectionDialog(null);
        }

        private void openUpdateSectionDialog() {
                Section selectedSection = getSelectedSection();
                if (selectedSection == null) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Please select a section to update.",
                                "Update Section",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                openSectionDialog(selectedSection);
        }

        private void openSectionDialog(Section editingSection) {
                boolean isEditing = editingSection != null;

                JTextField fieldSectionCode = new JTextField(
                        isEditing ? safeText(editingSection.getSectionCode(), "") : "",
                        24
                );
                JTextField fieldSectionName = new JTextField(
                        isEditing ? safeText(editingSection.getSectionName(), "") : "",
                        24
                );
                JSpinner spinnerCapacity = new JSpinner(new SpinnerNumberModel(
                        Math.max(1, normalizeCapacity(isEditing ? editingSection.getCapacity() : 40)),
                        1,
                        500,
                        1
                ));

                JComboBox<String> comboStatus = new JComboBox<>(BASE_STATUS_OPTIONS.toArray(new String[0]));
                comboStatus.setSelectedItem(isEditing ? normalizeStatus(editingSection.getStatus()) : STATUS_OPEN);

                JPanel panel = buildSectionFormPanel(fieldSectionCode, fieldSectionName, spinnerCapacity, comboStatus);

                while (true) {
                        int option = JOptionPane.showConfirmDialog(
                                this,
                                panel,
                                isEditing ? "Update Section" : "Create Section",
                                JOptionPane.OK_CANCEL_OPTION,
                                JOptionPane.PLAIN_MESSAGE
                        );

                        if (option != JOptionPane.OK_OPTION) {
                                return;
                        }

                        SectionFormValue formValue = validateSectionForm(
                                fieldSectionCode,
                                fieldSectionName,
                                spinnerCapacity,
                                comboStatus,
                                editingSection
                        );

                        if (formValue == null) {
                                continue;
                        }

                        Section sectionToPersist = isEditing ? editingSection : new Section();
                        sectionToPersist
                                .setSectionCode(formValue.sectionCode)
                                .setSectionName(formValue.sectionName)
                                .setCapacity(formValue.capacity)
                                .setStatus(formValue.status);

                        boolean saved = isEditing
                                ? sectionService.updateSection(sectionToPersist)
                                : sectionService.createSection(sectionToPersist);

                        if (!saved) {
                                JOptionPane.showMessageDialog(
                                        this,
                                        "Unable to save section. Please verify values and try again.",
                                        isEditing ? "Update Section" : "Create Section",
                                        JOptionPane.ERROR_MESSAGE
                                );
                                return;
                        }

                        initializeSections();

                        JOptionPane.showMessageDialog(
                                this,
                                isEditing ? "Section updated successfully." : "Section created successfully.",
                                isEditing ? "Update Section" : "Create Section",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                        return;
                }
        }

        private JPanel buildSectionFormPanel(
                JTextField fieldSectionCode,
                JTextField fieldSectionName,
                JSpinner spinnerCapacity,
                JComboBox<String> comboStatus
        ) {
                JPanel panel = new JPanel(new GridBagLayout());
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(6, 6, 6, 6);
                gbc.fill = GridBagConstraints.HORIZONTAL;

                gbc.gridx = 0;
                gbc.gridy = 0;
                panel.add(new JLabel("Section Code"), gbc);

                gbc.gridx = 1;
                panel.add(fieldSectionCode, gbc);

                gbc.gridx = 0;
                gbc.gridy = 1;
                panel.add(new JLabel("Section Name"), gbc);

                gbc.gridx = 1;
                panel.add(fieldSectionName, gbc);

                gbc.gridx = 0;
                gbc.gridy = 2;
                panel.add(new JLabel("Capacity"), gbc);

                gbc.gridx = 1;
                panel.add(spinnerCapacity, gbc);

                gbc.gridx = 0;
                gbc.gridy = 3;
                panel.add(new JLabel("Status"), gbc);

                gbc.gridx = 1;
                panel.add(comboStatus, gbc);

                return panel;
        }

        private SectionFormValue validateSectionForm(
                JTextField fieldSectionCode,
                JTextField fieldSectionName,
                JSpinner spinnerCapacity,
                JComboBox<String> comboStatus,
                Section editingSection
        ) {
                String sectionCode = safeText(fieldSectionCode.getText(), "");
                String sectionName = safeText(fieldSectionName.getText(), "");
                int capacity = ((Number) spinnerCapacity.getValue()).intValue();
                String status = comboStatus.getSelectedItem() == null
                        ? STATUS_OPEN
                        : normalizeStatus(comboStatus.getSelectedItem().toString());

                if (sectionCode.isEmpty()) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Section code is required.",
                                "Section Validation",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return null;
                }

                if (sectionName.isEmpty()) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Section name is required.",
                                "Section Validation",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return null;
                }

                if (capacity <= 0) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Capacity must be greater than zero.",
                                "Section Validation",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return null;
                }

                Long editingId = editingSection == null ? null : editingSection.getId();
                if (isSectionCodeInUse(sectionCode, editingId)) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Section code already exists. Please use a different code.",
                                "Section Validation",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return null;
                }

                return new SectionFormValue(sectionCode, sectionName, capacity, status);
        }

        private boolean isSectionCodeInUse(String sectionCode, Long sectionIdToIgnore) {
                String normalizedSectionCode = sectionCode.trim();

                return sections
                        .stream()
                        .filter(section -> sectionIdToIgnore == null || !sectionIdToIgnore.equals(section.getId()))
                        .map(section -> safeText(section.getSectionCode(), ""))
                        .anyMatch(existingCode -> existingCode.equalsIgnoreCase(normalizedSectionCode));
        }

        private void deleteSelectedSection() {
                Section selectedSection = getSelectedSection();
                if (selectedSection == null) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Please select a section to delete.",
                                "Delete Section",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                int option = JOptionPane.showConfirmDialog(
                        this,
                        "Delete section " + safeText(selectedSection.getSectionCode(), "") + "?",
                        "Confirm Delete",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                if (option != JOptionPane.YES_OPTION) {
                        return;
                }

                boolean deleted = sectionService.deleteSection(selectedSection.getId());
                if (!deleted) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Failed to delete section. It may be referenced by schedules or enrollments.",
                                "Delete Section",
                                JOptionPane.ERROR_MESSAGE
                        );
                        return;
                }

                initializeSections();
                JOptionPane.showMessageDialog(
                        this,
                        "Section deleted successfully.",
                        "Delete Section",
                        JOptionPane.INFORMATION_MESSAGE
                );
        }

        private void clearFilters() {
                txtSearch.setText("");
                if (cbxStatus.getItemCount() > 0) {
                        cbxStatus.setSelectedItem(FILTER_ALL);
                }

                applyFilters();
        }

        private String deriveDisplayStatus(Section section, int enrolledCount, int capacity) {
                String persistedStatus = normalizeStatus(section.getStatus());
                if (capacity > 0 && enrolledCount >= capacity && STATUS_OPEN.equals(persistedStatus)) {
                        return STATUS_CLOSED;
                }

                return persistedStatus;
        }

        private String normalizeStatus(String status) {
                if (status == null || status.trim().isEmpty()) {
                        return STATUS_OPEN;
                }

                String normalized = status.trim().toUpperCase();
                return switch (normalized) {
                        case STATUS_OPEN, STATUS_CLOSED, STATUS_WAITLIST, STATUS_DISSOLVED -> normalized;
                        default -> STATUS_OPEN;
                };
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

        private static final class SectionFormValue {

                private final String sectionCode;
                private final String sectionName;
                private final int capacity;
                private final String status;

                private SectionFormValue(String sectionCode, String sectionName, int capacity, String status) {
                        this.sectionCode = sectionCode;
                        this.sectionName = sectionName;
                        this.capacity = capacity;
                        this.status = status;
                }
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
                jScrollPane1 = new javax.swing.JScrollPane();
                tableSections = new javax.swing.JTable();
                jLabel3 = new javax.swing.JLabel();
                txtSearch = new javax.swing.JTextField();
                jLabel4 = new javax.swing.JLabel();
                cbxStatus = new javax.swing.JComboBox<>();
                btnAddSection = new javax.swing.JButton();
                btnClearFilter = new javax.swing.JButton();

                setBackground(new java.awt.Color(255, 255, 255));
                setPreferredSize(new java.awt.Dimension(1181, 684));
                setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

                jLabel1.setFont(new java.awt.Font("Poppins", 0, 24)); // NOI18N
                jLabel1.setText("Sections Management");
                add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(6, 6, -1, -1));

                jLabel2.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel2.setForeground(new java.awt.Color(153, 153, 153));
                jLabel2.setText("Manage academic sections and capabilities.");
                add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(6, 49, -1, -1));

                jPanel1.setBackground(new java.awt.Color(255, 255, 255));
                jPanel1.setBorder(new com.group5.paul_esys.ui.PanelRoundBorder());
                jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

                tableSections.setModel(new javax.swing.table.DefaultTableModel(
                        new Object [][] {
                                {null, null, null, null, null},
                                {null, null, null, null, null},
                                {null, null, null, null, null},
                                {null, null, null, null, null}
                        },
                        new String [] {
                                "Section Name", "Code", "Capacity", "Enrolled", "Status"
                        }
                ) {
                        Class<?>[] types = new Class<?>[] {
                                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
                        };
                        boolean[] canEdit = new boolean [] {
                                true, false, true, false, false
                        };

                        public Class<?> getColumnClass(int columnIndex) {
                                return types [columnIndex];
                        }

                        public boolean isCellEditable(int rowIndex, int columnIndex) {
                                return canEdit [columnIndex];
                        }
                });
                jScrollPane1.setViewportView(tableSections);

                jPanel1.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(11, 52, 1150, 540));

                jLabel3.setText("Search");
                jPanel1.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 20, -1, -1));

                txtSearch.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());
                txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
                        public void keyReleased(java.awt.event.KeyEvent evt) {
                                txtSearchKeyReleased(evt);
                        }
                });
                jPanel1.add(txtSearch, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 10, 340, 36));

                jLabel4.setText("Status");
                jPanel1.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 20, -1, -1));

                cbxStatus.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "OPEN", "CLOSED", "WAITLIST", "DISSOLVED" }));
                cbxStatus.addItemListener(this::cbxStatusItemStateChanged);
                jPanel1.add(cbxStatus, new org.netbeans.lib.awtextra.AbsoluteConstraints(460, 10, 280, 36));

                btnAddSection.setBackground(new java.awt.Color(119, 0, 0));
                btnAddSection.setForeground(new java.awt.Color(255, 255, 255));
                btnAddSection.setText("Add Section");
                btnAddSection.addActionListener(this::btnAddSectionActionPerformed);
                jPanel1.add(btnAddSection, new org.netbeans.lib.awtextra.AbsoluteConstraints(850, 10, 150, 36));

                btnClearFilter.setBackground(new java.awt.Color(119, 0, 0));
                btnClearFilter.setForeground(new java.awt.Color(255, 255, 255));
                btnClearFilter.setText("Clear Filter");
                btnClearFilter.addActionListener(this::btnClearFilterActionPerformed);
                jPanel1.add(btnClearFilter, new org.netbeans.lib.awtextra.AbsoluteConstraints(1007, 10, 150, 36));

                add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(6, 77, 1170, 600));
        }// </editor-fold>//GEN-END:initComponents

        private void btnAddSectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddSectionActionPerformed
                openCreateSectionDialog();
        }//GEN-LAST:event_btnAddSectionActionPerformed

        private void btnClearFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearFilterActionPerformed
                clearFilters();
        }//GEN-LAST:event_btnClearFilterActionPerformed

        private void txtSearchKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtSearchKeyReleased
                applyFilters();
        }//GEN-LAST:event_txtSearchKeyReleased

        private void cbxStatusItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbxStatusItemStateChanged
                if (evt.getStateChange() == ItemEvent.SELECTED) {
                        applyFilters();
                }
        }//GEN-LAST:event_cbxStatusItemStateChanged


        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton btnAddSection;
        private javax.swing.JButton btnClearFilter;
        private javax.swing.JComboBox<String> cbxStatus;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel4;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JTable tableSections;
        private javax.swing.JTextField txtSearch;
        // End of variables declaration//GEN-END:variables
}
