package uk.co.furniss.draw.dom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XPathUtil {

	public static final String SVG_NS = "http://www.w3.org/2000/svg";
	public static final String XLINK_NS = "http://www.w3.org/1999/xlink";


	private static ThreadLocal<Map<String, XPathExpression>> expressionsMap = new ThreadLocal<Map<String, XPathExpression>>() {

		@Override
		protected Map<String, XPathExpression> initialValue() {
			return new HashMap<>();
		}
	};
	
	private static final XPathUtil SVG = new XPathUtil(SVG_NS);


	private final XPath xpath;
	
	public static XPathUtil getSVG() {
		return SVG;
	}
	
	private XPathUtil() {
		XPathFactory xpf = new net.sf.saxon.xpath.XPathFactoryImpl();
		xpath = xpf.newXPath();
//		nsContext = new NamespaceContextImpl();
//		xpath.setNamespaceContext(nsContext);
	}
	
	public XPathUtil(String nameSpace) {
		this();
		((net.sf.saxon.xpath.XPathEvaluator) xpath).getStaticContext().setDefaultElementNamespace(nameSpace);
	}
	
	private XPathExpression getXPE(String xpString) {
		Map<String, XPathExpression> expressions = expressionsMap.get();
		XPathExpression xpe = expressions.get(xpString);
		if (xpe == null) {
			try {
				xpe = xpath.compile(xpString);
			} catch (XPathExpressionException e) {
				throw new IllegalArgumentException("Failed to compile " + xpString, e);
			}
			expressions.put(xpString, xpe);
		}
		return xpe;
	}
	
	public List<Element> findElements(Element context, String xpString) {
		XPathExpression xpe = getXPE(xpString);

		try {
			NodeList nodes = (NodeList) xpe.evaluate(context, XPathConstants.NODESET);
			List<Element> answer = new ArrayList<>(nodes.getLength());
			for (int i=0; i < nodes.getLength(); i++) {
				answer.add((Element) nodes.item(i));
			}
			return answer;
		} catch (XPathExpressionException e) {
			throw new IllegalStateException("Failed to evaluate " + xpString);
		}
	}
	
	public Element findElement(Element context, String xpString) {
		XPathExpression xpe = getXPE(xpString);
		try {
			return (Element) xpe.evaluate(context, XPathConstants.NODE);
			
		} catch (XPathExpressionException e) {
			throw new IllegalStateException("Failed to evaluate " + xpString);
		}
	}
	
	public String findString(Element context, String xpString) {
		XPathExpression xpe = getXPE(xpString);
		try {
			return (String) xpe.evaluate(context, XPathConstants.STRING);
			
		} catch (XPathExpressionException e) {
			throw new IllegalStateException("Failed to evaluate " + xpString);
		}

	}
	
	public List<Node> findNodes(Element context, String xpString) {
		XPathExpression xpe = getXPE(xpString);

		try {
			NodeList nodes = (NodeList) xpe.evaluate(context, XPathConstants.NODESET);
			List<Node> answer = new ArrayList<>(nodes.getLength());
			for (int i=0; i < nodes.getLength(); i++) {
				answer.add(nodes.item(i));
			}
			return answer;
		} catch (XPathExpressionException e) {
			throw new IllegalStateException("Failed to evaluate " + xpString);
		}
	}
}
