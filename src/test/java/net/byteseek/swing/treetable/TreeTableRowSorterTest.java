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

import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.tree.DefaultMutableTreeNode;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class TreeTableRowSorterTest extends BaseTestClass {

    @Test
    void testIsSortableTrueByDefault() {
        TreeTableRowSorter sorter = new TreeTableRowSorter(model);
        assertTrue(sorter.isSortable(0), "Column 0 should be sortable by default");
        assertTrue(sorter.isSortable(1), "Column 1 should be sortable by default");
    }

    @Test
    void testSetAllColumnsSortable() {
        TreeTableRowSorter sorter = new TreeTableRowSorter(model);
        sorter.setSortable(0, false); // Disable sorting for column 0
        sorter.setAllColumnsSortable();
        assertTrue(sorter.isSortable(0), "Column 0 should be sortable after setAllColumnsSortable()");
        assertTrue(sorter.isSortable(1), "Column 1 should still be sortable");
    }

    @Test
    void testSetAllColumnsSortableAfterDisablingColumns() {
        TreeTableRowSorter sorter = new TreeTableRowSorter(model);
        sorter.setSortable(0, false); // Disable sorting for column 0
        sorter.setSortable(1, false); // Disable sorting for column 1
        sorter.setAllColumnsSortable();
        assertTrue(sorter.isSortable(0), "Column 0 should be sortable after setAllColumnsSortable()");
        assertTrue(sorter.isSortable(1), "Column 1 should be sortable after setAllColumnsSortable()");
    }

    @Test
    void testSetIsSortableFalse() {
        TreeTableRowSorter sorter = new TreeTableRowSorter(model);
        sorter.setSortable(0, false);
        assertFalse(sorter.isSortable(0), "Column 0 should not be sortable after being set to false");
    }

    @Test
    void testSetIsSortableTrueAfterSettingFalse() {
        TreeTableRowSorter sorter = new TreeTableRowSorter(model);
        sorter.setSortable(0, false);
        assertFalse(sorter.isSortable(0), "Column 0 should not be sortable after being set to false");
        sorter.setSortable(0, true);
        assertTrue(sorter.isSortable(0), "Column 0 should be sortable again after being set to true");
    }

    @Test
    void testMultipleColumnsSetToNotSortable() {
        TreeTableRowSorter sorter = new TreeTableRowSorter(model);
        sorter.setSortable(1, false);
        sorter.setSortable(2, false);
        assertFalse(sorter.isSortable(1), "Column 1 should not be sortable");
        assertFalse(sorter.isSortable(2), "Column 2 should not be sortable");
    }

    @Test
    void getModel() {
    }

    @Test
    void toggleSortOrder() {
        TreeTableRowSorter sorter = new TreeTableRowSorter(model);

        // Test 1: Initially, no sort keys
        assertEquals(0, sorter.getSortKeys().size(), "Sort keys should be empty initially");

        // Toggle sort order for a valid column; should become ascending
        sorter.toggleSortOrder(0);
        assertEquals(1, sorter.getSortKeys().size(), "Sort keys should contain one key");
        assertEquals(SortOrder.ASCENDING, sorter.getSortKeys().get(0).getSortOrder(), "Column 0 should now be sorted in ascending order.");
    }

    @Test
    void testToggleSortOrderSwitchToDescending() {
        TreeTableRowSorter sorter = new TreeTableRowSorter(model);

        // Set the column to ascending, then toggle to descending
        sorter.toggleSortOrder(0);
        sorter.toggleSortOrder(0);

        assertEquals(1, sorter.getSortKeys().size(), "Sort keys should contain one key");
        assertEquals(SortOrder.DESCENDING, sorter.getSortKeys().get(0).getSortOrder(), "Column 0 should now be sorted in descending order.");
    }

    @Test
    void testToggleSortOrderRemoveSortKey() {
        TreeTableRowSorter sorter = new TreeTableRowSorter(model);

        // Set the column to descending, then toggle to remove sorting
        sorter.toggleSortOrder(0);
        sorter.toggleSortOrder(0);
        sorter.toggleSortOrder(0);

        assertEquals(0, sorter.getSortKeys().size(), "Sort keys should be removed after toggling twice from a descending order.");
    }

    @Test
    void testToggleSortOrderUnsortableColumn() {
        TreeTableRowSorter sorter = new TreeTableRowSorter(model);
        sorter.setSortable(1, false); // Make column unsortable

        List<? extends RowSorter.SortKey> originalKeys = sorter.getSortKeys();

        // Attempt to toggle an unsortable column
        sorter.toggleSortOrder(1);

        assertEquals(originalKeys, sorter.getSortKeys(), "Sort keys should remain unchanged for an unsortable column.");
    }

    @Test
    void testToggleSortOrderInvalidColumn() {
        TreeTableRowSorter sorter = new TreeTableRowSorter(model);

        // Attempt to toggle a column index that does not exist
        assertThrows(IndexOutOfBoundsException.class, () -> sorter.toggleSortOrder(-1), "Toggling an invalid column index should throw an exception.");
        assertThrows(IndexOutOfBoundsException.class, () -> sorter.toggleSortOrder(model.getColumnCount()), "Toggling an out-of-bounds column index should throw an exception.");
    }

    @Test
    void testRowsUpdatedWithoutSorting() {
        model.bindTable(table);
        model.expandTree();
        TreeTableRowSorter sorter = (TreeTableRowSorter) table.getRowSorter();
        sorter.setSortKeys(Collections.emptyList()); // Disable sorting

        assertDoesNotThrow(() -> sorter.rowsUpdated(2, 4), "Updating rows without sorting should not throw an exception.");
    }

    @Test
    void testRowsUpdatedWithSortingAndRebuild() {
        model.bindTable(table);
        model.expandTree();
        TreeTableRowSorter sorter = (TreeTableRowSorter) table.getRowSorter();
        sorter.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING))); // Enable sorting
        sorter.setRebuildIndices(true);

        assertDoesNotThrow(() -> sorter.rowsUpdated(1, 3), "Updating rows with sorting and rebuild should not throw an exception.");
    }

    @Test
    void testRowsUpdatedWithSortingAndNoRebuild() {
        model.bindTable(table);
        model.expandTree();
        TreeTableRowSorter sorter = (TreeTableRowSorter) table.getRowSorter();
        sorter.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING))); // Enable sorting
        sorter.setRebuildIndices(false);

        assertDoesNotThrow(() -> sorter.rowsUpdated(2, 2), "Updating rows with sorting and no rebuild should not throw an exception.");
    }

    @Test
    void testRowsUpdatedWithInvalidIndices() {
        model.bindTable(table);
        TreeTableRowSorter sorter = (TreeTableRowSorter) table.getRowSorter();

        // Setting sorting to trigger validation in rowsUpdated
        sorter.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));

        assertThrows(IndexOutOfBoundsException.class, () -> sorter.rowsUpdated(-1, 2), "Negative indices should throw an exception.");
        assertThrows(IndexOutOfBoundsException.class, () -> sorter.rowsUpdated(5, 6), "Indices greater than row count should throw an exception.");
        assertThrows(IndexOutOfBoundsException.class, () -> sorter.rowsUpdated(3, 1), "Invalid range (start > end) should throw an exception.");
    }

    @Test
    void convertRowIndexToModel() {
        testConvertRowIndexToModelWithoutSorting();
        testConvertRowIndexToModelWithSorting();
        testConvertRowIndexToModelInvalidIndex();
    }

    private void testConvertRowIndexToModelWithoutSorting() {
        model.bindTable(table);
        TreeTableRowSorter sorter = (TreeTableRowSorter) table.getRowSorter();
        sorter.setSortKeys(Collections.emptyList()); // Disable sorting
        int rowCount = model.getRowCount();

        for (int i = 0; i < rowCount; i++) {
            assertEquals(i, sorter.convertRowIndexToModel(i), "Row index should match model index when sorting is disabled.");
        }
    }

    private void testConvertRowIndexToModelWithSorting() {
        model.bindTable(table);
        TreeTableRowSorter sorter = (TreeTableRowSorter) table.getRowSorter();
        sorter.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING))); // Enable sorting

        List<Integer> modelIndices = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            modelIndices.add(sorter.convertRowIndexToModel(i));
        }

        List<Integer> expectedIndices = new ArrayList<>(modelIndices);
        Collections.sort(expectedIndices); // Sorting based on ascending order

        assertEquals(expectedIndices, modelIndices, "Model indices should reflect correct sorting order.");
    }

    private void testConvertRowIndexToModelInvalidIndex() {
        model.bindTable(table);
        TreeTableRowSorter sorter = (TreeTableRowSorter) table.getRowSorter();
        int rowCount = model.getRowCount();
        sorter.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING))); // Enable sorting

        assertThrows(IndexOutOfBoundsException.class, () -> sorter.convertRowIndexToModel(-1), "Negative indices should throw an exception.");
        assertThrows(IndexOutOfBoundsException.class, () -> sorter.convertRowIndexToModel(rowCount), "Index exceeding row count should throw an exception.");
    }

    @Test
    void convertRowIndexToView() {
        testConvertRowIndexToViewWithoutSorting();
        testConvertRowIndexToViewWithSorting();
        testConvertRowIndexToViewInvalidIndex();
    }

    private void testConvertRowIndexToViewWithoutSorting() {
        model.bindTable(table);
        TreeTableRowSorter sorter = (TreeTableRowSorter) table.getRowSorter();
        sorter.setSortKeys(Collections.emptyList()); // Disable sorting
        int rowCount = model.getRowCount();

        for (int i = 0; i < rowCount; i++) {
            assertEquals(i, sorter.convertRowIndexToView(i), "Row index should match view index when sorting is disabled.");
        }
    }

    private void testConvertRowIndexToViewWithSorting() {
        model.bindTable(table);
        TreeTableRowSorter sorter = (TreeTableRowSorter) table.getRowSorter();
        sorter.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING))); // Enable sorting

        List<Integer> viewIndices = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            viewIndices.add(sorter.convertRowIndexToView(i));
        }

        List<Integer> expectedViewIndices = new ArrayList<>(viewIndices);
        Collections.sort(expectedViewIndices); // Sorting based on ascending order

        assertEquals(expectedViewIndices, viewIndices, "View indices should reflect correct sorting order.");
    }

    private void testConvertRowIndexToViewInvalidIndex() {
        model.bindTable(table);
        TreeTableRowSorter sorter = (TreeTableRowSorter) table.getRowSorter();
        int rowCount = model.getRowCount();

        assertThrows(IndexOutOfBoundsException.class, () -> sorter.convertRowIndexToView(-1), "Negative indices should throw an exception.");
        assertThrows(IndexOutOfBoundsException.class, () -> sorter.convertRowIndexToView(rowCount), "Index exceeding row count should throw an exception.");
    }

    @Test
    void setSortKeys() {
    }

    @Test
    void getSortKeys() {
    }

    @Test
    void getViewRowCount() {
    }

    @Test
    void getModelRowCount() {
        TreeTableRowSorter sorter = new TreeTableRowSorter(model);
        int expectedRowCount = model.getRowCount();
        assertEquals(expectedRowCount, sorter.getModelRowCount(), "getModelRowCount() should return the correct number of rows in the model.");
    }

    @Test
    void testGetModelRowCountWithDynamicChanges() {
        TreeTableRowSorter sorter = new TreeTableRowSorter(model);
        model.bindTable(table);

        // Initial count
        int rowCount = model.getRowCount();
        assertEquals(rowCount, sorter.getModelRowCount(), "Initial row count should match.");

        model.expandTree();
        rowCount = model.getRowCount();
        assertEquals(rowCount, sorter.getModelRowCount(), "Expanded row count should match.");
    }

    @Test
    void testNoExceptionWhenInvisibleRowsUpdatedWhenNotSorting() {
        model.bindTable(table);
        model.expandTree();
        RowSorter<?> sorter = table.getRowSorter();
        sorter.rowsUpdated(2, 2);
    }

    @Test
    void testExceptionWhenInvisibleRowsUpdatedWhenSorting() {
        model.bindTable(table);
        model.setSortKeys(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        RowSorter<?> sorter = table.getRowSorter();
        assertThrows(IndexOutOfBoundsException.class, () -> sorter.rowsUpdated(2, 2));
    }

    @Test
    void testNoExceptionWhenVisibleRowsUpdatedWhenNotSorting() {
        model.bindTable(table);
        model.expandTree();
        RowSorter<?> sorter = table.getRowSorter();
        sorter.rowsUpdated(2, 2);
    }

    @Test
    void testNoExceptionWhenVisibleRowsUpdatedWhenSorting() {
        model.bindTable(table);
        model.expandTree();
        RowSorter<?> sorter = table.getRowSorter();
        sorter.rowsUpdated(2, 2);
    }

    @Test
    void testUpdateSortIndicesWhenSortingEnabled() {
        model.bindTable(table);
        model.expandTree();
        TreeTableRowSorter sorter = (TreeTableRowSorter) table.getRowSorter();
        sorter.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));

        assertDoesNotThrow(() -> sorter.updateSortIndices(2));
    }

    @Test
    void testUpdateSortIndicesWhenSortingDisabled() {
        model.bindTable(table);
        model.expandTree();
        TreeTableRowSorter sorter = (TreeTableRowSorter) table.getRowSorter();
        sorter.setSortKeys(Collections.emptyList()); // Disable sorting

        assertDoesNotThrow(() -> sorter.updateSortIndices(2));
    }

    @Test
    void testUpdateSortIndicesRepositionsNodeCorrectly() {
        model.bindTable(table);
        model.expandTree();
        TreeTableRowSorter sorter = (TreeTableRowSorter) table.getRowSorter();
        sorter.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));

        // update the child at model index 2
        TestTreeTableModel.TestObject object = TreeUtils.getUserObject(child1);
        object.description = "aaa"; // comes before "child0".
        sorter.updateSortIndices(2);
        int newViewIndex = sorter.convertRowIndexToView(2);
        assertEquals(1, newViewIndex, "The updated node should have moved to the correct position.");
    }

    @Test
    void testUpdateSortIndicesNoChangeForCorrectlyPositionedRow() {
        model.bindTable(table);
        model.expandTree();
        TreeTableRowSorter sorter = (TreeTableRowSorter) table.getRowSorter();
        sorter.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));

        int originalViewIndex = sorter.convertRowIndexToView(2);
        sorter.updateSortIndices(2);
        int updatedViewIndex = sorter.convertRowIndexToView(2);
        assertEquals(originalViewIndex, updatedViewIndex, "Node should remain in the same place if it was already correctly positioned.");
    }

    @Test
    void testRowsInsertedWhenSortingEnabled() {
        model.bindTable(table);
        model.expandTree();
        model.setSortKeys(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        RowSorter<?> sorter = table.getRowSorter();
        assertDoesNotThrow(() -> sorter.rowsInserted(3, 5));
    }

    @Test
    void testRowsDeletedWithValidIndices() {
        model.bindTable(table);
        model.expandTree();
        model.setSortKeys(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        RowSorter<?> sorter = table.getRowSorter();
        assertDoesNotThrow(() -> sorter.rowsDeleted(1, 3));
    }

    /*  Can only test invalid indices in terms of model before change - but we get model after change
        in the current design.  Look at how DefaultRowSorter does it.
    @Test
    void testRowsDeletedWithInvalidIndices() {
        model.bindTable(table);
        model.expandTree();
        model.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
        RowSorter<?> sorter = table.getRowSorter();
        assertThrows(IndexOutOfBoundsException.class, () -> sorter.rowsDeleted(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> sorter.rowsDeleted(5, 10));
    }
     */

    @Test
    void testRowsDeletedWhenSortingEnabled() {
        model.bindTable(table);
        model.expandTree();
        TreeTableRowSorter sorter = (TreeTableRowSorter) table.getRowSorter();
        sorter.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.DESCENDING)));
        assertDoesNotThrow(() -> sorter.rowsDeleted(2, 4));
    }

    @Test
    void testRowsDeletedWithRebuildIndicesEnabled() {
        model.bindTable(table);
        model.expandTree();
        model.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
        TreeTableRowSorter sorter = (TreeTableRowSorter) table.getRowSorter();
        sorter.setRebuildIndices(true);
        assertDoesNotThrow(() -> sorter.rowsDeleted(0, 3));
    }

    @Test
    void testRowsDeletedWithRebuildIndicesDisabled() {
        model.bindTable(table);
        model.expandTree();
        model.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
        TreeTableRowSorter sorter = (TreeTableRowSorter) table.getRowSorter();
        sorter.setRebuildIndices(false);
        assertDoesNotThrow(() -> sorter.rowsDeleted(0, 3));
    }

    @Test
    void testRowsInsertedWhenSortingDisabled() {
        model.bindTable(table);
        model.expandTree();
        RowSorter<?> sorter = table.getRowSorter();
        model.setSortKeys((RowSorter.SortKey) null); // Disable sorting
        assertDoesNotThrow(() -> sorter.rowsInserted(1, 2));
    }

    @Test
    void testRowsInsertedWithValidIndices() {
        model.bindTable(table);
        model.expandTree();
        model.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
        TreeTableRowSorter sorter = (TreeTableRowSorter) table.getRowSorter();
        assertDoesNotThrow(() -> sorter.rowsInserted(0, 2));
    }

    @Test
    void testRowsInsertedWithInvalidIndices() {
        model.bindTable(table);
        model.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
        TreeTableRowSorter sorter = (TreeTableRowSorter) table.getRowSorter();
        assertThrows(IndexOutOfBoundsException.class, () -> sorter.rowsInserted(-1, 2), "Negative indices should throw an exception.");
        assertThrows(IndexOutOfBoundsException.class, () -> sorter.rowsInserted(5, 10), "Indices outside the row count should throw an exception.");
        assertThrows(IndexOutOfBoundsException.class, () -> sorter.rowsInserted(3, 2), "Invalid range (start > end) should throw an exception.");
    }

    @Test
    void testRowsInsertedWhenRebuildIndicesEnabled() {
        model.bindTable(table);
        model.expandTree();
        model.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
        TreeTableRowSorter sorter = (TreeTableRowSorter) table.getRowSorter();
        sorter.setRebuildIndices(true);
        assertDoesNotThrow(() -> sorter.rowsInserted(0, 2));
    }

    @Test
    void testRowsInsertedWhenRebuildIndicesDisabled() {
        model.bindTable(table);
        model.expandTree();
        model.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
        TreeTableRowSorter sorter = (TreeTableRowSorter) table.getRowSorter();
        sorter.setRebuildIndices(false);
        assertDoesNotThrow(() -> sorter.rowsInserted(0, 2));
    }

    @Test
    void testRowsInsertedWithRebuildIndicesEnabled() {
        model.bindTable(table);
        model.expandTree();
        model.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
        ((TreeTableRowSorter) table.getRowSorter()).setRebuildIndices(true);
        assertDoesNotThrow(() -> table.getRowSorter().rowsInserted(0, 4));
    }


    @Test
    void testExceptionWhenInvalidRowsInserted() {
        model.bindTable(table);
        model.setSortKeys(List.of(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
        RowSorter<?> sorter = table.getRowSorter();
        assertThrows(IndexOutOfBoundsException.class, () -> sorter.rowsInserted(-1, 5));
    }


}