package uk.co.furniss.draw.dom;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import uk.co.furniss.xml.svg.BaseElement;

public class SVGmangler  {
	
	private static final XPathUtil XPU = XPathUtil.getSVG();

	
	private static final Logger LOGGER = LoggerFactory.getLogger(SVGmangler.class.getName());

	static List<Element> getLayerElements(Element docElement) {
		return XPU.findElements(docElement, "//g[@inkscape:groupmode='layer']");
	}
	

	static List<String> getLayerNames(Element docElement) {
		List<Element> layerElements = getLayerElements(docElement);
		List<String> names = new ArrayList<>();
		for (Element layer : layerElements) {
			names.add(XPU.findString(layer, "@inkscape:label"));
		}
		return names;
		
	}
	

	static Element getLayerElement( Element docElement, String layerName ) {
		return XPU.findElement(docElement, "//g[@inkscape:groupmode='layer' and @inkscape:label='" + layerName + "']");
	}


	static SvgObject getSvgObject(Element topElement, String name) {
		LOGGER.debug("Looking in " + topElement.getAttribute("id") + " for " + name);
		Element xmlElement = XPU.findElement(topElement, ".//*[@id='" + name + "']");
		if (xmlElement != null) {
			return SvgObject.makeSvgObject(xmlElement);
		}
		throw new IllegalStateException("Cannot find svg object " + name + " in " + topElement.getAttribute("id") + ".");
	}

	/**
	 * convert colours defined in @style to named form
	 * @param topElement
	 */
	public static void rgbColours(Element topElement) {
        for (Element styledElement : XPU.findElements(topElement, "//*[@style]")) {
			BaseElement node = new BaseElement(styledElement);
			node.rgbColour();
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
				if (attributeName != "transform") {
    				if (normalElement || attributeName.equals("x") || attributeName.equals("y")) {
    					String attrValue = attribute.getNodeValue();
    					elementWithAttributes.setAttribute(attribute.getNodeName(),
    							attrValue.replaceAll("(\\d*\\.\\d{2})\\d+", "$1"));
    				}
				}
			}
		}
	}
	
	private static final Pattern TRANSLATE_PATTERN = Pattern.compile("translate\\(([\\d-\\.]+),([\\d-\\.]+)\\)");
	private static final Pattern XY_PATTERN = Pattern.compile("([\\d-\\.]+),([\\d-\\.]+)");
	
	// take a group, and if it has a translate transformation, apply that to the components of the group
	//   doesn't cope with nested groups, so must apply to inner most first
	public static void untranslateGroup(Element group) {
		String transform = group.getAttribute("transform");
		if (transform != null) {
			Matcher m = TRANSLATE_PATTERN.matcher(transform);
			if (m.matches()) {
				float dx = Float.parseFloat(m.group(1));
				float dy = Float.parseFloat(m.group(2));
			
				// anything with an x attribute (rect, in particular) : reset x and y 
				List<Element> xyElements = XPU.findElements(group, ".//*[@x]");
				for (Element element : xyElements) {
					float xx = Float.parseFloat(element.getAttribute("x")) + dx;
					float yy = Float.parseFloat(element.getAttribute("y")) + dy;
					element.setAttribute("x", Float.toString(xx));
					element.setAttribute("y", Float.toString(yy));
				}
				// path - modify xy pairs.  currently doesn't work for commands that don't have paired params
				List<Element> pathElements = XPU.findElements(group, ".//path");
				for (Element path : pathElements) {
					String pathString = path.getAttribute("d");
					String [] steps = pathString.split("\\s+");
					List<String> newSteps = new ArrayList<>(steps.length);
					int toMove = 1;
					for (String step : steps) {
						// look for x,y pair
						Matcher mm = XY_PATTERN.matcher(step);
						if (mm.matches() && toMove > 0) {
							// we have one and it's the first such: change the move 
							float nx = Float.parseFloat(mm.group(1)) + dx;
							float ny = Float.parseFloat(mm.group(2)) + dy;
							newSteps.add(Float.toString(nx) + "," + Float.toString(ny));
							toMove--;
						} else {
							if (step.matches("[A-Z]")) {
								// it's an absolute command, so coordinates are changed to apply the transform
								System.out.println("set toMove high on " + step + " in " + path.getAttribute("id"));
								toMove = 100;
							} else if (toMove > 1) {
								// relative command, so don't mangle things
								toMove = 0;
							}
							newSteps.add(step);
						}
						
					}
					// having mangled the node coordinates, replace them
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
