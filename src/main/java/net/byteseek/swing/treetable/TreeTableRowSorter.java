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

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.util.*;

/**
 * A class which sorts a TreeTableModel, given the sort keys to sort on.
 * It provides an index of the view to model, and the model to view.
 * It can be provided to a JTable to sort the rows.
 */
public class TreeTableRowSorter extends RowSorter<TreeTableModel> {

    /**
     * Configurable strategy to determine what columns are sorted, in what way,
     * after a request to sort on a column is made.
     */
    public interface ColumnSortStrategy {

        /**
         * Builds a list of sort keys reflecting what the new state of sorting should be,
         * after a request to sort on a column is made.
         *
         * @param columnToSort The column which should be sorted.
         * @param sortKeys The current state of sorting.
         */
        List<RowSorter.SortKey> buildNewSortKeys(int columnToSort, List<RowSorter.SortKey> sortKeys);
    }

    private static final int[] EMPTY_ARRAY = new int[0];

    /**
     * The model being sorted.
     */
    protected final TreeTableModel model;

    /**
     * The list of current sort keys.  This must not be null but it can be empty.
     */
    protected List<SortKey> sortKeys;

    /**
     * The list of default sort keys which are used if no other sort specified.
     */
    protected List<SortKey> defaultSortKeys;

    /**
     * The sort strategy to use to build new sort keys after sort is requested on a column.
     * This lets us change the behaviour when a column is clicked on to sort.  For example,
     * we could make it the primary sort column, or add it to the existing sort columns, or remove other columns.
     * Defaults to the {@link TreeTableSortStrategy} if not supplied.
     */
    protected ColumnSortStrategy sortStrategy;

    /**
     * A sorted index of rows, giving the model index for each view index.
     */
    protected SortRow[] viewToModelIndex;

    /**
     * A sorted index of rows, giving the view index for a model index.
     */
    protected int[] modelToViewIndex;

    /**
     * The row count after the sort arrays are built following changes.
     * Needed so we can size an array to the exact number of rows for a RowSorterEvent.
     * The client (JTable) of these events assumes the length of the array is the number of rows.
     * Our own arrays can be bigger than the number of rows, so this assumption doesn't hold.
     * By the time we need this, the model has already changed, but the new sort indexes have not yet been
     * defined.  So we can't get the row count from the model - we have to cache the last count that was built.
     */
    protected int lastRowCount;

    /**
     * Constructs a TreeTableRowSorter given a TreeTableModel.
     * @param model The TreeTableModel to sort.
     */
    public TreeTableRowSorter(final TreeTableModel model) {
        this(model, Collections.emptyList());
    }

    /**
     * Constructs a TreeTableRowSorter given a TreeTableModel and one or more default sort keys.
     * @param model The tree table model to sort.
     * @param defaultSortKeys The default sort if no other sort is defined.
     */
    public TreeTableRowSorter(final TreeTableModel model, final SortKey defaultSortKeys) {
        this(model, List.of(defaultSortKeys));
    }

    /**
     * Constructs a TreeTableRowSorter given a TreeTableModel.
     * @param model The TreeTableModel to sort.
     * @param defaultSortKeys The default sort if no other sort is defined.
     */
    public TreeTableRowSorter(final TreeTableModel model, final List<SortKey> defaultSortKeys) {
        if (model == null) {
            throw new IllegalArgumentException("Must supply a non null TreeTableModel.");
        }
        this.model = model;
        this.defaultSortKeys = defaultSortKeys == null ? Collections.emptyList() : defaultSortKeys;
        sortKeys = defaultSortKeys == null? new ArrayList<>() : new ArrayList<>(defaultSortKeys);
        buildSortIndexes(); // even if no columns are sorted, we might have node comparison sorts.
    }

    @Override
    public TreeTableModel getModel() {
        return model;
    }

    //TODO: check column index is same if columns are re-ordered.
    @Override
    public void toggleSortOrder(final int column) {
        checkColumnIndex(column); //TODO: why is this the only method to check the column index?
        setSortKeys(getSortStrategy().buildNewSortKeys(column, sortKeys));
    }

    @Override
    public int convertRowIndexToModel(final int index) {
        return isSorting() ? viewToModelIndex[index].modelIndex : index;
    }

    @Override
    public int convertRowIndexToView(final int index) {
        return isSorting() ? modelToViewIndex[index] : index;
    }

    @Override
    public void setSortKeys(final List<? extends SortKey> keys) {
        final List<? extends SortKey> newKeys = keys == null || keys.isEmpty() ? defaultSortKeys : keys;
        if (!sortKeys.equals(newKeys)) {
            this.sortKeys = new ArrayList<>(newKeys);
            fireSortOrderChanged(); //DefaultRowSorter fires before it actually does the sort, so we do that here.
            buildSortIndexes();
        }
    }

    @Override
    public List<? extends SortKey> getSortKeys() {
        return sortKeys;
    }

    @Override
    public int getViewRowCount() {
        return model.getRowCount();
    }

    @Override
    public int getModelRowCount() {
        return model.getRowCount();
    }

    @Override
    public void modelStructureChanged() {
        buildSortIndexes();
    }

    @Override
    public void allRowsChanged() {
        buildSortIndexes();
    }

    @Override
    public void rowsInserted(final int firstRow, final int endRow) {
       if (isSorting()) {
           buildSortIndexes();
       }
    }

    @Override
    public void rowsDeleted(final int firstRow, final int endRow) {
        if (isSorting()) {
            buildSortIndexes();
        }
    }

    @Override
    public void rowsUpdated(final int firstRow, final int endRow) {
        if (isSorting()) {
            buildSortIndexes();
        }
    }

    @Override
    public void rowsUpdated(final int firstRow, final int endRow, final int column) {
        if (isSorting()) {
            buildSortIndexes();
        }
    }

    /**
     * @return the SortStrategy for this RowSorter.  If none is defined, a {@link TreeTableSortStrategy} will be created.
     */
    public ColumnSortStrategy getSortStrategy() {
        if (sortStrategy == null) {
            sortStrategy = new TreeTableSortStrategy();
        }
        return sortStrategy;
    }

    /**
     * Sets the ColumnSortStrategy for this RowSorter.  If set to null, it will revert to a {@link TreeTableRowSorter}.
     * @param sortStrategy The ColumnSortStrategy to use.
     */
    public void setSortStrategy(final ColumnSortStrategy sortStrategy) {
        this.sortStrategy = sortStrategy;
    }

    /**
     * Gets the list of sort keys to use if no other sort is defined.
     * @return The list of sort keys to use if no other sort is defined.
     */
    public List<SortKey> getDefaultSortKeys() {
        return defaultSortKeys;
    }

    /**
     * Sets the default sort keys to use if no other sort is defined.  Ensures will never set to null.
     * @param newDefaults The new default sort keys to use.
     */
    public void setDefaultSortKeys(final List<SortKey> newDefaults) {
        this.defaultSortKeys = newDefaults == null? Collections.emptyList() : newDefaults;
        if (!defaultSortKeys.isEmpty()) {                    // If we have some keys as defaults
            if (sortKeys == null || sortKeys.isEmpty()) {    // And we currently have no sort keys defined:
                setSortKeys(newDefaults);                    // Set the defaults as the new sort keys.
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + model + ')';
    }

    /**
     * Compares two nodes in the tree table mode given their row indexes.
     * This method ensures that only nodes with common parents are compared.
     * If they are at different levels in the tree, nodes are walked up their parents until a common
     * parent is determined (the root is the common parent of all nodes ultimately).
     *
     * @param modelRowIndex1 The index of node 1.
     * @param modelRowIndex2 The index of node 2
     * @return Whether node 1 is less than (<0), equal to (=0) or greater than (>0) node 2.
     */
    protected int compare(final int modelRowIndex1, final int modelRowIndex2) {
        TreeNode firstNode = model.getNodeAtModelIndex(modelRowIndex1);
        TreeNode secondNode = model.getNodeAtModelIndex(modelRowIndex2);

        // If the nodes don't already share a parent, we have to find two comparable parent nodes that do.
        if (firstNode.getParent() != secondNode.getParent()) {

            // If the nodes are at different levels, walk one of them back, so they are at the same level as each other.
            final int firstLevel = getLevel(firstNode);
            final int secondLevel = getLevel(secondNode);
            if (firstLevel < secondLevel) {
                secondNode = getAncestor(secondNode, secondLevel - firstLevel);
            } else if (secondLevel < firstLevel) {
                firstNode =  getAncestor(firstNode, firstLevel - secondLevel);
            }

            // They are now both at the same level - find the nodes that share a common parent (root will be common to all).
            while (firstNode.getParent() != secondNode.getParent()) {
                firstNode = firstNode.getParent();
                secondNode = secondNode.getParent();
            }
        }

        // Nodes share a common parent - compare values:
        return compareWithCommonParent(firstNode, secondNode, modelRowIndex1 - modelRowIndex2);
    }

    /**
     * Compares two nodes (which must have a common parent),
     *
     * @param firstNode The first node to compare
     * @param secondNode The second node to compare
     * @param unsortedCompare A comparison result if they are not sorted.
     * @return The result of comparing the first and second node.
     */
    protected final int compareWithCommonParent(final TreeNode firstNode, final TreeNode secondNode, final int unsortedCompare) {
        final int nodeGrouping = groupNodes(firstNode, secondNode);
        if (nodeGrouping != 0) { // if we have a result from grouping, apply it.
            return nodeGrouping;
        }
        final List<SortKey> localKeys = sortKeys;
        // Go through all the sort keys to find something less than or bigger than.  If equal, try the next sort key.
        for (int sortIndex = 0; sortIndex < localKeys.size(); sortIndex++) {
            final SortKey key = localKeys.get(sortIndex);
            final SortOrder order = key.getSortOrder();
            if (order == SortOrder.UNSORTED) {
                return unsortedCompare;
            }
            final int result = compareNodeColumns(firstNode, secondNode, key.getColumn());
            if (result != 0) {  // if not equal, we have a definite unequal result - return it.
                return order == SortOrder.ASCENDING? result: result * -1; // invert the result if not ascending
            }
        }
        return unsortedCompare; // if equal, fall back to model order.  This is the same behaviour as DefaultRowSorter.
    }

    protected int groupNodes(final TreeNode firstNode, final TreeNode secondNode) {
        final Comparator<TreeNode> nodeComparator = model.getGroupingComparator();
        return nodeComparator == null ? 0 : nodeComparator.compare(firstNode, secondNode);
    }

    /**
     * Compares two nodes (which are assumed to have a common parent), given a particular column to compare the values of.
     *
     * @param firstNode The first node to compare
     * @param secondNode The second node to compare
     * @param column The column value to compare.
     * @return The result of comparing the first and second node on the specified column.
     */
    protected int compareNodeColumns(final TreeNode firstNode, final TreeNode secondNode, final int column) {
        final TreeTableModel localModel = model;
        final int result;
        final Object value1 = localModel.getColumnValue(firstNode, column);
        final Object value2 = localModel.getColumnValue(secondNode, column);
        final Comparator<Object> columnComparator = (Comparator<Object>) localModel.getColumnComparator(column);
        if (columnComparator != null) {
            result = columnComparator.compare(value1, value2);            // Compare with provided comparator.
        } else {
            if (value1 instanceof Comparable<?>) { // We assume that all values in the same column will be equivalent/comparable if one is.
                result = ((Comparable<Object>) value1).compareTo(value2); // Compare directly if Comparable<>
            } else {
                result = value1.toString().compareTo(value2.toString());  // Compare on string values:
            }
        }
        return result;
    }

    /**
     * Builds (or clears) the sort indexes, depending on the SortKeys definition.
     */
    private void buildSortIndexes() {
        if (sortKeys.size() > 0 || model.getGroupingComparator() != null) {
            sort();
        } else {
            clearSortIndexes();
        }
    }

    /**
     * Clears the sort indexes.
     */
    private void clearSortIndexes() {
        viewToModelIndex = null;
        modelToViewIndex = null;
    }

    /**
     * Creates sort indexes and notifies a sort change.
     */
    private void sort() {
        final int[] currentViewToModelIndex = buildViewToModelAsInts();
        buildViewToModelIndex();
        buildModelToViewIndex();
        fireRowSorterChanged(currentViewToModelIndex);
    }

    /**
     * Builds an array of SortRows that have model indexes.  These are then sorted using SortRow.compare.
     */
    private void buildViewToModelIndex() {
        final int newRowCount = model.getRowCount();
        // If we don't have an index, or the index is too small, create a new one.
        if (viewToModelIndex == null || viewToModelIndex.length < newRowCount) {
            viewToModelIndex = new SortRow[newRowCount];
        }
        createModelOrderRows(newRowCount);
        Arrays.sort(viewToModelIndex, 0, newRowCount); // The array can be bigger than the row count - only sort the valid rows.
        lastRowCount = newRowCount;
    }

    /**
     * Builds the reverse index, of the model to the view, given a view to model index.
     * Must build the view to model index first.
     */
    private void buildModelToViewIndex() {
        final int numRows = model.getRowCount();
        // if we don't have an index, or the existing array is less than the size we need, create a new one.
        if (modelToViewIndex == null || modelToViewIndex.length < numRows) {
            modelToViewIndex = new int[numRows];
        }
        for (int viewIndex = 0; viewIndex < numRows; viewIndex++) {
            modelToViewIndex[viewToModelIndex[viewIndex].modelIndex] = viewIndex;
        }
    }

    /**
     * Builds a copy of the view to model index as an integer array.
     * This is needed by JTable to allow selection (other things?) to work properly when the model and sort changes.
     *
     * @return An integer array containing the viewToModel index, or an empty array if there is no index.
     */
    private int[] buildViewToModelAsInts() {
        final SortRow[] localViewToModel = viewToModelIndex;
        if (localViewToModel != null) {
            final int[] result = new int[lastRowCount]; // must be the length before any changes made to view model index.
            for (int index = 0; index < lastRowCount; index++) {
                final SortRow row = localViewToModel[index];
                //TODO: should we try to detect nulls and replace with -1 (which is an invalid index)???  Or just throw error.
                //      Is expectation that all available array rows have a SortRow object valid...?
                result[index] = row == null? - 1 : row.modelIndex;
            }
            return result;
        }
        return EMPTY_ARRAY;
    }

    /**
     * @return true if we are sorting.
     */
    private boolean isSorting() {
        return viewToModelIndex != null; // invariant assumption: sorting if viewToModel index is not null.
    }

   /**
     * Ensures all the view to model indexes have a SortRow with the model index set to the view index.
     *
     * @param numRows The number of rows to define.
     */
    private void createModelOrderRows(final int numRows) {
        final SortRow[] sortRows = viewToModelIndex;
        for (int index = 0; index < numRows; index++) {
            if (sortRows[index] == null) {
                sortRows[index] = new SortRow(this, index);
            } else {
                sortRows[index].setModelIndex(index);
            }
        }
    }

    /**
     * Returns the ancestor of a TreeTableNode, given the number of levels down to go.
     *
     * @param node The node to get an ancestor for.
     * @param levelsDown The number of levels down to go (1 = the parent).
     * @return The ancestor of the node.
     */
    private TreeNode getAncestor(final TreeNode node, final int levelsDown) {
        TreeNode result = node;
        for (int num = 0; num < levelsDown; num++) {
            result = result.getParent();
        }
        return result;
    }

    /**
     * Returns the level of the node, zero being the root.
     * @param node The node to get the level for.
     * @return The level of the node.
     */
    private int getLevel(final TreeNode node) {
        int nodeLevel = 0;
        TreeNode currentNode = node;
        while ((currentNode = currentNode.getParent()) != null) {
            nodeLevel++;
        }
        return nodeLevel;
    }

    /**
     * Ensures the column index is within the bounds of the model.
     * @param column The column index to check.
     * @throws IllegalArgumentException if the column isn't within the bounds of the model.
     */
    private void checkColumnIndex(int column) {
        if (column < 0 || column >= model.getColumnCount()) {
            throw new IndexOutOfBoundsException("Column " + column + " must be less than " + model.getColumnCount() + " and zero or greater.");
        }
    }

    /**
     * A class carrying a model index, which can be placed into an array to sort using the SortRow.compare method.
     */
    private static class SortRow implements Comparable<SortRow> {

        private final TreeTableRowSorter rowSorter;
        private int modelIndex;

        public SortRow(final TreeTableRowSorter sorter, final int modelIndex) {
            this.rowSorter = sorter;
            this.modelIndex = modelIndex;
        }

        @Override
        public int compareTo(final SortRow o) {
            return rowSorter.compare(modelIndex, o.modelIndex);
        }

        public void setModelIndex(final int modelIndex) {
            this.modelIndex = modelIndex;
        }
    }

}
