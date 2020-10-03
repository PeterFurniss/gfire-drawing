package uk.co.furniss.draw.gfmap;

import java.util.EnumMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import uk.co.furniss.draw.dom.SVGbuilder;
import uk.co.furniss.draw.dom.SVGmangler;
import uk.co.furniss.draw.dom.XPathUtil;

public class GfMapBuilder extends SVGbuilder {
	
	public static final XPathUtil XPU = XPathUtil.getSVG();
	private final float xUnit;
	private float yUnit;

	
	private final float margin;
	private final Map<HexColour, Float> xOffsets = new EnumMap<>(HexColour.class);
	private final Map<HexColour, Float> yOffsets = new EnumMap<>(HexColour.class);


	private final float xNumber;
	private final float yNumber;
	private final GfPatternHexes patterns;
	
	
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(GfMapBuilder.class.getName());

	/**
	 * build godsfire 3-d map
	 * @param docElement	parent document (or element ?). Contains the pattern hexes
	 * @param xSpacing		width a hex in whatever units
	 */
	public GfMapBuilder(Element docElement, float xSpacing) {
		super(docElement);
				
        double cos30 = Math.cos(Math.PI/ 6.0);
        
        this.xUnit = xSpacing;
        yUnit = (float) (xUnit * cos30);
        margin = xUnit * 0.5f;
        
        //
		patterns = new GfPatternHexes(docElement);
		
		// define the offsets for hex of colour C, from the row, column bse
		for (HexColour colour : HexColour.values()) {
			xOffsets.put(colour,  colour.xOffset(xUnit));
			yOffsets.put(colour,  colour.yOffset(yUnit));
		}
        
        // location of a number within a hex
        float side = patterns.getHexWidth() / 2.0f;
        xNumber = side * 0.5f;
        yNumber = side * .2f;
	}
	
	public void systemCell(Cell cell, String name) {
		String fillColour = "white";
		String strokeColour = "black";
		String radius = "7";
		addComment("System cell, name and immediate neighbours for " + name + ".");
		cellCircle(cell.getRow(), cell.getCol(), cell.getLevel(), fillColour, strokeColour, radius);
		
		labelCell(cell, name);
		
	}

	public void adjacentCell(Cell cell) {
		String fillColour = "white";
		String strokeColour = "none";
		String radius = "3";
		cellCircle(cell.getRow(), cell.getCol(), cell.getLevel(), fillColour, strokeColour, radius);
	}
	
	public void nextButOneCell(Cell cell) {
		String fillColour = "white";
		String strokeColour = "none";
		String radius = "1.5";
		cellCircle(cell.getRow(), cell.getCol(), cell.getLevel(), fillColour, strokeColour, radius);
	}
	
	/**
	 * put a circle in the middle of the defined cell
	 * @param row
	 * @param col
	 * @param level
	 * @param fillColour
	 * @param strokeColour
	 * @param radius
	 */
	private void cellCircle(int row, int col, int level, String fillColour, String strokeColour, String radius) {
		HexColour colour = Levels.cellLevel(level);
		float cx = hexCentreX(row, col, colour) + patterns.getHex(colour).getCellXoffset(level);
		float cy = hexCentreY(row, col, colour) - patterns.getHex(colour).getCellYoffset(level);

		Element circle = addElement("circle");
		
		circle.setAttribute("fill", fillColour);
		circle.setAttribute("stroke",  strokeColour);
		if (!strokeColour.equals("none")) {
			circle.setAttribute("stroke-width", "0.61");
		}
		circle.setAttribute("r", radius);
		circle.setAttribute("cx", Float.toString(cx));
		circle.setAttribute("cy", Float.toString(cy));
	}
	
	private void labelCell(Cell cell, String name) {
		HexColour colour = cell.getCellColour();
		int row = cell.getRow();
		int col = cell.getCol();
		int level = cell.getLevel();
		float x = hexCentreX(row, col, colour) + patterns.getHex(colour).getCellXoffset(level);
		float y = hexCentreY(row, col, colour) - patterns.getHex(colour).getCellYoffset(level) + 8 ;
		
		Element box = addElement("rect");
		Element label = addElement("text");
//		box.setAttribute("style", "fill:rgb(238,170,255);stroke:none");
		box.setAttribute("style", "fill:fuchsia;stroke:none");
		float width = name.length() * 8;
		box.setAttribute("width", Float.toString(width));
		box.setAttribute("height", "12");
		box.setAttribute("x", Float.toString(x));
		box.setAttribute("y", Float.toString(y));
		label.setAttribute("x", Float.toString(x + 4));
		// text counts from the bottom ?
		label.setAttribute("y", Float.toString(y + 10));
		label.setAttribute("style", "fill:black;stroke:none;font-size:10.5;font-family:sans-serif");
		label.setTextContent(name);
	}
	
	public float hexCentreX (int row, int col, HexColour colour) {
		return xCoord(row-1, col-1, colour) + patterns.getHexWidth()/2.0f;
	}
	
	public float hexCentreY (int row, int col, HexColour colour) {
		return yCoord(row-1, col-1, colour) + patterns.getHexHeight()/2.0f;
	}
	

	;
	/**
	 * x coordinate of hex top-right origin (right ?) left, surely
	 */
	public float xCoord(int row, int col, HexColour colour) {
		boolean oddRow = row % 2 == 1;
		float xOffset = (oddRow ? xUnit *  1.5f  : 0.0f) + xOffsets.get(colour);
		int effectiveCol = col - (oddRow && colour == HexColour.GREEN ? 1 : 0);
		return margin + xUnit*effectiveCol*3.0f  + xOffset;
	}
	
	/**
	 * y coordinate of hex top-right origin
	 */
	public float yCoord(int row, int col, HexColour colour) {
		return margin + yUnit*row*3.0f  + yOffsets.get(colour);
	}

	// for a given x coordinate, which hex column is it in
	//  is this to be sure on the left or the right
	//    
	public int getColOfX(float xPosition, boolean left, int maxCols) {
		int c = (int) ((xPosition - margin) / (3.0f * xUnit));
		
		// if the nw and sw corners are to the right of xPosition, we need to go the column on the left
		if (left && xCoord(0, c, HexColour.RED) + xUnit * 0.5f >  xPosition ) {
			c--;
		}
		// is the right most part of the green 
		if (! left && (xCoord(0, c, HexColour.GREEN)+ 1.5f * xUnit) < xPosition) {
			c = c + 1;
		}
		if (c < 0) {
			c = 0;
		}
		if (c >= maxCols) {
			c = maxCols - 1;
		}
		return c;
		
	}
	
	public int getRowOfY(float yPosition, boolean top, int maxRows) {
		int r = (int) (( yPosition - margin) / (3.0f * yUnit));
		if ( top &&  ( yCoord(r, 0, HexColour.GREEN) > yPosition)) {
			r = r - 1;
		}

		if ( !top && ( yCoord(r, 0, HexColour.GREEN) + 2 * yUnit ) < yPosition) {
			r = r + 1;
		}
		if (r < 0) {
			r = 0;
		}
		if (r >= maxRows) {
			r = maxRows - 1;
		}
		return r;
	}

	
	public void addNumber(int row, int col, HexColour colour) {
		Element number = addElement("text");
		number.setAttribute("style", "font-family:Arial;font-size:8");
		number.setAttribute("x", Float.toString(xCoord(row, col, colour)+xNumber));
		number.setAttribute("y", Float.toString(yCoord(row, col, colour)+yNumber));
		number.setTextContent(String.format("%02d %02d",  row+1, col+1));
	}

	/**
	 * Add the lines between the hexes of the same colour
	 * @param colour   which colour we are working with
	 * @param rowA     row of first hex
	 * @param colA     col of first hex
	 * @param rowB		row of second hex
	 * @param colB		col of second hex
	 * @param direction	which direction this is (could be determined from the above, perhaps)
	 * 		0 = southeast, 1 = sw, 2 = w, 3 = nw,  4 = ne, 5 = e
	 * 
	 */
	public void addLink(HexColour colour, int rowA, int colA, int rowB, int colB, int direction) {
		
		float x1 = xCoord(rowA, colA, colour) + patterns.getCornerX(direction);
		float y1 = yCoord(rowA, colA, colour) + patterns.getCornerY(direction);
		int opposite = (direction + 3) % 6;
		float x2 = xCoord(rowB, colB, colour) + patterns.getCornerX(opposite);
		float y2 = yCoord(rowB, colB, colour) + patterns.getCornerY(opposite);
		
		Element line = addElement("line");
		line.setAttribute("x1", coordinate(x1));
		line.setAttribute("y1", coordinate(y1));
		line.setAttribute("x2", coordinate(x2));
		line.setAttribute("y2", coordinate(y2));
		line.setAttribute("stroke", colour.getRGB());
		line.setAttribute("stroke-width","1");

	}
	
	public void roundNumbers() {
		SVGmangler.roundNumbers(documentElement);
	}

	
	
	/**
	 * add hex at the requested position and colour, referencing the appropiate pattern
	 * @param colour  r, g, b
	 * @param row     logical row
	 * @param col     logical column
	 * @return        the hex element created
	 */
	public Element addHex(HexColour colour, int row, int col) {
		// create a reference (clone)
		Element use = addElement("use");
		use.setAttribute("height", "100%");
		use.setAttribute("width", "100%");
		float dx = xCoord(row, col, colour);
		float dy = yCoord(row, col, colour);
		use.setAttribute("x", Float.toString(dx));
		use.setAttribute("y", Float.toString(dy));
		// reference the pattern
		use.setAttributeNS(XPathUtil.XLINK_NS, "xlink:href", "#" + patterns.getHex(colour).getId());
//		appendingElement.appendChild(use);
		// add the logical r, c number
		addNumber(row,  col,  colour);
		return use;
	}


	
	
}
