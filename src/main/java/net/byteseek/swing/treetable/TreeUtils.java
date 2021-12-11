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
import javax.swing.RowSorter;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.List;
import java.util.function.Function;

/**
 * A collection of miscellaneous static utility methods and objects to build trees, group nodes, process
 * tree nodes and column data.
 */
public final class TreeUtils {

    /**
     * Char constants when building a visual text representation of the tree.
     */
    private static final char TAB_CHAR = '\t';
    private static final char NEW_LINE = '\n';

    /**
     * Static utility class - cannot construct it.
     */
    private TreeUtils() {}

    /**
     * Mirrors a tree of user objects as a tree of MutableTreeNodes, with each MutableTreeNode associated with the
     * appropriate user object.  If your user objects already have a tree structure, this is a quick way to
     * obtain a parallel tree of TreeNodes.  All nodes will be set to allow children.
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
     * @throws NullPointerException if the node passed in is null.
     */
    public static <T> DefaultMutableTreeNode buildTree(final T parent,
                                                       final Function<T, List<T>> childProvider) {
        return buildTree(parent, childProvider, allowsChildren -> true);
    }

    /**
     * Mirrors a tree of user objects as a tree of MutableTreeNodes, with each MutableTreeNode associated with the
     * appropriate user object.  If your user objects already have a tree structure, this is a quick way to
     * obtain a parallel tree of TreeNodes.   It allows you to set whether each node should allow children.
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
     * @param allowChildrenTest A predicate that decides whether a node should have children given the parent object.
     * @return A DefaultMutableTreeNode with all child nodes built and associated with their corresponding user objects.
     * @throws NullPointerException if the node passed in is null.
     */
    public static <T> DefaultMutableTreeNode buildTree(final T parent,
                                                       final Function<T, List<T>> childProvider,
                                                       final Predicate<T> allowChildrenTest) {
        final boolean allowsChildren = allowChildrenTest.test(parent);
        final DefaultMutableTreeNode parentNode = new DefaultMutableTreeNode(parent, allowsChildren);
        if (allowsChildren) {
            final List<T> children = childProvider.apply(parent);
            int indexToInsert = 0;
            for (T child : children) {
                final DefaultMutableTreeNode childNode = buildTree(child, (childProvider), allowChildrenTest);
                parentNode.insert(childNode, indexToInsert++);
            }
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
     * @throws NullPointerException if the node passed in is null.
     */
    public static List<TreeNode> getNodeList(final TreeNode parentNode) {
        final List<TreeNode> results = new ArrayList<>();
        walk(parentNode, results::add);
        return results;
    }

    /**
     * Applies an action to each child of a parent node.
     * @param parentNode the parent node.
     * @param action the action to apply for each child of the parent.
     */
    public static void forEachChild(final TreeNode parentNode, final Consumer<TreeNode> action) {
        for (int childIndex = 0; childIndex < parentNode.getChildCount(); childIndex++) {
            action.accept(parentNode.getChildAt(childIndex));
        }
    }

    /**
     * Applies an action to each child of a parent node if it meets a condition.
     *
     * @param parentNode the parent node.
     * @param condition the condition to meet in order to apply the action.
     * @param action the action to apply for each child of the parent that meets the condition.
     */
    public static void forEachChild(final TreeNode parentNode, final Predicate<TreeNode> condition, final Consumer<TreeNode> action) {
        for (int childIndex = 0; childIndex < parentNode.getChildCount(); childIndex++) {
            final TreeNode child = parentNode.getChildAt(childIndex);
            if (condition.test(child)) {
                action.accept(child);
            }
        }
    }

    /**
     * Builds a list of the children of a parent node.
     *
     * @param parentNode The parent tree node to get the list of children for.
     * @return A list of the children of the parent node.
     * @throws NullPointerException if the node passed in is null.
     */
    public static List<TreeNode> getChildren(final TreeNode parentNode) {
        final List<TreeNode> children = new ArrayList<>();
        forEachChild(parentNode, children::add);
        return children;
    }

    /**
     * Builds a list of the children of a parent node that passes a child predicate filter.
     *
     * @param parentNode The parent tree node to get the list of children for.
     * @param childPredicate The predicate which a child must pass to be included in the list.
     * @return A list of the children of the parent node.
     */
    public static List<TreeNode> getChildren(final TreeNode parentNode, final Predicate<TreeNode> childPredicate) {
        final List<TreeNode> children = new ArrayList<>();
        forEachChild(parentNode, treeNode -> {
            if (childPredicate.test(treeNode)) {
                children.add(treeNode);
            }
        });
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
     * @throws NullPointerException if the node passed in is null.
     */
    public static <T> T getUserObject(final TreeNode node) {
        return (T) ((DefaultMutableTreeNode) node).getUserObject();
    }

    /**
     * Returns the level of the node, zero being the root.
     * @param node The node to get the level for.
     * @return The level of the node.
     * @throws NullPointerException if the node passed in is null.
     */
    public static int getLevel(final TreeNode node) {
        int nodeLevel = 0;
        TreeNode currentNode = node;
        while ((currentNode = currentNode.getParent()) != null) {
            nodeLevel++;
        }
        return nodeLevel;
    }

    /**
     * Returns the ancestor of a TreeNode, given the number of levels down to go.
     *
     * @param node The node to get an ancestor for.
     * @param levelsDown The number of levels down to go (1 = the parent).
     * @return The ancestor of the node.
     */
    public static TreeNode getAncestor(final TreeNode node, final int levelsDown) {
        TreeNode result = node;
        for (int num = 0; num < levelsDown; num++) {
            result = result.getParent();
        }
        return result;
    }

    /**
     * Returns the furthest ancestor that meets the condition from the node passed in,
     * or null if no ancestor meets the condition.
     *
     * @param node The node to find the furthest ancestor of that meets a condition.
     * @param condition The condition to test the ancestors of the node.
     * @return the furthest ancestor that meets the condition from the node passed in,
     *         or null if no ancestor meets the condition.
     */
    public static TreeNode getFurthestAncestor(final TreeNode node, final Predicate<TreeNode> condition) {
        TreeNode furthestAncestor = null, currentParent = node;
        while ((currentParent = currentParent.getParent()) != null) {
            if (!condition.test(currentParent)) {
                break;
            }
            furthestAncestor = currentParent;
        }
        return furthestAncestor;
    }

    /**
     * Calculates the space taken up by columns to the left of a column in the TableColumnModel.
     *
     * @param columnModel The table column model
     * @param colIndex The column index of the column in the table column model (not the model index of the column).
     * @return the space taken up by columns to the left of the column with colIndex in the TableColumnModel.
     */
    public static int calculateWidthToLeftOfColumn(final TableColumnModel columnModel, final int colIndex) {
        int width = 0;
        for (int col = colIndex - 1; col >= 0; col--) {
            width += columnModel.getColumn(col).getWidth();
        }
        return width;
    }

    /**
     * Returns the TableColumn in the TableColumnModel with the given model index, or null if no such column exists.
     * The column index of a column may not match the model index if they have been re-ordered.
     * @param columnModel The TableColumnModel to search.
     * @param modelIndex The model index of the TableColumn requested.
     * @return the TableColumn in the TableColumnModel with the given model index, or null if no such column exists.
     */
    public static TableColumn getColumnWithModelIndex(final TableColumnModel columnModel, final int modelIndex) {
        for (int columnIndex = 0; columnIndex < columnModel.getColumnCount(); columnIndex++) {
            final TableColumn column = columnModel.getColumn(columnIndex);
            if (column.getModelIndex() == modelIndex) {
                return column;
            }
        }
        return null;
    }

    /**
     * Returns a string which (if printed or viewed in a text editor) is a visual representation of the nodes
     * in the list provided.  The nodes are indented according to their level, with each node on a separate line (\n line endings).
     *
     * @param treeNodes The list of tree nodes to build a text representation of.
     * @param includeRowNumbers If true, the row number for each table row will be put at the start of each line.
     * @return a visual text representation of the visible tree.
     */
    public static String buildTextTree(final List<? extends TreeNode> treeNodes, final boolean includeRowNumbers) {
        final StringBuilder builder = new StringBuilder();
        for (int row = 0; row < treeNodes.size(); row++) {
            TreeNode node = treeNodes.get(row);
            if (includeRowNumbers) {
                builder.append(row).append(TAB_CHAR);
            }
            final int depth = TreeUtils.getLevel(node);
            for (int i = 0; i < depth; i++) {
                builder.append(TAB_CHAR);
            }
            builder.append(node).append(NEW_LINE);
        }
        return builder.toString();
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
        forEachChild(node, child -> walk(child, method));
    }

    /**
     * Applies a method to the node passed in and all of its children using a depth-first tree walk.
     * Only applies the method if the predicate test passes, but will still process all of a failed nodes children.
     *
     * @param node The node to walk all children of.
     * @param method The method to apply to each node.
     * @param condition The predicate that decides whether to apply the method.
     */
    public static void walk(final TreeNode node, final Consumer<TreeNode> method, final Predicate<TreeNode> condition) {
        if (condition.test(node)) {
            method.accept(node);
        }
        forEachChild(node, child -> walk(child, method, condition));
    }

    //TODO: do we want a walk that ceases processing childnodes when it fails the predicate.

    //TODO: do we want a walk with a depth limit?


    /**
     * @param sortKeys The sort keys to check.
     * @param modelIndex The model index of the column to check.
     * @return true if a column with the column model index exists in the list of sort keys.
     */
    public static boolean columnInSortKeys(final List<? extends RowSorter.SortKey> sortKeys, final int modelIndex) {
        return findSortKeyIndex(sortKeys, modelIndex) >= 0;
    }

    /**
     * Returns the index of the sort key for a column in a table, or -1 if that column doesn't have a sort key for it.
     * @param sortKeys The list of sort keys to look in.
     * @param modelIndex The model index of the column to find a sort key for.
     * @return The index of the sort key for a column, or -1 if that column doesn't have a sort key for it.
     */
    public static int findSortKeyIndex(final List<? extends RowSorter.SortKey> sortKeys, final int modelIndex) {
        if (sortKeys != null) {
            for (int sortKeyIndex = 0; sortKeyIndex < sortKeys.size(); sortKeyIndex++) {
                RowSorter.SortKey key = sortKeys.get(sortKeyIndex);
                if (key.getColumn() == modelIndex) {
                    return sortKeyIndex;
                }
            }
        }
        return -1;
    }

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
