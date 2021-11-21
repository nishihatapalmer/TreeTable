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
import java.util.Collections;
import java.util.List;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import static net.byteseek.swing.treetable.TreeTableColumnSortStrategy.ToggleNewColumnAction.MAKE_FIRST;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TreeTableColumnSortStrategyTest {

    private static final List<RowSorter.SortKey> NO_SORT_KEYS = Collections.emptyList();

    private static final RowSorter.SortKey sortKey0Asc = new RowSorter.SortKey(0, SortOrder.ASCENDING);
    private static final RowSorter.SortKey sortKey0Desc = new RowSorter.SortKey(0, SortOrder.DESCENDING);
    private static final RowSorter.SortKey sortKey0Unsorted = new RowSorter.SortKey(0, SortOrder.UNSORTED);

    private static final RowSorter.SortKey sortKey1Asc = new RowSorter.SortKey(1, SortOrder.ASCENDING);
    private static final RowSorter.SortKey sortKey1Desc = new RowSorter.SortKey(1, SortOrder.DESCENDING);
    private static final RowSorter.SortKey sortKey1Unsorted = new RowSorter.SortKey(1, SortOrder.UNSORTED);

    private static final RowSorter.SortKey sortKey2Asc = new RowSorter.SortKey(2, SortOrder.ASCENDING);
    private static final RowSorter.SortKey sortKey2Desc = new RowSorter.SortKey(2, SortOrder.DESCENDING);
    private static final RowSorter.SortKey sortKey2Unsorted = new RowSorter.SortKey(2, SortOrder.UNSORTED);

    private static final RowSorter.SortKey sortKey3Asc = new RowSorter.SortKey(3, SortOrder.ASCENDING);
    private static final RowSorter.SortKey sortKey3Desc = new RowSorter.SortKey(3, SortOrder.DESCENDING);
    private static final RowSorter.SortKey sortKey3Unsorted = new RowSorter.SortKey(3, SortOrder.UNSORTED);

    private List<RowSorter.SortKey> currentSortKeys;
    private List<RowSorter.SortKey> builtSortKeys;

    @BeforeEach
    public void setup() {
        currentSortKeys = new ArrayList<>();
        builtSortKeys = new ArrayList<>();
    }

    @Test
    public void testDefaultConstructor() {
        TreeTableColumnSortStrategy strategy = new TreeTableColumnSortStrategy();
        assertEquals(TreeTableColumnSortStrategy.ToggleNewColumnAction.ADD_TO_END, strategy.getNewColumnAction());
        assertEquals(TreeTableColumnSortStrategy.ToggleExistingColumnAction.KEEP_POSITION, strategy.getExistingColumnAction());
        assertEquals(TreeTableColumnSortStrategy.WhenColumnUnsortedAction.REMOVE, strategy.getWhenUnsortedAction());
        assertEquals(3, strategy.getMaximumSortKeys());
    }

    @Test
    public void testParameterizedConstructor() {
        TreeTableColumnSortStrategy strategy = new TreeTableColumnSortStrategy(7,
                TreeTableColumnSortStrategy.ToggleNewColumnAction.MAKE_FIRST,
                TreeTableColumnSortStrategy.ToggleExistingColumnAction.MAKE_FIRST,
                TreeTableColumnSortStrategy.WhenColumnUnsortedAction.REMOVE_SUBSEQUENT);
        assertEquals(7, strategy.getMaximumSortKeys());
        assertEquals(TreeTableColumnSortStrategy.ToggleNewColumnAction.MAKE_FIRST, strategy.getNewColumnAction());
        assertEquals(TreeTableColumnSortStrategy.ToggleExistingColumnAction.MAKE_FIRST, strategy.getExistingColumnAction());
        assertEquals(TreeTableColumnSortStrategy.WhenColumnUnsortedAction.REMOVE_SUBSEQUENT, strategy.getWhenUnsortedAction());

        assertThrows(IllegalArgumentException.class,
                ()->  new TreeTableColumnSortStrategy(3,
                        null,
                        TreeTableColumnSortStrategy.ToggleExistingColumnAction.MAKE_FIRST,
                        TreeTableColumnSortStrategy.WhenColumnUnsortedAction.REMOVE));

        assertThrows(IllegalArgumentException.class,
                ()->  new TreeTableColumnSortStrategy(3,
                        MAKE_FIRST,
                        null,
                        TreeTableColumnSortStrategy.WhenColumnUnsortedAction.REMOVE));

        assertThrows(IllegalArgumentException.class,
                ()->  new TreeTableColumnSortStrategy(3,
                        MAKE_FIRST,
                        TreeTableColumnSortStrategy.ToggleExistingColumnAction.MAKE_FIRST,
                        null));
    }

    @Test
    public void testConstructMaxSortKeys() {
        for (int i = 0; i < 10; i++) {
            TreeTableColumnSortStrategy strategy = new TreeTableColumnSortStrategy(i,
                    TreeTableColumnSortStrategy.ToggleNewColumnAction.MAKE_FIRST,
                    TreeTableColumnSortStrategy.ToggleExistingColumnAction.MAKE_FIRST,
                    TreeTableColumnSortStrategy.WhenColumnUnsortedAction.REMOVE_SUBSEQUENT);
            assertEquals(i, strategy.getMaximumSortKeys());
        }
    }

    @Test
    public void testGetSetMaxSortKeys() {
        for (int i = 0; i < 10; i++) {
            TreeTableColumnSortStrategy strategy = new TreeTableColumnSortStrategy(1000,
                    TreeTableColumnSortStrategy.ToggleNewColumnAction.MAKE_FIRST,
                    TreeTableColumnSortStrategy.ToggleExistingColumnAction.MAKE_FIRST,
                    TreeTableColumnSortStrategy.WhenColumnUnsortedAction.REMOVE_SUBSEQUENT);
            assertEquals(1000, strategy.getMaximumSortKeys());
            strategy.setMaximumSortKeys(i);
            assertEquals(i, strategy.getMaximumSortKeys());
        }
    }

    @Test
    public void testConstructAllVariants() {
        for (TreeTableColumnSortStrategy.ToggleNewColumnAction newAction : TreeTableColumnSortStrategy.ToggleNewColumnAction.values()) {
            for (TreeTableColumnSortStrategy.ToggleExistingColumnAction existingAction : TreeTableColumnSortStrategy.ToggleExistingColumnAction.values()) {
                for (TreeTableColumnSortStrategy.WhenColumnUnsortedAction unsortedAction : TreeTableColumnSortStrategy.WhenColumnUnsortedAction.values()) {
                    for (int col = 0; col < 10; col++) {
                        testConstructVariant(col, newAction, existingAction, unsortedAction);
                    }
                }
            }
        }
    }

    private void testConstructVariant(int maxColumns,
                              TreeTableColumnSortStrategy.ToggleNewColumnAction newColumn,
                              TreeTableColumnSortStrategy.ToggleExistingColumnAction existingColumn,
                              TreeTableColumnSortStrategy.WhenColumnUnsortedAction removeAction) {
        TreeTableColumnSortStrategy strategy = new TreeTableColumnSortStrategy(maxColumns, newColumn, existingColumn, removeAction);
        assertEquals(maxColumns, strategy.getMaximumSortKeys());
        assertEquals(newColumn, strategy.getNewColumnAction());
        assertEquals(existingColumn, strategy.getExistingColumnAction());
        assertEquals(removeAction, strategy.getWhenUnsortedAction());

        assertTrue(strategy.toString().contains(TreeTableColumnSortStrategy.class.getSimpleName()));
        assertTrue(strategy.toString().contains(Integer.toString(maxColumns)));
        assertTrue(strategy.toString().contains(newColumn.toString()));
        assertTrue(strategy.toString().contains(existingColumn.toString()));
        assertTrue(strategy.toString().contains(removeAction.toString()));
    }

    @Test
    public void testGetSetNewColumnActions() {
        TreeTableColumnSortStrategy strategy = new TreeTableColumnSortStrategy();

        for (TreeTableColumnSortStrategy.ToggleNewColumnAction action : TreeTableColumnSortStrategy.ToggleNewColumnAction.values()) {
            strategy.setNewColumnAction(action);
            assertEquals(action, strategy.getNewColumnAction());
        }
    }

    @Test
    public void testGetSetExistingColumnActions() {
        TreeTableColumnSortStrategy strategy = new TreeTableColumnSortStrategy();

        for (TreeTableColumnSortStrategy.ToggleExistingColumnAction action : TreeTableColumnSortStrategy.ToggleExistingColumnAction.values()) {
            strategy.setExistingColumnAction(action);
            assertEquals(action, strategy.getExistingColumnAction());
        }
    }

    @Test
    public void testGetSetWhenUnsortedColumn() {
        TreeTableColumnSortStrategy strategy = new TreeTableColumnSortStrategy();

        for (TreeTableColumnSortStrategy.WhenColumnUnsortedAction action : TreeTableColumnSortStrategy.WhenColumnUnsortedAction.values()) {
            strategy.setWhenUnsortedAction(action);
            assertEquals(action, strategy.getWhenUnsortedAction());
        }
    }

    @Test
    public void testToggleNewColumnMakeFirst() {
        final int MAX_KEYS = 10;

        TreeTableColumnSortStrategy strategy = new TreeTableColumnSortStrategy(MAX_KEYS,
                TreeTableColumnSortStrategy.ToggleNewColumnAction.MAKE_FIRST,
                TreeTableColumnSortStrategy.ToggleExistingColumnAction.MAKE_FIRST,
                TreeTableColumnSortStrategy.WhenColumnUnsortedAction.REMOVE_SUBSEQUENT);

        // Test setting a new column beyond the MAX_KEYS limit with no existing sort keys:
        // Always only have a single column if the input is an empty list of sort keys.
        for (int column = 0; column < MAX_KEYS * 2; column++) {
            builtSortKeys = strategy.buildNewSortKeys(column, NO_SORT_KEYS);
            assertEquals(1, builtSortKeys.size());
            RowSorter.SortKey expected = new RowSorter.SortKey(column, SortOrder.ASCENDING);
            assertEquals(expected, builtSortKeys.get(0)); // always first, but only one of them.
        }

        // Test setting a new column beyond max keys using the previous state as input:
        // Will add columns up to MAX_COLUMNS.
        builtSortKeys = NO_SORT_KEYS;
        for (int column = 0; column < MAX_KEYS * 2; column++) {
            builtSortKeys = strategy.buildNewSortKeys(column, builtSortKeys);
            final int expectedSize = Math.min(column + 1, MAX_KEYS);
            assertEquals(expectedSize, builtSortKeys.size());
            RowSorter.SortKey expected = new RowSorter.SortKey(column, SortOrder.ASCENDING);
            assertEquals(expected, builtSortKeys.get(0)); // always first.
        }
    }

    @Test
    public void testToggleNewColumnAddToEnd() {
        final int MAX_KEYS = 10;

        TreeTableColumnSortStrategy strategy = new TreeTableColumnSortStrategy(MAX_KEYS,
                TreeTableColumnSortStrategy.ToggleNewColumnAction.ADD_TO_END,
                TreeTableColumnSortStrategy.ToggleExistingColumnAction.MAKE_FIRST,
                TreeTableColumnSortStrategy.WhenColumnUnsortedAction.REMOVE_SUBSEQUENT);

        // Test setting a new column beyond the MAX_KEYS limit with no existing sort keys:
        // Always only have a single column if the input is an empty list of sort keys.
        for (int column = 0; column < MAX_KEYS * 2; column++) {
            builtSortKeys = strategy.buildNewSortKeys(column, NO_SORT_KEYS);
            assertEquals(1, builtSortKeys.size());
            RowSorter.SortKey expected = new RowSorter.SortKey(column, SortOrder.ASCENDING);
            assertEquals(expected, builtSortKeys.get(0)); // always last, but only one of them.
        }

        // Test setting a new column beyond max keys using the previous state as input:
        // Will add columns up to MAX_COLUMNS.
        builtSortKeys = NO_SORT_KEYS;
        for (int column = 0; column < MAX_KEYS * 2; column++) {
            builtSortKeys = strategy.buildNewSortKeys(column, builtSortKeys);
            final int expectedSize = Math.min(column + 1, MAX_KEYS);
            assertEquals(expectedSize, builtSortKeys.size());
            RowSorter.SortKey expected = new RowSorter.SortKey(column, SortOrder.ASCENDING);
            assertEquals(expected, builtSortKeys.get(expectedSize - 1)); // always last
        }
    }

    @Test
    public void testToggleExistingColumnMakeFirst() {
        final int MAX_KEYS = 10;

        TreeTableColumnSortStrategy strategy = new TreeTableColumnSortStrategy(MAX_KEYS,
                TreeTableColumnSortStrategy.ToggleNewColumnAction.MAKE_FIRST,
                TreeTableColumnSortStrategy.ToggleExistingColumnAction.MAKE_FIRST,
                TreeTableColumnSortStrategy.WhenColumnUnsortedAction.REMOVE_SUBSEQUENT);

        // Set up a list of columns:
        for (int column = 0; column < MAX_KEYS; column++) {
            currentSortKeys.add(new RowSorter.SortKey(column, SortOrder.ASCENDING));
        }

        // For each of them from the end, make them first, using a new list each time.
        for (int column = MAX_KEYS -1; column >= 0; column--) {
            builtSortKeys = new ArrayList<>(currentSortKeys); // start with a fresh list.
            RowSorter.SortKey existing = new RowSorter.SortKey(column, SortOrder.ASCENDING);
            assertEquals(existing, builtSortKeys.get(column)); // at current position, column key is ascending.
            RowSorter.SortKey expected = new RowSorter.SortKey(column, SortOrder.DESCENDING);
            builtSortKeys = strategy.buildNewSortKeys(column, builtSortKeys);
            assertEquals(MAX_KEYS, builtSortKeys.size());
            assertEquals(expected, builtSortKeys.get(0)); // it is now first and switched to descending.
        }

        // For the last one MAX_KEYS times, make it first:
        builtSortKeys = new ArrayList<>(currentSortKeys); // start with a fresh list.
        for (int column = MAX_KEYS -1; column >= 0; column--) {
            RowSorter.SortKey existing = new RowSorter.SortKey(column, SortOrder.ASCENDING);
            assertEquals(existing, builtSortKeys.get(MAX_KEYS - 1)); // at end it's ascending.
            RowSorter.SortKey expected = new RowSorter.SortKey(column, SortOrder.DESCENDING);
            builtSortKeys = strategy.buildNewSortKeys(column, builtSortKeys);
            assertEquals(MAX_KEYS, builtSortKeys.size());
            assertEquals(expected, builtSortKeys.get(0)); // it is now first and switched to descending.
        }
    }

    @Test
    public void testToggleExistingColumnKeepPosition() {
        final int MAX_KEYS = 10;

        TreeTableColumnSortStrategy strategy = new TreeTableColumnSortStrategy(MAX_KEYS,
                TreeTableColumnSortStrategy.ToggleNewColumnAction.MAKE_FIRST,
                TreeTableColumnSortStrategy.ToggleExistingColumnAction.KEEP_POSITION,
                TreeTableColumnSortStrategy.WhenColumnUnsortedAction.REMOVE_SUBSEQUENT);

        // Set up a list of columns:
        for (int column = 0; column < MAX_KEYS; column++) {
            currentSortKeys.add(new RowSorter.SortKey(column, SortOrder.ASCENDING));
        }

        // For each of them from the end, toggle them, using a new list each time.
        for (int column = MAX_KEYS -1; column >= 0; column--) {
            builtSortKeys = new ArrayList<>(currentSortKeys); // start with a fresh list.
            RowSorter.SortKey existing = new RowSorter.SortKey(column, SortOrder.ASCENDING);
            assertEquals(existing, builtSortKeys.get(column)); // at current position, column key is ascending.
            RowSorter.SortKey expected = new RowSorter.SortKey(column, SortOrder.DESCENDING);
            builtSortKeys = strategy.buildNewSortKeys(column, builtSortKeys);
            assertEquals(MAX_KEYS, builtSortKeys.size());
            assertEquals(expected, builtSortKeys.get(column)); // it is still at current position and switched to descending.
        }

        // For each one using the same list modified each time:
        builtSortKeys = new ArrayList<>(currentSortKeys); // start with a fresh list.
        for (int column = MAX_KEYS -1; column >= 0; column--) {
            RowSorter.SortKey existing = new RowSorter.SortKey(column, SortOrder.ASCENDING);
            assertEquals(existing, builtSortKeys.get(column)); // at current position it's ascending.
            RowSorter.SortKey expected = new RowSorter.SortKey(column, SortOrder.DESCENDING);
            builtSortKeys = strategy.buildNewSortKeys(column, builtSortKeys);
            assertEquals(MAX_KEYS, builtSortKeys.size());
            assertEquals(expected, builtSortKeys.get(column)); // at current position and switched to descending.
        }
    }

    @Test
    public void testRemoveOnUnsorted() {
        final int MAX_KEYS = 10;

        TreeTableColumnSortStrategy strategy = new TreeTableColumnSortStrategy(MAX_KEYS,
                TreeTableColumnSortStrategy.ToggleNewColumnAction.MAKE_FIRST,
                TreeTableColumnSortStrategy.ToggleExistingColumnAction.KEEP_POSITION,
                TreeTableColumnSortStrategy.WhenColumnUnsortedAction.REMOVE);

        // Set up a list of columns:
        for (int column = 0; column < MAX_KEYS; column++) {
            currentSortKeys.add(new RowSorter.SortKey(column, SortOrder.ASCENDING));
        }

        // For each of them toggle them to unsorted, using a new list each time.
        for (int column = 0; column < MAX_KEYS; column++) {
            builtSortKeys = new ArrayList<>(currentSortKeys); // start with a fresh list.

            RowSorter.SortKey existing = new RowSorter.SortKey(column, SortOrder.ASCENDING);
            assertEquals(existing, builtSortKeys.get(column)); // at current position, column key is ascending.

            // Toggle the ascending column twice to make it unsorted:
            builtSortKeys = strategy.buildNewSortKeys(column, builtSortKeys);
            builtSortKeys = strategy.buildNewSortKeys(column, builtSortKeys);

            // New size is one 1 less than MAX_KEYS:
            assertEquals(MAX_KEYS - 1, builtSortKeys.size()); //

            // Cannot find this sort key with any sort order in the sort keys:
            assertFalse(builtSortKeys.contains(new RowSorter.SortKey(column, SortOrder.ASCENDING)));
            assertFalse(builtSortKeys.contains(new RowSorter.SortKey(column, SortOrder.DESCENDING)));
            assertFalse(builtSortKeys.contains(new RowSorter.SortKey(column, SortOrder.UNSORTED)));
        }

        // For each one using the same list modified each time (the list will shrink as we remove).
        builtSortKeys = new ArrayList<>(currentSortKeys); // start with a fresh list.
        for (int column = 0; column < MAX_KEYS; column++) {
            RowSorter.SortKey existing = new RowSorter.SortKey(column, SortOrder.ASCENDING);
            assertEquals(existing, builtSortKeys.get(0)); // the first column is always the one we want, as we remove them successively.

            assertEquals(MAX_KEYS - column, builtSortKeys.size());

            builtSortKeys = strategy.buildNewSortKeys(column, builtSortKeys);
            builtSortKeys = strategy.buildNewSortKeys(column, builtSortKeys);

            assertEquals(MAX_KEYS - column - 1, builtSortKeys.size());
            // Cannot find this sort key with any sort order in the sort keys:
            assertFalse(builtSortKeys.contains(new RowSorter.SortKey(column, SortOrder.ASCENDING)));
            assertFalse(builtSortKeys.contains(new RowSorter.SortKey(column, SortOrder.DESCENDING)));
            assertFalse(builtSortKeys.contains(new RowSorter.SortKey(column, SortOrder.UNSORTED)));
        }

    }

    @Test
    public void testRemoveSubsequentOnUnsorted() {
        final int MAX_KEYS = 10;

        TreeTableColumnSortStrategy strategy = new TreeTableColumnSortStrategy(MAX_KEYS,
                TreeTableColumnSortStrategy.ToggleNewColumnAction.MAKE_FIRST,
                TreeTableColumnSortStrategy.ToggleExistingColumnAction.KEEP_POSITION,
                TreeTableColumnSortStrategy.WhenColumnUnsortedAction.REMOVE_SUBSEQUENT);

        // Set up a list of columns:
        for (int column = 0; column < MAX_KEYS; column++) {
            currentSortKeys.add(new RowSorter.SortKey(column, SortOrder.ASCENDING));
        }

        // For each of them toggle them to unsorted, using a new list each time.
        for (int column = 0; column < MAX_KEYS; column++) {
            builtSortKeys = new ArrayList<>(currentSortKeys); // start with a fresh list.

            RowSorter.SortKey existing = new RowSorter.SortKey(column, SortOrder.ASCENDING);
            assertEquals(existing, builtSortKeys.get(column)); // at current position, column key is ascending.

            // Toggle the ascending column twice to make it unsorted:
            builtSortKeys = strategy.buildNewSortKeys(column, builtSortKeys);
            builtSortKeys = strategy.buildNewSortKeys(column, builtSortKeys);

            // New size is number of columns to the one we made unsorted (it and all others after it are gone:
            assertEquals(column, builtSortKeys.size()); //

            // Cannot find this sort key with any sort order in the sort keys:
            for (int missingColumn = column; missingColumn < MAX_KEYS; missingColumn++) {
                assertFalse(builtSortKeys.contains(new RowSorter.SortKey(missingColumn, SortOrder.ASCENDING)));
                assertFalse(builtSortKeys.contains(new RowSorter.SortKey(missingColumn, SortOrder.DESCENDING)));
                assertFalse(builtSortKeys.contains(new RowSorter.SortKey(missingColumn, SortOrder.UNSORTED)));
            }
        }

        // Going in reverse back through the list, the list reduces in size by one each time.
        builtSortKeys = new ArrayList<>(currentSortKeys); // start with a fresh list.
        for (int column = MAX_KEYS - 1; column >= 0; column--) {
            RowSorter.SortKey existing = new RowSorter.SortKey(column, SortOrder.ASCENDING);
            assertEquals(existing, builtSortKeys.get(column));

            assertEquals( column + 1, builtSortKeys.size());

            builtSortKeys = strategy.buildNewSortKeys(column, builtSortKeys);
            builtSortKeys = strategy.buildNewSortKeys(column, builtSortKeys);

            assertEquals( column, builtSortKeys.size());

            // Cannot find this sort key with any sort order in the sort keys:
            assertFalse(builtSortKeys.contains(new RowSorter.SortKey(column, SortOrder.ASCENDING)));
            assertFalse(builtSortKeys.contains(new RowSorter.SortKey(column, SortOrder.DESCENDING)));
            assertFalse(builtSortKeys.contains(new RowSorter.SortKey(column, SortOrder.UNSORTED)));
        }

    }

    @Test
    public void testRemoveAllOnUnsorted() {
        final int MAX_KEYS = 10;

        TreeTableColumnSortStrategy strategy = new TreeTableColumnSortStrategy(MAX_KEYS,
                TreeTableColumnSortStrategy.ToggleNewColumnAction.MAKE_FIRST,
                TreeTableColumnSortStrategy.ToggleExistingColumnAction.KEEP_POSITION,
                TreeTableColumnSortStrategy.WhenColumnUnsortedAction.REMOVE_ALL);

        // Set up a list of columns:
        for (int column = 0; column < MAX_KEYS; column++) {
            currentSortKeys.add(new RowSorter.SortKey(column, SortOrder.ASCENDING));
        }

        // For each of them toggle them to unsorted, using a new list each time.
        for (int column = 0; column < MAX_KEYS; column++) {
            builtSortKeys = new ArrayList<>(currentSortKeys); // start with a fresh list.

            RowSorter.SortKey existing = new RowSorter.SortKey(column, SortOrder.ASCENDING);
            assertEquals(existing, builtSortKeys.get(column)); // at current position, column key is ascending.

            // Toggle the ascending column twice to make it unsorted:
            builtSortKeys = strategy.buildNewSortKeys(column, builtSortKeys);
            builtSortKeys = strategy.buildNewSortKeys(column, builtSortKeys);

            // No sort keys left after one is removed.
            assertEquals(0, builtSortKeys.size()); //
        }

    }

    /**
     * The TreeTableColumnSortStrategy will never put a key with SortKey.UNSORTED into its sort key list when building.
     * This test is for the circumstance when a list of sort keys containing an UNSORTED key is passed into the strategy.
     */
    @Test
    public void testUnsortedKeyBecomesAscendingWhenToggledInCaseSomeonePassesOneIn() {
        final int MAX_KEYS = 10;

        TreeTableColumnSortStrategy strategy = new TreeTableColumnSortStrategy(MAX_KEYS,
                TreeTableColumnSortStrategy.ToggleNewColumnAction.ADD_TO_END,
                TreeTableColumnSortStrategy.ToggleExistingColumnAction.KEEP_POSITION,
                TreeTableColumnSortStrategy.WhenColumnUnsortedAction.REMOVE_ALL);

        // Set up a list of columns that are unsorted:
        for (int column = 0; column < MAX_KEYS; column++) {
            currentSortKeys.add(new RowSorter.SortKey(column, SortOrder.UNSORTED));
        }

        // For each of them toggle them to ascending, using a new list each time.
        for (int column = 0; column < MAX_KEYS; column++) {
            builtSortKeys = new ArrayList<>(currentSortKeys); // start with a fresh list.

            RowSorter.SortKey existing = new RowSorter.SortKey(column, SortOrder.UNSORTED);
            assertEquals(existing, builtSortKeys.get(column)); // at current position, column key is unsorted.

            // Toggle the ascending column to make it ascending:
            builtSortKeys = strategy.buildNewSortKeys(column, builtSortKeys);

            RowSorter.SortKey toggled = new RowSorter.SortKey(column, SortOrder.ASCENDING);
            assertEquals(toggled, builtSortKeys.get(column)); // at current position, column key is ascending.
        }
    }


}