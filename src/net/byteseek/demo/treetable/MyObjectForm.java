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
package net.byteseek.demo.treetable;

import net.byteseek.swing.treetable.*;

import javax.swing.*;
import javax.swing.tree.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

public class MyObjectForm {

    private JPanel panel1;
    private JPanel rootPanel;
    private JScrollPane scrollPane;
    private JTable table1;
    private JButton showRootButton;
    private JButton insertButton;
    private JButton deleteButton;
    private Random random;
    private List<String> wordList;
    TreeTableModel treeTableModel;
    boolean showRoot;
    private DefaultTreeModel treeModel;

    public MyObjectForm() {
        configureTable();
        MyObject myObjectTree = buildRandomTree(5, 5);
        TreeNode rootNode = TreeTableModel.buildTree( myObjectTree, parent -> ((MyObject) parent).getChildren());
        treeTableModel = createTreeTableModel( rootNode);
        treeModel = createTreeModel( rootNode);
        addButtonActionListeners();
    }

    private void addButtonActionListeners() {
        showRootButton.addActionListener(e -> treeTableModel.setShowRoot(!treeTableModel.getShowRoot()));

        insertButton.addActionListener(e -> {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) treeTableModel.getSelectedNode();
            MyObject newObject = new MyObject( getRandomDescription(), random.nextInt(100000000), random.nextBoolean());
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newObject, random.nextBoolean());
            if (treeTableModel.isExpanded(selectedNode)) { // If expanded, insert as first child:
                treeModel.insertNodeInto(newNode, selectedNode, 0);
            } else { // Insert as next sibling of selected node.
                DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) selectedNode.getParent();
                if (parentNode != null) {
                    int insertIndex = parentNode.getIndex(selectedNode) + 1;
                    treeModel.insertNodeInto(newNode, parentNode, insertIndex);
                }
            }
        });

        deleteButton.addActionListener(e -> {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) treeTableModel.getSelectedNode();
            if (selectedNode != treeTableModel.getRoot()) { // can't remove root node - will get illegal argument exception from a tree model.  use set root to change to a new root.
                treeModel.removeNodeFromParent(selectedNode);
            }
        });
    }

    private void configureTable() {
        table1.setRowHeight(24);
    }

    private DefaultTreeModel createTreeModel(TreeNode rootNode) {
        DefaultTreeModel model = new DefaultTreeModel(rootNode);
        model.addTreeModelListener(treeTableModel);
        return model;
    }

    public static void main(String[] args) {
        setSystemLookAndFeel();
        JFrame frame = new JFrame("TreeTable");
        frame.setContentPane(new MyObjectForm().rootPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private TreeTableModel createTreeTableModel(TreeNode rootNode) {
        TreeTableModel localTreeTableModel = new MyObjectTreeTableModel(rootNode, showRoot);
        localTreeTableModel.bindTable(table1); //, new RowSorter.SortKey(0, SortOrder.ASCENDING));
        localTreeTableModel.addExpandCollapseListener(new TreeTableModel.ExpandCollapseListener() {
            @Override
            public boolean nodeExpanding(TreeNode node) {
                if (node.getChildCount() == 0) { // if a node is expanding but has no children, set it to allow no children.
                    ((DefaultMutableTreeNode) node).setAllowsChildren(false);
                }
                return true;
            }
            @Override
            public boolean nodeCollapsing(TreeNode node) {
                return true;
            }
        });
        return localTreeTableModel;
    }

    private MutableTreeNode buildTree() {
        MutableTreeNode rootNode = new DefaultMutableTreeNode(new MyObject("My first test class", 1000, false), true);

        rootNode.insert(new DefaultMutableTreeNode(new MyObject("First child test class", 256, true), false), 0);
        rootNode.insert(new DefaultMutableTreeNode(new MyObject("Second child test class", 32, false), false), 1);

        MutableTreeNode subchildrenNode = new DefaultMutableTreeNode(new MyObject("Third child with children", 16, false), true);

        subchildrenNode.insert(new DefaultMutableTreeNode(new MyObject("First sub child!", 9999, true), false), 0);
        subchildrenNode.insert(new DefaultMutableTreeNode(new MyObject("Second sub child!!", 1111, false), false), 1);
        rootNode.insert(subchildrenNode, 2);

        rootNode.insert(new DefaultMutableTreeNode(new MyObject("Fourth child test class", 32, false), false), 3);
        return rootNode;
    }

    private MyObject buildRandomTree(int maxLevels, int chanceOutOfTenForChildren) {
        random = new Random(1086);
        readWordList();
        MyObject rootObject = new MyObject(getRandomDescription(), random.nextInt(100000000), random.nextBoolean());
        buildRandomChildren(rootObject, maxLevels, chanceOutOfTenForChildren, 0, true);
        return rootObject;
    }

    private void buildRandomChildren(MyObject parent, int maxLevels, int chanceOutOfTenForChildren, int level, boolean forceChildren) {
        boolean hasChildren = level <= maxLevels && random.nextInt(10) < chanceOutOfTenForChildren;
        if (hasChildren || forceChildren) { // force children for root to ensure we get a tree and not a root without children.
            int numChildren = random.nextInt(20) + 1;
            for (int child = 0; child < numChildren; child++) {
                MyObject childObject = new MyObject(getRandomDescription(), random.nextInt(100000000), random.nextBoolean());
                parent.addChild(childObject);
                buildRandomChildren(childObject, maxLevels, chanceOutOfTenForChildren, level + 1, false);
            }
        }
    }

    private void readWordList() {
        try {
            wordList = Files.readAllLines(Paths.get(getFilePath("/wordlist.txt")));
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    private String getRandomDescription() {
        return wordList.get(random.nextInt(wordList.size())) + ' ' + wordList.get(random.nextInt(wordList.size()));
    }

    private String getFilePath(final String resourceName) {
        return this.getClass().getResource(resourceName).getPath();
    }

    private static void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
