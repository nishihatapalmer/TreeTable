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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.RowSorter;
import javax.swing.tree.TreeNode;
import static net.byteseek.swing.treetable.TreeNodeComparator.EQUAL_VALUE;

/**
 * A class which implements the RowSorter interface, and sorts a TreeTableModel, given the sort keys to sort on.
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

    protected static final int[] EMPTY_ARRAY = new int[0];

    /**
     * Amount the SortRow array is expanded by over the new row count when it needs to resize to be bigger.
     * Having some headroom in the expansion means if a few more nodes are expanded, we don't have to reallocate
     * the SortRow array too often.
     */
    protected static final int EXPAND_SORTROW_SIZE = 128;

    /**
     * The model being sorted.
     */
    protected final TreeTableModel model;

    /**
     * The list of current sort keys.  This must not be null, but it can be empty.
     */
    protected List<SortKey> sortKeys = Collections.emptyList();

    /**
     * The list of default sort keys which are used if no other sort specified.
     */
    protected List<? extends SortKey> defaultSortKeys = Collections.emptyList();

    /**
     * The node comparator to use to compare nodes.
     */
    protected TreeNodeComparator nodeComparator;

    /**
     * The sort strategy to use to build new sort keys after sort is requested on a column.
     * This lets us change the behaviour when a column is clicked on to sort.  For example,
     * we could make it the primary sort column, or add it to the existing sort columns, or remove other columns.
     * Defaults to the {@link net.byteseek.swing.treetable.ColumnSortStrategy} if not supplied.
     */
    protected ColumnSortStrategy sortStrategy;

    /**
     * A sorted index of rows, giving the model index for each view index.
     * In other words, this array is sorted in order of visual display - 0 is the first row, 1 is the second row, etc.
     * Each SortRow object contains the modelIndex where we can find the model index of that table row.
     * So it lets us convert from visual sorted table rows back to the model index.
     * If not sorting, will be null.
     */
    protected SortRow[] viewToModelIndex;

    /**
     * An index to convert back from a model index to the view index, so converts from the model back to the
     * visual table row it represents.  If not sorting, will be null.
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
     * alwaysRebuildIndices mode forces the RowSorter to rebuild the sort indexes entirely on notification of row update, insert and removals.
     * The faster algorithms adjust the indexes in place.  This is mostly for debugging purposes.
     */
    protected boolean rebuildIndices;

    /**
     * Constructs a TreeTableRowSorter given a TreeTableModel.
     * @param model The TreeTableModel to sort.
     * @throws IllegalArgumentException if the model passed in is null.
     */
    public TreeTableRowSorter(final TreeTableModel model) {
        this(model, Collections.emptyList());
    }

    /**
     * Constructs a TreeTableRowSorter given a TreeTableModel and one or more default sort keys.
     * @param model The tree table model to sort.
     * @param defaultSortKeys The default sort if no other sort is defined.
     * @throws IllegalArgumentException if the model passed in is null.
     */
    public TreeTableRowSorter(final TreeTableModel model, final SortKey defaultSortKeys) {
        this(model, List.of(defaultSortKeys));
    }

    /**
     * Constructs a TreeTableRowSorter given a TreeTableModel.
     * @param model The TreeTableModel to sort.
     * @param defaultSortKeys The default sort if no other sort is defined.
     * @throws IllegalArgumentException if the model passed in is null.
     */
    public TreeTableRowSorter(final TreeTableModel model, final List<? extends SortKey> defaultSortKeys) {
        if (model == null) {
            throw new IllegalArgumentException("Must supply a non null TreeTableModel.");
        }
        this.model = model;
        this.nodeComparator = new TreeNodeComparator(model);
        setDefaultSortKeys(defaultSortKeys);
        setSortKeys(this.defaultSortKeys);
        rebuildIndices = true; //TODO: set true if we want things to work until the update/insert code is done.
        buildSortIndices();
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
            this.sortKeys = Collections.unmodifiableList(new ArrayList<>(newKeys));
            /*
             * sort order changed is fired before the sort indices are rebuilt, as is done for DefaultRowSorter.
             * The documentation for RowSorterEvent states that this message is fired first and is typically
             * followed by a SORTED message that indicates the sort order of the contents has actually been modified.
             */
            fireSortOrderChanged();
            buildSortIndices();
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
        buildSortIndices();
    }

    @Override
    public void allRowsChanged() {
        buildSortIndices();
    }

    //TODO: throw out of bonds exceptions

    @Override
    public void rowsInserted(final int firstModelIndex, final int endModelIndex) {
        if (isSorting()) {
            if (shouldRebuildIndex(firstModelIndex, endModelIndex)) {
                buildSortIndices();
            } else { // patch the existing sort index.
               final int[] oldViewToModel = buildViewToModelAsInts();
               insertSortedRows(firstModelIndex, endModelIndex);
               fireRowSorterChanged(oldViewToModel);
            }
        }
    }

    //TODO: profile to see what the difference between removing rows and full sort index rebuild.

    @Override
    public void rowsDeleted(final int firstModelIndex, final int endModelIndex) {
        if (isSorting()) {
            if (shouldRebuildIndex(firstModelIndex, endModelIndex)) {
                buildSortIndices();
            } else { // patch the existing sort index.
                final int[] oldViewToModel = buildViewToModelAsInts();
                removeSortedRows(firstModelIndex, endModelIndex);
                fireRowSorterChanged(oldViewToModel);
            }
        }
    }

    protected boolean shouldRebuildIndex(final int firstModelIndex, final int endModelIndex) {
        return rebuildIndices;
    }

    @Override
    public void rowsUpdated(final int firstModelIndex, final int endModelIndex) {
        if (isSorting()) {
            buildSortIndices();
        }
    }

    @Override
    public void rowsUpdated(final int firstModelIndex, final int endModelIndex, final int column) {
        if (isSorting()) {
            buildSortIndices();
        }
    }

    //TODO: do we need this once we are satisfied through testing and profiling that dynamic updates are better than full rebuilds?
    public boolean getRebuildIndices() {
        return rebuildIndices;
    }

    public void setRebuildIndices(final boolean rebuildIndices) {
        this.rebuildIndices = rebuildIndices;
    }

    protected void insertSortedRows(int firstModelIndex, int endModelIndex) {
        // Initialize useful constants:
        final int[] localModelToViewIndex = modelToViewIndex; // avoid repeated field access, use a local ref.
        final SortRow[] localViewToModelIndex = viewToModelIndex; // avoid repeated field access, use a local ref.
        final int localLastRowCount = lastRowCount;
        final int numToAdd = endModelIndex - firstModelIndex + 1;

        //TODO: deal with array re-sizing of indices.

        // Adjust all view to model entries from the insertion point to the end, to add the number of inserted nodes to their model index.
        // The model indexes are all increased by the number of rows being inserted to the model.
        for (int modelIndex = firstModelIndex; modelIndex < localLastRowCount; modelIndex++) {
            final int viewIndex = localModelToViewIndex[modelIndex];
            localViewToModelIndex[viewIndex].modelIndex += numToAdd;
        }

        // Build new SortRows to insert and sort them relative to each other:
        final SortRow[] newRows = new SortRow[numToAdd];
        for (int i = 0; i < numToAdd; i++) {
            newRows[i] = new SortRow(this, firstModelIndex + i);
        }
        Arrays.sort(newRows);

        // Build an array of all the insertion points in the current index, which will also be in insertion order.
        final int[] insertionPoints = new int[numToAdd];
        for (int i = 0; i < numToAdd; i++) {
            insertionPoints[i] =  -1 -Arrays.binarySearch(localViewToModelIndex, newRows[i]);
            //TODO: could insertion points be identical for new nodes?  seems like they could...
        }

        // Insert a gap for the new nodes in the model to view index://TODO:
       // final int positionAfterDeletions = endModelIndex + 1;
     //   final int remainingToCopyBack = localLastRowCount - positionAfterDeletions;
     //   System.arraycopy(localModelToViewIndex, positionAfterDeletions, localModelToViewIndex, firstModelIndex, remainingToCopyBack);

        //TODO: do we have to insert nodes backwards to avoid over-copying blocks?
        // Insert the new nodes, blocking up consecutive insertions into a single operation:
        int insertIndex = 0;
        while (insertIndex < insertionPoints.length) {
            final int lastInsertIndex = TreeUtils.findLastConsecutiveIndex(insertIndex, insertionPoints);
            final int insertStartPosition = insertionPoints[insertIndex];
            final int insertEndPosition = insertionPoints[lastInsertIndex];


            insertIndex = lastInsertIndex + 1;
        }

    }

    /**
     * Removes rows from the sorted indexes without requiring the view index to be re-sorted and all indexes
     * completely rebuilt, by moving items around and updating items in the indexes.
     * It does require a sort itself of the deleted view indices,although this is a straight sort of integers which
     * will be much more efficient than a tree sort, and isn't the entire tree.
     *
     * @param firstModelIndex The first model index of the nodes being deleted.
     * @param endModelIndex The end model index of the nodes being deleted.
     */
    protected void removeSortedRows(final int firstModelIndex, final int endModelIndex) {
        // Initialize useful constants:
        final int[] localModelToViewIndex = modelToViewIndex; // avoid repeated field access, use a local ref.
        final SortRow[] localViewToModelIndex = viewToModelIndex; // avoid repeated field access, use a local ref.
        final int localLastRowCount = lastRowCount;
        final int numToRemove = endModelIndex - firstModelIndex + 1;

        // Build a sorted list of all view rows removed.
        // This will let us identify consecutive blocks of nodes to move to patch up the view array more efficiently.
        final int[] removedViewRows = new int[numToRemove];
        if (numToRemove > 0) {
            System.arraycopy(localModelToViewIndex, firstModelIndex, removedViewRows, 0, numToRemove);
        }
        Arrays.sort(removedViewRows);

        // Adjust all view entries after the removed rows to fix up their new model indexes now the rows are deleted from the model.
        // The model indexes are all reduced by the number of rows being deleted from the model.
        for (int modelIndex = endModelIndex + 1; modelIndex < localLastRowCount; modelIndex++) {
            final int viewIndex = localModelToViewIndex[modelIndex];
            localViewToModelIndex[viewIndex].modelIndex -= numToRemove;
        }

        //TODO: test with disjoint blocks of nodes removed from view.

        // Delete the model to view removed rows by copying the remaining rows up over them.
        final int positionAfterDeletions = endModelIndex + 1;
        final int remainingToCopyBack = localLastRowCount - positionAfterDeletions;
        System.arraycopy(localModelToViewIndex, positionAfterDeletions, localModelToViewIndex, firstModelIndex, remainingToCopyBack);

        // Shift the view array contents over to cover the removed view rows and update model to view indexes for things which visually move.
        // Identify consecutive blocks of rows removing in the view model.
        // A sorted tree mostly retains consecutive blocks of removed rows, as parent-child relationships largely keep them together.
        // In addition, the removal is probably driven in the first place by collapse of a node (or removal of one).
        // Node collapse is automatically a removal of a visual block of nodes, so this is just re-translating model changes
        // back into a block operation on the view.  So identifying blocks of removals in the sorted view is probably worthwhile.
        int removeIndex = 0;
        int copyPosition = removedViewRows.length > 0 ? removedViewRows[0] : -1;
        while (removeIndex < removedViewRows.length) {
            // Calculate what blocks to move and update:
            final int lastRemoveIndex = TreeUtils.findLastConsecutiveIndex(removeIndex, removedViewRows);
            final int viewRowDeleteEnd = removedViewRows[lastRemoveIndex];
            final int nextBlockStart = lastRemoveIndex + 1 < removedViewRows.length ? removedViewRows[lastRemoveIndex + 1] : localLastRowCount;
            final int numToCopy = nextBlockStart - viewRowDeleteEnd - 1;
            final int numDeleted = viewRowDeleteEnd - copyPosition + 1;

            // Copy a block of view to model rows upwards to cover the deleted rows above.
            System.arraycopy(localViewToModelIndex, viewRowDeleteEnd + 1, localViewToModelIndex, copyPosition, numToCopy);

            // Update model to view indexes for view rows which have moved:
            for (int viewIndex = copyPosition; viewIndex < copyPosition + numToCopy; viewIndex++) {
                final int modelIndex = localViewToModelIndex[viewIndex].modelIndex;
                localModelToViewIndex[modelIndex] -= numDeleted;
            }
            copyPosition += numToCopy;
            removeIndex = lastRemoveIndex + 1;
        }

        // Since we have copied SortRow objects over to new positions, the ones left on the end are
        // now duplicate objects of the ones in the rest of the tree.  They must be distinct objects
        // that can have independent model indexes or future sorting will fail horribly.
        // We just set them to null here; they will be recreated with new SortRows when rebuilding
        // the model to view indices if needed.
        for (int i = localLastRowCount - numToRemove; i < localLastRowCount; i++) {
            localViewToModelIndex[i] = null;
        }

        // Update new row count.
        lastRowCount = model.getRowCount();
    }

    /**
     * @return the SortStrategy for this RowSorter.  If none is defined, a {@link net.byteseek.swing.treetable.ColumnSortStrategy} will be created.
     */
    public ColumnSortStrategy getSortStrategy() {
        if (sortStrategy == null) {
            sortStrategy = new net.byteseek.swing.treetable.ColumnSortStrategy();
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
    public List<? extends SortKey> getDefaultSortKeys() {
        return defaultSortKeys;
    }

    /**
     * Sets the default sort keys to use if no other sort is defined.  Ensures will never set to null.
     * @param newDefaults The new default sort keys to use.
     */
    public void setDefaultSortKeys(final List<? extends SortKey> newDefaults) {
        // Set the default sort keys to an unmodifiable list.
        if (newDefaults == null) {
            this.defaultSortKeys = Collections.emptyList();
        } else {
            List<? extends SortKey> noNullElementList = newDefaults.stream().filter(sortKey -> sortKey != null).collect(Collectors.toList());
            this.defaultSortKeys = noNullElementList.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(noNullElementList);
        }

        // If the default sort keys are not empty, and we don't have any sort keys set, set sort keys to the defaults:
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
            final int firstLevel = TreeUtils.getLevel(firstNode);
            final int secondLevel = TreeUtils.getLevel(secondNode);
            if (firstLevel < secondLevel) {
                secondNode = TreeUtils.getAncestor(secondNode, secondLevel - firstLevel);
            } else if (secondLevel < firstLevel) {
                firstNode =  TreeUtils.getAncestor(firstNode, firstLevel - secondLevel);
            }

            // If they now both the same node at the same level, this means we are comparing a node with one of its children, sub-children
            // or vice versa.  The only ancestor node of a child at the same level as the parent is the parent itself.
            // In this situation, if the first node is at the lower level, it's the parent and should sort earlier
            // in the tree, if the other way around, the second level should sort earlier.
            if (firstNode == secondNode) {
                return firstLevel - secondLevel;
            }

            // They are now both at the same level - find the nodes that share a common parent (root will be common to all).
            while (firstNode.getParent() != secondNode.getParent()) {
                firstNode = firstNode.getParent();
                secondNode = secondNode.getParent();
            }
        }

        // Nodes share a common parent - compare values, falling back to the model index order if they're still equal.
        final int comparison = nodeComparator.compare(firstNode, secondNode);
        return comparison == EQUAL_VALUE ? modelRowIndex1 - modelRowIndex2 : comparison;
    }

    /**
     * Builds the sort indexes if there are any sort keys or a grouping comparator to sort with,
     * and otherwise clears them.
     */
    protected void buildSortIndices() {
        if (sortKeys.size() > 0 || model.getGroupingComparator() != null) {
            sort();
        } else {
            clearSortIndexes();
        }
    }

    /**
     * Clears the sort indexes.
     */
    protected void clearSortIndexes() {
        viewToModelIndex = null;
        modelToViewIndex = null;
    }

    /**
     * Creates sort indexes and notifies a sort change.
     */
    protected void sort() {
        final int[] previousViewToModelIndex = buildViewToModelAsInts();
        buildViewToModelIndex();
        buildModelToViewIndex();
        fireRowSorterChanged(previousViewToModelIndex);
    }

    /**
     * Builds an array of SortRows that have model indexes.  These are then sorted using SortRow.compare.
     */
    protected void buildViewToModelIndex() {
        final int newRowCount = model.getRowCount();
        // If we don't have an index, or the index is too small, create a new one.
        if (viewToModelIndex == null || viewToModelIndex.length < newRowCount) {
            viewToModelIndex = new SortRow[newRowCount + EXPAND_SORTROW_SIZE];
        }
        createModelOrderRows(newRowCount);
        Arrays.sort(viewToModelIndex, 0, newRowCount); // The array can be bigger than the row count - only sort the valid rows.
        lastRowCount = newRowCount;
    }

    /**
     * Builds the reverse index, of the model to the view, given a view to model index.
     * Must build the view to model index first.
     */
    protected void buildModelToViewIndex() {
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
    protected int[] buildViewToModelAsInts() {
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
    public boolean isSorting() {
        return viewToModelIndex != null; // invariant assumption: sorting if viewToModel index is not null.
    }

   /**
     * Ensures all the view to model indexes have a SortRow with the model index set to the view index.
     *
     * @param numRows The number of rows to define.
     */
   protected void createModelOrderRows(final int numRows) {
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
     * Ensures the column index is within the bounds of the model.
     * @param column The column index to check.
     * @throws IllegalArgumentException if the column isn't within the bounds of the model.
     */
    protected void checkColumnIndex(int column) {
        if (column < 0 || column >= model.getColumnCount()) {
            throw new IndexOutOfBoundsException("Column " + column + " must be less than " + model.getColumnCount() + " and zero or greater.");
        }
    }

    /**
     * A class carrying a model index, which can be placed into an array to sort using the SortRow.compare method,
     * which delegates the comparison to the TreeTableRowSorter.compare() method.
     */
    protected static class SortRow implements Comparable<SortRow> {

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

        @Override
        public String toString() {
            return getClass().getSimpleName() + '(' + modelIndex + ':' + rowSorter.getModel().getNodeAtModelIndex(modelIndex) + ')';
        }

        public void setModelIndex(final int modelIndex) {
            this.modelIndex = modelIndex;
        }
    }

}
