package compiler;

import java.util.*;
import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;

public class SymbolTableASTVisitor extends BaseASTVisitor<Void,VoidException> {

	private final List<Map<String, STentry>> symTable = new ArrayList<>();
	private final Map<String, Map<String, STentry>> classTable = new HashMap<>();
	private int nestingLevel=0; // current nesting level
	private int decOffset=-2; // counter for offset of local declarations at current nesting level
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
		//rimuovere la hashmap corrente uscendo dallo scope
		symTable.remove(nestingLevel--);
		decOffset=prevNLDecOffset; // restores counter for offset of declarations at previous nesting level
		return null;
	}

	@Override
	public Void visitNode(VarNode n) {
		if (print) printNode(n);
		visit(n.exp);
		Map<String, STentry> hm = symTable.get(nestingLevel);
		STentry entry = new STentry(nestingLevel,n.getType(),decOffset--);
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
	public Void visitNode(TimesNode n) {
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
	//--------------------------- OPERATOR EXTENSIONS ------------------------------------
	@Override
	public Void visitNode(MinusNode n){
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return  null;
	}

	@Override
	public Void visitNode(DivNode n){
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return  null;
	}

	@Override
	public Void visitNode(OrNode n){
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return  null;
	}

	@Override
	public Void visitNode(AndNode n){
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return  null;
	}

	@Override
	public Void visitNode(GreaterEqualNode n){
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return  null;
	}

	@Override
	public Void visitNode(LessEqualNode n){
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return  null;
	}

	@Override
	public Void visitNode(NotNode n){
		if (print) printNode(n);
		visit(n.right);
		return  null;
	}

	//--------------------------- OPERATOR EXTENSIONS ------------------------------------
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

	/* ----------------- Obj Oriented Extension ----------------------*/
	@Override
	public Void visitNode(ClassNode n) throws VoidException {
		if (print) printNode(n);

		Set<String> fieldsAndMethods = new HashSet<>(); // optimization 1
		Map<String, STentry> globalSymTable = symTable.getFirst();
		List<TypeNode> allFields = new ArrayList<>();
		List<ArrowTypeNode> allMethods = new ArrayList<>();

		// handle super-class
		if (n.superId != null) {
			STentry superClassEntry = globalSymTable.get(n.superId);
			n.superEntry = superClassEntry;
			ClassTypeNode classType = (ClassTypeNode) superClassEntry.type;
			allFields.addAll(classType.fields);
			allMethods.addAll(classType.methods);
		}

		STentry entry = new STentry(0, new ClassTypeNode(allFields, allMethods), decOffset--);
		n.setType(entry.type);

		if (globalSymTable.put(n.id, entry) != null) {
			System.out.println("Class id " + n.id + " at line "+ n.getLine() +" already declared");
			stErrors++;
		}

		// init virtual table
		nestingLevel++;
		Map<String, STentry> virtualTable = new HashMap<>();
		if (n.superId != null) {
			virtualTable.putAll(classTable.get(n.superId));
		}
		symTable.add(virtualTable);
		classTable.put(n.id, virtualTable);

		// fields
		virtualTable = symTable.get(nestingLevel);
		int fieldOffset = -allFields.size() - 1;
		Set<String> newFields = new HashSet<>();

		for (FieldNode field : n.fields) {
			if (print) printNode(field);
			if (fieldsAndMethods.contains(field.id)) {
				System.out.println("Field or Method " + field.id + " at line " + field.getLine() + " already declared");
				stErrors++;
				} else {
				fieldsAndMethods.add(field.id);
				STentry oldEntry = virtualTable.get(field.id);
				STentry fieldEntry = makeFieldEntry(field, oldEntry, fieldOffset--);
				field.offset = fieldEntry.offset;
				virtualTable.put(field.id, fieldEntry);
				allFields.add(-fieldEntry.offset - 1, field.getType());
			}
		}
		int prevNLDecOffset = decOffset;
		decOffset = allMethods.size();

		// methods
		for (MethodNode method : n.methods) {
			if (fieldsAndMethods.contains(method.id)) {
				System.out.println("Method id " + method.id + " at line " + method.getLine() + " already declared");
				stErrors++;
				} else {
				fieldsAndMethods.add(method.id);
				visit(method);
				allMethods.add(method.offset, (ArrowTypeNode) method.getType());
			}
		}

		symTable.remove(nestingLevel--);
		decOffset = prevNLDecOffset;

		return null;
	}

	private STentry makeFieldEntry(FieldNode field, STentry oldEntry, int fieldOffset) {
		if (oldEntry == null) {
			return new STentry(nestingLevel, field.getType(), fieldOffset);
		}
		if (oldEntry.type instanceof ArrowTypeNode) {
			System.out.println("Cannot override method " + field.id + "() at line " + field.getLine() + " with a field ");
			stErrors++;
		}
		return new STentry(nestingLevel, field.getType(), oldEntry.offset);
	}


	@Override
	public Void visitNode(MethodNode n) throws VoidException {
		if (print) printNode(n);

		Map<String, STentry> virtualTable = symTable.get(nestingLevel);
		List<TypeNode> parTypes = extractParameterTypes(n);
		STentry methodEntry = createOrRetrieveMethodEntry(n, virtualTable, parTypes);

		n.offset = methodEntry.offset;
		n.setType(methodEntry.type);
		virtualTable.put(n.id, methodEntry);

		processMethodScope(n);
		return null;
	}

	private List<TypeNode> extractParameterTypes(MethodNode n) {
		List<TypeNode> parTypes = new ArrayList<>();
		for (ParNode par : n.parlist) {
			parTypes.add(par.getType());
		}
		return parTypes;
	}

	private STentry createOrRetrieveMethodEntry(MethodNode n, Map<String, STentry> virtualTable, List<TypeNode> parTypes) {
		STentry oldEntry = virtualTable.get(n.id);
		if (oldEntry == null) {
			return new STentry(nestingLevel, new ArrowTypeNode(parTypes, n.retType), decOffset++);
		} else {
			if (!(oldEntry.type instanceof ArrowTypeNode)) {
				System.out.println("Cannot override field " + n.id + " at line " + n.getLine() + " with method " + n.id + "()");
				stErrors++;

			}
			return new STentry(nestingLevel, oldEntry.type, oldEntry.offset);
		}
	}

	private void processMethodScope(MethodNode n) {
		nestingLevel++;
		Map<String, STentry> methodScope = new HashMap<>();
		symTable.add(methodScope);

		int prevNLDecOffset = decOffset;
		decOffset = -2;

		declareParameters(n, methodScope);
		for (Node dec : n.declist) {
			visit(dec);
		}
		visit(n.exp);

		symTable.remove(nestingLevel--);
		decOffset = prevNLDecOffset;
	}

	private void declareParameters(MethodNode n, Map<String, STentry> methodScope) {
		int parOffset = 1;
		for (ParNode par : n.parlist) {
			if (methodScope.put(par.id, new STentry(nestingLevel, par.getType(), parOffset++)) != null) {
				System.out.println("Par id " + par.id + " at line " + n.getLine() + " already declared");
				stErrors++;
			}
		}
	}
	@Override
	public Void visitNode(ClassCallNode n) throws VoidException {
		if (print) printNode(n);

		STentry entry = stLookup(n.classId);
		if (entry != null) {
			if (entry.type instanceof RefTypeNode) {
				n.entry = entry;
				n.nestingLevel = nestingLevel;

				String classId = ((RefTypeNode) entry.type).className;
				STentry methodEntry = classTable.get(classId).get(n.methodId);
				if (methodEntry != null) {
					n.methodEntry = methodEntry;
				} else {
					System.out.println("Method id " + n.classId + "." + n.methodId + " at line " + n.getLine() + " not declared");
					stErrors++;
					}
			} else {
				System.out.println("Reference id " + n.classId + " at line " + n.getLine() + " is not a RefType");
				stErrors++;
			}
		} else {
			System.out.println("Reference id " + n.classId + " at line " + n.getLine() + " not declared");
			stErrors++;
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
		}
		else {
			System.out.println("Class id " + n.classId + " at line " + n.getLine() + " not declared");
			stErrors++;
		}
		n.argList.forEach(this::visit);
		return null;
	}
	@Override
	public Void visitNode(EmptyNode n){
		if (print) printNode(n);
		return  null;
	}
}
