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

import java.util.Comparator;
import javax.swing.tree.TreeNode;

/**
 * Static utility class with useful comparators for tree nodes.
 */
public final class Comparators {

    private Comparators() {}

    /**
     * A node comparator that groups nodes by whether they allow children or not.
     * Can be used to group folders and non-folders in a tree, with column sorting within them.
     * This is provided because it's an obvious and easy example of grouping that works with vanilla TreeNodes.
     * <p>
     * Other means of grouping can be performed, using data from user objects in MutableTreeNodes,
     * or other types of TreeNode in your tree, by casting to the type of TreeNode inside your Comparator.
     *
     * Can set in {@link TreeTableModel#setGroupingComparator(Comparator)}.
     */
    public static final Comparator<TreeNode> ALLOWS_CHILDREN = (o1, o2) -> {
        final boolean o1AllowsChildren = o1.getAllowsChildren();
        return o1AllowsChildren == o2.getAllowsChildren() ? 0 : o1AllowsChildren ? -1 : 1;
    };

    /**
     * A node comparator that groups nodes by whether they allow children or not, in a descending order.
     * Can be used to group folders and non-folders in a tree, with column sorting within them.
     * This is provided because it's an obvious and easy example of grouping that works with vanilla TreeNodes.
     * <p>
     * Other means of grouping can be performed, using data from user objects in MutableTreeNodes,
     * or other types of TreeNode in your tree, by casting to the type of TreeNode inside your Comparator.
     *
     * Can set in {@link TreeTableModel#setGroupingComparator(Comparator)}.
     */
    public static final Comparator<TreeNode> ALLOWS_CHILDREN_DESCENDING = (o1, o2) -> {
        final boolean o1AllowsChildren = o1.getAllowsChildren();
        return o1AllowsChildren == o2.getAllowsChildren() ? 0 : o1AllowsChildren ? 1 : -1;
    };

    /**
     * A node comparator that groups nodes by whether they have children or not.
     * Can be used to group folders and non-folders in a tree, with column sorting within them.
     * This is provided because it's an obvious and easy example of grouping that works with vanilla TreeNodes.
     * <p>
     * Other means of grouping can be performed, using data from user objects in MutableTreeNodes,
     * or other types of TreeNode in your tree, by casting to the type of TreeNode inside your Comparator.
     *
     * Can set in {@link TreeTableModel#setGroupingComparator(Comparator)}.
     */
    public static final Comparator<TreeNode> HAS_CHILDREN = (o1, o2) -> {
        final boolean o1HasChildren = o1.getChildCount() > 0;
        return o1HasChildren == o2.getChildCount() > 0 ? 0 : o1HasChildren ? -1 : 1;
    };

    /**
     * A node comparator that groups nodes by whether they have children or not in a descending order.
     * Can be used to group folders and non-folders in a tree, with column sorting within them.
     * This is provided because it's an obvious and easy example of grouping that works with vanilla TreeNodes.
     * <p>
     * Other means of grouping can be performed, using data from user objects in MutableTreeNodes,
     * or other types of TreeNode in your tree, by casting to the type of TreeNode inside your Comparator.
     *
     * Can set in {@link TreeTableModel#setGroupingComparator(Comparator)}.
     */
    public static final Comparator<TreeNode> HAS_CHILDREN_DESCENDING = (o1, o2) -> {
        final boolean o1HasChildren = o1.getChildCount() > 0;
        return o1HasChildren == o2.getChildCount() > 0 ? 0 : o1HasChildren ? 1 : -1;
    };

    /**
     * A static node comparator that groups nodes by the number of children they have, in ascending order.
     * Can be used to group folders sorted by number of children, and non-folders in a tree, with column sorting within them.
     * This is provided because it's an obvious and easy example of grouping that works with vanilla TreeNodes.
     * <p>
     * Other means of grouping can be performed, using data from user objects in MutableTreeNodes,
     * or other types of TreeNode in your tree, by casting to the type of TreeNode inside your Comparator.
     *
     * Can set in {@link TreeTableModel#setGroupingComparator(Comparator)}.
     */
    public static final Comparator<TreeNode> NUM_CHILDREN = Comparator.comparingInt(TreeNode::getChildCount);

    /**
     * A static node comparator that groups nodes by the number of children they have, in descending order.
     * Can be used to group folders sorted by number of children and non-folders in a tree, with column sorting within them.
     * This is provided because it's an obvious and easy example of grouping that works with vanilla TreeNodes.
     * <p>
     * Other means of grouping can be performed, using data from user objects in MutableTreeNodes,
     * or other types of TreeNode in your tree, by casting to the type of TreeNode inside your Comparator.
     *
     * Can set in {@link TreeTableModel#setGroupingComparator(Comparator)}.
     */
    public static final Comparator<TreeNode> NUM_CHILDREN_DESCENDING = (o1, o2) -> o2.getChildCount() - o1.getChildCount();
}
