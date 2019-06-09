package gloom

import (
	"fmt"
	"regexp"
	"sort"
	"strconv"
)

var argMatch = regexp.MustCompile(`\$(\d+)`)

// evaluable defines a method for evaluating all
type evaluable interface {
	eval(i *Interpreter) error
}

// Define the token types for the interpreter
type literal string

func (s literal) eval(i *Interpreter) error {
	// Check for whether the literal is a defined variable
	val, isVar := i.Variables[s]
	if isVar {
		i.Stack.Push(val)
		return evaluate(i)
	}

	// Otherwise just push the value on the stack (conversions will happen later)
	i.Stack.Push(s)
	return nil
}

type gloomList struct {
	Elems []evaluable
}

// Helper method to count/check arguments
func (sc gloomList) countArgs() (int, error) {
	// Extract all "arg characters" in the list
	args := []int{}
	for _, arg := range sc.Elems {
		if v, ok := arg.(argument); ok {
			args = append(args, int(v))
		}
	}

	// Early exit for no-args case
	if len(args) == 0 {
		return 0, nil
	}

	// Sort the argument numbers in ascending order
	sort.Ints(args)
	if args[0] == 0 {
		return 0, fmt.Errorf("invalid argument `$0`")
	}

	// Check that we're not "skipping" arguments
	last := 0
	for _, argIdx := range args {
		if argIdx != last && argIdx != last+1 {
			return 0, fmt.Errorf("missing argument `$%d`", last+1)
		}

		last = argIdx
	}

	return last, nil
}

// Turn the list, and any sublists/arguments, into optimized representations
func (sc gloomList) optimize(i *Interpreter) (Reference, error) {
	for idx := 0; idx < len(sc.Elems); idx++ {
		switch v := sc.Elems[idx].(type) {
		case gloomList:
			subref, err := v.optimize(i)
			if err != nil {
				return Reference(0), err
			}

			sc.Elems[idx] = subref

		case literal:
			arg := argMatch.FindStringSubmatch(string(v))
			switch len(arg) {
			case 0:
			case 2:
				argIdx, err := strconv.ParseInt(arg[1], 0, 64)
				if err != nil {
					return Reference(0), err
				}

				sc.Elems[idx] = argument(argIdx)

			default:
				return Reference(0), fmt.Errorf("invalid argument found: %s", string(v))
			}
		}
	}

	ref := Reference(len(i.Lists))
	i.Lists = append(i.Lists, sc)
	return ref, nil
}

func (sc gloomList) eval(i *Interpreter) error {
	ref, err := sc.optimize(i)
	if err != nil {
		return err
	}

	i.Stack.Push(ref)
	return nil
}

// Execute runs the list as a function
func (sc gloomList) Execute(i *Interpreter) error {
	fmt.Printf("Stack: %v\n", i.Stack)

	// Count the number of arguments in the list
	numArgs, err := sc.countArgs()
	if err != nil {
		return err
	}
	fmt.Printf("NumArgs = %d\n", numArgs)

	// Extract all arguments from the stack
	args := make([]evaluable, numArgs)
	for count := 0; count < numArgs; count++ {
		val, err := i.Stack.Pop()
		if err != nil {
			return err
		}

		args[count] = val
	}
	fmt.Printf("Args = %v\n", args)

	// Replace the arguments in the list with the popped items
	evalElems := make([]evaluable, len(sc.Elems))
	for idx, elem := range sc.Elems {
		if v, ok := sc.Elems[idx].(argument); ok {
			evalElems[idx] = args[int(v)-1]
		} else {
			evalElems[idx] = elem
		}
	}

	// Evaluate the individual elements
	for _, node := range evalElems {
		err := node.eval(i)
		if err != nil {
			return err
		}
	}

	return nil
}

type intrinsic func(*Interpreter) error

func (fn intrinsic) eval(i *Interpreter) error {
	return fn(i)
}

type Reference int

func (ls Reference) eval(i *Interpreter) error {
	if int(ls) >= len(i.Lists) {
		return fmt.Errorf("attempt to get list with no reference: %d", int(ls))
	}
	return nil
}

type argument int

func (a argument) eval(i *Interpreter) error {
	return nil
}
