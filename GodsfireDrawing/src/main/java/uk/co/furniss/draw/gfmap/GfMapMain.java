package uk.co.furniss.draw.gfmap;

public class GfMapMain {

	private GfMapMain() {
		
	}

	public static void main(String[] args) {
		final int pageRow;
		final int pageCol;
		if (args.length == 0) {
			pageRow = 0;
			pageCol = 0;
		} else if (args.length == 2) {
			pageRow = Integer.parseInt(args[0]);
			pageCol = Integer.parseInt(args[1]);
		} else {
			throw new IllegalArgumentException("require pagerow, pagecol parameters");
		}
		
		
		String directory = "c:/Users/Peter/Documents/games/godfire_pix/basepix/";
		String svgSuffix = ".svg";
		String fileName = "pattern_hexes";

		
		String filePath = directory + fileName + svgSuffix;
		System.out.println("Will read " + filePath);

		// all the map
        float spacing = 34.0f;
		int rows = 16;
		int cols = 12;
		
        GfMap gf = new GfMap(filePath, rows, cols, spacing);
        
        // what about the nebulae ?
        
        gf.setPlanet(2, 2, -4, "Loshan");
        gf.setPlanet(3, 4,  2, "Kol");
        gf.setPlanet(2, 9,  0, "Dasar");
        gf.setPlanet(4, 7,  1, "Moros");
        gf.setPlanet(6, 4, -1, "Soont");
        gf.setPlanet(5,11,  4, "Nosset");
        gf.setPlanet(7, 2, -2, "Assab");
        
        gf.setPlanet( 9,  9,  0, "Huacho");
        gf.setPlanet(10,  6,  2, "Tufan");
        gf.setPlanet(10, 11, -4, "Weribe");
        gf.setPlanet(11,  3,  0, "Zia");
        gf.setPlanet(13,  9, -2, "Vand");
        gf.setPlanet(15,  2,  4, "Pirr");
        gf.setPlanet(15, 11,  4, "Chula");
     	gf.markNextButOnes();

        gf.moveToPage(pageRow, pageCol);

     	String outFile = directory + "godsfire_" + Integer.toString(pageRow) + "_" + Integer.toString(pageCol) + svgSuffix;

        gf.writeToFile(outFile);
        
	}

}
