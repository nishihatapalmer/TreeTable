package net.byteseek.swing.treetable;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import static org.junit.jupiter.api.Assertions.*;

class TreeTableModelTest {

    // Tests for Table binding.

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

    @Test
    public void testNullNodeNotVisible() {
        MutableTreeNode rootNode = createTree("root", "child");
        TreeTableModel model = new SimpleTreeTableModel(rootNode, true);
        assertFalse(model.isVisible(null));
    }

    @Test
    public void testRootVisible() {
        MutableTreeNode rootNode = createTree("root", "child");
        TreeTableModel model = new SimpleTreeTableModel(rootNode, true);
        assertTrue(model.isVisible(rootNode));

        model = new SimpleTreeTableModel(rootNode);
        assertTrue(model.isVisible(rootNode));
    }

    @Test
    public void testRootNotVisible() {
        MutableTreeNode rootNode = createTree("root", "child");
        TreeTableModel model = new SimpleTreeTableModel(rootNode, false);
        assertFalse(model.isVisible(rootNode));
    }

    @Test
    public void testShowRoot() {
        MutableTreeNode rootNode = createTree("root", "child");
        TreeTableModel model = new SimpleTreeTableModel(rootNode, false);
        assertFalse(model.isVisible(rootNode));

        model.setShowRoot(true);
        assertTrue(model.isVisible(rootNode));

        model.setShowRoot(false);
        assertFalse(model.isVisible(rootNode));
    }

    @Test
    public void testIsRootChildAlwaysVisible() {
        MutableTreeNode rootNode = createTree("root", "child");
        TreeNode childNode = rootNode.getChildAt(0);

        TreeTableModel model = new SimpleTreeTableModel(rootNode, true);
        assertFalse(model.isVisible(childNode)); // root node not expanded.

        model = new SimpleTreeTableModel(rootNode, false);
        assertTrue(model.isVisible(childNode)); // root node is expanded if not showing.
    }

    @Test
    public void testRemovedNodeNotVisible() {
        testRemovedNodeNotVisible(false);
        testRemovedNodeNotVisible(true);
    }

    private void testRemovedNodeNotVisible(boolean showRoot) {
        MutableTreeNode rootNode = createTree("root", "child1", "child2", "child3");
        TreeNode child1 = rootNode.getChildAt(0);
        TreeNode child2 = rootNode.getChildAt(1);
        TreeNode child3 = rootNode.getChildAt(2);

        TreeTableModel model = new SimpleTreeTableModel(rootNode, showRoot);
        assertNotEquals(showRoot, model.isVisible(child1)); // they are visible if not showing root.
        assertNotEquals(showRoot, model.isVisible(child2));
        assertNotEquals(showRoot, model.isVisible(child3));

        rootNode.remove(1);

        assertNotEquals(showRoot, model.isVisible(child1));
        assertFalse(model.isVisible(child2));
        assertNotEquals(showRoot, model.isVisible(child3));

        rootNode.remove(1);
        assertNotEquals(showRoot, model.isVisible(child1));
        assertFalse(model.isVisible(child2));
        assertFalse(model.isVisible(child3));
    }

    @Test
    public void testParentNotExpandedChildNotVisible() {
        testParentNotExpandedChildNotVisible(true);
        testParentNotExpandedChildNotVisible(false);
    }

    private void testParentNotExpandedChildNotVisible(boolean showRoot) {
        MutableTreeNode rootNode = createTree("root", "child1", "child2", "child3");
        MutableTreeNode child2 = (MutableTreeNode) rootNode.getChildAt(1);
        createChildren(child2, "sub1", "sub2", "sub3");
        TreeNode sub3 = child2.getChildAt(2);

        TreeTableModel model = new SimpleTreeTableModel(rootNode, showRoot);
        assertEquals(showRoot, model.isVisible(rootNode));
        assertNotEquals(showRoot, model.isVisible(child2));
        assertFalse(model.isVisible(sub3));

        //TODO: bug - expanding node when root is not expanded causes NullPointerExceptin in expandVisibleChildCounts.
        //      you can have a non expanded root if it is showing.
        model.expandNode(child2);
        assertNotEquals(showRoot, model.isVisible(sub3));
    }


    /*
     * Utility methods and classes
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
            return null;
        }

        @Override
        public TableColumnModel createTableColumnModel() {
            return new DefaultTableColumnModel(); // don't return a null model, just an empty one.
        }

    }

}