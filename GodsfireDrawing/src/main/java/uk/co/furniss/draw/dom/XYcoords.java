package uk.co.furniss.draw.dom;

import java.util.List;

public class XYcoords {
	private final float x;
	private final float y;
	
	public XYcoords(String xStr, String yStr) {
		this.x = Float.parseFloat(xStr);
		this.y = Float.parseFloat(yStr);
	}
	
	public XYcoords(float x, float y) {
		super();
		this.x = x;
		this.y = y;
	}
	
	public XYcoords(String xy) {
		String [] fields = xy.split(",");
		this.x = Float.parseFloat(fields[0]);
		this.y = Float.parseFloat(fields[1]);
		
	}
	
	public float getX() {
		return x;
	}
	public float getY() {
		return y;
	}
	
	public XYcoords maximal(XYcoords other) {
		return new XYcoords( x > other.x ? x : other.x,  y > other.y ? y : other.y);
		
	}
	
	public static XYcoords maxXY( List<XYcoords> list) {
		// could stream this, presumably
		XYcoords soFar = list.get(0);
		for (XYcoords one : list) {
			soFar = soFar.maximal(one);
		}
		return soFar;
	}

	@Override
	public String toString() {
		return x + "," + y;
	}
	
}
