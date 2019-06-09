package gloom

import (
	"fmt"
	"strings"
)

// TODO: Gloom has lists, but I forget the syntax
// I think lists are declared with '[ ... ]', see 'eval'

// TODO: For completion, add in my named arguments addition

// Interpreter contains all state necessary for interpretation of gloom code
type Interpreter struct {
	Variables map[literal]evaluable
	Stack     SimpleStack
	Retain    SimpleStack
	CallStack ExecutionStack
	Lists     []gloomList
}

// SimpleStack implements a basic FIFO stack
type SimpleStack struct {
	elems []evaluable
}

// Pop an item from the stack
func (e *SimpleStack) Pop() (evaluable, error) {
	if len(e.elems) == 0 {
		return nil, fmt.Errorf("attempt to pop from empty stack")
	}

	item := e.elems[len(e.elems)-1]
	e.elems = e.elems[:len(e.elems)-1]
	return item, nil
}

// Push an item onto the stack
func (e *SimpleStack) Push(item evaluable) {
	e.elems = append(e.elems, item)
}

// ExecutionStack implements a basic FIFO stack
type ExecutionStack struct {
	elems [][]evaluable
}

// Pop an item from the stack
func (e *ExecutionStack) Pop() ([]evaluable, error) {
	if len(e.elems) == 0 {
		return nil, fmt.Errorf("attempt to pop from empty stack")
	}

	item := e.elems[len(e.elems)-1]
	e.elems = e.elems[:len(e.elems)-1]
	return item, nil
}

// Push an item onto the stack
func (e *ExecutionStack) Push(item []evaluable) {
	e.elems = append(e.elems, item)
}

// findMatchedSubsection solves the "matching parens" problem, returning the index of the last exitCh if possible
func findMatchedSubsection(tokens []string, enterCh, exitCh string) (int, error) {
	index := 0
	depth := 0

	for {
		switch tokens[index] {
		case enterCh:
			depth++
		case exitCh:
			depth--
			if depth == 0 {
				return index, nil
			}
		}

		index++
		if index >= len(tokens) {
			return -1, fmt.Errorf("could not fully match (%s, %s) in given token range: %v", enterCh, exitCh, tokens)
		}
	}
}

// parse turns the line of gloom code into a list of evaluable items that can then be run
func parse(tokens []string) ([]evaluable, error) {
	evalSteps := []evaluable{}

	for i := 0; i < len(tokens); i++ {
		switch tokens[i] {
		case "[":
			// Determine how large the scoped section is
			index, err := findMatchedSubsection(tokens[i:], "[", "]")
			if err != nil {
				return evalSteps, err
			}

			// Parse the scope section
			scopeCode, err := parse(tokens[i+1 : i+index])
			if err != nil {
				return evalSteps, err
			}

			// TODO: Count number of named args

			// Add the new section to the list
			evalSteps = append(evalSteps, gloomList{scopeCode})
			i += index

		// Handle comments
		case "(":
			// Figure out where the comment ends and then ignore it
			index, err := findMatchedSubsection(tokens[i:], "(", ")")
			if err != nil {
				return evalSteps, err
			}
			i += index

		// Other
		default:
			if "null" != tokens[i] {
				evalSteps = append(evalSteps, literal(tokens[i]))
			}
		}
	}

	return evalSteps, nil
}

// RunString interpret the given string according to the gloom rules
func (i *Interpreter) RunString(line string) error {
	// parse the line
	code, err := parse(strings.Fields(line))
	if err != nil {
		return err
	}

	fmt.Printf("Parsed gloom code: %#v\n", code)
	return gloomList{code}.Execute(i)
}

// RunFile executes the code included in the given gloom file
func (i *Interpreter) RunFile(filename string) error {
	// TODO: Implement
	return nil
}

// New creates a new Interpreter instance
func New() *Interpreter {
	i := &Interpreter{Variables: make(map[literal]evaluable)}
	initIntrinsics(i)
	return i
}
