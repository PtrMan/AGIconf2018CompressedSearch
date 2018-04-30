package ls;

// instructions are encoded in the program with the use of instructionpages

// instruction-pages are groups of instructions for the same context/purpose
// for example arithmetic instructions are often used in close proximity

// class to translate between the program and the decoded instructions
class Code2InstructionTranslator {
	static class InstructionPage {
		public int[] code2instr;
	}

	public int currentInstructionPage = 0;

	public InstructionPage[] instructionPages;

	public int instructionsPerPage = 9;

	public int translate2Instruction(final int code) {
		boolean isPageChange = code >= instructionsPerPage;
		if( isPageChange )   currentInstructionPage = code - instructionsPerPage; // change page
		return isPageChange ? INSTR_SKIP : instructionPages[currentInstructionPage].code2instr[code];
	}

	static final int INSTR_SKIP = 0; // instruction may be ignored or translated to NOP, depending on the use of the instruction-set and relative jumps
}
