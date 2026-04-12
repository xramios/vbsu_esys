package com.group5.paul_esys.screens.registrar.forms;

import com.group5.paul_esys.modules.curriculum.model.Curriculum;
import com.group5.paul_esys.modules.curriculum.services.CurriculumService;
import com.group5.paul_esys.modules.semester.model.Semester;
import com.group5.paul_esys.modules.semester.services.SemesterService;
import java.awt.Component;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

/**
 *
 * @author janea
 */
public class SemesterForm extends javax.swing.JFrame {
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(SemesterForm.class.getName());
    private final CurriculumService curriculumService = CurriculumService.getInstance();
    private final SemesterService semesterService = SemesterService.getInstance();
    private final Map<String, Long> curriculumIdByLabel = new LinkedHashMap<>();
    private final Runnable onSavedCallback;
    private final Semester editingSemester;

    /**
     * Creates new form SemForm
     */
    public SemesterForm() {
        this(null, null);
    }

    public SemesterForm(Semester editingSemester, Runnable onSavedCallback) {
        this.editingSemester = editingSemester;
        this.onSavedCallback = onSavedCallback;
        this.setUndecorated(true);
        initComponents();
        this.setLocationRelativeTo(null);
        initializeForm();
    }

    private void initializeForm() {
        loadYearLevels();
        loadCurriculums();

        if (editingSemester == null) {
            return;
        }

        jLabel1.setText("Update Semester");
        jLabel4.setText("Update existing semester");
        btnSave.setText("Update");

        txtSem.setText(editingSemester.getSemester());
        if (editingSemester.getYearLevel() != null) {
            cbxYear.setSelectedItem(String.valueOf(editingSemester.getYearLevel()));
        }

        curriculumService
            .getCurriculumById(editingSemester.getCurriculumId())
            .ifPresent(curriculum -> cbxCur.setSelectedItem(buildCurriculumLabel(curriculum)));
    }

    private void loadYearLevels() {
        cbxYear.removeAllItems();

        for (int yearLevel = 1; yearLevel <= 6; yearLevel++) {
            cbxYear.addItem(String.valueOf(yearLevel));
        }

        cbxYear.setEditable(true);
        restrictYearLevelEditorToIntegers();

        if (editingSemester != null && editingSemester.getYearLevel() != null) {
            cbxYear.setSelectedItem(String.valueOf(editingSemester.getYearLevel()));
        } else if (cbxYear.getItemCount() > 0) {
            cbxYear.setSelectedIndex(0);
        }
    }

    private void restrictYearLevelEditorToIntegers() {
        Component editorComponent = cbxYear.getEditor().getEditorComponent();
        if (!(editorComponent instanceof JTextField textField)) {
            return;
        }

        AbstractDocument document = (AbstractDocument) textField.getDocument();
        document.setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
                if (isIntegerInput(string)) {
                    super.insertString(fb, offset, string, attr);
                }
            }

            @Override
            public void replace(
                FilterBypass fb,
                int offset,
                int length,
                String text,
                AttributeSet attrs
            ) throws BadLocationException {
                if (isIntegerInput(text)) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }

            private boolean isIntegerInput(String text) {
                return text == null || text.isEmpty() || text.chars().allMatch(Character::isDigit);
            }
        });
    }

    private String buildCurriculumLabel(Curriculum curriculum) {
        String curriculumName = curriculum.getName() == null ? "Curriculum" : curriculum.getName();
        if (curriculum.getCurYear() == null) {
            return curriculumName;
        }

        int year = formatYear(curriculum.getCurYear());

        return curriculumName + " (" + year + ")";
    }

    private int formatYear(Date date) {
        if (date instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate().getYear();
        }

        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().getYear();
    }

    private void loadCurriculums() {
        cbxCur.removeAllItems();
        curriculumIdByLabel.clear();

        for (Curriculum curriculum : curriculumService.getAllCurriculums()) {
            String label = buildCurriculumLabel(curriculum);
            cbxCur.addItem(label);
            curriculumIdByLabel.put(label, curriculum.getId());
        }
    }

    private boolean isValidForm() {
        if (txtSem.getText() == null || txtSem.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Semester name is required.",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE
            );
            return false;
        }

        String semesterName = txtSem.getText().trim();
        Integer yearLevel = readYearLevel();
        if (yearLevel == null || yearLevel < 1) {
            JOptionPane.showMessageDialog(
                this,
                "Year level must be a positive integer.",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE
            );
            return false;
        }

        if (hasDuplicateSemester(semesterName, yearLevel)) {
            JOptionPane.showMessageDialog(
                this,
                "A semester with the same year level and semester name already exists.",
                "Duplicate Entry",
                JOptionPane.WARNING_MESSAGE
            );
            return false;
        }

        Object selectedCurriculum = cbxCur.getSelectedItem();
        if (selectedCurriculum == null || !curriculumIdByLabel.containsKey(selectedCurriculum.toString())) {
            JOptionPane.showMessageDialog(
                this,
                "Curriculum is required.",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE
            );
            return false;
        }

        return true;
    }

    private boolean hasDuplicateSemester(String semesterName, Integer yearLevel) {
        Object selectedCurriculum = cbxCur.getSelectedItem();
        if (selectedCurriculum == null) {
            return false;
        }

        Long curriculumId = curriculumIdByLabel.get(selectedCurriculum.toString());
        if (curriculumId == null) {
            return false;
        }

        Semester semester = editingSemester == null ? new Semester() : editingSemester;
        semester
            .setCurriculumId(curriculumId)
            .setSemester(semesterName)
            .setYearLevel(yearLevel);

        return semesterService.semesterExists(semester);
    }

    private Integer readYearLevel() {
        Object selectedYearLevel = cbxYear.getEditor().getItem();
        if (selectedYearLevel == null) {
            return null;
        }

        String yearLevelText = selectedYearLevel.toString().trim();
        if (yearLevelText.isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(yearLevelText);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void saveSemester() {
        if (!isValidForm()) {
            return;
        }

        String selectedCurriculum = cbxCur.getSelectedItem().toString();
        Long curriculumId = curriculumIdByLabel.get(selectedCurriculum);
        Integer yearLevel = readYearLevel();
        String semesterName = txtSem.getText().trim();

        Semester semester = editingSemester == null ? new Semester() : editingSemester;
        semester
            .setSemester(semesterName)
            .setCurriculumId(curriculumId);
        semester.setYearLevel(yearLevel);

        boolean success = editingSemester == null
            ? semesterService.createSemester(semester)
            : semesterService.updateSemester(semester);

        if (!success) {
            JOptionPane.showMessageDialog(
                this,
                "Failed to save semester. Please try again.",
                "Save Failed",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        JOptionPane.showMessageDialog(
            this,
            editingSemester == null
                ? "Semester created successfully."
                : "Semester updated successfully.",
            "Success",
            JOptionPane.INFORMATION_MESSAGE
        );

        if (onSavedCallback != null) {
            onSavedCallback.run();
        }

        dispose();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                windowBar1 = new com.group5.paul_esys.components.WindowBar();
                jPanel1 = new javax.swing.JPanel();
                txtSem = new javax.swing.JTextField();
                jLabel1 = new javax.swing.JLabel();
                jLabel2 = new javax.swing.JLabel();
                jLabel3 = new javax.swing.JLabel();
                cbxCur = new javax.swing.JComboBox<>();
                btnSave = new javax.swing.JButton();
                btnCancel = new javax.swing.JButton();
                jLabel4 = new javax.swing.JLabel();
                jLabel5 = new javax.swing.JLabel();
                cbxYear = new javax.swing.JComboBox<>();

                setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

                windowBar1.setTitle("Semester");

                jPanel1.setBackground(new java.awt.Color(255, 255, 255));
                jPanel1.setAlignmentX(0.1F);
                jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

                txtSem.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtSem.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());
                txtSem.addActionListener(this::txtSemActionPerformed);
                jPanel1.add(txtSem, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 110, 250, -1));

                jLabel1.setFont(new java.awt.Font("Poppins", 0, 18)); // NOI18N
                jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel1.setText("Semester Form");
                jPanel1.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 20, 250, 20));

                jLabel2.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel2.setText("Year");
                jPanel1.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 160, 250, -1));

                jLabel3.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel3.setText("Curriculum");
                jPanel1.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 240, 250, -1));

                cbxCur.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jPanel1.add(cbxCur, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 270, 250, -1));

                btnSave.setBackground(new java.awt.Color(255, 234, 234));
                btnSave.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                btnSave.setText("Save");
                btnSave.addActionListener(this::btnSaveActionPerformed);
                jPanel1.add(btnSave, new org.netbeans.lib.awtextra.AbsoluteConstraints(200, 320, 100, -1));

                btnCancel.setBackground(new java.awt.Color(255, 234, 234));
                btnCancel.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                btnCancel.setText("Cancel");
                btnCancel.addActionListener(this::btnCancelActionPerformed);
                jPanel1.add(btnCancel, new org.netbeans.lib.awtextra.AbsoluteConstraints(90, 320, 100, -1));

                jLabel4.setForeground(new java.awt.Color(153, 153, 153));
                jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel4.setText("Start a new Semester");
                jPanel1.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 50, 250, -1));

                jLabel5.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel5.setText("Semester");
                jPanel1.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 80, 250, -1));

                cbxYear.setEditable(true);
                cbxYear.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
                jPanel1.add(cbxYear, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 190, 250, 36));

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
                getContentPane().setLayout(layout);
                layout.setHorizontalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(windowBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 387, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 387, javax.swing.GroupLayout.PREFERRED_SIZE)
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(windowBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 386, Short.MAX_VALUE))
                );

                pack();
        }// </editor-fold>//GEN-END:initComponents

    private void txtSemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtSemActionPerformed
        // no-op
    }//GEN-LAST:event_txtSemActionPerformed

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        saveSemester();
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
        java.awt.EventQueue.invokeLater(() -> new SemesterForm().setVisible(true));
    }

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton btnCancel;
        private javax.swing.JButton btnSave;
        private javax.swing.JComboBox<String> cbxCur;
        private javax.swing.JComboBox<String> cbxYear;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel4;
        private javax.swing.JLabel jLabel5;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JTextField txtSem;
        private com.group5.paul_esys.components.WindowBar windowBar1;
        // End of variables declaration//GEN-END:variables
}
