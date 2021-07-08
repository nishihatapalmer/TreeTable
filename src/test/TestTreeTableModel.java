package test;

import net.byteseek.swing.treetable.TreeTableCellRenderer;
import net.byteseek.swing.treetable.TreeTableModel;
import net.byteseek.swing.treetable.TreeTableNode;

import javax.swing.table.TableColumn;
import java.util.Comparator;

public class TestTreeTableModel extends TreeTableModel {

    private static final int NUM_COLUMNS = 3;

    private final TableColumn[] TABLE_COLUMNS = new TableColumn[NUM_COLUMNS];

    public TestTreeTableModel(TreeTableNode rootNode, boolean showRoot) {
        super(rootNode, NUM_COLUMNS, showRoot);
        buildColumns();
    }

    protected Object getColumnValue(final TreeTableNode node, final int column) {
        Object o = node.getUserObject();
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

    @Override
    protected Comparator<?> getColumnComparator(int column) {
        return null; //TODO: return comparator for column values to enable sorting.
    }

    @Override
    protected Comparator<TreeTableNode> getNodeComparator() {
        return null; //TODO: return node comparator to enable node-property related sorting for all columns.
    }

    private void buildColumns() {
        TABLE_COLUMNS[0] = createColumn("description", 0, new TreeTableCellRenderer(this));
        TABLE_COLUMNS[1] = createColumn("size", 1, null);
        TABLE_COLUMNS[2] = createColumn("enabled", 2,null);
    }

}
