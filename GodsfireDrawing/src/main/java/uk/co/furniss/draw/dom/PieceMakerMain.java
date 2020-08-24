package uk.co.furniss.draw.dom;

/**
 * make a sheet of pieces
 *  specifiation file says how many of each, corner numbers, which silhouette and colours
 *  		(that will take som designing).
 * 	piece silhouette is an image from an input svg file. object has a specific name
 */
public class PieceMakerMain {

	private PieceMakerMain() {
		
	}

	public static void main(String[] args) {
		String directory = "c:/Users/Peter/Documents/games/godfire_pix/basepix/";
		String svgSuffix = ".svg";
		String specSuffix = ".txt";
		String specName = "hexes5";
		String silhouName = "pieces";
		String outName = "madePieces";

		
		String specFile = directory + specName + specSuffix;
		System.out.println("Will read specification file " + specFile);

		String silhouFile = directory + silhouName + svgSuffix;
		System.out.println("Will read silhouette file " + silhouFile);
		
		ImageLibrary images = new ImageLibrary(silhouFile);
		
		// -----------------------
		 // need to rethink - need to mangle the svg file, not try and move things from it
		//    so after putting in what we want, take out all the stuff we don't want
		
        float spacing = 34.0f;
		int rows = 8;
		int cols = 12;
		
        GfMap gf = new GfMap(specFile, rows, cols, spacing);
        

        String outFile = directory + specName + "_modc" + svgSuffix;

        gf.writeToFile(outFile);
        
	}

}
