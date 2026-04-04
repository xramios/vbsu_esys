# Registrar Dashboard UI Task Plan

## 1. Purpose and Scope
This document defines the frontend/UI work breakdown for the **Registrar Dashboard** of the Enrollment System. It focuses **only on Java Swing UI design and implementation** using **NetBeans**, with no backend/DAO/database logic included.

### In Scope
- Dashboard shell and navigation
- Entity management UI for:
  - Students
  - Sections
  - Subjects
  - Curriculum
  - Rooms
  - Schedules
- Shared frontend components
- Validation and user feedback
- Search, filter, and table interaction patterns
- Cross-module UI for schedule conflict awareness

### Out of Scope
- DAO layer
- SQL queries
- Business rules implementation beyond UI validation hints
- Authentication logic
- Reporting/export implementation unless purely visual and non-data-driven

---

## 2. UI Architecture Overview

### 2.1 Global Dashboard Shell
- [ ] 2.1.1 Design the main registrar dashboard frame
  - [ ] Create the root `JFrame` for the registrar home screen.
  - [ ] Define a consistent application title bar and brand area.
  - [ ] Reserve space for user identity and session actions on the top header.
  - [ ] Standardize the default window size, minimum size, and resize behavior.

- [ ] 2.1.2 Build the navigation shell
  - [ ] Implement a collapsible left sidebar using `JPanel`.
  - [ ] Add navigation buttons for Students, Sections, Subjects, Curriculum, Rooms, and Schedules.
  - [ ] Use consistent icon + label button styling for all sidebar actions.
  - [ ] Add active-state highlighting for the selected module.
  - [ ] Include a logout or exit action in the navigation or header area.

- [ ] 2.1.3 Build the content container
  - [ ] Use a central container panel for module swapping.
  - [ ] Implement `CardLayout` for switching between module panels.
  - [ ] Define a default landing panel with summary cards or welcome text.
  - [ ] Ensure the content area refreshes correctly when navigation changes.

- [ ] 2.1.4 Define shared visual standards
  - [ ] Create a common color palette for header, sidebar, panels, and primary actions.
  - [ ] Establish typography rules for headings, labels, table headers, and helper text.
  - [ ] Create reusable button styles for primary, secondary, danger, and neutral actions.
  - [ ] Standardize spacing, padding, border radius look, and panel grouping.

- [ ] 2.1.5 Add global UI utility components
  - [ ] Create reusable empty-state panels for no records found.
  - [ ] Create loading/processing overlay placeholders for future backend integration.
  - [ ] Create reusable confirmation dialog templates for delete and discard actions.
  - [ ] Create reusable success and error notification panels or toast-like popups.

**Estimated UI Complexity:** `High`

---

## 3. Shared UI Patterns for All Modules

### 3.1 Standard Module Layout Pattern
- [ ] 3.1.1 Define a shared module page template
  - [ ] Place module title, subtitle, and action bar at the top.
  - [ ] Position search/filter controls above the table region.
  - [ ] Reserve the main body for `JTable` and record preview or form panel.
  - [ ] Add footer spacing or status strip for row count and hints.

- [ ] 3.1.2 Standardize action controls
  - [ ] Add New, Edit, Delete, Refresh, and View actions where applicable.
  - [ ] Use consistent button placement and order across all modules.
  - [ ] Add keyboard mnemonic support for primary actions.
  - [ ] Add tooltips for each action button.

### 3.2 Shared Table Design
- [ ] 3.2.1 Design common data table styling
  - [ ] Use a uniform `JTable` style with alternating row colors.
  - [ ] Enable row selection highlight and hover readability.
  - [ ] Set non-editable table cells for list views.
  - [ ] Define column width rules and alignment for IDs, names, and status fields.

- [ ] 3.2.2 Add custom table renderers
  - [ ] Render status values with colored badges or labels.
  - [ ] Align numeric columns to the right and dates to the center.
  - [ ] Provide compact action-column rendering where needed.
  - [ ] Highlight warnings such as full capacity or conflicting entries visually.

- [ ] 3.2.3 Add table interaction patterns
  - [ ] Support row double-click to open edit or details view.
  - [ ] Support row selection syncing with preview or form panels.
  - [ ] Add pagination placeholder or row count footer if needed.
  - [ ] Add empty table messaging when no records exist.

### 3.3 Shared Form Design
- [ ] 3.3.1 Define form container behavior
  - [ ] Use `JDialog` for create/edit modal forms where appropriate.
  - [ ] Use `JPanel` embedded forms for side-by-side detail editing when appropriate.
  - [ ] Standardize labeled input spacing and alignment.
  - [ ] Group related fields visually with titled borders.

- [ ] 3.3.2 Define validation presentation
  - [ ] Add inline error labels below invalid inputs.
  - [ ] Add field-level tooltips for expected formats and limits.
  - [ ] Highlight invalid fields with border color changes.
  - [ ] Disable save action until required fields are valid where appropriate.

**Estimated UI Complexity:** `Medium`

---

## 4. Students Management UI

### 4.1 Students Main Panel
- [ ] 4.1.1 Design `StudentsPanel` main view
  - [ ] Create a title section for student management.
  - [ ] Add primary actions for Add Student, Edit Student, Delete Student, and Refresh.
  - [ ] Allocate a data table area for student records.
  - [ ] Add a record summary area showing total count and selected row details.

- [ ] 4.1.2 Define the student table view
  - [ ] Create a `JTable` listing student ID, full name, course, year level, status, and birthdate.
  - [ ] Use a custom renderer for status values such as REGULAR and IRREGULAR.
  - [ ] Format the birthdate column for readability.
  - [ ] Keep the student ID and internal key fields non-editable and visually compact.

- [ ] 4.1.3 Add table search and filtering
  - [ ] Add a search bar for student ID and name lookup.
  - [ ] Add dropdown filters for course, year level, and status.
  - [ ] Support live row filtering behavior in the UI.
  - [ ] Add a clear filters action.

### 4.2 Student Entry/Edit Dialog
- [ ] 4.2.1 Design `StudentEntryDialog`
  - [ ] Use a modal `JDialog` for add and edit workflows.
  - [ ] Organize fields into logical groups: personal info and academic info.
  - [ ] Include First Name, Last Name, Middle Name, Birthdate, Student Status, Course, and Year Level.
  - [ ] Include read-only student ID display when editing existing records.

- [ ] 4.2.2 Design student input controls
  - [ ] Use `JTextField` inputs for names and student ID.
  - [ ] Use a date picker or date input component placeholder for birthdate.
  - [ ] Use `JComboBox` controls for course, status, and year level.
  - [ ] Use consistent tab order and focus behavior.

- [ ] 4.2.3 Add action buttons and dialog behavior
  - [ ] Add Save, Cancel, and Reset buttons.
  - [ ] Use mnemonic keys for primary actions.
  - [ ] Close the dialog on cancel or successful save action.
  - [ ] Prevent accidental data loss with a discard confirmation.

### 4.3 Student Validation UI
- [ ] 4.3.1 Design validation messaging
  - [ ] Add inline labels for required first name, last name, and birthdate fields.
  - [ ] Show format guidance for fields with constraints.
  - [ ] Highlight empty or invalid inputs immediately.
  - [ ] Reserve space for validation messages to prevent layout shifting.

- [ ] 4.3.2 Define student-specific UI feedback
  - [ ] Show status-specific visual markers for REGULAR and IRREGULAR values.
  - [ ] Display warnings when year level is outside expected range.
  - [ ] Provide helper text for course selection and student ID handling.

**Estimated UI Complexity:** `High`

---

## 5. Sections Management UI

### 5.1 Sections Main Panel
- [ ] 5.1.1 Design `SectionsPanel` main view
  - [ ] Create a title section for section management.
  - [ ] Add actions for New Section, Edit Section, Delete Section, and Refresh.
  - [ ] Place table and filter controls in a balanced split layout.
  - [ ] Add a section detail preview area for selected rows.

- [ ] 5.1.2 Define the section table view
  - [ ] Create a `JTable` listing section code, section name, subject, and capacity.
  - [ ] Add a capacity status indicator for near-full or full sections.
  - [ ] Use compact column widths for code and numeric fields.
  - [ ] Highlight rows that may require attention due to capacity limits.

- [ ] 5.1.3 Add section search and filtering
  - [ ] Add text search for section code and section name.
  - [ ] Add subject dropdown filter for section grouping.
  - [ ] Add capacity-based filter states such as available, near-full, and full.
  - [ ] Include a reset filters action.

### 5.2 Section Entry/Edit Dialog
- [ ] 5.2.1 Design `SectionEntryDialog`
  - [ ] Use a modal `JDialog` with a single-column form or two-column grid.
  - [ ] Include Section Code, Section Name, Subject, and Capacity fields.
  - [ ] Use a subject selector control with searchable behavior if available.
  - [ ] Display the dialog title dynamically for add versus edit mode.

- [ ] 5.2.2 Define input and action behavior
  - [ ] Use `JTextField` for section code and section name.
  - [ ] Use `JComboBox` for subject selection.
  - [ ] Use numeric input handling for capacity.
  - [ ] Add Save and Cancel buttons with consistent styling.

### 5.3 Section Validation UI
- [ ] 5.3.1 Add validation feedback for section fields
  - [ ] Require section code, section name, subject, and capacity.
  - [ ] Show capacity format and minimum value guidance.
  - [ ] Display inline error labels below each field.
  - [ ] Prevent save action when required fields are incomplete.

**Estimated UI Complexity:** `Medium`

---

## 6. Subjects Management UI

### 6.1 Subjects Main Panel
- [ ] 6.1.1 Design `SubjectsPanel` main view
  - [ ] Create a title and summary area for subject management.
  - [ ] Add actions for Add Subject, Edit Subject, Delete Subject, and Refresh.
  - [ ] Define a searchable, sortable table region.
  - [ ] Include a small details panel for selected subject metadata.

- [ ] 6.1.2 Define the subject table view
  - [ ] Create a `JTable` listing subject code, subject name, units, curriculum, and department.
  - [ ] Use alignment rules for units and ID-related columns.
  - [ ] Render long descriptions as truncated preview text or tooltip content.
  - [ ] Support row selection for preview synchronization.

- [ ] 6.1.3 Add search and filter controls
  - [ ] Add a search bar for subject code and subject name.
  - [ ] Add dropdown filters for curriculum and department.
  - [ ] Add a units filter for common subject ranges.
  - [ ] Include a clear search/filter action.

### 6.2 Subject Entry/Edit Dialog
- [ ] 6.2.1 Design `SubjectEntryDialog`
  - [ ] Use a modal dialog with well-grouped academic and classification fields.
  - [ ] Include Subject Code, Subject Name, Units, Curriculum, Department, and Description.
  - [ ] Provide a larger text area for description entry.
  - [ ] Support both create and edit states with the same dialog.

- [ ] 6.2.2 Define subject input controls
  - [ ] Use `JTextField` inputs for subject code and name.
  - [ ] Use a numeric field pattern for units.
  - [ ] Use `JComboBox` selectors for curriculum and department.
  - [ ] Use `JTextArea` with scroll pane for description.

- [ ] 6.2.3 Add action and layout details
  - [ ] Use `GridBagLayout` or `GroupLayout` for clean label alignment.
  - [ ] Add Save, Cancel, and Reset actions.
  - [ ] Keep the description section visually separated from short inputs.

### 6.3 Subject Validation UI
- [ ] 6.3.1 Add validation indicators
  - [ ] Require code, name, and units fields.
  - [ ] Show numeric format hint for units.
  - [ ] Highlight invalid selection fields when required.
  - [ ] Provide helper tooltips for curriculum and department selectors.

**Estimated UI Complexity:** `High`

---

## 7. Curriculum Management UI

### 7.1 Curriculum Main Panel
- [ ] 7.1.1 Design `CurriculumPanel` main view
  - [ ] Create a title section describing program curriculum maintenance.
  - [ ] Add actions for New Curriculum, Edit Curriculum, Delete Curriculum, and Refresh.
  - [ ] Set up a master-detail layout for curriculum records and semester composition.
  - [ ] Include a summary strip showing selected course and academic year.

- [ ] 7.1.2 Define the curriculum table view
  - [ ] Create a `JTable` listing curriculum year, course, and last updated information.
  - [ ] Add row selection behavior that drives the detail panel below or beside it.
  - [ ] Use clear visual differentiation for the currently selected curriculum.
  - [ ] Keep the table compact and readable for admin workflows.

- [ ] 7.1.3 Add search and filter controls
  - [ ] Add a search bar for curriculum year and course name.
  - [ ] Add course dropdown filtering.
  - [ ] Add quick filters for recent or older curriculum entries if useful.
  - [ ] Include a reset option.

### 7.2 Curriculum Entry/Edit Dialog
- [ ] 7.2.1 Design `CurriculumEntryDialog`
  - [ ] Use a modal `JDialog` for curriculum create/edit workflows.
  - [ ] Include Curriculum Year and Course fields.
  - [ ] Add a concise layout because the form is small.
  - [ ] Keep dialog controls aligned and centered for clarity.

- [ ] 7.2.2 Define curriculum inputs and actions
  - [ ] Use a date picker or year-based input placeholder for curriculum year.
  - [ ] Use `JComboBox` for course selection.
  - [ ] Add Save and Cancel buttons.
  - [ ] Support keyboard navigation with tab ordering.

### 7.3 Curriculum Composition UI
- [ ] 7.3.1 Build semester composition panel
  - [ ] Add a nested section for semesters attached to the selected curriculum.
  - [ ] Use tabs or an accordion-like grouped panel per semester.
  - [ ] Include controls to add, edit, or remove semester entries visually.
  - [ ] Display semester labels clearly with course context.

- [ ] 7.3.2 Build subject assignment panel
  - [ ] Create a subject list for the selected semester.
  - [ ] Add UI for moving subjects into and out of a semester.
  - [ ] Display year level assignment beside each subject.
  - [ ] Use a dual-list or table-based interaction model.

### 7.4 Curriculum Validation UI
- [ ] 7.4.1 Add minimal validation prompts
  - [ ] Require curriculum year and course fields.
  - [ ] Display simple helper text for year format expectations.
  - [ ] Prevent blank saves with inline feedback.

**Estimated UI Complexity:** `Very High`

---

## 8. Rooms Management UI

### 8.1 Rooms Main Panel
- [ ] 8.1.1 Design `RoomsPanel` main view
  - [ ] Create a title section for room management.
  - [ ] Add actions for Add Room, Edit Room, Delete Room, and Refresh.
  - [ ] Allocate space for a room list table and selected room details.
  - [ ] Ensure layout supports quick scanning of available spaces.

- [ ] 8.1.2 Define the room table view
  - [ ] Create a `JTable` listing room name and capacity.
  - [ ] Add visual emphasis for low-capacity or high-usage rooms if shown later.
  - [ ] Keep the table minimal and highly readable.
  - [ ] Use numeric alignment for capacity values.

- [ ] 8.1.3 Add search and filtering
  - [ ] Add search by room name.
  - [ ] Add capacity threshold filtering UI.
  - [ ] Add a reset filters control.
  - [ ] Support live filtering in the table region.

### 8.2 Room Entry/Edit Dialog
- [ ] 8.2.1 Design `RoomEntryDialog`
  - [ ] Use a modal `JDialog` for room create/edit workflow.
  - [ ] Include Room Name and Capacity input fields.
  - [ ] Keep the form compact and direct.
  - [ ] Use clear button spacing and logical focus order.

- [ ] 8.2.2 Define room input controls
  - [ ] Use `JTextField` for room name.
  - [ ] Use numeric input styling or formatted text field for capacity.
  - [ ] Add Save, Cancel, and Reset buttons.
  - [ ] Provide a simple form footer with validation guidance.

### 8.3 Room Validation UI
- [ ] 8.3.1 Add inline validation
  - [ ] Require room name and capacity.
  - [ ] Show capacity numeric format hint.
  - [ ] Add visual error states for empty or invalid inputs.
  - [ ] Reserve space for field-specific messages.

**Estimated UI Complexity:** `Low`

---

## 9. Schedules Management UI

### 9.1 Scheduling Main Panel
- [ ] 9.1.1 Design `SchedulesPanel` main view
  - [ ] Create a title section explaining class scheduling operations.
  - [ ] Add actions for New Schedule, Edit Schedule, Delete Schedule, and Refresh.
  - [ ] Use a master-detail layout to show schedule list and selected schedule details.
  - [ ] Include a schedule summary widget for day, time, room, and faculty.

- [ ] 9.1.2 Define the schedule table view
  - [ ] Create a `JTable` listing section, room, faculty, day, start time, end time, and enrollment period.
  - [ ] Use custom renderers for conflict warnings and day labels.
  - [ ] Display time values in a readable format.
  - [ ] Highlight schedules with potential overlaps visually.

- [ ] 9.1.3 Add search and filtering
  - [ ] Add search by section, room, or faculty name.
  - [ ] Add filters for day of week and enrollment period.
  - [ ] Add time range filtering UI if feasible in the frontend.
  - [ ] Provide a clear filter action and feedback on active filters.

### 9.2 Schedule Entry/Edit Dialog
- [ ] 9.2.1 Design `ScheduleEntryDialog`
  - [ ] Use a modal `JDialog` with grouped fields for section assignment, room assignment, faculty assignment, and timing.
  - [ ] Include Section, Room, Faculty, Day, Start Time, End Time, and Enrollment Period fields.
  - [ ] Lay out fields in a two-column form for faster input.
  - [ ] Make the dialog clearly distinct because scheduling is a high-risk workflow.

- [ ] 9.2.2 Define scheduling input controls
  - [ ] Use `JComboBox` for section, room, faculty, day, and enrollment period.
  - [ ] Use time input controls or formatted text fields for start and end time.
  - [ ] Place timing fields next to each other for easier comparison.
  - [ ] Include Save, Cancel, and Reset buttons.

### 9.3 Schedule Validation UI
- [ ] 9.3.1 Add validation messages
  - [ ] Require all schedule fields to be selected.
  - [ ] Show inline warnings if start time is not before end time.
  - [ ] Reserve display space for timing and selection errors.
  - [ ] Use tooltips to clarify time format and day selection.

### 9.4 Schedule Conflict Awareness UI
- [ ] 9.4.1 Create a conflict warning panel
  - [ ] Add a visible warning panel when a schedule conflict is detected or suspected.
  - [ ] Design the panel to show conflicting section, room, faculty, and time details.
  - [ ] Use color coding to indicate severity levels.
  - [ ] Make the warning non-blocking but clearly noticeable.

- [ ] 9.4.2 Create a conflict visualizer component
  - [ ] Add a timeline-style or comparison panel for overlapping schedule ranges.
  - [ ] Display conflict candidates side by side for quick review.
  - [ ] Highlight overlapping day and time segments.
  - [ ] Keep the component reusable for future scheduling workflows.

**Estimated UI Complexity:** `Very High`

---

## 10. Cross-Module UI

### 10.1 Shared Search and Reference Selectors
- [ ] 10.1.1 Design reusable lookup selector components
  - [ ] Create a searchable combo box or selection dialog pattern for linked entities.
  - [ ] Support references for courses, departments, subjects, sections, rooms, and faculty.
  - [ ] Make selected values visually clear and easy to replace.
  - [ ] Add placeholder text for empty selections.

### 10.2 Cross-Module Detail Preview
- [ ] 10.2.1 Build a reusable details preview panel
  - [ ] Show selected record summary without opening edit mode.
  - [ ] Use label/value rows with a compact and readable arrangement.
  - [ ] Support entity-specific content templates.
  - [ ] Make the preview usable in students, sections, subjects, curriculum, rooms, and schedules.

### 10.3 Schedule Conflict Visualizer
- [ ] 10.3.1 Design a shared conflict visualization widget
  - [ ] Present overlapping schedule information in a structured card or timeline format.
  - [ ] Add severity styling for warning and critical conflict conditions.
  - [ ] Make it reusable inside scheduling dialog and schedule panel.
  - [ ] Keep the widget UI-only and independent from conflict detection logic.

### 10.4 Global Notifications
- [ ] 10.4.1 Standardize notification styles
  - [ ] Create success, warning, and error notification visuals.
  - [ ] Use consistent icons and color codes across modules.
  - [ ] Add small dismiss controls where appropriate.
  - [ ] Ensure notifications do not block primary workflows unnecessarily.

**Estimated UI Complexity:** `Medium`

---

## 11. Navigation and Workflow Priorities

### 11.1 Recommended Build Order
- [ ] 11.1.1 Build the dashboard shell first
  - [ ] Implement the main frame, sidebar, header, and content swapping container.
  - [ ] Verify that module panels can be switched without layout breakage.

- [ ] 11.1.2 Build lookup and dependency modules next
  - [ ] Implement Rooms before Schedules because schedules depend on room selection.
  - [ ] Implement Subjects before Sections because sections reference subjects.
  - [ ] Implement Curriculum before advanced subject assignment views because subjects reference curricula.

- [ ] 11.1.3 Build primary administrative modules
  - [ ] Implement Students UI.
  - [ ] Implement Sections UI.
  - [ ] Implement Subjects UI.
  - [ ] Implement Curriculum UI.
  - [ ] Implement Rooms UI.
  - [ ] Implement Schedules UI.

- [ ] 11.1.4 Build cross-module polish
  - [ ] Add shared validation, notifications, and preview components.
  - [ ] Add conflict visualizer and responsive UI refinements.
  - [ ] Add keyboard support and consistent focus behavior.

### 11.2 Interaction Flow Expectations
- [ ] 11.2.1 Define the default user flow
  - [ ] Open dashboard and land on a summary or welcome view.
  - [ ] Use sidebar navigation to open a module.
  - [ ] Search and select a record in the table.
  - [ ] Open add/edit dialog from the action bar.
  - [ ] Save, cancel, or discard changes with clear feedback.

- [ ] 11.2.2 Define edit flow standards
  - [ ] Pre-fill form fields in edit mode.
  - [ ] Keep dialogs modal to focus attention on the current record.
  - [ ] Preserve table selection after closing dialogs when practical.
  - [ ] Reuse layout and control patterns across all entities.

---

## 12. UI Quality Checklist
- [ ] 12.1 All modules use consistent layout hierarchy.
- [ ] 12.2 All tables have readable column widths and selection behavior.
- [ ] 12.3 All dialogs include Save and Cancel actions.
- [ ] 12.4 All required fields show inline validation.
- [ ] 12.5 All search bars support clear/reset behavior.
- [ ] 12.6 All modules follow NetBeans-friendly Swing component organization.
- [ ] 12.7 All cross-entity references use consistent selector controls.
- [ ] 12.8 Schedule UI includes visible conflict awareness components.
- [ ] 12.9 No backend logic is embedded in the UI task scope.
- [ ] 12.10 All components are ready for later DAO binding without redesign.

---

## 13. Final Delivery Notes
- [ ] 13.1 Package all module panels under a consistent UI package structure.
- [ ] 13.2 Ensure every major entity has a dedicated `JPanel` and modal dialog where needed.
- [ ] 13.3 Keep naming conventions consistent for forms, panels, renderers, and helper classes.
- [ ] 13.4 Validate that the dashboard shell can host all modules cleanly.
- [ ] 13.5 Confirm the entire frontend is buildable in NetBeans using standard Swing components only.

**Overall UI Complexity:** `Very High`
