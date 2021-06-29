package net.byteseek.swing.treetable;

import javax.swing.*;

public class JTreeTable extends JTable {

    private TreeTableModel treeTableModel;

    public JTreeTable() {
    }

    public JTreeTable(TreeTableModel treeTableModel) {
        super(treeTableModel);
        this.treeTableModel = treeTableModel;
        setColumnModel(treeTableModel.getTableColumnModel());
    }

}
