package gloom

import (
	"fmt"
	"strconv"
)

const (
	gloomFalse = literal("0")
	gloomTrue  = literal("-1")
)

func initIntrinsics(i *Interpreter) {
	// Control flow
	i.Variables["rep"] = intrinsic(replicate)
	i.Variables["loop"] = intrinsic(loop)
	i.Variables["if"] = intrinsic(ifSwitch)

	// Variables
	i.Variables["!"] = intrinsic(newVar)
	i.Variables["eval"] = intrinsic(evaluate)

	// List library
	// size
	// copy
	// get
	// set
	// remove
	// insert
	// append

	// Stack Operators
	i.RunString("dup [ $1 $1 ] !")
	i.RunString("over [ $2 $1 $2 ] !")
	i.RunString("swap [ $1 $2 ] !")
	i.RunString("drop [ @ $1 ! ] !")
	i.Variables[">r"] = intrinsic(mainToRetain)
	i.Variables["r>"] = intrinsic(retainToMain)

	// Booleans
	i.Variables[">"] = intrinsic(greaterThan)
	i.RunString("not [ f t if ] !")
	i.RunString("and [ $1 $2 f if ] !")
	i.RunString("or [ $1 t $2 if ] !")
	i.RunString("xor [ $1 $2 or $1 $2 nand and ] !")
	i.RunString("nand [ and not ] !")
	i.RunString("nor [ or not ] !")
	i.RunString("xnor [ xor not ] !")
	i.RunString("< [ $1 $2 > ] !")
	i.RunString(">= [ $2 $1 > t [ $2 $1 = ] if ] !")
	i.RunString("<= [ $2 $1 < t [ $2 $1 = ] if ] !")
	i.RunString("= [ $2 $1 > not $1 $2 > not and ] !")
	i.RunString("!= [ = not ] !")
	i.Variables["t"] = gloomTrue
	i.Variables["f"] = gloomFalse

	// Math library
	i.Variables["+"] = intrinsic(plus)
	i.Variables["-"] = intrinsic(sub)
	i.Variables["*"] = intrinsic(mul)
	i.Variables["/"] = intrinsic(div)
	i.Variables["math.mod"] = intrinsic(mod)
	i.Variables["%"] = intrinsic(mod)
	i.RunString("math.pow [ $2 $2 [ over * ] $1 1 - rep swap drop ] !")
	i.RunString("math.max [ $2 $1 > $1 $2 if ] !")
	i.RunString("math.min [ $2 $1 < $1 $2 if ] !")

	// Debug library
	// debug.size
	// debug.remove
	// debug.clear
	// import

	// Type information
	// type
	// int?
	// list?
}

// plus implements the code necessary for addition of two integers
func (s *SimpleStack) popInt() (int64, error) {
	eval, err := s.Pop()
	if err != nil {
		return 0, err
	}

	if lit, ok := eval.(literal); ok {
		return strconv.ParseInt(string(lit), 0, 64)
	}

	return 0, fmt.Errorf("can't popInt on non-literal stack item: %v", eval)
}

func plus(i *Interpreter) error {
	// Extract the left hand value from the stack
	lhs, err := i.Stack.popInt()
	if err != nil {
		return err
	}

	// Extract the right hand value from the stack
	rhs, err := i.Stack.popInt()
	if err != nil {
		return err
	}

	// Perform the addition
	i.Stack.Push(literal(strconv.FormatInt(lhs+rhs, 10)))
	return nil
}

func sub(i *Interpreter) error {
	// Extract the left hand value from the stack
	lhs, err := i.Stack.popInt()
	if err != nil {
		return err
	}

	// Extract the right hand value from the stack
	rhs, err := i.Stack.popInt()
	if err != nil {
		return err
	}

	// Perform the subtraction
	i.Stack.Push(literal(strconv.FormatInt(lhs-rhs, 10)))
	return nil
}

func mul(i *Interpreter) error {
	// Extract the left hand value from the stack
	lhs, err := i.Stack.popInt()
	if err != nil {
		return err
	}

	// Extract the right hand value from the stack
	rhs, err := i.Stack.popInt()
	if err != nil {
		return err
	}

	i.Stack.Push(literal(strconv.FormatInt(lhs*rhs, 10)))
	return nil
}

func div(i *Interpreter) error {
	// Extract the left hand value from the stack
	lhs, err := i.Stack.popInt()
	if err != nil {
		return err
	}

	// Extract the right hand value from the stack
	rhs, err := i.Stack.popInt()
	if err != nil {
		return err
	}

	i.Stack.Push(literal(strconv.FormatInt(lhs/rhs, 10)))
	return nil
}

func mod(i *Interpreter) error {
	// Extract the left hand value from the stack
	lhs, err := i.Stack.popInt()
	if err != nil {
		return err
	}

	// Extract the right hand value from the stack
	rhs, err := i.Stack.popInt()
	if err != nil {
		return err
	}

	i.Stack.Push(literal(strconv.FormatInt(((lhs%rhs)+rhs)%rhs, 10)))
	return nil
}

func greaterThan(i *Interpreter) error {
	// Extract the left hand value from the stack
	lhs, err := i.Stack.popInt()
	if err != nil {
		return err
	}

	// Extract the right hand value from the stack
	rhs, err := i.Stack.popInt()
	if err != nil {
		return err
	}

	if lhs > rhs {
		i.Stack.Push(gloomTrue)
	} else {
		i.Stack.Push(gloomFalse)
	}
	return nil
}

func ifSwitch(i *Interpreter) error {
	// Pop the values from the stack
	ifFalse, err := i.Stack.Pop()
	if err != nil {
		return err
	}
	ifTrue, err := i.Stack.Pop()
	if err != nil {
		return err
	}

	// Evaluate the if test
	err = evaluate(i)
	if err != nil {
		return err
	}

	// Check which branch to run and run it
	truthy, err := i.Stack.popInt()
	if err != nil {
		return err
	}
	switch truthy {
	case -1:
		return ifTrue.eval(i)
	case 0:
		return ifFalse.eval(i)
	default:
		return nil
	}
}

func mainToRetain(i *Interpreter) error {
	val, err := i.Stack.Pop()
	if err != nil {
		return err
	}

	i.Retain.Push(val)
	return nil
}

func retainToMain(i *Interpreter) error {
	val, err := i.Retain.Pop()
	if err != nil {
		return err
	}

	i.Stack.Push(val)
	return nil
}

func (s *SimpleStack) popAsList() (gloomList, error) {
	item, err := s.Pop()
	if err != nil {
		return gloomList{}, err
	}

	switch v := item.(type) {
	case gloomList:
		return v, nil
	default:
		return gloomList{Elems: []evaluable{v}}, nil
	}
}

func newVar(i *Interpreter) error {
	// TODO: Has wrong behavior for `a [3 4 5] !` (gives a == 3)
	body, err := i.Stack.popAsList()
	if err != nil {
		return err
	}

	names, err := i.Stack.popAsList()
	if err != nil {
		return err
	}

	for idx, name := range names.Elems {
		if varName, ok := name.(literal); ok {
			// TODO: I think actual def behavior requires me to eval the assigned values
			fmt.Printf("Assigning %v to %s\n", body.Elems[idx], string(varName))
			i.Variables[varName] = body.Elems[idx]

		} else {
			return fmt.Errorf("attempt to assign to non-literal: %v", name)
		}
	}

	return nil
}

func replicate(i *Interpreter) error {
	n, err := i.Stack.popInt()
	if err != nil {
		return err
	}

	body, err := i.Stack.Pop()
	if err != nil {
		return err
	}

	for count := int64(0); count < n; count++ {
		err = body.eval(i)
		if err != nil {
			return err
		}
	}

	return nil
}

func evaluate(i *Interpreter) error {
	val, err := i.Stack.Pop()
	if err != nil {
		return err
	}

	if v, ok := val.(Reference); ok {
		return i.Lists[int(v)].Execute(i)
	}

	return val.eval(i)
}

func loop(i *Interpreter) error {
	body, err := i.Stack.Pop()

	for {
		err = body.eval(i)
		if err != nil {
			return err
		}

		test, err := i.Stack.popInt()
		if err != nil {
			return err
		}

		if test == -1 {
			return nil
		}
	}
}
