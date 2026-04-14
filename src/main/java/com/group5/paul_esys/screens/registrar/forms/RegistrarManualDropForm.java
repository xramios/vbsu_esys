/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JDialog.java to edit this template
 */
package com.group5.paul_esys.screens.registrar.forms;

import com.group5.paul_esys.modules.registrar.model.StudentDropCandidate;
import com.group5.paul_esys.modules.registrar.model.StudentDropTargetOption;
import com.group5.paul_esys.modules.registrar.services.RegistrarDropRequestService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;

/**
 *
 * @author nytri
 */
public class RegistrarManualDropForm extends javax.swing.JDialog {

        private final RegistrarDropRequestService dropRequestService = RegistrarDropRequestService.getInstance();
        private final Map<String, StudentDropCandidate> studentByLabel = new LinkedHashMap<>();
        private final Map<String, StudentDropTargetOption> offeringByLabel = new LinkedHashMap<>();
        private final Runnable onDroppedCallback;

        /**
         * Creates new form RegistrarManualDropForm
         */
        public RegistrarManualDropForm(java.awt.Frame parent, boolean modal) {
                this(parent, modal, null);
        }

        public RegistrarManualDropForm(java.awt.Frame parent, boolean modal, Runnable onDroppedCallback) {
                super(parent, modal);
                this.onDroppedCallback = onDroppedCallback;
                initComponents();
                initializeForm(parent);
        }

        private void initializeForm(java.awt.Frame parent) {
                setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
                setLocationRelativeTo(parent);
                loadStudents();
                loadOfferingsForSelectedStudent();
        }

        private void loadStudents() {
                studentByLabel.clear();
                cbxStudent.removeAllItems();

                List<StudentDropCandidate> candidates = dropRequestService.getStudentsWithDroppableOfferings();
                if (candidates.isEmpty()) {
                        cbxStudent.addItem("No students with droppable offerings");
                        cbxStudent.setEnabled(false);
                        cbxOffering.setEnabled(false);
                        btnDropStudent.setEnabled(false);
                        return;
                }

                cbxStudent.setEnabled(true);
                cbxOffering.setEnabled(true);
                btnDropStudent.setEnabled(true);

                for (StudentDropCandidate candidate : candidates) {
                        String label = safeText(candidate.getStudentId(), "N/A")
                                + " - "
                                + safeText(candidate.getStudentName(), "N/A");
                        cbxStudent.addItem(label);
                        studentByLabel.put(label, candidate);
                }
        }

        private void loadOfferingsForSelectedStudent() {
                offeringByLabel.clear();
                cbxOffering.removeAllItems();

                StudentDropCandidate selectedStudent = getSelectedStudentCandidate();
                if (selectedStudent == null) {
                        cbxOffering.addItem("No offerings available");
                        cbxOffering.setEnabled(false);
                        btnDropStudent.setEnabled(false);
                        return;
                }

                List<StudentDropTargetOption> options = dropRequestService.getDroppableOfferingsByStudent(selectedStudent.getStudentId());
                if (options.isEmpty()) {
                        cbxOffering.addItem("No offerings available");
                        cbxOffering.setEnabled(false);
                        btnDropStudent.setEnabled(false);
                        return;
                }

                cbxOffering.setEnabled(true);
                btnDropStudent.setEnabled(true);

                for (StudentDropTargetOption option : options) {
                        String label = buildOfferingLabel(option);
                        cbxOffering.addItem(label);
                        offeringByLabel.put(label, option);
                }
        }

        private String buildOfferingLabel(StudentDropTargetOption option) {
                return safeText(option.getSubjectCode(), "N/A")
                        + " - "
                        + safeText(option.getSubjectName(), "N/A")
                        + " | Sec "
                        + safeText(option.getSectionCode(), "N/A")
                        + " | "
                        + safeText(option.getEnrollmentPeriodLabel(), "N/A")
                        + " | Enrollment "
                        + safeText(option.getEnrollmentStatus(), "N/A")
                        + " | Offering #"
                        + safeText(option.getOfferingId() == null ? null : String.valueOf(option.getOfferingId()), "N/A");
        }

        private StudentDropCandidate getSelectedStudentCandidate() {
                Object selectedItem = cbxStudent.getSelectedItem();
                if (selectedItem == null) {
                        return null;
                }

                return studentByLabel.get(selectedItem.toString());
        }

        private StudentDropTargetOption getSelectedDropTargetOption() {
                Object selectedItem = cbxOffering.getSelectedItem();
                if (selectedItem == null) {
                        return null;
                }

                return offeringByLabel.get(selectedItem.toString());
        }

        private String safeText(String value, String fallback) {
                if (value == null) {
                        return fallback;
                }

                String normalized = value.trim();
                return normalized.isEmpty() ? fallback : normalized;
        }

        /**
         * This method is called from within the constructor to initialize the form.
         * WARNING: Do NOT modify this code. The content of this method is always
         * regenerated by the Form Editor.
         */
        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                jPanel1 = new javax.swing.JPanel();
                jLabel1 = new javax.swing.JLabel();
                jLabel2 = new javax.swing.JLabel();
                jLabel3 = new javax.swing.JLabel();
                cbxStudent = new javax.swing.JComboBox<>();
                jLabel4 = new javax.swing.JLabel();
                cbxOffering = new javax.swing.JComboBox<>();
                jLabel5 = new javax.swing.JLabel();
                jScrollPane1 = new javax.swing.JScrollPane();
                txtReason = new javax.swing.JTextArea();
                btnCancel = new javax.swing.JButton();
                btnDropStudent = new javax.swing.JButton();

                setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
                setTitle("Manual Drop Student");

                jPanel1.setBackground(new java.awt.Color(255, 255, 255));

                jLabel1.setFont(new java.awt.Font("Poppins", 0, 18)); // NOI18N
                jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel1.setText("Manual Drop Student");

                jLabel2.setForeground(new java.awt.Color(102, 102, 102));
                jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel2.setText("Drop a student directly from an active offering");

                jLabel3.setText("Student");

                cbxStudent.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "" }));
                cbxStudent.addActionListener(this::cbxStudentActionPerformed);

                jLabel4.setText("Offering");

                cbxOffering.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "" }));

                jLabel5.setText("Reason (Optional)");

                txtReason.setColumns(20);
                txtReason.setRows(4);
                jScrollPane1.setViewportView(txtReason);

                btnCancel.setText("Cancel");
                btnCancel.addActionListener(this::btnCancelActionPerformed);

                btnDropStudent.setBackground(new java.awt.Color(119, 0, 0));
                btnDropStudent.setForeground(new java.awt.Color(255, 255, 255));
                btnDropStudent.setText("Drop Student");
                btnDropStudent.addActionListener(this::btnDropStudentActionPerformed);

                javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
                jPanel1.setLayout(jPanel1Layout);
                jPanel1Layout.setHorizontalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(32, 32, 32)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 520, Short.MAX_VALUE)
                                        .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(cbxStudent, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(cbxOffering, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jScrollPane1)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel3)
                                                        .addComponent(jLabel4)
                                                        .addComponent(jLabel5))
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                                .addGap(0, 0, Short.MAX_VALUE)
                                                .addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(8, 8, 8)
                                                .addComponent(btnDropStudent, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(32, 32, 32))
                );
                jPanel1Layout.setVerticalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel2)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cbxStudent, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(10, 10, 10)
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cbxOffering, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(10, 10, 10)
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnDropStudent, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(18, Short.MAX_VALUE))
                );

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
                getContentPane().setLayout(layout);
                layout.setHorizontalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                );

                pack();
        }// </editor-fold>//GEN-END:initComponents

        private void cbxStudentActionPerformed(java.awt.event.ActionEvent evt) {
                loadOfferingsForSelectedStudent();
        }

        private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {
                dispose();
        }

        private void btnDropStudentActionPerformed(java.awt.event.ActionEvent evt) {
                StudentDropCandidate selectedStudent = getSelectedStudentCandidate();
                StudentDropTargetOption selectedOption = getSelectedDropTargetOption();

                if (selectedStudent == null || selectedOption == null) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Please select both student and offering.",
                                "Manual Drop",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                String reason = txtReason.getText() == null ? "" : txtReason.getText().trim();
                int confirm = JOptionPane.showConfirmDialog(
                        this,
                        "Drop student " + selectedStudent.getStudentId()
                                + " from the selected offering?",
                        "Manual Drop",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

                if (confirm != JOptionPane.YES_OPTION) {
                        return;
                }

                boolean dropped = dropRequestService.dropStudentFromOffering(
                        selectedStudent.getStudentId(),
                        selectedOption.getOfferingId(),
                        reason
                );

                if (!dropped) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Failed to drop student from offering. Please verify the selection and try again.",
                                "Manual Drop",
                                JOptionPane.ERROR_MESSAGE
                        );
                        return;
                }

                JOptionPane.showMessageDialog(
                        this,
                        "Student dropped successfully.",
                        "Manual Drop",
                        JOptionPane.INFORMATION_MESSAGE
                );

                if (onDroppedCallback != null) {
                        onDroppedCallback.run();
                }

                dispose();
        }

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton btnCancel;
        private javax.swing.JButton btnDropStudent;
        private javax.swing.JComboBox<String> cbxOffering;
        private javax.swing.JComboBox<String> cbxStudent;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel4;
        private javax.swing.JLabel jLabel5;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JTextArea txtReason;
        // End of variables declaration//GEN-END:variables
}
