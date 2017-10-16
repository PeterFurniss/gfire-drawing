package uk.co.furniss.draw.dom;

import java.util.EnumMap;
import java.util.Map;

import org.w3c.dom.Element;

public class Patterns {

	private static final XPathUtil XPU = XPathUtil.getSVG();

	private Map<HexColour, PatternHex> patternHexes = new EnumMap<>(HexColour.class);

	private final float[][] hexCorners;
	private final float hexHeight;
	
	public float getHexHeight() {
		return hexHeight;
	}

	public float getHexWidth() {
		return hexWidth;
	}

	private final float hexWidth;

	public Patterns(Element svgDoc) {
		for (HexColour colour : HexColour.values()) {
			String idPattern = colour.name().toLowerCase() + "Pattern";
			Element hexElement = XPU.findElement(svgDoc, "//g[@id='" + idPattern + "']");
			if (hexElement == null) {
				throw new IllegalArgumentException("No " + idPattern + " element");
			}
			PatternHex hex = new PatternHex(colour, hexElement);
			patternHexes.put(colour, hex);
		}
		
		Element hex = patternHexes.get(HexColour.BLUE).getElement();
		Element path = XPU.findElement(hex, "path[1]");
		this.hexCorners = SVGutil.getPathCoords(path);
		float ht = 0.0f;
		float wd = 0.0f;
		for (float[] xy : hexCorners) {
			if (xy[0] > wd) {
				wd = xy[0];
			}
			if (xy[1] > ht) {
				ht = xy[1];
			}
		}
		hexHeight = ht;
		hexWidth = wd;
	}

	public PatternHex getHex(HexColour colour) {
		return patternHexes.get(colour);
	}
	
	public float getCornerX(int direction) {
		return hexCorners[direction][0];
	}
	
	public float getCornerY(int direction) {
		return hexCorners[direction][1];
	}
}
