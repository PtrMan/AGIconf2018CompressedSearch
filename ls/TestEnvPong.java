package ls;

import env.Pong;
import ls.Instrs;
import ls.LevinSearch;
import misc.Timer;

class TestEnvPong {
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

			this.fatalError = false;

			// push difference between ball and paddle
			float ballPaddleDiff = pongEnv.paddleX - pongEnv.ballX;
			interpreter.ctx.stack.push(ballPaddleDiff);

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

			float controlRes = interpreter.ctx.stack.peek();
			if( Math.abs(1.0f - controlRes) < 0.001f ) {
				pongEnv.control = 1;
			}
			else if( Math.abs(-1.0f - controlRes) < 0.001f ) {
				pongEnv.control = -1;
			}
		}
	}

	public static void main(String[] args) {
		TestEnvPong testEnvPong = new TestEnvPong();
		testEnvPong.init();
		testEnvPong.run();
	}

	EnvImpl envImpl;
	Instrs.Interpreter interpreter;

	public void init() {
		interpreter = new ls.Instrs.Interpreter();

		interpreter.instrLookup = new ls.Instrs.InstructionLookupTable();
		interpreter.instrLookup.table = new ls.Instrs.IInstr[7];
		interpreter.instrLookup.table[0] = new ls.Instrs.DUP();
		interpreter.instrLookup.table[1] = new ls.Instrs.CMPGTZEROPOP();
		interpreter.instrLookup.table[2] = new ls.Instrs.XCHG();
		interpreter.instrLookup.table[3] = new ls.Instrs.MULVALUE(-1.0f);
		interpreter.instrLookup.table[4] = new ls.Instrs.PUSHIFFLAG(0.0f, 1.0f);
		interpreter.instrLookup.table[5] = new ls.Instrs.PUSHIFFLAG(0.0f, -1.0f);
		interpreter.instrLookup.table[6] = new ls.Instrs.ADD2_R();

		interpreter.ctx = new ls.Instrs.Ctx();
	}

	// returns whenever the score was high enough to be a winner
	boolean iterate() {
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

	public void run() {
		int maxProgramLength = 10;

		ls.LevinSearch ls = new ls.LevinSearch();
		ls.nInstrs = 7;
		ls.resize(2);

		Timer timer = new Timer();

		timer.start();

		for(;;) {
			envImpl = new EnvImpl(); // we just reset it this way
			envImpl.interpreter = interpreter;

			// translate generated program
			// we just need to set it here
			// TODO< copy if program can modify itself >
			envImpl.currentProgramInstrsIndices = ls.arr;

			if( /* debugProgram */ false ) {
				for( int idx = 0; idx < ls.arr.length; idx++ ) {
					System.out.format("%d ", ls.arr[idx]);
				}

				System.out.format("\n");

			}


			boolean passed = iterate();
			if( passed ) {
				timer.stop();

				System.out.format("search was _successful_!\n");
				System.out.format("took %d seconds", timer.retElapsedSeconds());


				return;
			}

			// next program
			boolean programOverflow = ls.next();
			if( programOverflow ) {
				if( ls.arr.length > maxProgramLength ) {
					timer.stop();

					System.out.format("search was not successful!\n");

					return;
				}

				ls.resize(ls.arr.length+1);

				System.out.format("search programlength=%d\n", ls.arr.length);
			}
		}

	}
}