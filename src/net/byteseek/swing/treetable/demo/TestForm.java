package net.byteseek.swing.treetable.demo;

import net.byteseek.swing.treetable.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
        createTreeTable();
        addNodes.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showRoot = !showRoot;
                treeTableModel.setShowRoot(showRoot);
            }
        });
    }

    public static void main(String[] args) {
        setSystemLookAndFeel();

        JFrame frame = new JFrame("Test");
        frame.setContentPane(new TestForm().rootPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private void createTreeTable() {
        random = new Random(0);
        readWordList();
        TreeTableNode rootNode = buildRandomTree(4, 5);
        treeTableModel = new TestTreeTableModel(rootNode, showRoot);
        treeTableModel.bindTable(table1);
        table1.setRowHeight(24);
    }



    private TreeTableNode buildTree() {
        TreeTableNode rootNode = new TreeTableNode(new TestClass("My first test class", 1000, false), true);

        rootNode.add(new TreeTableNode(new TestClass("First child test class", 256, true), false));
        rootNode.add(new TreeTableNode(new TestClass("Second child test class", 32, false), false));

        TreeTableNode subchildrenNode = new TreeTableNode(new TestClass("Third child with children", 16, false), true);

        subchildrenNode.add(new TreeTableNode(new TestClass("First sub child!", 9999, true), false));
        subchildrenNode.add(new TreeTableNode(new TestClass("Second sub child!!", 1111, false), false));
        rootNode.add(subchildrenNode);

        rootNode.add(new TreeTableNode(new TestClass("Fourth child test class", 32, false), false));
        return rootNode;
    }

    private TreeTableNode buildRandomTree(int maxLevels, int chanceOutOfTenForChildren) {
        TreeTableNode rootNode = randomTestNode(true);
        buildRandomChildren(rootNode, maxLevels, chanceOutOfTenForChildren);
        return rootNode;
    }

    private void buildRandomChildren(TreeTableNode parentNode, int maxLevels, int chanceOutOfTenForChildren) {
        boolean allowsChildren = parentNode.getAllowsChildren() && parentNode.getLevel() < maxLevels;
        if (allowsChildren) {
            int numChildren = random.nextInt(50);
            for (int child = 0; child < numChildren; child++) {
                TreeTableNode newChild = randomTestNode(random.nextInt(10) >= chanceOutOfTenForChildren && allowsChildren);
                parentNode.add(newChild);
                buildRandomChildren(newChild, maxLevels, chanceOutOfTenForChildren);
            }
        }
    }

    private void readWordList() {
        try {
            wordList = Files.readAllLines(Paths.get("/home/matt/english2.txt"));
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    private TreeTableNode randomTestNode(boolean allowsChildren) {
        TestClass randomClass = new TestClass(getRandomDescription(), random.nextInt(100000000), random.nextBoolean());
        return new TreeTableNode(randomClass, allowsChildren);
    }

    private String getRandomDescription() {
        return wordList.get(random.nextInt(wordList.size())) + ' ' + wordList.get(random.nextInt(wordList.size()));
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