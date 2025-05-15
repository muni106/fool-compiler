package compiler;

import compiler.AST.*;
import compiler.lib.*;

import java.util.*;

public class TypeRels {
	// <subClass: string, superClass: string>
	public static Map<String, String> superType = new HashMap<>();

	// valuta se il tipo "a" e' <= al tipo "b", dove "a" e "b" sono tipi di base: IntTypeNode o BoolTypeNode
	public static boolean isSubtype(TypeNode a, TypeNode b) {

		if ((a instanceof RefTypeNode) && (b instanceof RefTypeNode)) {
			String subClass = ((RefTypeNode) a ).classId;
			String superClass = ((RefTypeNode) b ).classId;

			while (subClass != null) {
				if (subClass.equals(superClass)) return true;
				else subClass = superType.get(subClass);
			}
			return false;
		}

		// Method subtyping
		if (a instanceof ArrowTypeNode subClassMethod && b instanceof ArrowTypeNode superClassMethod) {
			// contro-varianza parametri
			if (subClassMethod.parlist.size() != superClassMethod.parlist.size()) return false;
			for (int i = 0; i < subClassMethod.parlist.size(); i++) {
				if (!isSubtype(subClassMethod.parlist.get(i), superClassMethod.parlist.get(i))) {
					return false;
				}
			}
			// co-varianza del tipo di ritorno
			return isSubtype(subClassMethod.ret, superClassMethod.ret);
		}

        return a.getClass().equals(b.getClass())
                || ((a instanceof BoolTypeNode) && (b instanceof IntTypeNode))
                || ((a instanceof EmptyTypeNode) && (b instanceof RefTypeNode));
    }

	public static void addClassTypeReference(String subClass, String superClass) {
		superType.put(subClass, superClass);
	}

	// for ifNode
	public static TypeNode lowestCommonAncestor(TypeNode a, TypeNode b) {
		// bool/int Nodes management
		if ((a instanceof BoolTypeNode) && (b instanceof IntTypeNode)
			|| (a instanceof IntTypeNode) && (b instanceof BoolTypeNode)
			|| (a instanceof IntTypeNode) && (b instanceof IntTypeNode))
			return new IntTypeNode();
		if ((a instanceof BoolTypeNode) && (b instanceof BoolTypeNode)) return new BoolTypeNode();

		// EmptyTypeNode management
		if ((a instanceof EmptyTypeNode) && (b instanceof RefTypeNode)) return b;
		if ((a instanceof RefTypeNode) && (b instanceof EmptyTypeNode)) return a;

		// Class management
		if ((a instanceof  RefTypeNode refA) && (b instanceof RefTypeNode refB)) {
			List<String> pathA = new ArrayList<>();
			List<String> pathB = new ArrayList<>();

			String currA = refA.classId;
			String currB = refB.classId;

			pathA.add(currA);
			pathB.add(currB);

			// fill pathA
			while (superType.containsKey(currA)){
				currA = superType.get(currA);
				pathA.add(currA);
			}

			// fill pathB
			while (superType.containsKey(currB)){
				currB = superType.get(currB);
				pathA.add(currB);
			}

			// little optimization not required but cool
			if (!currA.equals(currB)) return null;

			for (int i = 0; i < pathA.size(); i++) {
				for (int j = 0; j < pathB.size(); j++) {
					if (pathA.get(i).equals(pathB.get(j))) return new RefTypeNode(pathA.get(i));
				}
			}
		}


		return null;

	}




}
