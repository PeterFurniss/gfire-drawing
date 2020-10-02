package uk.co.furniss.draw.gfmap;

import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Element;

import uk.co.furniss.draw.dom.XPathUtil;
import uk.co.furniss.draw.dom.XYcoords;
import uk.co.furniss.draw.dom.XmlUtil;

public class GfMap {

	int maxRows;
	int maxCols;
	private final GfMapBuilder svg;
	private final Element mapGroup;
	private Set<Cell> nextButOne = new HashSet<>();

	
	public GfMap(String patternFileName, int maxRows, int maxCols, float spacing) {
		this.maxRows = maxRows;
		this.maxCols = maxCols;
		
        Element svgDoc = XmlUtil.deserialiseXmlFile(patternFileName);

        svg = new GfMapBuilder(svgDoc, spacing);
        mapGroup = svg.makeGroup("gfmap");
        
		GfMapBuilder.ensureNamespace("xlink", XPathUtil.XLINK_NS, svgDoc);


    	makeMap();

	}

	private static final float OVERLAP = 12.0f;
	private static final float PAGE_WIDTH = 210.0f - OVERLAP;
	private static final float PAGE_HEIGHT = 297.0f - OVERLAP;

	public void moveToPage(int row, int col) {
		svg.translateElement(mapGroup, new XYcoords( -col * PAGE_WIDTH, -row * PAGE_HEIGHT));
	}
	
	private void makeMap() {
		svg.addComment("the hexes by row, column and colour");
		for (int row = 0; row < maxRows; row++) {
    		for (int col = 0; col < maxCols; col++) {
    			svg.addHex(HexColour.RED, row, col);
    			svg.addHex(HexColour.GREEN, row, col);
    			svg.addHex(HexColour.BLUE, row, col);
        		
        	}
        }
        // do the line links between hexes of same colour
		svg.addComment("linking lines between hexes of the same colour");
		int direction;
     	for (int row = 0; row < maxRows; row++) {
     		int odd = row % 2;
    		for (int col = 0; col < maxCols; col++) {
    			
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
	
	// mark system cells and their neighbours
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
}
