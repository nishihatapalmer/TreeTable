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
package net.byteseek.swing.treetable;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

public class TreeTableNode extends DefaultMutableTreeNode {

    /**
     * Convenience static utility method to build a root TreeTableNode and all sub children given the user object
     * which is the parent, and a ChildProvider which returns the list of children for user objects in the tree.
     * This can often be provided as one line lambda expression.
     *
     * @param parent The user object which is at the root of the tree.
     * @param provider An object which provides a list of user objects from the parent user object.
     * @return A TreeTableNode with all child TreeTableNodes built and associated with their corresponding user objects.
     */
    public static TreeTableNode buildTree(final Object parent, final TreeTableModel.ChildProvider provider) {
        final List<?> children = provider.getChildren(parent);
        final TreeTableNode parentNode = new TreeTableNode(parent, children.size() > 0);
        for (Object child : children) {
            parentNode.add(buildTree(child, provider));
        }
        return parentNode;
    }

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


