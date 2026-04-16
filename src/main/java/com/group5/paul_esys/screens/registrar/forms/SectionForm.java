/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.group5.paul_esys.screens.registrar.forms;

import com.group5.paul_esys.modules.sections.model.Section;
import com.group5.paul_esys.modules.sections.services.SectionService;
import com.group5.paul_esys.utils.FormValidationUtil;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;

/**
 *
 * @author nytri
 */
public class SectionForm extends javax.swing.JFrame {
	
	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(SectionForm.class.getName());
        private static final int MIN_SECTION_CODE_LENGTH = 2;
        private static final int MAX_SECTION_CODE_LENGTH = 20;
        private static final Pattern SECTION_CODE_PATTERN = Pattern.compile("^[A-Za-z0-9._\\-/]+$");

        private static final String STATUS_OPEN = "OPEN";
        private static final List<String> STATUS_OPTIONS = List.of(
                "OPEN",
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

                jLabel1.setText("Section Management");
                jLabel4.setText("Section Code (Identifier)");

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
                if (showValidationError(
                        FormValidationUtil.validateRequiredText(
                                "Section code",
                                sectionCode,
                                MIN_SECTION_CODE_LENGTH,
                                MAX_SECTION_CODE_LENGTH,
                                SECTION_CODE_PATTERN,
                                "letters, numbers, and . _ - /"
                        )
                )) {
                        return false;
                }

                if (showValidationError(
                        FormValidationUtil.validateNumberRange(
                                "Capacity",
                                (Number) spinnerCapaity.getValue(),
                                1,
                                FormValidationUtil.LARGE_NUMBER_LIMIT
                        )
                )) {
                        return false;
                }

                if (showValidationError(FormValidationUtil.validateRequiredSelection("Status", cbxStatus.getSelectedItem()))) {
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
                        logger.warning("Form validation failed. Section not saved.");
                        return;
                }

                Section section = editingSection == null ? new Section() : editingSection;
                String unifiedCode = FormValidationUtil.normalizeOptionalText(txtSectionCode.getText());
                section
                        .setSectionCode(unifiedCode)
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

        private boolean showValidationError(Optional<String> validationError) {
                if (validationError.isEmpty()) {
                        return false;
                }

                JOptionPane.showMessageDialog(
                        this,
                        validationError.get(),
                        "Validation Error",
                        JOptionPane.WARNING_MESSAGE
                );
                return true;
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

                setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

                windowBar1.setName(""); // NOI18N
                windowBar1.setPreferredSize(new java.awt.Dimension(354, 36));
                windowBar1.setTitle("Section Form");

                jPanel1.setBackground(new java.awt.Color(255, 255, 255));
                jPanel1.setAlignmentX(0.5f);
                jPanel1.setAlignmentY(0.5f);
                jPanel1.setPreferredSize(new java.awt.Dimension(380, 400));
                jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

                jLabel1.setFont(new java.awt.Font("Poppins", 0, 18)); // NOI18N
                jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel1.setText("Section Form");
                jPanel1.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 30, 260, -1));

                jLabel2.setForeground(new java.awt.Color(153, 153, 153));
                jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel2.setText("Add/Update Section");
                jPanel1.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 60, 260, -1));

                jLabel3.setText("Section Name");
                jPanel1.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 100, 260, -1));
                jPanel1.add(txtSectionName, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 120, 260, -1));
                jPanel1.add(txtSectionCode, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 170, 260, -1));

                jLabel4.setText("Section Code");
                jPanel1.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 150, 260, -1));

                jLabel5.setText("Capacity");
                jPanel1.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 200, 260, -1));

                spinnerCapaity.setModel(new javax.swing.SpinnerNumberModel(0, 0, 1000, 1));
                jPanel1.add(spinnerCapaity, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 230, 260, -1));

                jLabel6.setText("Status");
                jPanel1.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 260, 260, -1));

                cbxStatus.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
                jPanel1.add(cbxStatus, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 280, 260, -1));

                btnSave.setBackground(new java.awt.Color(119, 0, 0));
                btnSave.setForeground(new java.awt.Color(255, 255, 255));
                btnSave.setText("Save");
                btnSave.addActionListener(this::btnSaveActionPerformed);
                jPanel1.add(btnSave, new org.netbeans.lib.awtextra.AbsoluteConstraints(200, 330, -1, -1));

                btnCancel.setBackground(new java.awt.Color(119, 0, 0));
                btnCancel.setForeground(new java.awt.Color(255, 255, 255));
                btnCancel.setText("Cancel");
                btnCancel.addActionListener(this::btnCancelActionPerformed);
                jPanel1.add(btnCancel, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 330, -1, -1));

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
                getContentPane().setLayout(layout);
                layout.setHorizontalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(windowBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 380, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(windowBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 383, javax.swing.GroupLayout.PREFERRED_SIZE))
                );

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
