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
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class TreeTableRowSorterTest  extends BaseTestClass  {

    @Test
    void getModel() {
    }

    @Test
    void toggleSortOrder() {
    }

    @Test
    void convertRowIndexToModel() {
    }

    @Test
    void convertRowIndexToView() {
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


}