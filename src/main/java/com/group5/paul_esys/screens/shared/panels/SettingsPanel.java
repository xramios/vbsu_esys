package com.group5.paul_esys.screens.shared.panels;

import com.group5.paul_esys.modules.admin.model.Admin;
import com.group5.paul_esys.modules.faculty.model.Faculty;
import com.group5.paul_esys.modules.registrar.model.Registrar;
import com.group5.paul_esys.modules.students.model.Student;
import com.group5.paul_esys.modules.users.models.user.UserInformation;
import com.group5.paul_esys.modules.users.services.AccountSecurityService;
import com.group5.paul_esys.modules.users.services.UserSession;
import com.group5.paul_esys.utils.FormValidationUtil;
import com.group5.paul_esys.utils.ThemeManager;
import com.group5.paul_esys.utils.ThemeOption;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

public class SettingsPanel extends javax.swing.JPanel {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 128;

    private final AccountSecurityService accountSecurityService = AccountSecurityService.getInstance();

    public SettingsPanel() {
        initComponents();
        initializePanel();
    }

    private void initializePanel() {
        loadUserInformation();
        bindActions();
        loadThemeOptions();
    }

    public void refreshData() {
        loadUserInformation();
        loadThemeOptions();
    }

    private void loadUserInformation() {
        UserInformation<?> userInformation = UserSession.getInstance().getUserInformation();
        if (userInformation == null) {
            return;
        }

        lblAccountIdValue.setText(
                valueOrFallback(userInformation.getId() == null ? null : String.valueOf(userInformation.getId())));
        lblAccountRoleValue.setText(resolveRoleDisplayName(userInformation.getUser()));
        lblAccountNameValue.setText(resolveFullName(userInformation.getUser()));
        lblAccountEmailValue.setText(valueOrFallback(userInformation.getEmail()));
        lblAccountIdentifierValue.setText(resolveIdentifier(userInformation.getUser()));
        lblAccountContactValue.setText(resolveContactNumber(userInformation.getUser()));
    }

    private String resolveRoleDisplayName(Object user) {
        if (user instanceof Student) {
            return "Student";
        }
        if (user instanceof Registrar) {
            return "Registrar";
        }
        if (user instanceof Faculty) {
            return "Faculty";
        }
        if (user instanceof Admin) {
            return "Admin";
        }
        return "-";
    }

    private String resolveFullName(Object user) {
        if (user instanceof Student student) {
            return formatName(student.getFirstName(), student.getMiddleName(), student.getLastName());
        }
        if (user instanceof Registrar registrar) {
            return formatName(registrar.getFirstName(), null, registrar.getLastName());
        }
        if (user instanceof Faculty faculty) {
            return formatName(faculty.getFirstName(), faculty.getMiddleName(), faculty.getLastName());
        }
        if (user instanceof Admin admin) {
            return formatName(admin.getFirstName(), null, admin.getLastName());
        }
        return "-";
    }

    private String resolveIdentifier(Object user) {
        if (user instanceof Student student) {
            return valueOrFallback(student.getStudentId());
        }
        if (user instanceof Registrar registrar) {
            return valueOrFallback(registrar.getEmployeeId());
        }
        return "-";
    }

    private String resolveContactNumber(Object user) {
        if (user instanceof Registrar registrar) {
            return valueOrFallback(registrar.getContactNumber());
        }
        if (user instanceof Faculty faculty) {
            return valueOrFallback(faculty.getContactNumber());
        }
        if (user instanceof Admin admin) {
            return valueOrFallback(admin.getContactNumber());
        }
        return "-";
    }

    private String formatName(String firstName, String middleName, String lastName) {
        StringBuilder builder = new StringBuilder();

        if (firstName != null && !firstName.isBlank()) {
            builder.append(firstName.trim());
        }

        if (middleName != null && !middleName.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append(" ");
            }
            builder.append(middleName.trim());
        }

        if (lastName != null && !lastName.isBlank()) {
            if (!builder.isEmpty()) {
                builder.append(" ");
            }
            builder.append(lastName.trim());
        }

        return valueOrFallback(builder.toString());
    }

    private String valueOrFallback(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.trim();
    }

    private void bindActions() {
        btnChangePassword.addActionListener(evt -> submitPasswordChange());
        btnApplyTheme.addActionListener(evt -> applySelectedTheme());
    }

    private void loadThemeOptions() {
        cmbTheme.removeAllItems();
        for (ThemeOption option : ThemeManager.getThemeOptions()) {
            cmbTheme.addItem(option);
        }
        cmbTheme.setSelectedItem(ThemeManager.getSavedThemeOption());
    }

    private void submitPasswordChange() {
        String currentPassword = FormValidationUtil.normalizeOptionalText(new String(txtCurrentPassword.getPassword()));
        String newPassword = FormValidationUtil.normalizeOptionalText(new String(txtNewPassword.getPassword()));
        String confirmPassword = FormValidationUtil.normalizeOptionalText(new String(txtConfirmPassword.getPassword()));

        if (currentPassword == null) {
            showValidationError("Current Password is required.");
            return;
        }

        String validationError = FormValidationUtil
                .validatePassword(
                        "New Password",
                        newPassword,
                        true,
                        MIN_PASSWORD_LENGTH,
                        MAX_PASSWORD_LENGTH)
                .orElse(null);

        if (validationError != null) {
            showValidationError(validationError);
            return;
        }

        if (confirmPassword == null) {
            showValidationError("Confirm Password is required.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showValidationError("New Password and Confirm Password do not match.");
            return;
        }

        if (newPassword.equals(currentPassword)) {
            showValidationError("New Password must be different from Current Password.");
            return;
        }

        UserInformation<?> userInformation = UserSession.getInstance().getUserInformation();
        if (userInformation == null || userInformation.getId() == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "No active user session found.",
                    "Change Password",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        Long userId = userInformation.getId();
        setPasswordControlsEnabled(false);

        AtomicReference<String> failureReason = new AtomicReference<>("Unable to update password.");
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                boolean currentPasswordValid = accountSecurityService.verifyCurrentPassword(userId, currentPassword);
                if (!currentPasswordValid) {
                    failureReason.set("Current Password is incorrect.");
                    return false;
                }

                boolean updated = accountSecurityService.updatePassword(userId, newPassword);
                if (!updated) {
                    failureReason.set("Unable to update password. Please try again.");
                    return false;
                }

                return true;
            }

            @Override
            protected void done() {
                setPasswordControlsEnabled(true);
                try {
                    boolean success = get();
                    if (!success) {
                        JOptionPane.showMessageDialog(SettingsPanel.this,
                                failureReason.get(),
                                "Change Password",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    clearPasswordFields();
                    JOptionPane.showMessageDialog(SettingsPanel.this,
                            "Password updated successfully.",
                            "Change Password",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(SettingsPanel.this,
                            "Unable to update password. Please try again.",
                            "Change Password",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void applySelectedTheme() {
        ThemeOption selected = (ThemeOption) cmbTheme.getSelectedItem();
        if (selected == null) {
            return;
        }

        boolean applied = ThemeManager.applyTheme(selected.getClassName());
        if (!applied) {
            JOptionPane.showMessageDialog(
                    this,
                    "Unable to apply theme.",
                    "Theme",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(
                this,
                "Theme updated successfully.",
                "Theme",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showValidationError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "Validation Error",
                JOptionPane.WARNING_MESSAGE);
    }

    private void setPasswordControlsEnabled(boolean enabled) {
        txtCurrentPassword.setEnabled(enabled);
        txtNewPassword.setEnabled(enabled);
        txtConfirmPassword.setEnabled(enabled);
        btnChangePassword.setEnabled(enabled);
    }

    private void clearPasswordFields() {
        txtCurrentPassword.setText("");
        txtNewPassword.setText("");
        txtConfirmPassword.setText("");
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                rootPanel = new javax.swing.JPanel();
                contentPanel = new javax.swing.JPanel();
                accountPanel = new javax.swing.JPanel();
                lblAccountSection = new javax.swing.JLabel();
                lblAccountId = new javax.swing.JLabel();
                lblAccountRole = new javax.swing.JLabel();
                lblAccountName = new javax.swing.JLabel();
                lblAccountEmail = new javax.swing.JLabel();
                lblAccountIdentifier = new javax.swing.JLabel();
                lblAccountContact = new javax.swing.JLabel();
                lblAccountIdValue = new javax.swing.JLabel();
                lblAccountRoleValue = new javax.swing.JLabel();
                lblAccountNameValue = new javax.swing.JLabel();
                lblAccountEmailValue = new javax.swing.JLabel();
                lblAccountIdentifierValue = new javax.swing.JLabel();
                lblAccountContactValue = new javax.swing.JLabel();
                passwordPanel = new javax.swing.JPanel();
                lblChangePassword = new javax.swing.JLabel();
                lblCurrentPassword = new javax.swing.JLabel();
                lblNewPassword = new javax.swing.JLabel();
                lblConfirmPassword = new javax.swing.JLabel();
                txtCurrentPassword = new javax.swing.JPasswordField();
                txtNewPassword = new javax.swing.JPasswordField();
                txtConfirmPassword = new javax.swing.JPasswordField();
                btnChangePassword = new javax.swing.JButton();
                themePanel = new javax.swing.JPanel();
                lblThemeSection = new javax.swing.JLabel();
                lblTheme = new javax.swing.JLabel();
                cmbTheme = new javax.swing.JComboBox<>();
                btnApplyTheme = new javax.swing.JButton();

                setBackground(new java.awt.Color(255, 255, 255));

                accountPanel.setBackground(new java.awt.Color(255, 255, 255));
                accountPanel.setBorder(new com.group5.paul_esys.ui.PanelRoundBorder());

                lblAccountSection.setFont(new java.awt.Font("Poppins", 1, 20)); // NOI18N
                lblAccountSection.setText("Account Information");

                lblAccountId.setText("Account ID");

                lblAccountRole.setText("Role");

                lblAccountName.setText("Full Name");

                lblAccountEmail.setText("Email");

                lblAccountIdentifier.setText("School Identifier");

                lblAccountContact.setText("Contact Number");

                lblAccountIdValue.setFont(new java.awt.Font("Poppins", 1, 14)); // NOI18N
                lblAccountIdValue.setText("-");

                lblAccountRoleValue.setFont(new java.awt.Font("Poppins", 1, 14)); // NOI18N
                lblAccountRoleValue.setText("-");

                lblAccountNameValue.setFont(new java.awt.Font("Poppins", 1, 14)); // NOI18N
                lblAccountNameValue.setText("-");

                lblAccountEmailValue.setFont(new java.awt.Font("Poppins", 1, 14)); // NOI18N
                lblAccountEmailValue.setText("-");

                lblAccountIdentifierValue.setFont(new java.awt.Font("Poppins", 1, 14)); // NOI18N
                lblAccountIdentifierValue.setText("-");

                lblAccountContactValue.setFont(new java.awt.Font("Poppins", 1, 14)); // NOI18N
                lblAccountContactValue.setText("-");

                javax.swing.GroupLayout accountPanelLayout = new javax.swing.GroupLayout(accountPanel);
                accountPanel.setLayout(accountPanelLayout);
                accountPanelLayout.setHorizontalGroup(
                        accountPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(accountPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(accountPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(lblAccountSection)
                                        .addGroup(accountPanelLayout.createSequentialGroup()
                                                .addGroup(accountPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(lblAccountId)
                                                        .addComponent(lblAccountRole)
                                                        .addComponent(lblAccountName)
                                                        .addComponent(lblAccountEmail)
                                                        .addComponent(lblAccountIdentifier)
                                                        .addComponent(lblAccountContact))
                                                .addGap(40, 40, 40)
                                                .addGroup(accountPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(lblAccountIdValue)
                                                        .addComponent(lblAccountRoleValue)
                                                        .addComponent(lblAccountNameValue)
                                                        .addComponent(lblAccountEmailValue)
                                                        .addComponent(lblAccountIdentifierValue)
                                                        .addComponent(lblAccountContactValue))))
                                .addContainerGap(245, Short.MAX_VALUE))
                );
                accountPanelLayout.setVerticalGroup(
                        accountPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(accountPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lblAccountSection)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(accountPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblAccountId)
                                        .addComponent(lblAccountIdValue))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(accountPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblAccountRole)
                                        .addComponent(lblAccountRoleValue))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(accountPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblAccountName)
                                        .addComponent(lblAccountNameValue))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(accountPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblAccountEmail)
                                        .addComponent(lblAccountEmailValue))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(accountPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblAccountIdentifier)
                                        .addComponent(lblAccountIdentifierValue))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(accountPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblAccountContact)
                                        .addComponent(lblAccountContactValue))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );

                passwordPanel.setBackground(new java.awt.Color(255, 255, 255));
                passwordPanel.setBorder(new com.group5.paul_esys.ui.PanelRoundBorder());

                lblChangePassword.setFont(new java.awt.Font("Poppins", 1, 20)); // NOI18N
                lblChangePassword.setText("Change Password");

                lblCurrentPassword.setText("Current Password");

                lblNewPassword.setText("New Password");

                lblConfirmPassword.setText("Confirm Password");

                txtCurrentPassword.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtCurrentPassword.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                txtNewPassword.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtNewPassword.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                txtConfirmPassword.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                txtConfirmPassword.setBorder(new com.group5.paul_esys.ui.TextFieldRoundBorder());

                btnChangePassword.setBackground(new java.awt.Color(119, 0, 0));
                btnChangePassword.setForeground(new java.awt.Color(255, 255, 255));
                btnChangePassword.setText("Update Password");

                javax.swing.GroupLayout passwordPanelLayout = new javax.swing.GroupLayout(passwordPanel);
                passwordPanel.setLayout(passwordPanelLayout);
                passwordPanelLayout.setHorizontalGroup(
                        passwordPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(passwordPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(passwordPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(txtCurrentPassword)
                                        .addComponent(txtConfirmPassword)
                                        .addComponent(txtNewPassword)
                                        .addGroup(passwordPanelLayout.createSequentialGroup()
                                                .addGroup(passwordPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(lblChangePassword)
                                                        .addComponent(lblCurrentPassword)
                                                        .addComponent(lblNewPassword)
                                                        .addComponent(lblConfirmPassword))
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, passwordPanelLayout.createSequentialGroup()
                                                .addGap(0, 0, Short.MAX_VALUE)
                                                .addComponent(btnChangePassword)))
                                .addContainerGap())
                );
                passwordPanelLayout.setVerticalGroup(
                        passwordPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(passwordPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lblChangePassword)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblCurrentPassword)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtCurrentPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblNewPassword)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtNewPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblConfirmPassword)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtConfirmPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnChangePassword)
                                .addContainerGap())
                );

                themePanel.setBackground(new java.awt.Color(255, 255, 255));
                themePanel.setBorder(new com.group5.paul_esys.ui.PanelRoundBorder());

                lblThemeSection.setFont(new java.awt.Font("Poppins", 1, 20)); // NOI18N
                lblThemeSection.setText("Appearance & Themes");

                lblTheme.setText("Theme Preset");

                cmbTheme.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N

                btnApplyTheme.setText("Apply Theme");

                javax.swing.GroupLayout themePanelLayout = new javax.swing.GroupLayout(themePanel);
                themePanel.setLayout(themePanelLayout);
                themePanelLayout.setHorizontalGroup(
                        themePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(themePanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(themePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(lblThemeSection)
                                        .addComponent(lblTheme)
                                        .addComponent(cmbTheme, javax.swing.GroupLayout.PREFERRED_SIZE, 360, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnApplyTheme, javax.swing.GroupLayout.Alignment.TRAILING))
                                .addContainerGap(696, Short.MAX_VALUE))
                );
                themePanelLayout.setVerticalGroup(
                        themePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(themePanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lblThemeSection)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblTheme)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cmbTheme, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnApplyTheme)
                                .addContainerGap(270, Short.MAX_VALUE))
                );

                javax.swing.GroupLayout contentPanelLayout = new javax.swing.GroupLayout(contentPanel);
                contentPanel.setLayout(contentPanelLayout);
                contentPanelLayout.setHorizontalGroup(
                        contentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(contentPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(contentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(themePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(contentPanelLayout.createSequentialGroup()
                                                .addComponent(accountPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(passwordPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );
                contentPanelLayout.setVerticalGroup(
                        contentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(contentPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(contentPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(accountPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(passwordPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(themePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
                );

                javax.swing.GroupLayout rootPanelLayout = new javax.swing.GroupLayout(rootPanel);
                rootPanel.setLayout(rootPanelLayout);
                rootPanelLayout.setHorizontalGroup(
                        rootPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(rootPanelLayout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(contentPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                );
                rootPanelLayout.setVerticalGroup(
                        rootPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(rootPanelLayout.createSequentialGroup()
                                .addGap(0, 1, Short.MAX_VALUE)
                                .addComponent(contentPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 1, Short.MAX_VALUE))
                );

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
                this.setLayout(layout);
                layout.setHorizontalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(rootPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(rootPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                );
        }// </editor-fold>//GEN-END:initComponents

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JPanel accountPanel;
        private javax.swing.JButton btnApplyTheme;
        private javax.swing.JButton btnChangePassword;
        private javax.swing.JComboBox<com.group5.paul_esys.utils.ThemeOption> cmbTheme;
        private javax.swing.JPanel contentPanel;
        private javax.swing.JLabel lblAccountContact;
        private javax.swing.JLabel lblAccountContactValue;
        private javax.swing.JLabel lblAccountEmail;
        private javax.swing.JLabel lblAccountEmailValue;
        private javax.swing.JLabel lblAccountId;
        private javax.swing.JLabel lblAccountIdValue;
        private javax.swing.JLabel lblAccountIdentifier;
        private javax.swing.JLabel lblAccountIdentifierValue;
        private javax.swing.JLabel lblAccountName;
        private javax.swing.JLabel lblAccountNameValue;
        private javax.swing.JLabel lblAccountRole;
        private javax.swing.JLabel lblAccountRoleValue;
        private javax.swing.JLabel lblAccountSection;
        private javax.swing.JLabel lblChangePassword;
        private javax.swing.JLabel lblConfirmPassword;
        private javax.swing.JLabel lblCurrentPassword;
        private javax.swing.JLabel lblNewPassword;
        private javax.swing.JLabel lblTheme;
        private javax.swing.JLabel lblThemeSection;
        private javax.swing.JPanel passwordPanel;
        private javax.swing.JPanel rootPanel;
        private javax.swing.JPanel themePanel;
        private javax.swing.JPasswordField txtConfirmPassword;
        private javax.swing.JPasswordField txtCurrentPassword;
        private javax.swing.JPasswordField txtNewPassword;
        // End of variables declaration//GEN-END:variables
}
