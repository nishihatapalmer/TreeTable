package net.byteseek.swing.treetable;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

public class TreeTableNode extends DefaultMutableTreeNode {

    private boolean expanded;

    public TreeTableNode(Object userObject, boolean allowsChildren) {
        super(userObject, allowsChildren);
    }

    public void setExpanded(final boolean expanded) {
        if (this.expanded != expanded) {
            toggleExpanded();
        }
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void toggleExpanded() {
        expanded = !expanded;
    }

    public void addVisibleChildren(List<TreeTableNode> visibleNodes) {
        if (expanded) { // if expanded, then add in the visible children.
            for (int childIndex = 0; childIndex < getChildCount(); childIndex++) {
                final TreeTableNode child = (TreeTableNode) getChildAt(childIndex);
                visibleNodes.add(child);
                child.addVisibleChildren(visibleNodes); // And the children of the child if they're visible...
            }
        }
    }

    public int getChildVisibleNodeCount() {
        return calculateVisibleChildNodes();
    }

    private int calculateVisibleChildNodes() {
        int totalVisibleChildNodes = 0;
        if (expanded) { // if expanded, then add in the visible children.
            for (int childIndex = 0; childIndex < getChildCount(); childIndex++) {
                totalVisibleChildNodes += (1 + ((TreeTableNode) getChildAt(childIndex)).getChildVisibleNodeCount());
            }
        }
        return totalVisibleChildNodes;
    }

}


