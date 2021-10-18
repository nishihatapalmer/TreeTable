package net.byteseek.swing.treetable;

import org.junit.jupiter.api.BeforeAll;
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
    private MutableTreeNode child1;
    private MutableTreeNode child2;
    private MutableTreeNode child3;
    private RowSorter.SortKey key1;
    private RowSorter.SortKey key2;
    private RowSorter.SortKey key3;
    List<RowSorter.SortKey> bindKeys;

    private TreeTableModel model;
    private JTable table;

    @BeforeEach
    public void setup() {
        rootNode = createTree("root", "child1", "child2", "child3");
        child1 = (MutableTreeNode) rootNode.getChildAt(0);
        child2 = (MutableTreeNode) rootNode.getChildAt(1);
        child3 = (MutableTreeNode) rootNode.getChildAt(2);
        key1 = new RowSorter.SortKey(0, SortOrder.DESCENDING);
        key2 = new RowSorter.SortKey(1, SortOrder.ASCENDING);
        key3 = new RowSorter.SortKey(2, SortOrder.DESCENDING);
        bindKeys = new ArrayList<>();
        bindKeys.add(key1);
        bindKeys.add(key2);
        bindKeys.add(key3);
        model = new SimpleTreeTableModel(rootNode, true);
        table = new JTable();
    }


    // Tests for get allows children stuff - expansion etc.

    // Expansion tests

    // Insertion tests

    // Removal tests

    // Structure change tests

    // Integration with JTable

    // Integration with TreeModelListener events

    /*
     * Node visibility tests
     */

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

    //TODO: test different bind variants for accuracy...

    @Test
    public void testBindTableWithSortKeys() {
        // Test single sort key
        model.bindTable(table, key1);
        testDefaultTableBinding();
        testOneSortKey();

        // Test multi key parameters
        model.bindTable(table, key1, key2);
        testDefaultTableBinding();
        testTwoSortKeys();

        // Test list method
        model.bindTable(table, bindKeys);
        testDefaultTableBinding();
        testListOfKeys();
    }

    private void testOneSortKey() {
        List<? extends RowSorter.SortKey> sortKeyList = table.getRowSorter().getSortKeys();
        assertEquals(1, sortKeyList.size());
        assertEquals(key1, sortKeyList.get(0));

    }

    private void testTwoSortKeys() { //TODO: difference between ? extends RowSorter and List<RowSorter.sortkey>
        List<? extends RowSorter.SortKey> sortKeyList = table.getRowSorter().getSortKeys();
        assertEquals(2, sortKeyList.size());
        assertEquals(key1, sortKeyList.get(0));
        assertEquals(key2, sortKeyList.get(1));
    }

    private void testListOfKeys() {
        List<? extends RowSorter.SortKey>sortKeyList = table.getRowSorter().getSortKeys();
        assertEquals(3, sortKeyList.size());
        assertEquals(key1, sortKeyList.get(0));
        assertEquals(key2, sortKeyList.get(1));
        assertEquals(key3, sortKeyList.get(2));
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
        model.bindTable(table, headerRenderer, key1);
        assertEquals(headerRenderer, table.getTableHeader().getDefaultRenderer());
        testDefaultTableBinding();
        testOneSortKey();

        // Test multi key parameters
        model.bindTable(table, headerRenderer, key1, key2);
        assertEquals(headerRenderer, table.getTableHeader().getDefaultRenderer());
        testDefaultTableBinding();
        testTwoSortKeys();

        // Test list method
        model.bindTable(table, headerRenderer, bindKeys);
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
    //TODO: test static tree build utility method.

    //TODO: test using a TreeModel to update nodes while inside an expand or collapse event.

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

        assertFalse(model.isVisible(child1));
        assertFalse(model.isVisible(child2));
        assertFalse(model.isVisible(child3));

        rootNode.remove(1);
        assertFalse(model.isVisible(child1));
        assertFalse(model.isVisible(child2));
        assertFalse(model.isVisible(child3));

        rootNode.remove(1);
        assertFalse(model.isVisible(child1));
        assertFalse(model.isVisible(child2));
        assertFalse(model.isVisible(child3));
    }

    @Test
    public void testRemovedNodeNotVisibleRootHidden() {
        TreeTableModel model = new SimpleTreeTableModel(rootNode, false);

        assertTrue(model.isVisible(child1));
        assertTrue(model.isVisible(child2));
        assertTrue(model.isVisible(child3));

        rootNode.remove(1);
        assertTrue(model.isVisible(child1));
        assertFalse(model.isVisible(child2));
        assertTrue(model.isVisible(child3));

        rootNode.remove(1);
        assertTrue(model.isVisible(child1));
        assertFalse(model.isVisible(child2));
        assertFalse(model.isVisible(child3));
    }

    @Test
    public void testParentNotExpandedChildNotVisible() {
        createChildren(child2, "sub1", "sub2", "sub3");
        TreeNode sub3 = child2.getChildAt(2);

        model = new SimpleTreeTableModel(rootNode, true);
        assertTrue(model.isVisible(rootNode));
        assertFalse(model.isVisible(child2));
        assertFalse(model.isVisible(sub3));

        //TODO: bug - expanding node when root is not expanded causes NullPointerExceptin in expandVisibleChildCounts.
        //      you can have a non expanded root if it is showing.
        model.expandNode(child2);
        assertFalse(model.isVisible(sub3));
    }

    @Test
    public void testParentsExpandedChildVisible() {
        createChildren(child2, "sub1", "sub2", "sub3");
        TreeNode sub3 = child2.getChildAt(2);

        model = new SimpleTreeTableModel(rootNode, false);
        assertFalse(model.isVisible(rootNode));
        assertTrue(model.isVisible(child2));
        assertFalse(model.isVisible(sub3));

        model.expandNode(child2);
        assertTrue(model.isVisible(sub3));
    }


    /* *****************************************************************************************************************
     *                                 Utility methods and classes
     */

    protected void createChildren(MutableTreeNode parentNode, Object... childValues) {
        for (int i = 0; i < childValues.length; i++) {
            MutableTreeNode childNodes = new DefaultMutableTreeNode(childValues[i]);
            parentNode.insert(childNodes, i);
        }
    }

    protected MutableTreeNode createTree(Object... nodeValues) {
        MutableTreeNode rootNode = new DefaultMutableTreeNode(nodeValues[0]);
        for (int i = 1; i < nodeValues.length; i++) {
            MutableTreeNode childNodes = new DefaultMutableTreeNode(nodeValues[i]);
            rootNode.insert(childNodes, i - 1);
        }
        return rootNode;
    }

    protected static class SimpleTreeTableModel extends TreeTableModel {

        public SimpleTreeTableModel(TreeNode node) {
            super(node);
        }

        public SimpleTreeTableModel(TreeNode node, boolean showRoot) {
            super(node, showRoot);
        }

        @Override
        public Object getColumnValue(TreeNode node, int column) {
            return "value";
        }

        @Override
        public TableColumnModel createTableColumnModel() {
            TableColumnModel columns = new DefaultTableColumnModel();
            columns.addColumn(createColumn(0, "header1"));
            columns.addColumn(createColumn(1, "header2"));
            return columns;
        }

    }

}