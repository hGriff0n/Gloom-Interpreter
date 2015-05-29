import java.util.HashMap;
import java.io.*;
import java.util.Scanner;

abstract class TokenBase {
	private String token;
	private int needs;

	public TokenBase(String token, int needs) {
		this.token = token;
		this.needs = needs;
	}
	
	public void hasElements(Stack<String> stack) {
		if (stack.size() < needs) {
			throw new TokenError(String.format("Error in %s: Needs %d elements, Stack has %d", token, needs, stack.size()));
		}
	}
	
	public static int toInt(String val) {
		return Integer.parseInt(val);
	}
	
	public String getToken() {
		return token;
	}
	
	public int getNeeds() {
		return needs;
	}
	
	public static boolean isReference(String val) {
		return hasType(val, "reference");
	}
	
	public static boolean hasType(String val, String type) {
		try {
		switch (type) {
			case "bool":
				return "-1".equals(val) || "0".equals(val);
			case "list":
				if (val.charAt(0) == '[') {
					return true;
				}
			case "reference":
				toInt(val.substring(1));
				return val.charAt(0) == '&';
			case "int":
				toInt(val);
				return true;
			default:
				return true;
		}
		} catch (NumberFormatException e) {
			return "string".equals(type);
		}
	}
	
	public static String type(String val) {
		try {
			switch (val.charAt(0)) {
				case '&':
					toInt(val.substring(1));
				case '[':
					return "list";
				case '$':
					return "string";
				default:
					toInt(val);
					return "int";
			}
		} catch (RuntimeException e) {
			return "string";
		}
	}
	
	void run(Interpreter gloom, Stack<String> main, Stack<String> retain) {
		hasElements(main);
	}
	
	public static HashMap<String, TokenBase> allTokens() {
		HashMap<String, TokenBase> ret = new HashMap<>();
		
		for (Operation op : Operation.values()) {
			ret.put(op.token, op.instruction);
		}
		
		return ret;
	}
	
	public static String nList(String list) {
		if (list.length() == 3) {
			return "";
		}
		
		return list.substring(2, list.length() - 2);
	}
	
	public static String removeDelay(String str) {
		return str.charAt(0) == ':' ? str.substring(1) : str;
	}
	
	public static String deref(Interpreter gloom, String token) {
		return isReference(token) ? gloom.getReference(token) : token;
	}
	
	public static String concat(String[] list) {
		String sum = "";
		for (String token : list) {
			sum = sum + " " + token;
		}
		return sum.substring(1);
	}
	
	public static String concat(String[] list, int begin, int end) {
		String sum = "";
		for (int i = begin; i < end; ++i) {
			sum = sum + " " + list[i];
		}
		return sum.substring(1);
	}
}

class TokenError extends RuntimeException {
	String errorMsg;
	
	public TokenError(String msg) {
		errorMsg = msg;
	}
	
	public String what() {
		return errorMsg;
	}
}

class Evaluate extends TokenBase {
	public Evaluate(String token, int needs) {
		super(token, needs);
	}
	
	protected int countArgs(String func) {
		int i = 1;
		
		while (func.contains("$" + i)) {
			++i;
		}
		
		return i;
	}
	
	void run(Interpreter gloom, Stack<String> main, Stack<String> retain) {
		super.run(gloom, main, retain);
		
		String toEval = deref(gloom, main.pop());
		
		if ("list".equals(type(toEval))) {
			toEval = nList(toEval);
		}
		
		int nArgs = countArgs(toEval);
		
		for (int i = 1; i < nArgs; ++i) {
			toEval = toEval.replace("$" + i, main.top());
			main.pop();
		}
		
		gloom.execute(toEval);
	}
}

class Size extends TokenBase {
	public Size(String token, int needs) {
		super(token, needs);
	}
	
	void run(Interpreter gloom, Stack<String> main, Stack<String> retain) {
		super.run(gloom, main, retain);
		
		if (hasType(main.top(), "list")) {
			main.push("" + (deref(gloom, main.pop()).split(" ").length - 2));
		} else {
			main.push(String.format("Error in %s: Not defined for value %s of type %s", getToken(), main.top(), type(main.pop())));
		}
	}
}

class Get extends TokenBase {
	public Get(String token, int needs) {
		super(token, needs);
	}
	
	void run(Interpreter gloom, Stack<String> main, Stack<String> retain) {
		super.run(gloom, main, retain);
		
		if (hasType(main.top(), "list")) {
			main.push(deref(gloom, main.pop()).split(" ")[toInt(main.pop()) + 1]);
		} else {
			main.push(String.format("Error in %s: Not defined for value %s of type %s", getToken(), main.top(), type(main.pop())));
		}
	}
}

class Set extends TokenBase {
	public Set(String token, int needs) {
		super(token, needs);
	}
	
	void run(Interpreter gloom, Stack<String> main, Stack<String> retain) {
		super.run(gloom, main, retain);
		
		
		if (hasType(main.top(), "list")) {
			String ref = main.pop();
			boolean isRef = hasType(ref, "reference");
			
			String[] sett = deref(gloom, ref).split(" ");
			sett[toInt(main.pop()) + 1] = main.pop();
			
			if (isRef) {
				gloom.setReference(ref, concat(sett));
			} else {
				main.push(concat(sett));
			}
		} else {
			main.push(String.format("Error in %s: Not defined for value %s of type %s", getToken(), main.top(), type(main.pop())));
		}
	}
}

class Remove extends TokenBase {
	public Remove(String token, int needs) {
		super(token, needs);
	}
	
	void run(Interpreter gloom, Stack<String> main, Stack<String> retain) {
		super.run(gloom, main, retain);
		
		if (hasType(main.top(), "list")) {
			String ref = main.pop();
			boolean isRef = hasType(ref, "reference");
			
			String[] sett = deref(gloom, ref).split(" ");
			int spot = toInt(main.pop()) + 1;
			
			if (isRef) {
				gloom.setReference(ref, concat(sett, 0, spot) + " " + concat(sett, spot + 1, sett.length));
			}
			
			main.push(sett[spot]);
		} else {
			main.push(String.format("Error in %s: Not defined for value %s of type %s", getToken(), main.top(), type(main.pop())));
		}
	}
}

class Insert extends TokenBase {
	public Insert(String token, int needs) {
		super(token, needs);
	}
	
	void run(Interpreter gloom, Stack<String> main, Stack<String> retain) {
		super.run(gloom, main, retain);
		
		if (hasType(main.top(), "list")) {
			String ref = main.pop();
			boolean isRef = hasType(ref, "reference");
			
			String[] sett = deref(gloom, ref).split(" ");
			int spot = toInt(main.pop()) + 1;
			
			if (isRef) {
				gloom.setReference(ref, concat(sett, 0, spot) + " " + main.pop() + " " + concat(sett, spot, sett.length));
			} else {
				main.push(concat(sett, 0, spot) + " " + main.pop() + " " + concat(sett, spot, sett.length));
			}
		} else {
			main.push(String.format("Error in %s: Not defined for value %s of type %s", getToken(), main.top(), type(main.pop())));
		}
	}
}

class Append extends TokenBase {
	public Append(String token, int needs) {
		super(token, needs);
	}
	
	void run(Interpreter gloom, Stack<String> main, Stack<String> retain) {
		super.run(gloom, main, retain);
		
		if (hasType(main.top(), "list")) {
			String[] list1 = deref(gloom, main.pop()).split(" ");
			if (hasType(main.top(), "list")) {
				String[] list2 = deref(gloom, main.pop()).split(" ");
				main.push(gloom.newReference(concat(list2, 0, list2.length - 1) + " " + concat(list1, 1, list1.length)));
				return;
			}
		}
		
		main.push(String.format("Error in %s: Not defined for value %s of type %s", getToken(), main.top(), type(main.pop())));
	}
}

class Copy extends TokenBase {
	public Copy(String token, int needs) {
		super(token, needs);
	}
	
	void run(Interpreter gloom, Stack<String> main, Stack<String> retain) {
		super.run(gloom, main, retain);
		
		if (hasType(main.top(), "list")) {
			main.push(gloom.newReference(deref(gloom, main.pop())));
		} else {
			main.push(String.format("Error in %s: Not defined for value %s of type %s", getToken(), main.top(), type(main.pop())));
		}
	}
}

// Functions & Variables
class Def extends TokenBase {
	public Def(String token, int needs) {
		super(token, needs);
	}
	
	protected int countArgs(String func) {
		int i = 0;
		do {
			++i;
		} while (func.contains("$" + i));
		return i;
	}
	
	void run(Interpreter gloom, Stack<String> main, Stack<String> retain) {
		super.run(gloom, main, retain);
		
		String body = deref(gloom, main.pop());
		String names = deref(gloom, main.pop());
		
		for (String name : names.split(" ")) {
			gloom.addVariable(new Variable(body, removeDelay(name), countArgs(body) - 1));
		}
	}
}

class Variable extends TokenBase {
	private String body;
	
	public Variable(String body, String token, int needs) {
		super(token, needs);
		this.body = body;
	}
	
	void run(Interpreter gloom, Stack<String> main, Stack<String> retain) {
		super.run(gloom, main, retain);
		
		gloom.execute(body + " eval");
	}
}

// Control Flow
class Loop extends TokenBase {
	public Loop(String token, int needs) {
		super(token, needs);
	}
	
	void run(Interpreter gloom, Stack<String> main, Stack<String> retain) {
		super.run(gloom, main, retain);
		
		String body = main.pop();
		
		do {
			gloom.execute(body + " eval");
		} while (main.pop().equals("-1"));
	}
}

class If extends TokenBase {
	public If(String token, int needs) {
		super(token, needs);
	}
	
	
	void run(Interpreter gloom, Stack<String> main, Stack<String> retain) {
		super.run(gloom, main, retain);
		
		String ifFalse = isReference(main.top()) ? gloom.getReference(main.pop()) : main.pop();
		String ifTrue = isReference(main.top()) ? gloom.getReference(main.pop()) : main.pop();

		gloom.execute("eval");
		
		switch (main.pop()) {
			case "-1":
				gloom.execute(ifTrue + " eval");
				break;
			case "0":
				gloom.execute(ifFalse + " eval");
				break;
			default:
				return;
		}
	}
}

// Math classes
abstract class Math extends TokenBase {
	public Math(String token, int needs) {
		super(token, needs);
	}
	
	abstract String op(String left, String right);
	abstract boolean definedFor(String type, boolean lhs);
	
	void run(Interpreter gloom, Stack<String> main, Stack<String> retain) {
		super.run(gloom, main, retain);
		
		if (definedFor(type(main.top()), true)) {
			String right = main.pop();
			
			if (definedFor(type(main.top()), false)) {
				main.push(op(main.pop(), right));
				return;
				
			}
		}
		
		main.push(String.format("Error in %s: Not defined for value %s of type %s", getToken(), main.top(), type(main.pop())));
	}
}

class Add extends Math {
	public Add(String token, int needs) {
		super(token, needs);
	}
	
	String op(String left, String right) {
		return "" + (toInt(left) + toInt(right));
	}
	
	boolean definedFor(String type, boolean lhs) {
		return "int".equals(type);
	}
	
}

class Sub extends Math {
	public Sub(String token, int needs) {
		super(token, needs);
	}
	
	String op(String left, String right) {
		return "" + (toInt(left) - toInt(right));
	}
	
	boolean definedFor(String type, boolean lhs) {
		return "int".equals(type);
	}
}

class Mult extends Math {
	public Mult(String token, int needs) {
		super(token, needs);
	}
	
	String op(String left, String right) {
		return "" + (toInt(left) * toInt(right));
	}
	
	boolean definedFor(String type, boolean lhs) {
		return "int".equals(type);
	}
}

class Div extends Math {
	public Div(String token, int needs) {
		super(token, needs);
	}
	
	String op(String left, String right) {
		return "" + (toInt(left) / toInt(right));
	}
	
	boolean definedFor(String type, boolean lhs) {
		return "int".equals(type);
	}
}

class Mod extends Math {
	public Mod(String token, int needs) {
		super(token, needs);
	}
	
	String op(String left, String right) {
		int rhs = toInt(right);
		
		return "" + (((toInt(left) % rhs) + rhs) % rhs);
	}
	
	boolean definedFor(String type, boolean lhs) {
		return "int".equals(type);
	}
}

// Comparison classes
abstract class Compare extends TokenBase {
	public Compare(String token, int needs) {
		super(token, needs);
	}
}

class Greater extends TokenBase /* Compare */ {
	public Greater(String token, int needs) {
		super(token, needs);
	}
	
	void run(Interpreter gloom, Stack<String> main, Stack<String> retain) {
		super.run(gloom, main, retain);
		
		main.push((toInt(main.pop()) < toInt(main.pop())) ? "-1" : "0");
	}
}

// Retainer stack interaction classes
class ToRetain extends TokenBase {
	public ToRetain(String token, int needs) {
		super(token, needs);
	}
	
	void run(Interpreter gloom, Stack<String> main, Stack<String> retain) {
		super.run(gloom, main, retain);
		
		retain.push(main.pop());
	}
}

class ToMain extends TokenBase {
	public ToMain(String token, int needs) {
		super(token, needs);
	}
	
	void run(Interpreter gloom, Stack<String> main, Stack<String> retain) {
		super.run(gloom, retain, main);
		
		main.push(retain.pop());
	}
}

// Other Implementation Classes
class Rep extends TokenBase {
	public Rep(String token, int needs) {
		super(token, needs);
	}
	
	void run(Interpreter gloom, Stack<String> main, Stack<String> retain) {
		int n = toInt(main.pop());
		String body = main.pop();
		
		for (int i = 0; i < n; ++i) {
			gloom.execute(body + " eval");
		}
	}
}

// Type info classes
class Type extends TokenBase {
	public Type(String token, int needs) {
		super(token, needs);
	}
	
	void run(Interpreter gloom, Stack<String> main, Stack<String> retain) {
		super.run(gloom, main, retain);
		
		main.push(type(main.top()));
	}
}

class TypeCheck extends TokenBase {
	String predicate;
	
	private String name(String token) {
		switch (token) {
			case "ref":
				return "reference";
			default:
				return token;
		}
	}

	public TypeCheck(String token, int needs) {
		super(token, needs);
		predicate = name(token.substring(0, token.length() - 1));			// type check operator assumes token is type + "?"
	}
	
	void run(Interpreter gloom, Stack<String> main, Stack<String> retain) {
		super.run(gloom, main, retain);
		
		main.push(hasType(main.pop(), predicate) ? "-1" : "0");
	}
}

class Import extends TokenBase {
	public Import(String token, int needs) {
		super(token, needs);
	}
	
	void run(Interpreter gloom, Stack<String> main, Stack<String> retain) {
		super.run(gloom, main, retain);
		
		String filename = main.pop();
		try {
			gloom.loadFile(filename);
		} catch (FileNotFoundException e) {
			throw new TokenError(String.format("Error in %s: File % not found", getToken(), filename));
		}
	}
}

class DebugOp extends TokenBase {
	private String operation;
	
	public DebugOp(String token, int needs) {
		super(token, needs);
		operation = token.split("\\.")[1];
	}
	
	void run(Interpreter gloom, Stack<String> main, Stack<String> retain) {
		super.run(gloom, main, retain);
		
		switch(operation) {
			case "size":
				String stack = main.pop();
				if ("main".equals(stack)) {
					main.push("" + main.size());
				} else {
					main.push("" + retain.size());
				}
				break;
			case "remove":
				int n = toInt(main.pop());
				
				if (n < 0) {
					throw new TokenError("You can't remove what's not there!");
				}
				
				while (n-- > 0) {
					main.pop();
				}
				break;
			default:
				break;
		}
	}
}

enum Operation {
 	// List Library
	SIZE(new Size("size", 1)),
	COPY(new Copy("copy", 1)),
	GET(new Get("get", 2)),
	SET(new Set("set", 3)),
	REMOVE(new Remove("remove", 2)),
	INSERT(new Insert("insert", 3)),
	APPEND(new Append("append", 2)),
	
	// Control Flow
	REPLICATE(new Rep("rep", 2)),
	LOOP(new Loop("loop", 1)),
	IF(new If("if", 3)),
	
	// Variable Creators
	//LET(new Let("!!", 2)),
	DEFINE(new Def("!", 2)),
	EVAL(new Evaluate("eval", 1)),
	
	// Math Library
	PLUS(new Add("+", 2)),
	SUB(new Sub("-", 2)),
	MULT(new Mult("*", 2)),
	DIV(new Div("/", 2)),
	MOD(new Mod("mod", 2)),
	MODI(new Variable("mod", "%", 2)),
	POWI(new Variable("math.pow", "^", 2)),
	POW(new Variable("[ $2 $2 [ over * ] $1 1 - rep swap drop ]", "math.pow", 2)),
	MAX(new Variable("[ $2 $1 > $2 $1 if ]", "math.max", 2)),
	MIN(new Variable("[ $2 $1 > $1 $2 if ]", "math.min", 2)),
	
	// Stack Operators
	DUP(new Variable("[ $1 $1 ]", "dup", 1)),
	OVER(new Variable("[ $2 $1 $2 ]", "over", 2)),
	SWAP(new Variable("[ $1 $2 ]", "swap", 2)),
	DROP(new Variable("[ [ @ ] $1 ! ]", "drop", 1)),
	RETAIN(new ToRetain(">r", 1)),
	USE(new ToMain("r>", 1)),
	
	// Boolean Algebra
	GT(new Greater(">", 2)),
	LT(new Variable("[ $1 $2 > ]", "<", 2)),
	GTE(new Variable("[ $2 $1 > t [ $2 $1 = ] if ]", ">=", 2)),
	LTE(new Variable("[ $1 $2 > t [ $2 $1 = ] if ]", "<=", 2)),
	VEQ(new Variable("[ $2 $1 > not $1 $2 > not and ]", "=", 2)),
	NEQ(new Variable("[ = not ]", "!=", 2)),
	NOT(new Variable("[ $1 f t if ]", "not", 1)),
	AND(new Variable("[ $1 $2 f if ]", "and", 2)),
	OR(new Variable("[ $1 t $2 if ]", "or", 2)),
	XOR(new Variable("[ $1 $2 or $1 $2 nand and ]", "xor", 2)),
	NAND(new Variable("[ and not ]", "nand", 2)),
	NOR(new Variable("[ or not ]", "nor", 2)),
	XNOR(new Variable("[ xor not ]", "xnor", 2)),
	TRUE(new Variable("-1", "t", 0)),
	FALSE(new Variable("0", "f", 0)),
	
	// Debug Library
	STACK(new DebugOp("debug.size", 1)),
	DREMOVE(new DebugOp("debug.remove", 1)),
	CLEAR(new Variable("[ main debug.size debug.remove ]", "debug.clear", 0)),
	IMPORT(new Import("import", 1)),
	
	// Type information
	TYPE(new Type("type", 1)),
	INTQ(new TypeCheck("int?", 1)),
	LISTQ(new TypeCheck("list?", 1));
	
	//EQ(new Equal("=", 2)), // There might be times where VEQ does not hold
	//IOTA(new Variable("[ $$1 $$1 0 > [ $$1 1 - -1 ] [ 0 ]1 if ] loop ]", "iota", 1)),
	//FOR(new Variable("[ [ $$1 $3 > [ $1 eval $$1 $2 - -1 ] [ 0 ] if ] loop ]", "for", 3));
	
	public String token;
	public TokenBase instruction;
	
	Operation(TokenBase inst) {
		token = inst.getToken();
		instruction = inst;
	}
}