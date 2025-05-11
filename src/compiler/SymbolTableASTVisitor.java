package compiler;

import java.util.*;
import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;

public class SymbolTableASTVisitor extends BaseASTVisitor<Void,VoidException> {
	private Map<String, Map<String, STentry>> classTable = new HashMap<>();
	private List<Map<String, STentry>> symTable = new ArrayList<>();
	private int nestingLevel=0; // current nesting level
	private int decOffset=-2; // counter for offset of local declarations at current nesting level
	int stErrors=0;

	SymbolTableASTVisitor() {}
	SymbolTableASTVisitor(boolean debug) {print = debug;} // enables print for debugging

	private STentry stLookup(String id) {
		int j = nestingLevel;
		STentry entry = null;
		while (j >= 0 && entry == null)
			entry = symTable.get(j--).get(id);
		return entry;
	}

	@Override
	public Void visitNode(ProgLetInNode n) {
		if (print) printNode(n);
		Map<String, STentry> hm = new HashMap<>();
		symTable.add(hm);
	    for (Node dec : n.declist) visit(dec);
		visit(n.exp);
		symTable.remove(0);
		return null;
	}

	@Override
	public Void visitNode(ProgNode n) {
		if (print) printNode(n);
		visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(FunNode n) {
		if (print) printNode(n);
		Map<String, STentry> hm = symTable.get(nestingLevel);
		List<TypeNode> parTypes = new ArrayList<>();
		for (ParNode par : n.parlist) parTypes.add(par.getType());
		STentry entry = new STentry(nestingLevel, new ArrowTypeNode(parTypes, n.retType), decOffset--);
		//inserimento di ID nella symtable
		if (hm.put(n.id, entry) != null) {
			System.out.println("Fun id " + n.id + " at line "+ n.getLine() +" already declared");
			stErrors++;
		}
		//creare una nuova hashmap per la symTable
		nestingLevel++;
		Map<String, STentry> hmn = new HashMap<>();
		symTable.add(hmn);
		int prevNLDecOffset=decOffset; // stores counter for offset of declarations at previous nesting level
		decOffset=-2;

		int parOffset=1;
		for (ParNode par : n.parlist)
			if (hmn.put(par.id, new STentry(nestingLevel, par.getType(), parOffset++)) != null) {
				System.out.println("Par id " + par.id + " at line "+ n.getLine() +" already declared");
				stErrors++;
			}
		for (Node dec : n.declist) visit(dec);
		visit(n.exp);
		//rimuovere la hashmap corrente poiche' esco dallo scope
		symTable.remove(nestingLevel--);
		decOffset=prevNLDecOffset; // restores counter for offset of declarations at previous nesting level
		return null;
	}

	@Override
	public Void visitNode(VarNode n) {
		if (print) printNode(n);
		visit(n.exp);
		Map<String, STentry> hm = symTable.get(nestingLevel);
		STentry entry = new STentry(nestingLevel, n.getType(), decOffset--);
		//inserimento di ID nella symtable
		if (hm.put(n.id, entry) != null) {
			System.out.println("Var id " + n.id + " at line "+ n.getLine() +" already declared");
			stErrors++;
		}
		return null;
	}

	@Override
	public Void visitNode(PrintNode n) {
		if (print) printNode(n);
		visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(IfNode n) {
		if (print) printNode(n);
		visit(n.cond);
		visit(n.th);
		visit(n.el);
		return null;
	}

	@Override
	public Void visitNode(EqualNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(GreaterEqualNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(LessEqualNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(TimesNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(DivNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(PlusNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(MinusNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(AndNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(NotNode n) {
		if (print) printNode(n);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(OrNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(CallNode n) {
		if (print) printNode(n);
		STentry entry = stLookup(n.id);
		if (entry == null) {
			System.out.println("Fun id " + n.id + " at line "+ n.getLine() + " not declared");
			stErrors++;
		} else {
			n.entry = entry;
			n.nl = nestingLevel;
		}
		for (Node arg : n.arglist) visit(arg);
		return null;
	}

	@Override
	public Void visitNode(IdNode n) {
		if (print) printNode(n);
		STentry entry = stLookup(n.id);
		if (entry == null) {
			System.out.println("Var or Par id " + n.id + " at line "+ n.getLine() + " not declared");
			stErrors++;
		} else {
			n.entry = entry;
			n.nestingLevel = nestingLevel;
		}
		return null;
	}

	@Override
	public Void visitNode(BoolNode n) {
		if (print) printNode(n, n.val.toString());
		return null;
	}

	@Override
	public Void visitNode(IntNode n) {
		if (print) printNode(n, n.val.toString());
		return null;
	}

	@Override
	public Void visitNode(ClassNode n) {
		if (print) printNode(n);
		// new classTypeNode with empty methods and fields
		ClassTypeNode classTypeNode = new ClassTypeNode();
		STentry entry = new STentry(0, classTypeNode, decOffset--);
		Map<String, STentry> virtualTable = new HashMap<>();
		int fieldsOffset = -1;
		final Set<String> fieldsName = new HashSet<>();
		final Set<String> methodsName = new HashSet<>();

		if (symTable.getFirst().put(n.id, entry) != null) {
			System.out.println("Class id " + n.id + " at line " + n.getLine() + " already declared");
			stErrors++;
		}

		if (n.superId != null) {
			STentry superClassEntry = symTable.getFirst().get(n.superId); // Look up superclass in global table
			if (superClassEntry == null) {
				System.out.println("Superclass id " + n.superId + " at line " + n.getLine() + " not declared.");
				stErrors++;
				// virtualTable remains a new empty HashMap
			} else if (superClassEntry.type instanceof ClassTypeNode) {
				Map<String, STentry> superVirtualTable = classTable.get(n.superId);
				if (superVirtualTable != null) {
					virtualTable = new HashMap<>(superVirtualTable); // Create a copy
					for (String id : superVirtualTable.keySet()) {
						if (superVirtualTable.get(id).type instanceof ArrowTypeNode) {
							classTypeNode.methods.add((ArrowTypeNode) superVirtualTable.get(id).type);
						} else {
							classTypeNode.fields.add( superVirtualTable.get(id).type);
						}
					}
				} else {
					// This indicates an earlier issue if a declared class doesn't have its VT in classTable
					System.out.println("Internal error: Superclass " + n.superId + " virtual table not found in classTable at line " + n.getLine());
					stErrors++;
					// virtualTable remains a new empty HashMap
				}
			} else {
				System.out.println("Superclass id " + n.superId + " at line " + n.getLine() + " is not a class type.");
				stErrors++;
				// virtualTable remains a new empty HashMap
			}
		}

		classTable.put(n.id, virtualTable);
		symTable.add(virtualTable);
		nestingLevel++;


		for (FieldNode field : n.fields) {
			if (fieldsName.contains(field.id)) {
				System.out.println("Field " + field.id + " at line " + n.getLine() + " already declared");
				stErrors++;
			} else {
				fieldsName.add(field.id);
			}
			visit(field);
			STentry fieldEntry = new STentry(nestingLevel, field.getType(), fieldsOffset--);
			classTypeNode.fields.add(-fieldEntry.offset - 1, fieldEntry.type);
			virtualTable.put(field.id, fieldEntry);
		}




		int prevOffset = decOffset;
		decOffset = 0;

		for (MethodNode method : n.methods) {
			if (methodsName.contains(method.id)) {
				System.out.println("Method id " + method.id + " at line " + n.getLine() + " already declared");
				stErrors++;
			} else {
				methodsName.add(method.id);
			}
			visit(method);
			ArrowTypeNode methodType = (ArrowTypeNode) symTable.get(nestingLevel).get(method.id).type;
			classTypeNode.methods.add(method.offset, methodType);
		}
		decOffset = prevOffset;
		symTable.remove(nestingLevel--);
		return null;
	}

	@Override
	public Void visitNode(FieldNode n) {
		if (print) printNode(n);
		return null;
	}

	@Override
	public Void visitNode(MethodNode n) {
		if (print) printNode(n);
		Map<String, STentry> hm = symTable.get(nestingLevel);
		List<TypeNode> parTypes = new ArrayList<>();
		for (ParNode par : n.parlist) parTypes.add(par.getType());
		STentry entry = new STentry(nestingLevel, new ArrowTypeNode(parTypes,n.retType), decOffset++);
		//inserimento di ID nella symtable
		hm.put(n.id, entry);
		//creare una nuova hashmap per la symTable
		nestingLevel++;
		Map<String, STentry> hmn = new HashMap<>();
		symTable.add( hmn );
		int prevNLDecOffset = decOffset; // stores counter for offset of declarations at previous nesting level
		decOffset=-2;

		int parOffset=1;
		for ( ParNode par : n.parlist )
			if ( hmn.put(par.id, new STentry(nestingLevel, par.getType(), parOffset++)) != null ) {
				System.out.println("Par id " + par.id + " at line "+ n.getLine() +" already declared");
				stErrors++;
			}
		for ( Node dec : n.declist ) visit( dec );
		visit( n.exp );
		//rimuove la hashmap corrente => esco dallo scope
		symTable.remove( nestingLevel-- );
		decOffset = prevNLDecOffset; // restores counter for offset of declarations at previous nesting level
		return null;
	}

	@Override
	public Void visitNode(ClassCallNode n) {
		if (print) printNode(n);

		STentry entry = stLookup(n.classId);

		if (entry == null) {
			System.out.println("Class id  " + n.classId + " at line " + n.getLine() + " not declared");
			stErrors++;
			return null;
		} else {
			n.entry = entry;
			n.nestingLevel = nestingLevel;
		}

		RefTypeNode ref;
		if (!(entry.type instanceof RefTypeNode)) {
			System.out.println(n.classId + " at line " + n.getLine() + " is not a class");
			stErrors++;
			return null;
		}
		ref = (RefTypeNode) entry.type;


		Map<String, STentry> virtualTable = classTable.get(ref.className);



		STentry methodEntry = virtualTable.get(n.methodId);
		if (methodEntry == null) {
			System.out.println("Method id " + n.classId + " at line " + n.getLine() + " not declared in class " + ref.className);
			stErrors++;
			return null;
		}

		n.methodEntry = methodEntry;

		for (Node arg : n.argList) visit(arg);
		return null;
	}

	@Override
	public Void visitNode(NewNode n) {
		if (print) printNode(n);
		// Retrieve the STentry of the class ID from the class table
		Map<String, STentry> virtualTable = classTable.get(n.classId);
		if (virtualTable == null) {
			System.out.println("Class id " + n.classId + " at line " + n.getLine() + " not declared");
			stErrors++;
			return null;
		}
		// Retrieve the STentry from level 0 of the symbol table
		STentry classEntry = symTable.getFirst().get(n.classId);
		if (classEntry == null) {
			System.out.println("Class " + n.classId + " at line " + n.getLine() + " not found in symbol table");
			stErrors++;
			return null;
		}else {
			n.entry = classEntry;
		}

		for (Node arg : n.argList)
			visit(arg);
		return null;
	}

	@Override
	public Void visitNode(EmptyNode n) {
		if (print) printNode(n);
		return null;
	}
}
