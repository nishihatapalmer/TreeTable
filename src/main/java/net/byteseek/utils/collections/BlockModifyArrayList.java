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

    private float growMultiplier = 1.25f;
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
            if (size - index >= 0) {
                System.arraycopy(elements, index, elements, index + 1, size - index);
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
        final E[] localElements = this.elements;
        for (int elementIndex = 0; elementIndex < numToAdd; elementIndex++) {
            localElements[size++] = elements.get(elementIndex);
        }
    }

    /**
     * Inserts a list of elements at the given index.
     *
     * @param elements the list of elements to insert.
     * @param index the index to insert them at.
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
            final E[] localElements = this.elements;

            // move the existing elements up to create a gap
            if (numToShift >= 0) {
                System.arraycopy(localElements, index, localElements, newPosition, numToShift);
            }
            // insert the new elements in the gap.
            for (int elementIndex = 0; elementIndex < numToAdd; elementIndex++) {
                localElements[index + elementIndex] = elements.get(elementIndex);
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
        final E[] localElements = elements;

        // move the existing elements up to create a gap
        if (numToShift - 1 + 1 >= 0) { //TODO: this calculation is clearly not optimised!   Looks like arrived at by trial and error.  Fix.
            System.arraycopy(localElements, index, localElements, newPosition, numToShift - 1 + 1);
        }
        // Insert the new elements
        int insertPos = index;
        for (E element : c) {
            localElements[insertPos++] = element;
        }
        size += numToAdd;
        return true;
    }

    @Override
    public E set(final int index, final E element) {
        checkIndex(index);
        final E previousValue = elements[index];
        elements[index] = element;
        return previousValue;
    }

    @Override
    public E remove(final int index) {
        checkIndex(index);
        final E elementToRemove = elements[index];
        if (size - 1 - index >= 0) {
            System.arraycopy(elements, index + 1, elements, index, size - 1 - index);
        }
        size--;
        elements[size] = null; // null out old object left dangling on the end.
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
        checkToNotSmallerThanFrom(from, to);
        if (to >= size -1) { // If we're removing everything up to or past the end, just set the size down.
            Arrays.fill(elements, from, size, null);
            size = from;
        } else { // got some stuff at the end we have to move over to cover the gap:
            final int rowAfterRemoved = to + 1;
            final int numToRemove = rowAfterRemoved - from;
            final int numToMove = size - rowAfterRemoved;
            if (numToMove >= 0) {
                System.arraycopy(elements, rowAfterRemoved, elements, from, numToMove);
            }
            Arrays.fill(elements, from + numToMove, size, null); // null out dangling objects.
            size -= numToRemove;
        }
    }

    /**
     * Replaces a range of elements with a different list of values.
     * The range and list do not have to be the same size.
     * If the range is bigger than the replacing list, then the rest of the array will be moved down to close the gap.
     * If the range is smaller than the replacing list, then the rest of the array will be moved up to make room.
     *
     * @param from The starting index to replace elements
     * @param to The ending index to replace elements (must be at least from).
     * @param newValues A list of new values to overwrite the range with.
     */
    public void replace(final int from, final int to, final List<? extends E> newValues) {
        checkIndex(from);
        checkToNotSmallerThanFrom(from, to);
        final int numNewValues = newValues.size();
        moveElementsForReplace(from, to, numNewValues);
        int listIndex = 0;
        final E[] localElements = elements;
        for (int position = from; position < from + numNewValues; position++) {
            localElements[position] = newValues.get(listIndex++);
        }
    }

    /**
     * Replaces a range of elements in the list with values from an Enumeration.
     * The range to replace and the number of new elements provided do not need to be the same size.
     * If the range is larger than the number of new values, the remaining elements will be shifted down to close the gap.
     * If the range is smaller, elements will be shifted up to make space for the new values.
     *
     * @param from The starting index of the range to replace (inclusive).
     * @param to The ending index of the range to replace (inclusive).
     * @param newValues An Enumeration providing the new values to overwrite the range.
     * @param numNewValues The number of new values to insert into the range.
     *                     The Enumeration must have at least this number of elements.
     */
    public void replace(final int from, final int to, final Enumeration<? extends E> newValues, final int numNewValues) {
        checkIndex(from);
        checkToNotSmallerThanFrom(from, to);
        moveElementsForReplace(from, to, numNewValues);
        final E[] localElements = elements;
        for (int position = from; position < from + numNewValues; position++) {
            localElements[position] = newValues.nextElement();
        }
    }

    private void moveElementsForReplace(final int from, final int to, final int numNewValues) {
        final E[] localElements = elements;
        final int safeTo = to < size ? to : size - 1;
        final int numExistingValues = safeTo - from + 1;
        final int delta = numNewValues - numExistingValues;
        if (delta > 0) { // more new values than already exist - ensure we have space to add them and move elements over if needed.
            checkResize(delta);
            if (safeTo < size - 1) { // If there are any elements after the range which are not being replaced, move them over.
                System.arraycopy(localElements, safeTo + 1, localElements, safeTo + delta + 1, size - safeTo - 1);
            }
        } else if (delta < 0) { // fewer new values than existing values - shift the elements after to over to fill the gap and null dangling ones.
            // Note: delta is negative here, so we add it in order to perform subtraction.
            if (safeTo < size - 1) { // If there are any elements after the range which are not being replaced, move them over.
                System.arraycopy(localElements, safeTo + 1 + delta, localElements, safeTo + 1, size - safeTo - 1);
            }
            Arrays.fill(localElements, size + delta, size, null); // null old dangling objects on end
        }
        size += delta;
    }

    @Override
    public int indexOf(Object o) {
        final E[] localElements = elements;
        final int localSize = size;
        if (o == null) {
            for (int i = 0; i < localSize; i++) {
                if (localElements[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i < localSize; i++) {
                if (localElements[i].equals(o)) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        final E[] localElements = elements;
        final int localSize = size;
        if (o == null) {
            for (int i = localSize - 1; i >=0; i--) {
                if (localElements[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = localSize - 1; i >=0; i--) {
                if (localElements[i].equals(o)) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public void clear() {
        Arrays.fill(elements, 0, size, null);
        size = 0;
    }

    private void checkToNotSmallerThanFrom(int from, int to) {
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
     * Checks whether the backing array needs to resize if we add more elements.
     * @param numToAdd then number of elements to add.
     */
    private void checkResize(final int numToAdd) {
        if (size >= elements.length - numToAdd) {  // calculate subtracting from elements.length to avoid any integer overflow when size is large.
            growArray(numToAdd);
        }
    }

    /**
     * Grows the array up to the maximum array size.
     */
    private void growArray(final int numToAdd) {
        if (elements.length == Integer.MAX_VALUE - 1) {
            throw new IllegalArgumentException("Cannot increase the list size beyond Integer.MAX_VALUE: " + this);
        }
        final E[] newArray = (E[]) new Object[getGrowSize(numToAdd)];
        System.arraycopy(elements, 0, newArray, 0, elements.length);
        elements = newArray;
    }

    /**
     * Returns the amount to grow to.  Grows faster initially, then slows down growth as we get a lot larger.
     * @return A new size for the array.
     */
    private int getGrowSize(final int numToAdd) {
        // We grow by a multiplier that gradually reduces.
        // This is because we want to grow faster when we're smaller (to avoid frequent re-sizes if large
        // numbers of elements are being added), but to grow proportionally a bit slower once we're getting quite big
        // (or we will waste a lot of space on average).
        long newSize = (long) ((elements.length + numToAdd) * growMultiplier);
        if (growMultiplier > 1.05f) {
            growMultiplier -= 0.02f;
        }
        return (int) Math.min(newSize, Integer.MAX_VALUE - 1);
    }

}
