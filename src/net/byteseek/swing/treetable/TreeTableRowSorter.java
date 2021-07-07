package net.byteseek.swing.treetable;

import javax.swing.*;
import java.util.*;

//TODO: are some columns not sortable?

public class TreeTableRowSorter extends RowSorter<TreeTableModel> {

    protected static final int MAX_DEFAULT_SORT_KEYS = 3;

    protected final TreeTableModel model;
    protected List<SortKey> sortKeys; // cannot be null - must be an empty list at minimum (or at least, via getSortKeys)
    protected int maximumSortKeys = MAX_DEFAULT_SORT_KEYS;

    protected SortRow[] viewToModelIndex;
    protected int[] modelToViewIndex;

    public TreeTableRowSorter(final TreeTableModel model) {
        if (model == null) {
            throw new IllegalArgumentException("Must supply a non null TreeTableModel.");
        }
        this.model = model;
        sortKeys = new ArrayList<>();
    }

    @Override
    public TreeTableModel getModel() {
        return model;
    }

    public int getMaximumSortKeys() {
        return maximumSortKeys;
    }

    public void setMaximumSortKeys(final int maximumSortKeys) {
        this.maximumSortKeys = maximumSortKeys;
    }

    //TODO: define good sort order toggling behaviour.  This erases later sorts once a prior sort becomes unsorted.
    //      should adding a sort column require a SHIFT-click or something?
    //      Or should we not allow multi column sorting, or restrict it to just two columns...?
    @Override
    public void toggleSortOrder(final int column) {
        checkColumnIndex(column);
        final List<SortKey> newKeys = new ArrayList<>(sortKeys);
        final int sortKeyIndex = findKeyColumn(column);
        // if not currently being sorted - and less than max sort keys being used - add a new SortKey.
        if (sortKeyIndex < 0 && newKeys.size() < maximumSortKeys) {
            newKeys.add(new SortKey(column, SortOrder.ASCENDING));
        } else { // It's a column currently being sorted.
            final SortKey currentKey = newKeys.get(sortKeyIndex);
            final SortOrder nextState = nextOrder(currentKey.getSortOrder());
            //TODO: this multi column behaviour cycle, but remove if a descendant column becomes unsorted... does it work?
            // If we're going to an unsorted state, remove all the sort keys after this one, in reverse order to avoid rebuilding the list each time.
            // Make algorithm a strategy?  So you can choose different toggle strategies?
            if (nextState == SortOrder.UNSORTED) {
                for (int removeIndex = newKeys.size() - 1;  removeIndex >= sortKeyIndex; removeIndex--) {
                    newKeys.remove(removeIndex);
                }
            } else { // Update the sort key:
                newKeys.set(sortKeyIndex, new SortKey(currentKey.getColumn(), nextState));
            }
        }
        setSortKeys(newKeys);
    }

    @Override
    public int convertRowIndexToModel(final int index) {
        return viewToModelIndex == null? index
                : viewToModelIndex[index].modelIndex;
    }

    @Override
    public int convertRowIndexToView(final int index) {
        return modelToViewIndex == null? index
                : modelToViewIndex[index];
    }

    @Override
    public void setSortKeys(final List<? extends SortKey> keys) {
        List<? extends SortKey> newKeys = keys == null? Collections.emptyList() : keys;
        if (!sortKeys.equals(newKeys)) {
            this.sortKeys = new ArrayList<>(newKeys);
            buildSortIndexes();
            fireSortOrderChanged();
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
        //buildSortIndexes();
    }

    @Override
    public void allRowsChanged() {
        //buildSortIndexes();
    }

    @Override
    public void rowsInserted(final int firstRow, final int endRow) {
       // buildSortIndexes();
    }

    @Override
    public void rowsDeleted(final int firstRow, final int endRow) {
        //buildSortIndexes();
    }

    @Override
    public void rowsUpdated(final int firstRow, final int endRow) {
        //buildSortIndexes();
    }

    @Override
    public void rowsUpdated(final int firstRow, final int endRow, final int column) {
        //buildSortIndexes();
    }

    protected int compare(final int modelRowIndex1, final int modelRowIndex2) {
        TreeTableNode firstNode = model.getNodeAtRow(modelRowIndex1);
        TreeTableNode secondNode = model.getNodeAtRow(modelRowIndex2);

        // If the nodes don't already share a parent, we have to find two comparable parent nodes that do.
        if (firstNode.getParent() != secondNode.getParent()) {

            // If the nodes are at different levels, walk one of them back so they are at the same level as each other.
            final int firstLevel = firstNode.getLevel();
            final int secondLevel = secondNode.getLevel();
            if (firstLevel < secondLevel) {
                secondNode = getAncestor(secondNode, secondLevel - firstLevel);
            } else if (secondLevel < firstLevel) {
                firstNode =  getAncestor(firstNode, firstLevel - secondLevel);
            }

            // They are now both at the same level - find the nodes that share a common parent:
            while (firstNode.getParent() != secondNode.getParent()) {
                firstNode = (TreeTableNode) firstNode.getParent();
                secondNode = (TreeTableNode) secondNode.getParent();
            }
        }

        // Nodes share a common parent - compare values:
        return compare(firstNode, secondNode, modelRowIndex1 - modelRowIndex2);
    }

    protected int compare(final TreeTableNode firstNode, final TreeTableNode secondNode, final int unsortedCompare) {
        final List<SortKey> localKeys = sortKeys;
        final TreeTableModel localModel = model;
        for (int sortIndex = 0; sortIndex < localKeys.size(); sortIndex++) {
            final SortKey key = localKeys.get(sortIndex);
            final SortOrder order = key.getSortOrder();
            if (order == SortOrder.UNSORTED) {
                return unsortedCompare;
            }
            final int column = key.getColumn();
            final Object value1 = localModel.getColumnValue(firstNode, column);
            final Object value2 = localModel.getColumnValue(secondNode, column);
            final int result = compareNodeValues(value1, value2, column);
            if (result != 0) {  // if not equal, we have a result - return it.
                return order == SortOrder.ASCENDING? result: result * -1; // invert the result if not ascending
            }
            // result is equal - sort on next sort key (if any).
        }
        //TODO: If all comparisons are equal, should we just return equal?  Why give an order to them at all?  Giving a definite order might help some sort algorithms I guess.
        return unsortedCompare; // If we got through all sort keys and everything is still equal, use the unsorted order to break the tie.
    }

    protected int compareNodeValues(final Object value1, final Object value2, final int column) {
        final Comparator<Object> comparator = (Comparator<Object>) model.getColumnComparator(column);
        final int result;
        if (comparator != null)  {
            result = comparator.compare(value1, value2);                     // Use the provided comparator:
        } else {
            if (value1 instanceof Comparable<?>) {                           // Compare directly if Comparable<>
                result = ((Comparable<Object>) value1).compareTo(value2);
            } else {                                                         // Compare on string values:
                result = value1.toString().compareTo(value2.toString());
            }
        }
        return result;
    }

    private void buildSortIndexes() {
        if (sortKeys.size() == 0) {
            clearSortIndexes();
        } else {
            createSortIndexes();
        }
    }

    private void clearSortIndexes() {
        viewToModelIndex = null;
        modelToViewIndex = null;
    }

    private void createSortIndexes() {
        buildViewToModelIndex();
        buildModelToViewIndex();
    }

    private void buildViewToModelIndex() {
        viewToModelIndex = createModelOrderRows(model.getRowCount());
        Arrays.sort(viewToModelIndex);
    }

    private void buildModelToViewIndex() {
        final int numRows = model.getRowCount();
        modelToViewIndex = new int[numRows];
        for (int viewIndex = 0; viewIndex < numRows; viewIndex++) {
            modelToViewIndex[viewToModelIndex[viewIndex].modelIndex] = viewIndex;
        }
    }

    private SortOrder nextOrder(final SortOrder sortOrder) {
        final int keyState = sortOrder.ordinal();
        final SortOrder[] values = SortOrder.values();
        final int nextKeyState = (keyState + 1) % values.length;
        return values[nextKeyState];
    }

    private int findKeyColumn(final int column) {
        final List<SortKey> localKeys = sortKeys;
        if (localKeys != null) {
            for (int keyIndex = 0; keyIndex < localKeys.size(); keyIndex++) {
                if (localKeys.get(keyIndex).getColumn() == column) {
                    return keyIndex;
                }
            }
        }
        return -1;
    }

    private SortRow[] createModelOrderRows(final int numRows) {
        final SortRow[] newRows = new SortRow[numRows];
        for (int index = 0; index < numRows; index++) {
            newRows[index] = new SortRow(this, index);
        }
        return newRows;
    }

    private TreeTableNode getAncestor(final TreeTableNode node, final int levelsDown) {
        TreeTableNode result = node;
        for (int num = 0; num < levelsDown; num++) {
            result = (TreeTableNode) result.getParent();
        }
        return result;
    }

    private void checkColumnIndex(int column) {
        if (column < 0 || column >= model.getColumnCount()) {
            throw new IndexOutOfBoundsException("Column " + column + " must be less than " + model.getColumnCount() + " and zero or greater.");
        }
    }

    private static class SortRow implements Comparable<SortRow> {

        private final TreeTableRowSorter rowSorter;
        private int modelIndex;

        public SortRow(TreeTableRowSorter sorter, int modelIndex) {
            this.rowSorter = sorter;
            this.modelIndex = modelIndex;
        }

        @Override
        public int compareTo(SortRow o) {
            return rowSorter.compare(modelIndex, o.modelIndex);
        }
    }
}
