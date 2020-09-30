package uk.co.furniss.draw.dom;

import java.util.HashMap;
import java.util.Map;

public enum Orientation { BOTTOM(""), RIGHT("rotate(-90)"), TOP("scale(-1)"), LEFT("rotate(90)");
	private final String rotation;
	private static final Map<String, Orientation> map;
	public String getRotation() {
		return rotation;
	}
	static {
		map = new HashMap<>();
		for (Orientation o : Orientation.values()) {
			map.put(o.rotation, o);
		}
	}
	
	private Orientation(String rot) {
		this.rotation = rot;
	}
	
	public static Orientation fromTransform(String transform) {
		Orientation lookedUp = map.get(transform);
		if (lookedUp == null) {
			lookedUp = BOTTOM;
		}
		return lookedUp;
	}
	
}