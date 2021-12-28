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
import javax.swing.tree.TreeNode;
import static net.byteseek.swing.treetable.TreeNodeComparator.EQUAL_VALUE;
import static net.byteseek.swing.treetable.TreeNodeComparator.GREATER_THAN;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TreeNodeComparatorTest  extends BaseTestClass {

    public static final int LESS_THAN = -1;
    private TreeNodeComparator comparator;

    @BeforeEach
    public void setupComparator() {
        model.expandTree();
        model.bindTable(table);
        comparator = new TreeNodeComparator(model);
    }

    /**
     * Comparison of null nodes is NOT SUPPORTED and may throw errors or show inconsistent behaviour.
     *
     * Comparing null nodes currently produces a null pointer exception if any sorting is defined.
     * If sorting isn't defined, it will always return equal no matter what, even if some of them are null.
     *
     * This is by design; when this is used by the RowSorter, the nodes are known
     * not to be null when we compare them, so any null checks are redundant.
     * This specific behaviour should not be relied upon to continue - but it is how it currently behaves.
     */
    @Test
    public void testCompareNullNodesNullPointerException() {
        // A Sort Key set will cause a NPE with null nodes.
        model.setSortKeys(sortKey1);
        assertThrows(NullPointerException.class,
                ()-> comparator.compare(null, rootNode));
        assertThrows(NullPointerException.class,
                ()-> comparator.compare(rootNode, null));
        assertThrows(NullPointerException.class,
                ()-> comparator.compare(null, null));

        // Not sorting - always returns equal for everything.
        model.clearSortKeys();
        testAlwaysEqualIfNotSorting();

        // A grouping comparator set will cause a NPE with null nodes.
        model.setGroupingComparator(Comparators.HAS_CHILDREN);
        assertThrows(NullPointerException.class,
                ()-> comparator.compare(null, rootNode));
        assertThrows(NullPointerException.class,
                ()-> comparator.compare(rootNode, null));
        assertThrows(NullPointerException.class,
                ()-> comparator.compare(null, null));

        // Not sorting - always returns equal for everything.
        model.clearGroupingComparator();
        testAlwaysEqualIfNotSorting();
    }

    /**
     * The TreeNodeComparator does not have a way to break ties if all
     * comparisons are equal, or if no sorting is defined.  It always
     * returns EQUAL_VALUE (0) if there is no sort to be performed.
     */
    @Test
    public void testAlwaysEqualIfNotSorting() {
        assertNodesCompareEqual(rootNode, child1);
        assertNodesCompareEqual(child1, child2);
        assertNodesCompareEqual(child2, child0);
        assertNodesCompareEqual(child1, child0);
        assertNodesCompareEqual(child0, rootNode);
        assertNodesCompareEqual(child0, subchild0);
        assertNodesCompareEqual(rootNode, subchild1);
    }

    private void assertNodesCompareEqual(final TreeNode node1, final TreeNode node2) {
        assertEquals(0, comparator.compare(node1, node1));
        assertEquals(0, comparator.compare(node2, node2));
        assertEquals(0, comparator.compare(node1, node2));
        assertEquals(0, comparator.compare(node2, node1));
    }

    @Test
    public void testCompareWithColumnComparator() {
        // column 2 in sortkey3 uses a custom comparator in the test tree model defined in the baseclass.
        // the test comparator just compares all boolean values to EQUAL.
        model.setSortKeys(sortKey3);
        assertEquals(0, comparator.compare(rootNode, rootNode));
        assertEquals(0, comparator.compare(rootNode, child0));
        assertEquals(0, comparator.compare(rootNode, child1));
        assertEquals(0, comparator.compare(rootNode, child2));
        assertEquals(0, comparator.compare(rootNode, subchild0));
        assertEquals(0, comparator.compare(rootNode, subchild1));
        assertEquals(0, comparator.compare(rootNode, subchild2));
        assertEquals(0, comparator.compare(child1, child1));
        assertEquals(0, comparator.compare(child1, subchild0));
        assertEquals(0, comparator.compare(subchild2, child2));
        assertEquals(0, comparator.compare(child2, subchild0));
    }

    @Test
    public void testCompareWithComparableValues() {
        model.setSortKeys(sortKey2);
        assertEquals(EQUAL_VALUE, comparator.compare(rootNode, rootNode));
        assertEquals(EQUAL_VALUE, comparator.compare(child1, child1));

        assertTrue(comparator.compare(rootNode, child0)     < 0);
        assertTrue(comparator.compare(rootNode, child1)     < 0);
        assertTrue(comparator.compare(rootNode, child2)     < 0);
        assertTrue(comparator.compare(rootNode, subchild0)  > 0);
        assertTrue(comparator.compare(rootNode, subchild1)  > 0);
        assertTrue(comparator.compare(rootNode, subchild2)  > 0);
        assertTrue(comparator.compare(child1, subchild0)    > 0);
        assertTrue(comparator.compare(subchild2, child2)    < 0);
        assertTrue(comparator.compare(child2, subchild0)    > 0);
        assertTrue(comparator.compare(child1, rootNode)     > 0);
    }

    @Test
    public void testFallBackToStringCompare() {
        // column 3 returns the test object which isn't comparable.
        model.setSortKeys(new RowSorter.SortKey(3, SortOrder.DESCENDING));

        assertTrue(comparator.compare(rootNode, child0)     < 0);
        assertTrue(comparator.compare(rootNode, child1)     < 0);
        assertTrue(comparator.compare(rootNode, child2)     < 0);
        assertTrue(comparator.compare(rootNode, subchild0)  > 0);
        assertTrue(comparator.compare(rootNode, subchild1)  > 0);
        assertTrue(comparator.compare(rootNode, subchild2)  > 0);
        assertTrue(comparator.compare(child1, subchild0)    > 0);
        assertTrue(comparator.compare(subchild2, child2)    < 0);
        assertTrue(comparator.compare(child2, subchild0)    > 0);
        assertTrue(comparator.compare(child1, rootNode)     > 0);
    }

    @Test
    public void testCompareNullValues() {
        model.setSortKeys(new RowSorter.SortKey(4, SortOrder.ASCENDING));
        assertEquals(EQUAL_VALUE, comparator.compare(rootNode, child1)); // both have null column 4.
        assertEquals(LESS_THAN, comparator.compare(rootNode, child0)); // null is less than child0 (not null)
        assertEquals(GREATER_THAN, comparator.compare( child0, rootNode)); // not null is greater than null.
    }

    @Test
    public void testUnsortedKeysIgnored() {
        List<RowSorter.SortKey> keys = new ArrayList<>();
        keys.add(sortKey2);
        keys.add(sortKey3);
        model.setSortKeys(keys);
        List<TreeNode> notUnsortedKeys = buildTableNodeList();

        keys.add(1, new RowSorter.SortKey(1, SortOrder.UNSORTED));
        List<TreeNode> withUnsortedKeys = buildTableNodeList();

        assertEquals(notUnsortedKeys, withUnsortedKeys);
    }

    private List<TreeNode> buildTableNodeList() {
        List<TreeNode> nodes = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            nodes.add(model.getNodeAtTableRow(i));
        }
        return nodes;
    }


}