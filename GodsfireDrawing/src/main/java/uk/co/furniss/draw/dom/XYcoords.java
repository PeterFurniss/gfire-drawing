package uk.co.furniss.draw.dom;

import java.util.List;

public class XYcoords {
	private final float x;
	private final float y;
	static final XYcoords ORIGIN = new XYcoords(0.0f, 0.0f);
	
	XYcoords(String xStr, String yStr) {
		this.x = Float.parseFloat(xStr);
		this.y = Float.parseFloat(yStr);
	}
	
	public XYcoords(float x, float y) {
		super();
		this.x = x;
		this.y = y;
	}
	
	XYcoords(String xy) {
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
	
	public XYcoords minus() {
		return new XYcoords(-x, -y);
	}
	
	public XYcoords add(XYcoords other) {
		if (other == null)  {
			return this;
		}
		return new XYcoords(x + other.x, y + other.y);
	}
	
	public XYcoords subtract(XYcoords other) {
		if (other == null)  {
			return this;
		}
		return new XYcoords(x - other.x, y - other.y);
	}
	
	public XYcoords deltaX(float extra) {
		return new XYcoords(x + extra, y);
	}
	
	public XYcoords deltaY(float extra) {
		return new XYcoords(x, y + extra);
	}
	
	public XYcoords bottomRightMost(XYcoords other) {
		return new XYcoords( x > other.x ? x : other.x,  y > other.y ? y : other.y);
	}
	
	XYcoords topLeftMost(XYcoords other) {
		return new XYcoords( x < other.x ? x : other.x,  y < other.y ? y : other.y);
	}
	
	// this is degenerate - should use SvgPath at least
	public static XYcoords maxXY( List<XYcoords> list) {
		// could stream this, presumably
		XYcoords soFar = list.get(0);
		for (XYcoords one : list) {
			soFar = soFar.bottomRightMost(one);
		}
		return soFar;
	}

	@Override
	public String toString() {
		return x + "," + y;
	}
	
	public String tabbed() {
		return x + "\t" + y;
	}

	public XYcoords meanWith( XYcoords o ) {
		return new XYcoords( (x + o.x) / 2, (y + o.y) / 2);
	}

	public XYcoords scaleTo(XYcoords base, float factor) {
		return scaleTo(base, factor, factor);
	}
	public XYcoords scaleTo(XYcoords base, float xFactor, float yFactor) {
		return subtract(base).scale(xFactor,  yFactor).add(base);
	}

	public XYcoords scale(float xFactor, float yFactor) {
		return new XYcoords(x * xFactor, y * yFactor);
	}
	
	public XYcoords scale(float factor) {
		return scale(factor, factor);
	}
}
