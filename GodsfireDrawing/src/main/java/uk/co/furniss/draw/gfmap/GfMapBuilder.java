package uk.co.furniss.draw.gfmap;

import java.util.EnumMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import uk.co.furniss.draw.dom.SVGbuilder;
import uk.co.furniss.draw.dom.SVGmangler;
import uk.co.furniss.draw.dom.XPathUtil;

class GfMapBuilder extends SVGbuilder {
	
	private final float hexSide;
	private final float hexHalfHeight;
	// xUnit are the actual hexSide plus 	the interhex gap
	// yUnit is the equivalent vertically 
	private final float xUnit;
	private float yUnit;

	
	private final float margin;
	private final Map<HexColour, Float> xOffsets = new EnumMap<>(HexColour.class);
	private final Map<HexColour, Float> yOffsets = new EnumMap<>(HexColour.class);


	private final float xNumber;
	private final float yNumber;
	private final GfPatternHexes patterns;
	private final float scaling;
//	private final String scalingPerCent;
	
	
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(GfMapBuilder.class.getName());

	private static final double COS30 = Math.cos(Math.PI/ 6.0);

	/**
	 * build godsfire 3-d map
	 * @param docElement	parent document (or element ?). Contains the pattern hexes
	 * @param hexSide		width a hex in whatever units
	 * @param interHexGap TODO
	 */

	public GfMapBuilder(Element docElement, float hexSide, float interHexGap) {
		super(docElement);
				
        this.hexSide = hexSide;
        this.hexHalfHeight = (float) (hexSide * COS30);
        
        this.xUnit = hexSide + interHexGap;
        yUnit = (float) (xUnit * COS30);
        
        margin = 20.0f;
        
		patterns = new GfPatternHexes(docElement);
		float patternSide = patterns.getHexWidth() / 2.0f;
		scaling = hexSide / patternSide;
//		scalingPerCent = Float.toString(scaling * 100) + "%";
        
        	
		// define the offsets for hex of colour C, from the row, column bse
		for (HexColour colour : HexColour.values()) {
			xOffsets.put(colour,  colour.xOffset(xUnit));
			yOffsets.put(colour,  colour.yOffset(yUnit));
		}
        
        // location of a number within a hex
        LOGGER.debug("pattern side is {}, map hexSide is {}", patternSide, hexSide);
        xNumber = hexSide * 0.5f;
        yNumber = hexSide * .2f;
	}
	
	private static final float SYSTEM_CELL_CIRCLE = 7;
	private static final float ADJACENT_CELL_CIRCLE = 3;
	private static final float TWO_STEP_CELL_CIRCLE = 1.5f;
	

	public void systemCell(Cell cell, String name) {
		String fillColour = "white";
		String strokeColour = "black";
		addComment("System cell, name and immediate neighbours for " + name + ".");
		cellCircle(cell.getRow(), cell.getCol(), cell.getLevel(), fillColour, strokeColour, SYSTEM_CELL_CIRCLE);
		
		labelCell(cell, name);
		
	}


	public void adjacentCell(Cell cell) {
		String fillColour = "white";
		String strokeColour = "none";
		cellCircle(cell.getRow(), cell.getCol(), cell.getLevel(), fillColour, strokeColour, ADJACENT_CELL_CIRCLE);
	}
	

	public void nextButOneCell(Cell cell) {
		String fillColour = "white";
		String strokeColour = "none";
		cellCircle(cell.getRow(), cell.getCol(), cell.getLevel(), fillColour, strokeColour, TWO_STEP_CELL_CIRCLE);
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
	private void cellCircle(int row, int col, int level, String fillColour, String strokeColour, float radius) {
		HexColour colour = Levels.cellLevel(level);
		float cx = hexCentreX(row, col, colour) + patterns.getHex(colour).getCellXoffset(level) * scaling;
		float cy = hexCentreY(row, col, colour) - patterns.getHex(colour).getCellYoffset(level) * scaling;

		Element circle = addElement("circle");
		
		circle.setAttribute("fill", fillColour);
		circle.setAttribute("stroke",  strokeColour);
		if (!strokeColour.equals("none")) {
			circle.setAttribute("stroke-width", "0.61");
		}
		circle.setAttribute("r", Float.toString(radius * scaling));
		circle.setAttribute("cx", Float.toString(cx));
		circle.setAttribute("cy", Float.toString(cy));
	}
	
	// debugging
	@SuppressWarnings("unused")
	private void markPoint(float x, float y) {
		Element circle = addElement("circle");
		circle.setAttribute("fill", "black");
		circle.setAttribute("stroke",  "none");
		circle.setAttribute("r", "0.5");
		circle.setAttribute("cx", Float.toString(x));
		circle.setAttribute("cy", Float.toString(y));
		
		
	}
	
	private void labelCell(Cell cell, String name) {
		HexColour colour = cell.getCellColour();
		int row = cell.getRow();
		int col = cell.getCol();
		int level = cell.getLevel();
		float x = hexCentreX(row, col, colour) + patterns.getHex(colour).getCellXoffset(level) * scaling;
		float y = hexCentreY(row, col, colour) - patterns.getHex(colour).getCellYoffset(level) * scaling + 8 * scaling;
		
		Element box = addElement("rect");
		Element label = addElement("text");
		box.setAttribute("style", "fill:fuchsia;stroke:none");
		float width = name.length() * 8 * scaling;
		box.setAttribute("width", Float.toString(width));
		box.setAttribute("height", Float.toString(12 * scaling));
		box.setAttribute("x", Float.toString(x));
		box.setAttribute("y", Float.toString(y));
		label.setAttribute("x", Float.toString(x + 4 * scaling));
		// text counts from the bottom ?
		label.setAttribute("y", Float.toString(y + 10 * scaling));
		label.setAttribute("style", "fill:black;stroke:none;font-size:" + Float.toString(10.5f * scaling) + ";font-family:sans-serif");
		label.setTextContent(name);
	}
	

	private float hexCentreX (int row, int col, HexColour colour) {
		return xCoord(row-1, col-1, colour) + hexSide; //patterns.getHexWidth()/2.0f;
	}
	
	private float hexCentreY (int row, int col, HexColour colour) {
		return yCoord(row-1, col-1, colour) + hexHalfHeight; // patterns.getHexHeight()/2.0f;
	}

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

	

	private void addNumber(int row, int col, HexColour colour) {
		Element number = addElement("text");
		number.setAttribute("style", "font-family:Arial;font-size:" + Float.toString(8 * scaling));
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
		
		float x1 = xCoord(rowA, colA, colour) + patterns.getCornerX(direction) * scaling;
		float y1 = yCoord(rowA, colA, colour) + patterns.getCornerY(direction) * scaling;
		int opposite = (direction + 3) % 6;
		float x2 = xCoord(rowB, colB, colour) + patterns.getCornerX(opposite) * scaling;
		float y2 = yCoord(rowB, colB, colour) + patterns.getCornerY(opposite) * scaling;
		
		Element line = addElement("line");
		line.setAttribute("x1", coordinate(x1));
		line.setAttribute("y1", coordinate(y1));
		line.setAttribute("x2", coordinate(x2));
		line.setAttribute("y2", coordinate(y2));
		line.setAttribute("stroke", colour.getRGB());
		// default line width is 1
		line.setAttribute("stroke-width",Float.toString(scaling));

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
		float dx = xCoord(row, col, colour) ;
		float dy = yCoord(row, col, colour);
		use.setAttribute("transform", "scale(" + Float.toString(scaling) + ")");
//		use.setAttribute("transform", "matrix(" + Float.toString(scaling) + ",0,0," + Float.toString(scaling) +
//				",0,0)"); // + Float.toString(dx / scaling) + "," + Float.toString(dy / scaling) + ")");
		use.setAttribute("x", Float.toString(dx / scaling));
		use.setAttribute("y", Float.toString(dy / scaling));
		// reference the pattern
		use.setAttributeNS(XPathUtil.XLINK_NS, "xlink:href", "#" + patterns.getHex(colour).getId());
//		appendingElement.appendChild(use);
		// add the logical r, c number
		addNumber(row,  col,  colour);
//		markPoint(dx,dy);
		return use;
	}


	
	
}
