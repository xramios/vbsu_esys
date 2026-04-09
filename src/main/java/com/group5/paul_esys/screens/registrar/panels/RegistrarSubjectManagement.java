/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package com.group5.paul_esys.screens.registrar.panels;

import com.group5.paul_esys.modules.curriculum.model.Curriculum;
import com.group5.paul_esys.modules.curriculum.services.CurriculumService;
import com.group5.paul_esys.modules.departments.model.Department;
import com.group5.paul_esys.modules.departments.services.DepartmentService;
import com.group5.paul_esys.modules.subjects.model.Subject;
import com.group5.paul_esys.modules.subjects.services.SubjectService;
import com.group5.paul_esys.screens.registrar.forms.SubjectForm;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author nytri
 */
public class RegistrarSubjectManagement extends javax.swing.JPanel {

        private static final String FILTER_ALL = "ALL";

        private final SubjectService subjectService = SubjectService.getInstance();
        private final DepartmentService departmentService = DepartmentService.getInstance();
        private final CurriculumService curriculumService = CurriculumService.getInstance();

        private final Map<Long, String> departmentNameById = new LinkedHashMap<>();
        private final Map<Long, String> curriculumNameById = new LinkedHashMap<>();
        private final Map<String, Long> departmentIdByName = new LinkedHashMap<>();

        private List<Subject> subjects = new ArrayList<>();
        private List<Subject> filteredSubjects = new ArrayList<>();

	/**
	 * Creates new form RegistrarSubjectPanel
	 */
	public RegistrarSubjectManagement() {
		initComponents();
		initializeSubjectPanel();
	}

        private void initializeSubjectPanel() {
                menuItemUpdateSubject.setText("Update Subject");
                menuItemDeleteSubject.setText("Delete Subject");

                tableSubjects.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
                tableSubjects.setComponentPopupMenu(jPopupMenu1);

                configureTableModel();
                registerFilterListeners();
                registerTablePopupSelectionBehavior();
                initializeSubjects();
        }

        private void configureTableModel() {
                DefaultTableModel model = new DefaultTableModel(
                        new Object[][]{},
                        new String[]{"Name", "Code", "Units", "Description", "Curriculum", "Department"}
                ) {
                        @Override
                        public boolean isCellEditable(int rowIndex, int columnIndex) {
                                return false;
                        }
                };

                tableSubjects.setModel(model);
                tableSubjects.setRowHeight(28);
        }

        private void registerFilterListeners() {
                txtSearch.getDocument().addDocumentListener(new DocumentListener() {
                        @Override
                        public void insertUpdate(DocumentEvent e) {
                                applyFilters();
                        }

                        @Override
                        public void removeUpdate(DocumentEvent e) {
                                applyFilters();
                        }

                        @Override
                        public void changedUpdate(DocumentEvent e) {
                                applyFilters();
                        }
                });

                cbxDepartment.addItemListener(evt -> {
                        if (evt.getStateChange() == ItemEvent.SELECTED) {
                                applyFilters();
                        }
                });
        }

        private void registerTablePopupSelectionBehavior() {
                tableSubjects.addMouseListener(new MouseAdapter() {
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
                                if (SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() == 2) {
                                        openUpdateSubjectForm();
                                }
                        }
                });
        }

        private void selectRowFromPointer(MouseEvent evt) {
                if (!evt.isPopupTrigger()) {
                        return;
                }

                int row = tableSubjects.rowAtPoint(evt.getPoint());
                if (row >= 0) {
                        tableSubjects.setRowSelectionInterval(row, row);
                }
        }

        private void initializeSubjects() {
                loadLookupData();
                subjects = subjectService.getAllSubjects();
                reloadDepartmentFilterOptions();
                applyFilters();
        }

        private void loadLookupData() {
                departmentNameById.clear();
                for (Department department : departmentService.getAllDepartments()) {
                        departmentNameById.put(department.getId(), safeText(department.getDepartmentName(), "N/A"));
                }

                curriculumNameById.clear();
                for (Curriculum curriculum : curriculumService.getAllCurriculums()) {
                        curriculumNameById.put(curriculum.getId(), buildCurriculumDisplayName(curriculum));
                }
        }

        private String buildCurriculumDisplayName(Curriculum curriculum) {
                String curriculumName = safeText(curriculum.getName(), "Curriculum");
                if (curriculum.getCurYear() == null) {
                        return curriculumName;
                }

                return curriculumName + " (" + extractYear(curriculum.getCurYear()) + ")";
        }

        private int extractYear(Date date) {
                if (date instanceof java.sql.Date sqlDate) {
                        return sqlDate.toLocalDate().getYear();
                }

                return date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate().getYear();
        }

        private void reloadDepartmentFilterOptions() {
                Object selectedDepartment = cbxDepartment.getSelectedItem();
                String selectedValue = selectedDepartment == null
                        ? FILTER_ALL
                        : selectedDepartment.toString();

                cbxDepartment.removeAllItems();
                cbxDepartment.addItem(FILTER_ALL);
                departmentIdByName.clear();

                for (Department department : departmentService.getAllDepartments()) {
                        String departmentName = safeText(department.getDepartmentName(), "Department " + department.getId());
                        cbxDepartment.addItem(departmentName);
                        departmentIdByName.put(departmentName, department.getId());
                }

                if (FILTER_ALL.equals(selectedValue) || departmentIdByName.containsKey(selectedValue)) {
                        cbxDepartment.setSelectedItem(selectedValue);
                } else {
                        cbxDepartment.setSelectedItem(FILTER_ALL);
                }
        }

        private void applyFilters() {
                String searchTerm = txtSearch.getText() == null
                        ? ""
                        : txtSearch.getText().trim().toLowerCase();

                String selectedDepartment = cbxDepartment.getSelectedItem() == null
                        ? FILTER_ALL
                        : cbxDepartment.getSelectedItem().toString();

                Long selectedDepartmentId = FILTER_ALL.equals(selectedDepartment)
                        ? null
                        : departmentIdByName.get(selectedDepartment);

                List<Subject> matchingSubjects = subjects
                        .stream()
                        .filter(subject -> matchesSearch(subject, searchTerm))
                        .filter(subject -> matchesDepartment(subject, selectedDepartmentId))
                        .collect(Collectors.toList());

                populateTable(matchingSubjects);
        }

        private boolean matchesSearch(Subject subject, String searchTerm) {
                if (searchTerm.isEmpty()) {
                        return true;
                }

                String subjectName = safeText(subject.getSubjectName(), "").toLowerCase();
                String subjectCode = safeText(subject.getSubjectCode(), "").toLowerCase();
                String description = safeText(subject.getDescription(), "").toLowerCase();

                return subjectName.contains(searchTerm)
                        || subjectCode.contains(searchTerm)
                        || description.contains(searchTerm);
        }

        private boolean matchesDepartment(Subject subject, Long selectedDepartmentId) {
                return selectedDepartmentId == null || selectedDepartmentId.equals(subject.getDepartmentId());
        }

        private void populateTable(List<Subject> subjectsToDisplay) {
                filteredSubjects = new ArrayList<>(subjectsToDisplay);

                DefaultTableModel model = (DefaultTableModel) tableSubjects.getModel();
                model.setRowCount(0);

                for (Subject subject : subjectsToDisplay) {
                        model.addRow(
                                new Object[]{
                                        safeText(subject.getSubjectName(), "N/A"),
                                        safeText(subject.getSubjectCode(), "N/A"),
                                        subject.getUnits() == null ? "N/A" : subject.getUnits(),
                                        buildDescriptionPreview(subject.getDescription()),
                                        curriculumNameById.getOrDefault(subject.getCurriculumId(), "N/A"),
                                        departmentNameById.getOrDefault(subject.getDepartmentId(), "N/A")
                                }
                        );
                }
        }

        private String buildDescriptionPreview(String description) {
                String safeDescription = safeText(description, "").trim();
                if (safeDescription.length() <= 120) {
                        return safeDescription;
                }

                return safeDescription.substring(0, 117) + "...";
        }

        private String safeText(String value, String fallback) {
                if (value == null || value.trim().isEmpty()) {
                        return fallback;
                }
                return value.trim();
        }

        private Subject getSelectedSubject() {
                int selectedRow = tableSubjects.getSelectedRow();
                if (selectedRow < 0) {
                        return null;
                }

                int modelRow = tableSubjects.convertRowIndexToModel(selectedRow);
                if (modelRow < 0 || modelRow >= filteredSubjects.size()) {
                        return null;
                }

                return filteredSubjects.get(modelRow);
        }

        private void openCreateSubjectForm() {
                openSubjectForm(null);
        }

        private void openUpdateSubjectForm() {
                Subject selectedSubject = getSelectedSubject();
                if (selectedSubject == null) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Please select a subject to update.",
                                "Update Subject",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                openSubjectForm(selectedSubject);
        }

        private void openSubjectForm(Subject editingSubject) {
                Window window = SwingUtilities.getWindowAncestor(this);
                Frame parentFrame = window instanceof Frame ? (Frame) window : null;

                SubjectForm form = new SubjectForm(
                        parentFrame,
                        true,
                        editingSubject,
                        this::initializeSubjects
                );
                form.setVisible(true);
        }

        private void deleteSelectedSubject() {
                Subject selectedSubject = getSelectedSubject();
                if (selectedSubject == null) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Please select a subject to delete.",
                                "Delete Subject",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                int option = JOptionPane.showConfirmDialog(
                        this,
                        "Delete subject " + safeText(selectedSubject.getSubjectCode(), "") + "?",
                        "Confirm Delete",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                if (option != JOptionPane.YES_OPTION) {
                        return;
                }

                boolean deleted = subjectService.deleteSubject(selectedSubject.getId());
                if (!deleted) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Failed to delete subject. It may be referenced by other records.",
                                "Delete Subject",
                                JOptionPane.ERROR_MESSAGE
                        );
                        return;
                }

                initializeSubjects();
                JOptionPane.showMessageDialog(
                        this,
                        "Subject deleted successfully.",
                        "Delete Subject",
                        JOptionPane.INFORMATION_MESSAGE
                );
        }

	/**
	 * This method is called from within the constructor to initialize the
	 * form. WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                jPopupMenu1 = new javax.swing.JPopupMenu();
                menuItemUpdateSubject = new javax.swing.JMenuItem();
                menuItemDeleteSubject = new javax.swing.JMenuItem();
                jPanel1 = new javax.swing.JPanel();
                jLabel3 = new javax.swing.JLabel();
                txtSearch = new javax.swing.JTextField();
                jScrollPane1 = new javax.swing.JScrollPane();
                tableSubjects = new javax.swing.JTable();
                btnCreateSubject = new javax.swing.JButton();
                jLabel4 = new javax.swing.JLabel();
                cbxDepartment = new javax.swing.JComboBox<>();
                btnClearFilter = new javax.swing.JButton();
                jLabel1 = new javax.swing.JLabel();
                jLabel2 = new javax.swing.JLabel();

                menuItemUpdateSubject.setText("jMenuItem1");
                menuItemUpdateSubject.addActionListener(this::menuItemUpdateSubjectActionPerformed);
                jPopupMenu1.add(menuItemUpdateSubject);

                menuItemDeleteSubject.setText("jMenuItem2");
                menuItemDeleteSubject.addActionListener(this::menuItemDeleteSubjectActionPerformed);
                jPopupMenu1.add(menuItemDeleteSubject);

                setBackground(new java.awt.Color(255, 255, 255));
                setPreferredSize(new java.awt.Dimension(1181, 684));

                jPanel1.setBackground(new java.awt.Color(255, 255, 255));
                jPanel1.setBorder(new com.group5.paul_esys.ui.PanelRoundBorder());

                jLabel3.setText("Search:");

                txtSearch.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtSearch.setToolTipText("Search by code, title, description");
                txtSearch.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                tableSubjects.setModel(new javax.swing.table.DefaultTableModel(
                        new Object [][] {
                                {null, null, null, null, null, null},
                                {null, null, null, null, null, null},
                                {null, null, null, null, null, null},
                                {null, null, null, null, null, null}
                        },
                        new String [] {
                                "Name", "Code", "Units", "Description", "Curriculum", "Department"
                        }
                ));
                jScrollPane1.setViewportView(tableSubjects);

                btnCreateSubject.setBackground(new java.awt.Color(255, 234, 234));
                btnCreateSubject.setText("Create Subject");
                btnCreateSubject.addActionListener(this::btnCreateSubjectActionPerformed);

                jLabel4.setText("Department:");

                cbxDepartment.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "ALL" }));

                btnClearFilter.setBackground(new java.awt.Color(255, 234, 234));
                btnClearFilter.setText("Clear Filter");
                btnClearFilter.addActionListener(this::btnClearFilterActionPerformed);

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
                                                .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 478, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel4)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(cbxDepartment, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnClearFilter)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnCreateSubject)))
                                .addContainerGap())
                );
                jPanel1Layout.setVerticalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel3)
                                        .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnCreateSubject, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel4)
                                        .addComponent(cbxDepartment, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnClearFilter, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 537, Short.MAX_VALUE)
                                .addContainerGap())
                );

                jLabel1.setFont(new java.awt.Font("Poppins", 0, 24)); // NOI18N
                jLabel1.setText("Subject Management");

                jLabel2.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel2.setForeground(new java.awt.Color(153, 153, 153));
                jLabel2.setText("Create and update course subjects and credit units.");

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

        private void menuItemUpdateSubjectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemUpdateSubjectActionPerformed
                openUpdateSubjectForm();
        }//GEN-LAST:event_menuItemUpdateSubjectActionPerformed

        private void menuItemDeleteSubjectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemDeleteSubjectActionPerformed
                deleteSelectedSubject();
        }//GEN-LAST:event_menuItemDeleteSubjectActionPerformed

        private void btnClearFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearFilterActionPerformed
                txtSearch.setText("");
                if (cbxDepartment.getItemCount() > 0) {
                        cbxDepartment.setSelectedItem(FILTER_ALL);
                }
                applyFilters();
        }//GEN-LAST:event_btnClearFilterActionPerformed

        private void btnCreateSubjectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCreateSubjectActionPerformed
                openCreateSubjectForm();
        }//GEN-LAST:event_btnCreateSubjectActionPerformed


        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton btnClearFilter;
        private javax.swing.JButton btnCreateSubject;
        private javax.swing.JComboBox<String> cbxDepartment;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel4;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JPopupMenu jPopupMenu1;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JMenuItem menuItemDeleteSubject;
        private javax.swing.JMenuItem menuItemUpdateSubject;
        private javax.swing.JTable tableSubjects;
        private javax.swing.JTextField txtSearch;
        // End of variables declaration//GEN-END:variables
}
