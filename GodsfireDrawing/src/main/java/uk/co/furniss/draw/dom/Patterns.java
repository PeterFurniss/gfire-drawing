package uk.co.furniss.draw.dom;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

// find the pattern hexes in the document and remember things about them
public class Patterns {

	private static final XPathUtil XPU = XPathUtil.getSVG();

	private Map<HexColour, PatternHex> patternHexes = new EnumMap<>(HexColour.class);

	private final List<XYcoords> hexCorners;
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
		this.hexCorners = GfMapBuilder.getPathCoords(path);
		System.out.println(hexCorners);
		XYcoords max = XYcoords.maxXY(hexCorners);
		hexHeight = max.getY();
		hexWidth = max.getX();
	}

	public PatternHex getHex(HexColour colour) {
		return patternHexes.get(colour);
	}
	
	public float getCornerX(int direction) {
		return hexCorners.get(direction).getX();
	}
	
	public float getCornerY(int direction) {
		return hexCorners.get(direction).getY();
	}
}
