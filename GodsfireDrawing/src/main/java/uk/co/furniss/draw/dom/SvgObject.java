package uk.co.furniss.draw.dom;

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
		
		// modify to cope with multiple paths - or it being the path itself
		final Element path;
		if (hexElement.getLocalName().equals("path")) {
			path = hexElement;
		} else{
			path = XPU.findElement(hexElement, "path[1]");
		}
		List<XYcoords> pathCoords = SVGbuilder.getPathCoords(path);
		XYcoords txty = XYcoords.maxXY(pathCoords);
		
		System.out.println("path nodes for " + id);
		System.out.println(pathCoords);
		System.out.println("max x, y " + txty);
		

	}



	public Element getElement() {
		return element;
	}

	public String getId() {
		return id;
	}
}
