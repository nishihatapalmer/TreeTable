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
     * The position that a new sort key should have in the sort list.
     */
    public enum NewSortKeyPosition {
        /**
         * Make the sort key the first sorted key.
         */
        MAKE_FIRST,

        /**
         * Make the sort key the last sorted key.
         */
        ADD_TO_END
    }

    /**
     * How to update the position of an existing sort key when it is toggled.
     */
    public enum UpdateSortKeyPosition {
        /**
         * Make the sort key the first sorted key.
         */
        MAKE_FIRST,

        /**
         * Don't move the sort key from it's current position.
         */
        KEEP_POSITION
    }

    /**
     * The action to take when removing a sort key.
     */
    public enum RemoveSortKeyAction {
        /**
         * Removes the sort key from the list of sort keys. This is the default.
         */
        REMOVE,

        /**
         * Removes the sort key from the list of sort keys, and also any subsequent keys defined.
         * If we have 3 sort keys, and we make the second one unsorted, the second and third sort key would
         * be removed from the list.
         */
        REMOVE_SUBSEQUENT,

        /**
         * Clears all sort keys.
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
     * The position for a new sort key.
     */
    protected NewSortKeyPosition newSortKeyPosition;

    /**
     * The position for an updated sort key.
     */
    protected UpdateSortKeyPosition updateSortKeyPosition;

    /**
     * The action to take when a column becomes unsorted.
     */
    protected RemoveSortKeyAction removeSortKeyAction;

    /**
     * Constructs a TreeTableColumnSortStrategy with the default settings.
     * New columns clicked are added to end of existing sort columns, replacing the last sort column if we're at max columns.
     * Existing columns clicked are cycled around ascending/descending/unsorted, and maintain their sort position when clicked again.
     * Once unsorted, the column is removed from the sort keys.
     */
    public TreeTableColumnSortStrategy() {
        this(DEFAULT_MAX_SORT_KEYS, NewSortKeyPosition.ADD_TO_END, UpdateSortKeyPosition.KEEP_POSITION,
                RemoveSortKeyAction.REMOVE);
    }

    /**
     * Constructs a TreeTableColumnSortStrategy with the particular configuration passed in.
     *
     * @param maximumSortKeys The maximum number of sorted columns at any one time.
     * @param newSortKeyPosition The position of a new sort key in the sort list.
     * @param updateSortKeyPosition The position of an updated sort key in the sort list.
     * @param removeSortKeyAction What to do when a sort key is removed.
     * @throws IllegalArgumentException if any of the actions are null.
     */
    public TreeTableColumnSortStrategy(final int maximumSortKeys,
                                       final NewSortKeyPosition newSortKeyPosition,
                                       final UpdateSortKeyPosition updateSortKeyPosition,
                                       final RemoveSortKeyAction removeSortKeyAction) {
        if (newSortKeyPosition == null || updateSortKeyPosition == null || removeSortKeyAction == null) {
            throw new IllegalArgumentException("Actions to take cannot be null.");
        }
        this.maximumSortKeys = maximumSortKeys;
        this.newSortKeyPosition = newSortKeyPosition;
        this.updateSortKeyPosition = updateSortKeyPosition;
        this.removeSortKeyAction = removeSortKeyAction;
    }

    @Override
    public List<RowSorter.SortKey> buildNewSortKeys(final int columnToSort, final List<RowSorter.SortKey> sortKeys) {
        final List<RowSorter.SortKey> newKeys = new ArrayList<>(sortKeys);
        final int sortKeyIndex = findKeyColumn(newKeys, columnToSort);
        if (sortKeyIndex < 0) {
            addNewSortKey(newKeys, columnToSort);
        } else {
            toggleExistingSortKey(newKeys, sortKeyIndex);
        }
        return newKeys;
    }

    /**
     * @return the action to take on a new column becoming sorted.
     */
    public NewSortKeyPosition getNewSortKeyPosition() {
        return newSortKeyPosition;
    }

    /**
     * Sets the action to take when a new column is sorted.
     * @param newSortKeyPosition the action to take when a new column is sorted.
     */
    public void setNewSortKeyPosition(NewSortKeyPosition newSortKeyPosition) {
        this.newSortKeyPosition = newSortKeyPosition;
    }

    /**
     * @return the action to take on a sorted column becoming unsorted.
     */
    public RemoveSortKeyAction getRemoveSortKeyAction() {
        return removeSortKeyAction;
    }

    /**
     * Sets the action to take when a sorted column becomes unsorted.
     * @param removeSortKeyAction The action to take when a sorted column becomes unsorted.
     */
    public void setRemoveSortKeyAction(RemoveSortKeyAction removeSortKeyAction) {
        this.removeSortKeyAction = removeSortKeyAction;
    }

    /**
     * @return the action to take on an existing sorted column being toggled again.
     */
    public UpdateSortKeyPosition getUpdateSortKeyPosition() {
        return updateSortKeyPosition;
    }

    /**
     * Sets the action to take when an existing sorted column is toggled.
     * @param updateSortKeyPosition the action to take when an existing sorted column is toggled.
     */
    public void setUpdateSortKeyPosition(UpdateSortKeyPosition updateSortKeyPosition) {
        this.updateSortKeyPosition = updateSortKeyPosition;
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
                ", new: " + newSortKeyPosition +
                ", existing: " + updateSortKeyPosition +
                ", unsorted: " + removeSortKeyAction + ')';
    }

    /**
     * Adds a new ASCENDING sort key for the column, and puts it in the appropriate position in the sort list.
     *
     * @param newKeys A list of SortKeys to manipulate.
     * @param columnToSort The index of the newly sorted column.
     */
    protected void addNewSortKey(final List<RowSorter.SortKey> newKeys, final int columnToSort) {
        final RowSorter.SortKey newKey = new RowSorter.SortKey(columnToSort, SortOrder.ASCENDING);
        switch(newSortKeyPosition) {
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
    protected void toggleExistingSortKey(final List<RowSorter.SortKey> newKeys, final int sortKeyIndex) {
        final RowSorter.SortKey currentKey = newKeys.get(sortKeyIndex);
        final SortOrder nextState = nextSortOrder(currentKey.getSortOrder());
        if (nextState == SortOrder.UNSORTED) {
            removeSortKey(newKeys, sortKeyIndex);
        } else {
            updateSortKey(newKeys, sortKeyIndex, nextState);
        }
    }

    /**
     * The action to taken when a sorted column becomes unsorted.
     * @param newKeys A list of new keys to manipulate.
     * @param sortKeyIndex The index of the SortKey for the column which is becoming unsorted.
     */
    protected void removeSortKey(final List<RowSorter.SortKey> newKeys, final int sortKeyIndex) {
        switch (removeSortKeyAction) {
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
     * Updates a sort key to another sort order, and alters its position if required.
     * @param sortKeys The list of sort keys
     * @param sortKeyIndex The index of the sort key to update.
     * @param newSortOrder The new sort order.
     */
    protected void updateSortKey(final List<RowSorter.SortKey> sortKeys, final int sortKeyIndex, final SortOrder newSortOrder) {
        final RowSorter.SortKey newKey = new RowSorter.SortKey(sortKeys.get(sortKeyIndex).getColumn(), newSortOrder);
        switch (updateSortKeyPosition) {
            case MAKE_FIRST: {
                sortKeys.remove(sortKeyIndex);
                sortKeys.add(0, newKey);
                break;
            }
            case KEEP_POSITION: {
                sortKeys.set(sortKeyIndex, newKey);
                break;
            }
        }
    }

    /**
     * Cycles through and wraps around the SortOrder enum, from ASCENDING -> DESCENDING -> UNSORTED -> ASCENDING -> ...
     * @param sortOrder A SortOrder enumeration.
     * @return The next sort order (wrapping around).
     */
    protected SortOrder nextSortOrder(final SortOrder sortOrder) {
        return SortOrder.values()[(sortOrder.ordinal() + 1) % SortOrder.values().length];
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
