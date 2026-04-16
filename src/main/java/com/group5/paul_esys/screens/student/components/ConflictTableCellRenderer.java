package com.group5.paul_esys.screens.student.components;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Custom table cell renderer that highlights schedule conflicts with visual indicators.
 * Applies background colors and icons to rows containing conflicting schedules.
 */
public class ConflictTableCellRenderer extends DefaultTableCellRenderer {

  public static final Color CONFLICT_BACKGROUND = new Color(255, 240, 240); // Light red
  public static final Color CONFLICT_LIGHT_BACKGROUND = new Color(255, 248, 248); // Very light red
  public static final Color NORMAL_BACKGROUND = Color.WHITE;

  private final boolean hasConflict;
  private final String conflictIcon;

  /**
   * Creates a renderer for a cell.
   * @param hasConflict true if this row represents a schedule with conflicts
   * @param conflictIcon optional icon to display (e.g., "⚠" or "🔴")
   */
  public ConflictTableCellRenderer(boolean hasConflict, String conflictIcon) {
    this.hasConflict = hasConflict;
    this.conflictIcon = conflictIcon;
  }

  @Override
  public Component getTableCellRendererComponent(
      JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    
    Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

    if (isSelected) {
      component.setBackground(table.getSelectionBackground());
      component.setForeground(table.getSelectionForeground());
      return component;
    }

    if (hasConflict) {
      component.setBackground(CONFLICT_BACKGROUND);
      component.setForeground(new Color(180, 0, 0)); // Dark red text

      // Add visual indicator to subject code column (column 0)
      if (column == 0 && value != null) {
        setText("⚠ " + value.toString());
      }
    } else {
      component.setBackground(NORMAL_BACKGROUND);
      component.setForeground(table.getForeground());
    }

    return component;
  }
}
