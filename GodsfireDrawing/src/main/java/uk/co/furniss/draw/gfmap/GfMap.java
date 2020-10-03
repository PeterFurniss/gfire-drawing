package uk.co.furniss.draw.gfmap;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import uk.co.furniss.draw.dom.XPathUtil;
import uk.co.furniss.draw.dom.XYcoords;
import uk.co.furniss.draw.dom.XmlUtil;

public class GfMap {

	private static final float PAGE_HEIGHT = 297.0f;
	private static final float PAGE_WIDTH = 210.0f;
	int maxRows;
	int maxCols;
	private final GfMapBuilder svg;
	private Set<Cell> nextButOne = new HashSet<>();

	
	private static final Logger LOGGER = LoggerFactory.getLogger(GfMap.class.getName());
	
	public GfMap(String patternFileName, int maxRows, int maxCols, float hexSide, float interHexGap) {
		this.maxRows = maxRows;
		this.maxCols = maxCols;
		
        Element svgDoc = XmlUtil.deserialiseXmlFile(patternFileName);

        svg = new GfMapBuilder(svgDoc, hexSide, interHexGap);
        
		GfMapBuilder.ensureNamespace("xlink", XPathUtil.XLINK_NS, svgDoc);


	}

	private static final float OVERLAP = 25.0f;
	private static final float EXPOSED_PAGE_WIDTH = PAGE_WIDTH - OVERLAP;
	private static final float EXPOSED_PAGE_HEIGHT = PAGE_HEIGHT - OVERLAP;

	// do the whole thing as one diagram
	public void makeMap() {
		drawHexes(0, maxRows-1, 0, maxCols - 1);
		drawLinkLines(0, maxRows-1, 0, maxCols - 1);
		markSystems();
	}

	// do one page
	Element makeMap(int verticalPage, int horizontalPage ) {
		String suffix = "_" + Integer.toString(verticalPage) + "_" + Integer.toString(horizontalPage);
		Element layer = svg.makeLayer("page" + suffix);
		Element pageGroup = svg.makeGroup("grp" + suffix,  layer);
		// work out which are the extreme hexes (or triads) that will be needed
		float leftX = horizontalPage * EXPOSED_PAGE_WIDTH;
		float rightX = leftX + PAGE_WIDTH;
		float topY = verticalPage * EXPOSED_PAGE_HEIGHT;
		float bottomY = topY + PAGE_HEIGHT;
		int firstCol = svg.getColOfX(leftX, true, maxCols);
		int lastCol = svg.getColOfX(rightX, false, maxCols);
		LOGGER.debug("cols {} to {}", firstCol, lastCol);
		int firstRow = svg.getRowOfY(topY, true, maxRows);
		int lastRow = svg.getRowOfY(bottomY, false, maxRows);
		LOGGER.debug("rows {} to {}", firstRow, lastRow);
		drawHexes(firstRow, lastRow, firstCol, lastCol);
        drawLinkLines(firstRow, lastRow, firstCol, lastCol);
        markSystems();
        svg.translateElement(pageGroup, new XYcoords( -horizontalPage * EXPOSED_PAGE_WIDTH, -verticalPage * EXPOSED_PAGE_HEIGHT));
        return layer;
	}

	
	public void drawHexes(int firstRow, int lastRow, int firstCol, int lastCol) {
		svg.addComment("the hexes by row, column and colour");
		for (int row = firstRow; row <= lastRow; row++) {
    		for (int col = firstCol; col <= lastCol; col++) {
    			svg.addHex(HexColour.RED, row, col);
    			svg.addHex(HexColour.GREEN, row, col);
    			svg.addHex(HexColour.BLUE, row, col);
        		
        	}
        }
	}
	
	public void drawLinkLines(int firstRow, int lastRow, int firstCol, int lastCol) {
		// do the line links between hexes of same colour
		svg.addComment("linking lines between hexes of the same colour");
		int direction;
		// because the lines come from the cells to the west, we need to include cells that are to the
		// west (and north and south) of the ones that would really be here
		int startRow = firstRow > 0 ? firstRow - 1 : firstRow;
		int finalRow = lastRow < maxRows - 1 ? lastRow + 1 : lastRow;
		int startCol = firstCol > 0 ? firstCol - 1 : firstCol;
		
     	for (int row = startRow; row <= finalRow; row++) {
     		int odd = row % 2;
    		for (int col = startCol; col <= lastCol; col++) {
    			// the line suppression is correctly using maxRows, maxCols, not last of each
    			// east
    			if (col < maxCols-1) {
    				direction = 5;
    				svg.addLink(HexColour.RED, row, col, row, col+1, direction);
	    			svg.addLink(HexColour.GREEN, row, col, row, col+1, direction);
	    			svg.addLink(HexColour.BLUE, row, col, row, col+1, direction);
    			}
    			// northeast
    			if (row > 0 ) {
    				direction = 4;
    				if (col + odd < maxCols) {
    					svg.addLink(HexColour.RED,   row, col, row-1, col+odd, direction);
            			svg.addLink(HexColour.BLUE,  row, col, row-1, col+odd, direction);
    				}
    				if (col - odd + 1 < maxCols) {
    					svg.addLink(HexColour.GREEN, row, col, row-1, col+1-odd, direction);
    				}
       			}  		
    			//southeast
    			if (row < maxRows-1) {
    				direction = 0;
    				if (col + odd < maxCols) {
    	    			svg.addLink(HexColour.RED, row, col, row+1, col+odd, direction);
    	    			svg.addLink(HexColour.BLUE, row, col, row+1, col + odd, direction);
    				}
    				if ( col + 1 - odd < maxCols) {
    					svg.addLink(HexColour.GREEN, row, col, row+1, col+1-odd, direction);
    				}
    			}

        		
        	}
        }
	}


	
	public void markSystems() {
		setPlanet(2, 2, -4, "Loshan");
        setPlanet(3, 4,  2, "Kol");
        setPlanet(2, 9,  0, "Dasar");
        setPlanet(4, 7,  1, "Moros");
        setPlanet(6, 4, -1, "Soont");
        setPlanet(5,11,  4, "Nosset");
        setPlanet(7, 2, -2, "Assab");
        
        setPlanet( 9,  9,  0, "Huacho");
        setPlanet(10,  6,  2, "Tufan");
        setPlanet(10, 11, -4, "Weribe");
        setPlanet(11,  3,  0, "Zia");
        setPlanet(13,  9, -2, "Vand");
        setPlanet(15,  2,  4, "Pirr");
        setPlanet(15, 11,  4, "Chula");
     	markNextButOnes();
	}

	
	// mark system cells and their neighbours
	// just write all of these anyway - coping with the window boundaries is too complicated
	public void setPlanet(int row, int col, int gfLevel, String name) {
     	Cell sys = new Cell(row, col, gfLevel);
     	Set<Cell> closer = new HashSet<>();
     	Set<Cell> ourNextButOne = new HashSet<>();
     	svg.systemCell(sys, name);
     	closer.add(sys);
     	for (Cell neighbour : sys.neighbours(maxRows, maxCols)) {
			svg.adjacentCell(neighbour);
			closer.add(neighbour);
			ourNextButOne.addAll(neighbour.neighbours(maxRows,  maxCols));
		}
     	// the next-but-ones overlap for some systems (Soont and Assab), so we combine them all in a hashset to 
     	// avoid duplicates
//     	int before = nextButOne.size();
     	nextButOne.addAll(ourNextButOne);
//     	System.out.println(name + " ( at " + sys + ") has " + ourNextButOne.size() + " next but one, of which " + (nextButOne.size() - before)
//     			+ " are new");
//     	System.out.println(ourNextButOne);
	}

	public void markNextButOnes() {
		svg.addComment("next-but-one cells for all systems (which may overlap)");
     	for (Cell cell : nextButOne) {
			svg.nextButOneCell(cell);
		}

	}
	public void writeToFile(String outFile) {
	     // from 3  (hexes_5 has the exact path)
        svg.roundNumbers();
        
        svg.writeToFile(outFile);
	}

	public int pagesWide() {
		return (int) (svg.xCoord(0,  maxCols, HexColour.GREEN) / EXPOSED_PAGE_WIDTH) + 1;
	}

	public int pagesDeep() {
		return (int) (svg.yCoord( maxRows, 0, HexColour.BLUE) / EXPOSED_PAGE_HEIGHT) + 1;
	}




}
