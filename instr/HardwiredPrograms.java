package instr;

import java.util.List;
import java.util.ArrayList;

import instr.InstrCode;
import ls.Instrs;

// small hardwired database of useful programs
// can be used as subroutines or training/learning material

public class HardwiredPrograms {
	public static List<InstrCode> retArrAppendNTimes(Instrs.InstructionLookupTable instrLookupTable) throws Exception {
		List<InstrCode> instrs = new ArrayList<InstrCode>();

		// program to append stack value below the top of the stack  (at top of stack) times
		
		// executes loop which decrements the counter as long as it is above zero
		{
			instrs.add(instrLookupTable.lookupIdxByExactHumanName("CMPGTZERO"));
			instrs.add(instrLookupTable.lookupIdxByExactHumanName("PREDIGNOREIFNOTFLAG 7"));
			
			// MACRO 1               decrement
			instrs.add(instrLookupTable.lookupIdxByExactHumanName("PUSH -1"));
			instrs.add(instrLookupTable.lookupIdxByExactHumanName("ADD_R(2)"));

			instrs.add(instrLookupTable.lookupIdxByExactHumanName("XCHG"));
			instrs.add(instrLookupTable.lookupIdxByExactHumanName("DUP"));

			// (change page to array ops)

			instrs.add(instrLookupTable.lookupIdxByExactHumanName("ARRPOPAPPEND 0"));

			// (change page to default ops)

			instrs.add(instrLookupTable.lookupIdxByExactHumanName("XCHG"));
			instrs.add(instrLookupTable.lookupIdxByExactHumanName("JMP -9"));
		}

		return instrs;
	}

	public static List<InstrCode> retArrRemoveNTimes(Instrs.InstructionLookupTable instrLookupTable) throws Exception {
		List<InstrCode> instrs = new ArrayList<InstrCode>();

		// program to remove elements from the array at the current index  (at top of stack) times
		
		// executes loop which decrements the counter as long as it is above zero
		{
			instrs.add(instrLookupTable.lookupIdxByExactHumanName("CMPGTZERO"));
			instrs.add(instrLookupTable.lookupIdxByExactHumanName("PREDIGNOREIFNOTFLAG 5"));
			
			// MACRO 1               decrement
			instrs.add(instrLookupTable.lookupIdxByExactHumanName("PUSH -1"));
			instrs.add(instrLookupTable.lookupIdxByExactHumanName("ADD_R(2)"));

			// (change page to array ops)

			instrs.add(instrLookupTable.lookupIdxByExactHumanName("ARRDEL 0"));

			// (change page to default ops)

			instrs.add(instrLookupTable.lookupIdxByExactHumanName("NOP"));
			instrs.add(instrLookupTable.lookupIdxByExactHumanName("JMP -7"));
		}

		return instrs;
	}

	public static List<InstrCode> retArrClear(Instrs.InstructionLookupTable instrLookupTable) throws Exception {
		List<InstrCode> instrs = new ArrayList<InstrCode>();

		// program to clear a array

		// basically the program  retArrRemoveNTimes() fed with the size of the array

		instrs.add(instrLookupTable.lookupIdxByExactHumanName("PUSH 0"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("ARRSETIDX 0"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("POP"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("ARRLEN 0"));

		{
			instrs.add(instrLookupTable.lookupIdxByExactHumanName("CMPGTZERO"));
			instrs.add(instrLookupTable.lookupIdxByExactHumanName("PREDIGNOREIFNOTFLAG 5"));
			
			// MACRO 1               decrement
			instrs.add(instrLookupTable.lookupIdxByExactHumanName("PUSH -1"));
			instrs.add(instrLookupTable.lookupIdxByExactHumanName("ADD_R(2)"));

			// (change page to array ops)

			instrs.add(instrLookupTable.lookupIdxByExactHumanName("ARRDEL 0"));

			// (change page to default ops)

			instrs.add(instrLookupTable.lookupIdxByExactHumanName("NOP"));
			instrs.add(instrLookupTable.lookupIdxByExactHumanName("JMP -7"));
		}

		return instrs;
	}


	public static List<InstrCode> retArrCopy(Instrs.InstructionLookupTable instrLookupTable) throws Exception {
		List<InstrCode> instrs = new ArrayList<InstrCode>();

		// program to append arr 0 to arr 1

		instrs.add(instrLookupTable.lookupIdxByExactHumanName("PUSH 0"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("ARRSETIDX 0"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("ARRSETIDX 1"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("POP"));
		
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("ARRCHECKIDX 0"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("POP"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("PREDIGNOREIFNOTFLAG 4"));

		instrs.add(instrLookupTable.lookupIdxByExactHumanName("ARRREADNPUSH 0"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("ARRREL 1 0"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("ARRPOPAPPEND 1"));

		instrs.add(instrLookupTable.lookupIdxByExactHumanName("JMP -7"));

		return instrs;
	}

	// append reversed array 0 to arr 1
	public static List<InstrCode> retArrAppendReversed0(Instrs.InstructionLookupTable instrLookupTable) throws Exception {
		List<InstrCode> instrs = new ArrayList<InstrCode>();

		// program to append arr 0 to arr 1

		instrs.add(instrLookupTable.lookupIdxByExactHumanName("ARRLEN 0"));
		
		// MACRO 1               decrement
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("PUSH -1"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("ADD_R(2)"));

		instrs.add(instrLookupTable.lookupIdxByExactHumanName("ARRSETIDX 0"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("POP"));

		instrs.add(instrLookupTable.lookupIdxByExactHumanName("PUSH 0"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("ARRSETIDX 1"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("POP"));
		

		instrs.add(instrLookupTable.lookupIdxByExactHumanName("ARRCHECKIDX 0"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("POP"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("PREDIGNOREIFNOTFLAG 4"));

		instrs.add(instrLookupTable.lookupIdxByExactHumanName("ARRREADNPUSH 0"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("ARRREL -1 0"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("ARRPOPAPPEND 1"));

		instrs.add(instrLookupTable.lookupIdxByExactHumanName("JMP -7"));

		return instrs;
	}
	
	// TODO< exchange array 0 and 1 >

	// TODO< check if array 0 is equal to array 1 at current indices >
	// (can be used to search for an string in a other string)



	//////////////////
	// math functions

	// exponentiation
	// top of stack : exponent (whole number)
	// below top of stack : value
	public static List<InstrCode> retMathPow(Instrs.InstructionLookupTable instrLookupTable) throws Exception {
		List<InstrCode> instrs = new ArrayList<InstrCode>();

		// duplicate 
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("XCHG"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("DUP"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("XCHG 2"));

		instrs.add(instrLookupTable.lookupIdxByExactHumanName("CMPGTZERO"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("PREDIGNOREIFNOTFLAG 9"));

		// MACRO 1               decrement
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("PUSH -1"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("ADD_R(2)"));

		// top of stack : exponent (whole number)
		// below : res
		// below : value

		instrs.add(instrLookupTable.lookupIdxByExactHumanName("XCHG 2"));

		// top of stack : value
		// below : res
		// below : exponent (whole number)

		instrs.add(instrLookupTable.lookupIdxByExactHumanName("DUP"));

		// top of stack : value
		// below : value
		// below : res
		// below : exponent (whole number)

		instrs.add(instrLookupTable.lookupIdxByExactHumanName("XCHG 2"));

		// top of stack : res
		// below : value
		// below : value
		// below : exponent (whole number)

		instrs.add(instrLookupTable.lookupIdxByExactHumanName("MUL_R(2)"));

		// top of stack : res
		// below : value
		// below : exponent (whole number)

		instrs.add(instrLookupTable.lookupIdxByExactHumanName("XCHG"));

		// top of stack : value
		// below : res
		// below : exponent (whole number)

		instrs.add(instrLookupTable.lookupIdxByExactHumanName("XCHG 2"));

		// top of stack : exponent (whole number)
		// below : value
		// below : res


		instrs.add(instrLookupTable.lookupIdxByExactHumanName("JMP -10"));

		instrs.add(instrLookupTable.lookupIdxByExactHumanName("POP"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("POP"));

		return instrs;
	}

	// max
	public static List<InstrCode> retMathMax(Instrs.InstructionLookupTable instrLookupTable) throws Exception {
		List<InstrCode> instrs = new ArrayList<InstrCode>();

		instrs.add(instrLookupTable.lookupIdxByExactHumanName("CMPGT"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("PREDIGNOREIFFLAG 4"));

		// reached if not greater than
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("XCHG"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("NOP"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("NOP"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("NOP"));

		instrs.add(instrLookupTable.lookupIdxByExactHumanName("POP"));

		return instrs;
	}

	// min
	public static List<InstrCode> retMathMin(Instrs.InstructionLookupTable instrLookupTable) throws Exception {
		List<InstrCode> instrs = new ArrayList<InstrCode>();

		instrs.add(instrLookupTable.lookupIdxByExactHumanName("CMPGT"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("PREDIGNOREIFNOTFLAG 4"));

		// reached if not greater than
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("XCHG"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("NOP"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("NOP"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("NOP"));

		instrs.add(instrLookupTable.lookupIdxByExactHumanName("POP"));

		return instrs;
	}

	// 2d quadratic distance
	public static List<InstrCode> ret2dQuadraticDist(Instrs.InstructionLookupTable instrLookupTable) throws Exception {
		List<InstrCode> instrs = new ArrayList<InstrCode>();

		instrs.add(instrLookupTable.lookupIdxByExactHumanName("DUP"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("MUL_R(2)"));

		instrs.add(instrLookupTable.lookupIdxByExactHumanName("XCHG"));

		instrs.add(instrLookupTable.lookupIdxByExactHumanName("DUP"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("MUL_R(2)"));

		instrs.add(instrLookupTable.lookupIdxByExactHumanName("ADD_R(2)"));

		return instrs;
	}


	// loop template for iterating over a array and doing a operation
	// is filled with NOP because the AI can copy paste parts of the program
	public static List<InstrCode> retArrIterationNop7(Instrs.InstructionLookupTable instrLookupTable) throws Exception {
		List<InstrCode> instrs = new ArrayList<InstrCode>();

		instrs.add(instrLookupTable.lookupIdxByExactHumanName("PUSH 0"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("ARRSETIDX 0"));


		instrs.add(instrLookupTable.lookupIdxByExactHumanName("NOP"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("NOP"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("ARRCHECKIDX 0"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("PREDIGNOREIFNOTFLAG 9"));

		// body
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("NOP"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("NOP"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("NOP"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("NOP"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("NOP"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("NOP"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("NOP"));

		instrs.add(instrLookupTable.lookupIdxByExactHumanName("ARRREL 1 0"));
		instrs.add(instrLookupTable.lookupIdxByExactHumanName("JMP -13"));

		return instrs;
	}




	// components of neural networks

	// RELU activation - 
}
