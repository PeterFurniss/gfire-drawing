package uk.co.furniss.draw.gfmap;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import uk.co.furniss.draw.dom.XPathUtil;
import uk.co.furniss.draw.dom.XYcoords;

// find the pattern hexes in the document and remember things about them
class GfPatternHexes {

	private static final XPathUtil XPU = XPathUtil.getSVG();

	private Map<HexColour, PatternHex> patternHexes = new EnumMap<>(HexColour.class);

	private final List<XYcoords> hexCorners;
	private final float hexHeight;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(GfPatternHexes.class.getName());
	
	public float getHexHeight() {
		return hexHeight;
	}

	public float getHexWidth() {
		return hexWidth;
	}

	private final float hexWidth;


	public GfPatternHexes(Element svgDoc) {
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
		LOGGER.debug("corners of pattern hex {}", hexCorners);
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
