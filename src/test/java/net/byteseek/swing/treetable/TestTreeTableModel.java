package net.byteseek.swing.treetable;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

class TestTreeTableModel extends TreeTableModel {

    private int columnCount = 3;

    public TestTreeTableModel(TreeNode node) {
        super(node);
    }

    public TestTreeTableModel(TreeNode node, boolean showRoot) {
        super(node, showRoot);
    }

    public TestTreeTableModel(TreeNode node, boolean showRoot, int columnCount) {
        super(node, showRoot);
        this.columnCount = columnCount;
    }

    public void setColumnCount(int newCount) {
        columnCount = newCount;
    }

    @Override
    public Object getColumnValue(TreeNode node, int column) {
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
        TestObject test = (TestObject) treeNode.getUserObject();
        switch (column) {
            case 0:
                return test.description;
            case 1:
                return test.size;
            case 2:
                return test.enabled;
            default:
                return null;
        }
    }

    @Override
    public TableColumnModel createTableColumnModel() {
        TableColumnModel columns = new DefaultTableColumnModel();
        for (int columnNumber = 0; columnNumber < columnCount; columnNumber++) {
            columns.addColumn(createColumn(columnNumber, "header" + columnNumber));
        }
        return columns;
    }

    @Override
    public void setColumnValue(final TreeNode node, int column, Object aValue) {
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
        TestObject test = (TestObject) treeNode.getUserObject();
        switch (column) {
            case 0: {
                test.description = (String) aValue;
                break;
            }
            case 1: {
                test.size = (Long) aValue;
                break;
            }
            case 2: {
                test.enabled = (Boolean) aValue;
                break;
            }
        }
    }

    public void printTree() {
        System.out.println("Model order tree:");
        System.out.println(TreeUtils.buildTextTree(getVisibleNodes(), true));
        System.out.println();
    }

    public void printTableTree() {
        if (table == null) {
            printTree();
        } else {
            System.out.println("Table order tree:");
            List<TreeNode> nodes = new ArrayList<>();
            for (int row = 0; row < table.getRowCount(); row++) {
                nodes.add(getNodeAtTableRow(row));
            }
            System.out.println(TreeUtils.buildTextTree(nodes, true));
            System.out.println();
        }
    }

    public static class TestObject {
        public String description;
        public long size;
        public boolean enabled;
        public List<TestObject> children = new ArrayList<>();

        public TestObject(String description, long size, boolean enabled) {
            this.description = description;
            this.size = size;
            this.enabled = enabled;
        }

        public List<TestObject> getChildren() {
            return children;
        }

        public String toString() {
            return "TestObject(" + description + "," + size + "," + enabled + ')';
        }
    }
}
