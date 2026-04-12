/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JDialog.java to edit this template
 */
package com.group5.paul_esys.screens.registrar.forms;

import com.group5.paul_esys.modules.departments.model.Department;
import com.group5.paul_esys.modules.departments.services.DepartmentService;
import com.group5.paul_esys.modules.subjects.model.Subject;
import com.group5.paul_esys.modules.subjects.services.SubjectService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import javax.swing.JOptionPane;

/**
 *
 * @author nytri
 */
public class SubjectForm extends javax.swing.JDialog {
	
	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(SubjectForm.class.getName());
        private final SubjectService subjectService = SubjectService.getInstance();
        private final DepartmentService departmentService = DepartmentService.getInstance();

        private final Map<String, Long> departmentIdByLabel = new LinkedHashMap<>();
        private final Map<Long, String> departmentLabelById = new LinkedHashMap<>();

        private final Subject editingSubject;
        private final Runnable onSavedCallback;

	/**
	 * Creates new form SubjectForm
	 */
	public SubjectForm(java.awt.Frame parent, boolean modal) {
		this(parent, modal, null, null);
	}

        public SubjectForm(java.awt.Frame parent, boolean modal, Subject editingSubject, Runnable onSavedCallback) {
		super(parent, modal);
		this.editingSubject = editingSubject;
		this.onSavedCallback = onSavedCallback;
		this.setUndecorated(true);
		initComponents();
		this.setLocationRelativeTo(null);
		initializeForm();
	}

        private void initializeForm() {
                btnSave.addActionListener(this::jButton1ActionPerformed);
                btnCancel.addActionListener(this::jButton2ActionPerformed);

                jLabel4.setText("Subject Code");
                spinnerUnit.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(1.0f), Float.valueOf(0.5f), Float.valueOf(25.0f), Float.valueOf(0.5f)));

                loadDepartments();

                if (editingSubject == null) {
                        return;
                }

                jLabel1.setText("Update Subject");
                jLabel2.setText("Update existing subject");
                btnSave.setText("Update");

                txtSubjectName.setText(editingSubject.getSubjectName());
                txtSubjectCode.setText(editingSubject.getSubjectCode());
                textAreaDescription.setText(editingSubject.getDescription() == null ? "" : editingSubject.getDescription());

                if (editingSubject.getUnits() != null) {
                        spinnerUnit.setValue(editingSubject.getUnits());
                }


                String departmentLabel = departmentLabelById.get(editingSubject.getDepartmentId());
                if (departmentLabel != null) {
                        cbxDepartment.setSelectedItem(departmentLabel);
                }
        }

        private void loadDepartments() {
                cbxDepartment.removeAllItems();
                departmentIdByLabel.clear();
                departmentLabelById.clear();

                for (Department department : departmentService.getAllDepartments()) {
                        String label = buildDepartmentLabel(department);
                        cbxDepartment.addItem(label);
                        departmentIdByLabel.put(label, department.getId());
                        departmentLabelById.put(department.getId(), label);
                }
	}

        private String buildDepartmentLabel(Department department) {
                String name = department.getDepartmentName() == null || department.getDepartmentName().trim().isEmpty()
                        ? "Department"
                        : department.getDepartmentName().trim();
                return name + " - ID " + department.getId();
	}

        private boolean isValidForm() {
                if (txtSubjectName.getText() == null || txtSubjectName.getText().trim().isEmpty()) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Subject name is required.",
                                "Validation Error",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return false;
                }

                if (txtSubjectCode.getText() == null || txtSubjectCode.getText().trim().isEmpty()) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Subject code is required.",
                                "Validation Error",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return false;
                }

                if (!hasValidUnits()) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Units must be greater than zero.",
                                "Validation Error",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return false;
                }

                if (!isValidDepartmentSelection()) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Please select a department.",
                                "Validation Error",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return false;
                }

                if (!isSubjectCodeAvailable()) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Subject code already exists. Please use a unique code.",
                                "Validation Error",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return false;
                }

                return true;
	}

        private boolean hasValidUnits() {
                return readUnits() >= 0;
        }

        private int readUnits() {
		return Integer.parseInt(spinnerUnit.getValue().toString());
        }

        private boolean isValidDepartmentSelection() {
                Object selectedDepartment = cbxDepartment.getSelectedItem();
                return selectedDepartment != null
                        && departmentIdByLabel.containsKey(selectedDepartment.toString());
        }

        private boolean isSubjectCodeAvailable() {
                String subjectCode = txtSubjectCode.getText().trim().toUpperCase();
                Optional<Subject> existingSubject = subjectService.getSubjectByCode(subjectCode);
                if (existingSubject.isEmpty()) {
                        return true;
                }

                return editingSubject != null && existingSubject.get().getId().equals(editingSubject.getId());
        }

        private void saveSubject() {
                if (!isValidForm()) {
                        return;
                }

                Long departmentId = departmentIdByLabel.get(cbxDepartment.getSelectedItem().toString());

                Subject subject = editingSubject == null ? new Subject() : editingSubject;
                subject
                        .setSubjectName(txtSubjectName.getText().trim())
                        .setSubjectCode(txtSubjectCode.getText().trim().toUpperCase())
                        .setUnits(Float.valueOf(readUnits()))
                        .setDescription(textAreaDescription.getText().trim())
                        .setDepartmentId(departmentId);

                boolean success = editingSubject == null
                        ? subjectService.createSubject(subject)
                        : subjectService.updateSubject(subject);

                if (!success) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Failed to save subject. Please try again.",
                                "Save Failed",
                                JOptionPane.ERROR_MESSAGE
                        );
                        return;
                }

                JOptionPane.showMessageDialog(
                        this,
                        editingSubject == null
                                ? "Subject created successfully."
                                : "Subject updated successfully.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                );

                if (onSavedCallback != null) {
                        onSavedCallback.run();
                }

                dispose();
	}

        private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
                saveSubject();
	}

        private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {
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
                txtSubjectName = new javax.swing.JTextField();
                txtSubjectCode = new javax.swing.JTextField();
                jLabel4 = new javax.swing.JLabel();
                jLabel5 = new javax.swing.JLabel();
                spinnerUnit = new javax.swing.JSpinner();
                cbxDepartment = new javax.swing.JComboBox<>();
                jLabel7 = new javax.swing.JLabel();
                jLabel8 = new javax.swing.JLabel();
                jScrollPane1 = new javax.swing.JScrollPane();
                textAreaDescription = new javax.swing.JTextArea();
                btnSave = new javax.swing.JButton();
                btnCancel = new javax.swing.JButton();

                setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

                windowBar1.setTitle("Subject Form");

                jPanel1.setBackground(new java.awt.Color(255, 255, 255));
                jPanel1.setAlignmentY(-5000.0F);

                jLabel1.setFont(new java.awt.Font("Poppins", 0, 18)); // NOI18N
                jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel1.setText("Subject Form");

                jLabel2.setForeground(new java.awt.Color(102, 102, 102));
                jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel2.setText("Add/Update Subject");

                jLabel3.setText("Subject Name");

                txtSubjectName.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                txtSubjectCode.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                jLabel4.setText("Subject Code");

                jLabel5.setText("Units");

                spinnerUnit.setModel(new javax.swing.SpinnerNumberModel(0, 0, 25, 1));
                spinnerUnit.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                cbxDepartment.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

                jLabel7.setText("Department");

                jLabel8.setText("Description");

                textAreaDescription.setColumns(20);
                textAreaDescription.setRows(5);
                jScrollPane1.setViewportView(textAreaDescription);

                btnSave.setBackground(new java.awt.Color(119, 0, 0));
                btnSave.setForeground(new java.awt.Color(255, 255, 255));
                btnSave.setText("Save");

                btnCancel.setBackground(new java.awt.Color(119, 0, 0));
                btnCancel.setForeground(new java.awt.Color(255, 255, 255));
                btnCancel.setText("Cancel");

                javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
                jPanel1.setLayout(jPanel1Layout);
                jPanel1Layout.setHorizontalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addGap(42, 42, 42)
                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                .addComponent(jLabel8)
                                                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 319, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                                .addComponent(jLabel5)
                                                                .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 320, Short.MAX_VALUE)
                                                                .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                .addComponent(txtSubjectName)
                                                                .addComponent(txtSubjectCode)
                                                                .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                .addComponent(spinnerUnit)
                                                                .addComponent(jLabel7)
                                                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                                                        .addGap(1, 1, 1)
                                                                        .addComponent(cbxDepartment, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addGap(102, 102, 102)
                                                .addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(18, 18, 18)
                                                .addComponent(btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap(42, Short.MAX_VALUE))
                );
                jPanel1Layout.setVerticalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(21, 21, 21)
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel2)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtSubjectName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtSubjectCode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spinnerUnit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel7)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cbxDepartment, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel8)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(42, Short.MAX_VALUE))
                );

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
                getContentPane().setLayout(layout);
                layout.setHorizontalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(windowBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 404, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(windowBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                );

                pack();
        }// </editor-fold>//GEN-END:initComponents

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

		/* Create and display the dialog */
		java.awt.EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				SubjectForm dialog = new SubjectForm(new javax.swing.JFrame(), true);
				dialog.addWindowListener(new java.awt.event.WindowAdapter() {
					@Override
					public void windowClosing(java.awt.event.WindowEvent e) {
						System.exit(0);
					}
				});
				dialog.setVisible(true);
			}
		});
	}

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton btnCancel;
        private javax.swing.JButton btnSave;
        private javax.swing.JComboBox<String> cbxDepartment;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel4;
        private javax.swing.JLabel jLabel5;
        private javax.swing.JLabel jLabel7;
        private javax.swing.JLabel jLabel8;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JSpinner spinnerUnit;
        private javax.swing.JTextArea textAreaDescription;
        private javax.swing.JTextField txtSubjectCode;
        private javax.swing.JTextField txtSubjectName;
        private com.group5.paul_esys.components.WindowBar windowBar1;
        // End of variables declaration//GEN-END:variables
}
