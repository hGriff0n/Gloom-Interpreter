import java.util.Scanner;
import java.io.*;
import java.util.Map;
import java.util.HashMap;

public class Interpreter {
	private Stack<String> main;										// The main gloom stack
	private Stack<String> retain;									// The retainer stack (For use in function calling as the original standard did not specify an "argument" syntax)
	private List<String> refs;										// When a list is encountered, it is replaced with a reference. The reference refers to the list's index
	private Map<String, TokenBase> variables;						// Map of the variable to it's representation
	private static Map<String, TokenBase> literals;					// Map of literals. This is a system wide object (ie. all Gloom Interpreters share this stack)

	// Small class that handles the importing of new files from within gloom
	private class GloomImportFile extends File {
	    public boolean isGloom;							// if isGloom = false then the imported file is java (though I never got the dynamic loading to work)

	    public GloomImportFile(String filename, boolean gloom) throws FileNotFoundException {
			super(filename);
			isGloom = gloom;
	    }
	}

	public Interpreter() {
		main = new Stack<String>();
		retain = new Stack<String>();
		refs = new List<String>();
		variables = new HashMap<String, TokenBase>();
		setLiterals();
	}
	
	// Initializes the system literals if they are not already set
	private static void setLiterals() {
		if (literals == null) {
			literals = TokenBase.allTokens();
		}
	}
	
	// Translates a gloom reference to its list index
	private int refNumber(String ref) {
		return TokenBase.toInt(ref.substring(1)) - 1;
	}
	
	// Creates a new reference for the given list
	public String newReference(String list) {
		refs.append(list);
		return "&" + refs.size();
	}
	
	// Sets the given reference to point to the new list
	public String setReference(String ref, String list) {
		refs.set(refNumber(ref), list);
		return ref;
	}
	
	// Get the list pointed to by the reference
	public String getReference(String ref) {
		return refs.get(refNumber(ref));
	}
	
	private String collect(Scanner scanner, String recur, String match, boolean addRefs) {
		String list = "";
		
		while (scanner.hasNext()) {
			String token = scanner.next();
			list = list + " ";
			if (token.equals(recur)) {
				token = token + collect(scanner, recur, match, addRefs);
				
				if ("[".equals(recur) && addRefs) {
					token = newReference(token);
				}
				
				list = list + token;
			} else if (token.equals(match)) {
				return list + match;
			} else {
				list = list + token;
			}
		}
		
		return "null";
	}
	
	// Recursively gathers comments
	public void getComment(Scanner scanner) {
		while (scanner.hasNext()) {
			String token = scanner.next();
			
			if (")".equals(token)) {
				return;
			} else if ("(".equals(token)) {
				getComment(scanner);
			}
		}
	}
	
	public List<String> parse(Scanner scanner, boolean addRefs) {
		List<String> processed = new List<>();
		
		while (scanner.hasNext()) {
			String token = scanner.next();
			
			switch (token) {
				case "[":
					token = parse(scanner, addRefs).toString();
					
					if (addRefs) {
						token = newReference(token);
					}
					
					break;
				case "(":
					getComment(scanner);
					token = "null";
					break;
				case "]":
					return processed;
				default:
			}
			
			if (!"null".equals(token)) {
				processed.add(token);
			}
		}
		
		return processed;
	}
	
	private List<String> makeList(Scanner scanner) {
		List<String> list = new List<>();
		
		while (scanner.hasNext()) {
			list.add(scanner.next());
		}
		
		return list;
	}
	
	// Loads, parses, and executes the gloom file
	public void execute(String file) {
		executeCode(parse(new Scanner(file), false));
	}
	
	// Executes a parsed list 
	public void executeCode(List<String> code) {
		for (int i = 0; i < code.size(); ++i) {
			String token = code.get(i);
			
			switch (token.charAt(0)) {
				case '$':
					main.push(token.substring(1));
					break;
				default:
					if (!call(token)) {
						main.push(token);
					}
			}
		}
	}
	
	public boolean call(String function) {
		try {
			if (variables.containsKey(function)) {
				variables.get(function).run(this, main, retain);
			} else if (literals.containsKey(function)) {
				literals.get(function).run(this, main, retain);
			} else {
				return false;
			}
		} catch (TokenError e) {
			main.push(e.what());
		}
		
		return true;
	}
	
	public void addVariable(TokenBase variable) {
		variables.put(variable.getToken(), variable);
	}
	
	public void evaluate(Scanner scanner) {
		executeCode(parse(scanner, true));
	}

	private GloomImportFile findFile(String filename) throws FileNotFoundException {
		if (filename.contains(".class") || filename.contains(".java") || filename.contains(".gloom")) {
		    return new GloomImportFile(filename.replace(".java", ".class"), filename.contains(".gloom"));

		} else {
		    GloomImportFile ret = new GloomImportFile(filename + ".gloom", true);
		    
		    if (!ret.isFile()) {
				return new GloomImportFile(filename + ".class", false);
		    }

		    if (!ret.isFile()) {
				throw new FileNotFoundException();
		    }

		    return ret;
		}
	}
	
	public void loadFile(String filename) throws FileNotFoundException {
		GloomImportFile file = findFile(filename);
		//System.out.println(file.getAbsolutePath());

		// need to add protections
		    // if a file was already imported throws error
		    // i need to override the previous definitions
		if (file.isGloom) {
		    evaluate(new Scanner(file));
		} else if (file.isJava) {
		    try {
				// here's the failure line
				Class<?> klass = Interpreter.class.getClassLoader().loadClass(filename + ".class");

				klass.getMethod("loadLibrary", Interpreter.class).invoke(this);

		    } catch (Exception e) {
				main.push(String.format("Attempt to load library %s failed: Library does not exist", file.getAbsolutePath()));
		    }
		} else {
		    throw new FileNotFoundException();
		}		
	}
	
	public void evaluate(String input) {
		evaluate(new Scanner(input));
	}
	
	public void clear() {
		main.clear();
		retain.clear();
	}
	
	public String expandReferences(String list) {
		String ret = "";
		
		for (String token : list.split(" ")) {			
			if (!"[".equals(token)) {
				if (TokenBase.isReference(token)) {
					token = expandReferences(getReference(token));
				}
			}
			
			ret = ret + " " + token;
		}
			
		return ret.substring(1);
	}
	
	public Stack<?> stack() {
		Stack<String> ret = new Stack<>();
		String rep = expandReferences(main.toString());
		
		if (rep.length() > 3) {
			for (String token : rep.substring(2, rep.length() - 2).split(" ")) {
				ret.push(token);
			}
		}
		
		return ret;
	}
	
	public static void main(String[] args) {
		Scanner input = new Scanner(System.in);
		Interpreter n = new Interpreter();
		
		do {
			n.clear();
			n.evaluate(input.nextLine());
			System.out.println("main: " + n.stack());
			if (n.main.isEmpty()) {
				n.main.push("1");
			}
		} while (!"exit".equals(n.main.top()));
	}
	
}
