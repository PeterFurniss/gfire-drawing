package uk.co.furniss.draw.dom;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

//manages a developing svg document - sub-classes are GfMapBuilder and SVGdocument
public class SVGbuilder {

	
	protected final Element documentElement;
	protected Element appendingElement;
	Document parentDocument;
	private int layers = 0;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SVGbuilder.class.getName());
	
	public SVGbuilder(Element docElement) {
		this.documentElement = docElement;
		appendingElement = this.documentElement;
		this.parentDocument = documentElement.getOwnerDocument();
    		

	}
	
	public static void ensureNamespace( String prefix, String nameSpace, Element parent ) {
		parent.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,  "xmlns:" + prefix, nameSpace);
	}

	// get the points of the path (not necessarily the bounds in the case of curves
	public static List<XYcoords> getPathCoords( Element pathElement ) {
		String dString = pathElement.getAttribute("d");
		LOGGER.debug("hex pattern path {}", dString);
		String [] pieces = dString.split("\\s+");
		List<XYcoords> answer = new ArrayList<>(pieces.length - 2);
		// this assumes the commands and their parameters are separated by spaces (which is what
		// inkscape does. svg allows all sorts of variations
		// special case the begining
		String cmd = pieces[0];
		if (! cmd.equalsIgnoreCase("m")) {
			throw new IllegalStateException("Path starts with " + cmd + ". Can't cope");
		}
		XYcoords start = new XYcoords(pieces[1]);
		
		// need to have a back value for 
		XYcoords point = start;
		answer.add(point);
		cmd = "l";
		
		for (int i = 2; i < pieces.length ; i++) {
			// what's next ?
			String next = pieces[i];
			if (next.length() == 1) {
				cmd = next;
				i++;
			}
			switch (cmd) {
			case "m":  // relative move
				cmd = "l";  // move is only for one segment
			case "l":  // relative line
			case "t":  // shortcut quadratic
				point = new XYcoords(pieces[i]);
				break;
			case "h":  // relative horizontal
				point = new XYcoords(Float.parseFloat(pieces[i]), point.getY());
				break;
			case "v":
				point = new XYcoords(point.getX(), Float.parseFloat(pieces[i]));
				break;
			case "z":
			case "Z":
				// does nothing for the coordinates - can't guarantee it's the last, so repeat the first
				point = answer.get(0);
				break;
			case "c":  // relative bezier - 3 xy pairs, last is next node
				i += 2;
				point =  new XYcoords(pieces[i]);
				break;
			case "s":  // shortcut bezier - 2 xy pairs
			case "q":  // quadratic
				i++;
				point =  new XYcoords(pieces[i]);
				break;
				
			case "a":  // arc segment - centre (x,y) _ x-rotation _ largearcflag _ sweep_flag _ destinatioin (x,y)
				i += 4;
				point = new XYcoords(pieces[i]);
				break;
			case "H":  // absolute horizontal (why does inkscape do this ?
					// need to convert to relative
					// how far have we come (in X) from the beginning
					float dx = answer.stream().map(XYcoords::getX).reduce(0.0f, (s, x) -> s + x);
					float relX = Float.parseFloat(pieces[i]) - ( start.getX() + dx );
					LOGGER.debug("From {}  H of {}, dx seems to be " + dx + " so relX is " + relX, start, pieces[i]);
					point = new XYcoords(relX, point.getY());
					break;
			default:
				LOGGER.warn("unsupported path command {}", cmd);
				break;
			}

			answer.add(point);
		}
		return answer;
	}
	
	public void translateElement(Element target, XYcoords translation) {
		target.setAttribute("transform", "translate(" + translation + ")");
	}

	public Element makeLayer(String layerName) {
		ensureNamespace("inkscape", "http://www.inkscape.org/namespaces/inkscape", documentElement);
		Element layer = createElement("g");
		documentElement.appendChild(layer);
		layer.setAttribute("inkscape:label", layerName);
		layer.setAttribute("id",  "layer" + ++layers);
		layer.setAttribute("inkscape:groupmode",  "layer");
		// hide all layers
		layer.setAttribute("style", "display:none");
		return layer;
	}
	
	public Element makeGroup(String groupId, Element parent) {
		Element group = createElement("g");
		parent.appendChild(group);
		group.setAttribute("id", groupId);
		appendingElement = group;
		return group;
	}
	
	/**
	 * add an element as child of the document we are working on
	 * @param name   what kind of element
	 * @return
	 */
	protected Element addElement( String name ) {
		Element child = createElement(name);
		appendingElement.appendChild(child);
		return child;
	}

	Element createElement( String name ) {
		Element child = parentDocument.createElementNS(XPathUtil.SVG_NS, name);
		return child;
	}

	public void addComment( String string ) {
		Comment comment = parentDocument.createComment("\n    " + string + "\n");
		appendingElement.appendChild(comment);
	}
	
	public Text createText(String content) {
		return parentDocument.createTextNode(content);
		
	}

	protected String coordinate( float xx ) {
		return String.format("%7.2f", xx).replaceAll("\\s*", "");
	}

	private String pathStep( float x, float y ) {
		return String.format("%7.2f,%7.2f",  x, y).replaceAll("\\s*", "");
	}

	public Element addTranslatedClone( String pattern, float dx, float dy ) {
		Element use = createTranslatedClone(pattern, dx, dy);
		appendingElement.appendChild(use);
		return use;
	}

	Element createTranslatedClone( String pattern, float dx, float dy ) {
		Element use = parentDocument.createElementNS(XPathUtil.SVG_NS, "use");
		use.setAttribute("height", "100%");
		use.setAttribute("width", "100%");
		use.setAttribute("x", Float.toString(dx));
		use.setAttribute("y", Float.toString(dy));
		use.setAttributeNS(XPathUtil.XLINK_NS, "xlink:href", "#" + pattern);
		return use;
	}

	public void writeToFile( String outFile ) {
	    try(  PrintWriter out = new PrintWriter( outFile )  ){
	        out.println( XmlUtil.serialiseXml(documentElement, true) );
	        System.out.println("Wrote " + outFile);
	    } catch (FileNotFoundException e) {
			throw new IllegalArgumentException("Failed to write to " + outFile, e);
		}
	}

}