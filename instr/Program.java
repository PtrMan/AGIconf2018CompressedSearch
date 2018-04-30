package instr;

import java.util.List;
import java.util.ArrayList;

public class Program<AtomType> {
	public List<AtomType> instrs;

	public Program(List<AtomType> instrs) {
		this.instrs = instrs;
	}

	public Program() {
		instrs = new ArrayList<>();
	}
}
