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

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TreeTableModelTest extends BaseTestClass {


    //TODO: test using a TreeModel to update nodes while inside an expand or collapse event.


    /* *****************************************************************************************************************
     *                                              Constructor tests
     */

    @Test
    public void testNullRootNodeConstructorException() {
        assertThrows(IllegalArgumentException.class,
                ()-> model = new TestTreeTableModel(null));

        assertThrows(IllegalArgumentException.class,
                ()-> model = new TestTreeTableModel(null, true));

        assertThrows(IllegalArgumentException.class,
                ()-> model = new TestTreeTableModel(null, false));
    }

    @Test
    public void testRootVisibleConstructor() {
        model = new TestTreeTableModel(rootNode, true);
        assertEquals(rootNode, model.getRoot());
        assertTrue(model.isVisible(rootNode));
    }

    @Test
    public void testRootVisibleDefaultConstructor() {
        model = new TestTreeTableModel(rootNode);
        assertEquals(rootNode, model.getRoot());
        assertTrue(model.isVisible(rootNode));
    }

    @Test
    public void testRootNotVisibleConstructor() {
        model = new TestTreeTableModel(rootNode, false);
        assertEquals(rootNode, model.getRoot());
        assertFalse(model.isVisible(rootNode));
    }


    /* *****************************************************************************************************************
     *                                              Table binding tests
     */

    @Test
    public void testDefaultBindTable() {
        assertNull(model.getTable());
        assertNull(table.getRowSorter());
        assertNotEquals(table.getColumnModel(), model.getTableColumnModel());
        assertNull(model.tableMouseListener);

        model.bindTable(table);
        testDefaultTableBinding();

        assertTrue(table.getRowSorter().getSortKeys().isEmpty());
        assertEquals(TreeTableHeaderRenderer.class, table.getTableHeader().getDefaultRenderer().getClass());
    }

    @Test
    public void testBindTableWithNullDefaultSortKey() {
        model.bindTable(table);
        model.setDefaultSortKeys((RowSorter.SortKey) null);
        assertEquals(0, model.getDefaultSortKeys().size());
        testDefaultTableBinding();
        testNoSortKeys();
    }

    @Test
    public void testBindTableWithDefaultSortKey() {
        model.bindTable(table);
        model.setDefaultSortKeys(sortKey1);
        assertEquals(1, model.getDefaultSortKeys().size());
        assertEquals(sortKey1, model.getDefaultSortKeys().get(0));
        testDefaultTableBinding();
        testOneSortKey();
    }

    @Test
    public void testBindTableWithTwoDefaultSortKeys() {
        model.bindTable(table);
        model.setDefaultSortKeys(sortKey1, sortKey2);
        assertEquals(2, model.getDefaultSortKeys().size());
        assertEquals(sortKey1, model.getDefaultSortKeys().get(0));
        assertEquals(sortKey2, model.getDefaultSortKeys().get(1));
        testDefaultTableBinding();
        testTwoSortKeys();
    }

    @Test
    public void testBindTableWitDefaultSortKeyList() {
        model.bindTable(table);
        model.setDefaultSortKeys(sortKeyList);
        assertEquals(sortKeyList, model.getDefaultSortKeys());
        testDefaultTableBinding();
        testListOfKeys();
    }

    @Test
    public void testBindTableWitDefaultEmptySortKeyList() {
        model.bindTable(table);
        model.setDefaultSortKeys(Collections.emptyList());
        assertEquals(Collections.emptyList(), model.getDefaultSortKeys());
        testDefaultTableBinding();
        testNoSortKeys();
    }

    @Test
    public void testBindTableWitDefaultNullSortKeyList() {
        model.bindTable(table);
        model.setDefaultSortKeys((List<RowSorter.SortKey>) null);
        assertEquals(Collections.emptyList(), model.getDefaultSortKeys());
        testDefaultTableBinding();
        testNoSortKeys();
    }

    private void testNoSortKeys() {
        assertTrue(table.getRowSorter().getSortKeys().isEmpty());
    }

    private void testOneSortKey() {
        List<? extends RowSorter.SortKey> sortKeyList = table.getRowSorter().getSortKeys();
        assertEquals(1, sortKeyList.size());
        assertEquals(sortKey1, sortKeyList.get(0));
    }

    private void testTwoSortKeys() {
        List<? extends RowSorter.SortKey> sortKeyList = table.getRowSorter().getSortKeys();
        assertEquals(2, sortKeyList.size());
        assertEquals(sortKey1, sortKeyList.get(0));
        assertEquals(sortKey2, sortKeyList.get(1));
    }

    private void testListOfKeys() {
        List<? extends RowSorter.SortKey>sortKeyList = table.getRowSorter().getSortKeys();
        assertEquals(3, sortKeyList.size());
        assertEquals(sortKey1, sortKeyList.get(0));
        assertEquals(sortKey2, sortKeyList.get(1));
        assertEquals(sortKey3, sortKeyList.get(2));
    }

    @Test
    public void testBindTableWithCustomRowSorter() {
        RowSorter<TreeTableModel> sorter = new TableRowSorter<>();
        model.bindTable(table, sorter);
        testDefaultTableBinding();
        assertEquals(sorter, table.getRowSorter());
    }

    @Test
    public void testBindTableWithCustomHeaderRenderer() {
        TableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        model.bindTable(table, headerRenderer);
        testDefaultTableBinding();
        assertEquals(headerRenderer, table.getTableHeader().getDefaultRenderer());
    }


    @Test
    public void testBindToAnotherTable() {
        model.bindTable(table);
        assertEquals(table, model.getTable());
        assertEquals(model, table.getModel());

        JTable newTable = new JTable();
        model.bindTable(newTable);
        assertEquals(newTable, model.getTable());
        assertEquals(model, newTable.getModel());
        assertNotEquals(model, table.getModel());
    }

    @Test
    public void testUnbindTable() {
        model.bindTable(table);
        assertEquals(table, model.getTable());
        assertEquals(model, table.getModel());
        assertNotNull(model.tableMouseListener);

        model.unbindTable();
        assertNull(model.getTable());
        assertNull(model.tableMouseListener);
        assertNull(table.getRowSorter());
        assertNotEquals(model.getTableColumnModel(), table.getColumnModel());

        InputMap inputMap = table.getInputMap();
        for (KeyStroke keyStroke : model.getExpandKeys()) {
            assertNotEquals(TreeTableModel.EXPAND_NODE_KEYSTROKE_KEY, inputMap.get(keyStroke));
        }
        for (KeyStroke keyStroke : model.getCollapseKeys()) {
            assertNotEquals(TreeTableModel.COLLAPSE_NODE_KEYSTROKE_KEY, inputMap.get(keyStroke));
        }
        for (KeyStroke keyStroke : model.getToggleKeys()) {
            assertNotEquals(TreeTableModel.TOGGLE_EXPAND_NODE_KEYSTROKE_KEY, inputMap.get(keyStroke));
        }

        ActionMap actionMap = table.getActionMap();
        assertNull(actionMap.get(TreeTableModel.EXPAND_NODE_KEYSTROKE_KEY));
        assertNull(actionMap.get(TreeTableModel.COLLAPSE_NODE_KEYSTROKE_KEY));
        assertNull(actionMap.get(TreeTableModel.TOGGLE_EXPAND_NODE_KEYSTROKE_KEY));
    }

    private void testDefaultTableBinding() {
        assertNotNull(model.tableMouseListener);
        assertEquals(table, model.getTable());
        assertEquals(model, table.getModel());
        assertEquals(model.getTableColumnModel(), table.getColumnModel());

        InputMap inputMap = table.getInputMap();
        for (KeyStroke keyStroke : model.getExpandKeys()) {
            assertEquals(TreeTableModel.EXPAND_NODE_KEYSTROKE_KEY, inputMap.get(keyStroke));
        }
        for (KeyStroke keyStroke : model.getCollapseKeys()) {
            assertEquals(TreeTableModel.COLLAPSE_NODE_KEYSTROKE_KEY, inputMap.get(keyStroke));
        }
        for (KeyStroke keyStroke : model.getToggleKeys()) {
            assertEquals(TreeTableModel.TOGGLE_EXPAND_NODE_KEYSTROKE_KEY, inputMap.get(keyStroke));
        }

        ActionMap actionMap = table.getActionMap();
        assertEquals(TreeTableModel.TreeTableExpandAction.class, actionMap.get(TreeTableModel.EXPAND_NODE_KEYSTROKE_KEY).getClass());
        assertEquals(TreeTableModel.TreeTableCollapseAction.class, actionMap.get(TreeTableModel.COLLAPSE_NODE_KEYSTROKE_KEY).getClass());
        assertEquals(TreeTableModel.TreeTableToggleExpandAction.class, actionMap.get(TreeTableModel.TOGGLE_EXPAND_NODE_KEYSTROKE_KEY).getClass());
    }

    /* *****************************************************************************************************************
     *                                          Test build static tree
     */
    @Test
    public void testStaticTreeBuild() {
        TestTreeTableModel.TestObject rootObject = new TestTreeTableModel.TestObject("root", 0, true);
        TestTreeTableModel.TestObject child1 = new TestTreeTableModel.TestObject("child1", 1, false);
        TestTreeTableModel.TestObject child2 = new TestTreeTableModel.TestObject("child2", 2, true);

        //TODO: add sub in.

        TestTreeTableModel.TestObject child3 = new TestTreeTableModel.TestObject("child3", 3, false);
        rootObject.getChildren().add(child1);
        rootObject.getChildren().add(child2);
        rootObject.getChildren().add(child3);
        DefaultMutableTreeNode node = TreeUtils.buildTree(rootObject, parent -> ((TestTreeTableModel.TestObject) parent).getChildren());
        assertEquals(rootObject, node.getUserObject());
        assertEquals(child1, ((DefaultMutableTreeNode) node.getChildAt(0)).getUserObject());
        assertEquals(child2, ((DefaultMutableTreeNode) node.getChildAt(1)).getUserObject());
        assertEquals(child3, ((DefaultMutableTreeNode) node.getChildAt(2)).getUserObject());
    }

    //TODO: test different tree building methods with expand collapse... look for places where the difference between
    //      what is visible and what is in the tree exists, e.g. expand collapse listeners who might alter tree structure.

    /* *****************************************************************************************************************
     *                                         Default methods which subclasses may override
     */

    @Test
    public void testDefaultNullIcon() {
        assertNull(model.getNodeIcon(null));
        assertNull(model.getNodeIcon(rootNode));
    }

    @Test
    public void testIsCellEditableAlwaysFalse() {
        for (int row = 0; row < 10; row++) {
            for (int column = 0; column < 3; column++) {
                assertFalse(model.isCellEditable(0, 1));
            }
        }
    }

    @Test
    public void testColumnClassIsObjectByDefault() {
        for (int column = 0; column < 3; column++) {
            assertEquals(Object.class, model.getColumnClass(column));
        }
    }

    @Test
    public void testColumnComparatorNullByDefault() {
        for (int column = 0; column < 2; column++) {
            assertNull(model.getColumnComparator(column));
        }
        assertNotNull(model.getColumnComparator(2)); // column 2 has a custom comparator.
    }

    /* *****************************************************************************************************************
     *                                          Grouping - node comparators
     */
    @Test
    public void testGetSetNodeComparator() {
        assertNull(model.getGroupingComparator());
        model.setGroupingComparator(Comparators.ALLOWS_CHILDREN);
        assertEquals(Comparators.ALLOWS_CHILDREN, model.getGroupingComparator());
        model.setGroupingComparator(null);
        assertNull(model.getGroupingComparator());
    }

    @Test
    public void testClearGroupingComparator() {
        assertNull(model.getGroupingComparator());
        model.setGroupingComparator(Comparators.ALLOWS_CHILDREN);
        assertEquals(Comparators.ALLOWS_CHILDREN, model.getGroupingComparator());
        model.clearGroupingComparator();
        assertNull(model.getGroupingComparator());
    }

    @Test
    public void testGroupsByAllowsChildrenNodeComparatorUnsorted() {
        assertNull(model.getGroupingComparator());
        model.bindTable(table);
        testGroupsByAllowsChildrenNodeComparator();
    }

    @Test
    public void testGroupsByAllowsChildrenNodeComparatorSorted() {
        assertNull(model.getGroupingComparator());
        model.bindTable(table);
        model.setDefaultSortKeys(sortKey1);
        testGroupsByAllowsChildrenNodeComparator();
    }

    private void testGroupsByAllowsChildrenNodeComparator() {
        /*   model order         sorted ascending     grouped by allows children.
         * 0 root                root                 root
         * 1  child0               child0               child1
         * 2  child1               child1                 subchildren0
         * 3     subchildren0        subchildren0         subchildren1
         * 4     subchildren1        subchildren1         subchildren2
         * 5     subchildren2        subchildren2         subchildren3
         * 6     subchildren3        subchildren3       child0
         * 7  child2               child2               child2
         */

        // expand all nodes with children:
        model.expandNode(rootNode);
        model.expandNode(child1);

        // test nodes are in correct table rows (table rows will be different to model rows if sorted or grouped).
        assertEquals(rootNode, model.getNodeAtTableRow(0));
        assertEquals(child0, model.getNodeAtTableRow(1));
        assertEquals(child1, model.getNodeAtTableRow(2));
        assertEquals(subchild0, model.getNodeAtTableRow(3));
        assertEquals(subchild1, model.getNodeAtTableRow(4));
        assertEquals(subchild2, model.getNodeAtTableRow(5));
        assertEquals(subchild3, model.getNodeAtTableRow(6));
        assertEquals(child2, model.getNodeAtTableRow(7));

        model.setGroupingComparator(Comparators.ALLOWS_CHILDREN);

        assertEquals(rootNode, model.getNodeAtTableRow(0));
        assertEquals(child1, model.getNodeAtTableRow(1));
        assertEquals(subchild0, model.getNodeAtTableRow(2));
        assertEquals(subchild1, model.getNodeAtTableRow(3));
        assertEquals(subchild2, model.getNodeAtTableRow(4));
        assertEquals(subchild3, model.getNodeAtTableRow(5));
        assertEquals(child0, model.getNodeAtTableRow(6));
        assertEquals(child2, model.getNodeAtTableRow(7));
    }

    /* *****************************************************************************************************************
     *                                          TableModel interface methods
     */

    //TODO: test row count with root showing or not showing.

    @Test
    public void testGetRowCount() {
        /*   model order
         * 0 root
         * 1  child0
         * 2  child1
         * 3     subchildren0
         * 4     subchildren1
         * 5     subchildren2
         * 6     subchildren3
         * 7  child2
         */

        assertEquals(1, model.getRowCount(), "root only");

        model.expandNode(child1);
        assertEquals(1, model.getRowCount(), "expanding a child which isn't visible has no effect");

        model.expandNode(rootNode);
        assertEquals(8, model.getRowCount(), "all nodes now visible");

        model.collapseNode(child1);
        assertEquals(4, model.getRowCount(), "just root and 3 children");
    }

    @Test
    public void testGetRowCountWithTable() {
        model.bindTable(table);
        testGetRowCount();
    }

    @Test
    public void testGetRowCountWithSortedTable() {
        model.setSortKeys(sortKey1);
        testGetRowCount();
    }

    @Test
    public void testGetRowCountWithGroupedTable() {
        model.setGroupingComparator(Comparators.ALLOWS_CHILDREN);
        model.bindTable(table);
        testGetRowCount();
    }

    @Test
    public void testGetRowCountWithSortedGroupedTable() {
        model.setGroupingComparator(Comparators.ALLOWS_CHILDREN);
        model.bindTable(table);
        model.setSortKeys(sortKey1);
        testGetRowCount();
    }

    @Test
    public void testGetColumnCount() {
        TestTreeTableModel model = new TestTreeTableModel(rootNode, true);
        assertEquals(5, model.getColumnCount());
        model.bindTable(table);
        assertEquals(5, model.getColumnCount());

        model.unbindTable();
        model = new TestTreeTableModel(rootNode, true, 10);
        assertEquals(10, model.getColumnCount());

        model.bindTable(table);
        assertEquals(10, model.getColumnCount());

        TableColumnModel tcol = table.getColumnModel();
        TableColumn col = tcol.getColumn(1);
        tcol.removeColumn(col);

        assertEquals(9, model.getColumnCount());
        model.unbindTable();
        assertEquals(9, model.getColumnCount());
    }

    @Test
    public void  testGetValueAt() {
        testGetValueAt(false);
        testGetValueAt(true);
    }

    private void testGetValueAt(boolean showRoot) {
        /*   model order         sorted ascending     grouped by allows children.       Column Values:
         * 0 root                root                 root                              0        true
         * 1  child0               child0               child1                          101      false
         * 2  child1               child1                 subchildren0                  1000     true
         * 3     subchildren0        subchildren0         subchildren1                  1001     false
         * 4     subchildren1        subchildren1         subchildren2                  1002     true
         * 5     subchildren2        subchildren2         subchildren3                  1003     false
         * 6     subchildren3        subchildren3       child0                          100      true
         * 7  child2               child2               child2                          102      true
         */

        model = new TestTreeTableModel(rootNode, showRoot);
        model.bindTable(table);
        int root = showRoot? 1 : 0;
        if (showRoot) {
            String[] expectedDescriptions = {"root"};
            int[] expectedSizes = {0};
            assertEquals(1, model.getRowCount());
            testGetValuesAt(expectedDescriptions, expectedSizes, true);
        } else {
            String[] expectedDescriptions = {"root", "child0", "child1", "child2"};
            int[] expectedSizes = {0, 100, 101, 102};
            assertEquals(3, model.getRowCount());
            testGetValuesAt(expectedDescriptions, expectedSizes, false); // will ignore first "root" item of expected.
        }

        // Expand children of root
        model.expandNode(rootNode);
        assertEquals(3 + root, model.getRowCount());
        String[] expectedDescriptions = {"root", "child0", "child1", "child2"};
        int[] expectedSizes = {0, 100, 101, 102};
        testGetValuesAt(expectedDescriptions, expectedSizes, showRoot);

        // Expand sub-children of child1
        model.expandNode(child1);
        assertEquals(7 + root, model.getRowCount());
        expectedDescriptions = new String[] {"root", "child0", "child1", "subchildren0", "subchildren1", "subchildren2", "subchildren3", "child2"};
        expectedSizes = new int[] {0, 100, 101, 1000, 1001, 1002, 1003, 102};
        testGetValuesAt(expectedDescriptions, expectedSizes, showRoot);

        // Use grouping to sort the nodes - nothing should change as getValueAt is in terms of model index, not sorted table index.
        model.setGroupingComparator(Comparators.HAS_CHILDREN);
        assertEquals(7 + root, model.getRowCount());
        testGetValuesAt(expectedDescriptions, expectedSizes, showRoot);

        // Set an ascending sort key - shouldn't change as ascending order sort is same as model index order.
        model.setSortKeys(sortKey1);
        assertEquals(7 + root, model.getRowCount());
        testGetValuesAt(expectedDescriptions, expectedSizes, showRoot);

        // Set a descending sort key - order of children changes, grouping is still operating.
        model.setSortKeys(sortKey2);
        assertEquals(7 + root, model.getRowCount());
        testGetValuesAt(expectedDescriptions, expectedSizes, showRoot);

        // get rid of grouping, keep descending sort:
        model.setGroupingComparator(null);
        assertEquals(7 + root, model.getRowCount());
        testGetValuesAt(expectedDescriptions, expectedSizes, showRoot);

        // collapse child1 children, still with descending sort:
        model.collapseNode(child1);
        assertEquals(3 + root, model.getRowCount());
        expectedDescriptions = new String[] {"root", "child0", "child1", "child2"};
        expectedSizes = new int[] {0, 100, 101, 102};
        testGetValuesAt(expectedDescriptions, expectedSizes, showRoot);
    }

    private void testGetValuesAt(String[] expectedDescriptions, int[] expectedSizes, boolean showRoot) {
        int ignoreRootOffset = showRoot? 0 : 1;
        for (int row = 0; row < model.getRowCount(); row++) {
            assertEquals(expectedDescriptions[row + ignoreRootOffset], model.getValueAt(row, 0), "row" + row);
            assertEquals(expectedSizes[row + ignoreRootOffset], (long) model.getValueAt(row, 1), "row" + row);
            assertEquals(expectedSizes[row + ignoreRootOffset] % 2 == 0, model.getValueAt(row, 2), "row" + row);
        }
    }

    @Test
    public void  testSetValueAt() {
        testSetValueAt(false);
        testSetValueAt(true);
    }

    private void testSetValueAt(boolean showRoot) {
        /*   model order         sorted ascending     grouped by allows children.       Column Values:
         * 0 root                root                 root                              0        true
         * 1  child0               child0               child1                          101      false
         * 2  child1               child1                 subchildren0                  1000     true
         * 3     subchildren0        subchildren0         subchildren1                  1001     false
         * 4     subchildren1        subchildren1         subchildren2                  1002     true
         * 5     subchildren2        subchildren2         subchildren3                  1003     false
         * 6     subchildren3        subchildren3       child0                          100      true
         * 7  child2               child2               child2                          102      true
         */

        model = new TestTreeTableModel(rootNode, showRoot);
        model.bindTable(table);
        int root = showRoot? 1 : 0;
        if (showRoot) {
            String[] expectedDescriptions = {"root"};
            long[] expectedSizes = {0};
            assertEquals(1, model.getRowCount());
            testSetValuesAt(expectedDescriptions, expectedSizes, true);
        } else {
            String[] expectedDescriptions = {"root", "child0", "child1", "child2"};
            long[] expectedSizes = {0, 100, 101, 102};
            assertEquals(3, model.getRowCount());
            testSetValuesAt(expectedDescriptions, expectedSizes, false); // will ignore first "root" item of expected.
        }

        // Expand children of root
        model.expandNode(rootNode);
        assertEquals(3 + root, model.getRowCount());
        String[] expectedDescriptions = {"root", "child0", "child1", "child2"};
        long[] expectedSizes = {0, 100, 101, 102};
        testSetValuesAt(expectedDescriptions, expectedSizes, showRoot);

        // Expand sub-children of child1
        model.expandNode(child1);
        assertEquals(7 + root, model.getRowCount());
        expectedDescriptions = new String[] {"root", "child0", "child1", "subchildren0", "subchildren1", "subchildren2", "subchildren3", "child2"};
        expectedSizes = new long[] {0, 100, 101, 1000, 1001, 1002, 1003, 102};
        testSetValuesAt(expectedDescriptions, expectedSizes, showRoot);

        // Use grouping to sort the nodes:
        model.setGroupingComparator(Comparators.ALLOWS_CHILDREN);
        assertEquals(7 + root, model.getRowCount());
        testSetValuesAt(expectedDescriptions, expectedSizes, showRoot);

        // Set an ascending sort key - shouldn't change as ascending order sort is same as model index order.
        model.setSortKeys(sortKey1);
        assertEquals(7 + root, model.getRowCount());
        testSetValuesAt(expectedDescriptions, expectedSizes, showRoot);

        // Set a descending sort key - order of children changes, grouping is still operating.
        model.setSortKeys(sortKey2);
        assertEquals(7 + root, model.getRowCount());
        testSetValuesAt(expectedDescriptions, expectedSizes, showRoot);

        // get rid of grouping, keep descending sort:
        model.setGroupingComparator(null);
        assertEquals(7 + root, model.getRowCount());
        testSetValuesAt(expectedDescriptions, expectedSizes, showRoot);

        // collapse child1 children, still with descending sort:
        model.collapseNode(child1);
        assertEquals(3 + root, model.getRowCount());
        expectedDescriptions = new String[] {"root", "child0", "child1", "child2"};
        expectedSizes = new long[] {0, 100, 101, 102};
        testSetValuesAt(expectedDescriptions, expectedSizes, showRoot);
    }

    private void testSetValuesAt(String[] expectedDescriptions, long[] expectedSizes, boolean showRoot) {
        int ignoreRootOffset = showRoot? 0 : 1;
        for (int row = 0; row < model.getRowCount(); row++) {
            Object currentValue =  model.getValueAt(row, 0);
            assertEquals(expectedDescriptions[row + ignoreRootOffset], currentValue , "row" + row);
            model.setValueAt(Integer.toString(row), row, 0);
            assertEquals(Integer.toString(row), model.getValueAt(row, 0), "row" + row);
            model.setValueAt(currentValue, row, 0); // reset back to old value so next tests work.

            currentValue =  model.getValueAt(row, 1);
            assertEquals(expectedSizes[row + ignoreRootOffset], (long) currentValue, "row" + row);
            model.setValueAt((long) row, row, 1);
            assertEquals((long) row, model.getValueAt(row, 1), "row" + row);
            model.setValueAt(currentValue, row, 1); // reset back to old value so next tests work.

            currentValue =  model.getValueAt(row, 2);
            assertEquals(expectedSizes[row + ignoreRootOffset] % 2 == 0, model.getValueAt(row, 2), "row" + row);
            model.setValueAt(expectedSizes[row + ignoreRootOffset] % 2 == 1, row, 2);
            assertEquals(expectedSizes[row + ignoreRootOffset] % 2 == 1, model.getValueAt(row, 2), "row" + row);
            model.setValueAt(currentValue, row, 2); // reset back to old value so next tests work.
        }
    }

    @Test
    public void testGetSetGroupingComparator() {
        createRandomTree(0, true);
        model.bindTable(table);
        model.expandTree();
        List<TreeNode> allNodesInVisualOrder = TreeUtils.getNodeList(rootNode); // gets all tree nodes depth first (visual order)

        // By default, no grouping is set.
        assertFalse(model.isGrouping());
        assertNull(model.getGroupingComparator());
        assertTrue(tableOrderMatches(allNodesInVisualOrder));

        model.setGroupingComparator(Comparators.NUM_CHILDREN_DESCENDING);
        assertTrue(model.isGrouping());
        assertEquals(Comparators.NUM_CHILDREN_DESCENDING, model.getGroupingComparator());
        assertFalse(tableOrderMatches(allNodesInVisualOrder));

        model.setGroupingComparator(Comparators.HAS_CHILDREN);
        assertTrue(model.isGrouping());
        assertEquals(Comparators.HAS_CHILDREN, model.getGroupingComparator());
        assertFalse(tableOrderMatches(allNodesInVisualOrder));

        model.setGroupingComparator(null);
        assertFalse(model.isGrouping());
        assertNull(model.getGroupingComparator());
        assertTrue(tableOrderMatches(allNodesInVisualOrder));
    }

    @Test
    public void testGroupingWorks() {
        for (int trial = 0; trial < 10; trial++) {
            createRandomTree(trial, true);
            model.expandTree();
            model.bindTable(table);
            model.setGroupingComparator(Comparators.NUM_CHILDREN_DESCENDING);
            testSorting(rootNode, Comparators.NUM_CHILDREN_DESCENDING);

            model.setGroupingComparator(Comparators.NUM_CHILDREN);
            testSorting(rootNode, Comparators.NUM_CHILDREN);
        }
    }

    private void testSorting(TreeNode parentNode, Comparator<TreeNode> nodeComparator) {
        if (parentNode.getChildCount() > 0) {

            // get children in same order as should be displayed in tree when sorted.
            List<TreeNode> children = TreeUtils.getChildren(parentNode);
            children.sort(nodeComparator);

            // For each child, validate that the table row is greater than the preceding one:
            int lastIndex = -1;
            for (TreeNode child : children) {
                final int modelIndex = model.getModelIndexForTreeNode(child);
                final int tableRow = table.convertRowIndexToView(modelIndex);
                if (tableRow <= lastIndex) {
                    System.out.println("oops");
                }
                assertTrue(tableRow > lastIndex);
                lastIndex = tableRow;

                // Test the sorting of the child's children:
                testSorting(child, nodeComparator);
            }
        }
    }

    private boolean tableOrderMatches(List<TreeNode> nodeList) {
        assertEquals(nodeList.size(), model.getRowCount());
        for (int i = 0; i < nodeList.size(); i++) {
            TreeNode listNode = nodeList.get(i);
            TreeNode tableNode = model.getNodeAtTableRow(i);
            if (listNode != tableNode) {
                return false;
            }
        }
        return true;
    }

    @Test
    public void testGetSetSortKeysWithBoundTable() {
        createRandomTree(0, true);
        model.bindTable(table);
        model.expandTree();
        testGetSetSortKeys(true);
    }

    @Test
    public void testGetSetSortKeysWithBoundTableWithNoRowSorter() {
        createRandomTree(0, true);
        model.bindTable(table, (RowSorter<TreeTableModel>) null);
        model.expandTree();
        testGetSetSortKeys(false);
    }

    @Test
    public void testSetSortEnabled() {
        model.bindTable(table);
        assertNotNull(table.getRowSorter());
        model.setSortEnabled(false);
        assertNull(table.getRowSorter());
        model.setSortEnabled(true);
        assertNotNull(table.getRowSorter());
        assertEquals(TreeTableRowSorter.class, table.getRowSorter().getClass());
    }

    @Test
    public void testSetColumnSortStrategy() {
        model.bindTable(table);
        TreeTableRowSorter.ColumnSortStrategy sortStrategy =
                new TreeTableColumnSortStrategy(5, TreeTableColumnSortStrategy.NewSortKeyPosition.ADD_TO_END,
                                                    TreeTableColumnSortStrategy.UpdateSortKeyPosition.MAKE_FIRST,
                                                    TreeTableColumnSortStrategy.RemoveSortKeyAction.REMOVE_ALL);
        model.setColumnSortStrategy(sortStrategy);
        TreeTableRowSorter.ColumnSortStrategy existing = ((TreeTableRowSorter) table.getRowSorter()).getSortStrategy();
        assertEquals(sortStrategy, existing);
    }

    /**
     * getting and setting sorting should be the same.
     */
    @Test
    public void testGetSetSortKeysWithNoTable() {
        createRandomTree(0, true);
        model.expandTree();
        testGetSetSortKeys(false);
    }

    @Test
    public void testGetSetSortKeysAfterUnbind() {
        createRandomTree(0, true);
        model.setSortKeys(sortKey1);
        model.bindTable(table);
        model.expandTree();

        // Set a sort key on the row sorter directly, bypassing the model interface (so the model cannot cache the value).
        table.getRowSorter().setSortKeys(Arrays.asList(sortKey3, sortKey2));

        // Unbind the table.
        model.unbindTable();

        // We should still have the sort key set on the rowsorter directly (the last active sort key in use).
        List<? extends RowSorter.SortKey> sortKeys = model.getSortKeys();
        assertEquals(2, sortKeys.size());
        assertEquals(sortKey3, sortKeys.get(0));
        assertEquals(sortKey2, sortKeys.get(1));
    }

    private void testGetSetSortKeys(boolean isSorting) {
        List<TreeNode> allNodesInVisualOrder = TreeUtils.getNodeList(rootNode); // gets all tree nodes depth first (visual order)

        assertFalse(model.isSorting());
        assertTrue(tableOrderMatches(allNodesInVisualOrder));
        List<? extends RowSorter.SortKey> sortKeys = model.getSortKeys();
        assertTrue(sortKeys.isEmpty());

        model.setSortKeys(sortKey1);
        assertEquals(isSorting, model.isSorting());
        assertNotEquals(isSorting, tableOrderMatches(allNodesInVisualOrder)); // the table order will only change if there is a table bound.
        sortKeys = model.getSortKeys();
        assertEquals(1, sortKeys.size());
        assertEquals(sortKey1, sortKeys.get(0));

        model.clearSortKeys();
        assertFalse(model.isSorting());
        sortKeys = model.getSortKeys();
        assertTrue(sortKeys.isEmpty());

        model.setSortKeys(sortKey2, sortKey1);
        assertEquals(isSorting, model.isSorting());
        assertNotEquals(isSorting, tableOrderMatches(allNodesInVisualOrder)); // the table order will only change if there is a table bound.
        sortKeys = model.getSortKeys();
        assertEquals(2, sortKeys.size());
        assertEquals(sortKey2, sortKeys.get(0));
        assertEquals(sortKey1, sortKeys.get(1));

        model.setSortKeys(sortKeyList);
        assertEquals(isSorting, model.isSorting());
        assertNotEquals(isSorting, tableOrderMatches(allNodesInVisualOrder)); // the table order will only change if there is a table bound.
        sortKeys = model.getSortKeys();
        assertEquals(sortKeyList.size(), sortKeys.size());
        for (int i = 0; i < sortKeyList.size(); i++) {
            assertEquals(sortKeyList.get(i), sortKeys.get(i));
        }

        model.setSortKeys(Collections.emptyList());
        assertFalse(model.isSorting());
        sortKeys = model.getSortKeys();
        assertTrue(sortKeys.isEmpty());
    }

    @Test
    public void testSortingWorks() {
        for (int trial = 0; trial < 10; trial++) {
            createRandomTree(trial, true);
            model.expandTree();
            model.bindTable(table);

            model.setSortKeys(sortKey1);
            Comparator<TreeNode> comparator = new SortComparator(sortKey1);
            testSorting(rootNode, comparator);

            model.setSortKeys(sortKey3, sortKey2);
            comparator = new SortComparator(sortKey3, sortKey2);
            testSorting(rootNode, comparator);

            model.setSortKeys(sortKeyList);
            comparator = new SortComparator(sortKeyList);
            testSorting(rootNode, comparator);
        }
    }


    @Test
    public void testGetSetClearFilteringNoTable() {
        assertFalse(model.isFiltering());
        assertNull(model.getNodeFilter());
        Predicate<TreeNode> predicate = treeNode -> treeNode.getAllowsChildren();
        model.setNodeFilter(predicate);
        assertTrue(model.isFiltering());
        assertEquals(predicate, model.getNodeFilter());
        model.clearNodeFilter();
        assertFalse(model.isFiltering());
        assertNull(model.getNodeFilter());
    }

    @Test
    public void testGetSetClearFilteringWithTable() {
        model.bindTable(table);
        assertFalse(model.isFiltering());
        assertNull(model.getNodeFilter());
        Predicate<TreeNode> predicate = treeNode -> treeNode.getAllowsChildren();
        model.setNodeFilter(predicate);
        assertTrue(model.isFiltering());
        assertEquals(predicate, model.getNodeFilter());
        model.clearNodeFilter();
        assertFalse(model.isFiltering());
        assertNull(model.getNodeFilter());
    }

    @Test
    public void testFilterVisibleRootNodeNoTable() {
        model.setShowRoot(true);

        // By default, root is unexpanded so is the only node visible in the tree:
        assertEquals(1, model.getRowCount());
        assertEquals(rootNode, model.getNodeAtModelIndex(0));

        // Filtering the root node out leaves us nothing visible in the model:
        Predicate<TreeNode> rootFilter = treeNode -> {
            TestTreeTableModel.TestObject obj = TreeUtils.getUserObject(treeNode);
            return obj.description == "root";
        };
        assertFalse(model.isFiltered(rootNode));
        model.setNodeFilter(rootFilter);
        assertTrue(model.isFiltered(rootNode));
        assertEquals(0, model.getRowCount());

        // Clearing the filter returns us to the original tree:
        model.clearNodeFilter();
        assertEquals(1, model.getRowCount());
        assertEquals(rootNode, model.getNodeAtModelIndex(0));
    }

    @Test
    public void testFilterVisibleRootNodeWithTable() {
        model.bindTable(table);
        model.setShowRoot(true);

        // By default, root is unexpanded so is the only node visible in the tree:
        assertEquals(1, table.getRowCount());
        assertEquals(rootNode, model.getNodeAtTableRow(0));

        // Filtering the root node out leaves us nothing visible in the model:
        Predicate<TreeNode> rootFilter = treeNode -> {
            TestTreeTableModel.TestObject obj = TreeUtils.getUserObject(treeNode);
            return obj.description == "root";
        };
        assertFalse(model.isFiltered(rootNode));
        model.setNodeFilter(rootFilter);
        assertTrue(model.isFiltered(rootNode));
        assertEquals(0, table.getRowCount());

        // Clearing the filter returns us to the original tree:
        model.clearNodeFilter();
        assertEquals(1, table.getRowCount());
        assertEquals(rootNode, model.getNodeAtTableRow(0));
    }

    @Test
    public void testFilterHiddenRootNodeNoTable() {
        // An unexpanded hidden root will have nothing visible in the tree.
        model.setShowRoot(false);
        assertEquals(3, model.getRowCount());
        assertEquals(child0, model.getNodeAtTableRow(0));
        assertEquals(child1, model.getNodeAtModelIndex(1));
        assertEquals(child2, model.getNodeAtModelIndex(2));

        // Filtering the root node out has no effect (as it is not visible):
        Predicate<TreeNode> rootFilter = treeNode -> {
            TestTreeTableModel.TestObject obj = TreeUtils.getUserObject(treeNode);
            return obj.description.equals("root");
        };
        assertFalse(model.isFiltered(rootNode));
        model.setNodeFilter(rootFilter);
        assertTrue(model.isFiltered(rootNode));

        // Filtering a hidden root doesn't change the model.
        assertEquals(3, model.getRowCount());
        assertEquals(child0, model.getNodeAtModelIndex(0));
        assertEquals(child1, model.getNodeAtModelIndex(1));
        assertEquals(child2, model.getNodeAtModelIndex(2));

        // Clearing the filter again has no effect:
        model.clearNodeFilter();
        assertEquals(3, model.getRowCount());
        assertEquals(child0, model.getNodeAtModelIndex(0));
        assertEquals(child1, model.getNodeAtModelIndex(1));
        assertEquals(child2, model.getNodeAtModelIndex(2));
    }

    @Test
    public void testFilterHiddenRootNodeWithTable() {
        model.bindTable(table);

        // An unexpanded hidden root will have nothing visible in the tree.
        model.setShowRoot(false);
        assertEquals(3, table.getRowCount());
        assertEquals(child0, model.getNodeAtTableRow(0));
        assertEquals(child1, model.getNodeAtTableRow(1));
        assertEquals(child2, model.getNodeAtTableRow(2));

        // Filtering the root node out has no effect (as it is not visible):
        Predicate<TreeNode> rootFilter = treeNode -> {
            TestTreeTableModel.TestObject obj = TreeUtils.getUserObject(treeNode);
            return obj.description.equals("root");
        };
        assertFalse(model.isFiltered(rootNode));
        model.setNodeFilter(rootFilter);
        assertTrue(model.isFiltered(rootNode));

        // Filtering a hidden root doesn't change the model.
        assertEquals(3, table.getRowCount());
        assertEquals(child0, model.getNodeAtTableRow(0));
        assertEquals(child1, model.getNodeAtTableRow(1));
        assertEquals(child2, model.getNodeAtTableRow(2));

        // Clearing the filter again has no effect:
        model.clearNodeFilter();
        assertEquals(3, table.getRowCount());
        assertEquals(child0, model.getNodeAtTableRow(0));
        assertEquals(child1, model.getNodeAtTableRow(1));
        assertEquals(child2, model.getNodeAtTableRow(2));
    }

    @Test
    public void testFilterAltersBoundTableCorrectlyShowRoot() {
       model.bindTable(table);
       model.expandTree();
       assertEquals(8, model.getRowCount());
       assertEquals(8, table.getRowCount());

        // Filtering out all the subchildren of child2:
        Predicate<TreeNode> filter = treeNode -> {
            TestTreeTableModel.TestObject obj = TreeUtils.getUserObject(treeNode);
            return obj.size > 999;
        };
        model.setNodeFilter(filter);

        assertEquals(4, model.getRowCount());
        assertEquals(4, table.getRowCount());
        assertEquals(rootNode, model.getNodeAtModelIndex(0));
        assertEquals(rootNode, model.getNodeAtTableRow(0));
        assertEquals(child0, model.getNodeAtModelIndex(1));
        assertEquals(child0, model.getNodeAtTableRow(1));
        assertEquals(child1, model.getNodeAtModelIndex(2));
        assertEquals(child1, model.getNodeAtTableRow(2));
        assertEquals(child2, model.getNodeAtModelIndex(3));
        assertEquals(child2, model.getNodeAtTableRow(3));

        filter = treeNode -> {
            TestTreeTableModel.TestObject obj = TreeUtils.getUserObject(treeNode);
            return obj.size > 99 && obj.size < 999;
        };
        model.setNodeFilter(filter);

        // If we filter out the children of the root, its subchildren will also be filtered out, leaving only the root.
        assertEquals(1, model.getRowCount());
        assertEquals(1, table.getRowCount());
        assertEquals(rootNode, model.getNodeAtModelIndex(0));
        assertEquals(rootNode, model.getNodeAtTableRow(0));

        filter = treeNode -> {
            TestTreeTableModel.TestObject obj = TreeUtils.getUserObject(treeNode);
            return !obj.enabled;
        };
        model.setNodeFilter(filter);

        // If we filter out all the unenabled children, we have root and two child objects.
        // the subchildren are also filtered out as this node is not enabled.
        assertEquals(3, model.getRowCount());
        assertEquals(3, table.getRowCount());
        assertEquals(rootNode, model.getNodeAtModelIndex(0));
        assertEquals(rootNode, model.getNodeAtTableRow(0));
        assertEquals(child0, model.getNodeAtModelIndex(1));
        assertEquals(child0, model.getNodeAtTableRow(1));
        assertEquals(child2, model.getNodeAtModelIndex(2));
        assertEquals(child2, model.getNodeAtTableRow(2));

        // Filtering out all the nodes whose description ends with 2:
        filter = treeNode -> {
            TestTreeTableModel.TestObject obj = TreeUtils.getUserObject(treeNode);
            return obj.description.endsWith("2");
        };
        model.setNodeFilter(filter);

        assertEquals(6, model.getRowCount());
        assertEquals(6, table.getRowCount());
        assertEquals(rootNode, model.getNodeAtModelIndex(0));
        assertEquals(rootNode, model.getNodeAtTableRow(0));
        assertEquals(child0, model.getNodeAtModelIndex(1));
        assertEquals(child0, model.getNodeAtTableRow(1));
        assertEquals(child1, model.getNodeAtModelIndex(2));
        assertEquals(child1, model.getNodeAtTableRow(2));
        assertEquals(subchild0, model.getNodeAtModelIndex(3));
        assertEquals(subchild0, model.getNodeAtTableRow(3));
        assertEquals(subchild1, model.getNodeAtModelIndex(4));
        assertEquals(subchild1, model.getNodeAtTableRow(4));
        assertEquals(subchild3, model.getNodeAtModelIndex(5));
        assertEquals(subchild3, model.getNodeAtTableRow(5));

    }

    @Test
    public void testFilterAltersBoundTableCorrectlyHiddenRoot() {
        model.setShowRoot(false);
        model.bindTable(table);
        model.expandTree();
        assertEquals(7, model.getRowCount());
        assertEquals(7, table.getRowCount());

        // Filtering out all the subchildren of child2:
        Predicate<TreeNode> filter = treeNode -> {
            TestTreeTableModel.TestObject obj = TreeUtils.getUserObject(treeNode);
            return obj.size > 999;
        };
        model.setNodeFilter(filter);

        assertEquals(3, model.getRowCount());
        assertEquals(3, table.getRowCount());
        assertEquals(child0, model.getNodeAtModelIndex(0));
        assertEquals(child0, model.getNodeAtTableRow(0));
        assertEquals(child1, model.getNodeAtModelIndex(1));
        assertEquals(child1, model.getNodeAtTableRow(1));
        assertEquals(child2, model.getNodeAtModelIndex(2));
        assertEquals(child2, model.getNodeAtTableRow(2));

        filter = treeNode -> {
            TestTreeTableModel.TestObject obj = TreeUtils.getUserObject(treeNode);
            return obj.size > 99 && obj.size < 999;
        };
        model.setNodeFilter(filter);

        // If we filter out the children of the hidden root, the tree will be empty.
        assertEquals(0, model.getRowCount());
        assertEquals(0, table.getRowCount());

        filter = treeNode -> {
            TestTreeTableModel.TestObject obj = TreeUtils.getUserObject(treeNode);
            return !obj.enabled;
        };
        model.setNodeFilter(filter);

        // If we filter out all the unenabled children, we have root and two child objects.
        // the subchildren are also filtered out as this node is not enabled.
        assertEquals(2, model.getRowCount());
        assertEquals(2, table.getRowCount());
        assertEquals(child0, model.getNodeAtModelIndex(0));
        assertEquals(child0, model.getNodeAtTableRow(0));
        assertEquals(child2, model.getNodeAtModelIndex(1));
        assertEquals(child2, model.getNodeAtTableRow(1));

        // Filtering out all the nodes whose description ends with 2:
        filter = treeNode -> {
            TestTreeTableModel.TestObject obj = TreeUtils.getUserObject(treeNode);
            return obj.description.endsWith("2");
        };
        model.setNodeFilter(filter);

        assertEquals(5, model.getRowCount());
        assertEquals(5, table.getRowCount());
        assertEquals(child0, model.getNodeAtModelIndex(0));
        assertEquals(child0, model.getNodeAtTableRow(0));
        assertEquals(child1, model.getNodeAtModelIndex(1));
        assertEquals(child1, model.getNodeAtTableRow(1));
        assertEquals(subchild0, model.getNodeAtModelIndex(2));
        assertEquals(subchild0, model.getNodeAtTableRow(2));
        assertEquals(subchild1, model.getNodeAtModelIndex(3));
        assertEquals(subchild1, model.getNodeAtTableRow(3));
        assertEquals(subchild3, model.getNodeAtModelIndex(4));
        assertEquals(subchild3, model.getNodeAtTableRow(4));
    }

    @Test
    public void testFilterAltersBoundTableCorrectlyShowRootSorted() {
        model.bindTable(table);
        model.expandTree();
        model.setSortKeys(new RowSorter.SortKey(1, SortOrder.DESCENDING));


        assertEquals(8, model.getRowCount());
        assertEquals(8, table.getRowCount());

        // Filtering out all the subchildren of child2:
        Predicate<TreeNode> filter = treeNode -> {
            TestTreeTableModel.TestObject obj = TreeUtils.getUserObject(treeNode);
            return obj.size > 999;
        };
        model.setNodeFilter(filter);

        assertEquals(4, model.getRowCount());
        assertEquals(4, table.getRowCount());
        assertEquals(rootNode, model.getNodeAtModelIndex(0));
        assertEquals(rootNode, model.getNodeAtTableRow(0));
        assertEquals(child0, model.getNodeAtModelIndex(1));
        assertEquals(child2, model.getNodeAtTableRow(1));
        assertEquals(child1, model.getNodeAtModelIndex(2));
        assertEquals(child1, model.getNodeAtTableRow(2));
        assertEquals(child2, model.getNodeAtModelIndex(3));
        assertEquals(child0, model.getNodeAtTableRow(3));

        filter = treeNode -> {
            TestTreeTableModel.TestObject obj = TreeUtils.getUserObject(treeNode);
            return obj.size > 99 && obj.size < 999;
        };
        model.setNodeFilter(filter);

        // If we filter out the children of the root, its subchildren will also be filtered out, leaving only the root.
        assertEquals(1, model.getRowCount());
        assertEquals(1, table.getRowCount());
        assertEquals(rootNode, model.getNodeAtModelIndex(0));
        assertEquals(rootNode, model.getNodeAtTableRow(0));

        filter = treeNode -> {
            TestTreeTableModel.TestObject obj = TreeUtils.getUserObject(treeNode);
            return !obj.enabled;
        };
        model.setNodeFilter(filter);

        // If we filter out all the unenabled children, we have root and two child objects.
        // the subchildren are also filtered out as this node is not enabled.
        assertEquals(3, model.getRowCount());
        assertEquals(3, table.getRowCount());
        assertEquals(rootNode, model.getNodeAtModelIndex(0));
        assertEquals(rootNode, model.getNodeAtTableRow(0));
        assertEquals(child0, model.getNodeAtModelIndex(1));
        assertEquals(child2, model.getNodeAtTableRow(1));
        assertEquals(child2, model.getNodeAtModelIndex(2));
        assertEquals(child0, model.getNodeAtTableRow(2));

        // Filtering out all the nodes whose description ends with 2:
        filter = treeNode -> {
            TestTreeTableModel.TestObject obj = TreeUtils.getUserObject(treeNode);
            return obj.description.endsWith("2");
        };
        model.setNodeFilter(filter);

        assertEquals(6, model.getRowCount());
        assertEquals(6, table.getRowCount());
        assertEquals(rootNode, model.getNodeAtModelIndex(0));
        assertEquals(rootNode, model.getNodeAtTableRow(0));
        assertEquals(child0, model.getNodeAtModelIndex(1));
        assertEquals(child1, model.getNodeAtTableRow(1));
        assertEquals(child1, model.getNodeAtModelIndex(2));
        assertEquals(subchild3, model.getNodeAtTableRow(2));
        assertEquals(subchild0, model.getNodeAtModelIndex(3));
        assertEquals(subchild1, model.getNodeAtTableRow(3));
        assertEquals(subchild1, model.getNodeAtModelIndex(4));
        assertEquals(subchild0, model.getNodeAtTableRow(4));
        assertEquals(subchild3, model.getNodeAtModelIndex(5));
        assertEquals(child0, model.getNodeAtTableRow(5));
    }

    @Test
    public void testFilterAltersBoundTableCorrectlyHiddenRootSorting() {
        //TODO: add sorting.
        model.setShowRoot(false);
        model.bindTable(table);
        model.expandTree();
        model.setSortKey(1, SortOrder.DESCENDING);
        assertEquals(7, model.getRowCount());
        assertEquals(7, table.getRowCount());

        // Filtering out all the subchildren of child2:
        Predicate<TreeNode> filter = treeNode -> {
            TestTreeTableModel.TestObject obj = TreeUtils.getUserObject(treeNode);
            return obj.size > 999;
        };
        model.setNodeFilter(filter);

        assertEquals(3, model.getRowCount());
        assertEquals(3, table.getRowCount());
        assertEquals(child0, model.getNodeAtModelIndex(0));
        assertEquals(child2, model.getNodeAtTableRow(0));
        assertEquals(child1, model.getNodeAtModelIndex(1));
        assertEquals(child1, model.getNodeAtTableRow(1));
        assertEquals(child2, model.getNodeAtModelIndex(2));
        assertEquals(child0, model.getNodeAtTableRow(2));

        filter = treeNode -> {
            TestTreeTableModel.TestObject obj = TreeUtils.getUserObject(treeNode);
            return obj.size > 99 && obj.size < 999;
        };
        model.setNodeFilter(filter);

        // If we filter out the children of the hidden root, the tree will be empty.
        assertEquals(0, model.getRowCount());
        assertEquals(0, table.getRowCount());

        filter = treeNode -> {
            TestTreeTableModel.TestObject obj = TreeUtils.getUserObject(treeNode);
            return !obj.enabled;
        };
        model.setNodeFilter(filter);

        // If we filter out all the unenabled children, we have root and two child objects.
        // the subchildren are also filtered out as this node is not enabled.
        assertEquals(2, model.getRowCount());
        assertEquals(2, table.getRowCount());
        assertEquals(child0, model.getNodeAtModelIndex(0));
        assertEquals(child2, model.getNodeAtTableRow(0));
        assertEquals(child2, model.getNodeAtModelIndex(1));
        assertEquals(child0, model.getNodeAtTableRow(1));

        // Filtering out all the nodes whose description ends with 2:
        filter = treeNode -> {
            TestTreeTableModel.TestObject obj = TreeUtils.getUserObject(treeNode);
            return obj.description.endsWith("2");
        };
        model.setNodeFilter(filter);

        assertEquals(5, model.getRowCount());
        assertEquals(5, table.getRowCount());
        assertEquals(child0, model.getNodeAtModelIndex(0));
        assertEquals(child1, model.getNodeAtTableRow(0));
        assertEquals(child1, model.getNodeAtModelIndex(1));
        assertEquals(subchild3, model.getNodeAtTableRow(1));
        assertEquals(subchild0, model.getNodeAtModelIndex(2));
        assertEquals(subchild1, model.getNodeAtTableRow(2));
        assertEquals(subchild1, model.getNodeAtModelIndex(3));
        assertEquals(subchild0, model.getNodeAtTableRow(3));
        assertEquals(subchild3, model.getNodeAtModelIndex(4));
        assertEquals(child0, model.getNodeAtTableRow(4));
    }


    @Test
    public void testGetSetTableHeaderRenderer() {
        TableCellRenderer previous = table.getTableHeader().getDefaultRenderer();
        assertNull(model.getTableHeaderRenderer()); // table not bound yet.
        model.bindTable(table);
        TableCellRenderer treeHeader = model.getTableHeaderRenderer();
        assertNotEquals(previous, treeHeader);
        assertEquals(TreeTableHeaderRenderer.class, treeHeader.getClass());
        model.unbindTable(); // replaces old renderer.
        assertEquals(previous, table.getTableHeader().getDefaultRenderer());

        model.setTableHeaderRenderer(treeHeader); // does nothing if table not bound.
        assertEquals(previous, table.getTableHeader().getDefaultRenderer());

        // now test setting a renderer with a bound table.
        model.bindTable(table);
        assertNotEquals(previous, treeHeader);
        assertEquals(TreeTableHeaderRenderer.class, treeHeader.getClass());
        model.setTableHeaderRenderer(previous);
        assertEquals(previous, table.getTableHeader().getDefaultRenderer());
    }

    /**
     * Tests that both algorithms for finding the model index of a node in the tree are equivalent.
     * <p>
     * The linear scan algorithm just looks in the visible node array for the node, so is entirely reliable,
     * assuming the model has built the array correctly in the first place of course.  It's generally the fastest
     * algorithm to use in a tree of around 100 nodes or so (simpler and better cache locality),
     * but gets linearly slower the bigger the visible tree is, on average.
     * <p>
     * The tree scan algorithm walks up the path of the node to find from the root, adding up all the children
     * which are visible in the tree before it.  This is efficient because we already store the total visible
     * number of children for each expanded node in a map, so we don't need to walk those sub-trees to get the totals.
     * For more than about 100 nodes in the visible tree, the tree scan is generally faster,
     * as it has to deal with much fewer nodes on average, despite being more complex.  Its performance will only
     * slow down very gradually as the tree gets even bigger - most path lengths and child numbers won't change dramatically
     * even if there are a lot of visible nodes in the tree as a whole.
     * <p>
     * This test takes a long time to run - it creates many random trees, and randomly expands and collapses nodes
     * with them to create very different visible tree structures.  It then selects all the visible
     * nodes in each tree and tries to find them with both algorithms.  Then it selects random nodes from all the nodes
     * in the tree (some of which may not be visible), and ensures that both algorithms give the same result.
     * <p>
     * It has already exposed a few minor edge cases to do with root node visibility in the rest of the code
     * (not the model index algorithms themselves!), since any mismatch between the two highlights bugs in the
     * building of the array of visible nodes.
     */
    @Test
    public void testModelIndexAlgorithmsAreEquivalent() {
        final int numTrials = 10; // increase to really test this - need lots of random trees to really exercise different tree structures.

        // validate basic test tree:
        model.expandNode(rootNode);
        model.expandNode(child1);
        testModelIndexAlgorithmsAreEquivalent(model);

        // validate 100 random trees:
        for (int trial = 0; trial < numTrials; trial++) {
            TreeNode rootNode = buildRandomTree(trial);

            // Test not showing root:
            TreeTableModel testModel = new TestTreeTableModel(rootNode, false);

            testModelIndexAlgorithmsAreEquivalent(testModel);

            //TODO: test filtered tree:

            // Test showing root:
            testModel = new TestTreeTableModel(rootNode, true);
            testModelIndexAlgorithmsAreEquivalent(testModel);

            //TODO: test filtered tree:

        }
    }

    private void testModelIndexAlgorithmsAreEquivalent(TreeTableModel modelToTest) {

        Random random = new Random(0);

        // Test not finding a node which isn't in the tree:
        DefaultMutableTreeNode nodeNotInTree = new DefaultMutableTreeNode();
        assertEquals(-1, modelToTest.getModelIndexForTreeNode(nodeNotInTree));
        assertEquals(-1, modelToTest.getModelIndexLinearScan(nodeNotInTree));
        assertEquals(-1, modelToTest.getModelIndexTreeScan(nodeNotInTree));

        List<TreeNode> allNodes = TreeUtils.getNodeList(modelToTest.getRoot());

        for (int trial = 0; trial < 100; trial++) {

            // Test finding index of nodes which are displayed in the tree:
            for (int i = 0; i < modelToTest.getRowCount(); i++) {
                TreeNode nodeToFind = modelToTest.displayedNodes.get(i); // naughty - go inside, but want to avoid calling model index to test model index.
                int modelIndex1 = modelToTest.getModelIndexLinearScan(nodeToFind);
                int modelIndex2 = modelToTest.getModelIndexTreeScan(nodeToFind);
                assertEquals(i, modelIndex1);
                assertEquals(i, modelIndex2);
            }

            // Test finding random nodes which are somewhere in the tree (whether currently visible or not).
            for (int i = 0; i < 100; i++) {
                TreeNode nodeToFind = allNodes.get(random.nextInt(allNodes.size()));
                int modelIndex1 = modelToTest.getModelIndexLinearScan(nodeToFind);
                int modelIndex2 = modelToTest.getModelIndexTreeScan(nodeToFind);
                assertEquals(modelIndex1, modelIndex2);
            }

            //DEBUG: uncomment to monitor we're getting enough visible nodes with random expansion of tree.
            //System.out.println(modelToTest.getRowCount());

            // Expand and collapse some more folders (will change which nodes are visible in the tree).
            expandAndCollapseRandomNodes(modelToTest, trial, 50, 10);
        }
    }


    /* *****************************************************************************************************************
     *                                          Node visibility
     */

    @Test
    public void testNullNodeNotVisible() {
        assertFalse(model.isVisible(null));
    }

    @Test
    public void testShowRoot() {
        // Start with not showing root
        model = new TestTreeTableModel(rootNode, false);
        assertFalse(model.isVisible(rootNode));
        assertFalse(model.getShowRoot());

        // Set it to show
        model.setShowRoot(true);
        assertTrue(model.isVisible(rootNode));
        assertTrue(model.getShowRoot());

        // Set it not to show
        model.setShowRoot(false);
        assertFalse(model.isVisible(rootNode));
        assertFalse(model.getShowRoot());
    }

    @Test
    public void testIsRootChildVisible() {
        // Not visible if root node is showing, but has not been expanded.
        TreeNode childNode = rootNode.getChildAt(0);
        assertFalse(model.isVisible(childNode)); // root node not expanded.

        // Visible if root is not showing, as it is expanded automatically if not showing.
        model = new TestTreeTableModel(rootNode, false);
        assertTrue(model.isVisible(childNode));
    }

    @Test
    public void testRemovedNodeNotVisibleRootShowing() {
        TreeTableModel model = new TestTreeTableModel(rootNode, true);

        assertFalse(model.isVisible(child0));
        assertFalse(model.isVisible(child1));
        assertFalse(model.isVisible(child2));

        rootNode.remove(1);
        assertFalse(model.isVisible(child0));
        assertFalse(model.isVisible(child1));
        assertFalse(model.isVisible(child2));

        rootNode.remove(1);
        assertFalse(model.isVisible(child0));
        assertFalse(model.isVisible(child1));
        assertFalse(model.isVisible(child2));
    }

    @Test
    public void testRemovedNodeNotVisibleRootHidden() {
        TreeTableModel model = new TestTreeTableModel(rootNode, false);

        assertTrue(model.isVisible(child0));
        assertTrue(model.isVisible(child1));
        assertTrue(model.isVisible(child2));

        rootNode.remove(1);
        assertTrue(model.isVisible(child0));
        assertFalse(model.isVisible(child1));
        assertTrue(model.isVisible(child2));

        rootNode.remove(1);
        assertTrue(model.isVisible(child0));
        assertFalse(model.isVisible(child1));
        assertFalse(model.isVisible(child2));
    }

    @Test
    public void testParentNotExpandedChildNotVisible() {
        model = new TestTreeTableModel(rootNode, true);
        assertTrue(model.isVisible(rootNode));
        assertFalse(model.isVisible(child1));
        assertFalse(model.isVisible(subchild3));

        model.expandNode(child1);
        assertFalse(model.isVisible(subchild3));
    }

    @Test
    public void testParentsExpandedChildVisible() {
        model = new TestTreeTableModel(rootNode, false);
        assertFalse(model.isVisible(rootNode));
        assertTrue(model.isVisible(child1));
        assertFalse(model.isVisible(subchild2));

        model.expandNode(child1);
        assertTrue(model.isVisible(subchild3));
    }


    /* *****************************************************************************************************************
     *                                 Test expansion and collapse
     */

    /**
     * Collapsing a hidden root will make the entire tree disappear, but it is still a legimitate
     * action.  It can only be done programmatically (since an invisible root cannot be selected visually
     * to collapse.
     */
    @Test
    public void testCollapseExpandHiddenRoot() {
        model = new TestTreeTableModel(rootNode, false);
        assertTrue(model.isExpanded(rootNode)); // hidden roots are expanded by default.

        model.collapseNode(rootNode);
        assertFalse(model.isExpanded(rootNode));

        model.expandNode(rootNode);
        assertTrue(model.isExpanded(rootNode));
    }


    @Test
    public void testExpandCollapseShowingRoot() {
        model = new TestTreeTableModel(rootNode, true);
        assertFalse(model.isExpanded(rootNode)); // showing roots are not expanded by default.

        model.expandNode(rootNode);
        assertTrue(model.isExpanded(rootNode));

        model.collapseNode(rootNode);
        assertFalse(model.isExpanded(rootNode));
    }


    @Test
    void testExpandCollapseTreeShowRoot() {
        model = new TestTreeTableModel(rootNode, true);
        assertFalse(model.isExpanded(rootNode)); // showing roots are not expanded by default.

        model.expandTree();
        assertTrue(model.isExpanded(rootNode));
        assertFalse(model.isExpanded(child0));
        assertTrue(model.isExpanded(child1));
        assertFalse(model.isExpanded(child2));

        model.collapseTree();
        assertFalse(model.isExpanded(rootNode)); // showing roots are not expanded by default.
    }

    @Test
    void testExpandCollapseTreeDoNotShowRoot() {
        model = new TestTreeTableModel(rootNode, false);
        assertTrue(model.isExpanded(rootNode)); // root is expanded if it is not showing.

        model.expandTree();
        assertTrue(model.isExpanded(rootNode));
        assertFalse(model.isExpanded(child0));
        assertTrue(model.isExpanded(child1));
        assertFalse(model.isExpanded(child2));

        model.collapseTree();
        assertTrue(model.isExpanded(rootNode)); // showing roots are not expanded by default.
    }

    @Test
    public void testExpandTreeWithDepth() {
        model = new TestTreeTableModel(rootNode, true);
        assertFalse(model.isExpanded(rootNode)); // showing roots are not expanded by default.

        model.expandTree(1);
        assertTrue(model.isExpanded(rootNode));
        assertFalse(model.isExpanded(child0));
        assertFalse(model.isExpanded(child1));
        assertFalse(model.isExpanded(child2));

        model.collapseTree();
        assertFalse(model.isExpanded(rootNode)); // showing roots are not expanded by default.

        model.expandTree(2);
        assertTrue(model.isExpanded(rootNode));
        assertFalse(model.isExpanded(child0));
        assertTrue(model.isExpanded(child1));
        assertFalse(model.isExpanded(child2));
    }

    @Test
    public void testExpandTreeWithPredicate() {
        model = new TestTreeTableModel(rootNode, true);
        assertFalse(model.isExpanded(rootNode)); // showing roots are not expanded by default.

        model.expandTree(treeNode -> {
            TestTreeTableModel.TestObject object = TreeUtils.getUserObject(treeNode);
            return object.description.equals("root");
        });

        assertTrue(model.isExpanded(rootNode));
        assertFalse(model.isExpanded(child0));
        assertFalse(model.isExpanded(child1));
        assertFalse(model.isExpanded(child2));

        model.collapseTree();
        assertFalse(model.isExpanded(rootNode)); // showing roots are not expanded by default.

        model.expandTree(treeNode -> {
            TestTreeTableModel.TestObject object = TreeUtils.getUserObject(treeNode);
            return object.description.equals("root") || object.description.startsWith("child");
        });
        assertTrue(model.isExpanded(rootNode));
        assertFalse(model.isExpanded(child0));
        assertTrue(model.isExpanded(child1));
        assertFalse(model.isExpanded(child2));
    }

    @Test
    public void testExpandTreeWithDepthAndPredicate() {
        model = new TestTreeTableModel(rootNode, true);
        assertFalse(model.isExpanded(rootNode)); // showing roots are not expanded by default.

        model.expandTree(0, treeNode -> {
            TestTreeTableModel.TestObject object = TreeUtils.getUserObject(treeNode);
            return object.description.equals("root") || object.description.startsWith("child");
        });

        assertFalse(model.isExpanded(rootNode));
        assertFalse(model.isExpanded(child0));
        assertFalse(model.isExpanded(child1));
        assertFalse(model.isExpanded(child2));

        model.collapseTree();
        assertFalse(model.isExpanded(rootNode)); // showing roots are not expanded by default.

        model.expandTree(1, treeNode -> {
            TestTreeTableModel.TestObject object = TreeUtils.getUserObject(treeNode);
            return object.description.equals("root") || object.description.startsWith("child");
        });
        assertTrue(model.isExpanded(rootNode));
        assertFalse(model.isExpanded(child0));
        assertFalse(model.isExpanded(child1));
        assertFalse(model.isExpanded(child2));

        model.expandTree(2, treeNode -> {
            TestTreeTableModel.TestObject object = TreeUtils.getUserObject(treeNode);
            return object.description.equals("root") || object.description.startsWith("child");
        });
        assertTrue(model.isExpanded(rootNode));
        assertFalse(model.isExpanded(child0));
        assertTrue(model.isExpanded(child1));
        assertFalse(model.isExpanded(child2));
    }

    @Test
    public void testCollapseChildren() {
        model = new TestTreeTableModel(rootNode, true);
        model.expandTree(); // all nodes should be expanded.
        assertTrue(model.isExpanded(child1));

        model.collapseNode(rootNode); // root node is collapsed, but child 1 is still expanded.
        assertTrue(model.isExpanded(child1));

        model.collapseChildren(rootNode); // all child nodes should be collapsed.
        assertFalse(model.isExpanded(rootNode));
        assertFalse(model.isExpanded(child1));
    }

    @Test
    public void testCollapseChildrenWithPredicate() {
        model = new TestTreeTableModel(rootNode, true);
        model.expandTree(); // all nodes should be expanded.
        assertTrue(model.isExpanded(child1));

        model.collapseNode(rootNode); // root node is collapsed, but child 1 is still expanded.
        assertTrue(model.isExpanded(child1));

        model.collapseChildren(rootNode, treeNode -> {
            TestTreeTableModel.TestObject object = TreeUtils.getUserObject(treeNode);
            return object.description.equals("root");
        }); // only root node should be collapsed.
        assertFalse(model.isExpanded(rootNode));
        assertTrue(model.isExpanded(child1));
    }

    @Test
    public void testToggleNode() {
        model = new TestTreeTableModel(rootNode, false);
        assertTrue(model.isExpanded(rootNode)); // root is expanded if it is not showing.

        model.toggleNode(rootNode);
        assertFalse(model.isExpanded(rootNode));

        model.toggleNode(rootNode);
        assertTrue(model.isExpanded(rootNode));
    }

    @Test
    public void testGetRoot() {
        assertEquals(rootNode, model.getRoot());
    }

    @Test
    public void testSetNullRootException() {
        assertThrows(IllegalArgumentException.class, () -> model.setRoot(null));
    }

    @Test
    public void testSetVisibleRoot() {
        DefaultMutableTreeNode newRoot = new DefaultMutableTreeNode("test");
        newRoot.add(subchild3);
        model.setShowRoot(true);
        model.setRoot(newRoot);
        assertEquals(newRoot, model.getRoot());
        assertEquals(1, model.getRowCount());
        model.expandTree();
        assertEquals(2, model.getRowCount());
    }

    @Test
    public void testSetHiddenRoot() {
        DefaultMutableTreeNode newRoot = new DefaultMutableTreeNode("test");
        newRoot.add(subchild3);
        model.setShowRoot(false);
        model.setRoot(newRoot);
        assertEquals(newRoot, model.getRoot());
        assertEquals(1, model.getRowCount());
        model.expandTree();
        assertEquals(1, model.getRowCount());
    }

    /* *****************************************************************************************************************
     *                                 Test node selection
     */

    @Test
    public void testGetSelectedNodeNoTable() {
        assertNull(model.getSelectedNode());
    }

    @Test
    public void testGetSelectedNodeNoSelection() {
        model.bindTable(table);
        assertNull(model.getSelectedNode());
    }

    @Test
    public void testGetSelectedNode() {
        model.bindTable(table);
        table.setRowSelectionInterval(0, 0);
        assertEquals(rootNode, model.getSelectedNode());
    }

    @Test
    public void testGetSelectedNodesNoTable() {
        assertTrue(model.getSelectedNodes().isEmpty());
    }

    @Test
    public void testGetSelectedNodesNoSelection() {
        model.bindTable(table);
        assertTrue(model.getSelectedNodes().isEmpty());
    }

    @Test
    public void testGetSelectedNodes() {
        model.bindTable(table);
        model.expandTree();
        table.setRowSelectionInterval(0, 2);
        assertEquals(3, model.getSelectedNodes().size());
    }

    @Test
    public void testGetNodeAtModelIndex() {
        assertNull(model.getNodeAtModelIndex(-1));
        assertEquals(rootNode, model.getNodeAtModelIndex(0));
        assertNull(model.getNodeAtModelIndex(1));
        model.expandTree();
        assertEquals(child0, model.getNodeAtModelIndex(1));
        assertNull(model.getNodeAtModelIndex(model.getRowCount()));
    }

    @Test
    public void testGetNodeAtTableRow() {
        //fail("TODO");
    }

    @Test
    public void testGetSelectionModelNoTable() {
        assertNull(model.getSelectionModel());
    }

    @Test
    public void testGetSelectionModel() {
        model.bindTable(table);
        assertNotNull(model.getSelectionModel());
    }

    /* *****************************************************************************************************
     *   Test using a tree model to update the tree.
     */

    @Test
    public void testTreeModelSetInvisibleRoot() {
        model.bindTable(table);
        model.setShowRoot(false);
        model.expandTree();
        assertTrue(model.getRowCount() > 1);

        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
        treeModel.addTreeModelListener(model);
        treeModel.setRoot(subchild2);

        assertEquals(subchild2, model.getRoot());
        assertEquals(0, model.getRowCount());
    }

    @Test
    public void testTreeModelSetVisibleRoot() {
        model.bindTable(table);
        model.setShowRoot(true);
        model.expandTree();
        assertTrue(model.getRowCount() > 1);

        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
        treeModel.addTreeModelListener(model);
        treeModel.setRoot(subchild2);

        assertEquals(subchild2, model.getRoot());
        assertEquals(1, model.getRowCount());
    }

    @Test
    public void testCreateTableColumn() {
        TableColumn column = TableUtils.createColumn(1, "test1");
        testColumn(column, 1, "test1", 75, null, null);

        column = TableUtils.createColumn(0, "test2", 100);
        testColumn(column, 0, "test2", 100, null, null);

        TreeCellRenderer renderer = new TreeCellRenderer(model);
        column = TableUtils.createColumn(9, "test3", renderer);
        testColumn(column, 9, "test3", 75, renderer, null);

        column = TableUtils.createColumn(3, "test4", 100, renderer);
        testColumn(column, 3, "test4", 100, renderer, null);

        TableCellEditor editor = new DefaultCellEditor(new JTextField());
        column = TableUtils.createColumn(8, "test5", renderer, editor);
        testColumn(column, 8, "test5", 75, renderer, editor);
    }

    private void testColumn(TableColumn column, int index, String header, int width,
                            TableCellRenderer renderer, TableCellEditor editor) {
        assertEquals(header, column.getIdentifier());
        assertEquals(width, column.getWidth());
        assertEquals(index, column.getModelIndex());
        assertEquals(renderer, column.getCellRenderer());
        assertEquals(editor, column.getCellEditor());
    }

    /**
     * This only checks that the test implementation of TreeTableModel works.
     */
    @Test
    public void testSetColumnValueTestImplementation() {
        TestTreeTableModel.TestObject object = TreeUtils.getUserObject(rootNode);
        assertEquals("root", object.description);
        model.setColumnValue(rootNode, 0, "root changed");
        assertEquals("root changed", object.description);
    }

    @Test
    public void testGetVisibleSubTreeCount() {
        assertEquals(0, model.getVisibleSubTreeCount(rootNode));
        assertEquals(0, model.getVisibleSubTreeCount(child1));

        model.expandNode(child1); // child 1 expanded but root node is not so will not be visible.
        assertEquals(0, model.getVisibleSubTreeCount(child1));

        model.expandNode(rootNode); // now child 1 children will be visible as root is expanded.
        assertEquals(7, model.getVisibleSubTreeCount(rootNode));
        assertEquals(4, model.getVisibleSubTreeCount(child1));

        model.collapseNode(child1);
        assertEquals(3, model.getVisibleSubTreeCount(rootNode));
    }

    @Test
    public void testGetFilteredChildCount() {
        assertEquals(3, model.getFilteredChildCount(rootNode));

        model.setNodeFilter(treeNode -> {
            TestTreeTableModel.TestObject object = TreeUtils.getUserObject(treeNode);
            return object.description.contains("child");
        });
        assertEquals(0, model.getFilteredChildCount(rootNode));

        model.setNodeFilter(treeNode -> {
            TestTreeTableModel.TestObject object = TreeUtils.getUserObject(treeNode);
            return object.description.contains("1") || object.description.contains("2");
        });
        assertEquals(1, model.getFilteredChildCount(rootNode));
    }

    @Test
    public void testTreeModelInsertNodeInto() {

    }

    @Test
    public void testTreeModelRemoveNodeFromParent() {

    }

    @Test
    public void testTreeModelNodeChanged() {

    }

    @Test
    public void testTreeModelReload() {

    }

    @Test
    public void testTreeModelNodesWereInserted() {

    }

    @Test
    public void testTreeModelNodesWereRemoved() {

    }

    @Test
    public void testTreeModelNodesChanged() {

    }

    @Test
    public void testTreeModelNodeStructureChanged() {

    }

    /* *****************************************************************************************************
     *   Test calling update methods when tree changed manually (no treemodel).
     */

    @Test
    public void testTreeNodesChangedChild() {
    }

    @Test
    public void testGetModelIndexForTableRow() {
        //fail("TODO");
    }

    @Test
    public void testGetModelIndexForTreeNode() {
        //fail("TODO");
    }

    @Test
    public void testAddExpandCollapseListener() {
        //fail("TODO");
    }

    @Test
    public void testRemoveExpandCollapseListener() {
        //fail("TODO");
    }

    @Test
    public void testAddMouseListener() {
        //fail("TODO");
    }

    @Test
    public void testRemoveMouseListener() {
        //fail("TODO");
    }

    @Test
    public void testAddKeyboardActions() {
        //fail("TODO");
    }

    @Test
    public void testRemoveKeyboardActions() {
        //fail("TODO");
    }


    /* *****************************************************************************************************************
     *                                 Test utility methods and classes
     */

    private class SortComparator implements Comparator<TreeNode> {

        private static final int LESS_THAN = -1;
        private static final int EQUAL_VALUE = 0;
        private static final int GREATER_THAN = 0;

        private List<RowSorter.SortKey> sortKeys;

        public SortComparator(RowSorter.SortKey... keys) {
            this(Arrays.asList(keys));
        }

        public SortComparator(List<RowSorter.SortKey> keys) {
            this.sortKeys = keys;
        }

        @Override
        public int compare(TreeNode o1, TreeNode o2) {
            // null object comparisons giving a total order.
            if (o1 == null) {
                return o2 == null ? EQUAL_VALUE : LESS_THAN;
            } else if (o2 == null) {
                return GREATER_THAN;
            }

            // compare values on sort keys:
            for (RowSorter.SortKey sortKey : sortKeys) {
                final SortOrder order = sortKey.getSortOrder();
                // If someone leaves in an unsorted sort key (they shouldn't really), we return unsorted order.
                if (order == SortOrder.UNSORTED) {
                    return getModelIndexOrder(o1, o2);
                }
                // Compare on the values if we have a sort direction:
                final int comparison = compareValues(o1, o2, sortKey);
                if (comparison != EQUAL_VALUE) {
                    return sortKey.getSortOrder() == SortOrder.ASCENDING ? comparison : -comparison;
                }
            }

            // fall back to model order if sort keys cannot decide.
            return getModelIndexOrder(o1, o2);
        }

        private int compareValues(TreeNode o1, TreeNode o2, RowSorter.SortKey sortKey) {
            final int sortedColumn = sortKey.getColumn();
            final Object value1 = model.getColumnValue(o1, sortedColumn);
            final Object value2 = model.getColumnValue(o2, sortedColumn);

            // Use a custom column comparator if the model provides one.
            Comparator columnComparator = model.getColumnComparator(sortedColumn);
            if (columnComparator != null) {
                return columnComparator.compare(value1, value2);
            }

            // See if the values are directly comparable (implement Comparable and are the same class).
            if (value1 instanceof Comparable && value1.getClass() == value2.getClass()) {
                final Comparable compare1 = (Comparable) value1;
                final Comparable compare2 = (Comparable) value2;
                return compare1.compareTo(compare2);
            }

            // Fall back on string value comparisons if they don't implement Comparable.
            return value1.toString().compareTo(value2.toString());
        }

        private int getModelIndexOrder(TreeNode o1, TreeNode o2) {
            return model.getModelIndexForTreeNode(o1) - model.getModelIndexForTreeNode(o2);
        }
    }

}