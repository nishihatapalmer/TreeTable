package net.byteseek.swing.treetable;

import javax.swing.*;
import javax.swing.event.TreeWillExpandListener;
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

    private List<TreeTableNode> displayedNodes = new ArrayList<>(); // needs updating every time we expand / collapse.

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
        table.setShowVerticalLines(false);
        table.setRowHeight(24);
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
        TableCellRenderer renderer = getTableColumnModel().getColumn(col).getCellRenderer();
        if (renderer instanceof TreeTableCellRenderer) {
            TreeTableNode node = getNodeAtRow(row);

            if (node != null && node.getAllowsChildren()) {
                final int columnStart = calculateWidthToLeft(col);
                final int indentWidth = ((TreeTableCellRenderer) renderer).getNodeIndent(node);
                final int mouseX = evt.getPoint().x;
                if (mouseX > columnStart && mouseX < columnStart + indentWidth) {
                    node.toggleExpanded();

                    //TODO: Should only rebuild if anything changes.
                    //TODO: should either add nodes required, or remove nodes required, not rebuild entirely.
                    buildVisibleNodes();
                }
            }
        }
        return true; // TODO: should only return true if something changed.
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

}
