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

import java.util.List;
import java.util.Iterator;

import javax.swing.tree.DefaultMutableTreeNode;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

//TODO: finish

class TreeUtilsTest extends BaseTestClass {

    @Test
    void buildTree() {
    }

    @Test
    void buildTree_withIterator_createsCorrectTree() {
        // Define test data
        List<TreeUtils.TreeTableRow> rows = List.of(
                new TreeUtils.TreeTableRow( "1", null, new Object[]{"Root"}),
                new TreeUtils.TreeTableRow( "2", "1", new Object[]{"Child1"}),
                new TreeUtils.TreeTableRow( "3", "1", new Object[]{"Child2"}),
                new TreeUtils.TreeTableRow( "4", "2", new Object[]{"GrandChild1"}) );
        Iterator<TreeUtils.TreeTableRow> iterator = rows.iterator();

        // Build the tree
        DefaultMutableTreeNode rootNode = TreeUtils.buildTree(iterator);

        // Verify root
        assertEquals(1, rootNode.getChildCount());
        DefaultMutableTreeNode child1Node = (DefaultMutableTreeNode) rootNode.getChildAt(0);

        // Verify Root node
        assertArrayEquals(new Object[]{"Root"}, (Object[]) child1Node.getUserObject());

        // Verify first level children
        assertEquals(2, child1Node.getChildCount());
        DefaultMutableTreeNode child1 = (DefaultMutableTreeNode) child1Node.getChildAt(0);
        DefaultMutableTreeNode child2 = (DefaultMutableTreeNode) child1Node.getChildAt(1);

        assertArrayEquals(new Object[]{"Child1"}, (Object[]) child1.getUserObject());
        assertArrayEquals(new Object[]{"Child2"}, (Object[]) child2.getUserObject());

        // Verify second level children
        assertEquals(1, child1.getChildCount());
        DefaultMutableTreeNode grandChild1 = (DefaultMutableTreeNode) child1.getChildAt(0);
        assertArrayEquals(new Object[]{"GrandChild1"}, (Object[]) grandChild1.getUserObject());
    }

    @Test
    void getNodeList() {
    }

    @Test
    void getChildren() {
    }

    @Test
    void testGetChildren() {
    }

    @Test
    void getObject() {
    }

    @Test
    void getLevel() {
    }

    @Test
    void getAncestor() {
    }

    @Test
    void buildTextTree() {
    }

    @Test
    void walk() {
    }

    @Test
    void testWalk() {
    }

    @Test
    void findLastConsecutiveIndex() {
    }
}