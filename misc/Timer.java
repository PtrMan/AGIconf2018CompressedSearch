package misc;

public class Timer {
	long startTime;
	long stopTime;

	public void start() {
		startTime = System.nanoTime();
	}

	public void stop() {
		stopTime = System.nanoTime();
	}

	public long retElapsedSeconds() {
		return (stopTime - startTime) / 1_000_000_000;
	}

	public long retElapsedMicroseconds() {
		return (stopTime - startTime) / 1_000;
	}
}
