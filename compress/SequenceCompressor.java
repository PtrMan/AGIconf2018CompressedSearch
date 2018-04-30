package compress;

import java.util.List;
import java.util.ArrayList;


// sequence compressor inspired by one of Schmidhbers papers from the 90s
// TODO< search paper >
// principle is to search for repeated elements
public class SequenceCompressor {
	public static class Todo extends Exception {

	}

	public static class Element {
		public boolean isLeaf;

		public long leafValue;
		public List<Element> nodeValues = new ArrayList<>();

		public boolean isEqual(Element other) throws Todo {
			// is just implemented for leafes
			if( isLeaf != other.isLeaf ) {
				throw new Todo();
			}

			// compare leaf values
			return leafValue == other.leafValue;
		}

		public static Element makeLeaf(long leafValue) {
			Element res = new Element();
			res.isLeaf = true;
			res.leafValue = leafValue;
			return res;
		}
	}

	// used to compress a sequence of elements by a (common) prefix
	public static class Prefix {
		public int hitCount = 0;
		public Element prefix = null; // null only valid for root prefix
		public List<Prefix> childrenPrefixes = new ArrayList<>();
		public Prefix parent = null;

		public void resetHitcountRec() {
			hitCount = 0;

			for( Prefix iChild : childrenPrefixes )   iChild.resetHitcountRec();
		}

		public void updateParentsRec(Prefix parent) {
			this.parent = parent;

			for( Prefix iChild : childrenPrefixes )   iChild.updateParentsRec(this);
		}

		public Prefix retChildrenByPrefix(Element p) throws Todo {
			for( Prefix iChild : childrenPrefixes ) {
				if( iChild.prefix.isEqual(p) )   return iChild;
			}
			return null;
		}
	}

	public static class PrefixDebug {
		public static void debug(Prefix p) {
			debug(p, 0);
		}

		public static void debug(Prefix p, int depth) {
			boolean isRoot = p.prefix == null;

			if( !isRoot ) {
				for( int i = 0; i < depth; i++)   System.out.format("   ");
				System.out.format("%d ", p.prefix.leafValue);				
				
				if( true ) {
					System.out.format(" cnt=%d", p.hitCount);
				}

				System.out.format("\n");
			}



			for( Prefix iChild : p.childrenPrefixes ) {
				debug(iChild, depth+1);
			}
		}
	}

	public Prefix rootPrefix = new Prefix();

	public List<Prefix> compress(List<Element> elements) throws Todo {
		insertToAllPrefixes(elements);

		rootPrefix.resetHitcountRec();

		incToAllPrefixes(elements);

		rootPrefix.updateParentsRec(null);

		if( false )   PrefixDebug.debug(rootPrefix);

		List<Prefix> res = new ArrayList<>();

		// search longest prefix which has the highest hitcount
		
		int idx = 0;
		while( idx < elements.size() - 1 ) {
			Prefix maxHitcountPrefix = null;
			int maxHitcountLength = 0;
			int maxHitcount = 0;

			for( int iLength = 2; iLength <= Math.min(maxLength, elements.size() - idx); iLength++ ) {
				Prefix p = traverse(rootPrefix, elements, idx, iLength);

				if( p.hitCount >= maxHitcount && iLength >= maxHitcountLength ) {
					maxHitcount = p.hitCount;
					maxHitcountLength = iLength;
					maxHitcountPrefix = p;
					continue;
				}
			}

			// System.out.format("append %b idx=%d\n", maxHitcountPrefix != null, idx);

			// encode maxHitcountPrefix in result
			res.add(maxHitcountPrefix);


			idx += maxHitcountLength;
		}

		return res;
	}

	Prefix traverse(Prefix rootPrefix, List<Element> elements, int startIdx, int length) throws Todo {
		Prefix p = rootPrefix;
		//Prefix pp = null; // previous p

		if( false ) {
			System.out.format("traverse\n");

			System.out.format("   elements\n");

			for( int idx = startIdx; idx < startIdx + length; idx++ ) {
				Element iElement = elements.get(idx);
				System.out.format("%d ", iElement.leafValue);
			}

			System.out.format("\n");
		}




		for( int idx = startIdx; idx < startIdx + length; idx++ ) {
			Element symbol = elements.get(idx);

			if( false ) {
				System.out.format("   Symbol=%d p!=null=%b\n", symbol.leafValue, p != null);
			}

			if( p == null )   return null;

			//pp = p;
			p = p.retChildrenByPrefix(symbol);
		}

		return p;
	}

	// inserts te elements into all prefixes for all possible lengths
	void insertToAllPrefixes(List<Element> elements) throws Todo {
		for( int startIdx = 0; startIdx	< elements.size(); startIdx++ ) {
			int length = Math.min(elements.size() - startIdx, maxLength);
			insertToPrefixByLength(elements, startIdx, length);
		}
	}

	void insertToPrefixByLength(List<Element> elements, int startIdx, int length) throws Todo {
		// iterate over length and add to prefix elements
		Prefix p = rootPrefix;

		for( int idx = startIdx; idx < startIdx + length; idx++ ) {
			Element elementAtIdx = elements.get(idx);

			Prefix a = retPrefixByElementOrGenerateAndAddIfNotExist(p, elementAtIdx);
			p = a;
		}
	}

	void incToAllPrefixes(List<Element> elements) throws Todo {
		for( int startIdx = 0; startIdx	< elements.size(); startIdx++ ) {
			for( int iLength = 1; startIdx + iLength <= elements.size() && iLength < maxLength; iLength++ ) {
				incToPrefixByLength(elements, startIdx, iLength);
			}
		}
	}

	void incToPrefixByLength(List<Element> elements, int startIdx, int length) throws Todo {
		// iterate over length and add to prefix elements
		Prefix p = rootPrefix;

		for( int idx = startIdx; idx < startIdx + length; idx++ ) {
			Element elementAtIdx = elements.get(idx);

			Prefix a = retPrefixByElement(p, elementAtIdx);
			p = a;
		}

		p.hitCount++;
	}

	Prefix retPrefixByElementOrGenerateAndAddIfNotExist(Prefix root, Element prefixElement) throws Todo {
		return retPrefixByElementOrGenerateAndAddIfNotExistFlag(root, prefixElement, true);
	}

	Prefix retPrefixByElement(Prefix root, Element prefixElement) throws Todo {
		return retPrefixByElementOrGenerateAndAddIfNotExistFlag(root, prefixElement, false);
	}

	Prefix retPrefixByElementOrGenerateAndAddIfNotExistFlag(Prefix root, Element prefixElement, boolean generateIfNotExist) throws Todo {
		for( Prefix iRootPrefix : root.childrenPrefixes ) {
			if( iRootPrefix.prefix.isEqual(prefixElement) ) {
				return iRootPrefix;
			}
		}

		if( !generateIfNotExist )   return null;

		Prefix generatedPrefix = new Prefix();
		generatedPrefix.prefix = prefixElement;
		root.childrenPrefixes.add(generatedPrefix);

		return generatedPrefix;
	}

	//List<Prefix> retRootPrefixes() {
	//	return rootPrefix.childrenPrefixes;
	//}

	int maxLength = 20;
}
