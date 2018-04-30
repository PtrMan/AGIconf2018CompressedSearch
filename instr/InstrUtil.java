package instr;

import java.util.List;

public class InstrUtil {
	// translates instructions (as InstrCode) to a string
	// the string can be used to compress the programs
	public static void translate2string(List<InstrCode> instrs, StringBuilder destStr) throws Exception {
		translate2string(instrs, destStr, ",");
	}

	private static void translate2string(List<InstrCode> instrs, StringBuilder destStr, String seperator) throws Exception {
		for( int idx = 0; idx < instrs.size()-1; idx++ ) {
			translate2string(instrs.get(idx), destStr);
			destStr.append(seperator);
		}

		translate2string(instrs.get(instrs.size()-1), destStr);
	}

	// translates the instruction to a compression friendly string and appends it
	private static void translate2string(InstrCode instr, StringBuilder destStr) throws Exception {
		int idx = instr.instrIdx;

		if( idx < 10 ) {
			destStr.append(idx);
			return;
		}
		idx -= 10;

		if( idx < 26 ) {
			destStr.append((char)(97 + idx)); // small letter
			return;
		}
		idx -= 26;

		if( idx < 26 ) {
			destStr.append((char)(65 + idx)); // big letter
			return;
		}
		idx -= 26;

		// TODO< encode instruction with special signs >
		throw new Exception("NOT IMPLEMENTED - TODO");
	}
}
