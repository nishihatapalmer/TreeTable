package net.byteseek.swing.treetable;

import javax.swing.*;

//TODO: so far there is nothing we have to do here - we could just use a default JTable... might be nice
//      to do everything in the model, node and renderer.  Horribly breaks MVC I guess, but honestly,
//      there isn't really anything to do here we can't do elsewhere.  Nice to be able to just drop a standard
//      JTable on a form in a visual designer, then bind the model to it and suddenly have a tree table.

public class JTreeTable extends JTable {

    public JTreeTable() {
        setShowVerticalLines(false);
        setRowHeight(24);
    }

    public JTreeTable(TreeTableModel treeTableModel) {
        super(treeTableModel);
        setShowVerticalLines(false);
        setRowHeight(24);
    }

}
