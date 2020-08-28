package uk.co.furniss.draw.gfmap;

import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Element;

import uk.co.furniss.draw.dom.XPathUtil;
import uk.co.furniss.draw.dom.XmlUtil;

public class GfMap {

	int maxRows;
	int maxCols;
	private final GfMapBuilder svg;
	private Set<Cell> nextButOne = new HashSet<>();
	
	public GfMap(String patternFileName, int maxRows, int maxCols, float spacing) {
		this.maxRows = maxRows;
		this.maxCols = maxCols;
		
        Element svgDoc = XmlUtil.deserialiseXmlFile(patternFileName);

        svg = new GfMapBuilder(svgDoc, spacing);
        
		GfMapBuilder.ensureNamespace("xlink", XPathUtil.XLINK_NS, svgDoc);


    	makeMap();

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
     	for (int row = 0; row < maxRows; row++) {
     		int odd = row % 2;
    		for (int col = 0; col < maxCols; col++) {
    			
    			// east
    			if (col < maxCols-1) {
	    			svg.addLink(HexColour.RED, row, col, row, col+1, 5);
	    			svg.addLink(HexColour.GREEN, row, col, row, col+1, 5);
	    			svg.addLink(HexColour.BLUE, row, col, row, col+1, 5);
    			}
    			// northeast
    			if (row > 0) {
    				svg.addLink(HexColour.RED,   row, col, row-1, col+odd, 4);
        			svg.addLink(HexColour.GREEN, row, col, row-1, col+1-odd, 4);
       				svg.addLink(HexColour.BLUE,  row, col, row-1, col+odd, 4);
       			}  		
    			//southeast
    			if (row < maxRows-1) {
	    			svg.addLink(HexColour.RED, row, col, row+1, col+odd, 0);
	    			svg.addLink(HexColour.GREEN, row, col, row+1, col+1-odd, 0);
	    			svg.addLink(HexColour.BLUE, row, col, row+1, col + odd, 0);
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