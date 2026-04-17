/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package com.group5.paul_esys.screens.registrar.panels;

import com.group5.paul_esys.modules.enrollment_period.model.EnrollmentPeriod;
import com.group5.paul_esys.modules.enrollment_period.services.EnrollmentPeriodService;
import com.group5.paul_esys.modules.enrollment_period.utils.EnrollmentPeriodUtils;
import com.group5.paul_esys.screens.registrar.forms.EnrollmentPeriodForm;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author nytri
 */
public class RegistrarEnrollmentPeriodManagement extends javax.swing.JPanel {

        private static final String FILTER_ALL = "ALL";

        private final EnrollmentPeriodService enrollmentPeriodService = EnrollmentPeriodService.getInstance();

        private List<EnrollmentPeriod> enrollmentPeriods = new ArrayList<>();
        private List<EnrollmentPeriod> filteredEnrollmentPeriods = new ArrayList<>();
        private boolean controlsEnabled = true;

	/**
	 * Creates new form EnrollmentPeriodManagement
	 */
	public RegistrarEnrollmentPeriodManagement() {
		initComponents();
		initializeEnrollmentPeriodsPanel();
	}

        private void initializeEnrollmentPeriodsPanel() {
                menuItemUpdate.setText("Update Enrollment Period");
                menuItemDelete.setText("Delete Enrollment Period");

                tableEnrollmentPeriods.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
                tableEnrollmentPeriods.setRowHeight(28);
                tableEnrollmentPeriods.setComponentPopupMenu(popMenu);

                resetStatusFilterModel();
                registerTablePopupSelectionBehavior();
                initializeEnrollmentPeriods();
        }

        private void resetStatusFilterModel() {
                cbxStatus.removeAllItems();
                cbxStatus.addItem(FILTER_ALL);
                cbxStatus.addItem("OPEN");
                cbxStatus.addItem("CLOSED");
                cbxStatus.setSelectedItem(FILTER_ALL);
        }

        private void registerTablePopupSelectionBehavior() {
                tableEnrollmentPeriods.addMouseListener(new MouseAdapter() {
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
                                if (javax.swing.SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() == 2) {
                                        openUpdateEnrollmentPeriodForm();
                                }
                        }
                });
        }

        private void selectRowFromPointer(MouseEvent evt) {
                if (!evt.isPopupTrigger()) {
                        return;
                }

                int row = tableEnrollmentPeriods.rowAtPoint(evt.getPoint());
                if (row >= 0) {
                        tableEnrollmentPeriods.setRowSelectionInterval(row, row);
                }
        }

        private void initializeEnrollmentPeriods() {
                setControlsEnabled(false);

                new SwingWorker<List<EnrollmentPeriod>, Void>() {
                        @Override
                        protected List<EnrollmentPeriod> doInBackground() {
                                return enrollmentPeriodService.getAllEnrollmentPeriods();
                        }

                        @Override
                        protected void done() {
                                try {
                                        enrollmentPeriods = get();
                                        applyFilters();
                                        updateActionAvailability();
                                } catch (InterruptedException | ExecutionException e) {
                                        JOptionPane.showMessageDialog(
                                                RegistrarEnrollmentPeriodManagement.this,
                                                "Failed to load enrollment periods: " + e.getMessage(),
                                                "Error",
                                                JOptionPane.ERROR_MESSAGE
                                        );
                                } finally {
                                        setControlsEnabled(true);
                                }
                        }
                }.execute();
        }

        public void refreshData() {
                initializeEnrollmentPeriods();
        }

        private void setControlsEnabled(boolean enabled) {
                controlsEnabled = enabled;
                btnAddPeriod.setEnabled(enabled);
                txtSearch.setEnabled(enabled);
                cbxStatus.setEnabled(enabled);
                tableEnrollmentPeriods.setEnabled(enabled);
                updateActionAvailability();
        }

        private void updateActionAvailability() {
                boolean mutationAllowed = controlsEnabled && !hasOpenEnrollmentPeriod();

                btnAddPeriod.setEnabled(mutationAllowed);
                menuItemUpdate.setEnabled(mutationAllowed);
                menuItemDelete.setEnabled(mutationAllowed);
        }

        private boolean hasOpenEnrollmentPeriod() {
                return enrollmentPeriodService.getOpenEnrollmentPeriodExcluding(null).isPresent();
        }

        private void applyFilters() {
                String searchTerm = txtSearch.getText() == null
                        ? ""
                        : txtSearch.getText().trim().toLowerCase();

                String selectedStatus = cbxStatus.getSelectedItem() == null
                        ? FILTER_ALL
                        : cbxStatus.getSelectedItem().toString();

                filteredEnrollmentPeriods = enrollmentPeriods
                        .stream()
                        .filter(period -> matchesSearch(period, searchTerm))
                        .filter(period -> matchesStatus(period, selectedStatus))
                        .collect(Collectors.toList());

                populateTable(filteredEnrollmentPeriods);
        }

        private boolean matchesSearch(EnrollmentPeriod period, String searchTerm) {
                if (searchTerm.isEmpty()) {
                        return true;
                }

                String schoolYear = EnrollmentPeriodUtils.safeText(period.getSchoolYear(), "").toLowerCase();
                String semester = EnrollmentPeriodUtils.safeText(period.getSemester(), "").toLowerCase();
                String startDate = EnrollmentPeriodUtils.formatDateTime(period.getStartDate()).toLowerCase();
                String endDate = EnrollmentPeriodUtils.formatDateTime(period.getEndDate()).toLowerCase();
                String description = EnrollmentPeriodUtils.safeText(period.getDescription(), "").toLowerCase();
                String status = EnrollmentPeriodUtils.resolveStatus(period).toLowerCase();

                return schoolYear.contains(searchTerm)
                        || semester.contains(searchTerm)
                        || startDate.contains(searchTerm)
                        || endDate.contains(searchTerm)
                        || description.contains(searchTerm)
                        || status.contains(searchTerm);
        }

        private boolean matchesStatus(EnrollmentPeriod period, String selectedStatus) {
                if (FILTER_ALL.equals(selectedStatus)) {
                        return true;
                }

                return EnrollmentPeriodUtils.resolveStatus(period).equals(selectedStatus);
        }

        private void populateTable(List<EnrollmentPeriod> periodsToDisplay) {
                DefaultTableModel model = (DefaultTableModel) tableEnrollmentPeriods.getModel();
                model.setRowCount(0);

                for (EnrollmentPeriod period : periodsToDisplay) {
                        model.addRow(new Object[]{
                                EnrollmentPeriodUtils.safeText(period.getSchoolYear(), "N/A"),
                                EnrollmentPeriodUtils.safeText(period.getSemester(), "N/A"),
                                EnrollmentPeriodUtils.formatDateTime(period.getStartDate()),
                                EnrollmentPeriodUtils.formatDateTime(period.getEndDate()),
                                EnrollmentPeriodUtils.safeText(period.getDescription(), "-"),
                                EnrollmentPeriodUtils.resolveStatus(period)
                        });
                }
        }

        private EnrollmentPeriod getSelectedEnrollmentPeriod() {
                int selectedRow = tableEnrollmentPeriods.getSelectedRow();
                if (selectedRow < 0) {
                        return null;
                }

                int modelRow = tableEnrollmentPeriods.convertRowIndexToModel(selectedRow);
                if (modelRow < 0 || modelRow >= filteredEnrollmentPeriods.size()) {
                        return null;
                }

                return filteredEnrollmentPeriods.get(modelRow);
        }

        private void openUpdateEnrollmentPeriodForm() {
                if (!controlsEnabled || hasOpenEnrollmentPeriod()) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Enrollment periods cannot be modified while an OPEN enrollment period exists.",
                                "Enrollment Period Locked",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                EnrollmentPeriod selectedPeriod = getSelectedEnrollmentPeriod();
                if (selectedPeriod == null) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Please select an enrollment period to update.",
                                "Update Enrollment Period",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                EnrollmentPeriodForm form = new EnrollmentPeriodForm(selectedPeriod, this::initializeEnrollmentPeriods);
                form.setVisible(true);
        }

        private void deleteSelectedEnrollmentPeriod() {
                if (!controlsEnabled || hasOpenEnrollmentPeriod()) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Enrollment periods cannot be modified while an OPEN enrollment period exists.",
                                "Enrollment Period Locked",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                EnrollmentPeriod selectedPeriod = getSelectedEnrollmentPeriod();
                if (selectedPeriod == null) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Please select an enrollment period to delete.",
                                "Delete Enrollment Period",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                String periodLabel = EnrollmentPeriodUtils.safeText(selectedPeriod.getSchoolYear(), "N/A")
                        + " - "
                        + EnrollmentPeriodUtils.safeText(selectedPeriod.getSemester(), "N/A");

                int option = JOptionPane.showConfirmDialog(
                        this,
                        "<html><body>"
                        + "Are you sure you want to delete enrollment period <b>" + periodLabel + "</b>?<br><br>"
                        + "<font color='red'><b>WARNING:</b> This will also delete ALL offerings and student enrollments<br>"
                        + "within this enrollment period. This action cannot be undone.</font>"
                        + "</body></html>",
                        "Confirm Cascaded Delete",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                if (option != JOptionPane.YES_OPTION) {
                        return;
                }

                setControlsEnabled(false);

                new SwingWorker<Boolean, Void>() {
                        private final Long periodId = selectedPeriod.getId();

                        @Override
                        protected Boolean doInBackground() {
                                return enrollmentPeriodService.deleteEnrollmentPeriod(periodId);
                        }

                        @Override
                        protected void done() {
                                try {
                                        boolean deleted = get();
                                        if (!deleted) {
                                                JOptionPane.showMessageDialog(
                                                        RegistrarEnrollmentPeriodManagement.this,
                                                        "Failed to delete enrollment period. It may be referenced by existing records.",
                                                        "Delete Enrollment Period",
                                                        JOptionPane.ERROR_MESSAGE
                                                );
                                                setControlsEnabled(true);
                                                return;
                                        }

                                        initializeEnrollmentPeriods();
                                        JOptionPane.showMessageDialog(
                                                RegistrarEnrollmentPeriodManagement.this,
                                                "Enrollment period deleted successfully.",
                                                "Delete Enrollment Period",
                                                JOptionPane.INFORMATION_MESSAGE
                                        );
                                } catch (InterruptedException | ExecutionException e) {
                                        JOptionPane.showMessageDialog(
                                                RegistrarEnrollmentPeriodManagement.this,
                                                "Failed to delete enrollment period: " + e.getMessage(),
                                                "Error",
                                                JOptionPane.ERROR_MESSAGE
                                        );
                                        setControlsEnabled(true);
                                }
                        }
                }.execute();
        }

	/**
	 * This method is called from within the constructor to initialize the
	 * form. WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                popMenu = new javax.swing.JPopupMenu();
                menuItemDelete = new javax.swing.JMenuItem();
                menuItemUpdate = new javax.swing.JMenuItem();
                jLabel1 = new javax.swing.JLabel();
                jLabel2 = new javax.swing.JLabel();
                jPanel1 = new javax.swing.JPanel();
                jScrollPane1 = new javax.swing.JScrollPane();
                tableEnrollmentPeriods = new javax.swing.JTable();
                jLabel3 = new javax.swing.JLabel();
                txtSearch = new javax.swing.JTextField();
                btnAddPeriod = new javax.swing.JButton();
                jLabel4 = new javax.swing.JLabel();
                cbxStatus = new javax.swing.JComboBox<>();

                menuItemDelete.setText("jMenuItem1");
                menuItemDelete.addActionListener(this::menuItemDeleteActionPerformed);
                popMenu.add(menuItemDelete);

                menuItemUpdate.setText("jMenuItem2");
                menuItemUpdate.addActionListener(this::menuItemUpdateActionPerformed);
                popMenu.add(menuItemUpdate);

                setBackground(new java.awt.Color(255, 255, 255));
                setMaximumSize(new java.awt.Dimension(1181, 684));
                setMinimumSize(new java.awt.Dimension(1181, 684));

                jLabel1.setFont(new java.awt.Font("Poppins", 0, 24)); // NOI18N
                jLabel1.setText("Enrollment Period Management");

                jLabel2.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel2.setForeground(new java.awt.Color(153, 153, 153));
                jLabel2.setText("Manage academic terms and enrollment schedules");

                jPanel1.setBackground(new java.awt.Color(255, 255, 255));
                jPanel1.setBorder(new com.group5.paul_esys.ui.PanelRoundBorder());

                tableEnrollmentPeriods.setModel(new javax.swing.table.DefaultTableModel(
                        new Object [][] {
                                {null, null, null, null, null, null},
                                {null, null, null, null, null, null},
                                {null, null, null, null, null, null},
                                {null, null, null, null, null, null}
                        },
                        new String [] {
                                "School Year", "Semester", "Start Date", "End Date", "Description", "Status"
                        }
                ) {
                        Class<?>[] types = new Class<?> [] {
                                java.lang.Object.class, java.lang.Object.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
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
                jScrollPane1.setViewportView(tableEnrollmentPeriods);

                jLabel3.setText("Search");

                txtSearch.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());
                txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
                        public void keyReleased(java.awt.event.KeyEvent evt) {
                                txtSearchKeyReleased(evt);
                        }
                });

                btnAddPeriod.setText("Add Enrollment Period");
                btnAddPeriod.addActionListener(this::btnAddPeriodActionPerformed);

                jLabel4.setText("Status");

                cbxStatus.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "OPEN", "CLOSED" }));
                cbxStatus.addItemListener(this::cbxStatusItemStateChanged);

                javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
                jPanel1.setLayout(jPanel1Layout);
                jPanel1Layout.setHorizontalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane1)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(jLabel3)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 350, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel4)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(cbxStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 261, Short.MAX_VALUE)
                                                .addComponent(btnAddPeriod, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap())
                );
                jPanel1Layout.setVerticalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel3)
                                        .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnAddPeriod)
                                        .addComponent(jLabel4)
                                        .addComponent(cbxStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 550, Short.MAX_VALUE)
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
                                                        .addComponent(jLabel2)
                                                        .addComponent(jLabel1))
                                                .addGap(0, 0, Short.MAX_VALUE)))
                                .addContainerGap())
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

        private void menuItemUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemUpdateActionPerformed
                openUpdateEnrollmentPeriodForm();
        }//GEN-LAST:event_menuItemUpdateActionPerformed

        private void menuItemDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemDeleteActionPerformed
                deleteSelectedEnrollmentPeriod();
        }//GEN-LAST:event_menuItemDeleteActionPerformed

        private void txtSearchKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtSearchKeyReleased
                applyFilters();
        }//GEN-LAST:event_txtSearchKeyReleased

        private void cbxStatusItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbxStatusItemStateChanged
                if (evt.getStateChange() == ItemEvent.SELECTED) {
                        applyFilters();
                }
        }//GEN-LAST:event_cbxStatusItemStateChanged

        private void btnAddPeriodActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddPeriodActionPerformed
                if (!controlsEnabled || hasOpenEnrollmentPeriod()) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Enrollment periods cannot be modified while an OPEN enrollment period exists.",
                                "Enrollment Period Locked",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                EnrollmentPeriodForm form = new EnrollmentPeriodForm(null, this::initializeEnrollmentPeriods);
                form.setVisible(true);
        }//GEN-LAST:event_btnAddPeriodActionPerformed


        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton btnAddPeriod;
        private javax.swing.JComboBox<String> cbxStatus;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel4;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JMenuItem menuItemDelete;
        private javax.swing.JMenuItem menuItemUpdate;
        private javax.swing.JPopupMenu popMenu;
        private javax.swing.JTable tableEnrollmentPeriods;
        private javax.swing.JTextField txtSearch;
        // End of variables declaration//GEN-END:variables
}
