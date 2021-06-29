package net.byteseek.swing.treetable;

import javax.swing.tree.DefaultMutableTreeNode;

public class TreeTableNode extends DefaultMutableTreeNode {

    private boolean expanded;

    public TreeTableNode(Object userObject, boolean allowsChildren) {
        super(userObject, allowsChildren);
    }

    public void setExpanded(final boolean expanded) {
        this.expanded = expanded;
    }

    public boolean isExpanded() {
        return expanded;
    }

    //TODO: do we need to cache values (and then invalidate the cache on expand / close events)?
    //      an expand or collapse will affect numVisible for all parent nodes.  Then this can be a get() method.
    public int calculateNumVisibleNodes() {
        int totalVisibleNodes = 1;
        if (expanded) { // if expanded, then add in the visible children.
            for (int childIndex = 0; childIndex < getChildCount(); childIndex++) {
                totalVisibleNodes += ((TreeTableNode) getChildAt(childIndex)).calculateNumVisibleNodes();
            }
        }
        return totalVisibleNodes;
    }

}


