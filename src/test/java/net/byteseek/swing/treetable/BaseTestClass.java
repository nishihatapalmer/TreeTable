/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2021, Matt Palmer
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.byteseek.swing.treetable;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import org.junit.jupiter.api.BeforeEach;

/**
 * Acts as a base class for tests that sets up a simple test tree and provides member fields to access them for
 * basic tests, and also utility methods to build random test trees.
 */
public class BaseTestClass {

    protected MutableTreeNode rootNode;
    protected MutableTreeNode child0;
    protected MutableTreeNode child1;
    protected MutableTreeNode child2;
    protected MutableTreeNode subchild0;
    protected MutableTreeNode subchild1;
    protected MutableTreeNode subchild2;
    protected MutableTreeNode subchild3;

    protected RowSorter.SortKey sortKey1;
    protected RowSorter.SortKey sortKey2;
    protected RowSorter.SortKey sortKey3;
    protected List<RowSorter.SortKey> sortKeyList;

    protected TestTreeTableModel model;
    protected JTable table;

    @BeforeEach
    public void setup() {
        rootNode = createTree();
        child0 = (MutableTreeNode) rootNode.getChildAt(0);
        child1 = (MutableTreeNode) rootNode.getChildAt(1);
        subchild0 = (MutableTreeNode) child1.getChildAt(0);
        subchild1 = (MutableTreeNode) child1.getChildAt(1);
        subchild2 = (MutableTreeNode) child1.getChildAt(2);
        subchild3 = (MutableTreeNode) child1.getChildAt(3);
        child2 = (MutableTreeNode) rootNode.getChildAt(2);

        sortKey1 = new RowSorter.SortKey(0, SortOrder.ASCENDING);
        sortKey2 = new RowSorter.SortKey(0, SortOrder.DESCENDING);
        sortKey3 = new RowSorter.SortKey(2, SortOrder.DESCENDING);

        sortKeyList = new ArrayList<>();
        sortKeyList.add(sortKey1);
        sortKeyList.add(sortKey2);
        sortKeyList.add(sortKey3);

        model = new TestTreeTableModel(rootNode, true);
        table = new JTable();
    }

    protected void createRandomTree(int randTree, boolean showRoot) {
        model.unbindTable();
        rootNode = buildRandomTree(randTree);
        model = new TestTreeTableModel(rootNode, showRoot);
    }

    public static DefaultMutableTreeNode buildRandomTree(int trial) {
        Random rand = new Random(trial);
        TestTreeTableModel.TestObject rootObject = new TestTreeTableModel.TestObject("root", 0, true);
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(rootObject);
        List<DefaultMutableTreeNode> allNodes = addChildren(rootNode, rand.nextInt(50) + 5, "rootchild", 0);
        int numSubFolders = rand.nextInt(400) + 20;
        for (int newFolder = 0; newFolder < numSubFolders; newFolder++) {
            DefaultMutableTreeNode randomNode = allNodes.get(rand.nextInt(allNodes.size()));
            allNodes.addAll(addChildren(randomNode, rand.nextInt(10) + 1, "sub", rand.nextInt(10000000)));
        }
        return rootNode;
    }

    public static  void expandAndCollapseRandomNodes(TreeTableModel model, int trial, int numToExpand, int percentToCollapse) {
        Random rand = new Random(trial);

        List<TreeNode> expanded = new ArrayList<>(model.getExpandedNodes());
        int numToCollapse = expanded.size() / 10;
        for (int i = 0; i < numToCollapse; i++) {
            int index = rand.nextInt(expanded.size());
            TreeNode randomNode = expanded.get(index);
            model.collapseNode(randomNode);
            expanded.remove(index);
        }

        // If we've managed to not expand the root, we won't have much to look for.
        if (model.getRowCount() == 0) {
            model.expandNode(model.getRoot());
        }

        for (int i = 0; i < numToExpand; i++) {
            TreeNode randomNode = model.getNodeAtModelIndex(rand.nextInt(model.getRowCount()));
            model.expandNode(randomNode);
        }
    }

    public static  void createChildren(MutableTreeNode parentNode, Object... childValues) {
        for (int i = 0; i < childValues.length; i++) {
            MutableTreeNode childNodes = new DefaultMutableTreeNode(childValues[i]);
            parentNode.insert(childNodes, i);
        }
    }

    public static  MutableTreeNode createTree() {
        TestTreeTableModel.TestObject rootObject = new TestTreeTableModel.TestObject("root", 0, true);
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(rootObject);
        addChildren(rootNode, 3, "child", 100);
        DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) rootNode.getChildAt(1);
        addChildren(childNode, 4, "subchildren", 1000);
        return rootNode;
    }

    public static  List<DefaultMutableTreeNode> addChildren(DefaultMutableTreeNode parent, int numChildren, String description, int sizeStart) {
        TestTreeTableModel.TestObject parentObject = (TestTreeTableModel.TestObject) parent.getUserObject();
        parent.setAllowsChildren(true);
        List<DefaultMutableTreeNode> results = new ArrayList<>();
        for (int i = 0; i < numChildren; i++) {
            TestTreeTableModel.TestObject object = new TestTreeTableModel.TestObject(description + i, sizeStart + i, (sizeStart + i) % 2 == 0);
            parentObject.children.add(object);
            DefaultMutableTreeNode childNodes = new DefaultMutableTreeNode(object, false);
            parent.insert(childNodes, i);
            results.add(childNodes);
        }
        return results;
    }

    public static class TestTreeTableModel extends TreeTableModel {

        private static final Comparator<Boolean> ALL_BOOLS_EQUAL_TEST_COMPARATOR = (o1, o2) -> 0;

        private int columnCount = 5;

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
                case 3: // return an object which is not comparable.
                    return test;
                case 4: // return alternating null and "not null" string.
                    return getModelIndexForTreeNode(node) % 2 == 0 ? null : "not null";
                default:
                    return null;
            }
        }

        @Override
        public Comparator<?> getColumnComparator(final int column) {
            return column == 2 ? ALL_BOOLS_EQUAL_TEST_COMPARATOR : null;
        }

        @Override
        public TableColumnModel createTableColumnModel() {
            TableColumnModel columns = new DefaultTableColumnModel();
            for (int columnNumber = 0; columnNumber < columnCount; columnNumber++) {
                columns.addColumn(TableUtils.createColumn(columnNumber, "header" + columnNumber));
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
}
