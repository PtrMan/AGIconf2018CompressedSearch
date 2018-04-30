package compress;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import instr.InstrCode;
import instr.Program;

import misc.Timer;
import misc.Math;

public class ProgramCompressor {
	// fast hashing based compressor

	// * stores instructions as words, words can encode the instruction and it's parameters and the segmentation has to be transparent to the compressor
	// * programs can be sliced
	// * slices can be partially stored (mask gets assigned a special value - 0)

	// TODO< masking of instructions - this can be used to get a higher compression ratio >
	public static class HashBasedFastProgramCompressor implements IProgramCompressor {
		public void config(
			int numberOfInstructions,
			int maximalPrimaryLength,
			int maximalSecondaryLength,
			int maximalSliceLength
		) {
			compressorOutputStream.config(numberOfInstructions, maximalPrimaryLength, maximalSecondaryLength, maximalSliceLength);
		}

		public void primary(List<Program<InstrCode>> programs) {
			timer.start();

			for( Program<InstrCode> iProgram : programs ) {
				int[] programWordsArg = new int[iProgram.instrs.size()];
				for( int idx = 0; idx < iProgram.instrs.size(); idx++ ) {
					programWordsArg[idx] = iProgram.instrs.get(idx).instrIdx;
				}

				// copy words because we can safely slice it in the hashed form
				int[] programWords = new int[programWordsArg.length];
				System.arraycopy(programWordsArg, 0, programWords, 0, programWordsArg.length);


				for( int sliceStartIdx = 0; sliceStartIdx < programWords.length; sliceStartIdx++ ) {
					for( int sliceLength = programWords.length - sliceStartIdx; sliceLength >= 2; sliceLength-- ) {

						int hash = SliceHasher.hashOfSlice(programWords, sliceStartIdx, sliceLength);
						primaryHashedCounter.incForHash(hash);

						// store subsequence
						primaryHashedSubsequences.append(hash, programWords, sliceStartIdx, sliceLength);
					}
				}
			}

			timer.stop();

			if( verbosity > 0 ) {
				System.out.format("time it took to index-compress t=%dus\n", timer.retElapsedMicroseconds());
			}

		}

		public void resetSecondaryAndCompressSecondary(Program<InstrCode> program) {
			compressorOutputStream.setCountOfPrimarySlices(primaryHashedSubsequences.retCountOfHashUniqueSlices());

			HashedCounter secondaryHashedCounter = new HashedCounter();
			HashedSubsequences secondaryHashedSubsequences = new HashedSubsequences();



			timer.start();

			secondaryHashedCounter.flush();
			secondaryHashedSubsequences.flush();

			compressorOutputStream.resetNewRound();

			


			// compression works by searching first for long subsequences to short sequences
			// we try to lookup if the subsequence exists in the primary if the hash of the sequence existed

			int[] programWords = new int[program.instrs.size()];
			for( int idx = 0; idx < program.instrs.size(); idx++ ) {
				programWords[idx] = program.instrs.get(idx).instrIdx;
			}

			
			for( int sliceStartIdx = 0; sliceStartIdx < programWords.length; sliceStartIdx++ ) {
				boolean longestSubsequentWasFound = false;

				// kepp it to store all subsequences until the old index if they don't exist already
				int sliceStartIdx2 = sliceStartIdx;

				// search for biggest possible subsequences of the input sequence which is known by the compressor
				// and store the commands. The commands can be used to calulate the size of the compressed output stream.
				for( int sliceLength = programWords.length - sliceStartIdx; sliceLength >= 2; sliceLength-- ) {

					int hash = SliceHasher.hashOfSlice(programWords, sliceStartIdx, sliceLength);

					boolean hashExistsInSecondary = secondaryHashedCounter.existsHash(hash);
					if( hashExistsInSecondary ) {
						// lookup in hashed database if subsequence exists
						boolean subsequenceExists = secondaryHashedSubsequences.existsSubsequenceEqual(hash, programWords, sliceStartIdx, sliceLength);
						if( subsequenceExists ) {
							// write command to insert the subsequence into the result stream
							compressorOutputStream.appendReferenceSecondary(hash, programWords, sliceStartIdx, sliceLength);

							// advance sliceStartIdx
							sliceStartIdx += sliceLength;
							sliceStartIdx--; // delta because the for loop increments
							
							longestSubsequentWasFound = true;
							break;
						}
					} 

					boolean hashExistsInPrimary = primaryHashedCounter.existsHash(hash);
					if( !hashExistsInPrimary ) {

						// lookup in hashed database if subsequence exists
						boolean subsequenceExists = primaryHashedSubsequences.existsSubsequenceEqual(hash, programWords, sliceStartIdx, sliceLength);
						if( !subsequenceExists )   continue;

						
						// write command to insert the subsequence into the result stream
						compressorOutputStream.appendReferencePrimary(hash, programWords, sliceStartIdx, sliceLength);

						// advance sliceStartIdx
						sliceStartIdx += sliceLength;
						sliceStartIdx--; // delta because the for loop increments
						
						longestSubsequentWasFound = true;
						break;

					}


					
				}

				// we need to add a command to the result stream to emit the current programWord because it couldn't get compressed
				if( !longestSubsequentWasFound ) {
					compressorOutputStream.appendUncompressed(programWords[sliceStartIdx]);
				}

				// store all old subsequences which we don't yet know
				// we do this because the compressor can reuse this data later
				{
					int sliceEndInFrontIdx = sliceStartIdx;
					for( int sliceStartInFrontIdx = 0; sliceStartInFrontIdx < sliceEndInFrontIdx; sliceStartInFrontIdx++ ) {
						int sliceInFrontLength = sliceEndInFrontIdx - sliceStartInFrontIdx;

						int hash = SliceHasher.hashOfSlice(programWords, sliceStartInFrontIdx, sliceInFrontLength);

						boolean hashExistsInPrimary = primaryHashedCounter.existsHash(hash);
						if( hashExistsInPrimary ) {
							// lookup in hashed database if subsequence exists
							boolean subsequenceExists = primaryHashedSubsequences.existsSubsequenceEqual(hash, programWords, sliceStartInFrontIdx, sliceInFrontLength);
							if( subsequenceExists )   continue;
						}

						boolean hashExistsInSecondary = secondaryHashedCounter.existsHash(hash);
						if( hashExistsInSecondary ) {
							// lookup in hashed database if subsequence exists
							boolean subsequenceExists = secondaryHashedSubsequences.existsSubsequenceEqual(hash, programWords, sliceStartInFrontIdx, sliceInFrontLength);
							if( subsequenceExists )    continue;
						}

						// we are here if it doesn't exist in the primary or secondary
						// so we need to add it
						secondaryHashedCounter.incForHash(hash);
						secondaryHashedSubsequences.append(hash, programWords, sliceStartInFrontIdx, sliceInFrontLength);

						compressorOutputStream.incrementSecondarySliceCount();
					}
				}
			}



			timer.stop();

			if( verbosity > 0 ) {
				System.out.format("time it took to secondary index-compress t=%dus\n", timer.retElapsedMicroseconds());
			}

			numberOfBits = ((BitCounterCompressorOutputStream)compressorOutputStream).retNumberOfBits();
			if( verbosity > 0 ) {
				System.out.format("compressed with #bits=%f\n", numberOfBits);
			}
		}

		// returns the number of the secondary 
		public double retNumberOfCompressedBits() {
			return numberOfBits;
		}

		public int verbosity = 0;

		Timer timer = new Timer();

		HashedCounter primaryHashedCounter = new HashedCounter();
		HashedSubsequences primaryHashedSubsequences = new HashedSubsequences();

		// we just have to count the number of bits
		ICompressorOutputStream compressorOutputStream = new BitCounterCompressorOutputStream();


		// used to store the number of bits to which we compressed the secondary program
		double numberOfBits;
	}

	public interface IProgramCompressor {
		// /param maximalPrimaryLength maximal length of a sequence/program of the primary
		// /param maximalSecondaryLength maximal length of a sequence/program of the secondary
		void config(
			int numberOfInstructions,
			int maximalPrimaryLength,
			int maximalSecondaryLength,
			int maximalSliceLength
		);

		// fills the primary program storage with the programs
		void primary(List<Program<InstrCode>> programs);

		// resets the secondary compressed program and compresses the program
		void resetSecondaryAndCompressSecondary(Program<InstrCode> program);

		// returns the number of the secondary 
		double retNumberOfCompressedBits();
	}












	// hashed database for all subsequences
	static class HashedSubsequences {
		static class ByHash {
			static class Element {
				public int[] subsequence; // can point to the same array for efficiency
				public int sliceStartIdx;
				public int sliceLength;

				public int hash;

				public Element(int hash, int[] subsequence, int sliceStartIdx, int sliceLength) {
					this.hash = hash;
					this.subsequence = subsequence;
					this.sliceStartIdx = sliceStartIdx;
					this.sliceLength = sliceLength;
				}
			}

			ArrayList<Element> list = new ArrayList<>();

			public void append(int hash, int[] subsequence, int sliceStartIdx, int sliceLength) {
				list.add(new Element(hash, subsequence, sliceStartIdx, sliceLength));
			}

			public boolean existsHash(int hash) {
				for( Element iElement : list ) {
					if( iElement.hash == hash )   return true;
				}

				return false;
			}

			public boolean existsSubsequenceEqual(int hash, int[] subsequence, int sliceStartIdx, int sliceLength) {
				for( Element iElement : list ) {
					if( iElement.hash != hash )   continue;
					if( iElement.sliceLength != sliceLength )  continue;
					if( !SubsequenceHelper.checkEqual(iElement.subsequence, iElement.sliceStartIdx, subsequence, sliceStartIdx, sliceLength) )   continue;
					return true;
				}

				return false;
			}

			public void flush() {
				list.clear();
			}
		}

		public HashedSubsequences() {
			elements = new ByHash[50000];
			for( int i = 0; i < elements.length; i++ )  elements[i] = new ByHash();
		}

		public boolean existsHash(int hash) {
			int idx = hash < 0 ? -hash : hash;
			idx = idx % elements.length;

			ByHash byHash = elements[idx];
			return byHash.existsHash(hash);
		}

		public boolean existsSubsequenceEqual(int hash, int[] subsequence, int sliceStartIdx, int sliceLength) {
			int idx = hash < 0 ? -hash : hash;
			idx = idx % elements.length;

			ByHash byHash = elements[idx];
			return byHash.existsSubsequenceEqual(hash, subsequence, sliceStartIdx, sliceLength);
		}

		// doesn't check for duplication
		public void append(int hash, int[] subsequence, int sliceStartIdx, int sliceLength) {
			int idx = hash < 0 ? -hash : hash;
			idx = idx % elements.length;

			ByHash byHash = elements[idx];
			byHash.append(hash, subsequence, sliceStartIdx, sliceLength);
		}

		public long retCountOfHashUniqueSlices() {
			long cnt = 0;
			for( ByHash iElement : elements ) {
				cnt += iElement.list.size();
			}
			return cnt;
		}

		public void flush() {
			for( ByHash iByHash : elements )   iByHash.flush();
		}

		ByHash[] elements;
	}

	// hashed database to keep track of the count of the hashes
	// is used to approximate the real count of the sequences
	static class HashedCounter {
		static class ByHash {
			static class Element {
				public int counter;
				public int hash;

				public Element(int hash, int counter) {
					this.hash = hash;
					this.counter = counter;
				}
			}

			ArrayList<Element> list = new ArrayList<>();

			public void inc(int hash) {
				for( Element iElement : list ) {
					if( iElement.hash == hash ) {
						iElement.counter++;
						return;
					}
				}

				list.add(new Element(hash, 1));
			}

			public boolean existsHash(int hash) {
				for( Element iElement : list ) {
					if( iElement.hash == hash )   return true;
				}

				return false;
			}

			public void flush() {
				list.clear();
			}
		}

		public HashedCounter() {
			elements = new ByHash[50000];
			for( int i = 0; i < elements.length; i++ )  elements[i] = new ByHash();
		}

		public void flush() {
			for( ByHash iByHash : elements )   iByHash.flush();
		}

		public boolean existsHash(int hash) {
			int idx = hash < 0 ? -hash : hash;
			idx = idx % elements.length;

			ByHash byHash = elements[idx];
			return byHash.existsHash(hash);
		}

		public void incForHash(int hash) {
			int idx = hash < 0 ? -hash : hash;
			idx = idx % elements.length;

			ByHash byHash = elements[idx];
			byHash.inc(hash);
		}

		ByHash[] elements;
	}


	static class SubsequenceHelper {
		public static boolean checkEqual(int[] sliceA, int sliceAStartIdx, int[] sliceB, int sliceBStartIdx, int sliceLength) {
			for( int offset = 0; offset < sliceLength; offset++ ) {
				if( sliceA[sliceAStartIdx + offset] != sliceB[sliceBStartIdx + offset] ) {
					return false;
				}
			}

			return true;
		}
	}

	static class SliceHasher {
		static int hashOfSlice(int[] instrs, int sliceStartIdx, int sliceLength) {
			int hashVal = 0xDEADBEEF;
			for( int i = sliceStartIdx; i < sliceStartIdx + sliceLength; i++ ) {
				hashVal += 1337;
				hashVal = (hashVal << 3) | (hashVal >> (32-3));
				hashVal ^= hashInstr(instrs[i]);	
			}
			return hashVal;
		}

		static int hashInstr(int instr) {
			return instr; // TODO< tune >
		}
	}

	// interface which implements the output stream
	// the output stream can either count the # of bits or write it as a binary stream
	public interface ICompressorOutputStream {
		// called when the compressed output has to reference an slice of the primary database
		void appendReferencePrimary(int hash, int[] programWords, int sliceStartIdx, int sliceLength);

		// called when the compressed output has to reference an slice of the secondary database
		void appendReferenceSecondary(int hash, int[] programWords, int sliceStartIdx, int sliceLength);

		// called when a subsequence couldn't be found and the program-word has to be appended as it is
		void appendUncompressed(int word);

		// called when a new secondary slice was added
		void incrementSecondarySliceCount();

		void setCountOfPrimarySlices(long cnt);

		// /param maximalPrimaryLength maximal length of a sequence/program of the primary
		// /param maximalSecondaryLength maximal length of a sequence/program of the secondary
		void config(int numberOfInstructions, int maximalPrimaryLength, int maximalSecondaryLength, int maximalSliceLength);

		// called when a reset for a new round is necessary
		void resetNewRound();
	}

	// implementation to count the # of bits
	// we calculate with bits as a real value because an encoder could encode it this way with combining the integers with multiplication
	public static class BitCounterCompressorOutputStream implements ICompressorOutputStream {
		// called when the compressed output has to reference an slice of the primary database
		public void appendReferencePrimary(int hash, int[] programWords, int sliceStartIdx, int sliceLength) {
			numberOfBits += Math.log2(3); // we need some way to encode the choice to append a reference to the primary

			numberOfBits += Math.log2(primaryCnt); // we need to encode which primary slice we chose
			numberOfBits += Math.log2(maximalPrimaryLength); // we need to encode the startIdx of the slice
			numberOfBits += Math.log2(maximalSliceLength); // we need to encode the length of the slice
		}

		// called when the compressed output has to reference an slice of the secondary database
		public void appendReferenceSecondary(int hash, int[] programWords, int sliceStartIdx, int sliceLength) {
			numberOfBits += Math.log2(3); // we need some way to encode the choice to append a reference to the secondary

			numberOfBits += Math.log2(secondaryCnt); // we need to encode which primary slice we chose
			numberOfBits += Math.log2(maximalSecondaryLength); // we need to encode the startIdx of the slice
			numberOfBits += Math.log2(maximalSliceLength); // we need to encode the length of the slice
		}

		// called when a subsequence couldn't be found and the program-word has to be appended as it is
		public void appendUncompressed(int word) {
			numberOfBits += Math.log2(3); // we need some way to encode the choice to append a word

			numberOfBits += Math.log2(numberOfInstructions);
		}


		public void incrementSecondarySliceCount() {
			secondaryCnt++;
		}

		public void setCountOfPrimarySlices(long cnt) {
			primaryCnt = 0;
		}

		public void config(int numberOfInstructions, int maximalPrimaryLength, int maximalSecondaryLength, int maximalSliceLength) {
			this.numberOfInstructions = numberOfInstructions;
			this.maximalPrimaryLength = maximalPrimaryLength;
			this.maximalSecondaryLength = maximalSecondaryLength;
			this.maximalSliceLength = maximalSliceLength;
		}

		public void resetNewRound() {
			secondaryCnt = 0;
			numberOfBits = 0;
		}



		public double retNumberOfBits() {
			return numberOfBits;
		}

		long primaryCnt = 0;
		long secondaryCnt = 0;
		int 
			numberOfInstructions,
			maximalPrimaryLength,
			maximalSecondaryLength,
			maximalSliceLength;

		double numberOfBits = 0;
	}
}
