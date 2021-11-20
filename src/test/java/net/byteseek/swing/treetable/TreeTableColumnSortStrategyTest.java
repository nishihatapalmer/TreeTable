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
        TreeTableColumnSortStrategy strategy = new TreeTableColumnSortStrategy(7,
                TreeTableColumnSortStrategy.ToggleNewColumnAction.MAKE_FIRST,
                TreeTableColumnSortStrategy.ToggleExistingColumnAction.MAKE_FIRST,
                TreeTableColumnSortStrategy.WhenColumnUnsortedAction.REMOVE_SUBSEQUENT);

        builtSortKeys = strategy.buildNewSortKeys(0, NO_SORT_KEYS);
        assertEquals(1, builtSortKeys.size());
        assertEquals(sortKey0Asc, builtSortKeys.get(0));




    }


}