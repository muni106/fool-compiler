package compiler;

import compiler.AST.*;
import compiler.lib.*;

import java.util.HashMap;
import java.util.Map;

public class TypeRels {
	// <subClass: string, superClass: string>
	public static Map<String, String> superType = new HashMap<>();

	// valuta se il tipo "a" e' <= al tipo "b", dove "a" e "b" sono tipi di base: IntTypeNode o BoolTypeNode
	public static boolean isSubtype(TypeNode a, TypeNode b) {
		if (a.getClass().equals(b.getClass())
				|| ((a instanceof BoolTypeNode) && (b instanceof IntTypeNode))
				|| ((a instanceof  EmptyTypeNode) && (b instanceof RefTypeNode))
				|| ((a instanceof  RefTypeNode) && (b instanceof EmptyTypeNode)))
			return true;

		if (a instanceof RefTypeNode && b instanceof RefTypeNode) {
			String subClass = ((RefTypeNode) a ).className;
			String superClass = ((RefTypeNode) b ).className;

			while (subClass != null) {
				if (subClass.equals(superClass)) return true;
				else subClass = superType.get(subClass);
			}
		}

		// Method subtyping
		if (a instanceof ArrowTypeNode && b instanceof ArrowTypeNode) {
			ArrowTypeNode subClassMethod = (ArrowTypeNode) a;
			ArrowTypeNode superClassMethod = (ArrowTypeNode) b;
			// contro-varianza parametri
			if (subClassMethod.parlist.size() != superClassMethod.parlist.size()) return false;
			for (int i = 0; i < subClassMethod.parlist.size(); i++) {
				if (!isSubtype(superClassMethod.parlist.get(i), subClassMethod.parlist.get(i))) {
					return false;
				}
			}
			// co-varianza del tipo di ritorno
			return isSubtype(subClassMethod.ret, superClassMethod.ret);
		}

		// Field subtyping
		return false;
	}

	public static void addTypeReference(String subClass, String superClass) {
		superType.put(subClass, superClass);
	}


}
