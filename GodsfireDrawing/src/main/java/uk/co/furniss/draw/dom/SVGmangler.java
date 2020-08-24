package uk.co.furniss.draw.dom;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import uk.co.furniss.xml.svg.BaseElement;

public class SVGmangler  {
	
	public static final XPathUtil XPU = XPathUtil.getSVG();




	/**
	 * build godsfire 3-d map
	 * @param docElement	parent document (or element ?). Contains the pattern hexes
	 * @param xSpacing		width a hex in whatever units
	 */
	public SVGmangler(Element docElement) {
		super();
   
 		
	}

	public static SvgObject getSvgObject(Element topElement, String name) {
		Element xmlElement = XPU.findElement(topElement, "//*[@id=" + name + "]");
		if (xmlElement != null) {
			return new SvgObject(xmlElement);
		}
		throw new IllegalStateException("Cannot find svg object " + name);
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
	
	/**
	 *  round any number representing position to 2 decimal places.
	 *  not sure this really works fo all cases
	 * @param topElement scope of change
	 */
	public static void roundNumbers(Element topElement) {
		for (Element elementWithAttributes : XPU.findElements(topElement, "//*[@*]")) {
			boolean normalElement = !elementWithAttributes.getNodeName().contains("text");

			for (Node attribute : XPU.findNodes(elementWithAttributes, "@*")) {
				
				String attributeName = attribute.getNodeName();
				if (normalElement || attributeName.equals("x") || attributeName.equals("y")) {
					String attrValue = attribute.getNodeValue();
					elementWithAttributes.setAttribute(attribute.getNodeName(),
							attrValue.replaceAll("(\\d*\\.\\d{2})\\d+", "$1"));
				}
			}
		}
	}
	
	private static final Pattern TRANSLATE_PATTERN = Pattern.compile("translate\\(([\\d-\\.]+),([\\d-\\.]+)\\)");
	private static final Pattern XY_PATTERN = Pattern.compile("([\\d-\\.]+),([\\d-\\.]+)");
	
	// i think this takes a group and moves it all back to sit on the origin
	//  won't work if there are line elements
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


	
	
	private static final Pattern SCALE_PATTERN = Pattern.compile("scale\\(([\\d\\.-]+),([\\d\\.-]+)\\)");

	/**
	 * for any text element in top that has a transform with scale, apply the transform directly
	 * to the x and y attributes of the text element and any of its children (such as tspan)
	 * @param top  the element at the top of the scope of the action
	 */
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

}
