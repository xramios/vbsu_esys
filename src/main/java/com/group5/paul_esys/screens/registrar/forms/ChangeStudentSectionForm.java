/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JDialog.java to edit this template
 */
package com.group5.paul_esys.screens.registrar.forms;

import com.group5.paul_esys.modules.registrar.model.SectionChangeResult;
import com.group5.paul_esys.modules.registrar.model.SectionScheduleOption;
import com.group5.paul_esys.modules.registrar.model.StudentScheduleRow;
import com.group5.paul_esys.modules.registrar.services.RegistrarStudentScheduleService;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 *
 * @author nytri
 */
public class ChangeStudentSectionForm extends javax.swing.JDialog {
	
	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ChangeStudentSectionForm.class.getName());
        private final RegistrarStudentScheduleService registrarStudentScheduleService = RegistrarStudentScheduleService.getInstance();
        private final List<SectionScheduleOption> availableOptions = new ArrayList<>();
        private final String studentId;
        private final StudentScheduleRow selectedScheduleRow;
        private final Runnable onSavedCallback;
        private TableRowSorter<DefaultTableModel> optionRowSorter;

	/**
	 * Creates new form ChangeStudentSectionForm
	 */
	public ChangeStudentSectionForm(java.awt.Frame parent, boolean modal) {
		this(parent, modal, null, null, null);
	}

        public ChangeStudentSectionForm(
                java.awt.Frame parent,
                boolean modal,
                String studentId,
                StudentScheduleRow selectedScheduleRow,
                Runnable onSavedCallback
        ) {
		super(parent, modal);
                this.studentId = studentId;
                this.selectedScheduleRow = selectedScheduleRow;
                this.onSavedCallback = onSavedCallback;
		initComponents();
                initializeForm(parent);
	}

        private void initializeForm(java.awt.Frame parent) {
                setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
                setLocationRelativeTo(parent);

                btnCancel.addActionListener(evt -> dispose());
                jButton1.addActionListener(this::jButton1ActionPerformed);
                btnSearch.addKeyListener(new java.awt.event.KeyAdapter() {
                        @Override
                        public void keyReleased(java.awt.event.KeyEvent evt) {
                                applySearchFilter();
                        }
                });

                configureOptionsTable();
                loadAvailableSectionOptions();
        }

        private void configureOptionsTable() {
                DefaultTableModel model = new DefaultTableModel(
                        new Object[][]{},
                        new String[]{"Section", "Instructor", "Schedule", "Room", "Available Slots"}
                ) {
                        @Override
                        public boolean isCellEditable(int row, int column) {
                                return false;
                        }
                };

                tableAvailableRoomsAndSchedule.setModel(model);
                tableAvailableRoomsAndSchedule.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
                tableAvailableRoomsAndSchedule.setRowHeight(26);

                optionRowSorter = new TableRowSorter<>(model);
                tableAvailableRoomsAndSchedule.setRowSorter(optionRowSorter);
        }

        private void loadAvailableSectionOptions() {
                DefaultTableModel model = (DefaultTableModel) tableAvailableRoomsAndSchedule.getModel();
                model.setRowCount(0);
                availableOptions.clear();

                if (selectedScheduleRow == null) {
                        jButton1.setEnabled(false);
                        return;
                }

                setTitle("Change Section - " + selectedScheduleRow.subjectCode());

                List<SectionScheduleOption> options = registrarStudentScheduleService.getAvailableSectionSchedules(
                        selectedScheduleRow.enrollmentPeriodId(),
                        selectedScheduleRow.subjectId(),
                        selectedScheduleRow.offeringId()
                );

                availableOptions.addAll(options);

                for (SectionScheduleOption option : options) {
                        model.addRow(new Object[]{
                                option.sectionCode(),
                                option.instructor(),
                                option.schedule(),
                                option.room(),
                                option.availableSlots() == null ? "N/A" : String.valueOf(option.availableSlots())
                        });
                }

                boolean hasOptions = !availableOptions.isEmpty();
                jButton1.setEnabled(hasOptions);

                if (hasOptions) {
                        tableAvailableRoomsAndSchedule.setRowSelectionInterval(0, 0);
                }
        }

        private void applySearchFilter() {
                if (optionRowSorter == null) {
                        return;
                }

                String keyword = btnSearch.getText() == null ? "" : btnSearch.getText().trim();
                if (keyword.isEmpty()) {
                        optionRowSorter.setRowFilter(null);
                        return;
                }

                optionRowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(keyword)));
        }

        private SectionScheduleOption getSelectedOption() {
                int selectedViewRow = tableAvailableRoomsAndSchedule.getSelectedRow();
                if (selectedViewRow < 0) {
                        return null;
                }

                int selectedModelRow = tableAvailableRoomsAndSchedule.convertRowIndexToModel(selectedViewRow);
                if (selectedModelRow < 0 || selectedModelRow >= availableOptions.size()) {
                        return null;
                }

                return availableOptions.get(selectedModelRow);
        }

	/**
	 * This method is called from within the constructor to initialize the
	 * form. WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                jScrollPane1 = new javax.swing.JScrollPane();
                tableAvailableRoomsAndSchedule = new javax.swing.JTable();
                jLabel1 = new javax.swing.JLabel();
                btnSearch = new javax.swing.JTextField();
                jButton1 = new javax.swing.JButton();
                btnCancel = new javax.swing.JButton();

                setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

                tableAvailableRoomsAndSchedule.setModel(new javax.swing.table.DefaultTableModel(
                        new Object [][] {
                                {null, null, null, null},
                                {null, null, null, null},
                                {null, null, null, null},
                                {null, null, null, null}
                        },
                        new String [] {
                                "Title 1", "Title 2", "Title 3", "Title 4"
                        }
                ));
                jScrollPane1.setViewportView(tableAvailableRoomsAndSchedule);

                jLabel1.setText("Search");

                btnSearch.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                jButton1.setBackground(new java.awt.Color(119, 0, 0));
                jButton1.setForeground(new java.awt.Color(255, 255, 255));
                jButton1.setText("Save");

                btnCancel.setBackground(new java.awt.Color(119, 0, 0));
                btnCancel.setForeground(new java.awt.Color(255, 255, 255));
                btnCancel.setText("Cancel");

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
                getContentPane().setLayout(layout);
                layout.setHorizontalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 748, Short.MAX_VALUE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(jLabel1)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 350, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                                .addGap(0, 0, Short.MAX_VALUE)
                                                .addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap())
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGap(15, 15, 15)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel1)
                                        .addComponent(btnSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 362, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jButton1)
                                        .addComponent(btnCancel))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );

                pack();
        }// </editor-fold>//GEN-END:initComponents

        private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
                if (studentId == null || selectedScheduleRow == null) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Unable to process section change request.",
                                "Change Section",
                                JOptionPane.ERROR_MESSAGE
                        );
                        return;
                }

                SectionScheduleOption selectedOption = getSelectedOption();
                if (selectedOption == null) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Please select a target section and schedule.",
                                "Change Section",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                SectionChangeResult result = registrarStudentScheduleService.changeStudentSection(
                        studentId,
                        selectedScheduleRow.enrollmentDetailId(),
                        selectedOption.offeringId()
                );

                if (!result.success()) {
                        JOptionPane.showMessageDialog(
                                this,
                                result.message(),
                                "Change Section",
                                JOptionPane.ERROR_MESSAGE
                        );
                        return;
                }

                JOptionPane.showMessageDialog(
                        this,
                        result.message(),
                        "Change Section",
                        JOptionPane.INFORMATION_MESSAGE
                );

                if (onSavedCallback != null) {
                        onSavedCallback.run();
                }

                dispose();
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

		/* Create and display the dialog */
		java.awt.EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				ChangeStudentSectionForm dialog = new ChangeStudentSectionForm(new javax.swing.JFrame(), true);
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
        private javax.swing.JTextField btnSearch;
        private javax.swing.JButton jButton1;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JTable tableAvailableRoomsAndSchedule;
        // End of variables declaration//GEN-END:variables
}
