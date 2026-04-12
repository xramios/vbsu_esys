/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package com.group5.paul_esys.screens.registrar.panels;

import com.group5.paul_esys.modules.courses.model.Course;
import com.group5.paul_esys.modules.courses.services.CourseService;
import com.group5.paul_esys.modules.curriculum.model.Curriculum;
import com.group5.paul_esys.modules.curriculum.services.CurriculumService;
import com.group5.paul_esys.screens.registrar.cards.CurriculumCard;
import com.group5.paul_esys.screens.registrar.forms.CurriculumForm;
import com.group5.paul_esys.screens.registrar.forms.SemesterForm;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

/**
 *
 * @author nytri
 */
public class RegistrarCurriculumManagement extends javax.swing.JPanel {

        private final CurriculumService curriculumService = CurriculumService.getInstance();
        private final CourseService courseService = CourseService.getInstance();

        private final Map<Long, String> courseNameById = new LinkedHashMap<>();
        private final Map<Integer, CurriculumCard> curriculumCardsByTabIndex = new LinkedHashMap<>();
        private boolean isRefreshingTabs;

	/**
	 * Creates new form CurriculumManagement
	 */
	public RegistrarCurriculumManagement() {
		initComponents();
                initializeCurriculumManagement();
	}

        private void initializeCurriculumManagement() {
                tabbedPaneCurriculums.addChangeListener(evt -> loadSemestersForSelectedCurriculumIfNeeded());
                tabbedPaneCurriculums.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                                loadSemestersForSelectedCurriculumIfNeeded();
                        }
                });

                reloadCurriculumTabs();
        }

        private void reloadCurriculumTabs() {
                new SwingWorker<List<Curriculum>, Void>() {
                        @Override
                        protected List<Curriculum> doInBackground() throws Exception {
                                loadCourseLookup();
                                return curriculumService.getAllCurriculums();
                        }

                        @Override
                        protected void done() {
                                int selectedIndex = tabbedPaneCurriculums.getSelectedIndex();
                                if (selectedIndex < 0) {
                                        selectedIndex = 0;
                                }

                                isRefreshingTabs = true;
                                try {
                                        tabbedPaneCurriculums.removeAll();
                                        curriculumCardsByTabIndex.clear();

                                        List<Curriculum> curriculums = get();
                                        if (curriculums.isEmpty()) {
                                                showEmptyStateTab("No curriculums found. Click Add Curriculum to create one.");
                                                return;
                                        }

                                        for (Curriculum curriculum : curriculums) {
                                                CurriculumCard curriculumCard = new CurriculumCard(
                                                        curriculum,
                                                        courseNameById.get(curriculum.getCourse())
                                                );

                                                tabbedPaneCurriculums.addTab(buildCurriculumTabTitle(curriculum), curriculumCard);
                                                curriculumCardsByTabIndex.put(tabbedPaneCurriculums.getTabCount() - 1, curriculumCard);
                                        }

                                        if (selectedIndex >= tabbedPaneCurriculums.getTabCount()) {
                                                selectedIndex = 0;
                                        }

                                        if (tabbedPaneCurriculums.getTabCount() > 0) {
                                                tabbedPaneCurriculums.setSelectedIndex(selectedIndex);
                                        }
                                } catch (Exception ex) {
                                        showEmptyStateTab("Error loading curriculums: " + ex.getMessage());
                                } finally {
                                        isRefreshingTabs = false;
                                }
                        }
                }.execute();
        }

        private void loadCourseLookup() {
                courseNameById.clear();
                List<Course> courses = courseService.getAllCourses();
                for (Course course : courses) {
                        courseNameById.put(course.getId(), course.getCourseName());
                }
        }

        private void showEmptyStateTab(String message) {
                JPanel panel = new JPanel(new java.awt.GridBagLayout());
                panel.setBackground(java.awt.Color.WHITE);
                JLabel label = new JLabel(message);
                label.setForeground(new java.awt.Color(120, 120, 120));
                panel.add(label);
                tabbedPaneCurriculums.addTab("Curriculums", panel);
        }

        private String buildCurriculumTabTitle(Curriculum curriculum) {
                String baseName = safeText(curriculum.getName(), "Curriculum " + curriculum.getId());
                if (curriculum.getCurYear() == null) {
                        return baseName;
                }

                return baseName + " (" + formatYear(curriculum.getCurYear()) + ")";
        }

        private String formatYear(Date date) {
                if (date instanceof java.sql.Date sqlDate) {
                        return String.valueOf(sqlDate.toLocalDate().getYear());
                }

                return String.valueOf(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().getYear());
        }

        private String safeText(String value, String fallback) {
                if (value == null || value.trim().isEmpty()) {
                        return fallback;
                }
                return value.trim();
        }

        private CurriculumCard getSelectedCurriculumCard() {
                int selectedIndex = tabbedPaneCurriculums.getSelectedIndex();
                if (selectedIndex < 0) {
                        return null;
                }

                return curriculumCardsByTabIndex.get(selectedIndex);
        }

        private void loadSemestersForSelectedCurriculumIfNeeded() {
                if (isRefreshingTabs) {
                        return;
                }

                CurriculumCard selectedCard = getSelectedCurriculumCard();
                if (selectedCard != null) {
                        selectedCard.ensureSemestersLoaded();
                }
        }

	/**
	 * This method is called from within the constructor to initialize the
	 * form. WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                tabbedPaneCurriculums = new javax.swing.JTabbedPane();
                jLabel1 = new javax.swing.JLabel();
                jLabel2 = new javax.swing.JLabel();
                btnAddSemester = new javax.swing.JButton();
                btnddCurriculum = new javax.swing.JButton();

                setBackground(new java.awt.Color(255, 255, 255));
                setMaximumSize(new java.awt.Dimension(1181, 684));
                setPreferredSize(new java.awt.Dimension(1181, 684));

                tabbedPaneCurriculums.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
                tabbedPaneCurriculums.setTabPlacement(javax.swing.JTabbedPane.LEFT);

                jLabel1.setFont(new java.awt.Font("Poppins", 0, 24)); // NOI18N
                jLabel1.setText("Curriculum Management");

                jLabel2.setFont(new java.awt.Font("Poppins", 0, 14)); // NOI18N
                jLabel2.setForeground(new java.awt.Color(153, 153, 153));
                jLabel2.setText("Manage Curriculum, Semesters, Semester Subjects efficiently");

                btnAddSemester.setBackground(new java.awt.Color(119, 0, 0));
                btnAddSemester.setForeground(new java.awt.Color(255, 255, 255));
                btnAddSemester.setText("Add Semester");
                btnAddSemester.addActionListener(this::btnAddSemesterActionPerformed);

                btnddCurriculum.setBackground(new java.awt.Color(119, 0, 0));
                btnddCurriculum.setForeground(new java.awt.Color(255, 255, 255));
                btnddCurriculum.setText("Add Curriculum");
                btnddCurriculum.addActionListener(this::btnddCurriculumActionPerformed);

                javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
                this.setLayout(layout);
                layout.setHorizontalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(tabbedPaneCurriculums)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jLabel2)
                                                        .addComponent(jLabel1))
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 437, Short.MAX_VALUE)
                                                .addComponent(btnddCurriculum, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(btnAddSemester, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap())
                );
                layout.setVerticalGroup(
                        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                                .addComponent(jLabel1)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jLabel2))
                                        .addComponent(btnddCurriculum, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnAddSemester, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(tabbedPaneCurriculums, javax.swing.GroupLayout.DEFAULT_SIZE, 595, Short.MAX_VALUE)
                                .addContainerGap())
                );
        }// </editor-fold>//GEN-END:initComponents

        private void btnddCurriculumActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnddCurriculumActionPerformed
                CurriculumForm form = new CurriculumForm(null, this::reloadCurriculumTabs);
                form.setVisible(true);
        }//GEN-LAST:event_btnddCurriculumActionPerformed

        private void btnAddSemesterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddSemesterActionPerformed
                CurriculumCard selectedCard = getSelectedCurriculumCard();
                boolean shouldReloadSelectedCard = selectedCard != null && selectedCard.isSemestersLoaded();
                int selectedIndex = tabbedPaneCurriculums.getSelectedIndex();

                SemesterForm form = new SemesterForm(null, () -> {
                        reloadCurriculumTabs();
                        if (!shouldReloadSelectedCard || selectedIndex < 0) {
                                return;
                        }

                        CurriculumCard refreshedCard = curriculumCardsByTabIndex.get(selectedIndex);
                        if (refreshedCard != null) {
                                refreshedCard.reloadSemesters();
                        }
                });
                form.setVisible(true);
        }//GEN-LAST:event_btnAddSemesterActionPerformed


        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JButton btnAddSemester;
        private javax.swing.JButton btnddCurriculum;
        private javax.swing.JLabel jLabel1;
        private javax.swing.JLabel jLabel2;
        private javax.swing.JTabbedPane tabbedPaneCurriculums;
        // End of variables declaration//GEN-END:variables
}
