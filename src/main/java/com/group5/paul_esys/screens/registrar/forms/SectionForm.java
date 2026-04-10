/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.group5.paul_esys.screens.registrar.forms;

import com.group5.paul_esys.modules.sections.model.Section;
import com.group5.paul_esys.modules.sections.services.SectionService;
import java.util.List;
import javax.swing.JOptionPane;

/**
 *
 * @author nytri
 */
public class SectionForm extends javax.swing.JFrame {
	
	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(SectionForm.class.getName());
        private static final String STATUS_OPEN = "OPEN";
        private static final List<String> STATUS_OPTIONS = List.of(
                STATUS_OPEN,
                "CLOSED",
                "WAITLIST",
                "DISSOLVED"
        );

        private final SectionService sectionService = SectionService.getInstance();
        private final Section editingSection;
        private final Runnable onSavedCallback;

	/**
	 * Creates new form SectionForm
	 */
	public SectionForm() {
                this(null, null);
	}

        public SectionForm(Section editingSection, Runnable onSavedCallback) {
                this.editingSection = editingSection;
                this.onSavedCallback = onSavedCallback;
                this.setUndecorated(true);
                initComponents();
                this.setLocationRelativeTo(null);
                initializeForm();
        }

        private void initializeForm() {
                spinnerCapaity.setModel(new javax.swing.SpinnerNumberModel(40, 1, 500, 1));
                cbxStatus.setModel(new javax.swing.DefaultComboBoxModel<>(STATUS_OPTIONS.toArray(new String[0])));
                cbxStatus.setSelectedItem(STATUS_OPEN);

                if (editingSection == null) {
                        windowBar1.setTitle("Section Form");
                        jLabel1.setText("Section Form");
                        jLabel2.setText("Add section");
                        btnSave.setText("Save");
                        return;
                }

                windowBar1.setTitle("Update Section");
                jLabel1.setText("Update Section");
                jLabel2.setText("Update existing section");
                btnSave.setText("Update");

                txtSectionCode.setText(readSafeText(editingSection.getSectionCode()));
                txtSectionName.setText(readSafeText(editingSection.getSectionName()));
                spinnerCapaity.setValue(Math.max(1, normalizeCapacity(editingSection.getCapacity())));
                cbxStatus.setSelectedItem(normalizeStatus(editingSection.getStatus()));
        }

        private String readSafeText(String value) {
                if (value == null) {
                        return "";
                }

                return value.trim();
        }

        private int normalizeCapacity(Integer capacity) {
                return capacity == null ? 0 : capacity;
        }

        private String normalizeStatus(String status) {
                if (status == null || status.trim().isEmpty()) {
                        return STATUS_OPEN;
                }

                String normalized = status.trim().toUpperCase();
                return STATUS_OPTIONS.contains(normalized) ? normalized : STATUS_OPEN;
        }

        private boolean isValidForm() {
                String sectionCode = readSafeText(txtSectionCode.getText());
                if (sectionCode.isEmpty()) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Section code is required.",
                                "Validation Error",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return false;
                }

                String sectionName = readSafeText(txtSectionName.getText());
                if (sectionName.isEmpty()) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Section name is required.",
                                "Validation Error",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return false;
                }

                int capacity = ((Number) spinnerCapaity.getValue()).intValue();
                if (capacity <= 0) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Capacity must be greater than zero.",
                                "Validation Error",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return false;
                }

                if (!isSectionCodeAvailable(sectionCode)) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Section code already exists. Please use a different code.",
                                "Validation Error",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return false;
                }

                return true;
        }

        private boolean isSectionCodeAvailable(String sectionCode) {
                Long editingId = editingSection == null ? null : editingSection.getId();
                String normalizedCode = sectionCode.trim();

                for (Section section : sectionService.getAllSections()) {
                        if (editingId != null && editingId.equals(section.getId())) {
                                continue;
                        }

                        String existingCode = readSafeText(section.getSectionCode());
                        if (existingCode.equalsIgnoreCase(normalizedCode)) {
                                return false;
                        }
                }

                return true;
        }

        private void saveSection() {
                if (!isValidForm()) {
                        return;
                }

                Section section = editingSection == null ? new Section() : editingSection;
                section
                        .setSectionCode(readSafeText(txtSectionCode.getText()))
                        .setSectionName(readSafeText(txtSectionName.getText()))
                        .setCapacity(((Number) spinnerCapaity.getValue()).intValue())
                        .setStatus(normalizeStatus(cbxStatus.getSelectedItem() == null ? STATUS_OPEN : cbxStatus.getSelectedItem().toString()));

                boolean success = editingSection == null
                        ? sectionService.createSection(section)
                        : sectionService.updateSection(section);

                if (!success) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Unable to save section. Please verify values and try again.",
                                editingSection == null ? "Create Section" : "Update Section",
                                JOptionPane.ERROR_MESSAGE
                        );
                        return;
                }

                JOptionPane.showMessageDialog(
                        this,
                        editingSection == null ? "Section created successfully." : "Section updated successfully.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                );

                if (onSavedCallback != null) {
                        onSavedCallback.run();
                }

                dispose();
        }

	/**
	 * This method is called from within the constructor to initialize the
	 * form. WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                windowBar1 = new com.group5.paul_esys.components.WindowBar();
                jPanel1 = new javax.swing.JPanel();
                jLabel1 = new javax.swing.JLabel();
                jLabel2 = new javax.swing.JLabel();
                jLabel3 = new javax.swing.JLabel();
                txtSectionName = new javax.swing.JTextField();
                txtSectionCode = new javax.swing.JTextField();
                jLabel4 = new javax.swing.JLabel();
                jLabel5 = new javax.swing.JLabel();
                spinnerCapaity = new javax.swing.JSpinner();
                jLabel6 = new javax.swing.JLabel();
                cbxStatus = new javax.swing.JComboBox<>();
                btnSave = new javax.swing.JButton();
                btnCancel = new javax.swing.JButton();

                setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
                getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.Y_AXIS));

                windowBar1.setTitle("Section Form");
                getContentPane().add(windowBar1);

                jPanel1.setBackground(new java.awt.Color(255, 255, 255));

                jLabel1.setFont(new java.awt.Font("Poppins", 0, 18)); // NOI18N
                jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel1.setText("Section Form");

                jLabel2.setForeground(new java.awt.Color(153, 153, 153));
                jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel2.setText("Add/Update Section");

                jLabel3.setText("Section Name");

                jLabel4.setText("Section Code");

                jLabel5.setText("Capacity");

                spinnerCapaity.setModel(new javax.swing.SpinnerNumberModel(40, 1, 500, 1));

                jLabel6.setText("Status");

                cbxStatus.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "OPEN", "CLOSED", "WAITLIST", "DISSOLVED" }));

                btnSave.setBackground(new java.awt.Color(119, 0, 0));
                btnSave.setForeground(new java.awt.Color(255, 255, 255));
                btnSave.setText("Save");
                btnSave.addActionListener(this::btnSaveActionPerformed);

                btnCancel.setBackground(new java.awt.Color(119, 0, 0));
                btnCancel.setForeground(new java.awt.Color(255, 255, 255));
                btnCancel.setText("Cancel");
                btnCancel.addActionListener(this::btnCancelActionPerformed);

                javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
                jPanel1.setLayout(jPanel1Layout);
                jPanel1Layout.setHorizontalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(45, 45, 45)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(txtSectionCode, javax.swing.GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE)
                                        .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(txtSectionName)
                                        .addComponent(spinnerCapaity)
                                        .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(cbxStatus, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addContainerGap(97, Short.MAX_VALUE)
                                .addComponent(btnCancel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(btnSave)
                                .addGap(97, 97, 97))
                );
                jPanel1Layout.setVerticalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(42, 42, 42)
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel2)
                                .addGap(35, 35, 35)
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtSectionName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtSectionCode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerCapaity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel6)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cbxStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(34, 34, 34)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(btnSave)
                                        .addComponent(btnCancel))
                                .addContainerGap(43, Short.MAX_VALUE))
                );

                getContentPane().add(jPanel1);

                pack();
        }// </editor-fold>//GEN-END:initComponents

        private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
                saveSection();
        }//GEN-LAST:event_btnSaveActionPerformed

        private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
                dispose();
        }//GEN-LAST:event_btnCancelActionPerformed

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		/* Set the Nimbus look and feel */
		//<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
		/* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
		 */
		try {
			for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
			logger.log(java.util.logging.Level.SEVERE, null, ex);
		}
		//</editor-fold>

		/* Create and display the form */
		java.awt.EventQueue.invokeLater(() -> new SectionForm().setVisible(true));
	}

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton btnCancel;
        private javax.swing.JButton btnSave;
        private javax.swing.JComboBox<String> cbxStatus;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel4;
        private javax.swing.JLabel jLabel5;
        private javax.swing.JLabel jLabel6;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JSpinner spinnerCapaity;
        private javax.swing.JTextField txtSectionCode;
        private javax.swing.JTextField txtSectionName;
        private com.group5.paul_esys.components.WindowBar windowBar1;
        // End of variables declaration//GEN-END:variables
}
