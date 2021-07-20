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
package net.byteseek.swing.treetable.demo;

import net.byteseek.swing.treetable.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

public class TestForm {

    private JPanel panel1;
    private JPanel rootPanel;
    private JLabel topLabel;
    private JScrollPane scrollPane;
    private JButton addNodes;
    private JTable table1;
    private Random random;
    private List<String> wordList;
    TreeTableModel treeTableModel;
    boolean showRoot;

    public TestForm() {
        createTreeTable(buildRandomTree(5, 5));
        addNodes.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showRoot = !showRoot;
                treeTableModel.setShowRoot(showRoot);
            }
        });
        table1.setRowHeight(24);
    }

    public static void main(String[] args) {
        setSystemLookAndFeel();
        JFrame frame = new JFrame("Test");
        frame.setContentPane(new TestForm().rootPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private void createTreeTable(MyObject objectTree) {
        final TreeTableNode rootNode = TreeTableNode.buildTree(objectTree, parent -> ((MyObject) parent).getChildren());
        treeTableModel = new MyObjectTreeTableModel(rootNode, showRoot);
        treeTableModel.bindTable(table1, new RowSorter.SortKey(0, SortOrder.ASCENDING));
        /*
        treeTableModel.addTreeTableEventListener(new TreeTableEvent.Listener() {
            @Override
            public boolean actionTreeEvent(TreeTableEvent event) {
                if (event.getEventType() == TreeTableEvent.TreeTableEventType.EXPANDING) {
                    TreeTableNode node = event.getNode();
                    if (node.getChildCount() == 0) {
                        node.setAllowsChildren(false); //TODO: status not immediately reflecting in tree after event (no repaint...?)
                    }
                }
                return true;
            }
        });
         */
    }

    private TreeTableNode buildTree() {
        TreeTableNode rootNode = new TreeTableNode(new MyObject("My first test class", 1000, false), true);

        rootNode.add(new TreeTableNode(new MyObject("First child test class", 256, true), false));
        rootNode.add(new TreeTableNode(new MyObject("Second child test class", 32, false), false));

        TreeTableNode subchildrenNode = new TreeTableNode(new MyObject("Third child with children", 16, false), true);

        subchildrenNode.add(new TreeTableNode(new MyObject("First sub child!", 9999, true), false));
        subchildrenNode.add(new TreeTableNode(new MyObject("Second sub child!!", 1111, false), false));
        rootNode.add(subchildrenNode);

        rootNode.add(new TreeTableNode(new MyObject("Fourth child test class", 32, false), false));
        return rootNode;
    }

    private MyObject buildRandomTree(int maxLevels, int chanceOutOfTenForChildren) {
        random = new Random(999);
        readWordList();
        MyObject rootObject = new MyObject(getRandomDescription(), random.nextInt(100000000), random.nextBoolean());
        buildRandomChildren(rootObject, maxLevels, chanceOutOfTenForChildren, 0, true);
        return rootObject;
    }

    private void buildRandomChildren(MyObject parent, int maxLevels, int chanceOutOfTenForChildren, int level, boolean forceChildren) {
        boolean hasChildren = level <= maxLevels && random.nextInt(10) < chanceOutOfTenForChildren;
        if (hasChildren || forceChildren) {
            int numChildren = random.nextInt(50);
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


    //TODO: we build a tree for the tree table, but we never build the actual tree of objects...
    //      another option is to give a root node a root object, and tell it to build all its child nodes
    //      given a mapping from MyObject.getChildren()...

    private TreeTableNode randomTestObject(boolean allowsChildren) {
        MyObject randomClass = new MyObject(getRandomDescription(), random.nextInt(100000000), random.nextBoolean());
        return new TreeTableNode(randomClass, allowsChildren);
    }

    private String getRandomDescription() {
        return wordList.get(random.nextInt(wordList.size())) + ' ' + wordList.get(random.nextInt(wordList.size()));
    }

    private File getFile(final String resourceName) {
        return new File(getFilePath(resourceName));
    }

    private String getFilePath(final String resourceName) {
        return this.getClass().getResource(resourceName).getPath();
    }

    private static void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            // String os = System.getProperty("os.name").toLowerCase();
            // if (os.indexOf("windows") != -1 || os.indexOf("mac os x") != -1)
            // {
            // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            // }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedLookAndFeelException e) {
            throw new RuntimeException(e);
        }
    }

}
