package net.byteseek.swing.treetable;

import java.util.AbstractList;
import java.util.List;

class NodeDisplayList extends AbstractList<TreeTableNode> {

    private static final int DEFAULT_CAPACITY = 256;
    
    private int growMultiplierPercent = 200;
    private TreeTableNode[] displayedNodes = new TreeTableNode[DEFAULT_CAPACITY];
    private int size;

    public int size() {
        return size;
    }
    
    public TreeTableNode get(int index) {
        checkIndex(index);
        return displayedNodes[index];
    }

    public boolean add(final TreeTableNode node) {
        checkResize(1);
        displayedNodes[size++] = node;
        return true;
    }

    public boolean add(List<TreeTableNode> nodes) {
        final int numToAdd = nodes.size();
        checkResize(numToAdd);
        for (int nodeIndex = 0; nodeIndex < numToAdd; nodeIndex++) {
            displayedNodes[size++] = nodes.get(nodeIndex);
        }
        return true;
    }

    public void insert(TreeTableNode node, int index) {
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

    public void insert(List<TreeTableNode> nodes, int index) {
        if (index == size) {
            add(nodes);
        } else {
            checkIndex(index);
            final int numToAdd = nodes.size();
            checkResize(numToAdd);
            final int numToShift = size - index;
            // Shift the others along by the number of nodes:
            for (int position = 0; position < numToShift; position++) {
                displayedNodes[size + position] = displayedNodes[index + position];
            }
            // insert the new nodes:
            for (int nodeIndex = 0; nodeIndex < numToAdd; nodeIndex++) {
                displayedNodes[index + nodeIndex] = nodes.get(nodeIndex);
            }
            size += numToAdd;
        }
    }

    public TreeTableNode remove(int index) {
        checkIndex(index);
        TreeTableNode nodeToRemove = displayedNodes[index];
        for (int position = index; position < size - 1; position++) {
            displayedNodes[position] = displayedNodes[position + 1];
        }
        size--;
        return nodeToRemove;
    }

    public void remove(int from, int to) {
        checkIndex(from);
        if (to <= from) {
            throw new IllegalArgumentException("to:" + to + " must be greater than from:" + from);
        }
        if (to >= size) { // If we're removing everything up to or past the end, just set the size down.
            size = from;
        } else { // got some stuff at the end we have to move over to cover the gap:
            final int numToRemove = to - from;
            final int remainingLength = size - to;
            for (int position = 0; position < remainingLength; position++) {
                displayedNodes[from + position] = displayedNodes[to + position];
            }
            size -= numToRemove;
        }
    }

    public void clear() {
        size = 0;
    }

    private void checkIndex(int index) {
        if (index >= size || index < 0) {
            throw new IndexOutOfBoundsException("Index = " + index + " size = " + size);
        }
    }

    private void checkResize(int numToAdd) {
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
