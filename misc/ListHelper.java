package misc;

import java.util.List;

public class ListHelper<Type> {
	public void addRange(List<Type> dest, List<Type> src) {
		for( Type iVal : src )   dest.add(iVal);
	}
}
