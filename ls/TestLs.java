package ls;

import java.util.List;
import java.util.ArrayList;

class TestLs {
	public static void main(String[] args) {
		Instrs.InstructionLookupTable instrLookupTable = new Instrs.InstructionLookupTable();
		instrLookupTable.table = new Instrs.IInstr[3];
		instrLookupTable.table[0] = new Instrs.NOP();
		instrLookupTable.table[1] = new Instrs.ADD2_R();
		instrLookupTable.table[2] = new Instrs.MUL2_R();




		List<Integer> program = new ArrayList<Integer>();

		// program to calculate the ength of a 3 dimensional vector

		// works by adding to the result we keep on the stack

		// stack:
		// [0] x
		// [1] y
		// [2] z

		program.add(instrLookupTable.lookupIdxByExactHumanName("DUP"));		
		program.add(instrLookupTable.lookupIdxByExactHumanName("MUL2_R"));

		program.add(instrLookupTable.lookupIdxByExactHumanName("XCHG"));
		program.add(instrLookupTable.lookupIdxByExactHumanName("DUP"));
		program.add(instrLookupTable.lookupIdxByExactHumanName("MUL2_R"));
		program.add(instrLookupTable.lookupIdxByExactHumanName("ADD2_R"));

		program.add(instrLookupTable.lookupIdxByExactHumanName("XCHG"));
		program.add(instrLookupTable.lookupIdxByExactHumanName("DUP"));
		program.add(instrLookupTable.lookupIdxByExactHumanName("MUL2_R"));
		program.add(instrLookupTable.lookupIdxByExactHumanName("ADD2_R"));

		program.add(instrLookupTable.lookupIdxByExactHumanName("SQRT"));


	}
}