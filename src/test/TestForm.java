package test;

import net.byteseek.swing.treetable.JTreeTable;
import net.byteseek.swing.treetable.TreeTableModel;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

public class TestForm {

    private JPanel panel1;
    private JPanel rootPanel;
    private JLabel topLabel;
    private JScrollPane scrollPane;
    private JTreeTable JTreeTable1;

    public static void main(String[] args) {
        JFrame frame = new JFrame("Test");
        frame.setContentPane(new TestForm().rootPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private void createUIComponents() {
        TestClass object = new TestClass("My first test class", 1000, false);
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(object, true);
        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(new TestClass("First child test class", 256, true), true);
        rootNode.add(childNode);
        rootNode.add(new DefaultMutableTreeNode(new TestClass("Second child test class", 32, false), true));
        TreeTableModel treeTableModel = new TestTreeTableModel(rootNode, false);
        JTreeTable1 = new JTreeTable(treeTableModel);
    }
}
