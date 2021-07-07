package net.byteseek.swing.treetable;

import java.util.AbstractList;
import java.util.List;

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
            //TODO: bug - if list is one, will overwrite data,  Should still run this in reverse.
            // Shift the others along by numToAdd to leave room.
            for (int position = 0; position < numToShift; position++) {
                displayedNodes[newPosition + position] = displayedNodes[index + position];
            }
            // insert the new nodes:
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
            throw new IllegalArgumentException("to:" + to + " must be greater than from:" + from);
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
