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

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * A collection of miscellaneous static utility methods and objects to build trees, group nodes and process
 * tree data.
 */
public final class TreeUtils {

    /**
     * Static utility class - cannot construct it.
     */
    private TreeUtils() {}

    /**
     * Mirrors a tree of user objects as a tree of MutableTreeNodes, with each MutableTreeNode associated with the
     * appropriate user object.  If your user objects already have a tree structure, this is a quick way to
     * obtain a parallel tree of TreeNodes.
     *
     * <p>
     * It will build a root DefaultMutableTreeNode and all sub children given the user object which is the parent,
     * and a ChildProvider method which returns the list of children for user objects in the tree.
     * This can be provided as a one line lambda expression, or by implementing the TreeTableModel.ChildProvider interface.
     * <p>
     * Note - this will not keep the tree of MutableTreeNodes up to date if your user object tree structure changes.
     * You must inform the TreeTableModel of any changes to your tree structure to keep it in sync.
     *
     * @param parent The user object which is at the root of the tree.
     * @param childProvider An object which provides a list of user objects from the parent user object.
     * @return A DefaultMutableTreeNode with all child nodes built and associated with their corresponding user objects.
     */
    public static DefaultMutableTreeNode buildTree(final Object parent, final Function<Object, List<?>> childProvider) {
        final List<?> children = childProvider.apply(parent);
        final DefaultMutableTreeNode parentNode = new DefaultMutableTreeNode(parent, children.size() > 0);
        int indexToInsert = 0;
        for (Object child : children) {
            parentNode.insert(buildTree(child, childProvider), indexToInsert++);
        }
        return parentNode;
    }

    /**
     * Returns a list containing the parent node and all children and sub-children in the
     * tree structure under the parent, using a depth-first tree walking strategy.
     * This gives the same (unsorted) order the nodes would have visually in the tree
     * if they were all expanded.
     *
     * @param parentNode The node to get a list of itself and all children and sub-children.
     * @return  a list of all the nodes in the tree structure starting from the parent node.
     */
    public static List<TreeNode> getNodeList(final TreeNode parentNode) {
        final List<TreeNode> results = new ArrayList<>();
        walk(parentNode, results::add);
        return results;
    }

    /**
     * @param parentNode The parent tree node to get the list of children for.
     * @return A list of the children of the parent node.
     */
    public static List<TreeNode> getChildren(final TreeNode parentNode) {
        final List<TreeNode> children = new ArrayList<>();
        for (int childIndex = 0; childIndex < parentNode.getChildCount(); childIndex++) {
            children.add(parentNode.getChildAt(childIndex));
        }
        return children;
    }

    /**
     * Returns the user object associated with a DefaultMutableTreeNode, given only a TreeNode type.
     * This is a convenience method to avoid typing all the double-casting to DefaultMutableTreeNode
     * and then to the type of the user object.
     *
     * @param node The TreeNode to obtain the user object from (which must be a DefaultMutableTreeNode).
     * @param <T> The type of the user object.
     * @return The user object associated with the DefaultMutableTreeNode passed in.
     */
    public static <T> T getObject(final TreeNode node) {
        return (T) ((DefaultMutableTreeNode) node).getUserObject();
    }

    /**
     * Applies a method to the node passed in and all of its children using a depth first
     * tree walk.
     *
     * @param node The node to walk all children of.
     * @param method The method to apply to each node.
     */
    public static void walk(final TreeNode node, final Consumer<TreeNode> method) {
        method.accept(node);
        for (int childIndex = 0; childIndex < node.getChildCount(); childIndex++) {
            walk(node.getChildAt(childIndex), method);
        }
    }

    /**
     * Applies a method to the node passed in and all of its children using a depth-first tree walk.
     * Only applies the method if the predicate test passes, but will still process all of a failed nodes children.
     *
     * @param node The node to walk all children of.
     * @param method The method to apply to each node.
     * @param predicate The predicate that decides whether to apply the method.
     */
    public static void walk(final TreeNode node, final Consumer<TreeNode> method, final Predicate<TreeNode> predicate) {
        if (predicate.test(node)) {
            method.accept(node);
        }
        for (int childIndex = 0; childIndex < node.getChildCount(); childIndex++) {
            walk(node.getChildAt(childIndex), method, predicate);
        }
    }

    /**
     * A node comparator that groups nodes by whether they allow children or not.
     * Can be used to group folders and non-folders in a tree, with column sorting within them.
     * This is provided because it's an obvious and easy example of grouping that works with vanilla TreeNodes.
     * <p>
     * Other means of grouping can be performed, using data from user objects in MutableTreeNodes,
     * or other types of TreeNode in your tree, by casting to the type of TreeNode inside your Comparator.
     *
     * Can set in {@link net.byteseek.swing.treetable.TreeTableModel#setGroupingComparator(Comparator)}.
     */
    public static final Comparator<TreeNode> GROUP_BY_ALLOWS_CHILDREN = (o1, o2) -> {
        final boolean allowsChildren = o1.getAllowsChildren();
        return allowsChildren == o2.getAllowsChildren() ? 0 : allowsChildren ? -1 : 1;
    };

    /**
     * A node comparator that groups nodes by whether they allow children or not, in a descending order.
     * Can be used to group folders and non-folders in a tree, with column sorting within them.
     * This is provided because it's an obvious and easy example of grouping that works with vanilla TreeNodes.
     * <p>
     * Other means of grouping can be performed, using data from user objects in MutableTreeNodes,
     * or other types of TreeNode in your tree, by casting to the type of TreeNode inside your Comparator.
     *
     * Can set in {@link net.byteseek.swing.treetable.TreeTableModel#setGroupingComparator(Comparator)}.
     */
    public static final Comparator<TreeNode> GROUP_BY_ALLOWS_CHILDREN_DESCENDING = (o1, o2) -> {
        final boolean allowsChildren = o1.getAllowsChildren();
        return allowsChildren == o2.getAllowsChildren() ? 0 : allowsChildren ? 1 : -1;
    };

    /**
     * A node comparator that groups nodes by whether they have children or not.
     * Can be used to group folders and non-folders in a tree, with column sorting within them.
     * This is provided because it's an obvious and easy example of grouping that works with vanilla TreeNodes.
     * <p>
     * Other means of grouping can be performed, using data from user objects in MutableTreeNodes,
     * or other types of TreeNode in your tree, by casting to the type of TreeNode inside your Comparator.
     *
     * Can set in {@link net.byteseek.swing.treetable.TreeTableModel#setGroupingComparator(Comparator)}.
     */
    public static final Comparator<TreeNode> GROUP_BY_HAS_CHILDREN = (o1, o2) -> {
        final boolean hasChildren = o1.getChildCount() > 0;
        return hasChildren == o2.getChildCount() > 0 ? 0 : hasChildren ? -1 : 1;
    };

    /**
     * A node comparator that groups nodes by whether they have children or not in a descending order.
     * Can be used to group folders and non-folders in a tree, with column sorting within them.
     * This is provided because it's an obvious and easy example of grouping that works with vanilla TreeNodes.
     * <p>
     * Other means of grouping can be performed, using data from user objects in MutableTreeNodes,
     * or other types of TreeNode in your tree, by casting to the type of TreeNode inside your Comparator.
     *
     * Can set in {@link net.byteseek.swing.treetable.TreeTableModel#setGroupingComparator(Comparator)}.
     */
    public static final Comparator<TreeNode> GROUP_BY_HAS_CHILDREN_DESCENDING = (o1, o2) -> {
        final boolean hasChildren = o1.getChildCount() > 0;
        return hasChildren == o2.getChildCount() > 0 ? 0 : hasChildren ? 1 : -1;
    };

    /**
     * A static node comparator that groups nodes by the number of children they have, in ascending order.
     * Can be used to group folders sorted by number of children, and non-folders in a tree, with column sorting within them.
     * This is provided because it's an obvious and easy example of grouping that works with vanilla TreeNodes.
     * <p>
     * Other means of grouping can be performed, using data from user objects in MutableTreeNodes,
     * or other types of TreeNode in your tree, by casting to the type of TreeNode inside your Comparator.
     *
     * Can set in {@link net.byteseek.swing.treetable.TreeTableModel#setGroupingComparator(Comparator)}.
     */
    public static final Comparator<TreeNode> GROUP_BY_NUM_CHILDREN = Comparator.comparingInt(TreeNode::getChildCount);

    /**
     * A static node comparator that groups nodes by the number of children they have, in descending order.
     * Can be used to group folders sorted by number of children and non-folders in a tree, with column sorting within them.
     * This is provided because it's an obvious and easy example of grouping that works with vanilla TreeNodes.
     * <p>
     * Other means of grouping can be performed, using data from user objects in MutableTreeNodes,
     * or other types of TreeNode in your tree, by casting to the type of TreeNode inside your Comparator.
     *
     * Can set in {@link net.byteseek.swing.treetable.TreeTableModel#setGroupingComparator(Comparator)}.
     */
    public static final Comparator<TreeNode> GROUP_BY_NUM_CHILDREN_DESCENDING = (o1, o2) -> o2.getChildCount() - o1.getChildCount();

    /**
     * Given a sorted array of indices, and a position to start looking in them,
     * it returns the index of the last consecutive index, or the from index passed in if none exist.
     * For example, if we have [1, 2, 5, 6, 7, 8, 10], then calling this from 0, would return 1, as we have 1 and 2
     * consecutive, so the index of 2 is returned.  If we call it on 1, we get 1 returned, as the next index isn't 3.
     * If we call it on index 2, we get 5 returned, the index of 8 which is the last in a consecutive run of indexes
     * from 5.
     * <p>
     * This is used to optimise notifying the table when multiple child nodes are inserted or removed.  We can translate
     * the table notification messages for consecutive indices, as that maps to the table structure.  So we group
     * consecutive updates together.
     *
     * @param from The position in the indices to start looking in.
     * @param sortedIndices A sorted array of index positions, some of which may be consecutive.
     * @return The index of the last consecutive index in the array, or from if there are no others.
     */
    public static int findLastConsecutiveIndex(final int from, final int[] sortedIndices) {
        int lastConsecutiveIndex = from;
        for (int i = from + 1; i < sortedIndices.length; i++) {
            if (sortedIndices[i] - sortedIndices[i - 1] != 1) { // not a gap of one?  stop looking.
                break;
            }
            lastConsecutiveIndex = i; // one apart - update last consecutive index.
        }
        return lastConsecutiveIndex;
    }

}
