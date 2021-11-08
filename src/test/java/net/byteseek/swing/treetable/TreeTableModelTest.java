package net.byteseek.swing.treetable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import static net.byteseek.swing.treetable.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TreeTableModelTest {

    private MutableTreeNode rootNode;
    private MutableTreeNode child0;
    private MutableTreeNode child1;
    private MutableTreeNode child2;
    private MutableTreeNode subchild0;
    private MutableTreeNode subchild1;
    private MutableTreeNode subchild2;
    private MutableTreeNode subchild3;

    private RowSorter.SortKey sortKey1;
    private RowSorter.SortKey sortKey2;
    private RowSorter.SortKey sortKey3;
    List<RowSorter.SortKey> sortKeyList;

    private TestTreeTableModel model;
    private JTable table;

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
        model.bindTable(table, (RowSorter.SortKey) null);
        testDefaultTableBinding();
        testNoSortKeys();
    }

    @Test
    public void testBindTableWithDefaultSortKey() {
        model.bindTable(table, sortKey1);
        testDefaultTableBinding();
        testOneSortKey();
    }

    @Test
    public void testBindTableWithTwoDefaultSortKeys() {
        model.bindTable(table, sortKey1, sortKey2);
        testDefaultTableBinding();
        testTwoSortKeys();
    }

    @Test
    public void testBindTableWitDefaultSortKeyList() {
        model.bindTable(table, sortKeyList);
        testDefaultTableBinding();
        testListOfKeys();
    }

    @Test
    public void testBindTableWitDefaultEmptySortKeyList() {
        model.bindTable(table, Collections.emptyList());
        testDefaultTableBinding();
        testNoSortKeys();
    }

    @Test
    public void testBindTableWitDefaultNullSortKeyList() {
        model.bindTable(table, (List<RowSorter.SortKey>) null);
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
    public void testBindTableWithCustomHeaderAndNullSortKey() {
        TableCellRenderer headerRenderer = new DefaultTableCellRenderer();

        // Test null sort key
        model.bindTable(table, headerRenderer, (RowSorter.SortKey) null);
        assertEquals(headerRenderer, table.getTableHeader().getDefaultRenderer());
        testDefaultTableBinding();
        testNoSortKeys();
    }

    @Test
    public void testBindTableWithCustomHeaderAndOneSortKey() {
        TableCellRenderer headerRenderer = new DefaultTableCellRenderer();

        // Test single sort key
        model.bindTable(table, headerRenderer, sortKey1);
        assertEquals(headerRenderer, table.getTableHeader().getDefaultRenderer());
        testDefaultTableBinding();
        testOneSortKey();
    }

    @Test
    public void testBindTableWithCustomHeaderAndTwoSortKeys() {
        TableCellRenderer headerRenderer = new DefaultTableCellRenderer();

        // Test multi key parameters
        model.bindTable(table, headerRenderer, sortKey1, sortKey2);
        assertEquals(headerRenderer, table.getTableHeader().getDefaultRenderer());
        testDefaultTableBinding();
        testTwoSortKeys();
    }

    @Test
    public void testBindTableWithCustomHeaderAndSortKeysList() {
        TableCellRenderer headerRenderer = new DefaultTableCellRenderer();

        // Test list method
        model.bindTable(table, headerRenderer, sortKeyList);
        assertEquals(headerRenderer, table.getTableHeader().getDefaultRenderer());
        testDefaultTableBinding();
        testListOfKeys();
    }

    @Test
    public void testBindTableWithCustomHeaderAndNullSortKeysList() {
        TableCellRenderer headerRenderer = new DefaultTableCellRenderer();

        // Test list method
        model.bindTable(table, headerRenderer, (List<? extends RowSorter.SortKey>) null);
        assertEquals(headerRenderer, table.getTableHeader().getDefaultRenderer());
        testDefaultTableBinding();
        testNoSortKeys();
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
        for (int column = 0; column < 3; column++) {
            assertNull(model.getColumnComparator(column));
        }
    }

    /* *****************************************************************************************************************
     *                                          Grouping - node comparators
     */
    @Test
    public void testGetSetNodeComparator() {
        assertNull(model.getGroupingComparator());
        model.setGroupingComparator(TreeUtils.GROUP_BY_ALLOWS_CHILDREN);
        assertEquals(TreeUtils.GROUP_BY_ALLOWS_CHILDREN, model.getGroupingComparator());
        model.setGroupingComparator(null);
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
        model.bindTable(table, sortKey1);
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

        model.setGroupingComparator(TreeUtils.GROUP_BY_ALLOWS_CHILDREN);

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
        model.bindTable(table, sortKey1);
        testGetRowCount();
    }

    @Test
    public void testGetRowCountWithGroupedTable() {
        model.setGroupingComparator(TreeUtils.GROUP_BY_ALLOWS_CHILDREN);
        model.bindTable(table);
        testGetRowCount();
    }

    @Test
    public void testGetRowCountWithSortedGroupedTable() {
        model.setGroupingComparator(TreeUtils.GROUP_BY_ALLOWS_CHILDREN);
        model.bindTable(table, sortKey1);
        testGetRowCount();
    }

    @Test
    public void testGetColumnCount() {
        TestTreeTableModel model = new TestTreeTableModel(rootNode, true);
        assertEquals(3, model.getColumnCount());

        model = new TestTreeTableModel(rootNode, true, 10);
        assertEquals(10, model.getColumnCount());
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
        model.setGroupingComparator(TreeUtils.GROUP_BY_HAS_CHILDREN);
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
        model.setGroupingComparator(TreeUtils.GROUP_BY_ALLOWS_CHILDREN);
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

        model.setGroupingComparator(TreeUtils.GROUP_BY_NUM_CHILDREN_DESCENDING);
        assertTrue(model.isGrouping());
        assertEquals(TreeUtils.GROUP_BY_NUM_CHILDREN_DESCENDING, model.getGroupingComparator());
        assertFalse(tableOrderMatches(allNodesInVisualOrder));

        model.setGroupingComparator(TreeUtils.GROUP_BY_HAS_CHILDREN);
        assertTrue(model.isGrouping());
        assertEquals(TreeUtils.GROUP_BY_HAS_CHILDREN, model.getGroupingComparator());
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
            model.setGroupingComparator(TreeUtils.GROUP_BY_NUM_CHILDREN_DESCENDING);
            testSorting(rootNode, TreeUtils.GROUP_BY_NUM_CHILDREN_DESCENDING);

            model.setGroupingComparator(TreeUtils.GROUP_BY_NUM_CHILDREN);
            testSorting(rootNode, TreeUtils.GROUP_BY_NUM_CHILDREN);
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

    private void createRandomTree(int randTree, boolean showRoot) {
        model.unbindTable();
        rootNode = buildRandomTree(randTree);
        model = new TestTreeTableModel(rootNode, showRoot);
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
        testGetSetSortKeys(true);
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

    private void testGetSetSortKeys(boolean tableBound) {
        List<TreeNode> allNodesInVisualOrder = TreeUtils.getNodeList(rootNode); // gets all tree nodes depth first (visual order)

        assertFalse(model.isSorting());
        assertTrue(tableOrderMatches(allNodesInVisualOrder));
        List<? extends RowSorter.SortKey> sortKeys = model.getSortKeys();
        assertTrue(sortKeys.isEmpty());

        model.setSortKeys(sortKey1);
        assertTrue(model.isSorting());
        assertNotEquals(tableBound, tableOrderMatches(allNodesInVisualOrder)); // the table order will only change if there is a table bound.
        sortKeys = model.getSortKeys();
        assertEquals(1, sortKeys.size());
        assertEquals(sortKey1, sortKeys.get(0));

        model.setSortKeys(sortKey2, sortKey1);
        assertTrue(model.isSorting());
        assertNotEquals(tableBound, tableOrderMatches(allNodesInVisualOrder)); // the table order will only change if there is a table bound.
        sortKeys = model.getSortKeys();
        assertEquals(2, sortKeys.size());
        assertEquals(sortKey2, sortKeys.get(0));
        assertEquals(sortKey1, sortKeys.get(1));

        model.setSortKeys(sortKeyList);
        assertTrue(model.isSorting());
        assertNotEquals(tableBound, tableOrderMatches(allNodesInVisualOrder)); // the table order will only change if there is a table bound.
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

    @Test
    public void testGetSetFilteringNoTable() {
        //TODO:
    }

    @Test
    public void testGetSetFilteringWithTable() {
        //TODO:
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
        final int numTrials = 10;

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


    /* *****************************************************************************************************************
     *                                 Test utility methods and classes
     */



}