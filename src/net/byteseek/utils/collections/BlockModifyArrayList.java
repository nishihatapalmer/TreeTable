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

import java.util.AbstractList;
import java.util.List;

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

    /**
     * Adds a list of objects to the list.
     *
     * @param elements The elements to add.
     */
    public void add(final List<E> elements) {
        final int numToAdd = elements.size();
        checkResize(numToAdd);
        for (int elementIndex = 0; elementIndex < numToAdd; elementIndex++) {
            this.elements[size++] = elements.get(elementIndex);
        }
    }

    /**
     * Inserts an element into the list at the given index.
     * @param element The element to add.
     * @param index The index to add the element at.
     */
    public void insert(final E element, final int index) {
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
     * Inserts a list of elements at the given index.
     *
     * @param elements the list of elements to insert.
     * @param index the index to insert tham at.
     */
    public void insert(final List<E> elements, final int index) {
        if (index == size) {
            add(elements);
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
    public E remove(final int index) {
        checkIndex(index);
        final E elementToRemove = elements[index];
        for (int position = index; position < size - 1; position++) {
            elements[position] = elements[position + 1];
        }
        size--;
        return elementToRemove;
    }

    /**
     * Removes a block of elements between the from and to index (inclusive).
     * @param from the first index of the block to remove.
     * @param to the last index of the block to remove (inclusive).
     */
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
                elements[from + position] = elements[rowAfterRemoved + position];
            }
            size -= numToRemove;
        }
    }

    @Override
    public void clear() {
        size = 0;
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
