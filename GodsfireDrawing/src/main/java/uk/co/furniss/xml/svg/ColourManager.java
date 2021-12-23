package uk.co.furniss.xml.svg;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ColourManager {

	private static final Map<String, String> KNOWN_COLOURS = new HashMap<>();
	private static final Map<String, String> ROTATE_ONCE = new HashMap<>();
	private static final Map<String, String> ROTATE_TWICE = new HashMap<>();
	static {
		KNOWN_COLOURS.put("rgb(0,0,0)", "black");
		KNOWN_COLOURS.put("rgb(255,0,0)", "red");
		KNOWN_COLOURS.put("rgb(0,255,0)", "lime");
		KNOWN_COLOURS.put("rgb(0,128,0)", "green");
		KNOWN_COLOURS.put("rgb(0,0,255)", "blue");
		KNOWN_COLOURS.put("rgb(255,255,255)", "white");
		
		ROTATE_ONCE.put("red", "lime");
		ROTATE_ONCE.put("lime", "red");
		ROTATE_TWICE.put("red", "blue");
		ROTATE_TWICE.put("blue", "lime");
		ROTATE_TWICE.put("lime", "red");
	}
	private static final Pattern HEX_PATTERN = Pattern.compile("#([\\da-f]{2})([\\da-f]{2})([\\da-f]{2})");
	private ColourManager() {
		
	}

	public static  String convert(String old) {
		Matcher hexM = HEX_PATTERN.matcher(old);
		String answer;
		if (hexM.matches()) {
			answer = "rgb(" + unhex(hexM.group(1)) + "," + unhex(hexM.group(2)) + "," + unhex(hexM.group(3)) + ")";
		} else {
			answer = old;
		}
		if (KNOWN_COLOURS.containsKey(answer)) {
			answer = KNOWN_COLOURS.get(answer);
		}
		return answer;
	}
	
	private static String unhex(String hex) {
		return Integer.toString(Byte.toUnsignedInt((byte) Integer.parseInt(hex, 16)));
	}

	private static final Pattern RGB_PATTERN = Pattern.compile("rgb\\((\\d+),(\\d+),(\\d+)\\)");
	public static String rotate(String oldColour, int rotations) {
		Matcher m = RGB_PATTERN.matcher(oldColour);
		if (m.matches() ) {
			String r = m.group(1);
			String g = m.group(2);
			String b = m.group(3);
			String other;
			if (rotations == 1) {
				other = b;
				b = g;
				g = r;
				r = other;
			} else {
				other = b;
				b = r;
				r = g;
				g = b;
			}
			return "rgb(" + r + ", " + g + "," + b + ")";
		} else {
			if (rotations == 1) {
				if (ROTATE_ONCE.containsKey(oldColour)) {
					return ROTATE_ONCE.get(oldColour);
				} else {
					return oldColour;
				}
			} else if ( ROTATE_TWICE.containsKey(oldColour)) {
					return ROTATE_TWICE.get(oldColour);
				} else {
				return oldColour;
			}
		}
	}
}
