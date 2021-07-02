package net.byteseek.swing.treetable;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

//TODO: dynamically adjust visibleNodeCount through notifications to parent nodes when expand collapse happens above.

//TODO: Tree doesn't always re-point after update.  Calling repaint() not enough..?  Need Table Model Changed event?

//TODO: sorting (up / down / off).  multi column sort?

//TODO: test dynamic expand remove nodes

//TODO: customise visual appearance - all just on the JTable?

//TODO: test setting different icons.

//TODO: show plus sign on nodes that we haven't dynamically expanded (if they support having children).

public abstract class TreeTableModel extends AbstractTableModel {

    private final int numColumns;
    private boolean showRoot;

    private final TreeTableNode rootNode;
    private TableColumnModel columnModel;
    private TreeTableNodeList displayedNodes = new TreeTableNodeList();
    private List<TreeTableEvent.Listener> eventListeners = new ArrayList<>(2);

    public TreeTableModel(final TreeTableNode rootNode, final int numColumns, final boolean showRoot) {
        this.rootNode = rootNode;
        this.showRoot = showRoot;
        this.numColumns = numColumns;
        buildVisibleNodes();
    }

    public void addListener(TreeTableEvent.Listener listener) {
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener);
        }
    }

    public void removeListener(TreeTableEvent.Listener listener) {
        eventListeners.remove(listener);
    }

    @Override
    public int getRowCount() {
        return displayedNodes.size();
    }

    @Override
    public int getColumnCount() {
        return numColumns;
    }

    @Override
    public Object getValueAt(final int row, final int column) {
        return getColumnValue(getNodeAtRow(row), column);
    }

    public boolean getShowRoot() {
        return showRoot;
    }

    public void setShowRoot(boolean showRoot) {
        this.showRoot = showRoot;
        //TODO: invalidate model if different.
    }

    public void initializeTable(JTable table) {
        table.setAutoCreateColumnsFromModel(false);
        table.setModel(this);
        table.setColumnModel(getTableColumnModel());
        registerMouseListener(table);
        registerTableDataListener(table);
    }

    protected TreeTableNode getNodeAtRow(final int row) {
        return row >= 0 && row < displayedNodes.size() ? displayedNodes.get(row) : null;
    }

    protected abstract Object getColumnValue(TreeTableNode node, int column);

    protected abstract TableColumn getTableColumn(int column);

    protected TableColumnModel getTableColumnModel() {
        if (columnModel == null) {
            columnModel = new DefaultTableColumnModel();
            for (int column = 0; column < getColumnCount(); column++) {
                columnModel.addColumn(getTableColumn(column));
            }
        }
        return columnModel;
    }

    protected TableColumn createColumn(String headerValue, int modelIndex, TableCellRenderer renderer) {
        TableColumn tableColumn = new TableColumn(modelIndex);
        tableColumn.setHeaderValue(headerValue);
        if (renderer != null) {
            tableColumn.setCellRenderer(renderer);
        }
        return tableColumn;
    }

    protected void registerMouseListener(final JTable table) {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
            Point point = e.getPoint();
            checkExpandOrCollapse(e, table.rowAtPoint(point), table.columnAtPoint(point));
                //table.repaint(); //TODO: Should use table model changed messages (repaint doesn't always work).
            }
        });
    }

    protected void registerTableDataListener(final JTable table) {
        addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                table.tableChanged(e);
            }
        });
    }

    protected void checkExpandOrCollapse(MouseEvent evt, int row, int col) {
        TreeTableNode node = getNodeAtRow(row);
        if (clickOnExpand(node, col, evt)) {
            // listeners may change the node structure (e.g. dynamically add, remove or change nodes).
            // So can't rely on what was there before.  Get the number of *currently* visible children:
            final int numVisibleChildren = node.getChildVisibleNodeCount();
            if (listenersApprove(node)) {
                if (node.isExpanded()) {
                    removeDisplayedChildren(row, numVisibleChildren);
                }
                node.toggleExpanded();
                if (node.isExpanded()) {
                    addChildrenToDisplay(row, node);
                }
            }
        }
    }

    private boolean listenersApprove(TreeTableNode node) {
        final TreeTableEvent event = node.isExpanded() ?
                new TreeTableEvent(node, TreeTableEvent.TreeTableEventType.COLLAPSING) :
                new TreeTableEvent(node, TreeTableEvent.TreeTableEventType.EXPANDING);
        return listenersApprovedEvent(event);
    }

    private boolean clickOnExpand(TreeTableNode node, int column, MouseEvent evt) {
        TableCellRenderer renderer = getTableColumnModel().getColumn(column).getCellRenderer();
        if (renderer instanceof TreeTableCellRenderer) {
            if (node != null && node.getAllowsChildren()) {
                final int columnStart = calculateWidthToLeft(column);
                final int expandEnd = columnStart + ((TreeTableCellRenderer) renderer).getNodeIndent(node);
                final int mouseX = evt.getPoint().x;
                if (mouseX > columnStart && mouseX < expandEnd) {
                    return true;
                }
            }
        }
        return false;
    }

    private int calculateWidthToLeft(final int colIndex) {
        TableColumnModel model = getTableColumnModel();
        int width = 0;
        for (int col = colIndex - 1; col >= 0; col--) {
            width += model.getColumn(col).getWidth();
        }
        return width;
    }

    private boolean removeDisplayedChildren(final int row, final int numberToRemove) {
        if (numberToRemove > 0) {
            final int firstChildRow = row + 1;
            if (numberToRemove == 1) {
                displayedNodes.remove(firstChildRow);
            } else {
                displayedNodes.remove(firstChildRow, firstChildRow + numberToRemove);
            }
            fireTableRowsDeleted(firstChildRow, firstChildRow + numberToRemove - 1);
            return true;
        }
        return false;
    }

    private boolean addChildrenToDisplay(final int row, final TreeTableNode node) {
        final int childCount = node.getChildCount();
        if (childCount > 0) {
            final int firstChildRow = row + 1;
            List<TreeTableNode> newVisibleNodes = new ArrayList<>();
            node.addVisibleChildren(newVisibleNodes);
            if (newVisibleNodes.size() == 1) {
                displayedNodes.insert(newVisibleNodes.get(0), firstChildRow);
            } else {
                displayedNodes.insert(newVisibleNodes, firstChildRow);
            }
            fireTableRowsInserted(firstChildRow, firstChildRow + newVisibleNodes.size() - 1);
            return true;
        }
        return false;
    }

    private void buildVisibleNodes() {
        displayedNodes.clear();
        if (showRoot) {
            displayedNodes.add(rootNode);
        }
        rootNode.addVisibleChildren(displayedNodes);
    }

    private boolean listenersApprovedEvent(final TreeTableEvent event) {
        for (TreeTableEvent.Listener listener : eventListeners) {
            if (!listener.actionTreeEvent(event)) {
                return false;
            }
        }
        return true;
    }

}
