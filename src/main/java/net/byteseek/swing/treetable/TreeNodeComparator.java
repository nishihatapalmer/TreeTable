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
import java.util.List;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.tree.TreeNode;

/**
 * Compares two tree nodes to see if the first is smaller, equal or bigger than the second.
 * <b>It does NOT support comparing null TreeNodes</b> as null nodes are not compared by byteseek.
 * Consistent behaviour is not guaranteed if null nodes are passed in,
 * but it will probably throw a NullPointerException.
 * <p>
 * If first attempts to use a grouping comparator, if one is set on the TreeTableModel.
 * If no grouping comparator is set, or the grouping comparator says the nodes are equal,
 * it will process any sort columns which are set.  For the two values of the column defined by each sort key,
 * it will first do a null value comparison (null is smaller than not-null, two nulls are equal).
 * If the values aren't null, it will try to use a custom comparator defined for that column provided by the model.
 * If no custom comparator is set, or the custom comparator says the nodes are equal,
 * it will compare the node values for that column directly if they implement Comparable and are the same class.
 * If the values were not comparable, or they compare to equal, then
 * a string comparison of the object values will be used.
 * <p>
 *     <b>Null Warning</b>
 * It does not compare null TreeNodes, and will probably throw a NullPointerException if any are compared.
 * It can compare null column values of the TreeNodes.
 * This is an entirely selfish optimisation, as when this class is used in byteseek, we already know that the tree nodes
 * are not null, so the null checks would be wasted in every case.  If you want a null check version of this comparator,
 * create a subclass that does the null check in the overridden compare method, then delegate to super.compare().
 * <p>
 *     <b>Tree Sorting</b>
 * Note: to obtain an ordering of tree nodes in a tree, you must only compare nodes which are siblings.
 * To compare nodes which are not siblings, you must find the node or ancestors of those nodes which share a common
 * parent, and then compare those nodes.  This will give the relative ordering of the two original nodes.
 */
public class TreeNodeComparator implements Comparator<TreeNode> {

    protected static final int LESS_THAN    = -1;
    protected static final int EQUAL_VALUE  =  0;
    protected static final int GREATER_THAN =  1;

    protected final TreeTableModel model;

    /**
     * Constructs a TreeNodeComparator with the given TreeTableModel.
     *
     * @param model The TreeTableModel to obtain column values and sort keys from.
     */
    public TreeNodeComparator(final TreeTableModel model) {
        this.model = model;
    }

    @Override
    public int compare(final TreeNode node1, final TreeNode node2) {
        final TreeTableModel localModel = model; // use a local reference to avoid repeated field access.

        // If a grouping comparator is set on the model, use that first:
        final Comparator<TreeNode> groupingComparator = localModel.getGroupingComparator();
        if (groupingComparator != null) {
            final int comparison = groupingComparator.compare(node1, node2);

            // If one is definitely smaller than the other, return a result:
            if (comparison != EQUAL_VALUE) {
                /*
                 * Grouping comparators don't have a sort direction like ASCENDING or DESCENDING, since they
                 * are not defined by a sort key on a column.  If you want a grouping comparator that sorts in the
                 * other direction, specify a different grouping comparator that reverses the results.
                 * We just return the direct result of grouping comparison here.
                 */
                return comparison;
            }
        }

        // For all columns with a SortKey that are not UNSORTED:
        final List<? extends RowSorter.SortKey> keys = localModel.getSortKeys();
        for (RowSorter.SortKey sortKey : keys) {

            final SortOrder order = sortKey.getSortOrder();
            if (order != SortOrder.UNSORTED) {

                // Compare the values of the columns for those nodes:
                final int comparison = compareValues(node1, node2, sortKey);

                // If one is definitely smaller than the other, return a result:
                if (comparison != EQUAL_VALUE) {
                    /*
                     * SortKeys specify a direction for the sort - ASCENDING or DESCENDING.
                     * Make sure we reverse the comparison result if it is DESCENDING order.
                     */
                    return order == SortOrder.ASCENDING ? comparison : -comparison;
                }
            }
        }

        // No other sort comparison available - all sorted column values are equal.
        return EQUAL_VALUE;
    }

    /**
     * Compares the values of two nodes with a given SortKey.
     *
     * @param node1 the first node to compare
     * @param node2 the second node to compare
     * @param sortKey the SortKey which defines the comparison.
     * @return the result of comparing the two nodes.
     */
    protected int compareValues(final TreeNode node1, final TreeNode node2, final RowSorter.SortKey sortKey) {
        final TreeTableModel localModel = model; // reduce field access - use a local reference.
        final int sortedColumn = sortKey.getColumn();
        final Object value1 = localModel.getColumnValue(node1, sortedColumn);
        final Object value2 = localModel.getColumnValue(node2, sortedColumn);

        // Null value comparisons giving a total order.  null is "smaller" than not null, two nulls are equal.
        if (value1 == null || value2 == null) {
            return value1 == value2 ? EQUAL_VALUE : value1 == null? LESS_THAN : GREATER_THAN;
        }

        // Compare values using the best comparator we can find for them:
        final int result;
        final Comparator<?> columnComparator = localModel.getColumnComparator(sortedColumn);
        if (columnComparator != null) { // use defined comparator:
            result = ((Comparator) columnComparator).compare(value1, value2);
        } else if ((value1 instanceof Comparable) && (value2.getClass().equals(value1.getClass()))) { // compare values
            result = ((Comparable) value1).compareTo(value2);
        } else { // Compare them on a simple string comparison.
            result = value1.toString().compareTo(value2.toString());
        }
        return result;
    }

}
