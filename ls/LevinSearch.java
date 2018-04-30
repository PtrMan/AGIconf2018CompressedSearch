package ls;

public class LevinSearch {
	public int[] arr;
	public int nInstrs; // number of instructions

	public void resize(int size) {
		arr = new int[size];
		for( int idx = 0; idx < size; idx++ )   arr[idx] = nInstrs - 1;
	}

	public boolean next() {
		return next(0);
	}

	// search the next program in reverse to simply search
	// returns if the range overflowed and the search finished for this programlength
	public boolean next(int idx) {
		if( idx >= arr.length )   return true;

		boolean overflowed = arr[idx] == 0;

		arr[idx] = (overflowed ? nInstrs - 1 : arr[idx]-1); // next value for this idx
		return overflowed ? next(idx+1) : false; // propagate in case of overflow
	}
}
