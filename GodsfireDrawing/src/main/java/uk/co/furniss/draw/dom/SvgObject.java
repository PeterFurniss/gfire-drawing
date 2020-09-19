package uk.co.furniss.draw.dom;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;


public abstract class SvgObject {

	private static final XPathUtil XPU = XPathUtil.getSVG();
	
	// the xml as read (and possibly modified)
	protected final Element element;

	private final String id;	
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SvgObject.class.getName());
	protected static final Pattern STYLE_PATTERN = Pattern.compile("([\\w-]+):([^;]+)+(?:;|$)");

	public static SvgObject makeSvgObject(Element element) {
		String type = element.getLocalName();
		switch (type) {
		case "path":
			return new SvgPath(element);
		case "g":
			return new SvgGroup(element);
		case "circle":
			return new SvgCircle(element);
		case "rect":
			return new SvgRect(element);
		case "text":
			return new SvgText(element);
		default:
			throw new UnsupportedOperationException("Can't handle " + type);
		}
	}
	
	/**
	 * apply transformation to the object's coordinates, based on the origin.
	 * @return
	 */
	public final boolean internaliseTransformation() {
		return internaliseTransformation(null);
	}
	
	/**
	 * apply transformation to the object's coordinates, based on the given position.
	 * Used when transforming within a group, so the group shape stays the same.
	 * @param base
	 * @return
	 */
	public boolean internaliseTransformation(XYcoords base) {
		String transformAttr = element.getAttribute("transform");
		if (transformAttr.length() > 0) {
			LOGGER.debug("transform on a {} is {}", element.getLocalName(), transformAttr);
			Transform trans = new Transform(transformAttr);
			if (trans.isInternalising()) {
				element.removeAttribute("transform");
				if (trans.isTranslating()) {
					if (trans.isScaling()) {
						applyTransform(base, trans);
					} else {
						translate(trans);
					}
				} else {
					scaleTo(base, trans);
				}
				return true;
			}
		}
		return false;
	}

	public float convertPxToMm(float px) {
		return px/96.0f * 25.4f;
	}
	
	public SvgObject (Element originalElement) {
		this.element = originalElement;
		this.id = element.getAttribute("id");

	}
	
	/**
	 * make a copy of the element, with a new id
	 * @param deep TODO
	 * @return
	 */
	protected SvgObject(Element original, String newId, boolean deep) {
		this.element = (Element) original.cloneNode(deep);
		element.setAttribute("id", newId);
		this.id = newId;
	}

	public abstract SvgObject clone(String newId);
	/**
	 * get the absolute topLeft
	 * @return
	 */
	public abstract XYcoords getTopLeft();
	public abstract XYcoords getBottomRight();
	public abstract XYcoords getStart();

	
	public XYcoords getCentre() {
		XYcoords tl = getTopLeft();
		XYcoords br = getBottomRight();
		XYcoords centre = tl.meanWith(br);
		LOGGER.debug(" centre of {} is {}", id, centre);
		LOGGER.debug("          tl was {}, br was {}", tl, br);
		return centre;
	}
	
	public void setCentre(XYcoords absoluteCentre) {
		element.removeAttribute("transform");
		XYcoords movement = absoluteCentre.subtract(getCentre());
		move(movement);
	}

	public abstract void move(XYcoords movement);
	public abstract void scale(Transform trans);
	public abstract void scaleTo(XYcoords base, Transform trans);
	public abstract void translate(Transform trans);
	
	public final void applyTransform( XYcoords base, Transform trans ) {
		LOGGER.debug("applyTransform to {} with base {}", id, base);

		if (trans.isScaling()) {
			scaleTo(base, trans);
		}
		translate(trans);
	}
	
	public Element getElement() {
		return element;
	}

	public String getId() {
		return id;
	}

	public void openStyle() {
		if (element.getAttribute("style").length() > 0) {
			element.setAttribute("style", "fill-rule:evenodd");
		}
		
	}
	
	public String getTransformAttribute() {
		return element.getAttribute("transform");
	}
}
