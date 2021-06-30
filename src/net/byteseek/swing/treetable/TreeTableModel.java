package net.byteseek.swing.treetable;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

public abstract class TreeTableModel extends AbstractTableModel {

    private final TreeTableNode rootNode;
    private TableColumnModel columnModel;
    private final int numColumns;
    private boolean showRoot;

    //private List<TreeTableNode> displayedNodes = new ArrayList<>(); // needs updating every time we expand / collapse.
    private NodeDisplayList displayedNodes = new NodeDisplayList();

    public TreeTableModel(final TreeTableNode rootNode, final int numColumns, final boolean showRoot) {
        this.rootNode = rootNode;
        this.showRoot = showRoot;
        this.numColumns = numColumns;
        buildVisibleNodes();
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
        // table.setAutoCreateRowSorter(true);
        registerMouseListener(table);
    }

    protected void registerMouseListener(final JTable table) {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point point = e.getPoint();
                if (expandOrCollapseEvent(e, table.rowAtPoint(point), table.columnAtPoint(point))) {
                    table.repaint(); // Should use tree expanded messages, etc?
                }
            }
        });
    }

    protected abstract Object getColumnValue(Object o, int column);

    protected abstract TableColumn getTableColumn(int column);

    protected TreeTableNode getNodeAtRow(final int row) {
        return row >= 0 && row < displayedNodes.size() ? displayedNodes.get(row) : null;
    }

    protected boolean expandOrCollapseEvent(MouseEvent evt, int row, int col) {
        TreeTableNode node = getNodeAtRow(row);
        if (clickOnExpand(node, col, evt)) {
            if (node.isExpanded()) {
                removeDisplayedChildren(node, row);
            }
            node.toggleExpanded();
            if (node.isExpanded() && node.getChildCount() > 0) {
                addChildrenToDisplay(node, row);
            }
            //TODO: Should only rebuild if anything changes.
            //TODO: should either add nodes required, or remove nodes required, not rebuild entirely.
           // buildVisibleNodes();
            return true; // TODO: should only return true if something changed.
        }
        return false;
    }

    private void addChildrenToDisplay(TreeTableNode node, int row) {
        final int childCount = node.getChildCount();
        if (childCount > 0) {
            List<TreeTableNode> newVisibleNodes = new ArrayList<>();
            node.addVisibleChildren(newVisibleNodes);
            if (newVisibleNodes.size() == 1) {
                displayedNodes.insert(newVisibleNodes.get(0), row + 1);
            } else {
                displayedNodes.insert(newVisibleNodes, row + 1);
            }
        }
    }

    private void removeDisplayedChildren(TreeTableNode node, int row) {
        final int childCount = node.getChildCount();
        if (childCount == 1) {
            displayedNodes.remove(row + 1);
        } else if (childCount > 1) {
            final int numToRemove = node.getVisibleNodeCount() - 1;
            displayedNodes.remove(row + 1, row + numToRemove + 1);
        }
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

    private void buildVisibleNodes() {
        displayedNodes.clear();
        if (showRoot) {
            displayedNodes.add(rootNode);
        }
        rootNode.addVisibleChildren(displayedNodes);
    }

    private static class NodeDisplayList extends AbstractList<TreeTableNode> {

        private TreeTableNode[] displayedNodes = new TreeTableNode[128];
        private int size;

        public int size() {
            return size;
        }

        public TreeTableNode get(int index) {
            checkIndex(index);
            return displayedNodes[index];
        }

        public boolean add(final TreeTableNode node) {
            checkResize(1);
            displayedNodes[size++] = node;
            return true;
        }

        public boolean add(List<TreeTableNode> nodes) {
            final int numToAdd = nodes.size();
            checkResize(numToAdd);
            for (int nodeIndex = 0; nodeIndex < numToAdd; nodeIndex++) {
                displayedNodes[size++] = nodes.get(nodeIndex);
            }
            return true;
        }

        public void insert(TreeTableNode node, int index) {
            if (index == size) {
                add(node);
            } else {
                checkIndex(index);
                checkResize(1);
                // Shift the others along one:
                for (int position = size - 1; position >= index; position--) {
                    displayedNodes[position + 1] = displayedNodes[position];
                }
                // insert the new node:
                displayedNodes[index] = node;
                size++;
            }
        }

        public void insert(List<TreeTableNode> nodes, int index) {
            if (index == size) {
                add(nodes);
            } else {
                checkIndex(index);
                final int numToAdd = nodes.size();
                checkResize(numToAdd);
                // Shift the others along one by the number of nodes:
                for (int position = size - 1; position >= index; position--) {
                    displayedNodes[position + numToAdd] = displayedNodes[position];
                }
                // insert the new nodes:
                for (int nodeIndex = 0; nodeIndex < numToAdd; nodeIndex++) {
                    displayedNodes[size + nodeIndex] = nodes.get(nodeIndex);
                }
                size += numToAdd;
            }
        }

        public TreeTableNode remove(int index) {
            checkIndex(index);
            TreeTableNode nodeToRemove = displayedNodes[index];
            for (int position = index; position < size - 1; position++) {
                displayedNodes[position] = displayedNodes[position + 1];
            }
            size--;
            return nodeToRemove;
        }

        public void remove(int from, int to) {
            checkIndex(from);
            if (to <= from) {
                throw new IllegalArgumentException("to:" + to + " must be greater than from:" + from);
            }
            if (to >= size) { // If we're removing everything up to or past the end, just set the size down.
                size = from;
            } else { // got some stuff at the end we have to move over to cover the gap:
                final int numToRemove = to - from;
                final int remainingLength = size - to;
                for (int position = 0; position < remainingLength; position++) {
                    displayedNodes[from + position] = displayedNodes[to + position];
                }
                size -= numToRemove;
            }
        }

        public void clear() {
            Arrays.fill(displayedNodes, null); // clear all references to any nodes.
            size = 0;
        }

        private void checkIndex(int index) {
            if (index >= size || index < 0) {
                throw new IndexOutOfBoundsException("Index = " + index + " size = " + size);
            }
        }

        private void checkResize(int numToAdd) {
            if (size + numToAdd >= displayedNodes.length) {
                resizeArray();
            }
        }

        private void resizeArray() {
            TreeTableNode[] newArray = new TreeTableNode[displayedNodes.length + 128];
            System.arraycopy(displayedNodes, 0,newArray, 0, displayedNodes.length);
            displayedNodes = newArray;
        }

    }

}
