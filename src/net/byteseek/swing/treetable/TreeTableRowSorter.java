package net.byteseek.swing.treetable;

import javax.swing.*;
import java.text.Collator;
import java.util.*;

//TODO: are some columns not sortable?

public class TreeTableRowSorter extends RowSorter<TreeTableModel> {

    protected static final int MAX_DEFAULT_SORT_KEYS = 3;

    protected final TreeTableModel model;
    protected List<SortKey> sortKeys; // cannot be null - must be an empty list at minimum (or at least, via getSortKeys)
    protected int maximumSortKeys = MAX_DEFAULT_SORT_KEYS;

    protected SortRow[] viewToModelIndexes;
    protected int[] modelToViewIndexes;

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
        return viewToModelIndexes == null? index : viewToModelIndexes[index].rowIndex;
    }

    @Override
    public int convertRowIndexToView(final int index) {
        return modelToViewIndexes == null? index : modelToViewIndexes[index];
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

    }

    @Override
    public void allRowsChanged() {

    }

    @Override
    public void rowsInserted(final int firstRow, final int endRow) {

    }

    @Override
    public void rowsDeleted(final int firstRow, final int endRow) {

    }

    @Override
    public void rowsUpdated(final int firstRow, final int endRow) {

    }

    @Override
    public void rowsUpdated(final int firstRow, final int endRow, final int column) {

    }

    protected int compare(final int modelRowIndex1, final int modelRowIndex2) {
        TreeTableNode firstNode = model.getNodeAtRow(modelRowIndex1);
        TreeTableNode secondNode = model.getNodeAtRow(modelRowIndex2);

        // If the nodes don't already share a parent, we have to find two comparable parent nodes that do.
        if (firstNode.getParent() != secondNode.getParent()) {

            // Are the nodes at the same level as each other already?
            int firstLevel = firstNode.getLevel();
            int secondLevel = secondNode.getLevel();

            // If the nodes are at different depths, walk one of them back so they are at the same level as each other.
            if (firstLevel < secondLevel) {
                secondNode = getParentDown(secondNode, secondLevel - firstLevel);
            } else if (secondLevel < firstLevel) {
                firstNode =  getParentDown(firstNode, firstLevel - secondLevel);
            }

            // They are now both at the same level - find the nodes that share a common parent:
            while (firstNode.getParent() != secondNode.getParent()) {
                firstNode = (TreeTableNode) firstNode.getParent();
                secondNode = (TreeTableNode) secondNode.getParent();
            }
        }
        // Nodes now share a common parent, even if it is the root of the tree - compare values of nodes with same parent:
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
            if (result != 0) {  // if not equal, return result (adjusted for ascending/descending).  If equal, sort on next sort key (if any).
                return order == SortOrder.DESCENDING? result * -1 : result;
            }
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
            if (value1 instanceof Comparable<?>) {                           // Compare directly:
                result = ((Comparable<Object>) value1).compareTo(value2);
            } else {                                                         // Compare on string values:
                result = value1.toString().compareTo(value2.toString());
            }
        }
        return result;
    }

    protected void buildSortIndexes() {
        if (sortKeys.size() == 0) {
            clearSortIndexes();
        } else {
            createSortIndexes();
        }
    }

    protected void clearSortIndexes() {
        viewToModelIndexes = null;
        modelToViewIndexes = null;
    }

    protected void createSortIndexes() {
        buildViewToModelIndexes();
        buildModelToViewIndexes();
    }

    protected void buildViewToModelIndexes() {
        viewToModelIndexes = createModelOrderRows(this, model.getRowCount());
        Arrays.sort(viewToModelIndexes);
    }

    private void buildModelToViewIndexes() {
        modelToViewIndexes = new int[model.getRowCount()];
        //TODO: build 'em
    }

    protected SortOrder nextOrder(final SortOrder sortOrder) {
        final int keyState = sortOrder.ordinal();
        final SortOrder[] values = SortOrder.values();
        final int nextKeyState = (keyState + 1) % values.length;
        return values[nextKeyState];
    }

    protected int findKeyColumn(final int column) {
        final List<SortKey> localKeys = sortKeys; // local reference to avoid repeated get field calls.
        if (localKeys != null) {
            for (int keyIndex = 0; keyIndex < localKeys.size(); keyIndex++) {
                if (localKeys.get(keyIndex).getColumn() == column) {
                    return keyIndex;
                }
            }
        }
        return -1;
    }

    protected SortRow[] createModelOrderRows(final TreeTableRowSorter sorter, final int numRows) {
        final SortRow[] newRows = new SortRow[numRows];
        for (int index = 0; index < numRows; index++) {
            newRows[index] = new SortRow(sorter, index);
        }
        return newRows;
    }

    protected TreeTableNode getParentDown(final TreeTableNode node, final int levelsDown) {
        TreeTableNode result = node;
        for (int num = 0; num < levelsDown; num++) {
            result = (TreeTableNode) result.getParent();
        }
        return result;
    }

    protected void checkColumnIndex(int column) {
        if (column < 0 || column >= model.getColumnCount()) {
            throw new IndexOutOfBoundsException("Column " + column + " must be less than " + model.getColumnCount() + " and zero or greater.");
        }
    }

    protected static class SortRow implements Comparable<SortRow> {

        private final TreeTableRowSorter rowSorter;
        private int rowIndex;

        public SortRow(TreeTableRowSorter rowSorter, int rowIndex) {
            this.rowSorter = rowSorter;
            this.rowIndex = rowIndex;
        }

        @Override
        public int compareTo(SortRow o) {
            return rowSorter.compare(rowIndex, o.rowIndex);
        }
    }
}
