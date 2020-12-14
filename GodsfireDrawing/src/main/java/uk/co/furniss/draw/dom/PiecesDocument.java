package uk.co.furniss.draw.dom;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import uk.co.furniss.draw.piecemaker.Justification;

/**
 * the xml document that starts as the image library (possibly containing other layers)
 * and ends as output document - or has a layer for that
 * 
 *    approach needs changing, cos you can't transfer xml elements between documents -
 *    so the output doc needs to be a mangle of the input.  Or do some juggling
 */
public class PiecesDocument {

	private final Map<String, SvgObject> knownObjects = new HashMap<>();
	private final Map<String, SvgObject> defObjects = new HashMap<>();
	private final SVGbuilder svg;
	private final Element svgDoc;
	private Element libraryLayer;
	private Element defs;
	private static final XPathUtil XPU = XPathUtil.getSVG();
	public static final String TEMPLATE_SUFFIX = "_template";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(PiecesDocument.class.getName());

	public PiecesDocument(String inputFile) {
		
        svgDoc = XmlUtil.deserialiseXmlFile(inputFile);

        svg = new SVGbuilder(svgDoc);
        
        // I can't remember why this was done
        svg.ensureNamespace("xlink", XPathUtil.XLINK_NS, svgDoc);
        
        // by default,lookup in top level
        libraryLayer = svgDoc;
        
        defs = XPU.findElement(svgDoc,  "defs");
        if (defs == null) {
        	throw new IllegalStateException("Can't find a defs element");
        }

	}
	
	public void setLibraryLayer(String layerName) {
		libraryLayer = SVGmangler.getLayerElement(svgDoc, layerName);
		if (libraryLayer == null) {
			throw new IllegalArgumentException("No layer " + layerName);
		}
	}

	public String ensureTemplate(String name, boolean retainColour) {
		String templateName = name + TEMPLATE_SUFFIX;
		if (! defObjects.containsKey(templateName)) {
			LOGGER.debug("creating template object for {}", name);
			
    		SvgObject obj = knownObjects.get(name);
    		if (obj == null) {
    			obj = findSvgObject(name);
    		}
    		if (obj == null) {
    			throw new IllegalArgumentException("Cannot find object " + name + " in image file");
    		}
    
    		SvgObject templateObject = obj.clone(templateName );
    		LOGGER.debug("templating {} as {}", name, templateName);
//    		templateObject.internaliseTransformation();
    		
    		templateObject.setCentre(XYcoords.ORIGIN);
    		if (! retainColour) {
    			templateObject.openStyle();
    		}
    		addDefObject(templateObject);
		}
		return templateName;
	}
	
	
	public SvgObject findSvgObject(String name) {
		if (knownObjects.containsKey(name)) {
			return knownObjects.get(name);
		}
		SvgObject obj = SVGmangler.getSvgObject(libraryLayer, name);
		obj.internaliseTransformation();
		knownObjects.put(name,  obj);
		return obj;
	}

	public Collection<SvgObject> knownObjects() {
		return knownObjects.values();
	}
	
	public List<String> getLayerNames() {
		return SVGmangler.getLayerNames(svgDoc); 
	}

	public void writeToFile(String outFile) {
       //  assuming i don't want this
		// svg.roundNumbers();
       
       svg.writeToFile(outFile);
	}
	
	public void addDefObject(SvgObject object) {
		defs.appendChild(object.getElement());
		
	}

	public Element obtainEmptyLayer( String layerName ) {
		Element layer = SVGmangler.getLayerElement(svgDoc, layerName);
		if (layer != null) {
			// take out all the old child elements
			while (layer.hasChildNodes()) {
				layer.removeChild(layer.getFirstChild());
			}
		} else {
			layer = svg.createElement("g");
			svgDoc.appendChild(layer);
			layer.setAttribute("inkscape:groupmode", "layer");
			layer.setAttribute("inkscape:label", layerName);
			layer.setAttribute("id", "layer_" + layerName);
			layer.setAttribute("style", "display:inline");
			
		}
		return layer;
	}
	
	public void hideAllLayersButOne(String layerName) {
		List<Element> layers = SVGmangler.getLayerElements(svgDoc);
		for (Element layer : layers) {
			final String displayStyle;
			if (layer.getAttribute("inkscape:label").equals(layerName)) {
				displayStyle = "display:inline";
			} else {
				displayStyle = "display:none";
			}
			layer.setAttribute("style", displayStyle);
		}
	}
	
	public Element addCloneOfTemplate(Element layer, String templateName, float dx, float dy) {
		Element clone = svg.createTranslatedClone(templateName, dx, dy);
		layer.appendChild(clone);
		return clone;
	}

	public SvgRect makeRectangle(Element parentElement, float x, float y, float width, float height, String colour) {
		Element r = svg.createElement("rect");
		r.setAttribute("x", Float.toString(x));
		r.setAttribute("y", Float.toString(y));
		r.setAttribute("width", Float.toString(width));
		r.setAttribute("height", Float.toString(height));
		r.setAttribute("fill", colour);
		parentElement.appendChild(r);
		return new SvgRect(r);
	}
	
	public void drawLine( Element outputLayer, XYcoords start, XYcoords end ) {
		Element line = svg.createElement("line");
		line.setAttribute("x1", Float.toString(start.getX()));
		line.setAttribute("x2", Float.toString(end.getX()));
		line.setAttribute("y1", Float.toString(start.getY()));
		line.setAttribute("y2", Float.toString(end.getY()));
		line.setAttribute("stroke", "grey");
		line.setAttribute("stroke-width", "0.1");
		outputLayer.appendChild(line);
	}
	private static Pattern ASTER_PATTERN = Pattern.compile("(.*)aster(.*)");
	// escape pattern applies only to alpha-numerics immediately after an escape marker  (for now)
	private static Pattern ESCAPE_PATTERN = Pattern.compile("(.*?)\\\\(\\w)(\\w*)(.*)");

	public void addText( Element parent, String text, float size, XYcoords xy, String mods, String colour, Justification justification, String transform ) {
		Element textElement = svg.createElement("text");
		textElement.setAttribute("font-size", Float.toString(size) + "px");
		textElement.setAttribute("fill", colour);
		if (! transform.equals("")) {
			// transform will (invariably ?) the direction of the writing
			textElement.setAttribute("transform", transform);
		}
		if (text.equals("star")) {
    		textElement.appendChild(svg.createText("«"));
    		textElement.setAttribute("font-family", "wingdings");
		} else if (text.equals("blob")) {
    		textElement.appendChild(svg.createText("="));
    		textElement.setAttribute("font-family", "webdings");
		} else if (text.contains("aster")) {
			// wbrm star often follows a number
			Matcher asterMatch = ASTER_PATTERN.matcher(text);
			if (asterMatch.matches()) {
				String before = asterMatch.group(1);
				String after = asterMatch.group(2);
				Float dx = 0.0f;
				if (before.length() > 0) {
					Element tspanBefore = svg.createElement("tspan");
					textElement.appendChild(tspanBefore);
					tspanBefore.appendChild(svg.createText(before));
					int padding = before.length() - before.trim().length();
					if (padding > 0) {
						dx += padding * size * 0.65f ;
					}
				}
				Element tspanAster = svg.createElement("tspan");
				textElement.appendChild(tspanAster);
				if (dx > 0) {
					tspanAster.setAttribute("dx", Float.toString(dx));
				}
				tspanAster.setAttribute("font-family",  "wingdings 2");
				tspanAster.appendChild(svg.createText("Ý"));
				if (after.length() > 0) {
					Element tspanafter = svg.createElement("tspan");
					textElement.appendChild(tspanafter);
					tspanafter.appendChild(svg.createText(after));
					dx += size * after.length();
				}
			}
		} else if (text.contains("\\")) {
			// special modifier - underline for now, may be bold, italic etc later
			Matcher escapeMatch = ESCAPE_PATTERN.matcher(text);
			if (escapeMatch.matches()) {
				String before = escapeMatch.group(1);
				String control = escapeMatch.group(2);
				String subject = escapeMatch.group(3);
				String after = escapeMatch.group(4);
				if (subject.equals("")) {
					// mod on a non-alphanumeric
					subject = after;
					after = "";
				}
				Float dx = 0.0f;
				if (before.length() > 0) {
					Element tspanBefore = svg.createElement("tspan");
					textElement.appendChild(tspanBefore);
					tspanBefore.appendChild(svg.createText(before));
					// doesn't move along if blank
					int padding = before.length() - before.trim().length();
					if (padding > 0) {
						// guesstimated number
						dx += padding * size * 0.65f ;
					}
				}

				Element tspanSubject = svg.createElement("tspan");
				textElement.appendChild(tspanSubject);
				if (dx > 0) {
					tspanSubject.setAttribute("dx", Float.toString(dx));
				}
				final boolean useText;
				switch (control) {
				case "u":
					tspanSubject.setAttribute("text-decoration", "underline");
					useText = true;
					break;
				case "b":
					tspanSubject.setAttribute("font-weight", "bold");
					useText = true;
					break;
				case "i":
					tspanSubject.setAttribute("font-style", "italic");
					useText = true;
					break;
				case "C":   // cyclic magician
					useText = false;
					tspanSubject.appendChild(svg.createText("\u2b81"));
					break;
				case "U":   // unicode character - upper case to distinguish from underline
					useText = false;
					
					try {
						long uc = Long.decode("0x" + subject);
						ByteBuffer b = ByteBuffer.allocate(Long.BYTES);
						b.putLong(uc);
						String uchar = new String(b.array(), "UTF-16");
						tspanSubject.appendChild(svg.createText(uchar));
					} catch (UnsupportedEncodingException e) {
						LOGGER.warn("Failed to handle unicode " + text);
					}

					break;
				default:
					LOGGER.warn("Unrecognised escape sequence {} in {}",control, text);
					useText = true;
					break;
				}
				if (useText) {
					tspanSubject.appendChild(svg.createText(subject));
				}
				if (after.length() > 0) {
					Element tspanafter = svg.createElement("tspan");
					textElement.appendChild(tspanafter);
					tspanafter.appendChild(svg.createText(after));
					dx += size * after.length();
				}
			}

    	} else {
    		textElement.appendChild(svg.createText(text));
    		// these mods are set for the text field - i.e. defined in the example svg
    		if (mods.contains("bold")) {
    			textElement.setAttribute("font-weight", "bold");
    		}
    		if (mods.contains("italic")) {
    			textElement.setAttribute("font-style", "italic");
    		}
    	}

		textElement.setAttribute("text-anchor", justification.getAnchor());
		textElement.setAttribute("text-align", justification.getAlign());
		textElement.setAttribute("dominant-baseline", "baseline");
		textElement.setAttribute("x", Float.toString(xy.getX()));
		textElement.setAttribute("y", Float.toString(xy.getY()));

		parent.appendChild(textElement);
		
	}
	
}
