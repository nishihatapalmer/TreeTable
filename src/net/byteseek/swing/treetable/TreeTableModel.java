package net.byteseek.swing.treetable;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.List;

public abstract class TreeTableModel extends AbstractTableModel {

    private final DefaultMutableTreeNode rootNode;
    private boolean showRoot;

    private List<DefaultMutableTreeNode> displayedNodes = new ArrayList<>();

    public TreeTableModel(final DefaultMutableTreeNode rootNode, boolean showRoot) {
        this.rootNode = rootNode;
        this.showRoot = showRoot;
        addInitialNodes();
    }

    @Override
    public int getRowCount() {
        return displayedNodes.size();
    }

    @Override
    public Object getValueAt(int row, int column) {
        return getObjectColumnValue(displayedNodes.get(row).getUserObject(), column);
    }

    protected abstract Object getObjectColumnValue(Object o, int column);

    protected abstract String getHeaderName(int column);

    protected TableColumnModel getTableColumnModel() {
        TableColumnModel model = new DefaultTableColumnModel();
        for (int column = 0; column < getColumnCount(); column++) {
            TableColumn aColumn = new TableColumn(column);
            aColumn.setHeaderValue(getHeaderName(column));
            model.addColumn(aColumn);
        }
        return model;
    }

    private void addInitialNodes() {
        if (showRoot) {
            displayedNodes.add(rootNode);
        } else {
            addChildNodes(rootNode, 0);
        }
    }

    private void addChildNodes(DefaultMutableTreeNode parent, int insertIndex) {
        for (int childIndex = 0; childIndex < parent.getChildCount(); childIndex++) {
            displayedNodes.add(insertIndex + childIndex, (DefaultMutableTreeNode) parent.getChildAt(childIndex));
        }
    }

}
