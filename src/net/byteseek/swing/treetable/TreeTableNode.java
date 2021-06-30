package net.byteseek.swing.treetable;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

public class TreeTableNode extends DefaultMutableTreeNode {

    private boolean expanded;
    private int visibleNodeCount = -1;

    public TreeTableNode(Object userObject, boolean allowsChildren) {
        super(userObject, allowsChildren);
        expanded = true; // TODO: just for debugging.
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

        //TODO: should recalculate and send update notification to all parent nodes?
        visibleNodeCount = expanded ? -1 : 1;
    }

    public int getVisibleNodeCount() {
        if (visibleNodeCount < 1) {
            visibleNodeCount = calculateNumVisibleNodes();
        }
        return visibleNodeCount;
    }

    public void addVisibleChildren(List<TreeTableNode> visibleNodes) {
        if (expanded) { // if expanded, then add in the visible children.
            for (int childIndex = 0; childIndex < getChildCount(); childIndex++) {
                TreeTableNode child = (TreeTableNode) getChildAt(childIndex);
                visibleNodes.add(child);
                child.addVisibleChildren(visibleNodes); // And the children of the child if they're visible...
            }
        }
    }

    //TODO: do we need to cache values (and then invalidate the cache on expand / close events)?
    //      an expand or collapse will affect numVisible for all parent nodes.  Then this can be a get() method.
    private int calculateNumVisibleNodes() {
        int totalVisibleNodes = 1;
        if (expanded) { // if expanded, then add in the visible children.
            for (int childIndex = 0; childIndex < getChildCount(); childIndex++) {
                totalVisibleNodes += ((TreeTableNode) getChildAt(childIndex)).getVisibleNodeCount();
            }
        }
        return totalVisibleNodes;
    }

}


