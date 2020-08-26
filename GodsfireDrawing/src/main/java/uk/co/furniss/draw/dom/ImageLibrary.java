package uk.co.furniss.draw.dom;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

/**
 * loads a load of objects from an svg file allowing them to be used in a definition section
 * 
 *    approach needs changing, cos you can't transfer xml elements between documents -
 *    so the output doc needs to be a mangle of the input.  Or do some juggling
 */
public class ImageLibrary {

	private final Map<String, SvgObject> images = new HashMap<>();
	private final SVGbuilder svg;
	private final Element svgDoc;
	
	public ImageLibrary(String inputFile) {
		
        svgDoc = XmlUtil.deserialiseXmlFile(inputFile);

        svg = new SVGbuilder(svgDoc);
        
        svg.ensureNamespace("xlink", XPathUtil.XLINK_NS, svgDoc);

	}

	public SvgObject findSvgObject(String name) {
		if (images.containsKey(name)) {
			return images.get(name);
		}
		SvgObject obj = SVGmangler.getSvgObject(svgDoc, name);
		images.put(name,  obj);
		return obj;
	}

	Collection<SvgObject> knownObjects() {
		return images.values();
	}
}
