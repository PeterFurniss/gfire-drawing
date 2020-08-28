package uk.co.furniss.draw.gfmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;

import uk.co.furniss.draw.dom.XPathUtil;
import uk.co.furniss.draw.dom.XYcoords;

// i think this cracks the hand-drawn pattern hexes to understand them
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
		List<XYcoords> pathCoords = GfMapBuilder.getPathCoords(path);

		XYcoords max = XYcoords.maxXY(pathCoords);
		
		float centreX = max.getX() / 2.0f;
		float centreY = max.getY() / 2.0f;
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
