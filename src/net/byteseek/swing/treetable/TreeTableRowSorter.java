package net.byteseek.swing.treetable;

import javax.swing.*;
import java.util.*;

//TODO: do we need some columns to be not sortable?

/**
 * A class which sorts a TreeTableModel, given the sort keys to sort on.
 * It provides an index of the view to model, and the model to view.
 * It can be provided to a JTable to sort the rows.
 */
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
        return isSorting() ? viewToModelIndex[index].modelIndex : index;
    }

    @Override
    public int convertRowIndexToView(final int index) {
        return isSorting() ? modelToViewIndex[index] : index;
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
        buildSortIndexes();
        fireSortOrderChanged(); //TODO: check we're sending correct message here.
    }

    @Override
    public void allRowsChanged() {
        buildSortIndexes();
        fireSortOrderChanged(); //TODO: check we're sending correct message here.
    }

    @Override
    public void rowsInserted(final int firstRow, final int endRow) {
       if (isSorting()) {
           buildSortIndexes();
           fireRowSorterChanged(null); //TODO: check we're sending correct message here.
       }
    }

    @Override
    public void rowsDeleted(final int firstRow, final int endRow) {
        if (isSorting()) {
            buildSortIndexes();
            fireRowSorterChanged(null); //TODO: check we're sending correct message here.
        }
    }

    @Override
    public void rowsUpdated(final int firstRow, final int endRow) {
        if (isSorting()) {
            buildSortIndexes();
            fireRowSorterChanged(null); //TODO: check we're sending correct message here.
        }
    }

    @Override
    public void rowsUpdated(final int firstRow, final int endRow, final int column) {
        if (isSorting()) {
            buildSortIndexes();
            fireRowSorterChanged(null); //TODO: check we're sending correct message here.
        }
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

            // They are now both at the same level - find the nodes that share a common parent (root will be common to all).
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
        // Go through all the sort keys to find something less than or bigger than.  If equal, try the next sort key.
        for (int sortIndex = 0; sortIndex < localKeys.size(); sortIndex++) {
            final SortKey key = localKeys.get(sortIndex);
            final SortOrder order = key.getSortOrder();
            if (order == SortOrder.UNSORTED) {
                return unsortedCompare;
            }
            final int result = compareNodes(firstNode, secondNode, key.getColumn());
            if (result != 0) {  // if not equal, we have a definite unequal result - return it.
                return order == SortOrder.ASCENDING? result: result * -1; // invert the result if not ascending
            }
        }
        //TODO: If all comparisons are equal, should we just return equal?  Why give an order to them at all?  Giving a definite order might help some sort algorithms I guess.
        return unsortedCompare;
    }

    protected int compareNodes(final TreeTableNode firstNode, final TreeTableNode secondNode, final int column) {
        final TreeTableModel localModel = model;

        // Compare on node values, if a node comparator is defined:
        final Comparator<TreeTableNode> nodeComparator = localModel.getNodeComparator();
        int result = nodeComparator == null ? 0 : nodeComparator.compare(firstNode, secondNode); // Compare with node comparator.

        // If we don't have a node comparator, or the comparison is equal, compare on column values:
        if (result == 0) {
            final Object value1 = localModel.getColumnValue(firstNode, column);
            final Object value2 = localModel.getColumnValue(secondNode, column);
            final Comparator<Object> columnComparator = (Comparator<Object>) localModel.getColumnComparator(column);
            if (columnComparator != null) {
                result = columnComparator.compare(value1, value2);            // Compare with provided comparator.
            } else {
                if (value1 instanceof Comparable<?>) {
                    result = ((Comparable<Object>) value1).compareTo(value2); // Compare directly if Comparable<>
                } else {
                    result = value1.toString().compareTo(value2.toString());  // Compare on string values:
                }
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

    private boolean isSorting() {
        return viewToModelIndex != null;
    }

    private void createSortIndexes() {
        buildViewToModelIndex();
        buildModelToViewIndex();
    }

    private void buildViewToModelIndex() {
        final int newRowCount = model.getRowCount();
        // If we don't have an index, or the index isn't exactly the size we need, create a new one.
        if (viewToModelIndex == null || viewToModelIndex.length != newRowCount) {
            viewToModelIndex = new SortRow[newRowCount];
        }
        createModelOrderRows(model.getRowCount());
        Arrays.sort(viewToModelIndex);
    }

    private void buildModelToViewIndex() {
        final int numRows = model.getRowCount();
        // if we don't have an index, or the the existing array is less than the size we need, create a new one.
        if (modelToViewIndex == null || modelToViewIndex.length < numRows) {
            modelToViewIndex = new int[numRows];
        }
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

    private void createModelOrderRows(final int numRows) {
        final SortRow[] sortRows = viewToModelIndex;
        for (int index = 0; index < numRows; index++) {
            sortRows[index] = new SortRow(this, index);
        }
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
