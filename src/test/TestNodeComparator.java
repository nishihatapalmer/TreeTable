package test;

import net.byteseek.swing.treetable.TreeTableNode;

import java.util.Comparator;

public class TestNodeComparator implements Comparator<TreeTableNode> {

    @Override
    public int compare(TreeTableNode o1, TreeTableNode o2) {
        if (o1.getAllowsChildren()) {
            if (o2.getAllowsChildren()) {
                return 0;
            }
            return -1;
        } else if (o2.getAllowsChildren()) {
            return 1;
        }
        return 0;
    }
}
