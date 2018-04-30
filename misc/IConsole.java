package misc;

// abstraction of a console output for debugging and human introspection
public interface IConsole {
	IConsolePipe createPipe();

	// abstraction for a pipe in that we can update data
	public interface IConsolePipe {
		void pushPrefix(String str);

		void putInt(String key, int value);
	}
}
