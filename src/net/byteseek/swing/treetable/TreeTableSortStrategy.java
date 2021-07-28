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
import java.util.ArrayList;
import java.util.List;

//TODO: sort strategy of remove subsequent combined with default sort column means you can only ever sort on the primary column.
//      how to move to a new sort column when you're removing subsequent?  Getting to unsorted allows you to pick a new primary.
//      If you can't get to unsorted, you can't change the primary sort column.
//      You could if you only removed the column moving to unsorted, after picking the new column as a secondary sort.
//      You also can if you make each column clicked the primary sort column (but this makes multi column sorting kind of
//      pointless, because sorting secondarily on your prior primary isn't making a lot of sense to me in terms of
//      a strategy to explore the data set.
//      COULD introduce a Ctrl-click to set primary sort column.  Awkward, most people will never discover it.

//TODO: re-investigate sort insert / delete rows optimisations after we've done tree model insert/delete notifications on the TreeTableModel.

/**
 * A configurable class that decides what the next set of sort keys will be, given the current set and a request to sort on a column.
 * It defaults to adding new sort columns to the end of current sorted columns, and removing the column and all subsequent sort columns
 * if a column becomes unsorted.
 * <p>
 * This sort strategy allows a variety of configurable behaviour. For new columns, we can choose to:
 * <ul>
 *     <li>Make the new column the first sort column</li
 *     <li>Make the new column the first sort column, as long as we aren't already at the maximum number of sorts</li>
 *     <li>Add the new column to the end of the current sort columns</li>
 *     <li>Add the new column to the end of the current sort columns, as long as we aren't already at the maximum number of sorts</li>
 * </ul>
 * <p>For existing columns, the sort order will flip from ascending, to descending, to unsorted.  When a column becomes unsorted, we can choose to:
 * <ul>
 *     <li>Remove the unsorted column</li>
 *     <li>Remove the unsorted column and all subsequent sort columns</li>
 *     <li>Remove all sort columns.</li>
 * </ul>
 */
public class TreeTableSortStrategy implements TreeTableRowSorter.ColumnSortStrategy {

    public enum NewColumnAction { ADD_TO_START_IF_ROOM, ADD_TO_START, ADD_TO_END_IF_ROOM, ADD_TO_END }
    public enum WhenUnsortedAction { REMOVE, REMOVE_SUBSEQUENT, REMOVE_ALL }

    private static final int DEFAULT_MAX_SORT_KEYS = 3;
    private static final NewColumnAction DEFAULT_NEW_COLUMN_ACTION = NewColumnAction.ADD_TO_END;
    private static final WhenUnsortedAction DEFAULT_WHEN_UNSORTED_ACTION = WhenUnsortedAction.REMOVE_SUBSEQUENT;

    private int maximumSortKeys;
    private NewColumnAction newColumnAction;
    private WhenUnsortedAction whenUnsortedAction;

    /**
     * The list of default sort keys which are used if no other sort specified.
     */
    protected List<RowSorter.SortKey> defaultSortKeys;

    public TreeTableSortStrategy() {
        this(DEFAULT_MAX_SORT_KEYS, DEFAULT_NEW_COLUMN_ACTION, DEFAULT_WHEN_UNSORTED_ACTION);
    }

    public TreeTableSortStrategy(final int maximumSortKeys,
                                 final NewColumnAction newColumnAction,
                                 final WhenUnsortedAction whenUnsortedAction) {
        this.maximumSortKeys = maximumSortKeys;
        this.newColumnAction = newColumnAction;
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

    public NewColumnAction getNewColumnAction() {
        return newColumnAction;
    }

    public void setNewColumnAction(NewColumnAction newColumnAction) {
        this.newColumnAction = newColumnAction;
    }

    public WhenUnsortedAction getWhenUnsortedAction() {
        return whenUnsortedAction;
    }

    public void setWhenUnsortedAction(WhenUnsortedAction whenUnsortedAction) {
        this.whenUnsortedAction = whenUnsortedAction;
    }

    public int getMaximumSortKeys() {
        return maximumSortKeys;
    }

    public void setMaximumSortKeys(final int maximumSortKeys) {
        this.maximumSortKeys = maximumSortKeys;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(max keys: " + maximumSortKeys + ", new sort columns: " + newColumnAction + ", when unsorted: " + whenUnsortedAction + ')';
    }

    protected void processNewColumn(final List<RowSorter.SortKey> newKeys, final int columnToSort) {
        final RowSorter.SortKey newKey = new RowSorter.SortKey(columnToSort, SortOrder.ASCENDING);
        switch(newColumnAction) {
            case ADD_TO_END_IF_ROOM: { // adds to the end if we aren't past the max number of sorted columns.
                if (newKeys.size() < maximumSortKeys) {
                    newKeys.add(newKey);
                }
                break;
            }
            case ADD_TO_END: { // always add to the end - if there isn't enough room, the last sort key is replaced.
                if (newKeys.size() < maximumSortKeys) {
                    newKeys.add(newKey);
                } else {
                    newKeys.set(newKeys.size() - 1, newKey);
                }
                break;
            }
            case ADD_TO_START_IF_ROOM: { // adds to the start if we aren't already at maximum sort columns.
                if (newKeys.size() < maximumSortKeys) {
                    newKeys.add(0, newKey);
                }
                break;
            }
            case ADD_TO_START: { // adds to the start, and removes any existing ones past the max number of sorted columns.
                newKeys.add(0, newKey);
                truncateList(newKeys, maximumSortKeys);
                break;
            }
        }
    }

    protected void processExistingColumn(final List<RowSorter.SortKey> newKeys, final int sortKeyIndex) {
        final RowSorter.SortKey currentKey = newKeys.get(sortKeyIndex);
        final SortOrder nextState = nextOrder(currentKey.getSortOrder());
        if (nextState == SortOrder.UNSORTED) { // process a column becoming unsorted:
            processRemoveSortedColumn(newKeys, sortKeyIndex);
        } else { // Update the sort key:
            newKeys.set(sortKeyIndex, new RowSorter.SortKey(currentKey.getColumn(), nextState));
        }
    }

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

    protected final void truncateList(final List<?> list, final int newSize) {
        while (list.size() > newSize) {
            list.remove(list.size() - 1);
        }
    }
}
