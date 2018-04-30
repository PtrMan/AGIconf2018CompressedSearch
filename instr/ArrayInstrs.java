package instr;

import ls.Instrs;

// array manipulation instructions
public class ArrayInstrs {
	// append poped value to array and set the index of the array to the appended element
	public static class ARRPOPAPPEND implements Instrs.IInstr {
		public int arrayIdx;

		public ARRPOPAPPEND(int arrayIdx) {
			this.arrayIdx = arrayIdx;
		}

		public boolean exec(Instrs.Ctx ctx) {
			if( ctx.stack.empty() )   return false;
			double val = ctx.stack.pop();

			ctx.arrayCtx.append(val, arrayIdx);
			if( !ctx.arrayCtx.isOk() )   return false;
			int arrLength = ctx.arrayCtx.retArrLength(arrayIdx);
			if( !ctx.arrayCtx.isOk() )   return false;

			ctx.arrayCtx.setArrIdx(arrLength - 1, arrayIdx);
			if( !ctx.arrayCtx.isOk() )   return false;

			return true;
		}

		public String retHumanName() {
			return "ARRPOPAPPEND";
		}

		public String retExactHumanName() {
			return "ARRPOPAPPEND " + Integer.toString(arrayIdx);
		}
	}

	// delete array element at current index
	public static class ARRDEL implements Instrs.IInstr {
		public int arrayIdx;

		public ARRDEL(int arrayIdx) {
			this.arrayIdx = arrayIdx;
		}

		public boolean exec(Instrs.Ctx ctx) {
			int idx = ctx.arrayCtx.retArrIdx(arrayIdx);
			if( !ctx.arrayCtx.isOk() )   return false; // return if the array didn't exist
			
			ctx.arrayCtx.delAtIdx(idx, arrayIdx); // return if deleting failed
			if( !ctx.arrayCtx.isOk() )   return false; // return if deleting failed

			int arrLength = ctx.arrayCtx.retArrLength(arrayIdx);
			if( !ctx.arrayCtx.isOk() )   return false;

			if( idx >= arrLength ) {
				ctx.arrayCtx.setArrIdx(arrLength - 1, arrayIdx);
				if( !ctx.arrayCtx.isOk() )   return false;
			}

			return true;
		}

		public String retHumanName() {
			return "ARRDEL";
		}

		public String retExactHumanName() {
			return "ARRDEL " + Integer.toString(arrayIdx);
		}
	}

	public static class ARRLEN implements Instrs.IInstr {
		public int arrayIdx;

		public ARRLEN(int arrayIdx) {
			this.arrayIdx = arrayIdx;
		}

		public boolean exec(Instrs.Ctx ctx) {
			int arrLength = ctx.arrayCtx.retArrLength(arrayIdx);
			if( !ctx.arrayCtx.isOk() )   return false; // return if the array didn't exist

			ctx.stack.push((double)arrLength);

			return true;
		}

		public String retHumanName() {
			return "ARRLEN";
		}

		public String retExactHumanName() {
			return "ARRLEN " + Integer.toString(arrayIdx);
		}
	}

	public static class ARRSETIDX implements Instrs.IInstr {
		public int arrayIdx;

		public ARRSETIDX(int arrayIdx) {
			this.arrayIdx = arrayIdx;
		}

		public boolean exec(Instrs.Ctx ctx) {
			if( ctx.stack.empty() )   return false;
			long idxLong = Math.round(ctx.stack.peek());
			if( idxLong >= Integer.MAX_VALUE )  return false; // check for index overflow
			int idx = (int)idxLong;

			ctx.arrayCtx.setArrIdx(idx, arrayIdx);
			if( !ctx.arrayCtx.isOk() )   return false;

			return true;
		}

		public String retHumanName() {
			return "ARRSETIDX";
		}

		public String retExactHumanName() {
			return "ARRSETIDX " + Integer.toString(arrayIdx);
		}
	}

	public static class ARRRETIDX implements Instrs.IInstr {
		public int arrayIdx;

		public ARRRETIDX(int arrayIdx) {
			this.arrayIdx = arrayIdx;
		}

		public boolean exec(Instrs.Ctx ctx) {
			int idx = ctx.arrayCtx.retArrIdx(arrayIdx);
			if( !ctx.arrayCtx.isOk() )   return false;
			ctx.stack.push((double)idx);

			return true;
		}

		public String retHumanName() {
			return "ARRRETIDX";
		}

		public String retExactHumanName() {
			return "ARRRETIDX " + Integer.toString(arrayIdx);
		}
	}


	public static class ARRCHECKIDX implements Instrs.IInstr {
		public int arrayIdx;

		public ARRCHECKIDX(int arrayIdx) {
			this.arrayIdx = arrayIdx;
		}

		public boolean exec(Instrs.Ctx ctx) {
			ctx.retCurrentVm().flag = ctx.arrayCtx.retCheckIdx(arrayIdx);
			if( !ctx.arrayCtx.isOk() )   return false;
			return true;
		}

		public String retHumanName() {
			return "ARRCHECKIDX";
		}

		public String retExactHumanName() {
			return "ARRCHECKIDX " + Integer.toString(arrayIdx);
		}
	}

	// push value at current array index
	public static class ARRREADNPUSH implements Instrs.IInstr {
		public int arrayIdx;

		public ARRREADNPUSH(int arrayIdx) {
			this.arrayIdx = arrayIdx;
		}

		public boolean exec(Instrs.Ctx ctx) {
			int idx = ctx.arrayCtx.retArrIdx(arrayIdx);
			if( !ctx.arrayCtx.isOk() )   return false; // return if the array didn't exist
			
			double val = ctx.arrayCtx.retAt(idx, arrayIdx);
			if( !ctx.arrayCtx.isOk() )   return false;

			ctx.stack.push(val);

			return true;
		}

		public String retHumanName() {
			return "ARRREADNPUSH";
		}

		public String retExactHumanName() {
			return "ARRREADNPUSH " + Integer.toString(arrayIdx);
		}
	}

	public static class ARRREL implements Instrs.IInstr {
		public int arrayIdx;
		public int rel;
		String exactHumanName;

		public ARRREL(int rel, int arrayIdx, String exactHumanName) {
			this.rel = rel;
			this.arrayIdx = arrayIdx;
			this.exactHumanName = exactHumanName;
		}

		public boolean exec(Instrs.Ctx ctx) {
			int idx = ctx.arrayCtx.retArrIdx(arrayIdx);
			if( !ctx.arrayCtx.isOk() )   return false; // return if the array didn't exist
			
			ctx.arrayCtx.setArrIdx(idx, idx + Math.round(rel));
			// NOTE< we don't check if the op was successful because the next instruction should involve a rangecheck >

			return true;
		}

		public String retHumanName() {
			return "ARRREL";
		}

		public String retExactHumanName() {
			return exactHumanName;
		}
	}
}
