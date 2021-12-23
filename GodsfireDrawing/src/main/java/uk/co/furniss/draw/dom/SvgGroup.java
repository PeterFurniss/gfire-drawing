package uk.co.furniss.draw.dom;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;


class SvgGroup extends SvgObject {

	private static final XPathUtil XPU = XPathUtil.getSVG();
	
	private final List<SvgObject> children = new ArrayList<>();;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SvgGroup.class.getName());
	
	private XYcoords definedCentre = null;

	public SvgGroup (Element originalElement) {
		super(originalElement);
		String xOffset, yOffset;
		xOffset = element.getAttribute("inkscape:transform-center-x");
		yOffset = element.getAttribute("inkscape:transform-center-y");
		if (xOffset.length() > 0) {
			if (yOffset.length() > 0) {
				definedCentre = new XYcoords(xOffset, yOffset);
			} else {
				definedCentre = new XYcoords(xOffset, "0");
			}
			LOGGER.info("defined centre is {}", definedCentre);
		} else {
			if (yOffset.length() > 0) {
				definedCentre = new XYcoords("0", yOffset);
				element.removeAttribute("inkscape:transform-center-y");
				LOGGER.info("defined centre is {}", definedCentre);
			}
		}


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
	protected boolean hasDefinedCentre() {
		return definedCentre != null;
	}

	@Override
	protected XYcoords getDefinedCentre() {
		return definedCentre;
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
	public void transform( Trans trans ) {
		for (SvgObject child : children) {
			child.transform(trans);
		}
		
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
		// what should the default base be ? origin or some point in here ?
		// or different for scale and translate ? or child and whole
		XYcoords groupBase = base != null ? base :
			XYcoords.ORIGIN;
//			children.get(0).getStart();
		// first apply transformations of children
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

	@Override
	public XYcoords getStart() {
		return children.get(0).getStart();
	}
//	@Override
//	public void applyTransform(XYcoords base, Transform trans) {
//		for (SvgObject child : children) {
//			child.applyTransform(base, trans);
//		}
//	}

	@Override
	public String toString() {
		return "group  " + getId() + " trans " + getTransformAttribute() 
			+ children.stream().map(Object::toString).collect(Collectors.joining("\n    ","\n    ","\n  end group"));
	}

	@Override
	public void scale(Trans trans) {
		XYcoords centre = getCentre();
		for (SvgObject child : children) {
			child.scaleTo(centre, trans);
		}
	}
	
	@Override
	public void scaleTo(XYcoords base, Trans trans) {
		for (SvgObject child : children) {
			child.scaleTo(base, trans);
		}
	}
	
	
	@Override
	public void translate(Trans trans) {
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
