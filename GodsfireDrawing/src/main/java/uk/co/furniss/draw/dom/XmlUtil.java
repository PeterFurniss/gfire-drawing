package uk.co.furniss.draw.dom;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class XmlUtil {

	
	private XmlUtil() {
		// utility class
	}
	
	private static ThreadLocal<DocumentBuilder> docBuilders = new ThreadLocal<DocumentBuilder>() {
		@Override
		protected DocumentBuilder initialValue() {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);

				try {
					return factory.newDocumentBuilder();
				} catch (ParserConfigurationException e) {
					throw new IllegalStateException("Failed to create DocumentBuilder");
				}
		}
	};


	

	

	public static Element deserialiseXmlFile(String filePath) {
		File xmlFile = new File(filePath);
		return deserialiseXmlFile(xmlFile);
	}
	
	public static Element deserialiseXmlFile(File xmlFile) {
		FileInputStream instream;
		try {
			instream = new FileInputStream(xmlFile);
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("Failed to find file " + xmlFile.getAbsolutePath(), e);
		}
		return deserialiseXmlStream(instream);
	}

	private static Element deserialiseXmlStream(FileInputStream instream) {
		Element docElement;
		try {
			docElement = docBuilders.get().parse(instream).getDocumentElement();
		} catch (SAXException | IOException e) {
			throw new IllegalStateException("Failed to parse xml stream", e);
		}
		docElement.normalize();
		removeEmptyTextNodes(docElement);
		return docElement;
	}

	private static void removeEmptyTextNodes(Element docElement) {
		List<Node> textNodes = XPathUtil.getSVG().findNodes(docElement, "//text()");
		for (Node textNode : textNodes) {
			if (textNode.getNodeValue().matches("(?s)\\s*")) {
				textNode.getParentNode().removeChild(textNode);
			}
		}
	}
	
	public static String serialiseXml(Element topElement, boolean indent) {
		TransformerFactory factory = TransformerFactory.newInstance();
		try {
			Transformer t = factory.newTransformer();
			if (indent) {
				t.setOutputProperty(OutputKeys.INDENT, "yes");
			}
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			DOMSource s = new DOMSource(topElement);
			StreamResult r = new StreamResult(new StringWriter());
			t.transform(s, r);
			String answer = r.getWriter().toString();
			if (indent) {
				return answer;
			} else {
				// ensure truly one line
				return answer; //.replaceAll("\n", "&x0A;");
			}
		} catch (IllegalArgumentException | TransformerException e) {
			throw new IllegalStateException("Failed to serialise xml", e);
		}
	}
	
}
