package uk.co.furniss.xml.svg;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;

public class BaseElementS9 {

	private static final QName ATTR_STYLE = new QName("style");
	private static Pattern oldColourPattern = Pattern.compile(":#([\\da-f]{6})(?:;|$)");
	private final String name;
	private final XdmNode node;
	
	public BaseElementS9(XdmNode node) {
		this.node = node;
		this.name = node.getNodeName().getLocalName();
		
	}

	public String getName() {
		return name;
	}

	public XdmNode getNode() {
		return node;
	}

	public void rgbColour() {
		String style = getStyle();
		Matcher colourMatch = oldColourPattern.matcher(style);
		StringBuffer newStyle = new StringBuffer();
		
		while (colourMatch.find()) {
			String oldColour = colourMatch.group(1);
			colourMatch.appendReplacement(newStyle, ":" + rgb(oldColour) + ";");
		}
		colourMatch.appendTail(newStyle);
//		setStyle(newStyle.toString());
		
	}
	
	public String getStyle() {
		return node.getAttributeValue(ATTR_STYLE);
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
