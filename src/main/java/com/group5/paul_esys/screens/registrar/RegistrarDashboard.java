package com.group5.paul_esys.screens.registrar;

import com.group5.paul_esys.modules.registrar.model.Registrar;
import com.group5.paul_esys.modules.users.services.UserSession;
import com.group5.paul_esys.screens.registrar.panels.RegistrarDropRequestsManagement;
import com.group5.paul_esys.screens.registrar.panels.RegistrarEnrollmentPeriodManagement;
import com.group5.paul_esys.screens.registrar.panels.RegistrarOfferingsManagement;
import com.group5.paul_esys.screens.registrar.panels.RegistrarSchedulesManagement;
import com.group5.paul_esys.screens.registrar.panels.RegistrarSectionsManagement;
import com.group5.paul_esys.screens.registrar.panels.RegistrarStudentManagement;
import com.group5.paul_esys.screens.shared.panels.SettingsPanel;
import com.group5.paul_esys.screens.sign_in.SignIn;
import com.group5.paul_esys.utils.ThemeManager;
import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

/**
 *
 * @author Shan
 */
public final class RegistrarDashboard extends javax.swing.JFrame {

    private final java.util.Set<Integer> loadedTabs = new java.util.HashSet<>();

    /**
     * Creates new form RegisterPortal
     */
    public RegistrarDashboard() {
        ThemeManager.applySavedTheme();
        this.setUndecorated(true);
        initComponents();
        configureLogoutAction();
        this.setLocationRelativeTo(null);

        // Set yung pangalan ng title
        Registrar registrar = (Registrar) UserSession.getInstance()
                .getUserInformation()
                .getUser();
        String fullName = String.format(
                "%s, %s",
                registrar.getLastName(),
                registrar.getFirstName());
        this.windowBar1.setTitle("Welcome " + fullName);

        // Add nung mga tabs with lazy loading
        tabbedPaneStudents.add("Dashboard", null);
        tabbedPaneStudents.add("Students", null);
        tabbedPaneStudents.add("Sections", null);
        tabbedPaneStudents.add("Offerings", null);
        tabbedPaneStudents.add("Enrollment Periods", null);
        tabbedPaneStudents.add("Drop Requests", null);
        tabbedPaneStudents.add("Settings", null);

        loadTab(0);

        tabbedPaneStudents.addChangeListener(evt -> {
            int selectedIndex = tabbedPaneStudents.getSelectedIndex();
            if (selectedIndex < 0) {
                return;
            }

            if (!loadedTabs.contains(selectedIndex)) {
                loadTab(selectedIndex);
                return;
            }

            refreshTab(selectedIndex);
        });
    }

    private void configureLogoutAction() {
        JButton logoutButton = new JButton("Logout");
        logoutButton.setFont(new Font("Poppins", Font.PLAIN, 13));
        logoutButton.putClientProperty("JButton.buttonType", "roundRect");
        logoutButton.putClientProperty("JComponent.minimumWidth", 120);
        logoutButton.addActionListener(evt -> logoutCurrentUser());

        JPanel trailingPanel = new JPanel(new BorderLayout());
        trailingPanel.setOpaque(false);
        trailingPanel.setBorder(BorderFactory.createEmptyBorder(10, 8, 12, 8));
        trailingPanel.add(logoutButton, BorderLayout.SOUTH);

        tabbedPaneStudents.putClientProperty("JTabbedPane.trailingComponent", trailingPanel);
    }

    private void logoutCurrentUser() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to logout?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        UserSession.getInstance().logout();
        this.dispose();
        SwingUtilities.invokeLater(() -> new SignIn().setVisible(true));
    }

    private void loadTab(int tabIndex) {
        if (loadedTabs.contains(tabIndex)) {
            return;
        }

        javax.swing.JPanel panel = switch (tabIndex) {
            case 0 -> new com.group5.paul_esys.screens.registrar.panels.RegistrarDashboard();
            case 1 -> new RegistrarStudentManagement();
            case 2 -> new RegistrarSectionsManagement();
            case 3 -> new RegistrarOfferingsManagement();
            case 4 -> new RegistrarEnrollmentPeriodManagement();
            case 5 -> new RegistrarDropRequestsManagement();
            case 6 -> new SettingsPanel();
            default -> null;
        };

        if (panel != null) {
            tabbedPaneStudents.setComponentAt(tabIndex, panel);
            loadedTabs.add(tabIndex);
        }
    }

    private void refreshTab(int tabIndex) {
        JPanel panel = (JPanel) tabbedPaneStudents.getComponentAt(tabIndex);

        switch (panel) {
            case com.group5.paul_esys.screens.registrar.panels.RegistrarDashboard dashboardPanel ->
                dashboardPanel.refreshData();
            case RegistrarStudentManagement studentManagement ->
                studentManagement.initializeStudents();
            case RegistrarSectionsManagement sectionsManagement ->
                sectionsManagement.refreshData();
            case RegistrarOfferingsManagement offeringsManagement ->
                offeringsManagement.refreshData();
            case RegistrarSchedulesManagement schedulesManagement ->
                schedulesManagement.refreshData();
            case RegistrarEnrollmentPeriodManagement enrollmentPeriodManagement ->
                enrollmentPeriodManagement.refreshData();
            case RegistrarDropRequestsManagement dropRequestsManagement ->
                dropRequestsManagement.refreshData();
            case SettingsPanel settingsPanel ->
                settingsPanel.refreshData();
            default -> {
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        tabbedPaneStudents = new javax.swing.JTabbedPane();
        windowBar1 = new com.group5.paul_esys.components.WindowBar();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
        setSize(new java.awt.Dimension(1280, 720));

        tabbedPaneStudents.setBackground(new java.awt.Color(255, 255, 255));
        tabbedPaneStudents.setTabPlacement(javax.swing.JTabbedPane.LEFT);
        tabbedPaneStudents.setPreferredSize(new java.awt.Dimension(1280, 720));

        windowBar1.setTitle("Welcome!");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(windowBar1, javax.swing.GroupLayout.DEFAULT_SIZE,
                                javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(tabbedPaneStudents, javax.swing.GroupLayout.PREFERRED_SIZE, 1280,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE)));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(windowBar1, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(tabbedPaneStudents, javax.swing.GroupLayout.PREFERRED_SIZE, 684,
                                        javax.swing.GroupLayout.PREFERRED_SIZE)));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new RegistrarDashboard().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTabbedPane tabbedPaneStudents;
    private com.group5.paul_esys.components.WindowBar windowBar1;
    // End of variables declaration//GEN-END:variables
}
