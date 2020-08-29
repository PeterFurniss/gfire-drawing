package uk.co.furniss.draw.dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.w3c.dom.Element;


public class SvgGroup extends SvgObject {

	private static final XPathUtil XPU = XPathUtil.getSVG();
	
	private final List<SvgObject> children = new ArrayList<>();;
	
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
//   			System.out.println("  child " + child.getId() + " tl = " + tl);
   			if (tl.getX() < leftmostX ) {
   				leftmostX = tl.getX();
   			}
   			if (tl.getY() < topmostY) {
   				topmostY = tl.getY();
   			}
		}
//   		System.out.println(" group tl = " + leftmostX + "," + topmostY);
   		return new XYcoords(leftmostX, topmostY);
	}

	@Override
	public XYcoords getBottomRight() {
		
		float rightmostX = -100000.0f;
		float bottommostY  = -100000.0f;

   		for (SvgObject child : children) {
   			XYcoords br = child.getBottomRight();
//   			System.out.println("  child " + child.getId() + " tl = " + tl);
   			if (br.getX() > rightmostX ) {
   				rightmostX = br.getX();
   			}
   			if (br.getY() > bottommostY) {
   				bottommostY = br.getY();
   			}
		}
//   		System.out.println(" group tl = " + leftmostX + "," + topmostY);
   		return new XYcoords(rightmostX, bottommostY);
	}
	
	@Override
	public void setTopLeft( XYcoords absoluteTopLeft ) {
		// what is the current tl
		XYcoords oldTL = getTopLeft();
		// how much are we moving it
		XYcoords movement = absoluteTopLeft.subtract(oldTL);
		move(movement);
	}

	@Override
	public void move( XYcoords movement ) {
		for (SvgObject child : children) {
			child.move(movement);
		}
	}
	
	
//		// for x and y separately, find the one that goes farthest "back" from starting point 
//		float furthestX = 0.0f;
//		SvgObject bestX = null;
//		float furthestY = 0.0f;
//		SvgObject bestY = null;
//		for (Map.Entry<SvgObject, XYcoords> elem : topleftcorners.entrySet()) {
//			SvgObject which = elem.getKey();
//			XYcoords where = elem.getValue();
//			if (where.getX() < furthestX) {
//				furthestX = where.getX();
//				bestX = which;
//			}
//			if (where.getY() < furthestY) {
//				furthestY = where.getY();
//				bestY = which;
//			}
//		}
//
//	}

//	/**
//	 * move the object so it's top left corner is at 0,0
//	 */
//	@Override
//	public void moveTopLeft() {
//   		Map<SvgObject, XYcoords> topleftcorners = new HashMap<>();
//   		// find all their top left mosts
//   		for (SvgObject child : children) {
//    			topleftcorners.put(child, child.topLeftOffset());
//    		}
//    		// for x and y separately, find the one that goes farthest "back" from starting point 
//    		float furthestX = 0.0f;
//    		SvgObject bestX = null;
//    		float furthestY = 0.0f;
//    		SvgObject bestY = null;
//    		for (Map.Entry<SvgObject, XYcoords> elem : topleftcorners.entrySet()) {
//    			SvgObject which = elem.getKey();
//				XYcoords where = elem.getValue();
//				if (where.getX() < furthestX) {
//					furthestX = where.getX();
//					bestX = which;
//				}
//				if (where.getY() < furthestY) {
//					furthestY = where.getY();
//					bestY = which;
//				}
//			}
//    		// in each dimension, the best will be reset to minus the furthest
//    		// others are offset by the difference between their start and the best's start
//    		float offsetX = -furthestX - getStart(bestX).getX();
//    		float offsetY = -furthestY - getStart(bestY).getY();
//    		XYcoords offset = new XYcoords(offsetX, offsetY);
//    		for (SvgObject child : children) {
//    			XYcoords first = child.getStart();
//				child.setStart(offset.add(first);
//			}
//		
//	}
//	
//	private static final Pattern FIRST_IN_PATH = Pattern.compile("m\\s+(\\S*?,\\S*)(.*)");
//	private static final Pattern FIRST_IN_PATH_REPLACE = Pattern.compile("m\\s+\\S*?,\\S*");
//	
//	
//	private void setFirstStep (Element pathElement, XYcoords newStep) {
//		String dString = pathElement.getAttribute("d");
//		Matcher m = FIRST_IN_PATH_REPLACE.matcher(dString);
//		String newD = m.replaceFirst("m " + newStep.toString());
////		System.out.println("Changing \n  " + dString + "\nto\n  " + newD);
//		pathElement.setAttribute("d", newD);
//	}
//
//	private XYcoords getStart (Element pathElement) {
//		String dString = pathElement.getAttribute("d");
//		Matcher m = FIRST_IN_PATH.matcher(dString);
//		if (m.matches()) {
//			return new XYcoords(m.group(1));
//		} else {
//			throw new IllegalStateException("Can't find first path step in " + dString + " from a " + pathElement.getLocalName());
//		}
//	}
//

}
