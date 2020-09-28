package uk.co.furniss.draw.piecemaker;

import java.util.HashMap;
import java.util.Map;

public enum Justification {
	LEFT("start", "start"),CENTRE("middle", "center"),RIGHT("end","end");
	
	private final String anchor;
	private final String align;
	private static final Map<String, Justification> anchorMap;
	private static final Map<String, Justification> alignmentMap;
	
	static {
		anchorMap = new HashMap<>();
		alignmentMap = new HashMap<>();
		for (Justification one : Justification.values()) {
			anchorMap.put(one.anchor, one);
			alignmentMap.put(one.align, one);
		}
	}
	private Justification(String anchor, String align) {
		this.anchor = anchor;
		this.align = align;
	}

	public String getAnchor() {
		return anchor;
	}

	public String getAlign() {
		return align;
	}

	public static Justification fromAnchor(String anchor) {
		if (anchor == null || ! anchorMap.containsKey(anchor)) {
			return CENTRE;
		}
		return anchorMap.get(anchor);
	}

	public static Justification fromAlignment(String alignment) {
		if (alignment == null || ! alignmentMap.containsKey(alignment)) {
			return CENTRE;
		}
		return alignmentMap.get(alignment);
	}

}
