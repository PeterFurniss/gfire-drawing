package uk.co.furniss.draw.dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;

public class PatternHex {

	private static final XPathUtil XPU = XPathUtil.getSVG();
	
	private final HexColour colour;
	private final List<Rectangle> posns;

	private final Element element;

	private final String id;

	public PatternHex(HexColour colour, Element hexElement) {
		this.element = hexElement;
		this.colour = colour;
		this.id = element.getAttribute("id");
		Element path = XPU.findElement(hexElement, "path[1]");
		float[][] pathCoords = SVGutil.getPathCoords(path);
		float topX = 0.0f;
		float topY = 0.0f;
		for (float[] xy : pathCoords) {
			if (xy[0] > topX) {
				topX = xy[0];
			}
			if (xy[1] > topY) {
				topY = xy[1];
			}
		}
		float centreX = topX / 2.0f;
		float centreY = topY / 2.0f;
		List<Element> rElements = XPU.findElements(hexElement,  "rect|g/rect[1]");
        if (rElements.size() != 12) {
        	throw new IllegalArgumentException("Found " + rElements.size() + " rect in " 
        				+ hexElement.getAttribute("id"));
        }
        posns = new ArrayList<>(12);
        for (Element rE : rElements) {
			posns.add(new Rectangle(rE, centreX, centreY));
		}
        Collections.sort(posns);
        // the -6 is liable to come above the centre, but is always near it
        double nearness = 100.0;
        Rectangle nearest = null;
        for (Rectangle rect : posns) {
			double r = rect.getRange();
			if (r < nearness) {
				nearness = r;
				nearest = rect;
			}
        }
        posns.remove(nearest);
        posns.add(0, nearest);
//			listPositions();
		}

	public void listPositions() {
		System.out.println(colour);
		for (Rectangle rectangle : posns) {
			System.out.println("   " + rectangle.toString());
		}
	}
	
	public float getCellXoffset(int level) {
		return posns.get(Levels.index(level)).getXcentre();
	}
	
	public float getCellYoffset(int level) {
		return posns.get(Levels.index(level)).getYcentre();
	}

	public Element getElement() {
		return element;
	}

	public String getId() {
		return id;
	}
}
