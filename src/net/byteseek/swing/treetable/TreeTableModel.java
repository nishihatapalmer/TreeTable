package net.byteseek.swing.treetable;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

//TODO: Tree doesn't always re-point after update.  Calling repaint() not enough..?  Need Table Model Changed event?

public abstract class TreeTableModel extends AbstractTableModel {

    private final TreeTableNode rootNode;
    private TableColumnModel columnModel;
    private final int numColumns;
    private boolean showRoot;
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
        return getColumnValue(getNodeAtRow(row).getUserObject(), column);
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
    }

    protected TreeTableNode getNodeAtRow(final int row) {
        return row >= 0 && row < displayedNodes.size() ? displayedNodes.get(row) : null;
    }

    protected abstract Object getColumnValue(Object o, int column);

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
                if (expandOrCollapseEvent(e, table.rowAtPoint(point), table.columnAtPoint(point))) {
                    table.repaint(); //TODO: Should use table model changed messages (repaint doesn't always work).
                }
            }
        });
    }

    protected boolean expandOrCollapseEvent(MouseEvent evt, int row, int col) {
        TreeTableNode node = getNodeAtRow(row);
        if (clickOnExpand(node, col, evt) && listenersApprove(node)) {
            boolean nodesRemoved = false, nodesAdded = false;
            if (node.isExpanded()) {
                nodesRemoved = removeDisplayedChildren(node, row);
            }
            node.toggleExpanded();
            if (node.isExpanded()) {
                nodesAdded = addChildrenToDisplay(node, row);
            }
            return nodesRemoved || nodesAdded;
        }
        return false;
    }

    private boolean listenersApprove(TreeTableNode node) {
        final TreeTableEvent event = node.isExpanded() ?
                new TreeTableEvent(node, TreeTableEvent.TreeTableEventType.COLLAPSING) :
                new TreeTableEvent(node, TreeTableEvent.TreeTableEventType.EXPANDING);
        return listenersApprovedAndActionedEvent(event);
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

    private int calculateWidthToLeft(int colIndex) {
        TableColumnModel model = getTableColumnModel();
        int width = 0;
        for (int col = colIndex - 1; col >= 0; col--) {
            width += model.getColumn(col).getWidth();
        }
        return width;
    }

    private boolean removeDisplayedChildren(TreeTableNode node, int row) {
        final int childCount = node.getChildCount();
        if (childCount > 0) {
            final int numToRemove = node.getVisibleNodeCount() - 1;
            if (numToRemove == 1) {
                displayedNodes.remove(row + 1);
            } else {
                displayedNodes.remove(row + 1, row + numToRemove + 1);
            }
            return true;
        }
        return false;
    }

    private boolean addChildrenToDisplay(TreeTableNode node, int row) {
        final int childCount = node.getChildCount();
        if (childCount > 0) {
            List<TreeTableNode> newVisibleNodes = new ArrayList<>();
            node.addVisibleChildren(newVisibleNodes);
            if (newVisibleNodes.size() == 1) {
                displayedNodes.insert(newVisibleNodes.get(0), row + 1);
            } else {
                displayedNodes.insert(newVisibleNodes, row + 1);
            }
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

    private boolean listenersApprovedAndActionedEvent(TreeTableEvent event) {
        // First ask listeners if they're OK to proceed with event.
        // We ask them all first, in case later ones reject but the intial
        // ones have already made changes.
        for (TreeTableEvent.Listener listener : eventListeners) {
            if (!listener.acceptTreeEvent(event)) {
                return false;
            }
        }
        // No refusals, so we proceed to action the event for the listeners.
        for (TreeTableEvent.Listener listener : eventListeners) {
            listener.actionTreeEvent(event);
        }
        return true;
    }

}
