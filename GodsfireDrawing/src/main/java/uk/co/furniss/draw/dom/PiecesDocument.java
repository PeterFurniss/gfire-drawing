package uk.co.furniss.draw.dom;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 * the xml document that starts as the image library (possibly containing other layers)
 * and ends as output document - or has a layer for that
 * 
 *    approach needs changing, cos you can't transfer xml elements between documents -
 *    so the output doc needs to be a mangle of the input.  Or do some juggling
 */
public class PiecesDocument {

	private final Map<String, SvgObject> defObjects = new HashMap<>();
	private final SVGbuilder svg;
	private final Element svgDoc;
	private Element libraryLayer;
	private Element defs;
	private static final XPathUtil XPU = XPathUtil.getSVG();
	public static final String TEMPLATE_SUFFIX = "_template";


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

	public String ensureTemplate(String name) {
		String templateName = name + TEMPLATE_SUFFIX;
		if (! defObjects.containsKey(name)) {
	
    		SvgObject obj = SVGmangler.getSvgObject(libraryLayer, name);
    		if (obj == null) {
    			throw new IllegalArgumentException("Cannot find object " + name + " in image file");
    		}
    
    		SvgObject template = obj.clone(templateName );
    		template.setCentre(XYcoords.ORIGIN);
    		template.openStyle();
    		addDefObject(template);
    		defObjects.put(name, template);
		}
		return templateName;
	}
	
	public SvgObject findSvgObject(String name) {
		if (defObjects.containsKey(name)) {
			return defObjects.get(name);
		}
		SvgObject obj = SVGmangler.getSvgObject(libraryLayer, name);
		defObjects.put(name,  obj);
		return obj;
	}

	public Collection<SvgObject> knownObjects() {
		return defObjects.values();
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
		line.setAttribute("stroke", "black");
		line.setAttribute("stroke-width", "0.1");
		outputLayer.appendChild(line);
	}

	public void addText( Element parent, String text, float size, float  x, float y, boolean bold, String colour ) {
		Element textElement = svg.createElement("text");
		textElement.setAttribute("font-size", Float.toString(size) + "px");
		textElement.setAttribute("fill", colour);
		if (text.equals("star")) {
    		textElement.appendChild(svg.createText("«"));
    		textElement.setAttribute("font-family", "wingdings");
		} else if (text.equals("blob")) {
    		textElement.appendChild(svg.createText("="));
    		textElement.setAttribute("font-family", "webdings");
    	} else {
    		textElement.appendChild(svg.createText(text));
    		if (bold) {
    			textElement.setAttribute("font-weight", "bold");
    		}
    	}
		// x, y apply to the centre of the text
		textElement.setAttribute("text-anchor", "middle");
		textElement.setAttribute("dominant-baseline", "middle");
		textElement.setAttribute("x", Float.toString(x));
		textElement.setAttribute("y", Float.toString(y));

		parent.appendChild(textElement);
		
	}
	

	
}
