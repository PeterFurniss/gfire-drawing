package uk.co.furniss.draw.dom;

public class GfMapMain {

	private GfMapMain() {
		
	}

	public static void main(String[] args) {
		String directory = "c:/Users/Peter/Documents/games/godfire_pix/basepix/";
		String svgSuffix = ".svg";
		String fileName = "hexes5";

		
		String filePath = directory + fileName + svgSuffix;
		System.out.println("Will read " + filePath);

		// half the map
        float spacing = 34.0f;
		int rows = 8;
		int cols = 12;
		
        GfMap gf = new GfMap(filePath, rows, cols, spacing);
        
        gf.setPlanet(2, 2, -4, "Loshan");
        gf.setPlanet(3, 4,  2, "Kol");
        gf.setPlanet(2, 9,  0, "Dasar");
        gf.setPlanet(4, 7,  1, "Moros");
        gf.setPlanet(6, 4, -1, "Soont");
        gf.setPlanet(5,11,  4, "Nosset");
        gf.setPlanet(7, 2, -2, "Assab");
     	gf.markNextButOnes();
        String outFile = directory + fileName + "_modc" + svgSuffix;

        gf.writeToFile(outFile);
        
	}

}
