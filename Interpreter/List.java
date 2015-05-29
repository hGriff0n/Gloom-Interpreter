import java.util.NoSuchElementException;

/**
 * A simple list implementation.
 *
 * @author Grayson Hooper
 */
 
 // is this a representation of the gloom list ???
public class List<E> {
	E[] data;
	int top;			// the number of elements in data

	/**
	 * Constructs a stack with sensible defaults.
	 */
	@SuppressWarnings("unchecked")
	public List() {
		data = (E[])(new Object[20]);
		top = 0;
	}
	
	@SuppressWarnings("unchecked")
	private void resize() {
		E[] tmp = (E[])(new Object[data.length * 2]);
		
		for (int i = 0; i < top; ++i) {
			tmp[i] = data[i];
		}
		
		data = tmp;
	}
	
	private void checkBounds(int i) {
		if (i < 0 || i >= top) {
			throw new IndexOutOfBoundsException();
		}
	}
	
	private void checkBounds(int i, boolean over) {
		if (i < 0 || i > top) {
			throw new IndexOutOfBoundsException();
		}
	}

	/**
	 * Appends the specified element to the end of this list.
	 *
	 * @param e element to be appended to this list
	 */
	public void add(E e) {
		data[top++] = e;
		
		if (top == data.length) {
			resize();
		}
	}
	
	/**
	 * Inserts the specified element at the specified position in this list.
	 * Shifts the element currently at that position (if any) and any 
	 * subsequent elements to the right (adds one to their indices).
	 *
	 * @throws IndexOutOfBoundsException if the index is out of range 
	 * [index < 0 || index >= size()]
	 * @param index the index at which the specified element will be added
	 * @param e element to be inserted
	 */
	public void add(int index, E e) {
		checkBounds(index, true);
		
		if (++top == data.length) {
			resize();
		}
		
		for (int i = top; i > index; --i) {
			data[i] = data[i - 1];
		}
		
		data[index] = e;
	}
	
	public void append(E e) {
		add(e);
	}
	
	/**
	 * Returns the element at the specified position in this list.
	 *
	 * @throws IndexOutOfBoundsException if the index is out of range 
	 * [index < 0 || index >= size()]
	 * @param index of the element to return
	 * @return the element at the specified position in this list
	 */
	public E get(int index) {
		checkBounds(index);
		
		return data[index];
	}
	 
	/**
	 * Removes the element at the specified position in this list. Shifts any 
	 * subsequent elements to the left (subtracts one from their indices). 
	 * Returns the element that was removed from the list.
	 *
	 * @throws IndexOutOfBoundsException if the index is out of range 
	 * [index < 0 || index >= size()]
	 * @param the index of the element to be removed
	 * @return the element previously at the specified position
	 */
	public E remove(int index) {
		checkBounds(index);
		
		E tmp = data[index];
		
		for (int i = index; i < top + 1; ++i) {
			data[i] = data[i + 1];
		}
		
		data[top--] = null;
		return tmp;
	}
	
	/**
	 * Replaces the element at the specified position in this list with the 
	 * specified element.
	 *
	 * @throws IndexOutOfBoundsException if the index is out of range 
	 * [index < 0 || index >= size()]
	 * @param index index of the element to replace
	 * @param e element to be stored at the specified position
	 * @return the element previously at the specified position
	 */
	public void set(int index, E e) {
		checkBounds(index);
		
		data[index] = e;
	}

	/**
	 * Removes all of the elements from this list. The list will be empty after
	 * this call returns.
	 */
	public void clear() {
		for (int i = 0; i < top; ++i) {
			data[i] = null;
		}
	}
	
	/**
	 * Returns the number of elements in this list. If this list contains more 
	 * than Integer.MAX_VALUE elements, returns Integer.MAX_VALUE.
	 *
	 * @return the number of elements in this list
	 */
	public int size() {
		return top;
	}
	
	/**
	 * Returns true if this list contains no elements.
	 *
	 * @return if this list contains no elements
	 */
	public boolean isEmpty() {
		return top == 0;
	}
	
	public E[] internal() {
		return data;
	}

	/**
	 * Generates a string represtentation of this list. The list is represented 
	 * by an opening square bracket, followed by a space delineated list of 
	 * elements from front to back, followed by a closing square bracket. The 
	 * list containing the elements 12, 42, and 10 (where 12 is the first
	 * element and 10 is the last) would have the string representation 
	 * "[ 12 42 10 ]". The empty stack has the string representation "[ ]" 
	 * where only a single space separates the square brackets.
	 *
	 * @return the string representation of this list.
	 */
	public String toString() {
		String ret = "[ ";
		
		for (int i = 0; i < top; ++i) {
			ret = ret + data[i] + " ";
		}
		
		return ret + "]";
	}
}