/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JDialog.java to edit this template
 */
package com.group5.paul_esys.screens.registrar.forms;

import com.group5.paul_esys.modules.enrollment_period.model.EnrollmentPeriod;
import com.group5.paul_esys.modules.enrollment_period.services.EnrollmentPeriodService;
import com.group5.paul_esys.modules.enrollment_period.utils.EnrollmentPeriodUtils;
import com.group5.paul_esys.modules.offerings.model.Offering;
import com.group5.paul_esys.modules.offerings.services.OfferingService;
import com.group5.paul_esys.modules.sections.model.Section;
import com.group5.paul_esys.modules.sections.services.SectionService;
import com.group5.paul_esys.modules.subjects.model.Subject;
import com.group5.paul_esys.modules.subjects.services.SubjectService;
import com.group5.paul_esys.utils.FormValidationUtil;
import java.awt.Frame;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.swing.JOptionPane;

/**
 *
 * @author nytri
 */
public class OfferingForm extends javax.swing.JDialog {

    private final EnrollmentPeriodService enrollmentPeriodService = EnrollmentPeriodService.getInstance();
    private final SubjectService subjectService = SubjectService.getInstance();
    private final SectionService sectionService = SectionService.getInstance();
    private final OfferingService offeringService = OfferingService.getInstance();

    private final Map<String, Long> enrollmentPeriodIdByLabel = new LinkedHashMap<>();
    private final Map<String, Long> subjectIdByLabel = new LinkedHashMap<>();
    private final Map<String, Long> sectionIdByLabel = new LinkedHashMap<>();

    private final Long preselectedEnrollmentPeriodId;
    private final Runnable onSavedCallback;

    /**
     * Creates new form OfferingForm
     */
    public OfferingForm(Frame parent, boolean modal, Long preselectedEnrollmentPeriodId, Runnable onSavedCallback) {
        super(parent, modal);
        // Remove the system window decoration so only the custom WindowBar is visible.
        // Must be called before the dialog is realized (before initComponents()/pack()).
        setUndecorated(true);
        this.preselectedEnrollmentPeriodId = preselectedEnrollmentPeriodId;
        this.onSavedCallback = onSavedCallback;
        initComponents();
        loadEnrollmentPeriods();
        loadSubjects();
        loadSections();
        setMinimumSize(getSize());
        setLocationRelativeTo(parent);
    }

    private void loadEnrollmentPeriods() {
        cbxEnrollmentPeriod.removeAllItems();
        enrollmentPeriodIdByLabel.clear();

        List<EnrollmentPeriod> periods = enrollmentPeriodService.getAllEnrollmentPeriods();
        for (EnrollmentPeriod period : periods) {
            String label = buildEnrollmentPeriodLabel(period);
            cbxEnrollmentPeriod.addItem(label);
            enrollmentPeriodIdByLabel.put(label, period.getId());
        }

        selectEnrollmentPeriodById(preselectedEnrollmentPeriodId);
    }

    private void selectEnrollmentPeriodById(Long enrollmentPeriodId) {
        if (enrollmentPeriodId == null) {
            return;
        }

        for (Map.Entry<String, Long> entry : enrollmentPeriodIdByLabel.entrySet()) {
            if (enrollmentPeriodId.equals(entry.getValue())) {
                cbxEnrollmentPeriod.setSelectedItem(entry.getKey());
                return;
            }
        }
    }

    private String buildEnrollmentPeriodLabel(EnrollmentPeriod period) {
        String schoolYear = EnrollmentPeriodUtils.safeText(period.getSchoolYear(), "N/A");
        String semester = EnrollmentPeriodUtils.safeText(period.getSemester(), "N/A");
        String status = EnrollmentPeriodUtils.resolveStatus(period);
        return schoolYear + " | " + semester + " | " + status + " | ID " + period.getId();
    }

    private void loadSubjects() {
        cbxSubject.removeAllItems();
        subjectIdByLabel.clear();

        for (Subject subject : subjectService.getAllSubjects()) {
            if (subject.getId() == null) {
                continue;
            }
            String label = buildSubjectLabel(subject);
            cbxSubject.addItem(label);
            subjectIdByLabel.put(label, subject.getId());
        }
    }

    private String buildSubjectLabel(Subject subject) {
        String code = safeText(subject.getSubjectCode(), "NO-CODE");
        String name = safeText(subject.getSubjectName(), "Unnamed Subject");
        return code + " - " + name + " | ID " + subject.getId();
    }

    private void loadSections() {
        cbxSection.removeAllItems();
        sectionIdByLabel.clear();

        for (Section section : sectionService.getAllSections()) {
            if (section.getId() == null || isDissolved(section)) {
                continue;
            }
            String label = buildSectionLabel(section);
            cbxSection.addItem(label);
            sectionIdByLabel.put(label, section.getId());
        }
    }

    private boolean isDissolved(Section section) {
        String status = safeText(section.getStatus(), "OPEN");
        return "DISSOLVED".equalsIgnoreCase(status);
    }

    private String buildSectionLabel(Section section) {
        String code = safeText(section.getSectionCode(), "NO-CODE");
        String status = safeText(section.getStatus(), "OPEN").toUpperCase();
        String capacity = section.getCapacity() == null ? "N/A" : String.valueOf(section.getCapacity());
        return code + " | Status: " + status + " | Cap: " + capacity + " | ID " + section.getId();
    }

    private String safeText(String value, String fallback) {
        if (value == null) {
            return fallback;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }

    private void saveOffering() {
        Long enrollmentPeriodId = getSelectedEnrollmentPeriodId();
        Long subjectId = getSelectedSubjectId();
        Long sectionId = getSelectedSectionId();

        if (!isValidForm(enrollmentPeriodId, subjectId, sectionId)) {
            return;
        }

        Integer capacity = parseCapacity();
        if (capacity == INVALID_CAPACITY) {
            return;
        }

        if (offeringService.existsOffering(subjectId, sectionId, enrollmentPeriodId)) {
            JOptionPane.showMessageDialog(
                    this,
                    "This offering already exists for the selected subject, section, and enrollment period.",
                    "Duplicate Offering",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        Offering offering = new Offering()
                .setSubjectId(subjectId)
                .setSectionId(sectionId)
                .setEnrollmentPeriodId(enrollmentPeriodId)
                .setSemesterSubjectId(null)
                .setCapacity(capacity);

        if (!offeringService.createOffering(offering)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Failed to create offering. Please try again.",
                    "Create Offering",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        JOptionPane.showMessageDialog(
                this,
                "Offering created successfully.",
                "Create Offering",
                JOptionPane.INFORMATION_MESSAGE
        );

        if (onSavedCallback != null) {
            onSavedCallback.run();
        }

        dispose();
    }

    private static final Integer INVALID_CAPACITY = Integer.MIN_VALUE;

    private boolean isValidForm(Long enrollmentPeriodId, Long subjectId, Long sectionId) {
        if (showValidationError(FormValidationUtil.validateRequiredSelection("Enrollment period", cbxEnrollmentPeriod.getSelectedItem()))) {
            return false;
        }

        if (showValidationError(FormValidationUtil.validateRequiredSelection("Subject", cbxSubject.getSelectedItem()))) {
            return false;
        }

        if (showValidationError(FormValidationUtil.validateRequiredSelection("Section", cbxSection.getSelectedItem()))) {
            return false;
        }

        if (enrollmentPeriodId == null || subjectId == null || sectionId == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select valid enrollment period, subject, and section values.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE
            );
            return false;
        }

        return true;
    }

    private Integer parseCapacity() {
        String raw = FormValidationUtil.normalizeOptionalText(txtCapacity.getText());
        if (raw == null) {
            return null;
        }

        Optional<String> rangeValidation = FormValidationUtil.validateIntegerTextRange(
            "Capacity",
            raw,
            1,
            (int) FormValidationUtil.LARGE_NUMBER_LIMIT,
            true
        );
        if (showValidationError(rangeValidation)) {
            return INVALID_CAPACITY;
        }

        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Capacity must be a positive whole number.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE
            );
            return INVALID_CAPACITY;
        }
    }

    private boolean showValidationError(Optional<String> validationError) {
        if (validationError.isEmpty()) {
            return false;
        }

        JOptionPane.showMessageDialog(
                this,
                validationError.get(),
                "Validation Error",
                JOptionPane.WARNING_MESSAGE
        );
        return true;
    }

    private Long getSelectedEnrollmentPeriodId() {
        Object selectedItem = cbxEnrollmentPeriod.getSelectedItem();
        if (selectedItem == null) {
            return null;
        }
        return enrollmentPeriodIdByLabel.get(selectedItem.toString());
    }

    private Long getSelectedSubjectId() {
        Object selectedItem = cbxSubject.getSelectedItem();
        if (selectedItem == null) {
            return null;
        }
        return subjectIdByLabel.get(selectedItem.toString());
    }

    private Long getSelectedSectionId() {
        Object selectedItem = cbxSection.getSelectedItem();
        if (selectedItem == null) {
            return null;
        }
        return sectionIdByLabel.get(selectedItem.toString());
    }

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {
        dispose();
    }

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {
        saveOffering();
    }

    /**
     * This method is called from within the constructor to initialize the
     * form. WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                formPanel = new javax.swing.JPanel();
                jLabel1 = new javax.swing.JLabel();
                jLabel2 = new javax.swing.JLabel();
                jLabel3 = new javax.swing.JLabel();
                cbxEnrollmentPeriod = new javax.swing.JComboBox<>();
                jLabel4 = new javax.swing.JLabel();
                cbxSubject = new javax.swing.JComboBox<>();
                jLabel5 = new javax.swing.JLabel();
                cbxSection = new javax.swing.JComboBox<>();
                jLabel6 = new javax.swing.JLabel();
                txtCapacity = new javax.swing.JTextField();
                btnCancel = new javax.swing.JButton();
                btnSave = new javax.swing.JButton();
                windowBar1 = new com.group5.paul_esys.components.WindowBar();

                setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
                setTitle("Create Offering");

                formPanel.setBackground(new java.awt.Color(255, 255, 255));
                formPanel.setPreferredSize(new java.awt.Dimension(422, 270));

                jLabel1.setFont(new java.awt.Font("Poppins", 0, 18)); // NOI18N
                jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel1.setText("Create Offering");

                jLabel2.setFont(new java.awt.Font("Poppins", 0, 12)); // NOI18N
                jLabel2.setForeground(new java.awt.Color(102, 102, 102));
                jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                jLabel2.setText("Assign a subject to a section and enrollment period");

                jLabel3.setText("Enrollment Period");

                cbxEnrollmentPeriod.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "" }));

                jLabel4.setText("Subject");

                cbxSubject.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "" }));

                jLabel5.setText("Section");

                cbxSection.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "" }));

                jLabel6.setText("Capacity (Optional)");

                btnCancel.setBackground(new java.awt.Color(119, 0, 0));
                btnCancel.setForeground(new java.awt.Color(255, 255, 255));
                btnCancel.setText("Cancel");
                btnCancel.addActionListener(this::btnCancelActionPerformed);

                btnSave.setBackground(new java.awt.Color(119, 0, 0));
                btnSave.setForeground(new java.awt.Color(255, 255, 255));
                btnSave.setText("Save");
                btnSave.addActionListener(this::btnSaveActionPerformed);

                javax.swing.GroupLayout formPanelLayout = new javax.swing.GroupLayout(formPanel);
                formPanel.setLayout(formPanelLayout);
                formPanelLayout.setHorizontalGroup(
                        formPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(formPanelLayout.createSequentialGroup()
                                .addGap(41, 41, 41)
                                .addGroup(formPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 320, Short.MAX_VALUE)
                                        .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(cbxEnrollmentPeriod, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(cbxSubject, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(cbxSection, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(txtCapacity)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, formPanelLayout.createSequentialGroup()
                                                .addGap(0, 0, Short.MAX_VALUE)
                                                .addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(10, 10, 10)
                                                .addComponent(btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGap(43, 43, 43))
                        .addComponent(windowBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                );
                formPanelLayout.setVerticalGroup(
                        formPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, formPanelLayout.createSequentialGroup()
                                .addComponent(windowBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 32, Short.MAX_VALUE)
                                .addComponent(jLabel1)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel2)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cbxEnrollmentPeriod, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(10, 10, 10)
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cbxSubject, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(10, 10, 10)
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cbxSection, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(10, 10, 10)
                                .addComponent(jLabel6)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtCapacity, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addGroup(formPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(25, 25, 25))
                );

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
                getContentPane().setLayout(layout);
                layout.setHorizontalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(formPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 404, javax.swing.GroupLayout.PREFERRED_SIZE)
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(formPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 496, Short.MAX_VALUE)
                );

                pack();
        }// </editor-fold>//GEN-END:initComponents

        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton btnCancel;
        private javax.swing.JButton btnSave;
        private javax.swing.JComboBox<String> cbxEnrollmentPeriod;
        private javax.swing.JComboBox<String> cbxSection;
        private javax.swing.JComboBox<String> cbxSubject;
        private javax.swing.JPanel formPanel;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JLabel jLabel3;
        private javax.swing.JLabel jLabel4;
        private javax.swing.JLabel jLabel5;
        private javax.swing.JLabel jLabel6;
        private javax.swing.JTextField txtCapacity;
        private com.group5.paul_esys.components.WindowBar windowBar1;
        // End of variables declaration//GEN-END:variables
}
