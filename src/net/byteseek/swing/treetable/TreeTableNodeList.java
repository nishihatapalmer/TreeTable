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

import java.util.AbstractList;
import java.util.List;

/**
 * A List which provides efficient block operations for insert and remove, backed by an array.
 * It is almost identical to ArrayList, except the block operations don't have to call insert() or remove()
 * individually for each row inserted or removed (which causes an ArrayList to shift all the other values up or
 * down one each time).  So for block insert and remove, ArrayList is O(nm), whereas TreeTableNodeList is O(n+m).
 */
class TreeTableNodeList extends AbstractList<TreeTableNode> {

    private static final int DEFAULT_CAPACITY = 256;
    
    private int growMultiplierPercent = 200;
    private TreeTableNode[] displayedNodes = new TreeTableNode[DEFAULT_CAPACITY];
    private int size;

    @Override
    public int size() {
        return size;
    }

    @Override
    public TreeTableNode get(final int index) {
        checkIndex(index);
        return displayedNodes[index];
    }

    @Override
    public boolean add(final TreeTableNode node) {
        checkResize(1);
        displayedNodes[size++] = node;
        return true;
    }

    public boolean add(final List<TreeTableNode> nodes) {
        final int numToAdd = nodes.size();
        checkResize(numToAdd);
        for (int nodeIndex = 0; nodeIndex < numToAdd; nodeIndex++) {
            displayedNodes[size++] = nodes.get(nodeIndex);
        }
        return true;
    }

    public void insert(final TreeTableNode node, final int index) {
        if (index == size) {
            add(node);
        } else {
            checkIndex(index);
            checkResize(1);
            // Shift the others along one:
            for (int position = size - 1; position >= index; position--) {
                displayedNodes[position + 1] = displayedNodes[position];
            }
            // insert the new node:
            displayedNodes[index] = node;
            size++;
        }
    }

    //TODO: Insert is called on view index, not model index when sorted.
    public void insert(final List<TreeTableNode> nodes, final int index) {
        if (index == size) {
            add(nodes);
        } else {
            checkIndex(index);
            final int numToAdd = nodes.size();
            checkResize(numToAdd);
            final int numToShift = size - index;
            final int newPosition = index + numToAdd;
            // move the existing nodes up to create a gap
            for (int position = numToShift - 1; position >= 0; position--) {
                displayedNodes[newPosition + position] = displayedNodes[index + position];
            }

            // insert the new nodes in the gap.
            for (int nodeIndex = 0; nodeIndex < numToAdd; nodeIndex++) {
                displayedNodes[index + nodeIndex] = nodes.get(nodeIndex);
            }
            size += numToAdd;
        }
    }

    @Override
    public TreeTableNode remove(final int index) {
        checkIndex(index);
        TreeTableNode nodeToRemove = displayedNodes[index];
        for (int position = index; position < size - 1; position++) {
            displayedNodes[position] = displayedNodes[position + 1];
        }
        size--;
        return nodeToRemove;
    }

    public void remove(final int from, final int to) {
        checkIndex(from);
        if (to < from) {
            throw new IllegalArgumentException("to:" + to + " cannot be less than from:" + from);
        }
        if (to >= size) { // If we're removing everything up to or past the end, just set the size down.
            size = from;
        } else { // got some stuff at the end we have to move over to cover the gap:
            final int rowAfterRemoved = to + 1;
            final int numToRemove = rowAfterRemoved - from;
            final int numToMove = size - rowAfterRemoved;
            for (int position = 0; position < numToMove; position++) {
                displayedNodes[from + position] = displayedNodes[rowAfterRemoved + position];
            }
            size -= numToRemove;
        }
    }

    @Override
    public void clear() {
        size = 0;
    }

    private void checkIndex(final int index) {
        if (index >= size || index < 0) {
            throw new IndexOutOfBoundsException("Index = " + index + " size = " + size);
        }
    }

    private void checkResize(final int numToAdd) {
        if (size + numToAdd >= displayedNodes.length) {
            growArray();
        }
    }

    private void growArray() {
        if (displayedNodes.length == Integer.MAX_VALUE) {
            throw new OutOfMemoryError("Cannot increase the list size beyond Integer.MAX_VALUE: " + this);
        }
        TreeTableNode[] newArray = new TreeTableNode[getGrowSize()];
        System.arraycopy(displayedNodes, 0, newArray, 0, displayedNodes.length);
        displayedNodes = newArray;
    }
    
    private int getGrowSize() {
        // We grow by a multiplier that gradually reduces.
        // This is because we want to grow faster when we're smaller (to avoid frequent re-sizes if large
        // numbers of nodes are being added), but to grow proportionally a bit slower once we're getting quite big
        // (or we will waste a lot of space on average).
        long newSize = displayedNodes.length * growMultiplierPercent / 100;
        if (growMultiplierPercent > 120) {
            growMultiplierPercent -= 17; // the next time we grow, it will be by a smaller proportion (more absolute).
        }
        if (newSize > Integer.MAX_VALUE) {
            newSize = Integer.MAX_VALUE;
        }
        return (int) newSize;
    }

}
