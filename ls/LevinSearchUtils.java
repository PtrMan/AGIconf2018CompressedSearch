package ls;

import instr.InstrCode;
import instr.Program;

public class LevinSearchUtils {
	// interface used to retrieve probabilities of instruction(s)
	public interface IInstrProb<AtomType> {
		// /param atom instruction or any type of atom for which the prbability should be retrieved
		// /param index index in the program of the instruction
		double retByInstr(AtomType atom, int index);
	}

	// [1] https://en.wikipedia.org/wiki/Algorithmic_probability
    // [2] http://wiki.opencog.org/w/OpenCogPrime:OccamsRazor

    // approximation for not computable "solomonoff levin measure"
    // see mainly [2] and secondary [1]
    public double calcSolomonoffLevinMeasure<AtomType>(IInstrProb<AtomType> instrProb, Program<AtomType> program) {
        double prod = 1.0;
        for(int instructionI = 0; instructionI < program.instructions.Count; instructionI++)
            prod *= instrProb.retByInstr(program.instrs[instructionI], instructionI);
        return prod;
    }
}
