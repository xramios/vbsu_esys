/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package com.group5.paul_esys.screens.registrar.panels;

import com.group5.paul_esys.modules.sections.model.Section;
import com.group5.paul_esys.modules.sections.services.SectionService;
import com.group5.paul_esys.screens.registrar.forms.SectionForm;
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
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
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
                        new String[]{"Section Code", "Capacity", "Enrolled", "Status"}
                ) {
                        @Override
                        public Class<?> getColumnClass(int columnIndex) {
                                return switch (columnIndex) {
                                        case 1, 2 -> Integer.class;
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
                new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                                List<Section> latestSections = sectionService.getAllSections();
                                Map<Long, Integer> latestEnrolledCount = sectionService.getSelectedEnrollmentCountBySectionId();
                                SwingUtilities.invokeLater(() ->
                                        applySectionSnapshot(latestSections, latestEnrolledCount, true)
                                );
                                return null;
                        }
                }.execute();
        }

        private void refreshSectionsIfChanged() {
                new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                                List<Section> latestSections = sectionService.getAllSections();
                                Map<Long, Integer> latestEnrolledCount = sectionService.getSelectedEnrollmentCountBySectionId();
                                SwingUtilities.invokeLater(() ->
                                        applySectionSnapshot(latestSections, latestEnrolledCount, false)
                                );
                                return null;
                        }
                }.execute();
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

                String sectionCode = safeText(section.getSectionCode(), "").toLowerCase();

                return sectionCode.contains(searchTerm);
        }

        private boolean matchesStatus(Section section, String selectedStatus) {
                if (FILTER_ALL.equals(selectedStatus)) {
                        return true;
                }

                return resolveEffectiveStatus(section).equals(selectedStatus);
        }

        private String resolveEffectiveStatus(Section section) {
                int capacity = normalizeCapacity(section.getCapacity());
                int enrolledCount = enrolledCountBySectionId.getOrDefault(section.getId(), 0);

                return deriveDisplayStatus(section, enrolledCount, capacity);
        }

        private void populateTable(List<Section> sectionsToDisplay) {
                DefaultTableModel model = (DefaultTableModel) tableSections.getModel();
                model.setRowCount(0);

                for (Section section : sectionsToDisplay) {
                        int capacity = normalizeCapacity(section.getCapacity());
                        int enrolledCount = enrolledCountBySectionId.getOrDefault(section.getId(), 0);
                        String displayStatus = deriveDisplayStatus(section, enrolledCount, capacity);

                        model.addRow(new Object[]{
                                safeText(section.getSectionCode(), "N/A"),
                                capacity,
                                enrolledCount,
                                displayStatus
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
                SectionForm form = new SectionForm(null, this::refreshSections);
                form.setVisible(true);
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

                Long selectedSectionId = selectedSection.getId();
                SectionForm form = new SectionForm(selectedSection, () -> {
                        refreshSections();
                        selectSectionById(selectedSectionId);
                });
                form.setVisible(true);
        }

        private void refreshSections() {
                initializeSections();
                tableSections.revalidate();
                tableSections.repaint();
        }

        private void selectSectionById(Long sectionId) {
                if (sectionId == null) {
                        return;
                }

                for (int row = 0; row < filteredSections.size(); row++) {
                        Section section = filteredSections.get(row);
                        if (sectionId.equals(section.getId())) {
                                int viewRow = tableSections.convertRowIndexToView(row);
                                if (viewRow >= 0) {
                                        tableSections.setRowSelectionInterval(viewRow, viewRow);
                                }
                                return;
                        }
                }
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
                        "<html><body>"
                        + "Are you sure you want to delete section <b>" + safeText(selectedSection.getSectionCode(), "") + "</b>?<br><br>"
                        + "<font color='red'><b>WARNING:</b> This will also delete ALL offerings and student enrollments<br>"
                        + "associated with this section. This action cannot be undone.</font>"
                        + "</body></html>",
                        "Confirm Cascaded Delete",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                if (option != JOptionPane.YES_OPTION) {
                        return;
                }

                new SwingWorker<Boolean, Void>() {
                        @Override
                        protected Boolean doInBackground() throws Exception {
                                return sectionService.deleteSection(selectedSection.getId());
                        }

                        @Override
                        protected void done() {
                                try {
                                        boolean deleted = get();
                                        if (!deleted) {
                                                JOptionPane.showMessageDialog(
                                                        RegistrarSectionsManagement.this,
                                                        "Failed to delete section. It may be referenced by schedules or enrollments.",
                                                        "Delete Section",
                                                        JOptionPane.ERROR_MESSAGE
                                                );
                                                return;
                                        }
                                        initializeSections();
                                        JOptionPane.showMessageDialog(
                                                RegistrarSectionsManagement.this,
                                                "Section deleted successfully.",
                                                "Delete Section",
                                                JOptionPane.INFORMATION_MESSAGE
                                        );
                                } catch (Exception ex) {
                                        JOptionPane.showMessageDialog(
                                                RegistrarSectionsManagement.this,
                                                "Error deleting section: " + ex.getMessage(),
                                                "Delete Section",
                                                JOptionPane.ERROR_MESSAGE
                                        );
                                }
                        }
                }.execute();
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

                jLabel1.setFont(new java.awt.Font("Poppins", 0, 24)); // NOI18N
                jLabel1.setText("Sections Management");

                jLabel2.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel2.setForeground(new java.awt.Color(153, 153, 153));
                jLabel2.setText("Manage academic sections and capabilities.");

                jPanel1.setBackground(new java.awt.Color(255, 255, 255));
                jPanel1.setBorder(new com.group5.paul_esys.ui.PanelRoundBorder());

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
                        Class<?>[] types = new Class<?> [] {
                                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
                        };
                        boolean[] canEdit = new boolean [] {
                                true, false, true, false, false
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
                jScrollPane1.setViewportView(tableSections);

                jLabel3.setText("Search");

                txtSearch.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());
                txtSearch.addActionListener(this::txtSearchActionPerformed);
                txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
                        public void keyReleased(java.awt.event.KeyEvent evt) {
                                txtSearchKeyReleased(evt);
                        }
                });

                jLabel4.setText("Status");

                cbxStatus.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "OPEN", "CLOSED", "WAITLIST", "DISSOLVED" }));
                cbxStatus.addItemListener(this::cbxStatusItemStateChanged);

                btnAddSection.setBackground(new java.awt.Color(119, 0, 0));
                btnAddSection.setForeground(new java.awt.Color(255, 255, 255));
                btnAddSection.setText("Add Section");
                btnAddSection.addActionListener(this::btnAddSectionActionPerformed);

                btnClearFilter.setBackground(new java.awt.Color(119, 0, 0));
                btnClearFilter.setForeground(new java.awt.Color(255, 255, 255));
                btnClearFilter.setText("Clear Filter");
                btnClearFilter.addActionListener(this::btnClearFilterActionPerformed);

                javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
                jPanel1.setLayout(jPanel1Layout);
                jPanel1Layout.setHorizontalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addGap(7, 7, 7)
                                                .addComponent(jLabel3)
                                                .addGap(12, 12, 12)
                                                .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 340, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel4)
                                                .addGap(6, 6, 6)
                                                .addComponent(cbxStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 128, Short.MAX_VALUE)
                                                .addComponent(btnAddSection, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnClearFilter, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addGap(8, 8, 8)
                                                .addComponent(jScrollPane1)))
                                .addContainerGap())
                );
                jPanel1Layout.setVerticalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(7, 7, 7)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(btnAddSection, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(btnClearFilter, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addComponent(cbxStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addGap(10, 10, 10)
                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel3)
                                                        .addComponent(jLabel4))))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 540, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
                this.setLayout(layout);
                layout.setHorizontalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel1)
                                        .addComponent(jLabel2)
                                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(jLabel1)
                                .addGap(6, 6, 6)
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );
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

        private void txtSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtSearchActionPerformed
                // TODO add your handling code here:
        }//GEN-LAST:event_txtSearchActionPerformed


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
