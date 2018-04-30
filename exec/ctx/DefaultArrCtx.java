package exec.ctx;

import java.util.List;
import java.util.ArrayList;

import ls.Instrs;

// simple default implementation of a array context which has a own array
public class DefaultArrCtx implements Instrs.IArrayCtx {
	public DefaultArrCtx() {
	}

	public void append(double val, int arrayIdx) {
		isOkFlag = arrayIdx == 0;
		if( !isOkFlag )   return;
		arr.add(val);
	}

	public void setArrIdx(int idx, int arrayIdx) {
		isOkFlag = arrayIdx == 0;
		if( !isOkFlag )   return;
		arrIdx = idx;
	}

	public double retAt(int idx, int arrayIdx) {
		isOkFlag = arrayIdx == 0;
		if( !isOkFlag )   return 0;

		isOkFlag = idx >= 0 && idx < arr.size();
		if( !isOkFlag )   return 0;

		return arr.get(idx);
	}

	public int retArrLength(int arrayIdx) {
		isOkFlag = arrayIdx == 0;
		if( !isOkFlag )   return -1;

		return arr.size();
	}

	public int retArrIdx(int arrayIdx) {
		isOkFlag = arrayIdx == 0;
		if( !isOkFlag )   return -1;

		isOkFlag = true;
		return arrIdx;
	}

	public void delAtIdx(int idx, int arrayIdx) {
		isOkFlag = arrayIdx == 0;
		if( !isOkFlag )   return;

		isOkFlag = idx >= 0 && idx < arr.size();
		if( !isOkFlag )   return;

		arr.remove(idx);
	}

	public boolean retCheckIdx(int arrayIdx) {
		isOkFlag = arrayIdx == 0;
		if( !isOkFlag )   return false;

		return arrIdx >= 0 && arrIdx < arr.size();
	}

	public boolean isOk() {
		return isOkFlag;
	}

	public void reset() {
		arrIdx = 0;
		arr.clear();
		isOkFlag = true;
	}

	public int arrIdx = 0;
	public List<Double> arr = new ArrayList<>();

	boolean isOkFlag = true;
}