package uk.co.furniss.draw.gfmap;

import org.w3c.dom.Element;

public class GfMapMain {

	private GfMapMain() {
		
	}

	private static float ORIGINAL_CELL_SIZE = 15.0f;
	private static float ORIGINAL_HEX_SIDE = 32.0f;
	
	
	public static void main(String[] args) {
		

		String directory = "c:/Users/Peter/Documents/games/godfire_pix/basepix/";
		String svgSuffix = ".svg";
		String fileName = "pattern_hexes";
		
		boolean transpose = true;
		
		String filePath = directory + fileName + svgSuffix;
		System.out.println("Will read " + filePath);

		// all the map
		//  hexes don't have scaling yet
		// (tweak OVERLAP in GfMap  to slightly change number of pages
		int rows = 16;
		int cols = 12;

		if (transpose) {
			int other = rows;
			rows = cols;
			cols = other;
		}
		
		float cellSize = 11;
		float scaleFactor = cellSize / ORIGINAL_CELL_SIZE;
		float hexSide = ORIGINAL_HEX_SIDE * scaleFactor;		
		float horizontalGap = 2.0f * scaleFactor;
		
        GfMap gf = new GfMap(filePath, rows, cols, hexSide, horizontalGap, transpose);
        
		int horizontals = gf.pagesWide();
		int verticals =   gf.pagesDeep();
		System.out.println("Map is " + (horizontals * verticals) + " pages, " +horizontals + " pages wide, " + verticals + " pages deep");
		
		Element layer = null;
        final String outFileName;
        if (args.length == 1) {
        	// single argument - do the whole map as one layer
        	gf.makeMap();
        	outFileName = "godsfire_onemap";
        } else if (args.length == 2) {
        	// two arguments - do a single page
			int horiz = Integer.parseInt(args[0]);
			int vert  = Integer.parseInt(args[1]);
			if (horiz >= horizontals || vert >= verticals) {
				throw new IllegalArgumentException("Page " + horiz + ", " + vert + " is outside the boundaries of the map (page number is zero-based)");
			}
			if (transpose) {
				layer = gf.makeMap(vert, horiz);
			} else {
				layer = gf.makeMap(horiz,  vert);
			}
			outFileName = "godsfire_" + Integer.toString(horiz) + "_" + Integer.toString(vert);
		} else {
			// no (or >=3 ?) arguments - do the whole map with a layer for each page

			for (int h = 0; h < horizontals; h++) {
				for (int v = 0; v < verticals ; v++ ) {
					layer = gf.makeMap(v, h);
                // what about the nebulae ?
				}
			}
			outFileName = "godsfire_map";
		}
        if (layer != null) {
        	layer.setAttribute("style",  "display:inline");
        }
        String outFile = directory + outFileName + svgSuffix;

        gf.writeToFile(outFile);
        
	}


}
