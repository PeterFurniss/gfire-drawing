package uk.co.furniss.draw.dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Element;


public class SvgObject {

	private static final XPathUtil XPU = XPathUtil.getSVG();
	
	// the xml as read (and possibly modified)
	private final Element element;

	private final String id;

	public SvgObject (Element hexElement) {
		this.element = hexElement;
		this.id = element.getAttribute("id");
		
		// analyse the path to find overall size
		// and possibly to canonicalise the start
		
		// modify to cope with multipl paths
		Element path = XPU.findElement(hexElement, "path[1]");
		float[][] pathCoords = SVGbuilder.getPathCoords(path);
		XYcoords txty = getFirstXY(pathCoords);
		
		float centreX = txty.getX() / 2.0f;
		float centreY = txty.getY() / 2.0f;
		

		}



	private XYcoords getFirstXY( float[][] pathCoords ) {
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
		XYcoords txty = new XYcoords(topX, topY);
		return txty;
	}



	public Element getElement() {
		return element;
	}

	public String getId() {
		return id;
	}
}
