/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.group5.paul_esys.screens.registrar.forms;

import com.group5.paul_esys.modules.courses.model.Course;
import com.group5.paul_esys.modules.courses.services.CourseService;
import com.group5.paul_esys.modules.curriculum.model.Curriculum;
import com.group5.paul_esys.modules.curriculum.services.CurriculumService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JOptionPane;

/**
 *
 * @author nytri
 */
public class CurriculumForm extends javax.swing.JFrame {
	
	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(CurriculumForm.class.getName());
        private final CourseService courseService = CourseService.getInstance();
        private final CurriculumService curriculumService = CurriculumService.getInstance();
        private final Map<String, Long> courseIdByName = new LinkedHashMap<>();
        private final Runnable onSavedCallback;
        private final Curriculum editingCurriculum;

	/**
	 * Creates new form Curriculum
	 */
	public CurriculumForm() {
                this(null, null);
        }

        public CurriculumForm(Curriculum editingCurriculum, Runnable onSavedCallback) {
                this.editingCurriculum = editingCurriculum;
                this.onSavedCallback = onSavedCallback;
                this.setUndecorated(true);
		initComponents();
                this.setLocationRelativeTo(null);
                btnSave.addActionListener(this::btnSaveActionPerformed);
                initializeForm();
        }

        private void initializeForm() {
                int currentYear = LocalDate.now().getYear();
                spinnerYear.setModel(new javax.swing.SpinnerNumberModel(currentYear, 2000, 2100, 1));
                loadCourses();

                if (editingCurriculum == null) {
                        return;
                }

                jLabel3.setText("Update Curriculum");
                btnSave.setText("Update");

                if (editingCurriculum.getCurYear() != null) {
                        LocalDate localDate = editingCurriculum
                                .getCurYear()
                                .toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();
                        spinnerYear.setValue(localDate.getYear());
                }

                courseService
                        .getCourseById(editingCurriculum.getCourse())
                        .ifPresent(course -> cbxCourse.setSelectedItem(course.getCourseName()));
        }

        private void loadCourses() {
                cbxCourse.removeAllItems();
                courseIdByName.clear();

                for (Course course : courseService.getAllCourses()) {
                        cbxCourse.addItem(course.getCourseName());
                        courseIdByName.put(course.getCourseName(), course.getId());
                }
        }

        private String buildCurriculumName(String courseName, int year) {
                String cleaned = courseName.replaceAll("[^A-Za-z0-9 ]", " ").trim();
                if (cleaned.isEmpty()) {
                        return "CUR" + year;
                }

                String[] parts = cleaned.split("\\s+");
                StringBuilder code = new StringBuilder();
                for (String part : parts) {
                        if (!part.isEmpty() && code.length() < 6) {
                                code.append(Character.toUpperCase(part.charAt(0)));
                        }
                }

                if (code.length() < 2) {
                        String compact = cleaned.replaceAll("\\s+", "").toUpperCase();
                        code = new StringBuilder(compact.substring(0, Math.min(3, compact.length())));
                }

                return code + String.valueOf(year);
        }

        private void saveCurriculum() {
                Object selectedCourse = cbxCourse.getSelectedItem();
                if (selectedCourse == null || !courseIdByName.containsKey(selectedCourse.toString())) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Please select a course.",
                                "Validation Error",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                int selectedYear = (Integer) spinnerYear.getValue();
                Long selectedCourseId = courseIdByName.get(selectedCourse.toString());
                Date curriculumYear = java.sql.Date.valueOf(LocalDate.of(selectedYear, 1, 1));

                Curriculum curriculum = editingCurriculum == null ? new Curriculum() : editingCurriculum;
                curriculum
                        .setName(buildCurriculumName(selectedCourse.toString(), selectedYear))
                        .setCurYear(curriculumYear)
                        .setCourse(selectedCourseId);

                boolean success = editingCurriculum == null
                        ? curriculumService.createCurriculum(curriculum)
                        : curriculumService.updateCurriculum(curriculum);

                if (!success) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Failed to save curriculum. Please try again.",
                                "Save Failed",
                                JOptionPane.ERROR_MESSAGE
                        );
                        return;
                }

                JOptionPane.showMessageDialog(
                        this,
                        editingCurriculum == null
                                ? "Curriculum created successfully."
                                : "Curriculum updated successfully.",
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
	@SuppressWarnings("unchecked")
        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                windowBar1 = new com.group5.paul_esys.components.WindowBar();
                jPanel1 = new javax.swing.JPanel();
                jLabel1 = new javax.swing.JLabel();
                spinnerYear = new javax.swing.JSpinner();
                jLabel2 = new javax.swing.JLabel();
                cbxCourse = new javax.swing.JComboBox<>();
                btnSave = new javax.swing.JButton();
                btnCancel = new javax.swing.JButton();
                jLabel3 = new javax.swing.JLabel();

                setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
                setBackground(new java.awt.Color(255, 255, 255));
                getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.Y_AXIS));

                windowBar1.setTitle("Curriculum Form");
                getContentPane().add(windowBar1);

                jPanel1.setBackground(new java.awt.Color(255, 255, 255));

                jLabel1.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
                jLabel1.setText("Year");

                spinnerYear.setModel(new javax.swing.SpinnerNumberModel());
                spinnerYear.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                jLabel2.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
                jLabel2.setText("Course");

                cbxCourse.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

                btnSave.setBackground(new java.awt.Color(255, 234, 234));
                btnSave.setForeground(new java.awt.Color(0, 0, 0));
                btnSave.setText("Save");

                btnCancel.setText("Cancel");
                btnCancel.addActionListener(this::btnCancelActionPerformed);

                jLabel3.setFont(new java.awt.Font("Poppins", 0, 18)); // NOI18N
                jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel3.setText("Create new Curriculum");

                javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
                jPanel1.setLayout(jPanel1Layout);
                jPanel1Layout.setHorizontalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(63, 63, 63)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(btnCancel)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnSave))
                                        .addComponent(spinnerYear, javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(cbxCourse, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE))
                                .addContainerGap(67, Short.MAX_VALUE))
                );
                jPanel1Layout.setVerticalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addContainerGap(18, Short.MAX_VALUE)
                                .addComponent(jLabel3)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerYear, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cbxCourse, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18))
                );

                getContentPane().add(jPanel1);

                pack();
        }// </editor-fold>//GEN-END:initComponents

        private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
		this.dispose();
        }//GEN-LAST:event_btnCancelActionPerformed

        private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {
                saveCurriculum();
        }

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
		java.awt.EventQueue.invokeLater(() -> new CurriculumForm().setVisible(true));
	}

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton btnCancel;
        private javax.swing.JButton btnSave;
        private javax.swing.JComboBox<String> cbxCourse;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JSpinner spinnerYear;
        private com.group5.paul_esys.components.WindowBar windowBar1;
        // End of variables declaration//GEN-END:variables
}
