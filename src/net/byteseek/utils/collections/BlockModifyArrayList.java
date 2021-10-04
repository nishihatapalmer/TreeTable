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
package net.byteseek.utils.collections;

import java.util.*;

/**
 * A List which provides efficient block operations for insert and remove of objects backed by an array.
 * It is almost identical to ArrayList, except the block operations don't have to call insert() or remove()
 * individually for each object inserted or removed (which causes an ArrayList to shift all the other values up or
 * down one each time).  So for block insert and remove, ArrayList is O(nm), whereas BlockModifyArrayList is O(n+m).
 */
public class BlockModifyArrayList<E> extends AbstractList<E> {

    private static final int DEFAULT_CAPACITY = 256;

    private int growMultiplierPercent = 200;
    private E[] elements = (E[]) new Object[DEFAULT_CAPACITY];
    private int size;

    @Override
    public int size() {
        return size;
    }

    @Override
    public E get(final int index) {
        checkIndex(index);
        return elements[index];
    }

    @Override
    public boolean add(final E element) {
        checkResize(1);
        elements[size++] = element;
        return true;
    }

    @Override
    public void add(final int index, final E element) {
        if (index == size) {
            add(element);
        } else {
            checkIndex(index);
            checkResize(1);
            // Shift the others along one:
            for (int position = size - 1; position >= index; position--) {
                elements[position + 1] = elements[position];
            }
            // insert the new element:
            elements[index] = element;
            size++;
        }
    }

    /**
     * Adds a list of objects to the list.
     *
     * @param elements The elements to add.
     */
    public void addAll(final List<? extends E> elements) {
        final int numToAdd = elements.size();
        checkResize(numToAdd);
        for (int elementIndex = 0; elementIndex < numToAdd; elementIndex++) {
            this.elements[size++] = elements.get(elementIndex);
        }
    }

    /**
     * Inserts a list of elements at the given index.
     *
     * @param elements the list of elements to insert.
     * @param index the index to insert tham at.
     */
    public void addAll(final int index, final List<? extends E> elements) {
        if (index == size) {
            addAll(elements);
        } else {
            checkIndex(index);
            final int numToAdd = elements.size();
            checkResize(numToAdd);
            final int numToShift = size - index;
            final int newPosition = index + numToAdd;
            // move the existing elements up to create a gap
            for (int position = numToShift - 1; position >= 0; position--) {
                this.elements[newPosition + position] = this.elements[index + position];
            }

            // insert the new elements in the gap.
            for (int elementIndex = 0; elementIndex < numToAdd; elementIndex++) {
                this.elements[index + elementIndex] = elements.get(elementIndex);
            }
            size += numToAdd;
        }
    }

    @Override
    public boolean addAll(final int index, final Collection<? extends E> c) {
        final int numToAdd = c.size();
        checkResize(numToAdd);
        final int numToShift = size - index;
        final int newPosition = index + numToAdd;
        // move the existing elements up to create a gap
        for (int position = numToShift - 1; position >= 0; position--) {
            this.elements[newPosition + position] = this.elements[index + position];
        }
        // Insert the new elements
        int insertPos = index;
        for (E element : c) {
            this.elements[insertPos++] = element;
        }
        size += numToAdd;
        return true;
    }

    @Override
    public E set(final int index, final E element) {
        final E previousValue = elements[index];
        elements[index] = element;
        return previousValue;
    }

    @Override
    public E remove(final int index) {
        checkIndex(index);
        final E elementToRemove = elements[index];
        for (int position = index; position < size - 1; position++) {
            elements[position] = elements[position + 1];
        }
        size--;
        return elementToRemove;
    }


    //TODO: examine use of *inclusive* from and to.  most other interfaces use exclusive to.
    /**
     * Removes a block of elements between the from and to index (inclusive).
     * @param from the first index of the block to remove.
     * @param to the last index of the block to remove (inclusive).
     */
    public void remove(final int from, final int to) {
        checkIndex(from);
        checkFromTo(from, to);
        if (to >= size) { // If we're removing everything up to or past the end, just set the size down.
            size = from;
        } else { // got some stuff at the end we have to move over to cover the gap:
            final int rowAfterRemoved = to + 1;
            final int numToRemove = rowAfterRemoved - from;
            final int numToMove = size - rowAfterRemoved;
            for (int position = 0; position < numToMove; position++) {
                elements[from + position] = elements[rowAfterRemoved + position];
            }
            size -= numToRemove;
        }
    }

    public void replace(final int from, final int to, final List<? extends E> newValues) {
        final int numNewValues = newValues.size();
        moveElementsForReplace(from, to, numNewValues);
        int listIndex = 0;
        for (int position = from; position < from + numNewValues; position++) {
            elements[position] = newValues.get(listIndex++);
        }
    }

    public void replace(final int from, final int to, final Enumeration<? extends E> newValues, final int numNewValues) {
        moveElementsForReplace(from, to, numNewValues);
        for (int position = from; position < from + numNewValues; position++) {
            elements[position] = newValues.nextElement();
        }
    }

    //TODO: what about zero width insertion (from = to)?
    //TODO: test!!!
    private void moveElementsForReplace(final int from, final int to, final int numNewValues) {
        checkFromTo(from, to);
        final int safeTo = to < size ? to : size - 1;
        final int numExistingValues = safeTo - from + 1;
        final int delta = numNewValues - numExistingValues;
        if (delta > 0) { // more new values than already exist - shift the elements after to over to make room.
            checkResize(delta);
            final int startPos = size - 1 + delta;
            final int endPos = safeTo + delta;
            for (int position = startPos; position >= endPos; position--) {
                elements[position] = elements[position - delta];
            }
        } else if (delta < 0) { // fewer new values than existing values - shift the elements after to over to fill the gap.
            final int startPos = safeTo + delta; // delta is negative.
            final int endPos = size - 1 + delta;
            for (int position = startPos; position <= endPos; position++) {
                elements[position] = elements[position - delta]; // delta is negative.
            }
        }
        size += delta;
    }


    @Override
    public int indexOf(Object o) {
        if (o == null) {
            for (int i = 0; i < size; i++) {
                if (elements[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i < size; i++) {
                if (elements[i].equals(o)) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        if (o == null) {
            for (int i = size - 1; i >=0; i--) {
                if (elements[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = size - 1; i >=0; i--) {
                if (elements[i].equals(o)) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public void clear() {
        size = 0;
    }

    private void checkFromTo(int from, int to) {
        if (to < from) {
            throw new IllegalArgumentException("to:" + to + " cannot be less than from:" + from);
        }
    }

    /**
     * Checks the index is within bounds.
     * @param index the index to check.
     */
    private void checkIndex(final int index) {
        if (index >= size || index < 0) {
            throw new IndexOutOfBoundsException("Index = " + index + " size = " + size);
        }
    }

    /**
     * Checks whether the backing array needs to resise if we add more elements.
     * @param numToAdd then number of elements to add.
     */
    private void checkResize(final int numToAdd) {
        if (size + numToAdd >= elements.length) {
            growArray();
        }
    }

    /**
     * Grows the array up to the maximum array size.
     */
    private void growArray() {
        if (elements.length == Integer.MAX_VALUE) {
            throw new OutOfMemoryError("Cannot increase the list size beyond Integer.MAX_VALUE: " + this);
        }
        E[] newArray = (E[]) new Object[getGrowSize()];
        System.arraycopy(elements, 0, newArray, 0, elements.length);
        elements = newArray;
    }

    /**
     * Returns the amount to grow to.  Grows faster initially, then slows down growth as we get a lot larger.
     * @return A new size for the array.
     */
    private int getGrowSize() {
        // We grow by a multiplier that gradually reduces.
        // This is because we want to grow faster when we're smaller (to avoid frequent re-sizes if large
        // numbers of elements are being added), but to grow proportionally a bit slower once we're getting quite big
        // (or we will waste a lot of space on average).
        long newSize = elements.length * growMultiplierPercent / 100;
        if (growMultiplierPercent > 120) {
            growMultiplierPercent -= 17; // the next time we grow, it will be by a smaller proportion (more absolute).
        }
        if (newSize > Integer.MAX_VALUE) {
            newSize = Integer.MAX_VALUE;
        }
        return (int) newSize;
    }

}
