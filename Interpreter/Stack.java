import java.util.NoSuchElementException;

/**
 * A simple linked-stack implementation.
 * Node is a helper class for Stack
 *
 * @author Grayson Hooper
 */
	
public class Stack<E> {
	public class Node {
		private Node nextNode;
		private E val;
		
		public Node(E v, Node next) {
			val = v;
			nextNode = next;
		}
		
		public E value() {
			return val;
		}
	
		public Node next() {
			return nextNode;
		}
		
		public void clear() {
			if (nextNode != null) {
				nextNode.clear();
			}
		
			nextNode = null;
		}
		
		public String toString() {
			//return val + " " + (nextNode != null ? nextNode : "");
			return (nextNode != null ? nextNode.toString() : "") + val + " ";
		}
	}

	private Node top;
	private int stackSize;
	

	/**
	 * Constructs a stack with sensible defaults.
	 */
	public Stack() {
		top = null;
		stackSize = 0;
	}

	/**
	 * Pushes the specified element on to the stack.
	 *
	 * @param e the specified element
	 */
	public Stack<E> push(E e) {
		++stackSize;
		top = new Node(e, top);
		return this;
	}
	
	/**
	 * Pops the top element off the stack, returning the element.
	 *
	 * @throws NoSuchElementException if the stack is empty
	 * @return the popped value
	 */
	public E pop() {
		if (stackSize <= 0) {
			throw new NoSuchElementException();
		}
		
		--stackSize;
		E tmp = top.value();
		top = top.next();
		return tmp;
	}
	
	/**
	 * Returns the top element.
	 *
	 * @throws NoSuchElementException if the stack is empty
	 * @return the top element
	 */
	public E top() {
		if (stackSize <= 0) {
			throw new NoSuchElementException();
		}
	
		E tmp = pop();
		push(tmp);
		return tmp;
	}
	
	/**
	 * Clears every element from the stack.
	 */
	public void clear() {
		if (stackSize > 0) {
			top.clear();
			top = null;
			stackSize = 0;
		}
	}
	
	/**
	 * Returns the number of elements contained in this stack.
	 *
	 * @return the number of elements contained in this stack.
	 */
	public int size() {
		return stackSize;
	}
	
	/**
	 * Determines if the stack is empty (size == 0).
	 *
	 * @return true if the stack is empty; false otherwise
	 */
	public boolean isEmpty() {
		return stackSize == 0;
	}
	
	/**
	 * Generates a string represtentation of this stack. The stack is
	 * represented by an opening square bracket, followed by a space delineated
	 * list of elements from bottom to top, followed by a closing square 
	 * bracket. The stack containing the elements 12, 42, and 10 (where 10 is
	 * the top element) would have the string representation "[ 12 42 10 ]".
	 * The empty stack has the string representation "[ ]" where only a single
	 * space separates the square brackets.
	 *
	 * @return the string representation of this stack.
	 */
	public String toString() {
		return "[ " + (top != null ? top : "") + "]";
	}
}