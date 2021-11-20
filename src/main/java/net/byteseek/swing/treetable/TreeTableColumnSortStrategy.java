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
import java.util.List;
import javax.swing.RowSorter;
import javax.swing.SortOrder;

/**
 * A class that decides what the next set of sort keys will be, given the current set, a request to sort on a column,
 * and a maximum number of columns to sort on.
 * <p>
 * It defaults to adding new sort columns to the end of current sorted columns, replacing the last column if we're
 * at the maximum number of sort columns.  Existing columns maintain their sort position if clicked again, and cycle
 * round ascending/descending/unsorted.  Once a column is unsorted, it is removed from the sorted column keys.
 */
public class TreeTableColumnSortStrategy implements TreeTableRowSorter.ColumnSortStrategy {

    /**
     * The action to take when a sorted column is toggled for sorting.
     * These actions don't affect how the sort is toggled to another sort, only what happens to the column sort position.
     */
    public enum ToggleExistingColumnAction {
        /**
         * When sort is toggled, the existing sorted column is made first in the sorted columns.
         */
        MAKE_FIRST,

        /**
         * When sort is toggled, don't move the sorted column in the sort.
         * This is the default action.
         */
        KEEP_POSITION
    }

    /**
     * The action to take when an unsorted column is toggled for sorting.
     * These actions don't affect how the sort for the column is set, only what happens to the column sort position.
     */
    public enum ToggleNewColumnAction {
        /**
         * When sort is toggled, the new sorted column is made first in the sorted columns.
         */
        MAKE_FIRST,

        /**
         * When sort is toggled, the new sorted column is added to the end of the currently sorted columns,
         * or replaces the last sorted column if we're at the maximum number of sorted columns allowed.
         * This is the default action.
         */
        ADD_TO_END
    }

    /**
     * The action to take when a sorted column becomes unsorted.
     */
    public enum WhenColumnUnsortedAction {
        /**
         * Removes the column from the list of sorted columns. This is the default.
         */
        REMOVE,

        /**
         * Removes the column from the list of sorted columns, and also any subsequent columns defined.
         * If we have 3 sorted columns, and we make the second one unsorted, the second and third column would
         * be removed from the sort.
         */
        REMOVE_SUBSEQUENT,

        /**
         * Removes all sorted columns from the list of sorted columns.
         */
        REMOVE_ALL
    }

    /**
     * The default maximum number of sort keys.
     *
     * In practice, more than 3 levels of sort will produce no real or useful change in any tree sort order.
     * Even 3 levels is mostly unnecessary, unless you are sorting small-domain values first
     * like booleans or other data that has a reduced number of different values.
     */
    protected static final int DEFAULT_MAX_SORT_KEYS = 3;

    /**
     * The maximum number of sort columns allowed.
     */
    protected int maximumSortKeys;

    /**
     * The action to take when an unsorted column is sorted.
     */
    protected ToggleNewColumnAction newColumnAction;

    /**
     * The action to take when a sorted column is sorted.
     */
    protected ToggleExistingColumnAction existingColumnAction;

    /**
     * The action to take when a column becomes unsorted.
     */
    protected WhenColumnUnsortedAction whenUnsortedAction;

    /**
     * Constructs a TreeTableColumnSortStrategy with the default settings.
     * New columns clicked are added to end of existing sort columns, replacing the last sort column if we're at max columns.
     * Existing columns clicked are cycled around ascending/descending/unsorted, and maintain their sort position when clicked again.
     * Once unsorted, the column is removed from the sort keys.
     */
    public TreeTableColumnSortStrategy() {
        this(DEFAULT_MAX_SORT_KEYS, ToggleNewColumnAction.ADD_TO_END, ToggleExistingColumnAction.KEEP_POSITION,
                WhenColumnUnsortedAction.REMOVE);
    }

    /**
     * Constructs a TreeTableColumnSortStrategy with the particular configuration passed in.
     *
     * @param maximumSortKeys The maximum number of sorted columns at any one time.
     * @param newColumnAction What to do if a new column is sorted.
     * @param existingColumnAction What to do if an existing sorted column is toggled again.
     * @param whenUnsortedAction What to do when a column becomes unsorted.
     * @throws IllegalArgumentException if any of the actions are null.
     */
    public TreeTableColumnSortStrategy(final int maximumSortKeys,
                                       final ToggleNewColumnAction newColumnAction,
                                       final ToggleExistingColumnAction existingColumnAction,
                                       final WhenColumnUnsortedAction whenUnsortedAction) {
        if (newColumnAction == null || existingColumnAction == null || whenUnsortedAction == null) {
            throw new IllegalArgumentException("Actions to take cannot be null.");
        }
        this.maximumSortKeys = maximumSortKeys;
        this.newColumnAction = newColumnAction;
        this.existingColumnAction = existingColumnAction;
        this.whenUnsortedAction = whenUnsortedAction;
    }

    @Override
    public List<RowSorter.SortKey> buildNewSortKeys(final int columnToSort, final List<RowSorter.SortKey> sortKeys) {
        final List<RowSorter.SortKey> newKeys = new ArrayList<>(sortKeys);
        final int sortKeyIndex = findKeyColumn(newKeys, columnToSort);
        if (sortKeyIndex < 0) {
            processNewColumn(newKeys, columnToSort);
        } else {
            processExistingColumn(newKeys, sortKeyIndex);
        }
        return newKeys;
    }

    /**
     * @return the action to take on a new column becoming sorted.
     */
    public ToggleNewColumnAction getNewColumnAction() {
        return newColumnAction;
    }

    /**
     * Sets the action to take when a new column is sorted.
     * @param newColumnAction the action to take when a new column is sorted.
     */
    public void setNewColumnAction(ToggleNewColumnAction newColumnAction) {
        this.newColumnAction = newColumnAction;
    }

    /**
     * @return the action to take on a sorted column becoming unsorted.
     */
    public WhenColumnUnsortedAction getWhenUnsortedAction() {
        return whenUnsortedAction;
    }

    /**
     * Sets the action to take when a sorted column becomes unsorted.
     * @param whenUnsortedAction The action to take when a sorted column becomes unsorted.
     */
    public void setWhenUnsortedAction(WhenColumnUnsortedAction whenUnsortedAction) {
        this.whenUnsortedAction = whenUnsortedAction;
    }

    /**
     * @return the action to take on an existing sorted column being toggled again.
     */
    public ToggleExistingColumnAction getExistingColumnAction() {
        return existingColumnAction;
    }

    /**
     * Sets the action to take when an existing sorted column is toggled.
     * @param existingColumnAction the action to take when an existing sorted column is toggled.
     */
    public void setExistingColumnAction(ToggleExistingColumnAction existingColumnAction) {
        this.existingColumnAction = existingColumnAction;
    }

    /**
     * @return The maximum number of sorted columns.
     */
    public int getMaximumSortKeys() {
        return maximumSortKeys;
    }

    /**
     * Sets the maximum number of sorted columns.
     * @param maximumSortKeys the maximum number of sorted columns.
     */
    public void setMaximumSortKeys(final int maximumSortKeys) {
        this.maximumSortKeys = maximumSortKeys;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                "(max columns: " + maximumSortKeys +
                ", existing sort columns: " + existingColumnAction +
                ", new sort columns: " + newColumnAction +
                ", when unsorted: " + whenUnsortedAction + ')';
    }

    /**
     * Takes the appropriate action when a new column is sorted.
     * @param newKeys A list of SortKeys to manipulate.
     * @param columnToSort The index of the newly sorted column.
     */
    protected void processNewColumn(final List<RowSorter.SortKey> newKeys, final int columnToSort) {
        final RowSorter.SortKey newKey = new RowSorter.SortKey(columnToSort, SortOrder.ASCENDING);
        switch(newColumnAction) {
            case ADD_TO_END: { // always add to the end - if there isn't enough room, the last sort key is replaced.
                if (newKeys.size() < maximumSortKeys) {
                    newKeys.add(newKey);
                } else {
                    newKeys.set(newKeys.size() - 1, newKey);
                }
                break;
            }
            case MAKE_FIRST: { // adds to the start, and removes any existing ones past the max number of sorted columns.
                newKeys.add(0, newKey);
                truncateList(newKeys, maximumSortKeys);
                break;
            }
        }
    }

    /**
     * Takes the appropriate action when an existing sorted column is toggled.
     * @param newKeys The list of sort keys to manipulate.
     * @param sortKeyIndex The index of the existing sort key in the list of sort keys.
     */
    protected void processExistingColumn(final List<RowSorter.SortKey> newKeys, final int sortKeyIndex) {
        final RowSorter.SortKey currentKey = newKeys.get(sortKeyIndex);
        final SortOrder nextState = nextOrder(currentKey.getSortOrder());
        if (nextState == SortOrder.UNSORTED) { // process a column becoming unsorted:
            processRemoveSortedColumn(newKeys, sortKeyIndex);
        } else { // Update the sort key:
            final RowSorter.SortKey newKey = new RowSorter.SortKey(currentKey.getColumn(), nextState);
            switch (existingColumnAction) {
                case MAKE_FIRST: {
                    newKeys.remove(sortKeyIndex);
                    newKeys.add(0, newKey);
                    break;
                }
                case KEEP_POSITION: {
                    newKeys.set(sortKeyIndex, newKey);
                    break;
                }
            }
        }
    }

    /**
     * The action to taken when a sorted column becomes unsorted.
     * @param newKeys A list of new keys to manipulate.
     * @param sortKeyIndex The index of the SortKey for the column which is becoming unsorted.
     */
    protected void processRemoveSortedColumn(final List<RowSorter.SortKey> newKeys, final int sortKeyIndex) {
        switch (whenUnsortedAction) {
            case REMOVE: {
                newKeys.remove(sortKeyIndex);
                break;
            }
            case REMOVE_SUBSEQUENT: {
                truncateList(newKeys, sortKeyIndex);
                break;
            }
            case REMOVE_ALL: {
                newKeys.clear();
                break;
            }
        }
    }

    /**
     * @param sortOrder A SortOrder enumeration.
     * @return The next sort order (wrapping around).
     */
    protected SortOrder nextOrder(final SortOrder sortOrder) {
        switch (sortOrder) {
            case UNSORTED:
                return SortOrder.ASCENDING;
            case ASCENDING:
                return SortOrder.DESCENDING;
            case DESCENDING:
                return SortOrder.UNSORTED;
        }
        return SortOrder.UNSORTED; // If there's another sort order we can't handle, we just go to unsorted.
    }

    /**
     * Returns the index of a sort key for a particular column, or -1 if none is defined for that column.
     *
     * @param column The column to get a SortKey index for.
     * @return The index of the SortKey for a particular column, or -1 if none is defined.
     */
    protected final int findKeyColumn(final List<RowSorter.SortKey> sortKeys, final int column) {
        final List<RowSorter.SortKey> localKeys = sortKeys;
        if (localKeys != null) {
            for (int keyIndex = 0; keyIndex < localKeys.size(); keyIndex++) {
                if (localKeys.get(keyIndex).getColumn() == column) {
                    return keyIndex;
                }
            }
        }
        return -1;
    }

    /**
     * Truncates a list down to a new size by removing elements from the end until it's the right size.
     * This isn't particularly efficient, and could be avoided by refactoring the code that uses this to return lists
     * rather than manipulating an existing List in place (then we could just return a subList).
     * But the lists of sort keys are tiny (mostly just 3 elements at max), so we won't bother to do this small optimisation.
     *
     * @param list The list to truncate.
     * @param newSize The new size of the truncated list.
     */
    protected final void truncateList(final List<?> list, final int newSize) {
        while (list.size() > newSize) {
            list.remove(list.size() - 1);
        }
    }
}
