/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package com.group5.paul_esys.screens.registrar.panels;

import com.group5.paul_esys.modules.enums.DropRequestStatus;
import com.group5.paul_esys.modules.registrar.model.FacultyStudentDropRequest;
import com.group5.paul_esys.modules.registrar.services.RegistrarDropRequestService;
import com.group5.paul_esys.screens.registrar.forms.RegistrarManualDropForm;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.Window;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author nytri
 */
public final class RegistrarDropRequestsManagement extends javax.swing.JPanel {

        private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        private final RegistrarDropRequestService dropRequestService = RegistrarDropRequestService.getInstance();
        private final List<FacultyStudentDropRequest> dropRequests = new ArrayList<>();
        private transient SwingWorker<?, ?> currentWorker;
        private boolean suppressStatusFilterEvent;

        /**
         * Creates new form RegistrarDropRequestsManagement
         */
        public RegistrarDropRequestsManagement() {
                initComponents();
                initializePanel();
        }

        private void initializePanel() {
                configureTable();
                configureStatusFilter();
                clearRequestDetails();
                loadDropRequestsAsync();
        }

        private void configureTable() {
                tableDropRequests.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
                tableDropRequests.setRowHeight(28);
                tableDropRequests.getSelectionModel().addListSelectionListener(evt -> {
                        if (!evt.getValueIsAdjusting()) {
                                populateSelectedRequestDetails();
                        }
                });
        }

        private void configureStatusFilter() {
                suppressStatusFilterEvent = true;
                try {
                        cbxStatusFilter.removeAllItems();
                        cbxStatusFilter.addItem("PENDING");
                        cbxStatusFilter.addItem("ALL");
                        cbxStatusFilter.addItem("APPROVED");
                        cbxStatusFilter.addItem("REJECTED");
                        cbxStatusFilter.setSelectedItem("PENDING");
                } finally {
                        suppressStatusFilterEvent = false;
                }
        }

        private void setLoading(boolean loading) {
                setCursor(loading
                        ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                        : Cursor.getDefaultCursor());

                btnRefresh.setEnabled(!loading);
                btnApprove.setEnabled(!loading);
                btnReject.setEnabled(!loading);
                btnManualDrop.setEnabled(!loading);
        }

        private void cancelCurrentWorker() {
                if (currentWorker != null && !currentWorker.isDone()) {
                        currentWorker.cancel(true);
                }
        }

        private void executeAsync(SwingWorker<?, ?> worker) {
                cancelCurrentWorker();
                currentWorker = worker;
                setLoading(true);
                worker.addPropertyChangeListener(evt -> {
                        if ("state".equals(evt.getPropertyName())
                                && SwingWorker.StateValue.DONE == evt.getNewValue()) {
                                setLoading(false);
                        }
                });
                worker.execute();
        }

        private void loadDropRequestsAsync() {
                DropRequestStatus selectedStatus = resolveSelectedStatusFilter();

                SwingWorker<List<FacultyStudentDropRequest>, Void> worker = new SwingWorker<>() {
                        @Override
                        protected List<FacultyStudentDropRequest> doInBackground() {
                                return dropRequestService.getDropRequests(selectedStatus);
                        }

                        @Override
                        protected void done() {
                                try {
                                        List<FacultyStudentDropRequest> rows = get();
                                        refreshDropRequestsTable(rows);
                                } catch (Exception e) {
                                        JOptionPane.showMessageDialog(
                                                RegistrarDropRequestsManagement.this,
                                                "Failed to load drop requests: " + e.getMessage(),
                                                "Drop Requests",
                                                JOptionPane.ERROR_MESSAGE
                                        );
                                }
                        }
                };

                executeAsync(worker);
        }

        private DropRequestStatus resolveSelectedStatusFilter() {
                Object selectedItem = cbxStatusFilter.getSelectedItem();
                if (selectedItem == null) {
                        return DropRequestStatus.PENDING;
                }

                String value = selectedItem.toString().trim().toUpperCase();
                if ("ALL".equals(value)) {
                        return null;
                }

                return DropRequestStatus.fromValue(value);
        }

        private void refreshDropRequestsTable(List<FacultyStudentDropRequest> rows) {
                dropRequests.clear();
                if (rows != null) {
                        dropRequests.addAll(rows);
                }

                DefaultTableModel model = (DefaultTableModel) tableDropRequests.getModel();
                model.setRowCount(0);

                for (FacultyStudentDropRequest request : dropRequests) {
                        model.addRow(new Object[] {
                                request.getId(),
                                request.getStatus() == null ? "N/A" : request.getStatus().name(),
                                safeText(request.getStudentId(), "N/A"),
                                safeText(request.getStudentName(), "N/A"),
                                buildOfferingLabel(request),
                                safeText(request.getFacultyName(), "N/A"),
                                safeText(request.getEnrollmentDetailStatus(), "N/A"),
                                formatTimestamp(request.getCreatedAt())
                        });
                }

                lblCount.setText("Rows: " + dropRequests.size());

                if (!dropRequests.isEmpty()) {
                        tableDropRequests.setRowSelectionInterval(0, 0);
                } else {
                        clearRequestDetails();
                }
        }

        private void populateSelectedRequestDetails() {
                FacultyStudentDropRequest selectedRequest = getSelectedRequest();
                if (selectedRequest == null) {
                        clearRequestDetails();
                        return;
                }

                txtRequestId.setText(String.valueOf(selectedRequest.getId()));
                txtStudent.setText(safeText(selectedRequest.getStudentId(), "") + " - " + safeText(selectedRequest.getStudentName(), "N/A"));
                txtOffering.setText(buildOfferingLabel(selectedRequest));
                txtFaculty.setText(safeText(selectedRequest.getFacultyName(), "N/A"));
                txtRequestStatus.setText(selectedRequest.getStatus() == null ? "N/A" : selectedRequest.getStatus().name());
                txtRequestedAt.setText(formatTimestamp(selectedRequest.getCreatedAt()));
                txtReason.setText(safeText(selectedRequest.getReason(), "No reason provided."));
        }

        private void clearRequestDetails() {
                txtRequestId.setText("");
                txtStudent.setText("");
                txtOffering.setText("");
                txtFaculty.setText("");
                txtRequestStatus.setText("");
                txtRequestedAt.setText("");
                txtReason.setText("");
        }

        private FacultyStudentDropRequest getSelectedRequest() {
                int selectedRow = tableDropRequests.getSelectedRow();
                if (selectedRow < 0) {
                        return null;
                }

                int modelRow = tableDropRequests.convertRowIndexToModel(selectedRow);
                if (modelRow < 0 || modelRow >= tableDropRequests.getModel().getRowCount()) {
                        return null;
                }

                Object idValue = tableDropRequests.getModel().getValueAt(modelRow, 0);
                if (idValue == null) {
                        return null;
                }

                Long selectedId;
                if (idValue instanceof Number number) {
                        selectedId = number.longValue();
                } else {
                        try {
                                selectedId = Long.valueOf(idValue.toString());
                        } catch (NumberFormatException ex) {
                                return null;
                        }
                }

                return dropRequests
                        .stream()
                        .filter(request -> selectedId.equals(request.getId()))
                        .findFirst()
                        .orElse(null);
        }

        private String buildOfferingLabel(FacultyStudentDropRequest request) {
                return safeText(request.getSubjectCode(), "N/A")
                        + " - "
                        + safeText(request.getSubjectName(), "N/A")
                        + " | Sec "
                        + safeText(request.getSectionCode(), "N/A")
                        + " | "
                        + safeText(request.getEnrollmentPeriodLabel(), "N/A")
                        + " | Offering #"
                        + safeText(request.getOfferingId() == null ? null : String.valueOf(request.getOfferingId()), "N/A");
        }

        private String formatTimestamp(java.sql.Timestamp timestamp) {
                if (timestamp == null) {
                        return "N/A";
                }

                return timestamp
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()
                        .format(DATE_TIME_FORMATTER);
        }

        private String safeText(String value, String fallback) {
                if (value == null) {
                        return fallback;
                }

                String normalized = value.trim();
                return normalized.isEmpty() ? fallback : normalized;
        }

        private void approveSelectedRequest() {
                FacultyStudentDropRequest selectedRequest = getSelectedRequest();
                if (selectedRequest == null) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Please select a drop request first.",
                                "Approve Request",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                if (selectedRequest.getStatus() != DropRequestStatus.PENDING) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Only pending requests can be approved.",
                                "Approve Request",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        "Approve this request and drop the student from the offering?",
                        "Approve Request",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

                if (confirm != JOptionPane.YES_OPTION) {
                        return;
                }

                boolean approved = dropRequestService.approveDropRequest(selectedRequest.getId());
                if (!approved) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Failed to approve request. Please verify enrollment data and try again.",
                                "Approve Request",
                                JOptionPane.ERROR_MESSAGE
                        );
                        return;
                }

                JOptionPane.showMessageDialog(
                        this,
                        "Request approved and student dropped successfully.",
                        "Approve Request",
                        JOptionPane.INFORMATION_MESSAGE
                );
                loadDropRequestsAsync();
        }

        private void rejectSelectedRequest() {
                FacultyStudentDropRequest selectedRequest = getSelectedRequest();
                if (selectedRequest == null) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Please select a drop request first.",
                                "Reject Request",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                if (selectedRequest.getStatus() != DropRequestStatus.PENDING) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Only pending requests can be rejected.",
                                "Reject Request",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        "Reject the selected drop request?",
                        "Reject Request",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

                if (confirm != JOptionPane.YES_OPTION) {
                        return;
                }

                boolean rejected = dropRequestService.rejectDropRequest(selectedRequest.getId());
                if (!rejected) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Failed to reject request. Please try again.",
                                "Reject Request",
                                JOptionPane.ERROR_MESSAGE
                        );
                        return;
                }

                JOptionPane.showMessageDialog(
                        this,
                        "Request rejected successfully.",
                        "Reject Request",
                        JOptionPane.INFORMATION_MESSAGE
                );
                loadDropRequestsAsync();
        }

        private void openManualDropDialog() {
                Window owner = SwingUtilities.getWindowAncestor(this);
                Frame parent = owner instanceof Frame frame ? frame : null;

                RegistrarManualDropForm dialog = new RegistrarManualDropForm(parent, true, this::loadDropRequestsAsync);
                dialog.setVisible(true);
        }

        /**
         * This method is called from within the constructor to initialize the form.
         * WARNING: Do NOT modify this code. The content of this method is always
         * regenerated by the Form Editor.
         */
        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                jPanelHeader = new javax.swing.JPanel();
                jLabel1 = new javax.swing.JLabel();
                jLabel2 = new javax.swing.JLabel();
                jLabel3 = new javax.swing.JLabel();
                cbxStatusFilter = new javax.swing.JComboBox<>();
                btnRefresh = new javax.swing.JButton();
                btnManualDrop = new javax.swing.JButton();
                btnReject = new javax.swing.JButton();
                btnApprove = new javax.swing.JButton();
                lblCount = new javax.swing.JLabel();
                jScrollPane1 = new javax.swing.JScrollPane();
                tableDropRequests = new javax.swing.JTable();
                jPanelDetails = new javax.swing.JPanel();
                jLabel4 = new javax.swing.JLabel();
                txtRequestId = new javax.swing.JTextField();
                jLabel5 = new javax.swing.JLabel();
                txtStudent = new javax.swing.JTextField();
                jLabel6 = new javax.swing.JLabel();
                txtOffering = new javax.swing.JTextField();
                jLabel7 = new javax.swing.JLabel();
                txtFaculty = new javax.swing.JTextField();
                jLabel8 = new javax.swing.JLabel();
                txtRequestStatus = new javax.swing.JTextField();
                jLabel9 = new javax.swing.JLabel();
                txtRequestedAt = new javax.swing.JTextField();
                jLabel10 = new javax.swing.JLabel();
                jScrollPane2 = new javax.swing.JScrollPane();
                txtReason = new javax.swing.JTextArea();

                setBackground(new java.awt.Color(255, 255, 255));

                jPanelHeader.setBackground(new java.awt.Color(255, 255, 255));

                jLabel1.setFont(new java.awt.Font("Poppins", 0, 18)); // NOI18N
                jLabel1.setText("Drop Requests");

                jLabel2.setForeground(new java.awt.Color(102, 102, 102));
                jLabel2.setText("Review faculty drop requests and perform registrar manual drops");

                jLabel3.setText("Status");

                cbxStatusFilter.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "PENDING", "ALL", "APPROVED", "REJECTED" }));
                cbxStatusFilter.addActionListener(this::cbxStatusFilterActionPerformed);

                btnRefresh.setText("Refresh");
                btnRefresh.addActionListener(this::btnRefreshActionPerformed);

                btnManualDrop.setBackground(new java.awt.Color(119, 0, 0));
                btnManualDrop.setForeground(new java.awt.Color(255, 255, 255));
                btnManualDrop.setText("Manual Drop");
                btnManualDrop.addActionListener(this::btnManualDropActionPerformed);

                btnReject.setText("Reject Request");
                btnReject.addActionListener(this::btnRejectActionPerformed);

                btnApprove.setBackground(new java.awt.Color(119, 0, 0));
                btnApprove.setForeground(new java.awt.Color(255, 255, 255));
                btnApprove.setText("Approve Request");
                btnApprove.addActionListener(this::btnApproveActionPerformed);

                lblCount.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
                lblCount.setText("Rows: 0");

                javax.swing.GroupLayout jPanelHeaderLayout = new javax.swing.GroupLayout(jPanelHeader);
                jPanelHeader.setLayout(jPanelHeaderLayout);
                jPanelHeaderLayout.setHorizontalGroup(
                        jPanelHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanelHeaderLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanelHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel1)
                                        .addComponent(jLabel2)
                                        .addGroup(jPanelHeaderLayout.createSequentialGroup()
                                                .addComponent(jLabel3)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(cbxStatusFilter, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(12, 12, 12)
                                                .addComponent(btnRefresh)
                                                .addGap(18, 18, 18)
                                                .addComponent(lblCount, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 220, Short.MAX_VALUE)
                                                .addComponent(btnManualDrop)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnReject)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnApprove)))
                                .addContainerGap())
                );
                jPanelHeaderLayout.setVerticalGroup(
                        jPanelHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanelHeaderLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel2)
                                .addGap(12, 12, 12)
                                .addGroup(jPanelHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel3)
                                        .addComponent(cbxStatusFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnRefresh)
                                        .addComponent(btnManualDrop)
                                        .addComponent(btnReject)
                                        .addComponent(btnApprove)
                                        .addComponent(lblCount))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );

                tableDropRequests.setModel(new javax.swing.table.DefaultTableModel(
                        new Object [][] {

                        },
                        new String [] {
                                "Request #", "Status", "Student ID", "Student Name", "Offering", "Faculty", "Enrollment Detail", "Requested At"
                        }
                ) {
                        boolean[] canEdit = new boolean [] {
                                false, false, false, false, false, false, false, false
                        };

                        public boolean isCellEditable(int rowIndex, int columnIndex) {
                                return canEdit [columnIndex];
                        }
                });
                jScrollPane1.setViewportView(tableDropRequests);

                jPanelDetails.setBackground(new java.awt.Color(255, 255, 255));
                jPanelDetails.setBorder(javax.swing.BorderFactory.createTitledBorder("Selected Request"));

                jLabel4.setText("Request #");

                txtRequestId.setEditable(false);
                txtRequestId.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                jLabel5.setText("Student");

                txtStudent.setEditable(false);
                txtStudent.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                jLabel6.setText("Offering");

                txtOffering.setEditable(false);
                txtOffering.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                jLabel7.setText("Faculty");

                txtFaculty.setEditable(false);
                txtFaculty.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                jLabel8.setText("Status");

                txtRequestStatus.setEditable(false);
                txtRequestStatus.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                jLabel9.setText("Requested At");

                txtRequestedAt.setEditable(false);
                txtRequestedAt.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                jLabel10.setText("Reason");

                txtReason.setEditable(false);
                txtReason.setColumns(20);
                txtReason.setRows(3);
                txtReason.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());
                jScrollPane2.setViewportView(txtReason);

                javax.swing.GroupLayout jPanelDetailsLayout = new javax.swing.GroupLayout(jPanelDetails);
                jPanelDetails.setLayout(jPanelDetailsLayout);
                jPanelDetailsLayout.setHorizontalGroup(
                        jPanelDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanelDetailsLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanelDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane2)
                                        .addComponent(jLabel10)
                                        .addGroup(jPanelDetailsLayout.createSequentialGroup()
                                                .addGroup(jPanelDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                        .addComponent(jLabel4)
                                                        .addComponent(jLabel7)
                                                        .addComponent(txtFaculty, javax.swing.GroupLayout.DEFAULT_SIZE, 240, Short.MAX_VALUE)
                                                        .addComponent(txtRequestId))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(jPanelDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                        .addComponent(jLabel5)
                                                        .addComponent(txtStudent, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
                                                        .addComponent(jLabel8)
                                                        .addComponent(txtRequestStatus))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(jPanelDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(txtOffering, javax.swing.GroupLayout.DEFAULT_SIZE, 435, Short.MAX_VALUE)
                                                        .addComponent(txtRequestedAt)
                                                        .addGroup(jPanelDetailsLayout.createSequentialGroup()
                                                                .addGroup(jPanelDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                        .addComponent(jLabel6)
                                                                        .addComponent(jLabel9))
                                                                .addGap(0, 0, Short.MAX_VALUE)))))
                                .addContainerGap())
                );
                jPanelDetailsLayout.setVerticalGroup(
                        jPanelDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanelDetailsLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanelDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel4)
                                        .addComponent(jLabel5)
                                        .addComponent(jLabel6))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanelDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(txtRequestId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(txtStudent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(txtOffering, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(12, 12, 12)
                                .addGroup(jPanelDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel7)
                                        .addComponent(jLabel8)
                                        .addComponent(jLabel9))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanelDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(txtFaculty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(txtRequestStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(txtRequestedAt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(12, 12, 12)
                                .addComponent(jLabel10)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 74, Short.MAX_VALUE)
                                .addContainerGap())
                );

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
                this.setLayout(layout);
                layout.setHorizontalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jPanelHeader, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane1)
                                        .addComponent(jPanelDetails, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(jPanelHeader, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 360, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanelDetails, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
                );
        }// </editor-fold>//GEN-END:initComponents

        private void cbxStatusFilterActionPerformed(java.awt.event.ActionEvent evt) {
                if (!suppressStatusFilterEvent) {
                        loadDropRequestsAsync();
                }
        }

        private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {
                loadDropRequestsAsync();
        }

        private void btnApproveActionPerformed(java.awt.event.ActionEvent evt) {
                approveSelectedRequest();
        }

        private void btnRejectActionPerformed(java.awt.event.ActionEvent evt) {
                rejectSelectedRequest();
        }

        private void btnManualDropActionPerformed(java.awt.event.ActionEvent evt) {
                openManualDropDialog();
        }

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton btnApprove;
        private javax.swing.JButton btnManualDrop;
        private javax.swing.JButton btnRefresh;
        private javax.swing.JButton btnReject;
        private javax.swing.JComboBox<String> cbxStatusFilter;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel10;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel4;
        private javax.swing.JLabel jLabel5;
        private javax.swing.JLabel jLabel6;
        private javax.swing.JLabel jLabel7;
        private javax.swing.JLabel jLabel8;
        private javax.swing.JLabel jLabel9;
        private javax.swing.JPanel jPanelDetails;
        private javax.swing.JPanel jPanelHeader;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JScrollPane jScrollPane2;
        private javax.swing.JLabel lblCount;
        private javax.swing.JTable tableDropRequests;
        private javax.swing.JTextField txtFaculty;
        private javax.swing.JTextField txtOffering;
        private javax.swing.JTextArea txtReason;
        private javax.swing.JTextField txtRequestId;
        private javax.swing.JTextField txtRequestStatus;
        private javax.swing.JTextField txtRequestedAt;
        private javax.swing.JTextField txtStudent;
        // End of variables declaration//GEN-END:variables
}
