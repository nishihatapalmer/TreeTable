package net.byteseek.swing.treetable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

public class TestUtils {

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
}
