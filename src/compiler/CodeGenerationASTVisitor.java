package compiler;

import compiler.AST.*;
import compiler.lib.*;
import compiler.exc.*;
import svm.ExecuteVM;

import java.util.ArrayList;
import java.util.List;

import static compiler.lib.FOOLlib.*;

public class CodeGenerationASTVisitor extends BaseASTVisitor<String, VoidException> {
	List<List<String>> dispatchTables = new ArrayList<>();
	CodeGenerationASTVisitor() {}
	CodeGenerationASTVisitor(boolean debug) {super(false,debug);} //enables print for debugging


	@Override
	public String visitNode(ProgLetInNode n) {
		if (print) printNode(n);
		String declCode = null;
		for (Node dec : n.declist) declCode=nlJoin(declCode,visit(dec));
		return nlJoin(
				"push 0",
				declCode, // generate code for declarations (allocation)
				visit(n.exp),
				"halt",
				getCode()
		);
	}

	@Override
	public String visitNode(ProgNode n) {
		if (print) printNode(n);
		return nlJoin(
				visit(n.exp),
				"halt"
		);
	}

	@Override
	public String visitNode(FunNode n) {
		if (print) printNode(n,n.id);
		String declCode = null, popDecl = null, popParl = null;
		for (Node dec : n.declist) {
			declCode = nlJoin(declCode,visit(dec));
			popDecl = nlJoin(popDecl,"pop");
		}
		for (int i=0;i<n.parlist.size();i++) popParl = nlJoin(popParl,"pop");
		String funl = freshFunLabel();
		putCode(
				nlJoin(
						funl+":",
						"cfp", // set $fp to $sp value
						"lra", // load $ra value
						declCode, // generate code for local declarations (they use the new $fp!!!)
						visit(n.exp), // generate code for function body expression
						"stm", // set $tm to popped value (function result)
						popDecl, // remove local declarations from stack
						"sra", // set $ra to popped value
						"pop", // remove Access Link from stack
						popParl, // remove parameters from stack
						"sfp", // set $fp to popped value (Control Link)
						"ltm", // load $tm value (function result)
						"lra", // load $ra value
						"js"  // jump to to popped address
				)
		);
		return "push "+funl;
	}

	@Override
	public String visitNode(VarNode n) {
		if (print) printNode(n,n.id);
		return visit(n.exp);
	}

	@Override
	public String visitNode(PrintNode n) {
		if (print) printNode(n);
		return nlJoin(
				visit(n.exp),
				"print"
		);
	}

	@Override
	public String visitNode(IfNode n) {
		if (print) printNode(n);
		String l1 = freshLabel();
		String l2 = freshLabel();
		return nlJoin(
				visit(n.cond),
				"push 1",
				"beq "+l1,
				visit(n.el),
				"b "+l2,
				l1+":",
				visit(n.th),
				l2+":"
		);
	}

	@Override
	public String visitNode(EqualNode n) {
		if (print) printNode(n);
		String l1 = freshLabel();
		String l2 = freshLabel();
		return nlJoin(
				visit(n.left),
				visit(n.right),
				"beq " + l1,
				"push 0",
				"b " + l2,
				l1 + ":",
				"push 1",
				l2 + ":"
		);
	}


	@Override
	public String visitNode(GreaterEqualNode n) {
		if (print) printNode(n);
		String l1 = freshLabel();
		String l2 = freshLabel();
		return nlJoin(
				visit(n.right),
				visit(n.left),
				"bleq " + l1,
				"push 0",
				"b " + l2,
				l1 + ":",
				"push 1",
				l2 + ":"
		);
	}


	@Override
	public String visitNode(LessEqualNode n) {
		if (print) printNode(n);
		String l1 = freshLabel();
		String l2 = freshLabel();
		return nlJoin(
				visit(n.left),
				visit(n.right),
				"bleq " + l1,
				"push 0",
				"b " + l2,
				l1 + ":",
				"push 1",
				l2 + ":"
		);
	}

	@Override
	public String visitNode(TimesNode n) {
		if (print) printNode(n);
		return nlJoin(
				visit(n.left),
				visit(n.right),
				"mult"
		);
	}

	@Override
	public String visitNode(DivNode n) {
		if (print) printNode(n);
		return nlJoin(
				visit(n.left),
				visit(n.right),
				"div"
		);
	}

	@Override
	public String visitNode(PlusNode n) {
		if (print) printNode(n);
		return nlJoin(
				visit(n.left),
				visit(n.right),
				"add"
		);
	}

	@Override
	public String visitNode(MinusNode n) {
		if (print) printNode(n);
		return nlJoin(
				visit(n.left),
				visit(n.right),
				"sub"
		);
	}

	@Override
	public String visitNode(AndNode n) {
		if (print) printNode(n);
		String l1 = freshLabel();
		String l2 = freshLabel();
		return nlJoin(
				visit(n.left),
				visit(n.right),
				"mult",
				"push 0",
				"beq " + l1,
				"push 1",
				"b " + l2,
				l1 + ":",
				"push 0",
				l2 + ":"
		);
	}

	@Override
	public String visitNode(OrNode n) {
		if (print) printNode(n);
		String l1 = freshLabel();
		String l2 = freshLabel();
		return nlJoin(
				visit(n.left),
				visit(n.right),
				"add",
				"push 0",
				"beq", l1,
				"push 1",
				"b", l2,
				l1 + ":",
				"push 0",
				l2 + ":"
		);
	}

	@Override
	public String visitNode(NotNode n) {
		if (print) printNode(n);
		String l1 = freshLabel();
		String l2 = freshLabel();
		return nlJoin(
				visit(n.right),
				"push 0",
				"beq", l1,
				"push 0",
				"b", l2,
				l1 + ":",
				"push 1",
				l2 + ":"
		);
	}

	@Override
	public String visitNode(CallNode n) {
		if (print) printNode(n,n.id);
		String argCode = null, getAR = null;
		for (int i=n.arglist.size()-1;i>=0;i--) argCode=nlJoin(argCode,visit(n.arglist.get(i)));
		for (int i = 0;i<n.nl-n.entry.nl;i++) getAR=nlJoin(getAR,"lw");
		return nlJoin(
				"lfp", // load Control Link (pointer to frame of function "id" caller)
				argCode, // generate code for argument expressions in reversed order
				"lfp", getAR, // retrieve address of frame containing "id" declaration
				// by following the static chain (of Access Links)
				"stm", // set $tm to popped value (with the aim of duplicating top of stack)
				"ltm", // load Access Link (pointer to frame of function "id" declaration)
				"ltm", // duplicate top of stack
				"push "+n.entry.offset, "add", // compute address of "id" declaration
				"lw", // load address of "id" function
				"js"  // jump to popped address (saving address of subsequent instruction in $ra)
		);
	}

	@Override
	public String visitNode(IdNode n) {
		if (print) printNode(n,n.id);
		String getAR = null;
		for (int i = 0;i<n.nestingLevel-n.entry.nl;i++) getAR=nlJoin(getAR,"lw");
		return nlJoin(
				"lfp", getAR, // retrieve address of frame containing "id" declaration
				// by following the static chain (of Access Links)
				"push "+n.entry.offset, "add", // compute address of "id" declaration
				"lw" // load value of "id" variable
		);
	}

	@Override
	public String visitNode(BoolNode n) {
		if (print) printNode(n,n.val.toString());
		return "push "+(n.val?1:0);
	}

	@Override
	public String visitNode(IntNode n) {
		if (print) printNode(n,n.val.toString());
		return "push "+n.val;
	}

	@Override
	public String visitNode(ClassNode n){
		if (print) printNode(n, n.id);
		List<String> dispatchTable = new ArrayList<>();

		// retrieve dispatch table of super class if present
		if (n.superId != null && n.superEntry != null) {
			int superPos = -n.superEntry.offset - 2;
			if (superPos >= 0 && superPos < dispatchTables.size()) {
				dispatchTable = new ArrayList<>(dispatchTables.get(superPos));
			}
		}

		// add methods to dispatch table
		for (MethodNode method : n.methods) {
			visit(method);
			if (method.offset < dispatchTable.size()) {
				dispatchTable.set(method.offset, method.label);
			} else {
				while(dispatchTable.size() <= method.offset) {
					dispatchTable.add(null);
				}
				dispatchTable.set(method.offset, method.label);
			}
		}

		// add curr dispatch table to dispatch tables
		dispatchTables.add(dispatchTable);

		String dispTableCode = "";
		for (String label : dispatchTable) {
			dispTableCode = nlJoin(dispTableCode,
					"push " + label,
					"lhp",
					"sw",
					"lhp",
					"push 1",
					"add",
					"shp");
		}
		return nlJoin("lhp", dispTableCode );
	}

	@Override
	public String visitNode(MethodNode n) {
		if (print) printNode(n, n.id);
		String declCode = null, popDecl = null, popParl = null;
		for (Node dec : n.declarationList) {
			declCode = nlJoin(declCode, visit(dec));
			popDecl = nlJoin(popDecl, "pop");
		}

		// remove parameters from stack
		for (int i = 0; i < n.parameterList.size(); i++) {
			popParl = nlJoin(popParl, "pop");
		}

		String funl = freshFunLabel();
		n.label = funl;

		putCode(
				nlJoin(
						funl + ":",
						"cfp", // set $fp to $sp value
						"lra",  // load $ra value
						declCode, // generate code for local declarations (they use the new $fp!!!)
						visit(n.exp), // generate code for function body expression
						"stm", // set $tm to popped value (function result)
						popDecl, // remove local declarations from stack
						"sra", // set $ra to popped value
						"pop", // remove Access Link from stack
						popParl, // remove parameters from stack
						"sfp", // set $fp to popped value (Control Link)
						"ltm", // load $tm value (function result)
						"lra", // load $ra value
						"js"  // jump to to popped address
				)
		);

		return null;
	}
	@Override
	public String visitNode(ClassCallNode n) {
		if (print) printNode(n, n.classId);

		if (n.methodEntry == null) {
			System.err.println("Errore: methodEntry non definito per il metodo " + n.methodId);
			return "push -1"; // return error
		}

		// Parameter code generation
		String argCode = null, getAR = null;
		for (int i = n.argList.size() - 1; i >= 0; i--) {
			argCode = nlJoin(argCode, visit(n.argList.get(i)));
		}

		for (int i = 0; i < n.nestingLevel - n.entry.nl; i++) {
			getAR = nlJoin(getAR, "lw");
		}

		return nlJoin(
				"lfp",
				argCode,
				"lfp", getAR,
				"push " + n.entry.offset,
				"add",
				"lw",
				"stm",
				"ltm",
				"ltm",
				"lw",
				"push " + n.methodEntry.offset,
				"add",
				"lw",
				"js"
		);
	}
	@Override
	public String visitNode(NewNode n) {
		if (print) printNode(n, n.classId);
		String argCode = null, heapCode = null;
		for (Node param : n.argList) {
			argCode = nlJoin(argCode, visit(param));
			heapCode = nlJoin(heapCode,
					"lhp",
					"sw",
					"lhp",
					"push 1",
					"add",
					"shp"
			);
		}
		return nlJoin(
				argCode,
				heapCode,
				"push " + (ExecuteVM.MEMSIZE + n.entry.offset),
				"lw",
				"lhp",
				"sw",
				"lhp",
				"lhp",
				"push 1",
				"add",
				"shp"
		);
	}
	@Override
	public String visitNode(EmptyNode n) {
		if (print) printNode(n);
		return "push -1";
	}


}