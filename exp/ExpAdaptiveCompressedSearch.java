package exp;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Deflater;

import misc.Timer;
import misc.IConsole;

import instr.InstrCode;
import instr.InstrUtil;
import instr.ArrayInstrs;
import instr.HardwiredPrograms;
import instr.Program;

import compress.ProgramCompressor;

import exec.ctx.DefaultArrCtx;
import ls.Instrs;
import uncategorized.ProgramUsefulChecker;

import env.Pong;


// experiment to play around with compression of programs
// is using the Hash-based compressor because it is way faster than using gzip

// initialization
// (0) feed te ompressor with primary fragments from programs the AI already knows

// (repeated) steps are
// (a) enumerate possible programs out of fragments and instructions
// (b) compress the programs and store the # of bits each program needs
// (c) order the programs from low # of compressed bits to high
// (c) enumerate ordered programs

// we can't enumerate all possible or feasable programs with (a) because
// * number may be very large
// * solution(s) could hide in the stored programs generated with (a)
class ExpAdaptiveCompressedSearch {
	// keeps track of the instructions of the current program and supplies methods to add instructions to it
	// used to generate candidates for compression
	// TODO< move class to own file in compress >
	public static class CompressionCandidateGen {
		public Program prgrm; // working program
		public double prob = 1.0; // normalized product of all probabilities of the used instructions/fragments

		FragmentDatabase database; // archive to choose snippets from

		public CompressionCandidateGen(FragmentDatabase database) {
			this.database = database;
		}

		public void resetProgram() {
			prgrm = new Program();
		}

		// /param instr instruction to be appended
		// /param instrProb probability of the appended instruction
		public void appendInstr(InstrCode instr, double instrProb) {
			prgrm.instrs.add(instr);

			prob *= instrProb;
		}

		public void appendRandomSnippetWithMaxLength(int maxlen) {
			Fragment fragment = null;

			// find fragment which fullfills length condition
			for(;;) {
				int fragmentTupleIdx = database.retRandomIdx(rng);
				Fragment candidateFragment = database.retFragmentAtIdx(fragmentTupleIdx);

				if( candidateFragment.instrs.size() <= maxlen ) {
					// update probability of program
					prob *= database.retNormalizedProbAtIdx(fragmentTupleIdx);


					fragment = candidateFragment;
					break;
				}
			}

			// append slice/fragment
			for( InstrCode iInstr : fragment.instrs ) {
				prgrm.instrs.add(iInstr);
			}

		}

		Random rng = new Random();
	}

	// is a whole program or a part of it
	public static class Fragment {
		public List<InstrCode> instrs = new ArrayList<>();

		public Fragment(List<InstrCode> instrs) {
			this.instrs = instrs;
		}
	}
	
	// works internaly with a tuple of a (not normalized) probability and the corresponding fragment, which is a part of a program
	public static class FragmentDatabase {
		public List<FragmentProbabilityTuple> tuples = new ArrayList<>();

		double probabilityMass;


		public void setFragmentsWithUnnormalizedProbabilitiesAndUpdate(List<FragmentProbabilityTuple> tuples) {
			this.tuples.clear();
			for( FragmentProbabilityTuple iTuple : tuples ) {
				assert iTuple.fragment.instrs.size() > 0;

				this.tuples.add(iTuple);
			}

			calcProbabilityMass();
			calcNormalizedProbabilities();
		}

		void calcProbabilityMass() {
			probabilityMass = 0;
			for( FragmentProbabilityTuple iTuple : tuples ) {
				probabilityMass += iTuple.unormalizedProbability;
			}
		}

		void calcNormalizedProbabilities() {
			for( FragmentProbabilityTuple iTuple : tuples ) {
				iTuple.normalizedProbability = iTuple.unormalizedProbability / probabilityMass;
			}
		}

		// returns a random index of the tuples 
		public int retRandomIdx(Random rng) {
			double randNormalized = rng.nextDouble();
			double randUnnormalized = randNormalized * probabilityMass;

			double accumulatedProbabilityMass = 0;
			int idx = 0;
			for( FragmentProbabilityTuple iTuple : tuples ) {
				accumulatedProbabilityMass += iTuple.unormalizedProbability;
				if( accumulatedProbabilityMass > randUnnormalized ) {
					return idx;
				}

				idx++;
			}

			return tuples.size()-1;
		}

		public Fragment retFragmentAtIdx(int idx) {
			return tuples.get(idx).fragment;
		}

		public double retNormalizedProbAtIdx(int idx) {
			return tuples.get(idx).normalizedProbability;
		}


		public static class FragmentProbabilityTuple {
			public double unormalizedProbability;
			public double normalizedProbability;
			public Fragment fragment;

			public FragmentProbabilityTuple(Fragment fragment, double unormalizedProbability) {
				this.fragment = fragment;
				this.unormalizedProbability = unormalizedProbability;
			}
		}

	}

	public static class ProgramWithNumberOfBitsTuple implements Comparable<ProgramWithNumberOfBitsTuple> {
		public Program<InstrCode> program;
		public double numberOfBits = -1; // number of bits required for encoding the compressed program
		                                 // negative number indicates that it is not initialized

		public ProgramWithNumberOfBitsTuple(Program<InstrCode> program) {
			this.program = program;
		}

		@Override
		public int compareTo(ProgramWithNumberOfBitsTuple o) {
			if( this.numberOfBits > o.numberOfBits )   return 1;
			if( this.numberOfBits < o.numberOfBits )   return -1;
			return 0;

			//return (int)(this.numberOfBits - o.numberOfBits);
		}
	}

	static class ConsoleImpl implements IConsole {
		// implementation of a pipe
		static class ConsolePipeImpl implements IConsole.IConsolePipe {
			public void pushPrefix(String str) {
				prefixes.add(str);
			}

			public void putInt(String key, int value) {
				integerMap.put(key, value);
			}

			// called when it has to be translated to a single string with multiple lines
			public String composeString() {
				String res = "";
				for( String iPrefix : prefixes )   res += (iPrefix + ": ");

				for( Map.Entry<String, Integer> entry : integerMap.entrySet() ) {
					res = res + entry.getKey() + "=" + Integer.toString(entry.getValue()) + ",";
				}

				return res;
			}

			public List<String> prefixes = new ArrayList<>();
			public Map<String, Integer> integerMap = new HashMap<>();
		}

		public IConsole.IConsolePipe createPipe() {
			ConsolePipeImpl res = new ConsolePipeImpl();
			consolePipes.add(res);
			return res;
		}

		// called from outside when it could dump the state to the real console
		public void dumpToConsoleChance() {
			// TODO< check for timer and call dumpToConsole if enough time has passed
			// for now we just dump it to the console
			dumpToConsole();
		}

		void dumpToConsole() {
			for( ConsolePipeImpl iConsolePipe : consolePipes ) {
				System.out.format("%s\n", iConsolePipe.composeString());
			}
		}

		List<ConsolePipeImpl> consolePipes = new ArrayList<>();
	}


	public static void main(String[] args) throws Exception {
		int verbosity = 2;

		IConsole console = new ConsoleImpl();

		int numberOfTriedPrograms = 0;


		ProgramCompressor.HashBasedFastProgramCompressor hashBasedProgramCompressor = new ProgramCompressor.HashBasedFastProgramCompressor();


		// used to choose the fragments when we compose our "program under test"
		FragmentDatabase fragmentDatabase = new FragmentDatabase();
		List<FragmentDatabase.FragmentProbabilityTuple> fragmentDatabaseTuples = new ArrayList<>(); // because we will feed this into the fragment database


		Instrs.InstructionLookupTable instrLookupTable = new Instrs.InstructionLookupTable();


		{ // config
			int numberOfInstructions = 45;
			int maximalPrimaryLength = 10;
			int maximalSecondaryLength = 10;
			int maximalSliceLength = 10;
			hashBasedProgramCompressor.config(numberOfInstructions, maximalPrimaryLength, maximalSecondaryLength, maximalSliceLength);
		}


		// TODO< move to global static method for the whole AI >

		{ // fill instruction table
			instrLookupTable.table = new Instrs.IInstr[46];
			int instrLookupTableIdx = 0;
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.JMP(0); // NOP - useful for filling gaps, this way we don't need to allocate as many instructions for control flow purposes
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.ADD2(true); // ADD_R(2)
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.MUL2(true); // MUL_R(2)
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.DIV2(true); // DIV_R(2)
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.CMPGTZERO(false); // CMPGTZERO
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.CMPGTZERO(true); // CMPGTZEROPOP
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.CMPGT(false); // CMPGT
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.PUSHCONST("PUSH -1", -1);
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.XCHG();
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.DUP();
			
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.MULVALUE(-1, "-1"); // used for pong example
			// commented because it got decomposed, is enumerated as fragments
			//instrLookupTable.table[instrLookupTableIdx++] = new Instrs.PUSHIFFLAG(0.0f, 1.0f); // used for pong example    TODO< decompose into subprogram and call it for pong example >
			//instrLookupTable.table[instrLookupTableIdx++] = new Instrs.PUSHIFFLAG(0.0f, -1.0f); // used for pong example   TODO< decompose into subprogram and call it for pong example >

			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.PUSHCONST("PUSH 0", 0); // used for program to clear array

			// control flow
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.PREDIGNOREIFFLAG(4, false);
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.PREDIGNOREIFFLAG(5, false);
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.PREDIGNOREIFFLAG(7, false);
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.JMP(-9);
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.JMP(-7);
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.JMP(-5);
			//instrLookupTable.table[instrLookupTableIdx++] = new Instrs.JMP(-3);




			// instruction page : array instructions
			instrLookupTable.table[instrLookupTableIdx++] = new ArrayInstrs.ARRPOPAPPEND(0);
			instrLookupTable.table[instrLookupTableIdx++] = new ArrayInstrs.ARRDEL(0);
			instrLookupTable.table[instrLookupTableIdx++] = new ArrayInstrs.ARRLEN(0);
			instrLookupTable.table[instrLookupTableIdx++] = new ArrayInstrs.ARRSETIDX(0);
			instrLookupTable.table[instrLookupTableIdx++] = new ArrayInstrs.ARRSETIDX(1);
			instrLookupTable.table[instrLookupTableIdx++] = new ArrayInstrs.ARRCHECKIDX(0);
			instrLookupTable.table[instrLookupTableIdx++] = new ArrayInstrs.ARRREADNPUSH(0);
			instrLookupTable.table[instrLookupTableIdx++] = new ArrayInstrs.ARRPOPAPPEND(1);

			instrLookupTable.table[instrLookupTableIdx++] = new ArrayInstrs.ARRREL(-1, 0, "ARRREL -1 0");
			instrLookupTable.table[instrLookupTableIdx++] = new ArrayInstrs.ARRREL(1, 0, "ARRREL 1 0");




			// instruction page : math and misc
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.SQRT();
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.EXP();
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.SIN();
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.LOG();

			// TODO< BXOR, BOR, BNOT, BAND > - binary basic operations

			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.POP(); // used to throw unnecessary values on the stack away
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.PREDIGNOREIFFLAG(4, true); // in this page because it is not used often (just in the math min/max code)




			// instruction page : special control flow
			// contains instructions used to change virtual machines
			// contains instructions for control flow which are not ofter used

			// TODO 







			// special instructions just for hardwired programs
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.PREDIGNOREIFFLAG(9, false);
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.XCHG(2);
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.JMP(-10);
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.JMP(-13);
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.PUSHCONST("PUSH 1", 1);

			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.ADD2(false); // ADD(2)
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.MUL2(false); // MUL(2)
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.DIV2(false); // DIV(2)

			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.PREDIGNOREIFFLAG(2, true);
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.PREDIGNOREIFFLAG(3, true);
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.JMP(1);
			instrLookupTable.table[instrLookupTableIdx++] = new Instrs.CMPEQUAL(false); // CMPEQUAL


		}








		StringBuilder instrsAsStrBuilder = new StringBuilder();

		// programs which will be fed as the primary programs to the compressor
		List<Program<InstrCode>> compressionPrimaryProgramCandidates = new ArrayList<>();


		// iterate over all possible programs with a maximal length and add the programs which generate sensible results
		boolean bootstrapWithUsefulShortPrograms = true;
			boolean boostrapWithMathematicPrograms = false;
			boolean boostrapWithFlagDependentPrograms = true;

		if( bootstrapWithUsefulShortPrograms ) {
			List<Program<InstrCode>> enumeratedProgramsOfCurrentCycle = new ArrayList<>();

			int numberOfGeneratedBoostrapPrograms = 0; // counter


			if( boostrapWithMathematicPrograms ) { // mathematical programs
				int maximalLengthOfProgram = 4;

				InstrCode[] instructionsToUse = new InstrCode[12];
				instructionsToUse[0] = instrLookupTable.lookupIdxByExactHumanName("NOP");
				instructionsToUse[1] = instrLookupTable.lookupIdxByExactHumanName("ADD(2)");
				// TODO< sub >
				instructionsToUse[2] = instrLookupTable.lookupIdxByExactHumanName("MUL(2)");
				instructionsToUse[3] = instrLookupTable.lookupIdxByExactHumanName("DIV(2)");

				instructionsToUse[4] = instrLookupTable.lookupIdxByExactHumanName("XCHG");
				instructionsToUse[5] = instrLookupTable.lookupIdxByExactHumanName("DUP");
				
				instructionsToUse[6] = instrLookupTable.lookupIdxByExactHumanName("PUSH 0");
				instructionsToUse[7] = instrLookupTable.lookupIdxByExactHumanName("PUSH 1");
				instructionsToUse[7] = instrLookupTable.lookupIdxByExactHumanName("PUSH -1");

				instructionsToUse[8] = instrLookupTable.lookupIdxByExactHumanName("SQRT");
				instructionsToUse[9] = instrLookupTable.lookupIdxByExactHumanName("EXP");
				instructionsToUse[10] = instrLookupTable.lookupIdxByExactHumanName("SIN");
				instructionsToUse[11] = instrLookupTable.lookupIdxByExactHumanName("LOG");

				int numberOfInstructions = instructionsToUse.length;

				// initialize interpreter
				Instrs.Interpreter interpreter = new Instrs.Interpreter();
				interpreter.instrLookup = instrLookupTable;
				interpreter.ctx = new Instrs.Ctx();
				interpreter.ctx.arrayCtx = new DefaultArrCtx();

				for( int programLength = 1; programLength <= maximalLengthOfProgram; programLength++ ) {
					int[] programInstrIndices = new int[programLength];

					// TODO< use own long math-pow >
					for( long i = 0; i < Math.pow(numberOfInstructions, programLength); i++ ) {
						// translate i to the program
						long rem = i;
						for( int idx = 0; idx < programLength; idx++ ) {
							programInstrIndices[idx] = (int)(rem % numberOfInstructions);
							rem /= numberOfInstructions;
						}

						// translate the indices to the instructions to the instructions (of the program)
						for( int idx = 0; idx < programLength; idx++ ) {
							programInstrIndices[idx] = instructionsToUse[programInstrIndices[idx]].instrIdx;
						}

						int numberOfEntryArgs = 2;
						boolean programTerminatesAndReturnsSensibleResult = ProgramUsefulChecker.checkProgramReturnsSensibleRes(interpreter, programInstrIndices, numberOfEntryArgs);

						if( programTerminatesAndReturnsSensibleResult ) {
							if( verbosity > 1 )   System.out.format("found terminating program %s\n",  Arrays.toString(programInstrIndices));
							numberOfGeneratedBoostrapPrograms++;

							// because we eventually need to extract fragments
							{
								Program program = new Program();
								for( int iInstrIdx : programInstrIndices ) {
									program.instrs.add(new InstrCode(iInstrIdx));
								}

								enumeratedProgramsOfCurrentCycle.add(program);
								compressionPrimaryProgramCandidates.add(program);
							}
						}
					}
				}
			}



			// add all slices or all enumerated fragments to database
			if( true ) {
				for( Program<InstrCode> iProgram : enumeratedProgramsOfCurrentCycle ) {

					// add slices
					for( int sliceStartIdx = 0; sliceStartIdx < iProgram.instrs.size(); sliceStartIdx++ ) {
						for( int sliceLength = 2; sliceStartIdx + sliceLength <= iProgram.instrs.size(); sliceLength++ ) {

							// build program for slice/fragment
							List<InstrCode> instrs = new ArrayList<>();
							for( int offset = 0; offset < sliceLength; offset++ ) {
								instrs.add(iProgram.instrs.get(sliceStartIdx + offset));
							}
							assert instrs.size() > 0;
							Fragment fragment = new Fragment(instrs);


							fragmentDatabaseTuples.add(new FragmentDatabase.FragmentProbabilityTuple(fragment, 0.1));
						}
					}
				}
			}
			else {
				for( Program iProgram : enumeratedProgramsOfCurrentCycle ) {
					fragmentDatabaseTuples.add(new FragmentDatabase.FragmentProbabilityTuple(new Fragment(iProgram.instrs), 0.1));
				}
			}





			enumeratedProgramsOfCurrentCycle.clear();


			if( boostrapWithFlagDependentPrograms ) { // programs to load and do operations based on flag

				int maximalLengthOfProgram = 4;

				InstrCode[] instructionsToUse = new InstrCode[9];
				instructionsToUse[0] = instrLookupTable.lookupIdxByExactHumanName("NOP");

				instructionsToUse[1] = instrLookupTable.lookupIdxByExactHumanName("XCHG");
				instructionsToUse[2] = instrLookupTable.lookupIdxByExactHumanName("DUP");
				
				instructionsToUse[3] = instrLookupTable.lookupIdxByExactHumanName("PUSH 0");
				instructionsToUse[4] = instrLookupTable.lookupIdxByExactHumanName("PUSH 1");
				instructionsToUse[5] = instrLookupTable.lookupIdxByExactHumanName("PUSH -1");

				instructionsToUse[6] = instrLookupTable.lookupIdxByExactHumanName("PREDIGNOREIFFLAG 2");
				instructionsToUse[7] = instrLookupTable.lookupIdxByExactHumanName("PREDIGNOREIFFLAG 3");
				instructionsToUse[8] = instrLookupTable.lookupIdxByExactHumanName("JMP 1");

				int numberOfInstructions = instructionsToUse.length;

				// initialize interpreter
				Instrs.Interpreter interpreter = new Instrs.Interpreter();
				interpreter.instrLookup = instrLookupTable;
				interpreter.ctx = new Instrs.Ctx();
				interpreter.ctx.arrayCtx = new DefaultArrCtx();

				for( int programLength = 1; programLength <= maximalLengthOfProgram; programLength++ ) {
					int[] programInstrIndices = new int[programLength];


					// TODO< use own long math-pow >
					for( long i = 0; i < Math.pow(numberOfInstructions, programLength); i++ ) {
						// translate i to the program
						long rem = i;
						for( int idx = 0; idx < programLength; idx++ ) {
							programInstrIndices[idx] = (int)(rem % numberOfInstructions);
							rem /= numberOfInstructions;
						}

						// translate the indices to the instructions to the instructions (of the program)
						for( int idx = 0; idx < programLength; idx++ ) {
							programInstrIndices[idx] = instructionsToUse[programInstrIndices[idx]].instrIdx;
						}





						// FOR DEBUGGING
						// set program for testing
						//if( programLength == 4 ) {
						//	programInstrIndices[0] = instructionsToUse[6].instrIdx;
						//	programInstrIndices[1] = instructionsToUse[3].instrIdx;
						//	programInstrIndices[2] = instructionsToUse[8].instrIdx;
						//	programInstrIndices[3] = instructionsToUse[4].instrIdx;
						//}





						int numberOfEntryArgs = 0;
						boolean programTerminatesAndReturnsSensibleResult = ProgramUsefulChecker.checkProgramReturnsSensibleRes(interpreter, programInstrIndices, numberOfEntryArgs);

						if( programTerminatesAndReturnsSensibleResult ) {
							if( verbosity > 1 ) {
								System.out.format("found terminating program\n",  Arrays.toString(programInstrIndices));


								if( verbosity > 2 ) {
									// dump program to console
									for( int iInstrIdx : programInstrIndices ) {
										System.out.format("   %s\n", instrLookupTable.lookupByIdx(iInstrIdx).retExactHumanName());
									}
								}
							}  
							numberOfGeneratedBoostrapPrograms++;

							// because we eventually need to extract fragments
							{
								Program program = new Program();
								for( int iInstrIdx : programInstrIndices ) {
									program.instrs.add(new InstrCode(iInstrIdx));
								}

								enumeratedProgramsOfCurrentCycle.add(program);
								compressionPrimaryProgramCandidates.add(program);
							}
						}
					}
				}


			}



			// add all slices or all enumerated fragments to database
			if( false ) {
				for( Program<InstrCode> iProgram : enumeratedProgramsOfCurrentCycle ) {

					// add slices
					for( int sliceStartIdx = 0; sliceStartIdx < iProgram.instrs.size(); sliceStartIdx++ ) {
						for( int sliceLength = 2; sliceStartIdx + sliceLength <= iProgram.instrs.size(); sliceLength++ ) {

							// build program for slice/fragment
							List<InstrCode> instrs = new ArrayList<>();
							for( int offset = 0; offset < sliceLength; offset++ ) {
								instrs.add(iProgram.instrs.get(sliceStartIdx + offset));
							}
							assert instrs.size() > 0;
							Fragment fragment = new Fragment(instrs);


							fragmentDatabaseTuples.add(new FragmentDatabase.FragmentProbabilityTuple(fragment, 0.1));
						}
					}
				}
			}
			else {
				for( Program<InstrCode> iProgram : enumeratedProgramsOfCurrentCycle ) {
					fragmentDatabaseTuples.add(new FragmentDatabase.FragmentProbabilityTuple(new Fragment(iProgram.instrs), 0.1));
				}
			}



			if( verbosity > 0 )   System.out.format("# of found sensible programs=%d\n", numberOfGeneratedBoostrapPrograms);
		}




		/* commented because not required
		{ // boost the probability of the "useful" programs to bias the search
			// calculate the sum of the unnormalized probabilities of all fragments
			double unnormalizedProbabilityMassSum = 0;

			for( FragmentDatabase.FragmentProbabilityTuple iFragmentProbabilityTuple : fragmentDatabaseTuples ) {
				unnormalizedProbabilityMassSum += iFragmentProbabilityTuple.unormalizedProbability;
			}


			// boost the probability of specific fragments
			// FRAGMENT
			//     PREDIGNOREIFFLAG 2
			//     PUSH 0
			//     JMP 1
			//     PUSH 1
			{
				int instrIdxOfPredignoreOfFlag2 = instrLookupTable.lookupIdxByExactHumanName("PREDIGNOREIFFLAG 2").instrIdx;
				int instrIdxOfPush0 = instrLookupTable.lookupIdxByExactHumanName("PUSH 0").instrIdx;
				int instrIdxOfJmp1 = instrLookupTable.lookupIdxByExactHumanName("JMP 1").instrIdx;
				int instrIdxOfPush1 = instrLookupTable.lookupIdxByExactHumanName("PUSH 1").instrIdx;

				for( FragmentDatabase.FragmentProbabilityTuple  iFragmentProbabilityTuple : fragmentDatabaseTuples ) {
					if(
						iFragmentProbabilityTuple.fragment.instrs.size() == 4 &&
						iFragmentProbabilityTuple.fragment.instrs.get(0).instrIdx == instrIdxOfPredignoreOfFlag2 &&
						iFragmentProbabilityTuple.fragment.instrs.get(1).instrIdx == instrIdxOfPush0 &&
						iFragmentProbabilityTuple.fragment.instrs.get(2).instrIdx == instrIdxOfJmp1 &&
						iFragmentProbabilityTuple.fragment.instrs.get(3).instrIdx == instrIdxOfPush1
					) {
						iFragmentProbabilityTuple.unormalizedProbability = unnormalizedProbabilityMassSum * 5.0; // boost probability so it is ~ 5 times as likelike to select this instead of other fragments

						System.out.format("found and modified probability of fragment A\n");
						break;
					}
				}
			}

			// FRAGMENT
			//     PREDIGNOREIFFLAG 2
			//     PUSH 0
			//     JMP 1
			//     PUSH -1
			{
				int instrIdxOfPredignoreOfFlag2 = instrLookupTable.lookupIdxByExactHumanName("PREDIGNOREIFFLAG 2").instrIdx;
				int instrIdxOfPush0 = instrLookupTable.lookupIdxByExactHumanName("PUSH 0").instrIdx;
				int instrIdxOfJmp1 = instrLookupTable.lookupIdxByExactHumanName("JMP 1").instrIdx;
				int instrIdxOfPushM1 = instrLookupTable.lookupIdxByExactHumanName("PUSH -1").instrIdx;

				for( FragmentDatabase.FragmentProbabilityTuple  iFragmentProbabilityTuple : fragmentDatabaseTuples ) {
					if(
						iFragmentProbabilityTuple.fragment.instrs.size() == 4 &&
						iFragmentProbabilityTuple.fragment.instrs.get(0).instrIdx == instrIdxOfPredignoreOfFlag2 &&
						iFragmentProbabilityTuple.fragment.instrs.get(1).instrIdx == instrIdxOfPush0 &&
						iFragmentProbabilityTuple.fragment.instrs.get(2).instrIdx == instrIdxOfJmp1 &&
						iFragmentProbabilityTuple.fragment.instrs.get(3).instrIdx == instrIdxOfPushM1
					) {
						iFragmentProbabilityTuple.unormalizedProbability = unnormalizedProbabilityMassSum * 5.0; // boost probability so it is ~ 5 times as likelike to select this instead of other fragments

						System.out.format("found and modified probability of fragment B\n");
						break;
					}
				}
			}
		}
		//*/







		fragmentDatabase.setFragmentsWithUnnormalizedProbabilitiesAndUpdate(fragmentDatabaseTuples);

		if( verbosity > 0 )   System.out.format("main : feed primary\n");
		
		// feed compressor with primary programs
		hashBasedProgramCompressor.primary(compressionPrimaryProgramCandidates);
		
		if( verbosity > 0 )   System.out.format("main : feed primary done\n");




		// we try more and more programs per cycle
		// we do this to try simple programs quickly and not bias the order of evaluated programs by the compression ratio that much
		//
		// we need to limit the # of programs per cycle because the memory of a machine is limited

		// configuration for # of tried programs per cycle
		double numberOfGeneratedProgramsPerCyleBase = 50000;
		//   1.0 : linear increase in # of programs tried per cycle
		double numberOfGeneratedProgramsPerCyleExponent = 1.7;

		long numberOfGeneratedProgramsPerCyleMax = 8_000_000;



		Random rng = new Random();

		CompressionCandidateGen compCandidateGen = new CompressionCandidateGen(fragmentDatabase);

		int cycleCnt = 1;

		for(;;) { // repeat for each cycle

			List<ProgramWithNumberOfBitsTuple> programcandidatesOfCycle = new ArrayList<>();
			
			if( verbosity > 0 )   System.out.format("main : initiate cycle #=%d\n", cycleCnt);

			int currentCycleCnt = cycleCnt;
			cycleCnt++;
			
			{ // generate programs
				// table of instructions used to generate programs
				InstrCode[] instructionCandidates = new InstrCode[5];
				instructionCandidates[0] = instrLookupTable.lookupIdxByExactHumanName("DUP");
				instructionCandidates[1] = instrLookupTable.lookupIdxByExactHumanName("CMPGTZEROPOP");
				instructionCandidates[2] = instrLookupTable.lookupIdxByExactHumanName("XCHG");
				instructionCandidates[3] = instrLookupTable.lookupIdxByExactHumanName("MULVALUE -1");
				instructionCandidates[4] = instrLookupTable.lookupIdxByExactHumanName("ADD_R(2)");



				// number of generated programs per cycle has to be limited because we have to give a chance to the compressor to bias the search
				long numberOfGeneratedProgramsPerCycle; // = 2_000_000; // for testing // 15_000_000; // 1_000_000;
				numberOfGeneratedProgramsPerCycle = (long)(numberOfGeneratedProgramsPerCyleBase * Math.pow(currentCycleCnt, numberOfGeneratedProgramsPerCyleExponent));
				numberOfGeneratedProgramsPerCycle = Math.min(numberOfGeneratedProgramsPerCycle, numberOfGeneratedProgramsPerCyleMax);

				for( long candidateProgramCnt = 0; candidateProgramCnt < numberOfGeneratedProgramsPerCycle; candidateProgramCnt++ ) {
					if( verbosity > 0 ) {
						if( (candidateProgramCnt % 10000 ) == 0 ) {
							System.out.format("main : iterate over enumerated programs #=%d\n", candidateProgramCnt);
						}
					}


					compCandidateGen.resetProgram();

					////////
					// generate random program

					int numberOfFragmentsMin = 2;
					int numberOfFragmentsMax = 8;
					// TODO< increase the length as we continue to move over the searchspace >
					//int numberOfFragmentsOrInstructions = 8; // numberOfFragmentsMin + rng.nextInt(numberOfFragmentsMax - numberOfFragmentsMin /* exclusive->inclusive */+ 1);
					int numberOfFragmentsOrInstructions = 4; // 8!

					double SingleInstructionProbability = 0.85; // probability of appending a single instruction
					int maxFragments = 2; // maximal number of fragments in a single candidate program

					int fragmentMaxLen = 8; // maximal length of a appended fragment


					int fragmentsCnt = 0; // counter for the fragments

					for( int iFragment = 0; iFragment < numberOfFragmentsOrInstructions; iFragment++ ) {
						double selectionRng = rng.nextDouble();

						if( selectionRng < SingleInstructionProbability || fragmentsCnt >= maxFragments ) {
							// TODO< maybe we should probabilisically select the single instructions like done in ALS >

							// for now we select the instructions from a table with the potential instructions

							compCandidateGen.appendInstr(instructionCandidates[rng.nextInt(instructionCandidates.length)], 1.0 / (double)instructionCandidates.length);
						}
						else {
							compCandidateGen.appendRandomSnippetWithMaxLength(fragmentMaxLen);

							fragmentsCnt++;
						}
					}


					// emit program and add program to the programs of the current cycle
					programcandidatesOfCycle.add(new ProgramWithNumberOfBitsTuple(compCandidateGen.prgrm));

				}



			}





			////////////////
 			////////////////
			// compress each program and store the # of bits it needs
			boolean compress = false;

			// TODO OPTIMIZATION< parallelize this >
			int compressedProgramCnt = -1; // to keep track of the progress
			for (ProgramWithNumberOfBitsTuple iProgramWNumberOfBits : programcandidatesOfCycle) {
				if (compress && verbosity > 0) {
					if ((compressedProgramCnt % 500) == 0) {
						System.out.format("main : compress programs #=%d\n", compressedProgramCnt);
					}
				}

				if (compress) {
					hashBasedProgramCompressor.resetSecondaryAndCompressSecondary(iProgramWNumberOfBits.program);

					iProgramWNumberOfBits.numberOfBits = hashBasedProgramCompressor.retNumberOfCompressedBits();

					compressedProgramCnt++;
				}
			}

			ProgramWithNumberOfBitsTuple[] programcandidatesOfCycleAsArr = new ProgramWithNumberOfBitsTuple[programcandidatesOfCycle.size()];
			for (int idx = 0; idx < programcandidatesOfCycle.size(); idx++) {
				programcandidatesOfCycleAsArr[idx] = programcandidatesOfCycle.get(idx);
			}







			////////////////
 			////////////////
 			if( true ) { // sort all programs after the # of required bits
				if( verbosity > 0 )   System.out.format("main : start sort of program candidates\n");

				// sort all programs after the # of required bits
				Arrays.sort(programcandidatesOfCycleAsArr);

				if( verbosity > 0 )   System.out.format("main : finished sort of program candidates\n");
 			}
			



 			////////////////
 			////////////////
			// iterate over all programs in increasing # of bits and test the program for the fitness
			SimCtx simCtx = new SimCtx();
			simCtx.init(instrLookupTable);

			{
				if( verbosity > 0 )   System.out.format("main : start evaluation of program candidates\n");

				int checkedProgramInThisCylceCnt = 0;
				for( ProgramWithNumberOfBitsTuple iProgramWithNumberOfBits : programcandidatesOfCycleAsArr ) {
					if( verbosity > 0 ) {
						if( (checkedProgramInThisCylceCnt % 1000 ) == 0 ) {
							System.out.format("main : checked programs #=%d\n", checkedProgramInThisCylceCnt);
						}
					}

					if( checkProgramSolvesProblem(simCtx, iProgramWithNumberOfBits.program) ) {
						if( verbosity > 0 )   System.out.format("main : found solution, after #tries=%d!\n", numberOfTriedPrograms);

						// dump program
						if( verbosity > 1 ) {

							for( InstrCode iCode : iProgramWithNumberOfBits.program.instrs ) {
								System.out.format("   %s", instrLookupTable.lookupByIdx(iCode.instrIdx).retExactHumanName());
							}

							//interpreter.instrLookup.lookupByIdx(program.instrs.get(0).instrIdx).retExactHumanName()
						}

						return;
					}

					checkedProgramInThisCylceCnt++;
					numberOfTriedPrograms++;
				}

				if( verbosity > 0 )   System.out.format("main : finished evaluation of program candidates\n");
			}
		}

	}

	private static boolean checkProgramSolvesProblem(SimCtx simCtx, Program program) {
		return simCtx.checkProgramSolvesProblem(program);
	}


	static class SimCtx {
		Instrs.Interpreter interpreter;

		public void init(Instrs.InstructionLookupTable instrLookupTable) {
			interpreter = new ls.Instrs.Interpreter();

			interpreter.instrLookup = instrLookupTable;

			interpreter.ctx = new ls.Instrs.Ctx();
			interpreter.ctx.arrayCtx = new DefaultArrCtx();
		}

		public boolean checkProgramSolvesProblem(Program<InstrCode> program) {
			// HACK HACK HACK HACK< for checking if it produces the "right" program >
			// TODO< check for the right program >

			boolean mustPass = false;

			if( program.instrs.size() == 14 &&

				interpreter.instrLookup.lookupByIdx(program.instrs.get(0).instrIdx).retExactHumanName().equals("DUP") &&

				interpreter.instrLookup.lookupByIdx(program.instrs.get(1).instrIdx).retExactHumanName().equals("CMPGTZEROPOP") &&

				// macro
				interpreter.instrLookup.lookupByIdx(program.instrs.get(2).instrIdx).retExactHumanName().equals("PREDIGNOREIFFLAG 2") &&
				interpreter.instrLookup.lookupByIdx(program.instrs.get(3).instrIdx).retExactHumanName().equals("PUSH 0") &&
				interpreter.instrLookup.lookupByIdx(program.instrs.get(4).instrIdx).retExactHumanName().equals("JMP 1") &&
				interpreter.instrLookup.lookupByIdx(program.instrs.get(5).instrIdx).retExactHumanName().equals("PUSH 1") &&


				// FINE UNTIL HERE

				interpreter.instrLookup.lookupByIdx(program.instrs.get(6).instrIdx).retExactHumanName().equals("XCHG") &&
				interpreter.instrLookup.lookupByIdx(program.instrs.get(7).instrIdx).retExactHumanName().equals("MULVALUE -1") &&

				interpreter.instrLookup.lookupByIdx(program.instrs.get(8).instrIdx).retExactHumanName().equals("CMPGTZEROPOP") &&


				// macro
				interpreter.instrLookup.lookupByIdx(program.instrs.get(9).instrIdx).retExactHumanName().equals("PREDIGNOREIFFLAG 2") &&
				interpreter.instrLookup.lookupByIdx(program.instrs.get(10).instrIdx).retExactHumanName().equals("PUSH 0") &&
				interpreter.instrLookup.lookupByIdx(program.instrs.get(11).instrIdx).retExactHumanName().equals("JMP 1") &&
				interpreter.instrLookup.lookupByIdx(program.instrs.get(12).instrIdx).retExactHumanName().equals("PUSH -1") &&

				interpreter.instrLookup.lookupByIdx(program.instrs.get(13).instrIdx).retExactHumanName().equals("ADD_R(2)")
			) {
				System.out.format("NNNNNN");
				mustPass = true;
			}


			EnvImpl envImpl = new EnvImpl();
			envImpl.interpreter = interpreter;

			// translate generated program
			// we just need to set it here
			// TODO< copy if program can modify itself >
			envImpl.currentProgramInstrsIndices = new int[program.instrs.size()];
			for( int i = 0; i < program.instrs.size(); i++ ) {
				envImpl.currentProgramInstrsIndices[i] = program.instrs.get(i).instrIdx;
			}

			boolean passed = iterate(envImpl);

			if( mustPass ) {
				System.out.println(passed);
				System.exit(0);
			}


			return passed;
		}

		// returns whenever the score was high enough to be a winner
		boolean iterate(EnvImpl envImpl) {
			envImpl.remainingSteps = 1000;

			envImpl.initEnv();

			for(;;) {
				envImpl.step();

				if( envImpl.fatalError ) {
					return false;
				}

				if( envImpl.remainingSteps <= 0 ) {
					break;
				}
			}

			if( envImpl.accumulatedReward < 0.0f ) {
				return false;
			}
			// we let it passs if it did hit and miss in a 50:50 ratio

			return true;
		}



		// bundle of environment (Pong) and the interpreter which executes the program and some context helpers
		static class EnvImpl {
			public env.Pong pongEnv;
			public Instrs.Interpreter interpreter;

			boolean fatalError;

			int[] currentProgramInstrsIndices; // current program

			float accumulatedReward = 0.0f;
			int remainingSteps;

			public void initEnv() {
				pongEnv = new Pong();
				pongEnv.ballVelX = 0.7f;
				pongEnv.ballVelY = 0.3f;
				pongEnv.ballX = 3.0f;
				pongEnv.ballX = 4.0f;
			}

			public void step() {
				simulateEnv();
				runProgram();

				reward();

				remainingSteps--;
			}

			void simulateEnv() {
				float timedelta = 0.1f;
				pongEnv.tick(timedelta);
			}

			// accumulates the reward for RL
			void reward() {
				accumulatedReward += ((float)pongEnv.ballAction * 1.0f);
			}

			// setup arguments for program, run the program and check/interpret the result
			void runProgram() {
				interpreter.ctx.reset();

				interpreter.ctx.remainingSteps = 20; // TODO< calculate with ALS formula or a approximation of it >

				this.fatalError = false;

				// push difference between ball and paddle
				float ballPaddleDiff = pongEnv.paddleX - pongEnv.ballX;
				interpreter.ctx.stack.push((double)ballPaddleDiff);

				// interpret
				boolean fatalError = interpreter.interpret(currentProgramInstrsIndices);
				this.fatalError = fatalError;
				if( this.fatalError )   return;

				// work with result
				if( interpreter.ctx.stack.empty() ) {
					this.fatalError = true;
					return;
				}

				pongEnv.control = 0;

				double controlRes = interpreter.ctx.stack.peek();
				if( Math.abs(1.0 - controlRes) < 0.001 ) {
					pongEnv.control = 1;
				}
				else if( Math.abs(-1.0 - controlRes) < 0.001 ) {
					pongEnv.control = -1;
				}
			}
		}
	}
}
