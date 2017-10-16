package uk.co.furniss.draw.dom;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import uk.co.furniss.xml.svg.BaseElement;

public class SVGutil {
	
	public static final XPathUtil XPU = XPathUtil.getSVG();
	private final float xUnit;
	private float yUnit;

	
	private final float margin;
	private final Map<HexColour, Float> xOffsets = new EnumMap<>(HexColour.class);
	private final Map<HexColour, Float> yOffsets = new EnumMap<>(HexColour.class);


	private final float xNumber;
	private final float yNumber;
	private final Element svgDoc;

	private final Patterns patterns;

	public SVGutil(Element docElement, float xSpacing) {
		this.svgDoc = docElement;
		doc = svgDoc.getOwnerDocument();
				
        double cos30 = Math.cos(Math.PI/ 6.0);
        
        this.xUnit = xSpacing;
        yUnit = (float) (xUnit * cos30);
        margin = xUnit * 0.5f;
        
		patterns = new Patterns(docElement);
		
		for (HexColour colour : HexColour.values()) {
			xOffsets.put(colour,  colour.xOffset(xUnit));
			yOffsets.put(colour,  colour.yOffset(yUnit));
		}
        
        // location of a number within a hex
        float side = patterns.getHexWidth() / 2.0f;
        xNumber = side * 0.5f;
        yNumber = side * .2f;
        
 		
	}


	public void systemCell(Cell cell, String name) {
		String fillColour = "white";
		String strokeColour = "black";
		String radius = "7";

		cellCircle(cell.getRow(), cell.getCol(), cell.getLevel(), fillColour, strokeColour, radius);
		
		labelCell(cell, name);
		
	}

	public void adjacentCell(Cell cell) {
		String fillColour = "white";
		String strokeColour = "none";
		String radius = "3";
		cellCircle(cell.getRow(), cell.getCol(), cell.getLevel(), fillColour, strokeColour, radius);
	}
	
	public void nextButOneCell(Cell cell) {
		String fillColour = "white";
		String strokeColour = "none";
		String radius = "1.5";
		cellCircle(cell.getRow(), cell.getCol(), cell.getLevel(), fillColour, strokeColour, radius);
	}
	
	private void cellCircle(int row, int col, int level, String fillColour, String strokeColour, String radius) {
		HexColour colour = Levels.cellLevel(level);
		float cx = hexCentreX(row, col, colour) + patterns.getHex(colour).getCellXoffset(level);
		float cy = hexCentreY(row, col, colour) - patterns.getHex(colour).getCellYoffset(level);
		Element circle = addElement("ellipse");
		circle.setAttribute("style", "opacity:1;fill:" + fillColour + ";fill-opacity:1;stroke:"
				+ strokeColour + ";stroke-width:0.61885488;stroke-miterlimit:4;stroke-dasharray:none;"
				+ "stroke-dashoffset:0;stroke-opacity:1");
		circle.setAttribute("rx", radius);
		circle.setAttribute("ry", radius);
		circle.setAttribute("cx", Float.toString(cx));
		circle.setAttribute("cy", Float.toString(cy));
	}
	
	private void labelCell(Cell cell, String name) {
		HexColour colour = cell.getCellColour();
		int row = cell.getRow();
		int col = cell.getCol();
		int level = cell.getLevel();
		float x = hexCentreX(row, col, colour) + patterns.getHex(colour).getCellXoffset(level);
		float y = hexCentreY(row, col, colour) - patterns.getHex(colour).getCellYoffset(level) + 8 ;
		
		Element box = addElement("rect");
		Element label = addElement("text");
		box.setAttribute("style", "fill:rgb(238,170,255);stroke:none");
		float width = name.length() * 8;
		box.setAttribute("width", Float.toString(width));
		box.setAttribute("height", "12");
		box.setAttribute("x", Float.toString(x));
		box.setAttribute("y", Float.toString(y));
		label.setAttribute("x", Float.toString(x + 4));
		// text counts from the bottom ?
		label.setAttribute("y", Float.toString(y + 10));
		label.setAttribute("style", "fill:black;stroke:none;font-size:10.5;font-family:sans-serif");
		label.setTextContent(name);
	}
	
	public float hexCentreX (int row, int col, HexColour colour) {
		return xCoord(row-1, col-1, colour) + patterns.getHexWidth()/2.0f;
	}
	
	public float hexCentreY (int row, int col, HexColour colour) {
		return yCoord(row-1, col-1, colour) + patterns.getHexHeight()/2.0f;
	}
	

	;
	/**
	 * x coordinate of hex top-right origin
	 */
	public float xCoord(int row, int col, HexColour colour) {
		boolean oddRow = row % 2 == 1;
		float xOffset = (oddRow ? xUnit *  1.5f  : 0.0f) + xOffsets.get(colour);
		int effectiveCol = col - (oddRow && colour == HexColour.GREEN ? 1 : 0);
		return margin + xUnit*effectiveCol*3.0f  + xOffset;
	}
	
	/**
	 * y coordinate of hex top-right origin
	 */
	public float yCoord(int row, int col, HexColour colour) {
		return margin + yUnit*row*3.0f  + yOffsets.get(colour);
	}

	
	public void addNumber(int row, int col, HexColour colour) {
		Element number = addElement("text");
		number.setAttribute("style", "font-family:Arial;font-size:8");
		number.setAttribute("x", Float.toString(xCoord(row, col, colour)+xNumber));
		number.setAttribute("y", Float.toString(yCoord(row, col, colour)+yNumber));
		number.setTextContent(String.format("%02d %02d",  row+1, col+1));
	}

	public void addLink(HexColour colour, int rowA, int colA, int rowB, int colB, int direction) {
		
		float x1 = xCoord(rowA, colA, colour) + patterns.getCornerX(direction);
		float y1 = yCoord(rowA, colA, colour) + patterns.getCornerY(direction);
		int opposite = (direction + 3) % 6;
		float x2 = xCoord(rowB, colB, colour) + patterns.getCornerX(opposite);
		float y2 = yCoord(rowB, colB, colour) + patterns.getCornerY(opposite);
		
		Element path = addElement("path");
		path.setAttribute("d", "M " + pathStep(x1,y1) + " " + pathStep(x2,y2));
		path.setAttribute("style", "stroke:" + colour.getRGB() + ";stroke-width:1");
	}
	
	private Element addElement(String name) {
		Element child = doc.createElementNS(XPathUtil.SVG_NS, name);
		svgDoc.appendChild(child);
		return child;
	}
	
	private String pathStep(float x, float y) {
		return String.format("%7.2f,%7.2f",  x, y).replaceAll("\\s*", "");
	}
	
	public static float[][] getPathCoords(Element pathElement) {
		String dString = pathElement.getAttribute("d");
		String [] pieces = dString.split("\\s+");
		float [][] answer = new float[pieces.length - 2][];
		for (int i = 1; i < pieces.length -1; i++) {
			String [] xyString = pieces[i].split(",");
			float [] xy = new float[2];
			xy[0] = Float.parseFloat(xyString[0]);
			xy[1] = Float.parseFloat(xyString[1]);
			answer[i-1] = xy;
		}
		return answer;
	}
	
	public static void rgbColours(Element topElement) {
        for (Element styledElement : XPU.findElements(topElement, "//*[@style]")) {
			BaseElement node = new BaseElement(styledElement);
			node.rgbColour();
		}
	}

	public static void rotateColours(Element group, int rotations) {
		
		System.out.println("Looking for style in " + group.getNodeName() + ", id=" + group.getAttribute("id"));
        for (Element styledElement : XPU.findElements(group, ".//*[@style]")) {
			BaseElement node = new BaseElement(styledElement);
			node.rotateColour(node, rotations);
        }

	}
	
	public void roundNumbers() {
		roundNumbers(svgDoc);
	}

	public static void roundNumbers(Element topElement) {
		for (Element attribElement : XPU.findElements(topElement, "//*[@*]")) {
			boolean normalElement = !attribElement.getNodeName().contains("text");

			for (Node attribute : XPU.findNodes(attribElement, "@*")) {
				
				String attributeName = attribute.getNodeName();
				if (normalElement || attributeName.equals("x") || attributeName.equals("y")) {
					String attrValue = attribute.getNodeValue();
					attribElement.setAttribute(attribute.getNodeName(),
							attrValue.replaceAll("(\\d*\\.\\d{2})\\d+", "$1"));
				}
			}
		}
	}
	
	private static final Pattern TRANSLATE_PATTERN = Pattern.compile("translate\\(([\\d-\\.]+),([\\d-\\.]+)\\)");
	private static final Pattern XY_PATTERN = Pattern.compile("([\\d-\\.]+),([\\d-\\.]+)");
	public static void untranslateGroup(Element group) {
		String transform = group.getAttribute("transform");
		if (transform != null) {
			Matcher m = TRANSLATE_PATTERN.matcher(transform);
			if (m.matches()) {
				float dx = Float.parseFloat(m.group(1));
				float dy = Float.parseFloat(m.group(2));
			
				List<Element> xyElements = XPU.findElements(group, ".//*[@x]");
				for (Element element : xyElements) {
					float xx = Float.parseFloat(element.getAttribute("x")) + dx;
					float yy = Float.parseFloat(element.getAttribute("y")) + dy;
					element.setAttribute("x", Float.toString(xx));
					element.setAttribute("y", Float.toString(yy));
				}
				List<Element> pathElements = XPU.findElements(group, ".//path");
				for (Element path : pathElements) {
					String pathString = path.getAttribute("d");
					String [] steps = pathString.split("\\s+");
					List<String> newSteps = new ArrayList<>(steps.length);
					int toMove = 1;
					for (String step : steps) {
						Matcher mm = XY_PATTERN.matcher(step);
						if (mm.matches() && toMove > 0) {
							float nx = Float.parseFloat(mm.group(1)) + dx;
							float ny = Float.parseFloat(mm.group(2)) + dy;
							newSteps.add(Float.toString(nx) + "," + Float.toString(ny));
							toMove--;
						} else {
							if (step.matches("[A-Z]")) {
								System.out.println("set toMove high on " + step + " in " + path.getAttribute("id"));
								toMove = 100;
							} else if (toMove > 1) {
								toMove = 0;
							}
							newSteps.add(step);
						}
						
					}
					path.setAttribute("d", String.join(" ", newSteps));
				}
				group.removeAttribute("transform");
			}
		}
		
	}
	
	public void loadRectangles(Element svgDoc) {

	}
	
	public static void ensureNamespace(String prefix, String nameSpace,  Element parent) {
//		System.out.println("Setting attribute " + prefix + " to be " + nameSpace);
		parent.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,  "xmlns:" + prefix, nameSpace);
//		parent.setAttributeNS(nameSpace, "dummy", "xxx");
//		parent.removeAttributeNS(nameSpace, "dummy");
	}
	
	public Element addClone(String pattern, float dx, float dy) {
		Element use = doc.createElementNS(XPathUtil.SVG_NS, "use");
		use.setAttribute("height", "100%");
		use.setAttribute("width", "100%");
		use.setAttribute("x", Float.toString(dx));
		use.setAttribute("y", Float.toString(dy));
		use.setAttributeNS(XPathUtil.XLINK_NS, "xlink:href", "#" + pattern);
		svgDoc.appendChild(use);
		return use;
	}
	
	public Element addHex(HexColour colour, int row, int col) {
		Element use = addElement("use");
		use.setAttribute("height", "100%");
		use.setAttribute("width", "100%");
		float dx = xCoord(row, col, colour);
		float dy = yCoord(row, col, colour);
		use.setAttribute("x", Float.toString(dx));
		use.setAttribute("y", Float.toString(dy));
		use.setAttributeNS(XPathUtil.XLINK_NS, "xlink:href", "#" + patterns.getHex(colour).getId());
		svgDoc.appendChild(use);
		addNumber(row,  col,  colour);
		return use;
	}
	
	
	private static final Pattern SCALE_PATTERN = Pattern.compile("scale\\(([\\d\\.-]+),([\\d\\.-]+)\\)");
	private Document doc;

	public static void unscaleText(Element top) {
		List<Element> scaledText = XPU.findElements(top, "//text[@transform]");
		for (Element textElement : scaledText) {
			String transform = textElement.getAttribute("transform");
			if (transform != null) {
				Matcher m = SCALE_PATTERN.matcher(transform);
				if (m.matches()) {
					float xScale = Float.parseFloat(m.group(1));
					float yScale = Float.parseFloat(m.group(2));
					List<Element> selfAndChildren = XPU.findElements(textElement, ".//*[@x]");
					for (Element scalable : selfAndChildren) {
						float xx = Float.parseFloat(scalable.getAttribute("x")) * xScale;
						float yy = Float.parseFloat(scalable.getAttribute("y")) * yScale;
						scalable.setAttribute("x", Float.toString(xx));
						scalable.setAttribute("y", Float.toString(yy));
					}
					textElement.removeAttribute("transform");

				} else {
					System.out.println("Leaving transform " + transform + " for " + XPU.findString(textElement, "tspan"));
				}
			}

		}
	}


	public void writeToFile(String outFile) {
        try(  PrintWriter out = new PrintWriter( outFile )  ){
            out.println( XmlUtil.serialiseXml(svgDoc, true) );
            System.out.println("Wrote " + outFile);
        } catch (FileNotFoundException e) {
			throw new IllegalArgumentException("Failed to write to " + outFile, e);
		}
	}


}
