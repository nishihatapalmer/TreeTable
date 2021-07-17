package net.byteseek.swing.treetable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class MultiColumnSortStrategy implements TreeTableRowSorter.ColumnSortStrategy {

    public enum NewColumnAction { ADD_TO_START_IF_ROOM, ADD_TO_START, ADD_TO_END_IF_ROOM, ADD_TO_END }
    public enum WhenUnsortedAction { REMOVE, REMOVE_SUBSEQUENT, REMOVE_ALL }

    private static final int MAX_DEFAULT_SORT_KEYS = 2;
    private static final NewColumnAction DEFAULT_NEW_COLUMN_ACTION = NewColumnAction.ADD_TO_END;
    private static final WhenUnsortedAction DEFAULT_WHEN_UNSORTED_ACTION = WhenUnsortedAction.REMOVE_SUBSEQUENT;

    private int maximumSortKeys;
    private NewColumnAction newColumnAction;
    private WhenUnsortedAction whenUnsortedAction;

    public MultiColumnSortStrategy() {
        this(MAX_DEFAULT_SORT_KEYS, DEFAULT_NEW_COLUMN_ACTION, DEFAULT_WHEN_UNSORTED_ACTION);
    }

    public MultiColumnSortStrategy(final int maximumSortKeys, final NewColumnAction newColumnAction,
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
