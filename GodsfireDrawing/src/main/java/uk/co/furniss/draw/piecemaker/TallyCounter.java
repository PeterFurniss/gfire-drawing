package uk.co.furniss.draw.piecemaker;

import java.util.LinkedHashMap;
import java.util.Map;

class TallyCounter {

	private final Map<String, Integer> counter = new LinkedHashMap<>();
	
	public TallyCounter() {
	}
	

	
	
	
	public void increment(String key, int n) {
		Integer prev = counter.get(key);
		if (prev != null) {
			counter.put(key, prev + n);
		} else {
			counter.put(key, n);
		}
	}

	public Map<String, Integer> getCounts() {
		return counter;
	}
}
