package uk.co.furniss.draw;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.File;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.List;

import javax.swing.JFrame;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.batik.dom.GenericDOMImplementation;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.kitfox.svg.Group;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGElement;
import com.kitfox.svg.SVGElementException;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.SVGUniverse;
import com.kitfox.svg.Use;
import com.kitfox.svg.animation.AnimationElement;

public class SalamanderOne extends JFrame{
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 1500;

	SVGDiagram diagram;

		
	SalamanderOne(String title, SVGDiagram diagram) {
		        super(title);
		        this.diagram = diagram;
		        setVisible(true);
		        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		        setSize(WIDTH, HEIGHT);
			}
		
	    @Override
		public void paint(Graphics g) {
	        Graphics2D g2 = (Graphics2D) g;
	        
	        try {
				diagram.render(g2);
			} catch (SVGException e) {
				throw new IllegalStateException("Failed to render", e);
			};
	       
	    }	

	public static void main(String[] args) {
		String fileName = "gf_redA";
		String testFilePath = "c:/temp/pix/" + fileName + ".svg";
		
		File testFile = new File(testFilePath);
		if (! testFile.exists()) {
			throw new IllegalArgumentException("No file " + testFilePath);
		}

		SVGUniverse svgUniverse = new SVGUniverse();

			try {
				SVGDiagram diagram = svgUniverse.getDiagram(svgUniverse.loadSVG(testFile.toURI().toURL()));
				SalamanderOne frame = new SalamanderOne(fileName, diagram);
				
				listChildren(diagram.getRoot(), "");
				
				addUse(diagram.getRoot(), "g8006", 180.0f, 0.0f);
				diagram.updateTime(1.0);
				
				Document xml = makeDocument(diagram);
				System.out.println(writeDocument(xml));
				
				
			} catch (MalformedURLException e) {
				throw new IllegalStateException("Can't make file to url", e);
			} catch (SVGException e) {
				throw new IllegalStateException("Failed to update", e);
			}

	}

	private static void listChildren(SVGElement parent, String indent) {
		System.out.println(indent  + parent.getClass().getSimpleName() + "  " + parent.getId() );
		@SuppressWarnings("unchecked")
		List<SVGElement> children = parent.getChildren(null);
		for (SVGElement child : children) {
			listChildren(child, indent + "    ");
		}
		
	}
	
	
	private static void addUse(Group parent, String id, float dx, float dy) {
		Use clone = new Use();
		try {
			clone.addAttribute("xlink:href", AnimationElement.AT_XML, "#" + id);
			clone.addAttribute("transform", AnimationElement.AT_XML, "translate(" + Float.toString(dx) + "," + Float.toString(dy)+ ")");
			parent.loaderAddChild(null, clone);
		} catch (SVGElementException e) {
			throw new IllegalStateException("Failed to add use", e);
		}
	}

	private static Document makeDocument(SVGDiagram diagram) {
		DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();

		// Create an instance of org.w3c.dom.Document.
		String svgNS = "http://www.w3.org/2000/svg";
		Document document = domImpl.createDocument(svgNS, "svg", null);
		addElement(document.getDocumentElement(), diagram.getRoot(), document);
		return document;
	}
	
	@SuppressWarnings("unchecked")
	private static void addElement(Element domParent, SVGElement  svgElement, Document document) {
//		Set<String> cssAttributes = svgElement.getInlineAttributes();
//		for (String attrName : cssAttributes) {
//			domParent.setAttribute(attrName, svgElement.getStyle(attrib)ASvalue);
//		}
//		Set presAttributes = svgElement.getPresentationAttributes();
		List<SVGElement> children = svgElement.getChildren(null);
		for (SVGElement child : children) {
			Element newChild = document.createElementNS(document.getNamespaceURI(), child.getClass().getSimpleName());
			domParent.appendChild(newChild );
			addElement(newChild, child, document);
		}

	}
	
	private static String writeDocument(Document doc) {
		try {
		    Transformer transformer = TransformerFactory.newInstance().newTransformer();
		    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		    StreamResult result = new StreamResult(new StringWriter());
		    DOMSource source = new DOMSource(doc);
		    transformer.transform(source, result);
		    return result.getWriter().toString();
		} catch(TransformerException ex) {
		    ex.printStackTrace();
		    return null;
		}
	}
}
