package uk.co.furniss.xml.svg;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

public class BaseElement {

	private static Pattern COLOUR_PATTERN = Pattern.compile("((?:stroke|fill|stop-color):)([^-].*?)(;|$)");
	private final String name;
	private final Element element;
	
	public BaseElement(Element element) {
		this.element = element;
		this.name = element.getLocalName();
		
	}

	public String getName() {
		return name;
	}

	public Element getNode() {
		return element;
	}

	public void rgbColour() {
		String style = getStyle();
		Matcher cM = COLOUR_PATTERN.matcher(style);
		StringBuffer newStyle = new StringBuffer();
		
		while (cM.find()) {
			String oldColour = cM.group(2);
			cM.appendReplacement(newStyle, cM.group(1) + ColourManager.convert(oldColour) + cM.group(3));
		}
		cM.appendTail(newStyle);
		setStyle(newStyle.toString());
		
	}
	
	public void rotateColour(int rotations) {
		String style = getStyle();
		Matcher cM = COLOUR_PATTERN.matcher(style);
		StringBuffer newStyle = new StringBuffer();
		
		while (cM.find()) {
			String oldColour = cM.group(2);
			cM.appendReplacement(newStyle, cM.group(1) + ColourManager.rotate(oldColour, rotations) + cM.group(3));
		}
		cM.appendTail(newStyle);
		setStyle(newStyle.toString());
		
	}
	
	private void setStyle(String newStyle) {
		element.setAttribute("style", newStyle);
		
	}

	public String getStyle() {
		return element.getAttribute("style");
	}


	String rgb(String hex) {
		StringBuilder b = new StringBuilder();
		String glue = "rgb(";
		for (int i=0; i < 6; i += 2) {
			// there is almost certainly a better way of doing this
			b.append(glue).append(Integer.toString(Byte.toUnsignedInt((byte) Integer.parseInt(hex.substring(i,i+2), 16))));
			glue = ",";
		}
		return b.append(")").toString();
	}


}
