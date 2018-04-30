package ls;

import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

import instr.InstrCode;

// instructions working on stack (of floats)
public class Instrs {
	// instruction
	public static class Instr {
		public int type;

		public int argA;

		public static Instr makeByType(int type) {
			Instr res = new Instr();
			res.type = type;
			return res;
		}
	}

	public interface IResetable {
		void reset();
	}

	// interface to abstract the details of the array handling from the interpreter and instruction to a implementation
	public interface IArrayCtx extends IResetable {
		// append a value to the array specified with array Idx
		// next call of isOk() indicates state
		void append(double val, int arrayIdx);
		// result indicates if a error happend while executing
		// next call of isOk() indicates state
		void setArrIdx(int idx, int arrayIdx);

		// next call of isOk() indicates state
		double retAt(int idx, int arrayIdx);

		// next call of isOk() indicates state, if array doesn't exist
		int retArrLength(int arrayIdx);

		// next call of isOk() indicates state
		int retArrIdx(int arrayIdx);

		// next call of isOk() indicates state
		void delAtIdx(int idx, int arrayIdx);

		// returns whenever the array index is valid
		// next call of isOk() indicates state
		boolean retCheckIdx(int arrayIdx);



		// checks if last call wasn't errorous
		boolean isOk();
	}

	public interface IDomainSpecificCtx extends IResetable {
		// called when the interpretation of a program ended successfully without any hard error(s)
		void terminatedInterpreationGracefully(Ctx ctx);

		// used to do an domain specific action
		// called/used by the ACTION instruction
		void action(Ctx ctx, int actionIdx);
	}

	



	// context for interpretation
	public static class Ctx {
		// execution context which shares common datastructures
		public static class ExecutionCtx {
			public int ip = 0;
			public IInstr[] instrs; // can be null if it is fetched from the main instruction indices
		}


		public Stack<Double> stack = new Stack<>();

		public boolean verbose = false;

		public boolean terminatedGracefully; // used to indicate a gracefully termination done by the program or a fatal unrecoverable error while executing the program

		public IDomainSpecificCtx domainSpecificCtx; // context used for specialized domain specific instructions
		public IArrayCtx arrayCtx; // context for the array

		public int skipedInstrsCnt; // number of remaining skiped instructions

		public Integer jumpRelative; // if it is not null then a relative jump has to be executed

		public boolean configInterpretInvalidIndicesAsTermination;

		public int remainingSteps = -2; // used to terminate the VM after a time horizon
                                        // is infinite if == -2



		List<Vm> vms = new ArrayList();
		int currentSelectedVmIdx = 0;

		public Vm retCurrentVm() {
			return vms.get(currentSelectedVmIdx);
		}

		public void reset() {
			if( verbose ) {
				System.out.println("reset");
			}

			terminatedGracefully = false;
			stack.clear();
			skipedInstrsCnt = 0;

			if( domainSpecificCtx != null )   domainSpecificCtx.reset();
			if( arrayCtx != null )   arrayCtx.reset();
		}

		// VM "context"
		// we need multiple VM's because we can change the VM as we please with special instructions
		//
		// each VM holds it's own macros so the instructions are customizable
		// idea to use multiple VM's is from https://ourarchive.otago.ac.nz/bitstream/handle/10523/1147/dp2004-01.pdf?sequence=3 "An Architecture for Self-Organising Evolvable Virtual Machines"
		public static class Vm {
			public Stack<Ctx.ExecutionCtx> execCtxStack = new Stack<>();


			// slots of custom instructions
			// are callable with XMACRO
			// can be manipulated with special X' instructions by the VM
			public List<XMacroSlot> xmacros = new ArrayList<>();

			public boolean flag; // used to control execution flow


			public static class XMacroSlot {
				public IInstr[] instrs;
			}
		}
	}

	// abstract instruction
	public interface IInstr {
		boolean exec(Ctx ctx);

		String retHumanName();
		String retExactHumanName(); // used to lookup parameterized instructions by memonic and arguments
	}


	// used to lookup a instruction by code/index
	public static class InstructionLookupTable {
		public IInstr[] table;

		public IInstr lookupByIdx(int idx) {
			return table[idx];
		}

		public IInstr lookupByExactHumanName(String humanName) {
			for( IInstr iInstr : table ) {
				if( iInstr.retExactHumanName().equals(humanName) )   return iInstr;
			}

			return null;
		}

		public InstrCode lookupIdxByExactHumanName(String humanName) throws Exception {
			for( int idx = 0; idx < table.length; idx++ ) {
				if( table[idx].retExactHumanName().equals(humanName) )   return new InstrCode(idx);
			}

			throw new Exception("Couldn't lookup exactHumanName \"" + humanName + "\"");
		}
	}

	static public class Interpreter {
		public Ctx ctx;
		public InstructionLookupTable instrLookup;

		public boolean interpret(int[] instrsIndicesParam) {
			ctx.terminatedGracefully = false;
			ctx.skipedInstrsCnt = 0;

			boolean execedOpSuccessfully = true;

			// we need to have a root-VM
			ctx.vms.clear();
			ctx.vms.add(new Ctx.Vm());

			// we need to have a root execution context
			Ctx.ExecutionCtx mainExecutionContext = new Ctx.ExecutionCtx();
			mainExecutionContext.instrs = null; // to fetch the instructions from instrsIndicesParam

			ctx.retCurrentVm().execCtxStack.clear();
			ctx.retCurrentVm().execCtxStack.push(mainExecutionContext);

			for(;;) {
				// limit time steps
				if( ctx.remainingSteps != -2 ) { // if the time limit is active
					assert ctx.remainingSteps != -1 && ctx.remainingSteps > -3; // we have some internal problem if this is false
					if( ctx.remainingSteps == 0 ) {
						return false;
					}

					ctx.remainingSteps--;
				}

				Ctx.ExecutionCtx cachedCurrentExecCtx = ctx.retCurrentVm().execCtxStack.peek();

				// check if ip is valid
				if( cachedCurrentExecCtx.instrs == null ) {
					if( cachedCurrentExecCtx.ip >= instrsIndicesParam.length )   break;
				}
				else {
					if( cachedCurrentExecCtx.ip >= cachedCurrentExecCtx.instrs.length )   break;
				}

				// ignore ignored instructions
				if( ctx.skipedInstrsCnt > 0 ) {
					ctx.skipedInstrsCnt--;
					cachedCurrentExecCtx.ip++;
					continue;
				}

				// execute relative jump
				if( ctx.jumpRelative != null ) {
					cachedCurrentExecCtx.ip += ctx.jumpRelative;
					ctx.jumpRelative = null;
					continue;
				}

				// check if ip is valid
				if( cachedCurrentExecCtx.ip < 0 || cachedCurrentExecCtx.ip >= (cachedCurrentExecCtx.instrs == null ? instrsIndicesParam.length : cachedCurrentExecCtx.instrs.length) ) {
					if( ctx.configInterpretInvalidIndicesAsTermination ) {
						break;
					}
					else {
						return false;
					}
				}

				// look up instruction / fetch
				IInstr instr; // looked up instruction
				if( cachedCurrentExecCtx.instrs == null ) {
					int instrIdx = instrsIndicesParam[cachedCurrentExecCtx.ip];
					instr = instrLookup.lookupByIdx(instrIdx);
				}
				else {
					instr = cachedCurrentExecCtx.instrs[ctx.retCurrentVm().execCtxStack.peek().ip];
				}

				// interpret / execute instruction
				execedOpSuccessfully = interpret(instr); // interpret single instruction in current VM
				cachedCurrentExecCtx = ctx.retCurrentVm().execCtxStack.peek(); // we need to update the cache because interpret could have changed it
				

				// valid terminating condition
				boolean isValidTermination;
				if( cachedCurrentExecCtx.instrs == null ) {
					isValidTermination = cachedCurrentExecCtx.ip == instrsIndicesParam.length;
				}
				else {
					isValidTermination = cachedCurrentExecCtx.ip == cachedCurrentExecCtx.instrs.length;
				}

				// TODO< pop of exec stack and check if it is empty, if it is we can terminate >
				if( isValidTermination ) {
					break;
				}

				if( !execedOpSuccessfully ) {
					if( ctx.terminatedGracefully ) {
						break;
					}
					else {
						return false;
					}
				}
			}

			if( !postExec() )   return false;

			return true;
		}

		boolean postExec() {
			ctx.terminatedGracefully = true;
			if( ctx.domainSpecificCtx != null )   ctx.domainSpecificCtx.terminatedInterpreationGracefully(ctx);

			return true;
			
		}

		// interpretation of the instruction
		boolean interpret(IInstr instr) {
			boolean res = false;
			if( instr instanceof XMACRO ) {
				XMACRO macroInstr = (XMACRO)instr;

				// add selected execution context so the VM interprets the instructions of the macro
				//  check
				if( macroInstr.macroIdx < 0 || macroInstr.macroIdx >= ctx.retCurrentVm().xmacros.size() ) { // is the macroIdx valid
					return false;
				}

				ctx.retCurrentVm().execCtxStack.peek().ip++;

				//  do it
				Ctx.ExecutionCtx mainExecutionContext = new Ctx.ExecutionCtx();
				mainExecutionContext.instrs = ctx.retCurrentVm().xmacros.get(macroInstr.macroIdx).instrs;
				ctx.retCurrentVm().execCtxStack.push(mainExecutionContext);
			}
			else {
				res = instr.exec(ctx);
				ctx.retCurrentVm().execCtxStack.peek().ip++;
			}
			
			if( !res ) {
				return false;
			}

			return true;
		}

		
	}

	// add - with or without resilence
	public static class ADD2 implements IInstr {
		public ADD2(boolean withResilence) {
			this.withResilence = withResilence;
		}

		public boolean exec(Ctx ctx) {
			if( ctx.stack.size() >= 2 ) {
				double inputA = ctx.stack.pop();
				double inputB = ctx.stack.pop();
				ctx.stack.push(inputA + inputB);
			}
			else {
				if( withResilence ) {
					// alternative codepath for resilence
					ctx.stack.push(0.0);
				}
				else {
					return false;
				}
			}

			return true;
		}

		public String retHumanName() {
			return "ADD" + (withResilence ? "_R" : "") + "(2)";
		}

		public String retExactHumanName() {
			return retHumanName();
		}

		boolean withResilence;
	}

	// sub - with or without resilence
	public static class SUB2 implements IInstr {
		public SUB2(boolean withResilence) {
			this.withResilence = withResilence;
		}

		public boolean exec(Ctx ctx) {
			if( ctx.stack.size() >= 2 ) {
				double inputA = ctx.stack.pop();
				double inputB = ctx.stack.pop();
				ctx.stack.push(inputA - inputB);
			}
			else {
				if( withResilence ) {
					// alternative codepath for resilence
					ctx.stack.push(0.0);
				}
				else {
					return false;
				}
			}

			return true;
		}

		public String retHumanName() {
			return "SUB" + (withResilence ? "_R" : "") + "(2)";
		}

		public String retExactHumanName() {
			return retHumanName();
		}

		boolean withResilence;
	}

	// mul - with resilence
	public static class MUL2 implements IInstr {
		public MUL2(boolean withResilence) {
			this.withResilence = withResilence;
		}

		public boolean exec(Ctx ctx) {
			if( ctx.stack.size() >= 2 ) {
				double inputA = ctx.stack.pop();
				double inputB = ctx.stack.pop();
				ctx.stack.push(inputA * inputB);
			}
			else {
				if( withResilence ) {
					// alternative codepath for resilence
					ctx.stack.push(1.0);
				}
				else {
					return false;
				}
			}

			return true;
		}

		public String retHumanName() {
			return "MUL" + (withResilence ? "_R" : "") + "(2)";
		}

		public String retExactHumanName() {
			return retHumanName();
		}

		boolean withResilence;
	}

	// div - with resilence
	public static class DIV2 implements IInstr {
		public DIV2(boolean withResilence) {
			this.withResilence = withResilence;
		}

		public boolean exec(Ctx ctx) {
			if( ctx.stack.size() >= 2 ) {
				double inputA = ctx.stack.pop();
				double inputB = ctx.stack.pop();
				if( inputB == 0.0f ) {
					if( withResilence ) {
						// alternative codepath for resilence
						ctx.stack.push(1.0);
					}
					else {
						return false;
					}
				}
				else {
					ctx.stack.push(inputA / inputB);
				}
			}
			else {
				if( withResilence ) {
					// alternative codepath for resilence
					ctx.stack.push(1.0);
				}
				else {
					return false;
				}
			}

			return true;
		}

		public String retHumanName() {
			return "DIV" + (withResilence ? "_R" : "") + "(2)";
		}

		public String retExactHumanName() {
			return retHumanName();
		}

		boolean withResilence;
	}

	// exp
	public static class EXP implements IInstr {
		public boolean exec(Ctx ctx) {
			if( ctx.stack.empty() )   return false;

			double inputA = ctx.stack.pop();
			double res = Math.exp(inputA);
			ctx.stack.push(res);
			return true;
		}

		public String retHumanName() {
			return "EXP";
		}

		public String retExactHumanName() {
			return retHumanName();
		}
	}

	// sqrt
	public static class SQRT implements IInstr {
		public boolean exec(Ctx ctx) {
			if( ctx.stack.empty() )   return false;

			double inputA = ctx.stack.pop();
			double res = Math.sqrt(inputA);
			ctx.stack.push(res);
			return true;
		}

		public String retHumanName() {
			return "SQRT";
		}

		public String retExactHumanName() {
			return retHumanName();
		}
	}

	// sin
	public static class SIN implements IInstr {
		public boolean exec(Ctx ctx) {
			if( ctx.stack.empty() )   return false;

			double inputA = ctx.stack.pop();
			double res = Math.sin(inputA);
			ctx.stack.push(res);
			return true;
		}

		public String retHumanName() {
			return "SIN";
		}

		public String retExactHumanName() {
			return retHumanName();
		}
	}

	// log
	public static class LOG implements IInstr {
		public boolean exec(Ctx ctx) {
			if( ctx.stack.empty() )   return false;

			double inputA = ctx.stack.pop();
			double res = Math.log(inputA);
			ctx.stack.push(res);
			return true;
		}

		public String retHumanName() {
			return "LOG";
		}

		public String retExactHumanName() {
			return retHumanName();
		}
	}

	// tanh
	public static class TANH implements IInstr {
		public boolean exec(Ctx ctx) {
			if( ctx.stack.empty() )   return false;

			double inputA = ctx.stack.pop();
			double res = Math.tanh(inputA);
			ctx.stack.push(res);
			return true;
		}

		public String retHumanName() {
			return "TANH";
		}

		public String retExactHumanName() {
			return retHumanName();
		}
	}

	// atan
	public static class ATAN implements IInstr {
		public boolean exec(Ctx ctx) {
			if( ctx.stack.empty() )   return false;

			double inputA = ctx.stack.pop();
			double res = Math.atan(inputA);
			ctx.stack.push(res);
			return true;
		}

		public String retHumanName() {
			return "ATAN";
		}

		public String retExactHumanName() {
			return retHumanName();
		}
	}

	// control flow

	public static class JMP implements IInstr {
		int delta;

		public JMP(int delta) {
			this.delta = delta;
		}

		public boolean exec(Ctx ctx) {
			ctx.jumpRelative = new Integer(delta);
			return true;
		}

		public String retHumanName() {
			return delta == 0 ? "NOP" : "JMP";
		}

		public String retExactHumanName() {
			return delta == 0 ? "NOP" : "JMP " + Integer.toString(delta);
		}
	}


	// misc/helpers

	public static class CMPGT implements IInstr {
		private boolean enablePop;

		public CMPGT(boolean enablePop) {
			this.enablePop = enablePop;
		}

		public boolean exec(Ctx ctx) {
			if( ctx.stack.size() < 2 )   return false;

			double inputA = ctx.stack.get(ctx.stack.size()-1);
			double inputB = ctx.stack.get(ctx.stack.size()-2);
			ctx.retCurrentVm().flag = inputA > inputB;
			if( enablePop ) {
				ctx.stack.pop();
				ctx.stack.pop();
			}
			return true;
		}

		public String retHumanName() {
			return enablePop ? "CMPGTPOP" : "CMPGT";
		}

		public String retExactHumanName() {
			return retHumanName();
		}
	}

	public static class CMPEQUAL implements IInstr {
		private boolean enablePop;

		public CMPEQUAL(boolean enablePop) {
			this.enablePop = enablePop;
		}

		public boolean exec(Ctx ctx) {
			if( ctx.stack.size() < 2 )   return false;

			double inputA = ctx.stack.get(ctx.stack.size()-1);
			double inputB = ctx.stack.get(ctx.stack.size()-2);
			ctx.retCurrentVm().flag = Math.abs(inputA - inputB) < 0.001; // epsilon
			if( enablePop ) {
				ctx.stack.pop();
				ctx.stack.pop();
			}
			return true;
		}

		public String retHumanName() {
			return enablePop ? "CMPEQUALPOP" : "CMPEQUAL";
		}

		public String retExactHumanName() {
			return retHumanName();
		}
	}

	public static class PUSHCONST implements IInstr {
		double v = 0.0f;
		String exactHumanName;

		public PUSHCONST(String exactHumanName, double v) {
			this.exactHumanName = exactHumanName;
			this.v = v;
		}

		public boolean exec(Ctx ctx) {
			ctx.stack.push(v);
			return true;
		}

		public String retHumanName() {
			return "PUSHCONST";
		}

		public String retExactHumanName() {
			return exactHumanName;
		}
	}

	public static class POP implements IInstr {
		public boolean exec(Ctx ctx) {
			if( ctx.stack.empty() )   return false;
			ctx.stack.pop();
			return true;
		}

		public String retHumanName() {
			return "POP";
		}

		public String retExactHumanName() {
			return retHumanName();
		}
	}

	public static class XCHG implements IInstr {
		int rel;

		public XCHG() {
			rel = 1;
		}

		public XCHG(int rel) {
			this.rel = rel;
		}

		public boolean exec(Ctx ctx) {
			if( ctx.stack.size() < rel + 1 )   return false;
			double inputA = ctx.stack.get(ctx.stack.size()-1);
			double inputB = ctx.stack.get(ctx.stack.size()-1-rel);

			ctx.stack.set(ctx.stack.size()-1-rel, inputA);
			ctx.stack.set(ctx.stack.size()-1, inputB);

			return true;
		}

		public String retHumanName() {
			return "XCHG";
		}

		public String retExactHumanName() {
			return retHumanName() + (rel == 1 ? "" : " " + Integer.toString(rel));
		}
	}

	public static class DUP implements IInstr {
		public boolean exec(Ctx ctx) {
			if( ctx.stack.size() < 1 )   return false;
			ctx.stack.push(ctx.stack.peek());
			return true;
		}

		public String retHumanName() {
			return "DUP";
		}

		public String retExactHumanName() {
			return retHumanName();
		}
	}

	// generic action
	public static class ACTION implements IInstr {
		int actionIdx;

		public ACTION(int actionIdx) {
			this.actionIdx = actionIdx;
		}

		public boolean exec(Ctx ctx) {
			if( ctx.domainSpecificCtx == null )   return false;

			ctx.domainSpecificCtx.action(ctx, actionIdx);
			return true;
		}

		public String retHumanName() {
			return "ACTION";
		}

		public String retExactHumanName() {
			return "ACTION " + Integer.toString(actionIdx);
		}
	}

	// predicate to ignore next instruction(s) (numer of ignored instructions is the argument) if flag is set
	public static class PREDIGNOREIFFLAG implements IInstr {
		int nInstrs; // number of skiped instructions
		boolean flagCheckValue;

		public PREDIGNOREIFFLAG(int nInstrs, boolean flagCheckValue) {
			this.nInstrs = nInstrs;
			this.flagCheckValue = flagCheckValue;
		}

		public boolean exec(Ctx ctx) {
			if( ctx.retCurrentVm().flag == flagCheckValue ) {
				ctx.skipedInstrsCnt = nInstrs;
			}
			return true;
		}

		public String retHumanName() {
			return flagCheckValue ? "PREDIGNOREIFFLAG" : "PREDIGNOREIFNOTFLAG";
		}

		public String retExactHumanName() {
			return retHumanName() + " " + Integer.toString(nInstrs);
		}
	}

	// special preprocessor "instruction" (which is not executable)
	// instructs instruction preprocessor to insert a macro into the program
	public static class MACRO implements IInstr {
		public int macroIdx; // must be public to be visible by the preprocessor

		public MACRO(int macroIdx) {
			this.macroIdx = macroIdx;
		}

		public boolean exec(Ctx ctx) {
			return false; // not executable
		}

		public String retHumanName() {
			return "MACRO";
		}

		public String retExactHumanName() {
			return "MACRO " + Integer.toString(macroIdx);
		}
	}

	// execution based macro - which is just a list of instructions which are executed with their own jump-context
	public static class XMACRO implements IInstr {
		public int macroIdx; // must be public to be visible by the interpreter

		public XMACRO(int macroIdx) {
			this.macroIdx = macroIdx;
		}

		public boolean exec(Ctx ctx) {
			return false; // not executable
		}

		public String retHumanName() {
			return "XMACRO";
		}

		public String retExactHumanName() {
			return "XMACRO " + Integer.toString(macroIdx);
		}
	}


	//////////////////////////////////
	//////////////////////////////////
	//////////////////////////////////
	/// TODO = repalce with macros
	

	public static class MULVALUE implements IInstr {
		double v;
		String humanValue;

		public MULVALUE(double v, String humanValue) {
			this.v = v;
			this.humanValue = humanValue;
		}

		public boolean exec(Ctx ctx) {
			if( ctx.stack.empty() )   return false;
			double inputA = ctx.stack.pop();
			double inputB = v;
			ctx.stack.push(inputA * inputB);
			return true;
		}

		public String retHumanName() {
			return "MULVALUE";
		}

		public String retExactHumanName() {
			return "MULVALUE " + humanValue;
		}
	}




	// TODO< reencode PONG and remove >
	public static class PUSHIFFLAG implements IInstr {
		double trueV, falseV;

		public PUSHIFFLAG(double falseV, double trueV) {
			this.falseV = falseV;
			this.trueV = trueV;
		}

		public boolean exec(Ctx ctx) {
			ctx.stack.push(ctx.retCurrentVm().flag ? trueV : falseV);
			return true;
		}

		public String retHumanName() {
			return "PUSHIFFLAG";
		}

		public String retExactHumanName() {
			return "PUSHIFFLAG " + Double.toString(falseV) + " " + Double.toString(trueV);
		}
	}


	// TODO< write as XMACRO which uses PUSHCONST 0, CMPGTPOP
	// TODO< write as XMACRO which uses PUSHCONST 0, CMPGT, POP
	// compare greater than zero, update flag, pop (if enabled)
	public static class CMPGTZERO implements IInstr {
		private boolean enablePop;

		public CMPGTZERO(boolean enablePop) {
			this.enablePop = enablePop;
		}

		public boolean exec(Ctx ctx) {
			if( ctx.stack.empty() )   return false;

			double inputA = ctx.stack.peek();
			ctx.retCurrentVm().flag = inputA > 0;
			if( enablePop )   ctx.stack.pop();
			return true;
		}

		public String retHumanName() {
			return enablePop ? "CMPGTZEROPOP" : "CMPGTZERO";
		}

		public String retExactHumanName() {
			return retHumanName();
		}
	}


}

