package compiler;

import java.util.*;
import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;

public class SymbolTableASTVisitor extends BaseASTVisitor<Void,VoidException> {

	private final List<Map<String, STentry>> symTable = new ArrayList<>();
	private final Map<String, Map<String, STentry>> classTable = new HashMap<>();
	private int nestingLevel=0; // current nesting level
	private int decOffset=-2; // offset of local declarations => current nesting level
	int stErrors=0;

	SymbolTableASTVisitor() {}
	SymbolTableASTVisitor(boolean debug) {super(debug);} // enables print for debugging

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
		STentry entry = new STentry(nestingLevel, new ArrowTypeNode(parTypes,n.retType),decOffset--);
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
			if (hmn.put(par.id, new STentry(nestingLevel,par.getType(),parOffset++)) != null) {
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
		STentry entry = new STentry(nestingLevel,n.getType(), decOffset--);
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


	// OBJECT EXTENSION

	@Override
	public Void visitNode(ClassNode n) {
		if (print) printNode(n);

		Set<String> fieldsAndMethods = new HashSet<>(); // optimize wrong redefinition of fields and methods
		List<TypeNode> fieldTypeList = new ArrayList<>();
		List<ArrowTypeNode> methodTypeList = new ArrayList<>();

		// super class
		if (n.superId != null) {
			STentry superClassEntry = symTable.getFirst().get(n.superId);
			n.superEntry = superClassEntry;
			ClassTypeNode classType = (ClassTypeNode) superClassEntry.type;
			fieldTypeList.addAll(classType.fields);
			methodTypeList.addAll(classType.methods);
		}
		STentry entry = new STentry(0, new ClassTypeNode(fieldTypeList, methodTypeList), decOffset--);
		n.setType(entry.type);

		if (symTable.getFirst().put(n.id, entry) != null) {
			System.out.println("Class id " + n.id + " at line "+ n.getLine() +" already declared");
			stErrors++;
		}

		// virtual table
		nestingLevel++;
		Map<String, STentry> virtualTable = new HashMap<>();
		if (n.superId != null) {
			virtualTable.putAll(classTable.get(n.superId));
		}
		symTable.add(virtualTable);
		classTable.put(n.id, virtualTable);

		// fields
		virtualTable = symTable.get(nestingLevel);
		int fieldOffset = -fieldTypeList.size() - 1;
		for (FieldNode field : n.fields) {
			if (print) printNode(field);
			if (fieldsAndMethods.contains(field.id)) {
				System.out.println("Field: " + field.id + " at line: " + field.getLine() + " was already declared");
				stErrors++;
				} else {
				fieldsAndMethods.add(field.id);
				STentry superEntry = virtualTable.get(field.id);
				STentry overrideEntry = null;
				if (superEntry == null) {
					overrideEntry = new STentry(nestingLevel, field.getType(), fieldOffset);
				} else {
					if (superEntry.type instanceof ArrowTypeNode) {
					System.out.println("Cannot override method with field in line: " + field.getLine());
					stErrors++;
					}
					overrideEntry =  new STentry(nestingLevel, field.getType(), superEntry.offset);
				}
				field.offset = overrideEntry.offset;
				virtualTable.put(field.id, overrideEntry);
				fieldTypeList.add(-overrideEntry.offset - 1, field.getType());
			}
		}

		// methods
		int prevDecOffset = decOffset;
		decOffset = methodTypeList.size();
		for (MethodNode method : n.methods) {
			if (fieldsAndMethods.contains(method.id)) {
				System.out.println("Method: " + method.id + " at line: " + method.getLine() + " was already declared");
				stErrors++;
				} else {
				fieldsAndMethods.add(method.id);
				visit(method);
				methodTypeList.add(method.offset, (ArrowTypeNode) method.getType());
			}
		}

		symTable.remove(nestingLevel--);
		decOffset = prevDecOffset;
		return null;
	}

	@Override
	public Void visitNode(MethodNode n) {
		if (print) printNode(n);
		Map<String, STentry> virtualTable = symTable.get(nestingLevel);
		List<TypeNode> parameterTypeList = new ArrayList<>();
		for (ParNode par : n.parameterList) {
			parameterTypeList.add(par.getType());
		}
		STentry superEntry = virtualTable.get(n.id);
		STentry methodEntry;
		if (superEntry == null) {
			methodEntry = new STentry(nestingLevel, new ArrowTypeNode(parameterTypeList, n.returnType), decOffset++);
		} else {
			if (!(superEntry.type instanceof ArrowTypeNode)) {
				System.out.println("Cannot override field " + n.id + " at line " + n.getLine() + " with method " + n.id + "()");
				stErrors++;

			}
			methodEntry = new STentry(nestingLevel, superEntry.type, superEntry.offset);
		}
		n.offset = methodEntry.offset;
		n.setType(methodEntry.type);
		virtualTable.put(n.id, methodEntry);
		nestingLevel++;
		Map<String, STentry> methodScope = new HashMap<>();
		symTable.add(methodScope);
		int prevDecOffset = decOffset;
		decOffset = -2;
		int parametersOffset = 1;
		for (ParNode param : n.parameterList) {
			if (methodScope.put(param.id, new STentry(nestingLevel, param.getType(), parametersOffset++)) != null) {
				System.out.println("Parameter: " + param.id + " at line " + n.getLine() + " was already declared");
				stErrors++;
			}
		}
		for (Node dec : n.declarationList) {
			visit(dec);
		}
		visit(n.exp);

		symTable.remove(nestingLevel--);
		decOffset = prevDecOffset;
		return null;
	}

	@Override
	public Void visitNode(ClassCallNode n) {
		if (print) printNode(n);
		STentry entry = stLookup(n.classId);
		if (entry == null) {
			System.out.println(n.classId + " at line " + n.getLine() + " not declared");
			stErrors++;
		} else {
			if (!(entry.type instanceof RefTypeNode)) {
				System.out.println(n.classId + " at line " + n.getLine() + " is not a RefTypeNode");
				stErrors++;
			}
			n.entry = entry;
			n.nestingLevel = nestingLevel;

			String classId = ((RefTypeNode) entry.type).classId;
			STentry methodEntry = classTable.get(classId).get(n.methodId);
			if (methodEntry == null) {
				System.out.println("Method: " + n.methodId + " at line " + n.getLine() + " was not declared");
				stErrors++;
			} else {
				n.methodEntry = methodEntry;
			}
		}
		for (Node arg : n.argList) {
			visit(arg);
		}
		return null;
	}

	@Override
	public Void visitNode(NewNode n) {
		if (print) printNode(n);
		if(this.classTable.containsKey(n.classId)){
			n.entry = symTable.getFirst().get(n.classId);
		} else {
			System.out.println("Class " + n.classId + " at line: " + n.getLine() + " was not declared");
			stErrors++;
		}
		for (Node arg : n.argList) {
			visit(arg);
		}
		return null;
	}
	@Override
	public Void visitNode(EmptyNode n){
		if (print) printNode(n);
		return null;
	}
}
