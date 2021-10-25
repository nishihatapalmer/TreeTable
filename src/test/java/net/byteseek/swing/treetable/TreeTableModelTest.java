package net.byteseek.swing.treetable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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

    private SimpleTreeTableModel model;
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

        model = new SimpleTreeTableModel(rootNode, true);
        table = new JTable();
    }


    //TODO: test using a TreeModel to update nodes while inside an expand or collapse event.


    /* *****************************************************************************************************************
     *                                              Constructor tests
     */

    @Test
    public void testNullRootNodeConstructorException() {
        assertThrows(IllegalArgumentException.class,
                ()-> model = new SimpleTreeTableModel(null));

        assertThrows(IllegalArgumentException.class,
                ()-> model = new SimpleTreeTableModel(null, true));

        assertThrows(IllegalArgumentException.class,
                ()-> model = new SimpleTreeTableModel(null, false));
    }

    @Test
    public void testRootVisibleConstructor() {
        model = new SimpleTreeTableModel(rootNode, true);
        assertEquals(rootNode, model.getRoot());
        assertTrue(model.isVisible(rootNode));
    }

    @Test
    public void testRootVisibleDefaultConstructor() {
        model = new SimpleTreeTableModel(rootNode);
        assertEquals(rootNode, model.getRoot());
        assertTrue(model.isVisible(rootNode));
    }

    @Test
    public void testRootNotVisibleConstructor() {
        model = new SimpleTreeTableModel(rootNode, false);
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
    public void testBindTableWithSortKeys() {
        // Test single sort key
        model.bindTable(table, sortKey1);
        testDefaultTableBinding();
        testOneSortKey();

        // Test multi key parameters
        model.bindTable(table, sortKey1, sortKey2);
        testDefaultTableBinding();
        testTwoSortKeys();

        // Test list method
        model.bindTable(table, sortKeyList);
        testDefaultTableBinding();
        testListOfKeys();
    }

    private void testOneSortKey() {
        List<? extends RowSorter.SortKey> sortKeyList = table.getRowSorter().getSortKeys();
        assertEquals(1, sortKeyList.size());
        assertEquals(sortKey1, sortKeyList.get(0));

    }

    private void testTwoSortKeys() { //TODO: difference between ? extends RowSorter and List<RowSorter.sortkey>
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
    public void testBindTableWithCustomHeaderAndSortKeys() {
        TableCellRenderer headerRenderer = new DefaultTableCellRenderer();

        // Test single sort key
        model.bindTable(table, headerRenderer, sortKey1);
        assertEquals(headerRenderer, table.getTableHeader().getDefaultRenderer());
        testDefaultTableBinding();
        testOneSortKey();

        // Test multi key parameters
        model.bindTable(table, headerRenderer, sortKey1, sortKey2);
        assertEquals(headerRenderer, table.getTableHeader().getDefaultRenderer());
        testDefaultTableBinding();
        testTwoSortKeys();

        // Test list method
        model.bindTable(table, headerRenderer, sortKeyList);
        assertEquals(headerRenderer, table.getTableHeader().getDefaultRenderer());
        testDefaultTableBinding();
        testListOfKeys();
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
        TestObject rootObject = new TestObject("root", 0, true);
        TestObject child1 = new TestObject("child1", 1, false);
        TestObject child2 = new TestObject("child2", 2, true);

        //TODO: add sub in.

        TestObject child3 = new TestObject("child3", 3, false);
        rootObject.getChildren().add(child1);
        rootObject.getChildren().add(child2);
        rootObject.getChildren().add(child3);
        DefaultMutableTreeNode node = TreeTableModel.buildTree(rootObject, parent -> ((TestObject) parent).getChildren());
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
        model.setGroupingComparator(TreeTableModel.GROUP_BY_ALLOWS_CHILDREN);
        assertEquals(TreeTableModel.GROUP_BY_ALLOWS_CHILDREN, model.getGroupingComparator());
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

        model.setGroupingComparator(TreeTableModel.GROUP_BY_ALLOWS_CHILDREN);

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
        model.setGroupingComparator(TreeTableModel.GROUP_BY_ALLOWS_CHILDREN);
        model.bindTable(table);
        testGetRowCount();
    }

    @Test
    public void testGetRowCountWithSortedGroupedTable() {
        model.setGroupingComparator(TreeTableModel.GROUP_BY_ALLOWS_CHILDREN);
        model.bindTable(table, sortKey1);
        testGetRowCount();
    }

    @Test
    public void testGetColumnCount() {
        SimpleTreeTableModel model = new SimpleTreeTableModel(rootNode, true);
        assertEquals(3, model.getColumnCount());

        model = new SimpleTreeTableModel(rootNode, true, 10);
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

        model = new SimpleTreeTableModel(rootNode, showRoot);
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
        model.setGroupingComparator(TreeTableModel.GROUP_BY_HAS_CHILDREN);
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

    //TODO: set values and test them!
    
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

        model = new SimpleTreeTableModel(rootNode, showRoot);
        model.bindTable(table);
        int root = showRoot? 1 : 0;
        if (showRoot) {
            String[] expectedDescriptions = {"root"};
            int[] expectedSizes = {0};
            assertEquals(1, model.getRowCount());
            testSetValuesAt(expectedDescriptions, expectedSizes, true);
        } else {
            String[] expectedDescriptions = {"root", "child0", "child1", "child2"};
            int[] expectedSizes = {0, 100, 101, 102};
            assertEquals(3, model.getRowCount());
            testSetValuesAt(expectedDescriptions, expectedSizes, false); // will ignore first "root" item of expected.
        }

        // Expand children of root
        model.expandNode(rootNode);
        assertEquals(3 + root, model.getRowCount());
        String[] expectedDescriptions = {"root", "child0", "child1", "child2"};
        int[] expectedSizes = {0, 100, 101, 102};
        testSetValuesAt(expectedDescriptions, expectedSizes, showRoot);

        // Expand sub-children of child1
        model.expandNode(child1);
        assertEquals(7 + root, model.getRowCount());
        expectedDescriptions = new String[] {"root", "child0", "child1", "subchildren0", "subchildren1", "subchildren2", "subchildren3", "child2"};
        expectedSizes = new int[] {0, 100, 101, 1000, 1001, 1002, 1003, 102};
        testSetValuesAt(expectedDescriptions, expectedSizes, showRoot);

        // Use grouping to sort the nodes:
        model.setGroupingComparator(TreeTableModel.GROUP_BY_ALLOWS_CHILDREN);
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
        expectedSizes = new int[] {0, 100, 101, 102};
        testSetValuesAt(expectedDescriptions, expectedSizes, showRoot);
    }

    private void testSetValuesAt(String[] expectedDescriptions, int[] expectedSizes, boolean showRoot) {
        int ignoreRootOffset = showRoot? 0 : 1;
        for (int row = 0; row < model.getRowCount(); row++) {
            Object currentValue =  model.getValueAt(row, 0);
            assertEquals(expectedDescriptions[row + ignoreRootOffset], currentValue , "row" + row);
            model.setValueAt(Integer.toString(row), row, 0);
            assertEquals(Integer.toString(row), model.getValueAt(row, 0), "row" + row);
            model.setValueAt(currentValue, row, 0); // reset back to old value so next tests work.

            currentValue =  model.getValueAt(row, 1);
            assertEquals(expectedSizes[row + ignoreRootOffset], (long) currentValue, "row" + row);
            model.setValueAt(row, row, 1);
            assertEquals(row, model.getValueAt(row, 1), "row" + row);
            model.setValueAt(currentValue, row, 1); // reset back to old value so next tests work.

            currentValue =  model.getValueAt(row, 2);
            assertEquals(expectedSizes[row + ignoreRootOffset] % 2 == 0, model.getValueAt(row, 2), "row" + row);
            model.setValueAt(expectedSizes[row + ignoreRootOffset] % 2 == 1, row, 2);
            assertEquals(expectedSizes[row + ignoreRootOffset] % 2 == 1, model.getValueAt(row, 2), "row" + row);
            model.setValueAt(currentValue, row, 2); // reset back to old value so next tests work.
        }
    }


    @Test
    public void testModelIndexAlgorithms() {
        model.expandNode(rootNode);
        model.expandNode(child1);
        for (int i = 0; i < model.getRowCount(); i++) {
            TreeNode nodeToFind = model.displayedNodes.get(i); // naughty - go inside, but want to avoid calling model index to test model index.
            int modelIndex1 = model.getModelIndexLinearScan(nodeToFind);
            int modelIndex2 = model.getModelIndexTreeScan(nodeToFind);
            assertEquals(i, modelIndex1);
            assertEquals(i, modelIndex2);
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
        model = new SimpleTreeTableModel(rootNode, false);
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
        model = new SimpleTreeTableModel(rootNode, false);
        assertTrue(model.isVisible(childNode));
    }

    @Test
    public void testRemovedNodeNotVisibleRootShowing() {
        TreeTableModel model = new SimpleTreeTableModel(rootNode, true);

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
        TreeTableModel model = new SimpleTreeTableModel(rootNode, false);

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
        createChildren(child1, "sub1", "sub2", "sub3");
        TreeNode sub3 = child1.getChildAt(2);

        model = new SimpleTreeTableModel(rootNode, true);
        assertTrue(model.isVisible(rootNode));
        assertFalse(model.isVisible(child1));
        assertFalse(model.isVisible(sub3));

        //TODO: bug - expanding node when root is not expanded causes NullPointerExceptin in expandVisibleChildCounts.
        //      you can have a non expanded root if it is showing.
        model.expandNode(child1);
        assertFalse(model.isVisible(sub3));
    }

    @Test
    public void testParentsExpandedChildVisible() {
        TreeNode sub3 = child1.getChildAt(2);

        model = new SimpleTreeTableModel(rootNode, false);
        assertFalse(model.isVisible(rootNode));
        assertTrue(model.isVisible(child1));
        assertFalse(model.isVisible(sub3));

        model.expandNode(child1);
        assertTrue(model.isVisible(sub3));
    }

    //TODO: test looking for nodes that aren't in the tree, so won't be found by model index scans..


    /* *****************************************************************************************************************
     *                                 Utility methods and classes
     */

    protected void createChildren(MutableTreeNode parentNode, Object... childValues) {
        for (int i = 0; i < childValues.length; i++) {
            MutableTreeNode childNodes = new DefaultMutableTreeNode(childValues[i]);
            parentNode.insert(childNodes, i);
        }
    }

    protected MutableTreeNode createTree() {
        TestObject rootObject = new TestObject("root", 0, true);
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(rootObject);
        addChildren(rootNode, 3, "child", 100);
        DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) rootNode.getChildAt(1);
        addChildren(childNode, 4, "subchildren", 1000);
        return rootNode;
    }

    protected void addChildren(DefaultMutableTreeNode parent, int numChildren, String description, int sizeStart) {
        TestObject parentObject = (TestObject) parent.getUserObject();
        parent.setAllowsChildren(true);
        for (int i = 0; i < numChildren; i++) {
            TestObject object = new TestObject(description + i, sizeStart + i, (sizeStart + i) % 2 == 0);
            parentObject.children.add(object);
            MutableTreeNode childNodes = new DefaultMutableTreeNode(object, false);
            parent.insert(childNodes, i);
        }
    }

    protected static class TestObject {
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

    protected static class SimpleTreeTableModel extends TreeTableModel {

        private int columnCount = 3;

        public SimpleTreeTableModel(TreeNode node) {
            super(node);
        }

        public SimpleTreeTableModel(TreeNode node, boolean showRoot) {
            super(node, showRoot);
        }

        public SimpleTreeTableModel(TreeNode node, boolean showRoot, int columnCount) {
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
                case 0: return test.description;
                case 1: return test.size;
                case 2: return test.enabled;
                default: return 0;
            }
        }

        @Override
        public TableColumnModel createTableColumnModel() {
            TableColumnModel columns = new DefaultTableColumnModel();
            for (int columnNumber = 0; columnNumber < columnCount; columnNumber++) {
                columns.addColumn(createColumn(columnNumber, "header" + columnNumber ));
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
            for (int row = 0; row < getRowCount(); row++) {
                System.out.println(row + ": " + getNodeAtTableRow(row));
            }
            System.out.println();
        }

    }

}