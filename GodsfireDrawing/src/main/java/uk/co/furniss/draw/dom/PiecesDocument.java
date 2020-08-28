package uk.co.furniss.draw.dom;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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
	
	public Element addCloneOfTemplate(Element layer, String name, float dx, float dy) {
		Element clone = svg.createTranslatedClone(name + TEMPLATE_SUFFIX, dx, dy);
		layer.appendChild(clone);
		return clone;
	}

}
