package test;

import net.byteseek.swing.treetable.TreeTableCellRenderer;
import net.byteseek.swing.treetable.TreeTableModel;
import net.byteseek.swing.treetable.TreeTableNode;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

public class TestTreeTableModel extends TreeTableModel {

    private static final int NUM_COLUMNS = 3;

    private final TableColumn[] TABLE_COLUMNS = new TableColumn[NUM_COLUMNS];

    public TestTreeTableModel(TreeTableNode rootNode, boolean showRoot) {
        super(rootNode, NUM_COLUMNS, showRoot);
        buildColumns();
    }

    protected Object getColumnValue(final Object o, final int column) {
        if (o instanceof TestClass) {
            TestClass obj = (TestClass) o;
            switch (column) {
                case 0: return obj.getDescription();
                case 1: return obj.getSize();
                case 2: return obj.isEnabled();
            }
        }
        return null;
    }

    @Override
    protected TableColumn getTableColumn(int column) {
        return TABLE_COLUMNS[column];
    }

    private void buildColumns() {
        TABLE_COLUMNS[0] = createColumn("description", 0, new TreeTableCellRenderer(this));
        TABLE_COLUMNS[1] = createColumn("size", 1, null);
        TABLE_COLUMNS[2] = createColumn("enabled", 2,null);
    }

}
