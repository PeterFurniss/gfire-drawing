package uk.co.furniss.draw.dom;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import uk.co.furniss.draw.piecemaker.Justification;

/**
 * manipulation class for an <svg:text> element.
 * 		only used (currently) for attribute and position interrogation.
 * 		inkscape creates tspan elements inside the text, with the same attributes
 * 		content will be in the tspan element
 */
public class SvgText extends SvgObject {
	
	private  float x;
	private  float y;
	private  Map<String, String> styling = new HashMap<>();
	private  String style;
	private  String content;
	private  Orientation rotation;
	private Justification alignment;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SvgText.class.getName());
	
	public SvgText(Element textElement) {
		super(textElement);
		LOGGER.debug("text element {}", getId());
		this.x = Float.parseFloat(textElement.getAttribute("x"));
		this.y = Float.parseFloat(textElement.getAttribute("y"));
		LOGGER.debug("text element at {}, {}", x, y);
		this.content = textElement.getTextContent();
		this.style = textElement.getAttribute("style");
		LOGGER.debug("text element style {}\n                content {}", style, content);
		String transform = textElement.getAttribute("transform");
		this.rotation = Orientation.fromTransform(transform);
		Matcher mstyle = STYLE_PATTERN.matcher(style);
		while (mstyle.find()) {
			styling.put(mstyle.group(1),  mstyle.group(2));
		}
		
		Node firstChild = textElement.getFirstChild();
		if (firstChild.getNodeType() == Node.ELEMENT_NODE ) {
			Element tspanElement = (Element) firstChild;
			if (tspanElement.hasAttribute("dx") || tspanElement.hasAttribute("dy")) {
				throw new UnsupportedOperationException("Can't  cope with dx, dy on tspan for " + textElement.getAttribute("id"));
			}
			// the tspan seems to be one character before the end
//			this.x = Float.parseFloat(tspanElement.getAttribute("x"));
//			this.y = Float.parseFloat(tspanElement.getAttribute("y"));
			LOGGER.debug("tspan element at {}, {}", x, y);
			String tspanStyle = tspanElement.getAttribute("style");
			mstyle = STYLE_PATTERN.matcher(tspanStyle);
			while (mstyle.find()) {
				String key = mstyle.group(1);
				String value = mstyle.group(2);
				if (! value.equals(styling.get(key)) && styling.get(key) != null  && ! key.contains("inkscape")) {
					LOGGER.info("tspan styling for {} differs - text {}, tspan " + value, key, styling.get(key));
					styling.put(key,  value);

				}
			}
			
		}
		alignment = Justification.fromAlignment(styling.get("text-align"));
		LOGGER.debug("styling {}", styling);
	}

	// clone constructor
	private SvgText(SvgText original, String newId) {
		super(original.element, newId, true);
		this.x = original.x;
		this.y = original.y;
		this.style = original.style;
		this.content = original.content;
		this.styling = new HashMap<>(original.styling);
	}

	private static final Pattern FONTSIZE_PATTERN = Pattern.compile("([\\d\\.]+)(.*)");
	
	public float getFontSizePx() {
		String fontsizeString = styling.get("font-size");
		Matcher mFS = FONTSIZE_PATTERN.matcher(fontsizeString);
		if (mFS.matches()) {
			if (mFS.group(2).equals("px")) {
				return Float.parseFloat(mFS.group(1));
			}
			LOGGER.debug("matching {} gives units {}", fontsizeString, mFS.group(2));
			throw new IllegalStateException("Can't determine fontsize in px from " + fontsizeString);
		}
		throw new IllegalStateException("Failed to match " + fontsizeString);
	}
	
	
	@Override
	public void transform( Trans trans ) {
		XYcoords newXY = trans.apply(getTopLeft());
		setXY(newXY);
		scale(trans);
	}

	@Override
	public void scale(Trans trans) {
		float fontSize = getFontSizePx();
		String newFontSize = Float.toString(trans.scale(fontSize)) + "px";
		styling.put("font-size",  newFontSize);
		
		setStyling();
	}
	
	@Override
	public void scaleTo( XYcoords base, Trans trans ) {
		XYcoords newXY = trans.scaleTo(base, getTopLeft());
		setXY(newXY);
		scale(trans);
	}

	@Override
	public void translate(Trans trans) {
		XYcoords newXY = trans.translate(getTopLeft());
		setXY(newXY);
	}

	public void setXY( XYcoords newXY ) {
		x = newXY.getX();
		y = newXY.getY();
		setXY();
	}
	
	@Override
	public SvgObject clone(String newId ) {
		return new SvgText(this, newId);
	}

	@Override
	public XYcoords getStart() {
		return getTopLeft();
	}

	@Override
	public XYcoords getTopLeft() {
		float heightMm = convertPxToMm(getFontSizePx());
		return new XYcoords(x, y - heightMm);
	}

	@Override
	public XYcoords getBottomRight() {
//		float heightMm = convertPxToMm(getFontSizePx());

		// don't increment x by 1
		return new XYcoords(x , y);
	}


	public void setXY() {
		element.setAttribute("x", Float.toString(x));
		element.setAttribute("y", Float.toString(y));
	}

	public void setStyling() {
		style = styling.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue()).collect(Collectors.joining(";"));
		
		element.setAttribute("style", style);
	}

	
	@Override
	public void move( XYcoords movement ) {
		 x += movement.getX();
		 y += movement.getY();
		 setXY();

	}

	public String getFontFamily() {
		return styling.get("font-family");
	}

	public String getFontMod() {
		return styling.get("font-style") + "/" + styling.get("font-weight");
	}

	public Justification getAlignment() {
		return alignment;
	}
	
	public Orientation getRotation() {
		return rotation;
	}

}
