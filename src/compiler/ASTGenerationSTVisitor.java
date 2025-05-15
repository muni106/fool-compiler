package compiler;

import java.util.*;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import compiler.AST.*;
import compiler.FOOLParser.*;
import compiler.lib.*;
import static compiler.lib.FOOLlib.*;

public class ASTGenerationSTVisitor extends FOOLBaseVisitor<Node> {

	String indent;
	public boolean print;

	ASTGenerationSTVisitor() {}
	ASTGenerationSTVisitor(boolean debug) { print=debug; }

	private void printVarAndProdName(ParserRuleContext ctx) {
		String prefix="";
		Class<?> ctxClass=ctx.getClass(), parentClass=ctxClass.getSuperclass();
		if (!parentClass.equals(ParserRuleContext.class)) // parentClass is the var context (and not ctxClass itself)
			prefix=lowerizeFirstChar(extractCtxName(parentClass.getName()))+": production #";
		System.out.println(indent+prefix+lowerizeFirstChar(extractCtxName(ctxClass.getName())));
	}

	@Override
	public Node visit(ParseTree t) {
		if (t==null) return null;
		String temp=indent;
		indent=(indent==null)?"":indent+"  ";
		Node result = super.visit(t);
		indent=temp;
		return result;
	}

	@Override
	public Node visitProg(ProgContext c) {
		if (print) printVarAndProdName(c);
		return visit(c.progbody());
	}

	@Override
	public Node visitLetInProg(LetInProgContext c) {
		if (print) printVarAndProdName(c);
		List<DecNode> declist = new ArrayList<>();
		for (CldecContext clDec : c.cldec()) declist.add((DecNode) visit(clDec));
		for (DecContext dec : c.dec()) declist.add((DecNode) visit(dec));
		return new ProgLetInNode(declist, visit(c.exp()));
	}

	@Override
	public Node visitNoDecProg(NoDecProgContext c) {
		if (print) printVarAndProdName(c);
		return new ProgNode(visit(c.exp()));
	}

	@Override
	public Node visitTimesDiv(TimesDivContext c) {
		if (print) printVarAndProdName(c);
		Node n;
		if (c.TIMES() != null) {
			n = new TimesNode(visit(c.exp(0)), visit(c.exp(1)));
			n.setLine(c.TIMES().getSymbol().getLine());		// setLine added
		} else {
			n = new DivNode(visit(c.exp(0)), visit(c.exp(1)));
			n.setLine(c.DIV().getSymbol().getLine());		// setLine added
		}
		return n;
	}

	@Override
	public Node visitPlusMinus(PlusMinusContext c) {
		if (print) printVarAndProdName(c);
		Node n;
		if (c.PLUS() != null) {
			n = new PlusNode(visit(c.exp(0)), visit(c.exp(1)));
			n.setLine(c.PLUS().getSymbol().getLine());		// setLine added
		} else {
			n = new MinusNode(visit(c.exp(0)), visit(c.exp(1)));
			n.setLine(c.MINUS().getSymbol().getLine());		// setLine added
		}
		return n;
	}

	@Override
	public Node visitComp(CompContext c) {
		if (print) printVarAndProdName(c);
		Node n;
		if (c.LE() != null) {
			n = new LessEqualNode(visit(c.exp(0)), visit(c.exp(1)));
			n.setLine(c.LE().getSymbol().getLine());
		} else if (c.GE() != null) {
			n = new GreaterEqualNode(visit(c.exp(0)), visit(c.exp(1)));
			n.setLine(c.GE().getSymbol().getLine());
		} else {
			n = new EqualNode(visit(c.exp(0)), visit(c.exp(1)));
			n.setLine(c.EQ().getSymbol().getLine());
		}

		return n;
	}

	@Override
	public Node visitAndOr(AndOrContext c) {
		if (print) printVarAndProdName(c);
		Node n;
		if (c.AND() != null) {
			n = new AndNode(visit(c.exp(0)), visit(c.exp(1)));
			n.setLine(c.AND().getSymbol().getLine());
		} else {
			n = new OrNode(visit(c.exp(0)), visit(c.exp(1)));
			n.setLine(c.OR().getSymbol().getLine());
		}
		return n;
	}

	@Override
	public Node visitNot(NotContext c) {
		if (print) printVarAndProdName(c);
		Node n = new NotNode(visit(c.exp()));
		n.setLine(c.NOT().getSymbol().getLine());
		return n;
	}

	@Override
	public Node visitVardec(VardecContext c) {
		if (print) printVarAndProdName(c);
		Node n = null;
		if (c.ID()!=null) { //non-incomplete ST
			n = new VarNode(c.ID().getText(), (TypeNode) visit(c.type()), visit(c.exp()));
			n.setLine(c.VAR().getSymbol().getLine());
		}
		return n;
	}

	@Override
	public Node visitFundec(FundecContext c) {
		if (print) printVarAndProdName(c);
		List<ParNode> parList = new ArrayList<>();
		for (int i = 1; i < c.ID().size(); i++) {
			ParNode p = new ParNode(c.ID(i).getText(),(TypeNode) visit(c.type(i)));
			p.setLine(c.ID(i).getSymbol().getLine());
			parList.add(p);
		}
		List<DecNode> decList = new ArrayList<>();
		for (DecContext dec : c.dec()) decList.add((DecNode) visit(dec));
		Node n = null;
		if (c.ID().size()>0) { //non-incomplete ST
			n = new FunNode(c.ID(0).getText(),(TypeNode)visit(c.type(0)),parList,decList,visit(c.exp()));
			n.setLine(c.FUN().getSymbol().getLine());
		}
		return n;
	}

	@Override
	public Node visitIntType(IntTypeContext c) {
		if (print) printVarAndProdName(c);
		return new IntTypeNode();
	}

	@Override
	public Node visitBoolType(BoolTypeContext c) {
		if (print) printVarAndProdName(c);
		return new BoolTypeNode();
	}

	@Override
	public Node visitInteger(IntegerContext c) {
		if (print) printVarAndProdName(c);
		int v = Integer.parseInt(c.NUM().getText());
		return new IntNode(c.MINUS()==null?v:-v);
	}

	@Override
	public Node visitTrue(TrueContext c) {
		if (print) printVarAndProdName(c);
		return new BoolNode(true);
	}

	@Override
	public Node visitFalse(FalseContext c) {
		if (print) printVarAndProdName(c);
		return new BoolNode(false);
	}

	@Override
	public Node visitIf(IfContext c) {
		if (print) printVarAndProdName(c);
		Node ifNode = visit(c.exp(0));
		Node thenNode = visit(c.exp(1));
		Node elseNode = visit(c.exp(2));
		Node n = new IfNode(ifNode, thenNode, elseNode);
		n.setLine(c.IF().getSymbol().getLine());
		return n;
	}

	@Override
	public Node visitPrint(PrintContext c) {
		if (print) printVarAndProdName(c);
		return new PrintNode(visit(c.exp()));
	}

	@Override
	public Node visitPars(ParsContext c) {
		if (print) printVarAndProdName(c);
		return visit(c.exp());
	}

	@Override
	public Node visitId(IdContext c) {
		if (print) printVarAndProdName(c);
		Node n = new IdNode(c.ID().getText());
		n.setLine(c.ID().getSymbol().getLine());
		return n;
	}

	@Override
	public Node visitCall(CallContext c) {
		if (print) printVarAndProdName(c);
		List<Node> arglist = new ArrayList<>();
		for (ExpContext arg : c.exp()) arglist.add(visit(arg));
		Node n = new CallNode(c.ID().getText(), arglist);
		n.setLine(c.ID().getSymbol().getLine());
		return n;
	}

	// OBJECT EXTENSION
	@Override
	public Node visitCldec(CldecContext c) {
		if (print) printVarAndProdName(c);
		final List<MethodNode> methods = new ArrayList<>();
		final List<FieldNode> fields = new ArrayList<>();
		String superID = null;
		int startID = 1;
		if (c.EXTENDS() != null) {
			superID = c.ID(1).getText();
			startID = 2;
		}
		for (int i = startID; i < c.ID().size(); i++) {
			FieldNode field = new FieldNode(c.ID(i).getText(), (TypeNode) visit(c.type(i - startID)));
			field.setLine(c.ID(i).getSymbol().getLine());
			fields.add(field);
		}
		for (MethdecContext met : c.methdec()) {
			methods.add((MethodNode) visit(met));
		}
		Node n = null;
		if (!c.ID().isEmpty()) {
			n = new ClassNode(c.ID(0).getText(), superID, fields, methods);
			n.setLine(c.CLASS().getSymbol().getLine());
		}
		return n;
	}

	@Override
	public MethodNode visitMethdec(MethdecContext c) {
		if (print) printVarAndProdName(c);
		List<ParNode> parameterList = new ArrayList<>();
		for (int i = 1; i < c.ID().size(); i++) {
			ParNode p = new ParNode(c.ID(i).getText(), (TypeNode) visit(c.type(i)));
			p.setLine(c.ID(i).getSymbol().getLine());
			parameterList.add(p);
		}
		List<DecNode> declarationList = new ArrayList<>();
		for (DecContext dec : c.dec()) declarationList.add((DecNode) visit(dec));
		MethodNode n = null;
		if (!c.ID().isEmpty()) {
			n = new MethodNode( c.ID(0).getText(), (TypeNode) visit( c.type(0) ), parameterList, declarationList, visit( c.exp() ) );
			n.setLine(c.FUN().getSymbol().getLine());
		}
		return n;
	}

	@Override
	public Node visitNull(NullContext c) {
		if (print) printVarAndProdName(c);
		EmptyNode empty = new EmptyNode();
		empty.setLine(c.NULL().getSymbol().getLine());
		return empty;
	}

	@Override
	public Node visitNew(NewContext c) {
		if (print) printVarAndProdName(c);
		Node n = null;
		if(c.ID() !=  null) {
			String classId = c.ID().getText();
			List<Node> argList = new ArrayList<>();
			for (ExpContext argument : c.exp()) {
				argList.add(visit(argument));
			}
			n = new NewNode(classId, argList);
			n.setLine(c.ID().getSymbol().getLine());
		}
		return n;
	}

	@Override
	public Node visitDotCall(DotCallContext c) {
		if (print) printVarAndProdName(c);
		Node n = null;
		if (c.ID().size() == 2) {
			String classId = c.ID(0).getText();
			String methodId = c.ID(1).getText();
			List<Node> argList = new ArrayList<>();
			for (ExpContext arg : c.exp()) {
				argList.add(visit(arg));
			}
			n = new ClassCallNode(classId, methodId, argList);
			n.setLine(c.ID(0).getSymbol().getLine());
		}
		return n;
	}

	@Override
	public Node visitIdType(IdTypeContext c) {
		if (print) printVarAndProdName(c);
		String classId = c.ID().getText();
		RefTypeNode n = new RefTypeNode(classId);
		n.setLine(c.ID().getSymbol().getLine());
		return n;
	}

}
