package main

import (
	"fmt"

	"./gloom"
)

func main() {
	interpreter := gloom.New()

	// TODO: Add more options for controlling code evaluation (ie. targeting files/repl/etc.)

	// Run the code
	// TODO: This hangs because lists are currently greedily evaluated. Need to add list reference semantics
	// And remove list evaluation as the default
	err := interpreter.RunString("[ 3 3 ]")
	if err != nil {
		fmt.Printf("%v\n", err)
		return
	}

	// Get the program's value (top of the stack)
	// TODO: This leaves `lists` as references as-is
	val, err := interpreter.Stack.Pop()
	if err != nil {
		fmt.Printf("%v\n", err)
		return
	}

	if v, ok := val.(gloom.Reference); ok {
		fmt.Printf("%v\n", interpreter.Lists[int(v)])
	} else {
		fmt.Printf("%v\n", val)
	}
	fmt.Printf("Rest of stack: %v\n", interpreter.Stack)
	fmt.Printf("Vars: %v", interpreter.Variables)
}
