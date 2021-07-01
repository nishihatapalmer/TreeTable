package net.byteseek.swing.treetable;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;

class NodeDisplayList extends AbstractList<TreeTableNode> {

    private TreeTableNode[] displayedNodes = new TreeTableNode[128];
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
            // Shift the others along one by the number of nodes:
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
        Arrays.fill(displayedNodes, null); // clear all references to any nodes.
        size = 0;
    }

    private void checkIndex(int index) {
        if (index >= size || index < 0) {
            throw new IndexOutOfBoundsException("Index = " + index + " size = " + size);
        }
    }

    private void checkResize(int numToAdd) {
        if (size + numToAdd >= displayedNodes.length) {
            resizeArray();
        }
    }

    private void resizeArray() {
        TreeTableNode[] newArray = new TreeTableNode[displayedNodes.length + 128];
        System.arraycopy(displayedNodes, 0, newArray, 0, displayedNodes.length);
        displayedNodes = newArray;
    }

}
