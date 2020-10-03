package uk.co.furniss.draw.gfmap;

import org.w3c.dom.Element;

public class GfMapMain {

	private GfMapMain() {
		
	}

	public static void main(String[] args) {
		

		String directory = "c:/Users/Peter/Documents/games/godfire_pix/basepix/";
		String svgSuffix = ".svg";
		String fileName = "pattern_hexes";

		
		String filePath = directory + fileName + svgSuffix;
		System.out.println("Will read " + filePath);

		// all the map
		//  hexes don't have scaling yet
		// (tweak OVERLAP in GfMap  to slightly change number of pages
        float spacing = 34.0f;
		int rows = 16;
		int cols = 12;
		
        GfMap gf = new GfMap(filePath, rows, cols, spacing);
        
        Element layer = null;
        final String outFileName;
		if (args.length == 2) {
			int horiz = Integer.parseInt(args[0]);
			int vert  = Integer.parseInt(args[1]);
			layer = gf.makeMap(horiz,  vert);
			outFileName = "godsfire_" + Integer.toString(horiz) + "_" + Integer.toString(vert);
		} else {
			int horizontal = gf.pagesWide();
			int vertical =   gf.pagesDeep();
        
			for (int h = 0; h < horizontal; h++) {
				for (int v = 0; v < vertical ; v++ ) {
					layer = gf.makeMap(v, h);
                // what about the nebulae ?
				}
			}
			outFileName = "godsfire_map";
	        System.out.println("Map is on " + (horizontal * vertical) + " pages, " +horizontal + " pages wide, " + vertical + " pages deep");
		}
        layer.setAttribute("style",  "display:inline");
        String outFile = directory + outFileName + svgSuffix;

        gf.writeToFile(outFile);
        
	}


}
