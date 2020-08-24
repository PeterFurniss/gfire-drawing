package uk.co.furniss.draw.dom;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import javax.xml.XMLConstants;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

//manages a developing svg document
public class SVGbuilder {

	
	protected final Element svgDoc;
	Document parentDocument;
	
	public SVGbuilder(Element docElement) {
		this.svgDoc = docElement;
		this.parentDocument = svgDoc.getOwnerDocument();

	}
	
	public static void ensureNamespace( String prefix, String nameSpace, Element parent ) {
		parent.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,  "xmlns:" + prefix, nameSpace);
	}

	public static float[][] getPathCoords( Element pathElement ) {
		String dString = pathElement.getAttribute("d");
		String [] pieces = dString.split("\\s+");
		float [][] answer = new float[pieces.length - 2][];
		for (int i = 1; i < pieces.length -1; i++) {
			// this doesn't cope with the various instruction in path/@ - it assumes it's all straight lines
			String [] xyString = pieces[i].split(",");
			float [] xy = new float[2];
			xy[0] = Float.parseFloat(xyString[0]);
			xy[1] = Float.parseFloat(xyString[1]);
			answer[i-1] = xy;
		}
		return answer;
	}

	/**
	 * add an element as child of the document we are working on
	 * @param name   what kind of element
	 * @return
	 */
	protected Element addElement( String name ) {
		Element child = parentDocument.createElementNS(XPathUtil.SVG_NS, name);
		svgDoc.appendChild(child);
		return child;
	}

	public void addComment( String string ) {
		Comment comment = parentDocument.createComment("\n    " + string + "\n");
		svgDoc.appendChild(comment);
	}

	protected String coordinate( float xx ) {
		return String.format("%7.2f", xx).replaceAll("\\s*", "");
	}

	private String pathStep( float x, float y ) {
		return String.format("%7.2f,%7.2f",  x, y).replaceAll("\\s*", "");
	}

	public Element addTranslatedClone( String pattern, float dx, float dy ) {
		Element use = parentDocument.createElementNS(XPathUtil.SVG_NS, "use");
		use.setAttribute("height", "100%");
		use.setAttribute("width", "100%");
		use.setAttribute("x", Float.toString(dx));
		use.setAttribute("y", Float.toString(dy));
		use.setAttributeNS(XPathUtil.XLINK_NS, "xlink:href", "#" + pattern);
		svgDoc.appendChild(use);
		return use;
	}

	public void writeToFile( String outFile ) {
	    try(  PrintWriter out = new PrintWriter( outFile )  ){
	        out.println( XmlUtil.serialiseXml(svgDoc, true) );
	        System.out.println("Wrote " + outFile);
	    } catch (FileNotFoundException e) {
			throw new IllegalArgumentException("Failed to write to " + outFile, e);
		}
	}

}