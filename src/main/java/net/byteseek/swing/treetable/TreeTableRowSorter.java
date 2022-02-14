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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.tree.TreeNode;
import static net.byteseek.swing.treetable.TreeNodeComparator.EQUAL_VALUE;

//TODO: examine whether having UNSORTED keys in the sort keys affects overall sorted status?

/**
 * A class which implements the RowSorter interface, and sorts a TreeTableModel, given the sort keys to sort on.
 * It provides an index of the view to model, and the model to view.
 * It can be provided to a JTable bound to a TreeTableModel to sort the rows.
 * <p>
 * It does not implement filtering (although the DefaultRowSorter for a JTable does).
 * This is because filtering out tree nodes can affect other tree nodes (e.g. its children),
 * so filtering at a row level isn't a good match for a tree table.
 * The TreeTableModel implements view filtering using TreeNodes.  This RowSorter ignores filtering entirely.
 */
public class TreeTableRowSorter extends RowSorter<TreeTableModel> {

    /* *****************************************************************************************************************
     *                                                Constants
     */

    protected static final int[] EMPTY_ARRAY = new int[0];

    /**
     * Amount the SortRow array is expanded by over the new row count when it needs to resize to be bigger.
     * Having some headroom in the expansion means if a few more nodes are expanded, we don't have to reallocate
     * the SortRow array too often.
     */
    protected static final int EXPAND_SORTROW_SIZE = 128;

    /**
     * The model being sorted, provided on construction.
     */
    protected final TreeTableModel model;


    /* *****************************************************************************************************************
     *                                       User modifiable variables
     */

    /**
     * The list of current sort keys.  This must not be null, but it can be empty.
     */
    protected List<SortKey> sortKeys = Collections.emptyList();

    /**
     * The list of default sort keys which are used if no other sort specified.
     */
    protected List<? extends SortKey> defaultSortKeys = Collections.emptyList();

    /**
     * A set of column model indexes which are not sortable.
     */
    protected Set<Integer> unsortableColumns = new HashSet<>();

    /**
     * The node comparator to use to compare nodes.
     */
    protected Comparator<TreeNode> nodeComparator;

    /**
     * The sort strategy that builds new sort keys after sort is requested on a column.
     * This lets us change the behaviour when a column is clicked on to sort.  For example,
     * we could make it the primary sort column, or add it to the existing sort columns, or remove other columns.
     */
    protected ColumnSortStrategy sortStrategy;


    /* *****************************************************************************************************************
     *                                        Calculated sort indices
     */

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
     *
     * DefaultRowSorter doesn't need this, because it always re-creates its arrays to be exactly the size of the row count.
     * We hold on to arrays and re-use them as far as we can, so we need to know how many rows were placed into it
     * when it was last built.
     */
    protected int lastRowCount;

    /**
     * rebuildIndices mode forces the RowSorter to rebuild the sort indexes entirely on notification of row update,
     * insert and removals.  The faster algorithms adjust the indexes in place.  This is mostly for debugging purposes.
     */
    protected boolean rebuildIndices;


    /* *****************************************************************************************************************
     *                                                Constructors
     */

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
     *
     * <p><b>Important</b>
     * Default sort keys are not just the starting sort keys to use.
     * If you set default sort keys then sorting will always be enabled, and you cannot enter an unsorted state.
     * If it is fine to be completely unsorted, then pass in an empty list for the default sort keys.
     * To set an initial set of sort keys while having empty default sort keys, call {@link #setSortKeys(List)} after
     * construction.
     *
     * @param model The tree table model to sort.
     * @param defaultSortKeys The default sort if no other sort is defined.
     * @throws IllegalArgumentException if the model passed in is null.
     */
    public TreeTableRowSorter(final TreeTableModel model, final SortKey... defaultSortKeys) {
        this(model, List.of(defaultSortKeys));
    }

    /**
     * Constructs a TreeTableRowSorter given a TreeTableModel, and the list of default sort keys to use if no
     * other sort keys are set.
     *
     * <p><b>Important</b>
     * Default sort keys are not just the starting sort keys to use.
     * If you set default sort keys then sorting will always be enabled, and you cannot enter an unsorted state.
     * If it is fine to be completely unsorted, then pass in an empty list for the default sort keys.
     * To set an initial set of sort keys while having empty default sort keys, call {@link #setSortKeys(List)} after
     * construction.
     *
     * @param model The TreeTableModel to sort.
     * @param defaultSortKeys The default sort if no other sort is defined.  Can be empty or null if not defined.
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


    /* *****************************************************************************************************************
     *                                          RowSorter interface
     *
     */

    @Override
    public TreeTableModel getModel() {
        return model;
    }

    @Override
    public void toggleSortOrder(final int column) {
        if (isSortable(column)) {
            checkValidColumn(column);
            setSortKeys(getSortStrategy().buildNewSortKeys(column, sortKeys));
        }
    }

    @Override
    public int convertRowIndexToModel(final int index) {
        return isSorting() ? viewToModelIndex[index].modelIndex : checkValidIndex(index, model.getRowCount());
    }

    @Override
    public int convertRowIndexToView(final int index) {
        return isSorting() ? modelToViewIndex[index] : checkValidIndex(index, model.getRowCount());
    }

    @Override
    public void setSortKeys(final List<? extends SortKey> keys) {
        final List<? extends SortKey> newKeys = keys == null || keys.isEmpty() ? defaultSortKeys : keys;
        if (!sortKeys.equals(newKeys)) {
            this.sortKeys = Collections.unmodifiableList(getSortableKeys(newKeys));
            /* Note on event ordering:
             * Sort order changed is fired before the sort indices are rebuilt, as is done for DefaultRowSorter.
             * The documentation for RowSorterEvent states that this message is fired first and is typically
             * followed by a SORTED message that indicates the sort order of the contents has actually been modified,
             * which will be sent when the sort indices are rebuilt.
             */
            fireSortOrderChanged();
            buildSortIndices();
        }
    }

    /**
     * @param keys The list of keys, which may contain columns that are not sortable.
     * @return A new list of keys that only has sortable columns in it.
     */
    protected List<? extends SortKey> getSortableKeys(final List<? extends SortKey> keys) {
        final List<SortKey> list = new ArrayList<>();
        for (SortKey sortkey : keys) {
            if (!unsortableColumns.contains(sortkey.getColumn())) {
                list.add(sortkey);
            }
        }
        return list;
    }

    @Override
    public List<? extends SortKey> getSortKeys() {
        return sortKeys;
    }

    @Override
    public int getViewRowCount() {
        return model.getRowCount();
    }

    /**
     * This implementation always returns the same as {@link #getViewRowCount()}.
     * We do not have a separate model row count, as we don't implement filtering in this row sorter.
     * <p>
     * Filtering has been implemented in the TreeTableModel, as filtering tree nodes is a node-
     * concern rather than a row-concern.  A filtered tree node also filters out its children, which are
     * separate rows.  It's cleaner to filter tree nodes in the TreeTableModel; it is behaving as a view over a tree
     * to the table in any case, as nodes are expanded, collapsed, or indeed, filtered.
     * <p>
     * It also simplifies the row sorter logic quite a bit, as we only have a single set of things that need sorting
     * with no filtered gaps appearing in them.  The RowSorter just sorts the rows it is given.
     * <p>
     * {@inheritDoc}
     */
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

    //TODO: what about if model rows updated are not visible?  or not sorting?

    @Override
    public void rowsInserted(final int firstModelIndex, final int endModelIndex) {
        if (rebuildIndices) {
            buildSortIndices();
        } else {
            insertSortIndices(firstModelIndex, endModelIndex);
        }
    }

    @Override
    public void rowsDeleted(final int firstModelIndex, final int endModelIndex) {
        if (rebuildIndices) {
            buildSortIndices();
        } else {
            removeSortIndices(firstModelIndex, endModelIndex);
        }
    }

    @Override
    public void rowsUpdated(final int firstModelIndex, final int endModelIndex) {
        checkValidLastKnownIndices(firstModelIndex, endModelIndex);
        if (rebuildIndices || endModelIndex > firstModelIndex) {
            /*
             * Rebuild if required, or if we update more than one row.
             * If updating more than one row, it gets a little complex to determine the most efficient set of nodes to re-sort.
             * There's more than one workable method, but it's a lot of additional complexity for not much gain.
             * So we will just resort if updating values in more than one row at a time.
             * All code in standard Java Swing and in net.byteseek only ever updates a single row at a time,
             * which happens when a column value is set on a node.
             */
            buildSortIndices();
        } else {
            updateSortIndices(firstModelIndex);
        }
    }

    @Override
    public void rowsUpdated(final int firstModelIndex, final int endModelIndex, final int column) {
        if (TreeUtils.columnInSortKeys(sortKeys, column)) {
            rowsUpdated(firstModelIndex, endModelIndex);
        }
    }


    /* *****************************************************************************************************************
     *                                         Getters and setters
     */

    /**
     * @return true if we are sorting.
     */
    public boolean isSorting() {
        return viewToModelIndex != null; // invariant assumption: sorting if viewToModel index is not null.
    }

    /**
     * Returns true if a column is sortable.
     * The base implementation always returns true.
     * Override this method if you want to control which columns are sortable.
     *
     * @param column The model index of the column to check whether it is sortable.
     * @return Whether the column with the model index provided is sortable.
     */
    public boolean isSortable(final int column) {
        return !unsortableColumns.contains(Integer.valueOf(column));
    }

    /**
     * Sets whether a column is sortable or not sortable.
     * @param column The model index of the column
     * @param isSortable Whether the column is sortable or not.
     */
    public void setSortable(final int column, final boolean isSortable) {
        if (isSortable) {
            unsortableColumns.remove(Integer.valueOf(column));
        } else {
            unsortableColumns.add(Integer.valueOf(column));
        }
    }

    /**
     * Sets all columns to be sortable.
     */
    public void setAllColumnsSortable() {
        unsortableColumns.clear();
    }

    /**
     * @return the node comparator currently being used to compare node values.
     */
    public Comparator<TreeNode> getNodeComparator() {
        return nodeComparator;
    }

    /**
     * Sets the node comparator to use when comparing node values.
     *
     * @param nodeComparator The node comparator to use.
     */
    public void setNodeComparator(final Comparator<TreeNode> nodeComparator) {
        this.nodeComparator = nodeComparator;
    }

    /**
     * @return the SortStrategy for this RowSorter.  If none is defined, a {@link TreeTableColumnSortStrategy} will be created.
     */
    public ColumnSortStrategy getSortStrategy() {
        if (sortStrategy == null) {
            sortStrategy = new TreeTableColumnSortStrategy();
        }
        return sortStrategy;
    }

    /**
     * Sets the TreeTableColumnSortStrategy for this RowSorter.
     * If set to null, it will revert to a default TreeTableColumnSortStrategy.
     * @param sortStrategy The TreeTableColumnSortStrategy to use, or null if the default should be used.
     */
    public void setSortStrategy(final ColumnSortStrategy sortStrategy) {
        if (sortStrategy == null) {
            this.sortStrategy = new TreeTableColumnSortStrategy();
        } else {
            this.sortStrategy = sortStrategy;
        }
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
            List<? extends SortKey> noNullElementList = newDefaults.stream().filter(Objects::nonNull).collect(Collectors.toList());
            this.defaultSortKeys = noNullElementList.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(noNullElementList);
        }

        // If the default sort keys are not empty, and we don't have any sort keys set, set sort keys to the defaults:
        if (!defaultSortKeys.isEmpty() && (sortKeys == null || sortKeys.isEmpty())) {
            setSortKeys(newDefaults);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + model + ')';
    }


    /* *****************************************************************************************************************
     *                           Node comparison and sort index building methods.
     */

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
        if (needToSort()) {
            sort();
        } else {
            clearSortIndices();
        }
    }

    /**
     * @return true if we have sort keys or a grouping comparator set.
     */
    protected boolean needToSort() {
        return sortKeys.size() > 0 || model.getGroupingComparator() != null;
    }

    /**
     * Clears the sort indices, by nulling them.
     * Also fires a rowSorter change message if we were sorting before clearing the indices.
     */
    protected void clearSortIndices() {
        final boolean wasSorting = isSorting();
        final int[] previousViewToModelIndex = wasSorting? buildViewToModelAsInts() : null;
        viewToModelIndex = null;
        modelToViewIndex = null;
        if (wasSorting) {
            fireRowSorterChanged(previousViewToModelIndex);
        }
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
     * Builds an array of SortRows that have model indexes in ascending order.
     * These are then sorted using SortRow.compare, which in turn invokes the compare() method on this RowSorter.
     */
    protected void buildViewToModelIndex() {
        SortRow[] localViewToModelIndex = viewToModelIndex;
        final int newRowCount = model.getRowCount();

        // If we don't have an index, or the index is too small, create a new one.
        if (localViewToModelIndex == null || localViewToModelIndex.length < newRowCount) {
            viewToModelIndex = localViewToModelIndex = new SortRow[newRowCount + EXPAND_SORTROW_SIZE];
        }
        createModelOrderRows(newRowCount);
        Arrays.sort(localViewToModelIndex, 0, newRowCount); // The array can be bigger than the row count - only sort the valid rows.
        lastRowCount = newRowCount;
    }

    /**
     * Builds the reverse index, of the model to the view, given a view to model index.
     * Ensures the index array is sized the same as the view to model index.
     * Must build the view to model index first.
     */
    protected void buildModelToViewIndex() {
        final SortRow[] localViewToModelIndex = viewToModelIndex;
        int[] localModelToViewIndex = modelToViewIndex;

        // if we don't have an index, or the existing array is less than the size of our view to model index, create one:
        if (localModelToViewIndex == null || localModelToViewIndex.length < localViewToModelIndex.length) {
            modelToViewIndex = localModelToViewIndex = new int[localViewToModelIndex.length];
        }
        final int numRows = model.getRowCount(); // The number of rows may not be the same as the size of the array (which can be bigger).
        for (int viewIndex = 0; viewIndex < numRows; viewIndex++) {
            localModelToViewIndex[localViewToModelIndex[viewIndex].modelIndex] = viewIndex;
        }
    }

    /**
     * Builds a copy of the view to model index as an integer array.
     * This is needed by JTable to allow selection (other things?) to work properly when the model and sort changes.
     * <p>
     * JTable assumes that the length of the array is the number of rows, so we must give it an array sized exactly
     * to the number of rows in the old view to model index.
     *
     * @return An integer array containing the viewToModel index, or an empty array if there is no index.
     */
    protected int[] buildViewToModelAsInts() {
        final SortRow[] localViewToModel = viewToModelIndex;
        if (localViewToModel != null) {
            final int numRows = lastRowCount;
            final int[] result = new int[numRows]; // must be the length before any changes made to view model index.
            for (int index = 0; index < numRows; index++) {
                result[index] = localViewToModel[index].modelIndex;
            }
            return result;
        }
        return EMPTY_ARRAY;
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


   /* *****************************************************************************************************************
    *                                       Index patching methods
    *
    * These methods take the existing sort indices and patch them with new items inserted, updated or removed.
    * This is more efficient than re-sorting the entire tree on every change.
    *
    * In practical terms, there isn't much difference in perceived speed until over 100,000 visible rows,
    * assuming the TreeNode sort comparison operators are not horribly inefficient.
    * Re-sorting on each change when there are 500,000 visible nodes starts to become un-usable,
    * while patching remains fast.
    *
    * Most trees will not be this big, so it's arguable this won't make much difference for most applications.
    * Nevertheless, in most cases it still takes less processing power and generates less garbage to patch the
    * existing indexes, and we can support extremely large trees if required.
    */

    //TODO: profile to see what the difference between removing rows and full sort index rebuild.
    //      jmh for removing nodes indicates it is about the same performance as rebuilding the sort index
    //      up to about 100,000 nodes.  After that it is faster.  At around 500,000 nodes, the sort
    //      performance falls off a cliff, while the patching methods slow down as it gets bigger, but not very much.
    //

    //TODO: do we need this once we are satisfied through testing and profiling that dynamic updates are better than full rebuilds?
    public boolean getRebuildIndices() {
        return rebuildIndices;
    }
    public void setRebuildIndices(final boolean rebuildIndices) {
        this.rebuildIndices = rebuildIndices;
    }

    /**
     * If sorting, updates an index or rebuilds it entirely if rebuild conditions are met,
     * and notifies listeners of any change, passing in a copy of the old index as required by the event.
     *
     * @param firstModelIndex The first model index changing.
     * @param endModelIndex The end model index changing.
     */
    protected void insertSortIndices(final int firstModelIndex, final int endModelIndex) {
        checkValidLastKnownIndices(firstModelIndex, endModelIndex);
        if (isSorting()) {
            //TODO: turns out, you don't have to supply this.  If you do, selection and editing are preserved.  If you don't, they aren't.
            //      could make it configurable behaviour - if you want more efficient (not creating a new copy of the entire index on every update),
            //      then just provide a null old view to model and it will still work.
            //final int[] oldViewToModel = buildViewToModelAsInts();
            //insertSortedRowsToIndices(firstModelIndex, endModelIndex);
            //fireRowSorterChanged(oldViewToModel);

            final int[] selectedIndices = getSelectedRows();
            try {
                insertSortedRowsToIndices(firstModelIndex, endModelIndex);
                fireRowSorterChanged(null); // no prior index supplied.
            } finally {
                restoreSelectedRows(selectedIndices);
            }
        }
    }

    protected int[] getSelectedRows() {
        final ListSelectionModel selectModel = model.getSelectionModel();
        if (selectModel != null) {
            final int[] selectedIndices = model.getSelectedRowModelIndexes();
            selectModel.setValueIsAdjusting(true);
            selectModel.clearSelection();
            return selectedIndices;
        }
        return EMPTY_ARRAY;
    }

    protected void restoreSelectedRows(final int[] selectedIndices) {
        final int[] localIndex = modelToViewIndex;
        final ListSelectionModel selectModel = model.getSelectionModel();
        for (int i = 0; i < selectedIndices.length; i++) {
            final int selectedViewIndex = localIndex[selectedIndices[i]];
            selectModel.addSelectionInterval(selectedViewIndex, selectedViewIndex);
        }
        selectModel.setValueIsAdjusting(false);
    }


    protected void removeSortIndices(final int firstModelIndex, final int endModelIndex) {
        checkValidLastKnownIndices(firstModelIndex, endModelIndex);
        if (isSorting()) {
            final int[] oldViewToModel = buildViewToModelAsInts();
            removeSortedRowsFromIndices(firstModelIndex, endModelIndex);
            fireRowSorterChanged(oldViewToModel);
        }
    }

    //TODO: check indices here?
    protected void updateSortIndices(final int modelIndex) {
        if (isSorting()) {
            final int[] oldViewToModel = buildViewToModelAsInts();
            updateSiblings(modelIndex);
            fireRowSorterChanged(oldViewToModel);
        }
    }


    /**
     * Patches the existing index if a single node is updated.
     * Compares the updated node to its siblings in order to identify the new insertion point.
     * First scans forwards to see if the updated node is bigger than the siblings after it.
     * If not, scans backwards to see if the updated node is smaller than the siblings after it.
     * If there's a new position, it patches the view and model indices to move the udpated node
     * and all its children and sub-children.
     *
     * @param modelIndex The model index of the node that is updating.
     */
    protected void updateSiblings(final int modelIndex) {
        final TreeTableModel localModel = model; // avoid repeated getField - use a local reference.
        final TreeNode updatingNode = localModel.getNodeAtModelIndex(modelIndex);
        if (localModel.isVisible(updatingNode)) {
            final int updatingNodeViewIndex = modelToViewIndex[modelIndex];

            int newViewIndex = findNextInsertionViewIndex(updatingNode, updatingNodeViewIndex, modelIndex);
            if (newViewIndex == updatingNodeViewIndex) {
                newViewIndex = findPreviousInsertionViewIndex(updatingNode, updatingNodeViewIndex, modelIndex);
            }

            if (newViewIndex != updatingNodeViewIndex) {
                moveNodeToSiblingPosition(updatingNode, updatingNodeViewIndex, newViewIndex);
            }
        }
    }

    /**
     * Scan forward from updated node to see if it belongs after those nodes - if siblings are smaller than the updated node.
     * Forward scan is faster than looking behind, as we can skip all sub-child nodes since we know
     * how many children and sub-children they have.
     *
     * @param updatingNode the node which is updating
     * @param updatedNodeViewIndex the view index of the updating node
     * @param updatingNodeModelIndex the model index of the updating node.
     */
    protected int findNextInsertionViewIndex(TreeNode updatingNode, int updatedNodeViewIndex, int updatingNodeModelIndex) {
        final TreeTableModel localModel = model; // avoid repeated getField - use a local reference.
        final int numRows = lastRowCount; // avoid repeated getField - use a local reference.
        final SortRow[] localViewToModelIndex = viewToModelIndex; // avoid repeated getField - use a local reference.
        final Comparator<TreeNode> localComparator = nodeComparator; // avoid repeated getField - use a local reference.
        final TreeNode parent = updatingNode.getParent();

        /*
         * Loop while we find siblings that are smaller than our updated node.
         * We don't attempt to calculate how many siblings there might be, we can detect non siblings and stop.
         */
        TreeNode currentNode = updatingNode;
        int lastGoodViewIndex = updatedNodeViewIndex;
        int nodeCompare = -1;
        while (nodeCompare < 0) {
            final int nextSiblingIndex = lastGoodViewIndex + 1 + localModel.getLastKnownSubTreeCount(currentNode);

            // Stop if the next sibling would be past the number of available rows:
            if (nextSiblingIndex >= numRows) {
                break;
            }

            final int siblingModelIndex = localViewToModelIndex[nextSiblingIndex].modelIndex;
            final TreeNode siblingNode = localModel.getNodeAtModelIndex(siblingModelIndex);

            // Stop if the next "sibling" node doesn't have the same parent (not a sibling)
            if (siblingNode.getParent() != parent) {
                break;
            }

            nodeCompare = localComparator.compare(siblingNode, updatingNode);
            if (nodeCompare == 0) { // If they are equal, compare on their model indexes to provide stable sort.
                nodeCompare = siblingModelIndex - updatingNodeModelIndex;
            }

            // Stop if the next sibling is equal or bigger than our updated node.
            if (nodeCompare >= 0) {
                break;
            }

            // Set the current view index and node to be the node that is still smaller than our updating node:
            lastGoodViewIndex = nextSiblingIndex;
            currentNode = siblingNode;
        }
        return lastGoodViewIndex;
    }

    /*
     * If we didn't find a new sibling smaller than our current node, scan backwards.
     * To scan backwards, we have to examine all the nodes above the updated node
     * until we find the parent node.  Only nodes that share the same parent are siblings.
     */
    protected int findPreviousInsertionViewIndex(TreeNode updatingNode, int updatingNodeViewIndex, int updatingNodeModelIndex) {
        final TreeTableModel localModel = model; // avoid repeated getField - use a local reference.
        final SortRow[] localViewToModelIndex = viewToModelIndex; // avoid repeated getField - use a local reference.
        final Comparator<TreeNode> localComparator = nodeComparator; // avoid repeated getField - use a local reference.

        /*
         * Loop while we find siblings that are bigger than our updated node.
         * We don't attempt to calculate how many siblings there might be, we can detect non siblings and stop.
         */
        final TreeNode parent = updatingNode.getParent();
        int lastGoodViewIndex = updatingNodeViewIndex;
        int nodeCompare = 1;
        while (nodeCompare > 0) {
            // We have to just step back one, as we don't know what the next sibling will be.
            // The previous node could be a sibling, the original parent, or it could be part of a subtree of a sibling higher up.
            final int previousSiblingIndex = lastGoodViewIndex - 1;

            // Stop if we're past the start:
            if (previousSiblingIndex < 0) {
                break;
            }

            // Get the sibling node and model index:
            final int siblingModelIndex = localViewToModelIndex[previousSiblingIndex].modelIndex;
            final TreeNode siblingNode = localModel.getNodeAtModelIndex(siblingModelIndex);

            // If we found the original parent, then there are no more siblings - stop:
            if (siblingNode == parent) {
                break;
            }

            // If it shares the same parent as our original node, it's a sibling,
            // otherwise it's part of a subtree of the previous sibling (so we ignore it)
            if (siblingNode.getParent() == parent) {
                nodeCompare = localComparator.compare(siblingNode, updatingNode);
                if (nodeCompare == 0) {
                    nodeCompare = siblingModelIndex - updatingNodeModelIndex; //TODO: check comparison correct here.
                }
                if (nodeCompare < 1) {
                    break; // Found a sibling equal to or smaller than the updating node.
                }
            }

            lastGoodViewIndex = previousSiblingIndex;
        }
        return lastGoodViewIndex;
    }

    protected void moveNodeToSiblingPosition(final TreeNode node, final int nodeViewIndex, final int siblingViewIndex) {
        // Avoid repeated getField - use a local reference.
        final SortRow[] localViewToModelIndex = viewToModelIndex;

        // Take a copy of the node to move and its children view index entries.
        final int numNodes = 1 + model.getLastKnownSubTreeCount(node);
        final SortRow[] nodeRows = new SortRow[numNodes];
        System.arraycopy(localViewToModelIndex, nodeViewIndex, nodeRows, 0, numNodes);

        // Calculate positions of move depending on whether it's moving up or down in the view:
        final int blockFrom; final int blockTo; final int blockNumToMove; final int moveToPos; final int lastViewIndex;
        if (siblingViewIndex < nodeViewIndex) {    // Node is moving up in the view index - shift the other nodes down.
            blockFrom      = siblingViewIndex;                  // start at the position of the nodes to move to
            blockTo        = siblingViewIndex + numNodes;       // move them down by the number of nodes we're moving into its current position.
            blockNumToMove = nodeViewIndex - siblingViewIndex;  // number of nodes to move is the difference between their start points.
            moveToPos      = siblingViewIndex;                  // The moving node moves to the start of the sibling to move to.
            lastViewIndex  = nodeViewIndex + numNodes - 1;
        } else {                                    // Node is moving down in the view index - shift the other nodes up.
            blockFrom      = nodeViewIndex + numNodes;          // start just after the end of the moving node.
            blockTo        = nodeViewIndex;                     // move up to where the moving node starts.
            blockNumToMove = getViewIndexOfLastChild(siblingViewIndex) - blockFrom; // number of nodes is difference between block start and last child of node moving up.
            moveToPos      = nodeViewIndex + blockNumToMove;    // The moving node moves to after the nodes moving up.
            lastViewIndex  = blockFrom + blockNumToMove - 1;
        }

        // Move the other nodes up or down to make room for the moving node.
        System.arraycopy(localViewToModelIndex, blockFrom, localViewToModelIndex, blockTo, blockNumToMove);

        // Copy over the updated rows into their new position.
        System.arraycopy(nodeRows, 0, localViewToModelIndex, moveToPos, numNodes);

        // Fix up the modelToViewIndex, from the smallest view index affected to the last view index affected.
        final int[] localModelToViewIndex = modelToViewIndex; // Avoid repeated getField - use a local reference.
        final int viewIndexStart = Math.min(nodeViewIndex, siblingViewIndex);
        for (int viewIndex = viewIndexStart; viewIndex <= lastViewIndex; viewIndex++) {
            localModelToViewIndex[localViewToModelIndex[viewIndex].modelIndex] = viewIndex;
        }
    }

    protected int getViewIndexOfLastChild(final int viewIndex) {
        final TreeNode node = model.getNodeAtModelIndex(viewToModelIndex[viewIndex].modelIndex);
        return viewIndex + model.getLastKnownSubTreeCount(node);
    }

    /**
     * Returns the index of a child in a parent, removing any filtered nodes if filtering is active, or -1 if not found.
     *
     * @param parent The parent node to scan.
     * @param childToFind The child node to find.
     * @return the index of a child in a parent, removing any filtered nodes if filtering is active, or -1 if not found.
     */
    protected int getFilteredChildIndex(final TreeNode parent, final TreeNode childToFind) {
        final TreeTableModel localModel = model; // avoid repeated getField - use a local reference.
        if (localModel.isFiltering()) {
            if (!localModel.isFiltered(childToFind)) {
                int filteredChildIndex = -1;
                for (int i = 0; i < parent.getChildCount(); i++) {
                    final TreeNode child = parent.getChildAt(i);
                    if (!localModel.isFiltered(child)) {
                        filteredChildIndex++;
                    }
                    if (child == childToFind) {
                        return filteredChildIndex;
                    }
                }
            }
            return -1;
        }
        return parent.getIndex(childToFind);
    }

    /**
     * Patches the existing index to accommodate new rows inserted between first and end index inclusive.
     *
     * @param firstModelIndex The model index of first row inserted.
     * @param endModelIndex The model index of the end row inserted.
     */
    protected void insertSortedRowsToIndices(final int firstModelIndex, final int endModelIndex) {
        // Use local references to avoid repeated field access:
        final int[] localModelToViewIndex = modelToViewIndex;
        final SortRow[] localViewToModelIndex = viewToModelIndex;

        // Ensure we have enough space in the indices for the insertions:
        final int numToAdd = endModelIndex - firstModelIndex + 1;
        ensureIndicesHaveSpaceForIncrease(numToAdd);

        /*
         * Adjust all ViewToModel entries from the insertion point to the end, to add the number of inserted nodes to their model index.
         * The model indexes are all increased by the number of rows being inserted to the model, so they continue
         * to point at the same row (but at a higher model index given the insertions).
         */
        int localLastRowCount = lastRowCount;
        for (int modelIndex = firstModelIndex; modelIndex < localLastRowCount; modelIndex++) {
            final int viewIndex = localModelToViewIndex[modelIndex];
            localViewToModelIndex[viewIndex].modelIndex += numToAdd;
        }

        /*
         * Make space in the ModelToViewIndex for the new model rows, by moving all the rows after it up.
         * The new row view indexes will be set later when we determine the sorted insertion point (and thus view index) of each new row.
         */
        int numToMove = localLastRowCount - endModelIndex;
        System.arraycopy(localModelToViewIndex, firstModelIndex, localModelToViewIndex, endModelIndex + 1, numToMove);

        /*
         * Build a sorted list of the new rows to add.
         * Since we have to sort the inserted rows in this method, this might not be faster than just rebuilding if
         * we're inserting a substantial proportion of the total rows.
         */
        final List<SortRow> newRows = new ArrayList<>(numToAdd);
        for (int modelIndex = firstModelIndex; modelIndex <= endModelIndex; modelIndex++) {
            newRows.add(new SortRow(this, modelIndex));
        }
        Collections.sort(newRows);

        /*
         * Insert new rows in the ViewToModelIndex in the correct sorted position (using binarySearch),
         * set the new view index in the ModelToViewIndex for each of the new rows,
         * and adjust the ModelToViewIndex entries for view entries that move up in the view.
         *
         * Assumes there will be at least one inserted row.
         */
        int numInsertedSoFar = 0;
        int insertedRowNum = 0;
        final SortRow firstRow = newRows.get(insertedRowNum);
        int insertionPoint = -1 - Arrays.binarySearch(localViewToModelIndex, 0, localLastRowCount, firstRow);
        checkInsertionPoint(firstRow.modelIndex, insertionPoint);
        while (insertedRowNum < numToAdd) {

            /*
             * Locate the next block of insertions to make.
             *
             * Find if the next rows to insert go to the same position in the tree.
             * If they do, they are consecutive in the view.
             * If they don't, we've found the next insert position for the next set of inserts.
             * If we don't find a next insertion point, that will be because there are no further
             * rows to process (either none left, or all remaining rows sort to the same position).
             */
            int nextInsertionPoint = -1;
            final int originalRowNum = insertedRowNum;
            for (insertedRowNum = insertedRowNum + 1; insertedRowNum < numToAdd; insertedRowNum++) {

                /*
                 * Find the insertion point for the next row in our sorted list of rows.
                 */
                final SortRow nextRow = newRows.get(insertedRowNum);
                final int findNextInsertionPoint = -1 - Arrays.binarySearch(localViewToModelIndex, 0, localLastRowCount, nextRow);
                checkInsertionPoint(nextRow.modelIndex, findNextInsertionPoint);

                /*
                 * If the next insertion point isn't equal to our current position in the ViewToIndexModel,
                 * they will not be consecutive in the updated view.  An equal insertion point says they belong
                 * in exactly the same area of the tree when inserted, so we can merge those inserts.
                 */
                if (findNextInsertionPoint != insertionPoint) {
                    nextInsertionPoint = findNextInsertionPoint; // remember our next insertion point for the next loop around.
                    break; // cease looking for further equal insertion points.
                }
            }

            /*
             * Calculate how many rows will be inserted consecutively in one block.
             * We now have the insertion point to begin inserting rows, the order of the rows and how many to move in one block.
             */
            final int numRowsToInsert = insertedRowNum - originalRowNum;

            // Move the ViewToModelIndex entries from the insertion point up to create a space for the new rows.
            numToMove = localLastRowCount - insertionPoint + numRowsToInsert + 1; //TODO: check this.
            System.arraycopy(localViewToModelIndex, insertionPoint, localViewToModelIndex, insertionPoint + numRowsToInsert, numToMove);

            // Insert the new rows in the gap in the ViewToModelIndex, and update the ModelToViewIndex entries for them.
            for (int rowToInsert = 0; rowToInsert < numRowsToInsert; rowToInsert++) {
                final int insertPoint = insertionPoint + rowToInsert;
                final SortRow insertRow = newRows.get(originalRowNum + rowToInsert);
                localViewToModelIndex[insertPoint] = insertRow;
                localModelToViewIndex[insertRow.modelIndex] = insertPoint;
            }

            /*
             * We have now inserted the new rows in both indices.
             * Increase the row count, so it's correct for any subsequent processing.
             */
            localLastRowCount += numRowsToInsert;

            /*
             * Fix up all the view indexes that just moved up by the number inserted in the ModelToViewIndex.
             * We only have to update the ones up to the next block of inserts, as they will be updated anyway when
             * their turn comes around with the cumulative number of inserts made at that point.
             * If there are no further inserts, then all the entries up to the end will be updated.
             */
            final int nextInsertBlock = nextInsertionPoint < 0 ? localLastRowCount : nextInsertionPoint;
            numInsertedSoFar += numRowsToInsert;
            for (int viewIndex = insertionPoint + numRowsToInsert; viewIndex < nextInsertBlock; viewIndex++) {
                final int modelIndex = localViewToModelIndex[viewIndex].modelIndex;
                localModelToViewIndex[modelIndex] += numInsertedSoFar;
            }

            // Move on to the next non-consecutive insertion point:
            insertionPoint = nextInsertionPoint;
        }

        // Set the current row count to our new count of rows.
        lastRowCount = localLastRowCount;
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
    protected void removeSortedRowsFromIndices(final int firstModelIndex, final int endModelIndex) {
        // Initialize useful constants:
        final int[] localModelToViewIndex = modelToViewIndex; // avoid repeated field access, use a local ref.
        final SortRow[] localViewToModelIndex = viewToModelIndex; // avoid repeated field access, use a local ref.
        final int localLastRowCount = lastRowCount;
        final int numToRemove = endModelIndex - firstModelIndex + 1;

        /*
          * 0. Optimisation:
          *
          * Before we alter the indices, build a sorted list of view indexes affected by removing rows from the model.
          * This will let us identify consecutive blocks of nodes in the view, so we can remove them in a single operation later.
         */
        final int[] removedViewRows = new int[numToRemove];
        if (numToRemove > 0) {
            System.arraycopy(localModelToViewIndex, firstModelIndex, removedViewRows, 0, numToRemove);
        }
        Arrays.sort(removedViewRows);

        /*
         * 1. Fix up all the Model indexes of the ViewToModelIndex AFTER the removed rows:
         *
         * Since a contiguous block of rows from first to end model index has been removed from the model,
         * reduce the Model indexes of all the View entries AFTER the removed rows,
         * by the number of rows being deleted from the model.
         * They will now point back to their original rows in the model before the rows were deleted from it.
         */
        for (int modelIndex = endModelIndex + 1; modelIndex < localLastRowCount; modelIndex++) {
            final int viewIndex = localModelToViewIndex[modelIndex];
            localViewToModelIndex[viewIndex].modelIndex -= numToRemove;
        }

        /*
         * 2. Remove ModelToViewIndex entries for the removed rows.
         *
         * We don't need the ModelToViewIndex entries for the removed rows in the model, as they're no longer in the
         * model.  Copy the remaining entries back over them to cover the gap.  We don't have to clean up any copies
         * left on the end, as they will be set correctly if new rows are subsequently added.
         *
         */
        final int positionAfterDeletions = endModelIndex + 1; //TODO: test - what if all rows up to end are deleted?  will we get an ArrayIndexOutOfBoundsException?
        final int remainingToCopyBack = localLastRowCount - positionAfterDeletions;
        System.arraycopy(localModelToViewIndex, positionAfterDeletions, localModelToViewIndex, firstModelIndex, remainingToCopyBack);

        /*
         * 3. Remove entries in the ViewToModelIndex and fix up the View indexes of the ModelToViewIndex
         *
         * We do both of these in step, using the sorted list of view indexes we built in step 0  to let us identify
         * contiguous blocks of view rows to remove in each step.  Since the list is sorted in order of view index,
         * all changes later will not affect anything earlier in the view, so we alter both the indexes in "view" order of change:
        */
        int startRemoveIndex = 0;
        int viewRowDeleteStart = removedViewRows.length > 0 ? removedViewRows[0] : -1; //TODO: why set -1 as copy position if there are NO removed view rows?
        while (startRemoveIndex < removedViewRows.length) {

            /*
             * 3a. Calculate how many rows we can delete in this step.
             *
             * Get the index of the last row to delete which is in a consecutive block of view indexes from our current position.
             * If rows aren't consecutive, then we will just have the original index.
             */
            final int endRemoveIndex = TreeUtils.findLastConsecutiveIndex(startRemoveIndex, removedViewRows);
            final int viewRowDeleteEnd = removedViewRows[endRemoveIndex];
            final int numDeleted = viewRowDeleteEnd - viewRowDeleteStart + 1;

            /*
             *3b. Calculate the minimal number of rows to move to cover the gap.
             *
             * Calculate how many rows exist between the end of this block of rows to delete, and the next block
             * of rows to delete (or the end of the rows, if that comes first).  This lets us just copy the
             * minimal number of rows back over to cover the gap in this step, rather than copying all the remaining rows.
             */
            //TODO: bug? should we use localLastRowCount here, or localLastRowCount - 1?  probably correct in fact (there is no further block), but
            //     what are implications of nextBlockStart being beyond the end?  change to "last row to move" to be clearer in intent?
            final int nextBlockStart = endRemoveIndex + 1 < removedViewRows.length ? removedViewRows[endRemoveIndex + 1] : localLastRowCount;
            final int numToMove = nextBlockStart - viewRowDeleteEnd - 1;

            /*
             * 3c. Remove deleted view rows from the ViewToModelIndex.
             *
             * Moves a block of view to model rows upwards in the array to cover the deleted rows.
             */
            System.arraycopy(localViewToModelIndex, viewRowDeleteEnd + 1, localViewToModelIndex, viewRowDeleteStart, numToMove);

            /*
             * 3d. Update the View index of the ModelToViewIndex for each of the view rows which just moved above:
             */
            for (int viewIndex = viewRowDeleteStart; viewIndex < viewRowDeleteStart + numToMove; viewIndex++) {
                final int modelIndex = localViewToModelIndex[viewIndex].modelIndex;
                localModelToViewIndex[modelIndex] -= numDeleted;
            }

            // Update loop variables to move to next block of view rows to remove (if any):
            viewRowDeleteStart += numToMove;
            startRemoveIndex = endRemoveIndex + 1;
        }

        /*
          * Since we have copied SortRow objects over to new positions, the ones left on the end are
          * now duplicate objects of the ones in the rest of the index.  They must be distinct objects
          * that can have independent model indexes or future sorting will fail horribly.
          * We just set them to null here; they will be recreated with new SortRows when rebuilding
          * the model to view indices if the number of rows increases again.
          */
        for (int i = localLastRowCount - numToRemove; i < localLastRowCount; i++) {
            localViewToModelIndex[i] = null;
        }

        // Update new row count. //TODO: should we reset to model, or should we adjust by number of rows deleted?
        // our own calculations *SHOULD* be accurate.  If they're not, it's a bug which will manifest in other
        // ways too.
        lastRowCount = model.getRowCount();
    }

    /**
     * Checks that the indices have sufficient space for
     * @param increase - the number of new rows to add to the indices.
     */
    protected void ensureIndicesHaveSpaceForIncrease(final int increase) {
        if (lastRowCount + increase > viewToModelIndex.length) {
            final int newSize = lastRowCount + increase + EXPAND_SORTROW_SIZE;

             // Increase ModelToViewIndex size and copy over old elements:
            final int[] newModelToViewIndex = new int[newSize];
            System.arraycopy(modelToViewIndex, 0, newModelToViewIndex, 0, lastRowCount);
            modelToViewIndex = newModelToViewIndex;

            // Increase ViewToModelIndex size and copy over old elements:
            final SortRow[] newViewToModelIndex = new SortRow[newSize];
            System.arraycopy(viewToModelIndex, 0, newViewToModelIndex, 0, lastRowCount);
            viewToModelIndex = newViewToModelIndex;
        }
    }

    /* *****************************************************************************************************************
     *                       Index checking methods, throwing IndexOutOfBounds exceptions.
     */

    /**
     * Throws an IndexOutOfBoundsException if the column is not valid.
     * @param column The column index to check.
     */
    protected void checkValidColumn(final int column) {
        if (column < 0 || column >= model.getColumnCount()) {
            throw new IndexOutOfBoundsException(column);
        }
    }

    /**
     * Checks that the indices are within the last known model range and that the first index is not bigger
     * than the end index, otherwise throws an IndexOutOfBoundsException.  This is checked against the last
     * known state of the model (not the current model row count), as the model may have changed when this
     * check is performed.
     *
     * @param firstModelIndex the first model index to check
     * @param endModelIndex the second model index to check.
     * @throws IndexOutOfBoundsException if either index is invalid or the first is bigger than the end.
     */
    protected void checkValidLastKnownIndices(final int firstModelIndex, final int endModelIndex) {
        /*
         * We check that the index is valid within the bounds of the last known model row count, not the
         * current model count.  When we call this, the number of actual model rows may have
         * already been changed due to rows removed or inserted.
         */
        checkValidIndex(firstModelIndex, lastRowCount);
        checkValidIndex(endModelIndex, lastRowCount);
        if (firstModelIndex > endModelIndex) {
            throw new IndexOutOfBoundsException(firstModelIndex);
        }
    }

    /**
     * Checks that an index is valid - it's not negative or past the number of rows .
     * If valid, it just returns the index passed in, if not it throws an IndexOutOfBoundsException.
     *
     * @param index The index to check.
     * @return the index passed in.
     * @throws IndexOutOfBoundsException if the index is not valid.
     */
    protected int checkValidIndex(final int index, final int count) {
        if (index < 0 || index >= count) {
            throw new IndexOutOfBoundsException(index);
        }
        return index;
    }

    /**
     * The new SortRow with the inserted model index should not appear in the ViewToModelIndex at all,
     * since we have adjusted all model indexes in the ViewToModelIndex that used to point to those indexes upwards.
     * Put in a sanity check here that we never find a new row in the existing index.
     * @param insertRowModelIndex the row in the model index being inserted.
     * @param insertionPoint the point in the view where the new row is being inserted.
     */
    protected void checkInsertionPoint(final int insertRowModelIndex, final int insertionPoint) {
        if (insertionPoint < 0) { // This should never happen - it means we already found the new thing we want to insert.
            throw new RuntimeException("BUG in TreeTableRowSorter: a new SortRow with model index: " + insertRowModelIndex +
                    " was found to already exist in the ViewToModelIndex at position " + -(insertionPoint + 1));
        }
    }


    /* *****************************************************************************************************************
     *                                   Supporting classes and interfaces
     */

    /**
     * A class carrying a model index, which can be placed into an array to sort using the SortRow.compare method,
     * which delegates the comparison to the TreeTableRowSorter.compare() method.
     */
    public static class SortRow implements Comparable<SortRow> {

        protected final TreeTableRowSorter rowSorter;
        protected int modelIndex;

        /**
         * Constructs a SortRow given a TreeTableRowSorter and the model index of the item.
         *
         * @param sorter The TreeTableRowSorter to use to sort the rows.
         * @param modelIndex The model index of this sort row.
         */
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

        /**
         * Sets the model index of the sort row.  Allows updating existing objects rather than constantly having
         * to regenerate them, when refreshing the sort.
         *
         * @param modelIndex The new model index for this sort row.
         */
        public void setModelIndex(final int modelIndex) {
            this.modelIndex = modelIndex;
        }
    }

    /**
     * A strategy interface to build a list of new sort keys, passing in the current set of sort keys and the column to sort.
     */
    public interface ColumnSortStrategy {
        /**
         * Builds a list of sort keys given a column to sort and the existing list of sort keys.
         *
         * @param columnToSort The column which should be sorted.
         * @param sortKeys The current state of sorting.
         */
        List<RowSorter.SortKey> buildNewSortKeys(int columnToSort, List<RowSorter.SortKey> sortKeys);
    }

}
