package net.byteseek.swing.treetable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

//TODO: so far there is nothing we have to do here - we could just use a default JTable... might be nice
//      to do everything in the model, node and renderer.

public class JTreeTable extends JTable {

    private TreeTableModel treeTableModel;

    public JTreeTable() {
    }

    public JTreeTable(TreeTableModel treeTableModel) {
        super(treeTableModel);
        this.treeTableModel = treeTableModel;
        setColumnModel(treeTableModel.getTableColumnModel());
        setShowVerticalLines(false);
        setRowHeight(24);
    }

}
