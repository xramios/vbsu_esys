/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package com.group5.paul_esys.screens.registrar.panels;

import com.group5.paul_esys.modules.courses.model.Course;
import com.group5.paul_esys.modules.courses.services.CourseService;
import com.group5.paul_esys.modules.students.model.Student;
import com.group5.paul_esys.modules.students.services.StudentService;
import com.group5.paul_esys.screens.registrar.forms.StudentForm;
import com.group5.paul_esys.screens.registrar.forms.StudentSchedulesForm;
import com.group5.paul_esys.screens.registrar.forms.UpdateStudentForm;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author nytri
 */
public final class RegistrarStudentManagement extends javax.swing.JPanel {

        private final StudentService studentService = StudentService.getInstance();
        private final CourseService courseService = CourseService.getInstance();
        private List<Student> students = new ArrayList<>();
        private final Map<Long, String> courseNameById = new LinkedHashMap<>();

        private transient SwingWorker<?, ?> currentWorker;
        private volatile boolean initializing = false;

        /**
         * Creates new form StudentManagementPanel
         */
        public RegistrarStudentManagement() {
                initComponents();
                tableRegistrarStudents.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                jButton1.addActionListener(this::jButton1ActionPerformed);
                cbxStatusFilter.removeAllItems();
                cbxStatusFilter.addItem("ALL");
                cbxStatusFilter.addItem("REGULAR");
                cbxStatusFilter.addItem("IRREGULAR");
                this.initializeStudents();
        }

        private void setLoading(boolean loading) {
                setCursor(loading ? java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR)
                                : java.awt.Cursor.getDefaultCursor());
                btnRefresh.setEnabled(!loading);
                btnAddStudent.setEnabled(!loading);
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
                        if ("state".equals(evt.getPropertyName()) && SwingWorker.StateValue.DONE == evt.getNewValue()) {
                                setLoading(false);
                        }
                });
                worker.execute();
        }

        private Student getSelectedStudent() {
                int selectedRow = tableRegistrarStudents.getSelectedRow();
                if (selectedRow < 0) {
                        return null;
                }

                int modelRow = tableRegistrarStudents.convertRowIndexToModel(selectedRow);
                String studentId = tableRegistrarStudents
                        .getModel()
                        .getValueAt(modelRow, 0)
                        .toString();

                return students
                        .stream()
                        .filter(student -> studentId.equals(student.getStudentId()))
                        .findFirst()
                        .orElse(null);
        }

        private List<Student> getSelectedStudents() {
                int[] selectedRows = tableRegistrarStudents.getSelectedRows();
                if (selectedRows.length == 0) {
                        return List.of();
                }

                List<Student> selectedStudents = new ArrayList<>();
                for (int selectedRow : selectedRows) {
                        int modelRow = tableRegistrarStudents.convertRowIndexToModel(selectedRow);
                        Object studentIdCell = tableRegistrarStudents.getModel().getValueAt(modelRow, 0);

                        if (studentIdCell == null) {
                                continue;
                        }

                        String studentId = studentIdCell.toString();
                        students
                                .stream()
                                .filter(student -> studentId.equals(student.getStudentId()))
                                .findFirst()
                                .ifPresent(selectedStudents::add);
                }

                return selectedStudents;
        }

        private String buildDeleteConfirmationMessage(List<Student> selectedStudents) {
                if (selectedStudents.size() == 1) {
                        return "Delete student " + selectedStudents.get(0).getStudentId() + "?";
                }

                int previewCount = Math.min(10, selectedStudents.size());
                StringBuilder message = new StringBuilder(
                        "Delete " + selectedStudents.size() + " selected students?\n\n"
                );

                for (int index = 0; index < previewCount; index++) {
                        message.append("- ")
                                .append(selectedStudents.get(index).getStudentId())
                                .append("\n");
                }

                if (selectedStudents.size() > previewCount) {
                        int remaining = selectedStudents.size() - previewCount;
                        message.append("... and ").append(remaining).append(" more.");
                }

                return message.toString().trim();
        }

        private void populateStudentDetails(Student student) {
                if (student == null) {
                        txtStudentFirstName.setText("");
                        txtStudentMiddleName.setText("");
                        txtStudentLastName.setText("");
                        txtStudentCourse.setText("");
                        txtStudentYearLevel.setText("");
                        txtStudentEmailAddress.setText("");
                        return;
                }

                txtStudentFirstName.setText(student.getFirstName());
                txtStudentMiddleName.setText(student.getMiddleName() == null ? "" : student.getMiddleName());
                txtStudentLastName.setText(student.getLastName());
                txtStudentCourse.setText(courseNameById.getOrDefault(student.getCourseId(), "N/A"));
                txtStudentYearLevel.setText(student.getYearLevel() == null ? "" : String.valueOf(student.getYearLevel()));

                if (student.getUserId() == null) {
                        txtStudentEmailAddress.setText("");
                        return;
                }

                txtStudentEmailAddress.setText("Loading...");
                Long userId = student.getUserId();

                SwingWorker<String, Void> emailWorker = new SwingWorker<>() {
                        @Override
                        protected String doInBackground() {
                                return studentService.getUserEmailByUserId(userId).orElse("");
                        }

                        @Override
                        protected void done() {
                                try {
                                        String email = get();
                                        if (!isCancelled()) {
                                                txtStudentEmailAddress.setText(email);
                                        }
                                } catch (Exception e) {
                                        if (!isCancelled()) {
                                                txtStudentEmailAddress.setText("");
                                        }
                                }
                        }
                };
                emailWorker.execute();
        }

	/**
	 * This method is called from within the constructor to initialize the
	 * form. WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                jPopupMenu1 = new javax.swing.JPopupMenu();
                menuItemUpdateStudent = new javax.swing.JMenuItem();
                menuItemDeleteStudent = new javax.swing.JMenuItem();
                jPanel2 = new javax.swing.JPanel();
                btnRefresh = new javax.swing.JButton();
                btnAddStudent = new javax.swing.JButton();
                jLabel17 = new javax.swing.JLabel();
                jButton1 = new javax.swing.JButton();
                jPanel5 = new javax.swing.JPanel();
                jLabel11 = new javax.swing.JLabel();
                txtStudentsSearch = new javax.swing.JTextField();
                jLabel12 = new javax.swing.JLabel();
                cbxCourseFilter = new javax.swing.JComboBox<>();
                cbxYearLevelFilter = new javax.swing.JComboBox<>();
                jLabel13 = new javax.swing.JLabel();
                jLabel14 = new javax.swing.JLabel();
                cbxStatusFilter = new javax.swing.JComboBox<>();
                btnClearFilter = new javax.swing.JButton();
                jScrollPane1 = new javax.swing.JScrollPane();
                tableRegistrarStudents = new javax.swing.JTable();
                jPanel1 = new javax.swing.JPanel();
                jLabel6 = new javax.swing.JLabel();
                txtStudentFirstName = new javax.swing.JTextField();
                txtStudentMiddleName = new javax.swing.JTextField();
                jLabel7 = new javax.swing.JLabel();
                txtStudentLastName = new javax.swing.JTextField();
                jLabel8 = new javax.swing.JLabel();
                txtStudentCourse = new javax.swing.JTextField();
                jLabel9 = new javax.swing.JLabel();
                txtStudentYearLevel = new javax.swing.JTextField();
                jLabel10 = new javax.swing.JLabel();
                txtStudentEmailAddress = new javax.swing.JTextField();
                jLabel15 = new javax.swing.JLabel();
                jLabel16 = new javax.swing.JLabel();

                menuItemUpdateStudent.setText("Update Student");
                menuItemUpdateStudent.addActionListener(this::menuItemUpdateStudentActionPerformed);
                jPopupMenu1.add(menuItemUpdateStudent);

                menuItemDeleteStudent.setText("Delete Student");
                menuItemDeleteStudent.addActionListener(this::menuItemDeleteStudentActionPerformed);
                jPopupMenu1.add(menuItemDeleteStudent);

                setPreferredSize(new java.awt.Dimension(1181, 684));

                jPanel2.setBackground(new java.awt.Color(255, 255, 255));
                jPanel2.setBorder(new com.group5.paul_esys.ui.PanelRoundBorder());

                btnRefresh.setBackground(new java.awt.Color(236, 235, 240));
                btnRefresh.setText("Refresh");
                btnRefresh.addActionListener(this::btnRefreshActionPerformed);

                btnAddStudent.setBackground(new java.awt.Color(61, 115, 201));
                btnAddStudent.setForeground(new java.awt.Color(255, 255, 255));
                btnAddStudent.setText("Add Student");
                btnAddStudent.addActionListener(this::btnAddStudentActionPerformed);

                jLabel17.setBackground(new java.awt.Color(236, 235, 240));
                jLabel17.setFont(new java.awt.Font("Poppins", 0, 24)); // NOI18N
                jLabel17.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel17.setText("Student Management");

                jButton1.setText("Student Schedules");

                javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
                jPanel2.setLayout(jPanel2Layout);
                jPanel2Layout.setHorizontalGroup(
                        jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel17)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 158, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnAddStudent, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnRefresh, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
                );
                jPanel2Layout.setVerticalGroup(
                        jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(btnAddStudent, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                                .addGap(5, 5, 5)
                                                .addComponent(jLabel17, javax.swing.GroupLayout.DEFAULT_SIZE, 43, Short.MAX_VALUE))
                                        .addComponent(btnRefresh, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
                );

                jPanel5.setBackground(new java.awt.Color(255, 255, 255));
                jPanel5.setBorder(new com.group5.paul_esys.ui.PanelRoundBorder());

                jLabel11.setText("Search:");

                txtStudentsSearch.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtStudentsSearch.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());
                txtStudentsSearch.addKeyListener(new java.awt.event.KeyAdapter() {
                        public void keyReleased(java.awt.event.KeyEvent evt) {
                                txtStudentsSearchKeyReleased(evt);
                        }
                });

                jLabel12.setText("Course:");

                cbxCourseFilter.addItemListener(this::cbxCourseFilterItemStateChanged);

                cbxYearLevelFilter.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "ALL" }));
                cbxYearLevelFilter.addItemListener(this::cbxYearLevelFilterItemStateChanged);

                jLabel13.setText("Year Level:");

                jLabel14.setText("Status");

                cbxStatusFilter.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "ALL", "REGULAR", "IRREGULAR" }));
                cbxStatusFilter.addItemListener(this::cbxStatusFilterItemStateChanged);

                btnClearFilter.setBackground(new java.awt.Color(119, 0, 0));
                btnClearFilter.setForeground(new java.awt.Color(255, 255, 255));
                btnClearFilter.setText("Clear Filter");
                btnClearFilter.addActionListener(this::btnClearFilterActionPerformed);

                tableRegistrarStudents.setModel(new javax.swing.table.DefaultTableModel(
                        new Object [][] {

                        },
                        new String [] {
                                "Student ID", "First Name", "Last Name", "Course", "Status"
                        }
                ) {
                        Class[] types = new Class [] {
                                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
                        };
                        boolean[] canEdit = new boolean [] {
                                false, false, false, false, false
                        };

                        public Class getColumnClass(int columnIndex) {
                                return types [columnIndex];
                        }

                        public boolean isCellEditable(int rowIndex, int columnIndex) {
                                return canEdit [columnIndex];
                        }
                });
                tableRegistrarStudents.setComponentPopupMenu(jPopupMenu1);
                tableRegistrarStudents.addMouseListener(new java.awt.event.MouseAdapter() {
                        public void mouseClicked(java.awt.event.MouseEvent evt) {
                                tableRegistrarStudentsMouseClicked(evt);
                        }
                });
                jScrollPane1.setViewportView(tableRegistrarStudents);

                jPanel1.setBackground(new java.awt.Color(255, 255, 255));
                jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(new com.group5.paul_esys.ui.PanelRoundBorder(), "Student Information"));
                jPanel1.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N

                jLabel6.setText("First Name");

                txtStudentFirstName.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtStudentFirstName.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());
                txtStudentFirstName.setEnabled(false);

                txtStudentMiddleName.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtStudentMiddleName.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());
                txtStudentMiddleName.setEnabled(false);

                jLabel7.setText("Middle Name");

                txtStudentLastName.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtStudentLastName.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());
                txtStudentLastName.setEnabled(false);

                jLabel8.setText("Last Name");

                txtStudentCourse.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtStudentCourse.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());
                txtStudentCourse.setEnabled(false);

                jLabel9.setText("Course");

                txtStudentYearLevel.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtStudentYearLevel.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());
                txtStudentYearLevel.setEnabled(false);

                jLabel10.setText("Year Level");

                txtStudentEmailAddress.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtStudentEmailAddress.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());
                txtStudentEmailAddress.setEnabled(false);

                jLabel15.setText("Student Email Address (Portal Account)");

                jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel16.setText("Picture");

                javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
                jPanel1.setLayout(jPanel1Layout);
                jPanel1Layout.setHorizontalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel16, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                                .addGap(0, 0, Short.MAX_VALUE)
                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                                .addGroup(jPanel1Layout.createSequentialGroup()
                                                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(txtStudentFirstName, javax.swing.GroupLayout.PREFERRED_SIZE, 143, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                                .addGroup(jPanel1Layout.createSequentialGroup()
                                                                                        .addComponent(txtStudentMiddleName)
                                                                                        .addContainerGap())))
                                                                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                .addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                                .addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, 298, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                                        .addComponent(txtStudentEmailAddress, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 292, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                        .addComponent(txtStudentYearLevel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 292, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                        .addComponent(txtStudentCourse, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 292, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                                        .addComponent(txtStudentLastName, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 292, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                                .addContainerGap())))))
                );
                jPanel1Layout.setVerticalGroup(
                        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel16, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel6)
                                        .addComponent(jLabel7))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(txtStudentFirstName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(txtStudentMiddleName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel8)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtStudentLastName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel9)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtStudentCourse, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel10)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtStudentYearLevel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel15)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtStudentEmailAddress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
                );

                javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
                jPanel5.setLayout(jPanel5Layout);
                jPanel5Layout.setHorizontalGroup(
                        jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel5Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel5Layout.createSequentialGroup()
                                                .addComponent(jLabel11)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtStudentsSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 281, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel12)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(cbxCourseFilter, javax.swing.GroupLayout.PREFERRED_SIZE, 149, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel13)
                                                .addGap(5, 5, 5)
                                                .addComponent(cbxYearLevelFilter, javax.swing.GroupLayout.PREFERRED_SIZE, 149, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(7, 7, 7)
                                                .addComponent(jLabel14)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(cbxStatusFilter, javax.swing.GroupLayout.PREFERRED_SIZE, 149, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnClearFilter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addGroup(jPanel5Layout.createSequentialGroup()
                                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 770, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 316, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(0, 0, Short.MAX_VALUE)))
                                .addContainerGap())
                );
                jPanel5Layout.setVerticalGroup(
                        jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(cbxStatusFilter, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(btnClearFilter, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                .addComponent(jLabel12)
                                                .addComponent(jLabel13)
                                                .addComponent(jLabel14)
                                                .addComponent(cbxYearLevelFilter, javax.swing.GroupLayout.DEFAULT_SIZE, 37, Short.MAX_VALUE)
                                                .addComponent(cbxCourseFilter, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(txtStudentsSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(jLabel11)))
                                .addGap(9, 9, 9)
                                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 536, Short.MAX_VALUE)
                                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
                );

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
                this.setLayout(layout);
                layout.setHorizontalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
                );
        }// </editor-fold>//GEN-END:initComponents

        private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshActionPerformed
                initializeStudents();
        }//GEN-LAST:event_btnRefreshActionPerformed

        private void btnAddStudentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddStudentActionPerformed
                StudentForm form = new StudentForm(this::initializeStudents);
		form.setVisible(true);
        }//GEN-LAST:event_btnAddStudentActionPerformed

        private void txtStudentsSearchKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtStudentsSearchKeyReleased
                applyTableFilters();
        }//GEN-LAST:event_txtStudentsSearchKeyReleased

        private void cbxCourseFilterItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbxCourseFilterItemStateChanged
                applyTableFilters();
        }//GEN-LAST:event_cbxCourseFilterItemStateChanged

        private void cbxYearLevelFilterItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbxYearLevelFilterItemStateChanged
                applyTableFilters();
        }//GEN-LAST:event_cbxYearLevelFilterItemStateChanged

        private void cbxStatusFilterItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbxStatusFilterItemStateChanged
                applyTableFilters();
        }//GEN-LAST:event_cbxStatusFilterItemStateChanged

        private void btnClearFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearFilterActionPerformed
                txtStudentsSearch.setText("");
                if (cbxCourseFilter.getItemCount() > 0) {
                        cbxCourseFilter.setSelectedItem("ALL");
                }
                if (cbxYearLevelFilter.getItemCount() > 0) {
                        cbxYearLevelFilter.setSelectedItem("ALL");
                }
                if (cbxStatusFilter.getItemCount() > 0) {
                        cbxStatusFilter.setSelectedItem("ALL");
                }

                initializeStudents();
        }//GEN-LAST:event_btnClearFilterActionPerformed

        private void tableRegistrarStudentsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableRegistrarStudentsMouseClicked
                populateStudentDetails(getSelectedStudent());
        }//GEN-LAST:event_tableRegistrarStudentsMouseClicked

        private void menuItemDeleteStudentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemDeleteStudentActionPerformed
                List<Student> selectedStudents = getSelectedStudents();
                if (selectedStudents.isEmpty()) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Please select at least one student to delete.",
                                "Delete Student",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                int decision = JOptionPane.showConfirmDialog(
                        this,
                        buildDeleteConfirmationMessage(selectedStudents),
                        selectedStudents.size() == 1 ? "Confirm Delete" : "Confirm Bulk Delete",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );

                if (decision != JOptionPane.YES_OPTION) {
                        return;
                }

                executeAsync(new SwingWorker<Boolean, Void>() {
                        @Override
                        protected Boolean doInBackground() {
                                studentService.deleteAll(selectedStudents);
                                return true;
                        }

                        @Override
                        protected void done() {
                                try {
                                        get();
                                        initializeStudents();
                                        JOptionPane.showMessageDialog(
                                                RegistrarStudentManagement.this,
                                                selectedStudents.size() == 1
                                                        ? "Student deleted successfully."
                                                        : selectedStudents.size() + " students deleted successfully.",
                                                "Delete Student",
                                                JOptionPane.INFORMATION_MESSAGE
                                        );
                                } catch (Exception e) {
                                        JOptionPane.showMessageDialog(
                                                RegistrarStudentManagement.this,
                                                "Failed to delete student(s): " + e.getMessage(),
                                                "Error",
                                                JOptionPane.ERROR_MESSAGE
                                        );
                                }
                        }
                });
        }//GEN-LAST:event_menuItemDeleteStudentActionPerformed

        private void menuItemUpdateStudentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemUpdateStudentActionPerformed
                int[] selectedRows = tableRegistrarStudents.getSelectedRows();
                if (selectedRows.length > 1) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Please select only one student to update.",
                                "Update Student",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                Student selectedStudent = getSelectedStudent();
                if (selectedStudent == null) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Please select a student to update.",
                                "Update Student",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                java.awt.Window window = SwingUtilities.getWindowAncestor(this);
                java.awt.Frame parentFrame = window instanceof java.awt.Frame
                        ? (java.awt.Frame) window
                        : null;

                UpdateStudentForm form = new UpdateStudentForm(
                        parentFrame,
                        true,
                        selectedStudent,
                        this::initializeStudents
                );
                form.setVisible(true);
        }//GEN-LAST:event_menuItemUpdateStudentActionPerformed

        private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
                int[] selectedRows = tableRegistrarStudents.getSelectedRows();
                if (selectedRows.length != 1) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Please select exactly one student to view schedules.",
                                "Student Schedules",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                Student selectedStudent = getSelectedStudent();
                if (selectedStudent == null) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Unable to resolve selected student.",
                                "Student Schedules",
                                JOptionPane.ERROR_MESSAGE
                        );
                        return;
                }

                String selectedCourseName = courseNameById.getOrDefault(selectedStudent.getCourseId(), "N/A");
                String selectedEmailAddress = selectedStudent.getUserId() == null
                        ? ""
                        : studentService.getUserEmailByUserId(selectedStudent.getUserId()).orElse("");

                StudentSchedulesForm form = new StudentSchedulesForm(
                        selectedStudent,
                        selectedCourseName,
                        selectedEmailAddress
                );
                form.setVisible(true);
        }

        private void applyTableFilters() {
                if (initializing) return;
                String searchTerm = txtStudentsSearch.getText() == null
                        ? ""
                        : txtStudentsSearch.getText().trim().toLowerCase();
                String selectedCourse = cbxCourseFilter.getSelectedItem() == null
                        ? "ALL"
                        : cbxCourseFilter.getSelectedItem().toString();
                String selectedStatus = cbxStatusFilter.getSelectedItem() == null
                        ? "ALL"
                        : cbxStatusFilter.getSelectedItem().toString();
                String selectedYearLevel = cbxYearLevelFilter.getSelectedItem() == null
                        ? "ALL"
                        : cbxYearLevelFilter.getSelectedItem().toString();

                List<Student> filteredStudents = students
                        .stream()
                        .filter(student -> {
                                String fullName = ((student.getFirstName() == null ? "" : student.getFirstName())
                                        + " "
                                        + (student.getLastName() == null ? "" : student.getLastName())).toLowerCase();
                                String courseName = courseNameById.getOrDefault(student.getCourseId(), "N/A");
                                String studentId = student.getStudentId() == null ? "" : student.getStudentId().toLowerCase();

                                boolean matchesSearch = searchTerm.isEmpty()
                                        || studentId.contains(searchTerm)
                                        || fullName.contains(searchTerm);
                                boolean matchesCourse = "ALL".equals(selectedCourse)
                                        || courseName.equals(selectedCourse);
                                boolean matchesStatus = "ALL".equals(selectedStatus)
                                        || (student.getStudentStatus() != null
                                                && student.getStudentStatus().name().equals(selectedStatus));
                                boolean matchesYear = "ALL".equals(selectedYearLevel)
                                        || String.valueOf(student.getYearLevel()).equals(selectedYearLevel);

                                return matchesSearch && matchesCourse && matchesStatus && matchesYear;
                        })
                        .collect(Collectors.toList());

                DefaultTableModel model = (DefaultTableModel) tableRegistrarStudents.getModel();
                model.setRowCount(0);

                for (Student student : filteredStudents) {
                        String courseName = courseNameById.getOrDefault(student.getCourseId(), "N/A");
                        String status = student.getStudentStatus() == null ? "N/A" : student.getStudentStatus().toString();
                        model.addRow(
                                new Object[]{
                                        student.getStudentId(),
                                        student.getFirstName(),
                                        student.getLastName(),
                                        courseName,
                                        status
                                }
                        );
                }

                populateStudentDetails(null);
        }

	public void initializeStudents() {
                executeAsync(new SwingWorker<StudentLoadResult, Void>() {
                        @Override
                        protected StudentLoadResult doInBackground() {
                                Map<Long, String> lookup = new LinkedHashMap<>();
                                for (Course course : courseService.getAllCourses()) {
                                        if (isCancelled()) return null;
                                        lookup.put(course.getId(), course.getCourseName());
                                }

                                if (isCancelled()) return null;
                                List<Student> loadedStudents = studentService.list();
                                return new StudentLoadResult(loadedStudents, lookup);
                        }

                        @Override
                        protected void done() {
                                try {
                                        StudentLoadResult result = get();
                                        if (result == null || isCancelled()) return;

                                        students = result.students;
                                        courseNameById.clear();
                                        courseNameById.putAll(result.courseLookup);

                                        initializing = true;
                                        try {
                                                DefaultTableModel model = (DefaultTableModel) tableRegistrarStudents.getModel();
                                                model.setRowCount(0);

                                                cbxCourseFilter.removeAllItems();
                                                cbxCourseFilter.addItem("ALL");

                                                cbxYearLevelFilter.removeAllItems();
                                                cbxYearLevelFilter.addItem("ALL");

                                                List<String> distinctCourses = new ArrayList<>();
                                                List<String> distinctYearLevels = new ArrayList<>();

                                                for (Student student : students) {
                                                        String courseName = courseNameById.getOrDefault(student.getCourseId(), "N/A");
                                                        String yearLevel = String.valueOf(student.getYearLevel());
                                                        String status = student.getStudentStatus() == null ? "N/A" : student.getStudentStatus().toString();

                                                        model.addRow(new Object[]{
                                                                student.getStudentId(),
                                                                student.getFirstName(),
                                                                student.getLastName(),
                                                                courseName,
                                                                status
                                                        });

                                                        if (!distinctCourses.contains(courseName)) {
                                                                distinctCourses.add(courseName);
                                                        }

                                                        if (!distinctYearLevels.contains(yearLevel)) {
                                                                distinctYearLevels.add(yearLevel);
                                                        }
                                                }

                                                distinctCourses.forEach(cbxCourseFilter::addItem);
                                                distinctYearLevels.forEach(cbxYearLevelFilter::addItem);

                                                populateStudentDetails(null);
                                        } finally {
                                                initializing = false;
                                        }
                                } catch (Exception e) {
                                        if (!isCancelled()) {
                                                JOptionPane.showMessageDialog(
                                                        RegistrarStudentManagement.this,
                                                        "Failed to load students: " + e.getMessage(),
                                                        "Error",
                                                        JOptionPane.ERROR_MESSAGE
                                                );
                                        }
                                }
                        }
                });
	}

        private record StudentLoadResult(List<Student> students, Map<Long, String> courseLookup) {}

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton btnAddStudent;
        private javax.swing.JButton btnClearFilter;
        private javax.swing.JButton btnRefresh;
        private javax.swing.JComboBox<String> cbxCourseFilter;
        private javax.swing.JComboBox<String> cbxStatusFilter;
        private javax.swing.JComboBox<String> cbxYearLevelFilter;
        private javax.swing.JButton jButton1;
        private javax.swing.JLabel jLabel10;
        private javax.swing.JLabel jLabel11;
        private javax.swing.JLabel jLabel12;
        private javax.swing.JLabel jLabel13;
        private javax.swing.JLabel jLabel14;
        private javax.swing.JLabel jLabel15;
        private javax.swing.JLabel jLabel16;
        private javax.swing.JLabel jLabel17;
        private javax.swing.JLabel jLabel6;
        private javax.swing.JLabel jLabel7;
        private javax.swing.JLabel jLabel8;
        private javax.swing.JLabel jLabel9;
        private javax.swing.JPanel jPanel1;
        private javax.swing.JPanel jPanel2;
        private javax.swing.JPanel jPanel5;
        private javax.swing.JPopupMenu jPopupMenu1;
        private javax.swing.JScrollPane jScrollPane1;
        private javax.swing.JMenuItem menuItemDeleteStudent;
        private javax.swing.JMenuItem menuItemUpdateStudent;
        private javax.swing.JTable tableRegistrarStudents;
        private javax.swing.JTextField txtStudentCourse;
        private javax.swing.JTextField txtStudentEmailAddress;
        private javax.swing.JTextField txtStudentFirstName;
        private javax.swing.JTextField txtStudentLastName;
        private javax.swing.JTextField txtStudentMiddleName;
        private javax.swing.JTextField txtStudentYearLevel;
        private javax.swing.JTextField txtStudentsSearch;
        // End of variables declaration//GEN-END:variables
}
