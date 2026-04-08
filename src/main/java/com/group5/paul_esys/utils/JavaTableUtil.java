package com.group5.paul_esys.utils;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;

public class JavaTableUtil {

    public static Object getSelectedValueFromTable(JTable table) {
        return table.getValueAt(
            table.getSelectedRow(),
            table.getSelectedColumn()
        );
    }

    public static DefaultTableModel searchTable(JTable table, String searchTerm) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        sorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchTerm));
        return model;
    }
}
