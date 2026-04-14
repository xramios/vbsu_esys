/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package com.group5.paul_esys.screens.admin.panels;

import com.group5.paul_esys.modules.admin.model.Admin;
import com.group5.paul_esys.modules.admin.services.AdminService;
import com.group5.paul_esys.modules.faculty.model.Faculty;
import com.group5.paul_esys.modules.faculty.services.FacultyService;
import com.group5.paul_esys.modules.registrar.model.Registrar;
import com.group5.paul_esys.modules.registrar.services.RegistrarService;
import com.group5.paul_esys.modules.students.model.Student;
import com.group5.paul_esys.modules.students.services.StudentService;
import com.group5.paul_esys.modules.users.models.user.UserDirectoryRow;
import com.group5.paul_esys.modules.users.services.UserDirectoryService;
import com.group5.paul_esys.modules.users.services.UserSession;
import com.group5.paul_esys.screens.admin.forms.AdminForm;
import com.group5.paul_esys.screens.registrar.forms.FacultyForm;
import com.group5.paul_esys.screens.registrar.forms.RegistrarForm;
import com.group5.paul_esys.screens.registrar.forms.StudentForm;
import com.group5.paul_esys.screens.registrar.forms.UpdateStudentForm;
import com.group5.paul_esys.screens.sign_in.SignIn;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author nytri
 */
public class AdminDashboardPanel extends javax.swing.JPanel {

        private final UserDirectoryService userDirectoryService = UserDirectoryService.getInstance();
        private final StudentService studentService = StudentService.getInstance();
        private final FacultyService facultyService = FacultyService.getInstance();
        private final RegistrarService registrarService = RegistrarService.getInstance();
        private final AdminService adminService = AdminService.getInstance();

        private final List<UserDirectoryRow> allUsers = new ArrayList<>();
        private final List<UserDirectoryRow> filteredUsers = new ArrayList<>();

        private final DefaultTableModel userTableModel = new DefaultTableModel(
                        new Object[][]{},
                        new String[]{"Full Name", "Contact Number", "Email", "Role"}
        ) {
                @Override
                public boolean isCellEditable(int row, int column) {
                        return false;
                }
        };

        private final javax.swing.JPopupMenu popupMenu = new javax.swing.JPopupMenu();
        private final javax.swing.JPopupMenu tablePopupMenu = new javax.swing.JPopupMenu();
        private final JMenuItem menuItemAddRegistrar = new JMenuItem("Add Registrar");
        private final JMenuItem menuItemAddStudent = new JMenuItem("Add Student");
        private final JMenuItem menuItemAddFaculty = new JMenuItem("Add Faculty");
        private final JMenuItem menuItemAddAdmin = new JMenuItem("Add Admin");
        private final JMenuItem menuItemUpdateUser = new JMenuItem("Update User");
        private final JMenuItem menuItemDeleteUser = new JMenuItem("Delete User");

	/**
	 * Creates new form AdminDashboard
	 */
	public AdminDashboardPanel() {
		initComponents();
		initializeDashboard();
	}

        private void initializeDashboard() {
                configureHeader();
                configureAddUserMenu();
                configureUserTable();
                configureTablePopupMenu();
                bindUiActions();
                reloadUsers();
        }

        private void configureHeader() {
                UserSession session = UserSession.getInstance();
                if (session.getUserInformation() != null && session.getUserInformation().getUser() instanceof Admin admin) {
                        String fullName = String.format("%s, %s", admin.getLastName(), admin.getFirstName());
                        jLabel1.setText("Welcome Admin " + fullName + "!");
                }

                JButton logoutButton = new JButton("Logout");
                logoutButton.setFont(new Font("Poppins", Font.PLAIN, 13));
                logoutButton.putClientProperty("JButton.buttonType", "roundRect");
                logoutButton.putClientProperty("JComponent.minimumWidth", 120);
                logoutButton.addActionListener(evt -> logoutCurrentUser());

                JPanel trailingPanel = new JPanel(new BorderLayout());
                trailingPanel.setOpaque(false);
                trailingPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
                trailingPanel.add(logoutButton, BorderLayout.SOUTH);

                cbxRole.putClientProperty("JComboBox.trailingComponent", trailingPanel);
        }

        private void logoutCurrentUser() {
                int confirm = JOptionPane.showConfirmDialog(
                                this,
                                "Are you sure you want to logout?",
                                "Confirm Logout",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE
                );

                if (confirm != JOptionPane.YES_OPTION) {
                        return;
                }

                UserSession.getInstance().logout();
                Window window = SwingUtilities.getWindowAncestor(this);
                if (window != null) {
                        window.dispose();
                }
                SwingUtilities.invokeLater(() -> new SignIn().setVisible(true));
        }

        private void configureAddUserMenu() {
                menuItemAddRegistrar.setText("Add Registrar");
                menuItemAddStudent.setText("Add Student");
                menuItemAddFaculty.setText("Add Faculty");
                menuItemAddAdmin.setText("Add Admin");

                menuItemAddStudent.addActionListener(evt -> openStudentFormForCreate());
                menuItemAddFaculty.addActionListener(evt -> openFacultyFormForCreate());
                menuItemAddRegistrar.addActionListener(evt -> openRegistrarFormForCreate());
                menuItemAddAdmin.addActionListener(evt -> openAdminFormForCreate());

                popupMenu.add(menuItemAddRegistrar);
                popupMenu.add(menuItemAddStudent);
                popupMenu.add(menuItemAddFaculty);
                popupMenu.add(menuItemAddAdmin);
        }

        private void configureUserTable() {
                jTable1.setModel(userTableModel);
                jTable1.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
                jTable1.setRowHeight(28);

                jTable1.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent evt) {
                                if (evt.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(evt)) {
                                        openSelectedUserForUpdate();
                                        return;
                                }

                                if (SwingUtilities.isRightMouseButton(evt)) {
                                        int row = jTable1.rowAtPoint(evt.getPoint());
                                        if (row >= 0) {
                                                jTable1.setRowSelectionInterval(row, row);
                                        }
                                }
                        }
                });
        }

        private void configureTablePopupMenu() {
                menuItemUpdateUser.addActionListener(evt -> openSelectedUserForUpdate());
                menuItemDeleteUser.addActionListener(evt -> deleteSelectedUser());
                tablePopupMenu.add(menuItemUpdateUser);
                tablePopupMenu.add(menuItemDeleteUser);
                jTable1.setComponentPopupMenu(tablePopupMenu);
        }

        private void bindUiActions() {
                btnAddUser.addActionListener(evt -> showAddUserMenu());
                jButton1.addActionListener(evt -> clearFilters());
                cbxRole.addActionListener(evt -> applyFilters());

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
        }

        private void showAddUserMenu() {
                popupMenu.show(btnAddUser, 0, btnAddUser.getHeight());
        }

        private void clearFilters() {
                txtSearch.setText("");
                cbxRole.setSelectedItem("ALL");
                applyFilters();
        }

        private void reloadUsers() {
                allUsers.clear();
                allUsers.addAll(userDirectoryService.getAllUsers());
                applyFilters();
        }

        private void applyFilters() {
                String searchTerm = txtSearch.getText() == null ? "" : txtSearch.getText().trim();
                String selectedRole = cbxRole.getSelectedItem() == null
                                ? "ALL"
                                : cbxRole.getSelectedItem().toString();

                filteredUsers.clear();
                for (UserDirectoryRow userRow : allUsers) {
                        boolean matchesRole = "ALL".equalsIgnoreCase(selectedRole)
                                        || userRow.getRole().name().equalsIgnoreCase(selectedRole);
                        if (!matchesRole) {
                                continue;
                        }

                        if (!userRow.matchesSearchTerm(searchTerm)) {
                                continue;
                        }

                        filteredUsers.add(userRow);
                }

                userTableModel.setRowCount(0);
                for (UserDirectoryRow userRow : filteredUsers) {
                        userTableModel.addRow(new Object[]{
                                userRow.getFullName(),
                                userRow.getContactNumber() == null ? "-" : userRow.getContactNumber(),
                                userRow.getEmail() == null ? "-" : userRow.getEmail(),
                                userRow.getRole().name(),
                        });
                }
        }

        private UserDirectoryRow getSelectedUserRow() {
                int selectedRow = jTable1.getSelectedRow();
                if (selectedRow < 0) {
                        return null;
                }

                int modelRow = jTable1.convertRowIndexToModel(selectedRow);
                if (modelRow < 0 || modelRow >= filteredUsers.size()) {
                        return null;
                }

                return filteredUsers.get(modelRow);
        }

        private void openSelectedUserForUpdate() {
                UserDirectoryRow selectedUser = getSelectedUserRow();
                if (selectedUser == null) {
                        JOptionPane.showMessageDialog(
                                        this,
                                        "Please select a user to update.",
                                        "Update User",
                                        JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                switch (selectedUser.getRole()) {
                        case STUDENT -> openStudentFormForUpdate(selectedUser);
                        case FACULTY -> openFacultyFormForUpdate(selectedUser);
                        case REGISTRAR -> openRegistrarFormForUpdate(selectedUser);
                        case ADMIN -> openAdminFormForUpdate(selectedUser);
                }
        }

        private void openStudentFormForCreate() {
                StudentForm form = new StudentForm(this::reloadUsers);
                form.setVisible(true);
        }

        private void openStudentFormForUpdate(UserDirectoryRow selectedUser) {
                if (selectedUser.getStudentId() == null || selectedUser.getStudentId().isBlank()) {
                        JOptionPane.showMessageDialog(
                                        this,
                                        "Selected student is missing a student ID.",
                                        "Update Student",
                                        JOptionPane.ERROR_MESSAGE
                        );
                        return;
                }

                Optional<Student> student = studentService.get(selectedUser.getStudentId());
                if (student.isEmpty()) {
                        JOptionPane.showMessageDialog(
                                        this,
                                        "Student record not found.",
                                        "Update Student",
                                        JOptionPane.ERROR_MESSAGE
                        );
                        return;
                }

                                Frame parentFrame = getParentFrame();
                UpdateStudentForm form = new UpdateStudentForm(
                                        parentFrame,
                                true,
                                student.get(),
                                this::reloadUsers
                );
                form.setVisible(true);
        }

        private void openFacultyFormForCreate() {
                FacultyForm form = new FacultyForm(null, null, this::reloadUsers);
                form.setVisible(true);
        }

        private void openFacultyFormForUpdate(UserDirectoryRow selectedUser) {
                if (selectedUser.getProfileId() == null) {
                        JOptionPane.showMessageDialog(
                                        this,
                                        "Selected faculty user is missing a profile ID.",
                                        "Update Faculty",
                                        JOptionPane.ERROR_MESSAGE
                        );
                        return;
                }

                Optional<Faculty> faculty = facultyService.getFacultyById(selectedUser.getProfileId());
                if (faculty.isEmpty()) {
                        JOptionPane.showMessageDialog(
                                        this,
                                        "Faculty record not found.",
                                        "Update Faculty",
                                        JOptionPane.ERROR_MESSAGE
                        );
                        return;
                }

                FacultyForm form = new FacultyForm(faculty.get(), null, this::reloadUsers);
                form.setVisible(true);
        }

        private void openRegistrarFormForCreate() {
                RegistrarForm form = new RegistrarForm(null, this::reloadUsers);
                form.setVisible(true);
        }

        private void openRegistrarFormForUpdate(UserDirectoryRow selectedUser) {
                if (selectedUser.getProfileId() == null) {
                        JOptionPane.showMessageDialog(
                                        this,
                                        "Selected registrar user is missing a profile ID.",
                                        "Update Registrar",
                                        JOptionPane.ERROR_MESSAGE
                        );
                        return;
                }

                Optional<Registrar> registrar = registrarService.getRegistrarById(selectedUser.getProfileId());
                if (registrar.isEmpty()) {
                        JOptionPane.showMessageDialog(
                                        this,
                                        "Registrar record not found.",
                                        "Update Registrar",
                                        JOptionPane.ERROR_MESSAGE
                        );
                        return;
                }

                RegistrarForm form = new RegistrarForm(registrar.get(), this::reloadUsers);
                form.setVisible(true);
        }

        private void openAdminFormForCreate() {
                AdminForm form = new AdminForm(null, this::reloadUsers);
                form.setVisible(true);
        }

        private void openAdminFormForUpdate(UserDirectoryRow selectedUser) {
                if (selectedUser.getProfileId() == null) {
                        JOptionPane.showMessageDialog(
                                        this,
                                        "Selected admin user is missing a profile ID.",
                                        "Update Admin",
                                        JOptionPane.ERROR_MESSAGE
                        );
                        return;
                }

                Optional<Admin> admin = adminService.getAdminById(selectedUser.getProfileId());
                if (admin.isEmpty()) {
                        JOptionPane.showMessageDialog(
                                        this,
                                        "Admin record not found.",
                                        "Update Admin",
                                        JOptionPane.ERROR_MESSAGE
                        );
                        return;
                }

                AdminForm form = new AdminForm(admin.get(), this::reloadUsers);
                form.setVisible(true);
        }

        private void deleteSelectedUser() {
                UserDirectoryRow selectedUser = getSelectedUserRow();
                if (selectedUser == null) {
                        JOptionPane.showMessageDialog(
                                        this,
                                        "Please select a user to delete.",
                                        "Delete User",
                                        JOptionPane.WARNING_MESSAGE
                        );
                        return;
                }

                int confirmation = JOptionPane.showConfirmDialog(
                                this,
                                "Delete " + selectedUser.getFullName() + " (" + selectedUser.getRole().name() + ")?",
                                "Confirm Delete",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE
                );

                if (confirmation != JOptionPane.YES_OPTION) {
                        return;
                }

                boolean deleted = userDirectoryService.deleteUser(selectedUser);
                if (!deleted) {
                        JOptionPane.showMessageDialog(
                                        this,
                                        "Failed to delete user. The account may be referenced by other records.",
                                        "Delete User",
                                        JOptionPane.ERROR_MESSAGE
                        );
                        return;
                }

                JOptionPane.showMessageDialog(
                                this,
                                "User deleted successfully.",
                                "Delete User",
                                JOptionPane.INFORMATION_MESSAGE
                );
                reloadUsers();
        }

        private Frame getParentFrame() {
                Window window = SwingUtilities.getWindowAncestor(this);
                return window instanceof Frame frame ? frame : null;
        }

	/**
	 * This method is called from within the constructor to initialize the
	 * form. WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
	private void initComponents() {

                jLabel2 = new javax.swing.JLabel();
                jLabel1 = new javax.swing.JLabel();
                jPanel2 = new javax.swing.JPanel();
                jLabel3 = new javax.swing.JLabel();
                txtSearch = new javax.swing.JTextField();
                jLabel4 = new javax.swing.JLabel();
                cbxRole = new javax.swing.JComboBox<>();
                btnAddUser = new javax.swing.JButton();
                jButton1 = new javax.swing.JButton();
                jScrollPane1 = new javax.swing.JScrollPane();
                jTable1 = new javax.swing.JTable();

                jLabel2.setForeground(new java.awt.Color(153, 153, 153));
                jLabel2.setText("Manage users!");

                jLabel1.setFont(new java.awt.Font("Poppins", 0, 24)); // NOI18N
                jLabel1.setText("Welcome Admin!");

                jPanel2.setBackground(new java.awt.Color(255, 255, 255));
                jPanel2.setBorder(new com.group5.paul_esys.ui.PanelRoundBorder());

                jLabel3.setText("Search");

                txtSearch.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                jLabel4.setText("Role");

                cbxRole.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "ALL", "STUDENT", "REGISTRAR", "FACULTY", "ADMIN" }));

                btnAddUser.setBackground(new java.awt.Color(119, 0, 0));
                btnAddUser.setForeground(new java.awt.Color(255, 255, 255));
                btnAddUser.setText("Add User");

                jButton1.setBackground(new java.awt.Color(119, 0, 0));
                jButton1.setForeground(new java.awt.Color(255, 255, 255));
                jButton1.setText("Clear Filter");

                jTable1.setModel(new javax.swing.table.DefaultTableModel(
                        new Object [][] {
                                {null, null, null, null},
                                {null, null, null, null},
                                {null, null, null, null},
                                {null, null, null, null}
                        },
                        new String [] {
                                "Full Name", "Contact Number", "Email", "Role"
                        }
                ));
                jScrollPane1.setViewportView(jTable1);

                javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
                jPanel2.setLayout(jPanel2Layout);
                jPanel2Layout.setHorizontalGroup(
                        jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addComponent(jLabel3)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 500, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel4)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(cbxRole, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jButton1)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(btnAddUser, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addComponent(jScrollPane1))
                                .addContainerGap())
                );
                jPanel2Layout.setVerticalGroup(
                        jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel3)
                                        .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel4)
                                        .addComponent(cbxRole, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnAddUser)
                                        .addComponent(jButton1))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 582, Short.MAX_VALUE)
                                .addContainerGap())
                );

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
                this.setLayout(layout);
                layout.setHorizontalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel1)
                                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel2))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );
        }// </editor-fold>//GEN-END:initComponents

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton btnAddUser;
	private javax.swing.JComboBox<String> cbxRole;
	private javax.swing.JButton jButton1;
	private javax.swing.JLabel jLabel1;
	private javax.swing.JLabel jLabel2;
	private javax.swing.JLabel jLabel3;
	private javax.swing.JLabel jLabel4;
	private javax.swing.JPanel jPanel2;
	private javax.swing.JScrollPane jScrollPane1;
	private javax.swing.JTable jTable1;
	private javax.swing.JTextField txtSearch;
	// End of variables declaration//GEN-END:variables
}
