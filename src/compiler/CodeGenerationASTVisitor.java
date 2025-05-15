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
		return "push " + funl;
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
		// Inserisco tutti i metodi della classe, ereditati e non, nella Dispatch Table
		// Recupero la dispatch table della superclasse se presente
		List<String> dT = new ArrayList<>();

		// Controllo sia superId che superEntry prima di accedere all'offset
		if (n.superId != null && n.superEntry != null) {
			int superIndex = -n.superEntry.offset - 2;
			if (superIndex >= 0 && superIndex < dispatchTables.size()) {
				dT = new ArrayList<>(dispatchTables.get(superIndex));
			}
		}
		for (MethodNode method : n.methods) {
			visit(method);

			if (method.offset < dT.size()) { // Overriding
				dT.set(method.offset, method.label);
			} else { // Nuovo metodo
				// Assicuro che ci siano abbastanza spazi nella lista
				while(dT.size() <= method.offset) {
					dT.add(null);
				}
				dT.set(method.offset, method.label);
			}
		}

		dispatchTables.add(dT);
		// Generazione del codice assembly per la Dispatch Table
		String codeMethod = "";
		for (String label : dT) {
			codeMethod = nlJoin(codeMethod,
					"push " + label,  // Metto sullo stack la label
					"lhp",  // Metto sullo stack il valore di hp
					"sw",  // Scrivo la label all'indirizzo puntato da hp
					"lhp",  // Carico il valore di hp
					"push 1",
					"add",  // Incremento hp
					"shp");
		}

		return nlJoin(
				"lhp",  // Metto hp sullo stack, ovvero il dispatch pointer da ritornare
				codeMethod // Creo sullo heap la dispatch table
		);

	}

	@Override
	public String visitNode(MethodNode n) {
		if (print) printNode(n, n.id);

		// Generazione codice per dichiarazioni locali e rimozione dallo stack
		String declCode = null, popDecl = null, popParl = null;
		for (Node dec : n.declarationList) {
			declCode = nlJoin(declCode, visit(dec));
			popDecl = nlJoin(popDecl, "pop");
		}

		// Rimozione parametri dallo stack
		for (int i = 0; i < n.parameterList.size(); i++) {
			popParl = nlJoin(popParl, "pop");
		}

		// Generazione di una nuova etichetta per l'indirizzo del metodo
		String funl = freshFunLabel();
		n.label = funl; // Salvataggio dell'etichetta nel nodo

		// Inserimento del codice del metodo in FOOLlib
		putCode(
				nlJoin(
						funl + ":", // Etichetta del metodo
						"cfp", // Imposta $fp al valore di $sp
						"lra", // Carica il valore di $ra
						declCode, // Codice delle dichiarazioni locali
						visit(n.exp), // Codice del corpo della funzione
						"stm", // Salva il valore poppato (risultato della funzione)
						popDecl, // Rimuove le dichiarazioni locali dallo stack
						"sra", // Ripristina il valore di $ra
						"pop", // Rimuove l'Access Link dallo stack
						popParl, // Rimuove i parametri dallo stack
						"sfp", // Ripristina il valore di $fp (Control Link)
						"ltm", // Carica il valore di $tm (risultato della funzione)
						"lra", // Carica il valore di $ra
						"js" // Salta all'indirizzo memorizzato in $ra
				)
		);

		return null;
	}
	@Override
	public String visitNode(ClassCallNode n) {
		if (print) printNode(n, n.methodId);

		// Controllo che methodEntry sia definito
		if (n.methodEntry == null) {
			// Gestione dell'errore: il methodEntry non è stato definito
			System.err.println("Errore: methodEntry non definito per il metodo " + n.methodId);
			return "push -1"; // Ritorna un valore di errore o gestisci come preferisci
		}
		// Generazione codice per i parametri
		String argCode = null, getAR = null;
		for (int i = n.argList.size() - 1; i >= 0; i--) {
			argCode = nlJoin(argCode, visit(n.argList.get(i)));
		}

		// Risalita della catena statica per trovare l'object pointer
		for (int i = 0; i < n.nestingLevel - n.entry.nl; i++) {
			getAR = nlJoin(getAR, "lw");
		}

		return nlJoin(
				"lfp", // Carico il Control Link (puntatore al frame della funzione chiamante)
				argCode, // Codice per i parametri
				"lfp", getAR, // Risalita della catena statica per trovare l'object pointer
				"push " + n.entry.offset, "add", // Trova l'oggetto aggiungendo l'offset
				"lw", // Carica il puntatore all'oggetto
				"stm", // Imposta $tm al valore poppato
				"ltm", // Carica l'Access Link (puntatore al frame dell'oggetto)
				"ltm", // Duplica il valore in cima allo stack (object pointer)
				"lw", // Carica il dispatch pointer dell'oggetto
				"push " + n.methodEntry.offset, "add", // Recupera l'indirizzo del metodo nella dispatch table
				"lw", // Carica l'indirizzo del metodo
				"js"  // Salta all'indirizzo del metodo, salvando il ritorno in $ra
		);
	}
	@Override
	public String visitNode(NewNode n) {
		if (print) printNode(n, n.classId);

		String fieldsOnStack = null;
		String fieldsOnHeap = null;

		// Mettiamo sullo stack tutti i valori dei campi
		for (Node param : n.argList) {
			fieldsOnStack = nlJoin(fieldsOnStack, visit(param));

			// Spostiamo i valori dallo stack allo heap, incrementando $hp
			fieldsOnHeap = nlJoin(fieldsOnHeap,
					"lhp", // Carica il valore di $hp
					"sw",  // Memorizza il valore nello heap
					"lhp", // Ricarica $hp
					"push 1",
					"add",
					"shp" // Incrementa $hp
			);
		}
		// Inserimento del dispatch pointer
		String dispatchPointer = nlJoin(
				"push " + (ExecuteVM.MEMSIZE + n.entry.offset), // Recupera il dispatch pointer
				"lw", // Carica il valore dal global AR
				"lhp", // Carica l'heap pointer
				"sw"  // Memorizza il dispatch pointer nello heap
		);

		return nlJoin(
				fieldsOnStack, // Genera codice per i parametri
				fieldsOnHeap, // Sposta i valori dallo stack allo heap
				dispatchPointer, // Memorizza il dispatch pointer
				"lhp", // Duplica l'object pointer
				"lhp", "push 1", "add", "shp" // Incrementa $hp
		);
	}
	@Override
	public String visitNode(EmptyNode n) {
		if (print) printNode(n);
		return "push -1";
	}


}