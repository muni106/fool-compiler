package compiler;

import compiler.AST.*;
import compiler.lib.*;
import compiler.exc.*;

public class PrintEASTVisitor extends BaseEASTVisitor<Void,VoidException> {

	PrintEASTVisitor() { super(false,true); } 

	@Override
	public Void visitNode(ProgLetInNode n) {
		printNode(n);
		for (Node dec : n.declist) visit(dec);
		visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(ProgNode n) {
		printNode(n);
		visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(FunNode n) {
		printNode(n,n.id);
		visit(n.retType);
		for (ParNode par : n.parlist) visit(par);
		for (Node dec : n.declist) visit(dec);
		visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(ParNode n) {
		printNode(n,n.id);
		visit(n.getType());
		return null;
	}

	@Override
	public Void visitNode(VarNode n) {
		printNode(n,n.id);
		visit(n.getType());
		visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(PrintNode n) {
		printNode(n);
		visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(IfNode n) {
		printNode(n);
		visit(n.cond);
		visit(n.th);
		visit(n.el);
		return null;
	}

	@Override
	public Void visitNode(EqualNode n) {
		printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(GreaterEqualNode n) {
		printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(LessEqualNode n) {
		printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(AndNode n) {
		printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(OrNode n) {
		printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(NotNode n) {
		printNode(n);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(TimesNode n) {
		printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(DivNode n) {
		printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(PlusNode n) {
		printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(MinusNode n) {
		printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(CallNode n) {
		printNode(n,n.id + " at nestinglevel " + n.nl);
		visit(n.entry);
		for (Node arg : n.arglist) visit(arg);
		return null;
	}

	@Override
	public Void visitNode(IdNode n) {
		printNode(n,n.id + " at nestinglevel " + n.nestingLevel);
		visit(n.entry);
		return null;
	}

	@Override
	public Void visitNode(BoolNode n) {
		printNode(n, n.val.toString());
		return null;
	}

	@Override
	public Void visitNode(IntNode n) {
		printNode(n, n.val.toString());
		return null;
	}
	
	@Override
	public Void visitNode(ArrowTypeNode n) {
		printNode(n);
		for (Node par: n.parlist) visit(par);
		visit(n.ret,"->");
		return null;
	}

	@Override
	public Void visitNode(BoolTypeNode n) {
		printNode(n);
		return null;
	}

	@Override
	public Void visitNode(IntTypeNode n) {
		printNode(n);
		return null;
	}
	
	@Override
	public Void visitSTentry(STentry entry) {
		printSTentry("nestinglevel " + entry.nl);
		printSTentry("type");
		visit(entry.type);
		printSTentry("offset " + entry.offset);
		return null;
	}

	@Override
	public Void visitNode(ClassNode n) {
		printNode(n, n.id);
		for (int i = 0; i < n.fields.size(); i++) {
			visit(n.fields.get(i));
		}

		for (int i = 0; i < n.methods.size(); i++) {
			visit(n.methods.get(i));
		}
		return null;
	}


	@Override
	public Void visitNode(FieldNode n) {
		printNode(n, n.id);
		visit(n.getType());
		return null;
	}

	@Override
	public Void visitNode(MethodNode n) {
		printNode(n, n.id);
		visit(n.returnType);
		for (ParNode par : n.parameterList) visit(par);
		for (Node dec : n.declarationList) visit(dec);
		visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(ClassCallNode n) {
		final String id = n.classId + "." + n.methodId + " nesting level: " + n.nestingLevel;
		printNode(n, id);
		visit(n.entry);
		visit(n.methodEntry);
		for (int i = 0; i < n.argList.size(); i++) {
			visit(n.argList.get(i));
		}
		return null;
	}

	@Override
	public Void visitNode(NewNode n) {
		final String id = n.classId + " nesting level: " + n.entry.nl;
		printNode(n, id);
		visit(n.entry);
		for (Node arg : n.argList) visit(arg);
		return null;
	}

	@Override
	public Void visitNode(EmptyNode n) {
		printNode(n);
		return null;
	}

	@Override
	public Void visitNode(RefTypeNode n) {
		printNode(n, n.classId);
		return null;
	}

	@Override
	public Void visitNode(ClassTypeNode n) {
		printNode(n);
		return null;
	}

}
