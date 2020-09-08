package uk.co.furniss.draw.dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;


public class SvgGroup extends SvgObject {

	private static final XPathUtil XPU = XPathUtil.getSVG();
	
	private final List<SvgObject> children = new ArrayList<>();;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SvgGroup.class.getName());
	
	public SvgGroup (Element originalElement) {
		super(originalElement);
		List<Element> childElements = XPU.findElements(originalElement, "./*");
		for (Element child : childElements) {
			children.add(SvgObject.makeSvgObject(child));
		}
	}
	
	private SvgGroup(Element element, String newId) {
		super(element, newId, false);
	}
	
	private void addChild(SvgObject newChild) {
		children.add(newChild);
		getElement().appendChild(newChild.getElement());
	}

	@Override
	public SvgGroup clone(String newId) {
		SvgGroup cloneObject = new SvgGroup(getElement(), newId);
		
		for (SvgObject oldChild : children) {
			cloneObject.addChild(oldChild.clone(newId + "_" + oldChild.getId()));
		}
		return cloneObject;
	}
	
	
	@Override
	public XYcoords getTopLeft() {
		
		float leftmostX = 100000.0f;
		float topmostY  = 100000.0f;

   		for (SvgObject child : children) {
   			XYcoords tl = child.getTopLeft();
   			LOGGER.debug("for child {} centre will be {}", child.getId(), tl.meanWith(child.getBottomRight()));
   			if (tl.getX() < leftmostX ) {
   				leftmostX = tl.getX();
   			}
   			if (tl.getY() < topmostY) {
   				topmostY = tl.getY();
   			}
		}
   		XYcoords topLeft = new XYcoords(leftmostX, topmostY);
   		LOGGER.debug("for group {}, tl is {}", getId(), topLeft);
		return topLeft;
	}

	@Override
	public XYcoords getBottomRight() {
		
		float rightmostX = -100000.0f;
		float bottommostY  = -100000.0f;

   		for (SvgObject child : children) {
   			XYcoords br = child.getBottomRight();
   			if (br.getX() > rightmostX ) {
   				rightmostX = br.getX();
   			}
   			if (br.getY() > bottommostY) {
   				bottommostY = br.getY();
   			}
		}
   		XYcoords bottomRight = new XYcoords(rightmostX, bottommostY);
   		LOGGER.debug("for group {}, br is {}", getId(), bottomRight);

		return bottomRight;
	}
	

	@Override
	public void move( XYcoords movement ) {
		for (SvgObject child : children) {
			child.move(movement);
		}
	}

	
	@Override
	public boolean internaliseTransformation(XYcoords base) {
		// first apply transformations of children
		XYcoords groupBase = base != null ? base : getCentre();
		boolean changes = false;
		for (SvgObject child : children) {
			if (child.internaliseTransformation(groupBase)) {
				LOGGER.debug("made transformation changes to {}'s child {}", getId(), child.getId());
				changes = true;
			}
		}
		//then apply our own (if this can be) 
		if (super.internaliseTransformation(groupBase)) {
			LOGGER.debug("made group transformation changes to  {}", getId());
			changes = true;
		}
		return changes;
	}

//	@Override
//	public void applyTransform(XYcoords base, Transform trans) {
//		for (SvgObject child : children) {
//			child.applyTransform(base, trans);
//		}
//	}

	@Override
	public void scale(Transform trans) {
		XYcoords centre = getCentre();
		for (SvgObject child : children) {
			child.scaleTo(centre, trans);
		}
	}
	
	@Override
	public void scaleTo(XYcoords base, Transform trans) {
		for (SvgObject child : children) {
			child.scaleTo(base, trans);
		}
	}
	
	
	@Override
	public void translate(Transform trans) {
		for (SvgObject child : children) {
			child.translate(trans);
		}
	}
	
	@Override
	public void openStyle() {
		super.openStyle();
		for (SvgObject child : children) {
			child.openStyle();
		}
	}
	


}
