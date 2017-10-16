package uk.co.furniss.draw.dom;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

public class Rectangle implements Comparable<Rectangle>  {

	private final float size;
	private String id;
	private final float xCentre;
	private final float yCentre;
	private final String colour;
	final double bearing;
	private final double range;
	
	public Rectangle(Element rectElement, float xZero, float yZero) {
		id = rectElement.getAttribute("id");
		String w = rectElement.getAttribute("width");
		String h = rectElement.getAttribute("height");
		if (! w.equals(h)) {
			throw new IllegalArgumentException("Different height and width for " + id);
		}
		size = Float.parseFloat(w);
		float halfSize = size / 2.0f;
		xCentre = Float.parseFloat(rectElement.getAttribute("x")) - xZero + halfSize;
		yCentre = -(Float.parseFloat(rectElement.getAttribute("y"))- yZero + halfSize);
		double theta = Math.atan2(yCentre, xCentre) * 180.0 / Math.PI;
		bearing = theta;
		range = Math.sqrt(xCentre*xCentre + yCentre*yCentre);
		String style = rectElement.getAttribute("style");
		Pattern colourPattern = Pattern.compile(".*fill:(.*?)(?:;|$).*");
		Matcher m = colourPattern.matcher(style);
		if (m.matches()) {
			colour = m.group(1);
		} else {
			throw new IllegalArgumentException("Can't find colour in " + style + " for " + id);
		}
	}

	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return  id + "  " + colour + "(" + size + ")  r: " + range + ", th: " + bearing
				+ "   " + xCentre + ", " + yCentre;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getColour() {
		return colour;
	}

	public float getSize() {
		return size;
	}

	public double getBearing() {
		return bearing;
	}

	public double getRange() {
		return range;
	}

	@Override
	public int compareTo(Rectangle o) {
		return Double.compare(bearing,  o.bearing);
	}
	public float getXcentre() {
		
		return xCentre;
	}

	public float getYcentre() {
		
		return yCentre;
	}

	
}
