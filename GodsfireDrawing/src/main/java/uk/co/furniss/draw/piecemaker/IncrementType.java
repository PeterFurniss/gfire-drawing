package uk.co.furniss.draw.piecemaker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

enum IncrementType {
	NONE(""), INTEGER("int", "integer"), PAD3("true","pad3", "000"), ROMAN("roman");
	
	private static final Map<String, IncrementType> keyMap = new HashMap<>();
	private List<String> keyList = new ArrayList<>();
	private boolean increments;
	
	private IncrementType(String... keys) {
		for (String key : keys) {
			keyList.add(key);
		}
		increments = ! keyList.contains("");
	}
	
	public boolean isIncrementing() {
		return increments;
	}
	
	static {
		// can't be bothered to work out the stream
		for (IncrementType inc : values()) {
			for (String key : inc.keyList) { 
				keyMap.put(key, inc);
			}
		}
	}

	public static IncrementType getFromKey(String key) {
		if (key == null) {
			return IncrementType.NONE;
		}
		return keyMap.get(key.toLowerCase());
	}
	
	
}
