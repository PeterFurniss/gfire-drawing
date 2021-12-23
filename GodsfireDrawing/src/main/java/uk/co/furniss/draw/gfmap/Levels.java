package uk.co.furniss.draw.gfmap;

class Levels {

	private static HexColour[] levelColour = new HexColour[] 
			{ HexColour.BLUE, HexColour.GREEN, HexColour.RED,
			  HexColour.BLUE, HexColour.GREEN, HexColour.RED,
			  HexColour.BLUE, HexColour.GREEN, HexColour.RED,
			  HexColour.BLUE, HexColour.GREEN, HexColour.RED				
					
			};
	private Levels() {
		// utility
	}

	public static int index(int gfLevel) {
		return gfLevel + 6;
	}
	
	public static int gfLevel(int index) {
		return index - 6;
	}
	
	public static HexColour cellLevel(int gfLevel) {
		return levelColour[index(gfLevel)];
	}
}
