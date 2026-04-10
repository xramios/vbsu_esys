/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.group5.paul_esys.screens.registrar.forms;

import com.group5.paul_esys.modules.registrar.services.RegistrarStudentScheduleService;
import com.group5.paul_esys.modules.registrar.model.StudentScheduleRow;
import com.group5.paul_esys.modules.students.model.Student;
import java.awt.Frame;
import java.awt.Window;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 *
 * @author nytri
 */
public class StudentSchedulesForm extends javax.swing.JFrame {
	
	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(StudentSchedulesForm.class.getName());
        private static final DecimalFormat UNITS_FORMAT = new DecimalFormat("0.##");

        private final RegistrarStudentScheduleService registrarStudentScheduleService = RegistrarStudentScheduleService.getInstance();
        private final List<StudentScheduleRow> scheduleRows = new ArrayList<>();
        private final Student student;
        private final String courseName;
        private final String emailAddress;
        private TableRowSorter<DefaultTableModel> scheduleRowSorter;

	/**
	 * Creates new form StudentSchedulesForm
	 */
	public StudentSchedulesForm() {
		this(null, null, null);
	}

        public StudentSchedulesForm(Student student, String courseName, String emailAddress) {
                this.student = student;
                this.courseName = courseName;
                this.emailAddress = emailAddress;
                initComponents();
                initializeForm();
	}

        private void initializeForm() {
                setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
                setLocationRelativeTo(null);

                jButton1.addActionListener(this::jButton1ActionPerformed);
                jButton2.addActionListener(this::jButton2ActionPerformed);
                txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
                        @Override
                        public void keyReleased(java.awt.event.KeyEvent evt) {
                                applyScheduleSearchFilter();
                        }
                });

                configureScheduleTable();
                populateStudentDetails();
                loadStudentSchedules();
        }

        private void configureScheduleTable() {
                DefaultTableModel model = new DefaultTableModel(
                        new Object[][]{},
                        new String[]{"Code", "Course Name", "Instructor", "Schedule", "Room", "Credits"}
                ) {
                        @Override
                        public boolean isCellEditable(int row, int column) {
                                return false;
                        }
                };

                tableSchedules.setModel(model);
                tableSchedules.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
                tableSchedules.setRowHeight(26);
                scheduleRowSorter = new TableRowSorter<>(model);
                tableSchedules.setRowSorter(scheduleRowSorter);
        }

        private void populateStudentDetails() {
                if (student == null) {
                        txtStudentFirstName4.setText("");
                        txtStudentMiddleName4.setText("");
                        txtStudentLastName4.setText("");
                        txtStudentCourse4.setText("");
                        txtStudentYearLevel4.setText("");
                        txtStudentEmailAddress3.setText("");
                        jButton1.setEnabled(false);
                        jButton2.setEnabled(false);
                        return;
                }

                txtStudentFirstName4.setText(student.getFirstName() == null ? "" : student.getFirstName());
                txtStudentMiddleName4.setText(student.getMiddleName() == null ? "" : student.getMiddleName());
                txtStudentLastName4.setText(student.getLastName() == null ? "" : student.getLastName());
                txtStudentCourse4.setText(courseName == null || courseName.isBlank() ? "N/A" : courseName);
                txtStudentYearLevel4.setText(student.getYearLevel() == null ? "" : String.valueOf(student.getYearLevel()));
                txtStudentEmailAddress3.setText(emailAddress == null ? "" : emailAddress);
                setTitle("Student Schedules - " + student.getStudentId());
        }

        private void loadStudentSchedules() {
                DefaultTableModel model = (DefaultTableModel) tableSchedules.getModel();
                model.setRowCount(0);
                scheduleRows.clear();

                if (student == null || student.getStudentId() == null || student.getStudentId().isBlank()) {
                        return;
                }

                List<StudentScheduleRow> loadedRows = registrarStudentScheduleService.getStudentSchedules(student.getStudentId());
                scheduleRows.addAll(loadedRows);

                for (StudentScheduleRow row : loadedRows) {
                        model.addRow(new Object[]{
                                row.subjectCode(),
                                row.subjectName(),
                                row.instructor(),
                                row.sectionCode() + " | " + row.schedule(),
                                row.room(),
                                formatUnits(row.units())
                        });
                }
        }

        private String formatUnits(Float units) {
                if (units == null) {
                        return "0";
                }

                return UNITS_FORMAT.format(units);
        }

        private void applyScheduleSearchFilter() {
                if (scheduleRowSorter == null) {
                        return;
                }

                String keyword = txtSearch.getText() == null ? "" : txtSearch.getText().trim();
                if (keyword.isEmpty()) {
                        scheduleRowSorter.setRowFilter(null);
                        return;
                }

                scheduleRowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(keyword)));
        }

        private StudentScheduleRow getSelectedScheduleRow() {
                int selectedViewRow = tableSchedules.getSelectedRow();
                if (selectedViewRow < 0) {
                        return null;
                }

                int selectedModelRow = tableSchedules.convertRowIndexToModel(selectedViewRow);
                if (selectedModelRow < 0 || selectedModelRow >= scheduleRows.size()) {
                        return null;
                }

                return scheduleRows.get(selectedModelRow);
        }

	/**
	 * This method is called from within the constructor to initialize the
	 * form. WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                jPanel1 = new javax.swing.JPanel();
                jLabel1 = new javax.swing.JLabel();
                jScrollPane2 = new javax.swing.JScrollPane();
                tableSchedules = new javax.swing.JTable();
                txtSearch = new javax.swing.JTextField();
                jPanel6 = new javax.swing.JPanel();
                jLabel34 = new javax.swing.JLabel();
                txtStudentFirstName4 = new javax.swing.JTextField();
                txtStudentMiddleName4 = new javax.swing.JTextField();
                jLabel35 = new javax.swing.JLabel();
                txtStudentLastName4 = new javax.swing.JTextField();
                jLabel36 = new javax.swing.JLabel();
                txtStudentCourse4 = new javax.swing.JTextField();
                jLabel37 = new javax.swing.JLabel();
                txtStudentYearLevel4 = new javax.swing.JTextField();
                jLabel38 = new javax.swing.JLabel();
                txtStudentEmailAddress3 = new javax.swing.JTextField();
                jLabel39 = new javax.swing.JLabel();
                jLabel40 = new javax.swing.JLabel();
                jButton1 = new javax.swing.JButton();
                jButton2 = new javax.swing.JButton();
                jPanel5 = new javax.swing.JPanel();

                setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

                jLabel1.setText("Search");

                tableSchedules.setModel(new javax.swing.table.DefaultTableModel(
                        new Object [][] {
                                {null, null, null, null, null, null},
                                {null, null, null, null, null, null},
                                {null, null, null, null, null, null},
                                {null, null, null, null, null, null}
                        },
                        new String [] {
                                "Code", "Course Name", "Instructor", "Schedule", "Room", "Credits"
                        }
                ));
                jScrollPane2.setViewportView(tableSchedules);

                txtSearch.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                jPanel6.setBackground(new java.awt.Color(255, 255, 255));
                jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder(new com.group5.paul_esys.ui.PanelRoundBorder(), "Student Information"));
                jPanel6.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N

                jLabel34.setText("First Name");

                txtStudentFirstName4.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtStudentFirstName4.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());
                txtStudentFirstName4.setEnabled(false);

                txtStudentMiddleName4.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtStudentMiddleName4.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());
                txtStudentMiddleName4.setEnabled(false);

                jLabel35.setText("Middle Name");

                txtStudentLastName4.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtStudentLastName4.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());
                txtStudentLastName4.setEnabled(false);

                jLabel36.setText("Last Name");

                txtStudentCourse4.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtStudentCourse4.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());
                txtStudentCourse4.setEnabled(false);

                jLabel37.setText("Course");

                txtStudentYearLevel4.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtStudentYearLevel4.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());
                txtStudentYearLevel4.setEnabled(false);

                jLabel38.setText("Year Level");

                txtStudentEmailAddress3.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtStudentEmailAddress3.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());
                txtStudentEmailAddress3.setEnabled(false);

                jLabel39.setText("Student Email Address (Portal Account)");

                jLabel40.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel40.setText("Picture");

                javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
                jPanel6.setLayout(jPanel6Layout);
                jPanel6Layout.setHorizontalGroup(
                        jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel6Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel40, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                                                .addGap(0, 0, Short.MAX_VALUE)
                                                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                                .addGroup(jPanel6Layout.createSequentialGroup()
                                                                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(txtStudentFirstName4, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addComponent(jLabel34, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(jLabel35, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addGroup(jPanel6Layout.createSequentialGroup()
                                                                                        .addComponent(txtStudentMiddleName4)
                                                                                        .addContainerGap())))
                                                                .addComponent(jLabel36, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addComponent(jLabel37, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                .addComponent(jLabel38, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                .addComponent(jLabel39, javax.swing.GroupLayout.PREFERRED_SIZE, 298, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                                                                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                        .addComponent(txtStudentEmailAddress3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 292, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                        .addComponent(txtStudentYearLevel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 292, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                        .addComponent(txtStudentCourse4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 292, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                        .addComponent(txtStudentLastName4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 292, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                .addContainerGap())))))
                );
                jPanel6Layout.setVerticalGroup(
                        jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel40, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel34)
                                        .addComponent(jLabel35))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(txtStudentFirstName4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(txtStudentMiddleName4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel36)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtStudentLastName4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel37)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtStudentCourse4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel38)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtStudentYearLevel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel39)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtStudentEmailAddress3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
                );

                jButton1.setText("Change Section");

                jButton2.setText("Add Subject");

                jPanel5.setBackground(new java.awt.Color(255, 255, 255));
                jPanel5.setBorder(new com.group5.paul_esys.ui.PanelRoundBorder());

                javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
                jPanel5.setLayout(jPanel5Layout);
                jPanel5Layout.setHorizontalGroup(
                        jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
                );
                jPanel5Layout.setVerticalGroup(
                        jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 467, Short.MAX_VALUE)
                );

                javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
                jPanel1.setLayout(jPanel1Layout);
                jPanel1Layout.setHorizontalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, 316, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(jLabel1)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 400, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 218, Short.MAX_VALUE)
                                                .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 139, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addComponent(jScrollPane2))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, 139, Short.MAX_VALUE)
                                        .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
                );
                jPanel1Layout.setVerticalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(24, 24, 24)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel1)
                                        .addComponent(jButton1)
                                        .addComponent(jButton2))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 473, javax.swing.GroupLayout.PREFERRED_SIZE)))
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

        private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
                if (student == null) {
                        return;
                }

                StudentScheduleRow selectedScheduleRow = getSelectedScheduleRow();
                if (selectedScheduleRow == null) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Please select exactly one subject schedule to change its section.",
                                "Change Section",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                Window window = SwingUtilities.getWindowAncestor(this);
                Frame parentFrame = window instanceof Frame ? (Frame) window : this;

                ChangeStudentSectionForm form = new ChangeStudentSectionForm(
                        parentFrame,
                        true,
                        student.getStudentId(),
                        selectedScheduleRow,
                        this::loadStudentSchedules
                );
                form.setVisible(true);
        }

        private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {
                JOptionPane.showMessageDialog(
                        this,
                        "Add Subject flow is handled separately.",
                        "Information",
                        JOptionPane.INFORMATION_MESSAGE
                );
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
		java.awt.EventQueue.invokeLater(() -> new StudentSchedulesForm().setVisible(true));
	}

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton jButton1;
        private javax.swing.JButton jButton2;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel34;
        private javax.swing.JLabel jLabel35;
        private javax.swing.JLabel jLabel36;
        private javax.swing.JLabel jLabel37;
        private javax.swing.JLabel jLabel38;
        private javax.swing.JLabel jLabel39;
        private javax.swing.JLabel jLabel40;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JPanel jPanel5;
        private javax.swing.JPanel jPanel6;
        private javax.swing.JScrollPane jScrollPane2;
        private javax.swing.JTable tableSchedules;
        private javax.swing.JTextField txtSearch;
        private javax.swing.JTextField txtStudentCourse4;
        private javax.swing.JTextField txtStudentEmailAddress3;
        private javax.swing.JTextField txtStudentFirstName4;
        private javax.swing.JTextField txtStudentLastName4;
        private javax.swing.JTextField txtStudentMiddleName4;
        private javax.swing.JTextField txtStudentYearLevel4;
        // End of variables declaration//GEN-END:variables
}
