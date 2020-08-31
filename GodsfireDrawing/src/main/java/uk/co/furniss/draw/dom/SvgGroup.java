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

	@Override
	public void openStyle() {
		for (SvgObject child : children) {
			child.openStyle();
		}
	}
	


}
