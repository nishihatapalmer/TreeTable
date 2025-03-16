package net.byteseek.utils.collections;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

class BlockModifyArrayListTest {

    @Test
    void size_emptyList() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        assertEquals(0, list.size(), "The size of an empty list should be 0.");
    }

    @Test
    void size_afterAddingElements() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        assertEquals(3, list.size(), "The size should correctly reflect the number of elements added.");
    }

    @Test
    void size_afterRemovingElements() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.remove(1);
        assertEquals(2, list.size(), "The size should correctly reflect the number of elements after removal.");
    }

    @Test
    void get() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        list.add(10);
        list.add(20);
        list.add(30);
        assertEquals(10, list.get(0), "The first element should be 10.");
        assertEquals(20, list.get(1), "The second element should be 20.");
        assertEquals(30, list.get(2), "The third element should be 30.");
    }

    @Test
    void get_outOfBoundsException() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        list.add(10);
        list.add(20);

        Exception exception = assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
        assertEquals("Index = -1 size = 2", exception.getMessage(), "Invalid index should throw an IndexOutOfBoundsException.");

        exception = assertThrows(IndexOutOfBoundsException.class, () -> list.get(2));
        assertEquals("Index = 2 size = 2", exception.getMessage(), "Invalid index should throw an IndexOutOfBoundsException.");
    }

    @Test
    void add() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        assertTrue(list.add(10), "Adding an element should return true.");
        assertTrue(list.add(20), "Adding a second element should return true.");
        assertEquals(2, list.size(), "List size should be 2 after adding two elements.");
        assertEquals(10, list.get(0), "First element should be 10.");
        assertEquals(20, list.get(1), "Second element should be 20.");
    }

    @Test
    void add_invalidIndex() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        list.add(10);
        list.add(20);

        Exception exception = assertThrows(IndexOutOfBoundsException.class, () -> list.add(-1, 15));
        assertEquals("Index = -1 size = 2", exception.getMessage(), "Adding at an invalid index (-1) should throw IndexOutOfBoundsException.");

        exception = assertThrows(IndexOutOfBoundsException.class, () -> list.add(3, 15));
        assertEquals("Index = 3 size = 2", exception.getMessage(), "Adding at an out-of-range index (3) should throw IndexOutOfBoundsException.");
    }

    @Test
    void testAdd() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        list.add(0, 10);
        assertEquals(1, list.size(), "List size should be 1 after adding an element at index 0.");
        assertEquals(10, list.get(0), "Element at index 0 should be 10.");

        list.add(1, 20);
        assertEquals(2, list.size(), "List size should be 2 after adding an element at index 1.");
        assertEquals(20, list.get(1), "Element at index 1 should be 20.");

        list.add(1, 15);
        assertEquals(3, list.size(), "List size should be 3 after inserting an element at index 1.");
        assertEquals(15, list.get(1), "Element at index 1 should now be 15.");
        assertEquals(20, list.get(2), "Element at index 2 should now be 20.");
    }

    @Test
    void addAll() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        list.add(10);
        list.add(20);

        List<Integer> toAdd = List.of(30, 40, 50);
        list.addAll(toAdd);

        assertEquals(5, list.size(), "The size should reflect the number of elements after addAll.");
        assertEquals(10, list.get(0), "First element should be 10.");
        assertEquals(50, list.get(4), "Last element should be 50.");
    }

    @Test
    void testAddAll() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        list.add(10);
        list.add(20);

        List<Integer> toAdd = List.of(30, 40, 50);
        list.addAll(1, toAdd); // Insert at index 1.

        assertEquals(5, list.size(), "The size should reflect the number of elements after addAll at an index.");
        assertEquals(10, list.get(0), "First element should still be 10.");
        assertEquals(30, list.get(1), "Element at index 1 should be 30.");
        assertEquals(20, list.get(4), "Element at index 4 should now be the original 20.");
    }

    @Test
    void testAddAll1() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        list.add(10);
        list.add(20);

        Collection<Integer> toAdd = List.of(30, 40, 50);
        list.addAll(1, toAdd); // Insert at index 1.

        assertEquals(5, list.size(), "The size should reflect the number of elements after addAll from a Collection.");
        assertEquals(10, list.get(0), "First element should still be 10.");
        assertEquals(30, list.get(1), "Element at index 1 should be 30.");
        assertEquals(20, list.get(4), "Element at index 4 should now be the original 20.");
    }

    @Test
    void set() {
    }

    @Test
    void set_replacesElement() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        list.add(10);
        list.add(20);
        list.add(30);

        int previousValue = list.set(1, 25);
        assertEquals(20, previousValue, "The previous value at index 1 should be returned.");
        assertEquals(25, list.get(1), "The value at index 1 should be updated to 25.");

        previousValue = list.set(2, 35);
        assertEquals(30, previousValue, "The previous value at index 2 should be returned.");
        assertEquals(35, list.get(2), "The value at index 2 should be updated to 35.");
    }

    @Test
    void set_outOfBoundsException() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        list.add(10);
        list.add(20);

        Exception exception = assertThrows(IndexOutOfBoundsException.class, () -> list.set(-1, 15));
        assertEquals("Index = -1 size = 2", exception.getMessage(), "Setting an element at index -1 should throw IndexOutOfBoundsException.");

        exception = assertThrows(IndexOutOfBoundsException.class, () -> list.set(2, 15));
        assertEquals("Index = 2 size = 2", exception.getMessage(), "Setting an element at index 2 in a list of size 2 should throw IndexOutOfBoundsException.");
    }

    @Test
    void remove_removesCorrectElement() {
        BlockModifyArrayList<String> list = new BlockModifyArrayList<>();
        list.add("A");
        list.add("B");
        list.add("C");

        String removed = list.remove(1);

        assertEquals("B", removed, "Removed element should be 'B'.");
        assertEquals(2, list.size(), "List size should be updated after removal.");
    }

    @Test
    void remove_outOfBoundsException() {
        BlockModifyArrayList<String> list = new BlockModifyArrayList<>();
        list.add("A");

        Exception exception = assertThrows(IndexOutOfBoundsException.class, () -> list.remove(-1));
        assertEquals("Index = -1 size = 1", exception.getMessage(), "Accessing -1 should throw IndexOutOfBoundsException.");

        exception = assertThrows(IndexOutOfBoundsException.class, () -> list.remove(1));
        assertEquals("Index = 1 size = 1", exception.getMessage(), "Accessing index 1 in a list of size 1 should throw IndexOutOfBoundsException.");
    }

    @Test
    void remove_shiftsRemainingElements() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);

        list.remove(0);

        assertEquals(2, list.size(), "List size should be reduced after removal.");
        assertEquals(2, list.get(0), "First element should now be 2.");
        assertEquals(3, list.get(1), "Second element should now be 3.");
    }

    @Test
    void remove_afterSingleElementRemoval() {
        BlockModifyArrayList<Double> list = new BlockModifyArrayList<>();
        list.add(1.1);

        Double removed = list.remove(0);

        assertEquals(1.1, removed, "Removed element should be 1.1.");
        assertEquals(0, list.size(), "List should be empty after single element removal.");
    }

    @Test
    void testRemove() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        list.add(5);

        list.remove(1, 3);

        assertEquals(2, list.size(), "List size should be reduced appropriately after range removal.");
        assertEquals(1, list.get(0), "First element should remain unchanged.");
        assertEquals(5, list.get(1), "Element after removed range should correctly shift.");
    }

    @Test
    void testRemove_rangeToEnd() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);

        list.remove(1, 2);

        assertEquals(1, list.size(), "List size should correctly reflect the remaining element(s).");
        assertEquals(1, list.get(0), "First element should remain unchanged.");
    }

    @Test
    void testRemove_toLastPosition() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);

        list.remove(1, 2); // 'to' is the last position.

        assertEquals(1, list.size(), "List size should correctly reflect removal from index 1 to the last element.");
        assertEquals(1, list.get(0), "Remaining element should be at index 0.");
    }

    @Test
    void testRemove_toEqualsSize() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);

        list.remove(1, 3); // 'to' equals the size of the array.

        assertEquals(1, list.size(), "List size should correctly reflect removal from index 1 to the end.");
        assertEquals(1, list.get(0), "Remaining element should be at index 0.");
    }

    @Test
    void testRemove_toGreaterThanSize() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);

        list.remove(1, 5); // 'to' is greater than the size of the array.

        assertEquals(1, list.size(), "List size should correctly reflect removal from index 1 to the end.");
        assertEquals(1, list.get(0), "Remaining element should be at index 0.");
    }

    @Test
    void testRemove_invalidRange() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        list.add(1);

        Exception exception = assertThrows(IndexOutOfBoundsException.class, () -> list.remove(1, 0));
    }

    @Test
    void testRemove_toLessThanFrom_throwsIllegalArgumentException() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> list.remove(2, 1));
    }

    @Test
    void indexOf_elementExists() {
        BlockModifyArrayList<String> list = new BlockModifyArrayList<>();
        list.add("A");
        list.add("B");
        list.add("C");

        assertEquals(0, list.indexOf("A"), "indexOf should return 0 for the first occurrence of the element.");
        assertEquals(1, list.indexOf("B"), "indexOf should return the correct index of the element.");
        assertEquals(2, list.indexOf("C"), "indexOf should return the correct index of the last element.");
    }

    @Test
    void addAll_atSizeIndex() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        list.add(10);
        list.add(20);

        List<Integer> toAdd = List.of(30, 40, 50);
        list.addAll(list.size(), toAdd);

        assertEquals(5, list.size(), "The size should correctly reflect the new elements added at the end.");
        assertEquals(10, list.get(0), "First element should remain unchanged.");
        assertEquals(20, list.get(1), "Second element should remain unchanged.");
        assertEquals(30, list.get(2), "First added element should be at index 2.");
        assertEquals(40, list.get(3), "Second added element should be at index 3.");
        assertEquals(50, list.get(4), "Last added element should be at index 4.");
    }

    @Test
    void testRemove_outOfBoundsRange() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        list.add(1);
        list.add(2);

        Exception exception = assertThrows(IndexOutOfBoundsException.class, () -> list.remove(-1, 1));
    }

    @Test
    void testReplace_withList() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);

        List<Integer> newValues = List.of(5, 6, 7);
        list.replace(1, 2, newValues); // Replacing indices 1 and 2.

        assertEquals(5, list.size(), "The size should reflect the new elements added.");
        assertEquals(1, list.get(0), "The first element should remain unchanged.");
        assertEquals(5, list.get(1), "First replacement value should be at index 1.");
        assertEquals(6, list.get(2), "Second replacement value should be at index 2.");
        assertEquals(7, list.get(3), "Third replacement value should be at index 3.");
        assertEquals(4, list.get(4), "Element after the replaced range should remain unchanged.");
    }

    @Test
    void testReplace_withEnumeration() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);

        Enumeration<Integer> newValues = java.util.Collections.enumeration(List.of(8, 9));
        list.replace(1, 3, newValues, 2); // Replacing indices 1, 2, 3 with 8, 9.

        assertEquals(3, list.size(), "The size should reflect removal of extra elements in the replaced range.");
        assertEquals(1, list.get(0), "The first element should remain unchanged.");
        assertEquals(8, list.get(1), "First replacement value should be at index 1.");
        assertEquals(9, list.get(2), "Second replacement value should be at index 2.");
    }

    @Test
    void testReplace_variousSizes() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        list.add(1); // 0
        list.add(2); // 1
        list.add(3); // 2
        list.add(4); // 3
        list.add(5); // 4

        // Scenario 1: Replacement list smaller than range
        List<Integer> smallerList = List.of(5);
        list.replace(2, 3, smallerList);
        assertEquals(4, list.size(), "The size should reflect the smaller replacement.");
        assertEquals(1, list.get(0), "The first element should remain unchanged.");
        assertEquals(2, list.get(1), "The second element should remain unchanged.");
        assertEquals(5, list.get(2), "The replaced value should be updated.");

        // Reset list
        list.clear();
        list.add(1); // 0
        list.add(2); // 1
        list.add(3); // 2
        list.add(4); // 3
        list.add(5); // 4

        // Scenario 2: Replacement list of identical size to range
        List<Integer> identicalList = List.of(5, 6);
        list.replace(2, 3, identicalList);
        assertEquals(5, list.size(), "The size should remain unchanged after replacement.");
        assertEquals(5, list.get(2), "First replaced value should be updated.");
        assertEquals(6, list.get(3), "Second replaced value should match the new list.");

        // Reset list
        list.clear();
        list.add(1); // 0
        list.add(2); // 1
        list.add(3); // 2
        list.add(4); // 3
        list.add(5); // 4

        // Scenario 3: Replacement list larger than range
        List<Integer> largerList = List.of(5, 6, 7, 8);
        list.replace(2, 3, largerList);
        assertEquals(7, list.size(), "The size should expand to accommodate larger replacement.");
        assertEquals(5, list.get(2), "First replaced value should be updated.");
        assertEquals(6, list.get(3), "Second replaced value should match the new list.");
        assertEquals(7, list.get(4), "Additional values should be appended.");
        assertEquals(8, list.get(5), "Last value should be added at the end.");

        // Reset list
        list.clear();
        list.add(1); // 0
        list.add(2); // 1
        list.add(3); // 2
        list.add(4); // 3

        // Edge case: Replacing range at the end
        List<Integer> edgeCaseList = List.of(9, 10);
        list.replace(2, 3, edgeCaseList);
        assertEquals(4, list.size(), "The size should reflect the end boundary replacement.");
        assertEquals(1, list.get(0), "The first element should remain unchanged.");
        assertEquals(2, list.get(1), "The second element should remain unchanged.");
        assertEquals(9, list.get(2), "First replaced value should be updated at the boundary.");
        assertEquals(10, list.get(3), "Last replaced value should be updated at the boundary.");
    }

    @Test
    void testReplace_invalidRange() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        list.add(1);
        list.add(2);

        Exception exception = assertThrows(IndexOutOfBoundsException.class, () -> list.replace(2, 1, List.of(3)));
    }

    @Test
    void testReplace_singleElementRange() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);

        // Replace the element at index 1 with a single new element
        List<Integer> singleElement = List.of(10);
        list.replace(1, 1, singleElement);
        assertEquals(3, list.size(), "List size should remain unchanged when replacing one element with another.");
        assertEquals(1, list.get(0), "First element should remain unchanged.");
        assertEquals(10, list.get(1), "Element at index 1 should be updated to 10.");
        assertEquals(3, list.get(2), "Last element should remain unchanged.");

        // Replace the element at index 1 with multiple new elements
        List<Integer> multipleElements = List.of(20, 30, 40);
        list.replace(1, 1, multipleElements);
        assertEquals(5, list.size(), "List size should increase when replacing one element with multiple elements.");
        assertEquals(1, list.get(0), "First element should remain unchanged.");
        assertEquals(20, list.get(1), "First replacement value should be at index 1.");
        assertEquals(30, list.get(2), "Second replacement value should be at index 2.");
        assertEquals(40, list.get(3), "Third replacement value should be at index 3.");
        assertEquals(3, list.get(4), "Last element should remain unchanged.");
    }

    @Test
    void testReplace_outOfBounds() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        list.add(1);
        list.add(2);

        Exception exception = assertThrows(IndexOutOfBoundsException.class, () -> list.replace(-1, 1, List.of(3)));
    }

    @Test
    void indexOf_elementNotFound () {
        BlockModifyArrayList<String> list = new BlockModifyArrayList<>();
        list.add("A");
        list.add("B");
        list.add("C");

        assertEquals(-1, list.indexOf("D"), "indexOf should return -1 when the element is not in the list.");
        assertEquals(-1, list.indexOf(""), "indexOf should return -1 when the element does not match any in the list.");

        BlockModifyArrayList<String> emptyList = new BlockModifyArrayList<>();
        assertEquals(-1, emptyList.indexOf("A"), "indexOf should return -1 when called on an empty list.");
    }

    @Test
    void indexOf_nullElement () {
        BlockModifyArrayList<String> list = new BlockModifyArrayList<>();
        list.add("A");
        list.add(null);
        list.add("B");

        assertEquals(1, list.indexOf(null), "indexOf should return the correct index of the first null element.");
        list.remove(1);
        assertEquals(-1, list.indexOf(null), "indexOf should return -1 when no null elements exist in the list.");

        BlockModifyArrayList<String> emptyList = new BlockModifyArrayList<>();
        assertEquals(-1, emptyList.indexOf(null), "indexOf should return -1 when null is queried in an empty list.");
    }

    @Test
    void lastIndexOf_elementExists() {
        BlockModifyArrayList<String> list = new BlockModifyArrayList<>();
        list.add("A");
        list.add("B");
        list.add("A");
        list.add("C");

        assertEquals(2, list.lastIndexOf("A"), "lastIndexOf should return the last occurrence of the element.");
        assertEquals(1, list.lastIndexOf("B"), "lastIndexOf should return the correct index for 'B'.");
        assertEquals(3, list.lastIndexOf("C"), "lastIndexOf should return the correct index for 'C'.");
    }

    @Test
    void lastIndexOf_elementNotFound() {
        BlockModifyArrayList<String> list = new BlockModifyArrayList<>();
        list.add("A");
        list.add("B");
        list.add("C");

        assertEquals(-1, list.lastIndexOf("D"), "lastIndexOf should return -1 when the element is not in the list.");
        assertEquals(-1, list.lastIndexOf(""), "lastIndexOf should return -1 for an empty string.");
    }

    @Test
    void lastIndexOf_nullElement() {
        BlockModifyArrayList<String> list = new BlockModifyArrayList<>();
        list.add("A");
        list.add(null);
        list.add("B");
        list.add(null);

        assertEquals(3, list.lastIndexOf(null), "lastIndexOf should return the last occurrence of null.");
        list.remove(3);
        assertEquals(1, list.lastIndexOf(null), "lastIndexOf should return the correct index of the remaining null element.");
    }

    @Test
    void clear_emptyList() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        list.clear();
        assertEquals(0, list.size(), "Clear on an empty list should result in size 0.");
    }

    @Test
    void clear_nonEmptyList() {
        BlockModifyArrayList<String> list = new BlockModifyArrayList<>();
        list.add("A");
        list.add("B");
        list.add("C");
        list.clear();
        assertEquals(0, list.size(), "Clear on a non-empty list should result in size 0.");
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(0), "Accessing any element after clearing should throw IndexOutOfBoundsException.");
    }

    @Test
    void add_257Elements() {
        BlockModifyArrayList<Integer> list = new BlockModifyArrayList<>();
        for (int i = 0; i < 257; i++) {
            list.add(i);
        }

        assertEquals(257, list.size(), "List size should be 257 after adding 257 elements.");

        for (int i = 0; i < 257; i++) {
            assertEquals(i, list.get(i), "Element at index " + i + " should match the expected value.");
        }
    }
}