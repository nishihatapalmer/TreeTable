package net.byteseek.swing.treetable;

import javax.swing.table.TableRowSorter;

public class TreeTableRowSorter extends TableRowSorter<TreeTableModel> {

    public TreeTableRowSorter() {
        super();
    }

    public TreeTableRowSorter(TreeTableModel model) {
        super(model);
    }


}
