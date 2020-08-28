package uk.co.furniss.draw.gfmap;

public enum HexColour 
		{RED (0.0f, 0.0f, "rgb(255,0,0)"),
		GREEN(1.5f, 1.0f, "rgb(0,255,0)"),
		BLUE (0.0f, 2.0f, "rgb(0,0,255)");
	
private float xFactor;
private float yFactor;
private String rgb;

private HexColour(float xFactor, float yFactor, String rgb) {
	this.xFactor = xFactor;
	this.yFactor = yFactor;
	this.rgb = rgb;
	}

public float xOffset(float xUnit) {
	return xFactor * xUnit;
}

public float yOffset(float yUnit) {
	return yFactor * yUnit;
}

public String getRGB() {
	return rgb;
}




}