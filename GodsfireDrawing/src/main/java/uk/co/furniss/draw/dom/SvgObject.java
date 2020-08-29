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


public class SvgObject {

	private static final XPathUtil XPU = XPathUtil.getSVG();
	
	// the xml as read (and possibly modified)
	private final Element element;

	private final String id;
	// TBD - temp, until create polymorphs
	private final boolean isPath;

	public SvgObject (Element originalElement) {
		this.element = originalElement;
		this.id = element.getAttribute("id");
		isPath = element.getLocalName().equals("path");
		// remove any absolute paths
		makePathsRelative();
	}
	
	/**
	 * make a copy of the element, with a new id
	 * @return
	 */
	private SvgObject(SvgObject original, String newId) {
		this.element = (Element) original.element.cloneNode(true);
		element.setAttribute("id", newId);
		this.id = newId;
		isPath = original.isPath;
	}

	public SvgObject clone(String newId) {
		return new SvgObject(this, newId);
	}
	/**
	 * move the object so it's top left corner is at 0,0
	 */
	public void moveTopLeft() {
		// can assume it is relative, and we won't worry about curves that come outside the box
		
		if (isPath) {
			XYcoords tl = topLeftOfPath(element);
			setFirstStep(element, tl.minus());
		} else {
    		Map<Element, XYcoords> topleftcorners = new HashMap<>();
    		List<Element> pathElements = getPaths();
    		// find all their top left mosts
    		for (Element pathElement : pathElements) {
    			topleftcorners.put(pathElement, topLeftOfPath(pathElement));
    		}
    		// for x and y separately, find the one that goes farthest "back" from starting point 
    		float furthestX = 0.0f;
    		Element bestX = null;
    		float furthestY = 0.0f;
    		Element bestY = null;
    		for (Map.Entry<Element, XYcoords> elem : topleftcorners.entrySet()) {
				Element which = elem.getKey();
				XYcoords where = elem.getValue();
				if (where.getX() < furthestX) {
					furthestX = where.getX();
					bestX = which;
				}
				if (where.getY() < furthestY) {
					furthestY = where.getY();
					bestY = which;
				}
			}
    		// in each dimension, the best will be reset to minus the furthest
    		// others are offset by the difference between their start and the best's start
    		float offsetX = -furthestX - getFirstStep(bestX).getX();
    		float offsetY = -furthestY - getFirstStep(bestY).getY();
    		XYcoords offset = new XYcoords(offsetX, offsetY);
    		for (Element element : pathElements) {
    			XYcoords first = getFirstStep(element);
				setFirstStep(element, offset.add(first));
			}
		}
	}
	
	private static final Pattern FIRST_IN_PATH = Pattern.compile("m\\s+(\\S*?,\\S*)(.*)");
	private static final Pattern FIRST_IN_PATH_REPLACE = Pattern.compile("m\\s+\\S*?,\\S*");
	
	
	private void setFirstStep (Element pathElement, XYcoords newStep) {
		String dString = pathElement.getAttribute("d");
		Matcher m = FIRST_IN_PATH_REPLACE.matcher(dString);
		String newD = m.replaceFirst("m " + newStep.toString());
//		System.out.println("Changing \n  " + dString + "\nto\n  " + newD);
		pathElement.setAttribute("d", newD);
	}

	private XYcoords getFirstStep (Element pathElement) {
		String dString = pathElement.getAttribute("d");
		Matcher m = FIRST_IN_PATH.matcher(dString);
		if (m.matches()) {
			return new XYcoords(m.group(1));
		} else {
			throw new IllegalStateException("Can't find first path step in " + dString + " from a " + pathElement.getLocalName());
		}
	}

	/**
	 * get the top left of a path
	 * @param pathElement
	 * @return
	 */
	private XYcoords topLeftOfPath( Element pathElement ) {
		String dString = pathElement.getAttribute("d");
		// could do this with a hairy regex
		String[] pieces = dString.split("\\s+");
		XYcoords tl = XYcoords.ORIGIN;
		XYcoords currentRelative = XYcoords.ORIGIN;
		// first subpath starts at origin in relative
		XYcoords pathStart = XYcoords.ORIGIN;
		// initialise in case we start with h or v
		XYcoords deltaXY = currentRelative;
		String cmd = "l"; // after the initial move
		for (int i = 2; i < pieces.length; i++) {

			String next = pieces[i];
			if (next.length() == 1) {
				cmd = next;
				i++;
			}
			switch (cmd) {
			case "m": // relative move
//				pathStart = currentRelative;
			case "l": // relative line
			case "t": // shortcut quadratic
				deltaXY = new XYcoords(pieces[i]);
				break;
			case "h": // relative horizontal
				deltaXY = new XYcoords(Float.parseFloat(pieces[i]), 0.0f);
				break;
			case "v":
				deltaXY = new XYcoords(0.0f, Float.parseFloat(pieces[i]));
				break;
			case "z":
				// step back from the next field - z has no parameters
				i--;
				// line back to start of subpath
				deltaXY =  pathStart.subtract(currentRelative);
				break;
			case "c": // relative bezier - 3 xy pairs, last is next node
				i += 2;
				deltaXY = new XYcoords(pieces[i]);
				break;
			case "s": // shortcut bezier - 2 xy pairs
			case "q": // quadratic
				deltaXY = new XYcoords(pieces[++i]);
				break;

			case "a": // arc segment - centre (x,y) _ x-rotation _ largearcflag _ sweep_flag _
			          // destinatioin (x,y)
				i += 4;
				deltaXY = new XYcoords(pieces[i]);
				break;
			default:
				System.out.println("unsupported relative path command " + cmd);
				break;
			}
			currentRelative = currentRelative.add(deltaXY);
			tl = tl.topLeftMost(currentRelative);
			if (cmd.equals("m")) {
				// starting a new subpath will reset the destination for z
				pathStart = currentRelative;
				cmd = "l";
			}
//			System.out.println(cmd + "\t" + deltaXY.tabbed() + "\t" + currentRelative.tabbed()
//				+"\t" + pathStart.tabbed() + "\t" + tl.tabbed() );
		}
//		System.out.println(tl.toString() + " from " + dString);
		return tl;
	}
	
	// get the points of the path (not necessarily the bounds in the case of curves
	private void makePathsRelative() {
		List<Element> pathElements = getPaths();
		for (Element pathElement : pathElements) {
			boolean changed = false;
    		String dString = pathElement.getAttribute("d");
    		String [] pieces = dString.split("\\s+");
    		
    		List<String> newPieces = new ArrayList<>();
    		
    		// this assumes the commands and their parameters are separated by spaces (which is what
    		// inkscape does. svg allows all sorts of variations
    		// special case the begining
    		String cmd = pieces[0];
    		if (! cmd.equals("m")) {
    			throw new IllegalStateException("Path starts with " + cmd + ". Can't cope");
    		}
    		newPieces.add(cmd);
    		XYcoords currentAbsolute = new XYcoords(pieces[1]);
    		XYcoords start = currentAbsolute;
    		newPieces.add(pieces[1]);
    		// default to line after a move 
    		cmd = "l";
    		
    		XYcoords point = new XYcoords(0.0f, 0.0f);
    		for (int i = 2; i < pieces.length ; i++) {
    			// what's next ?
    			String next = pieces[i];
    			if (next.length() == 1) {
    				cmd = next;
    				newPieces.add(cmd.toLowerCase());
    				i++;
    			}
    			if (cmd.equals(cmd.toLowerCase())) {
         			switch (cmd) {
        			case "m":  // relative move
        				cmd = "l";  // move is only for one segment
        			case "l":  // relative line
        			case "t":  // shortcut quadratic
        				point = new XYcoords(pieces[i]);
        				break;
        			case "h":  // relative horizontal
        				point = new XYcoords(Float.parseFloat(pieces[i]), 0.0f);
        				break;
        			case "v":
        				point = new XYcoords(0.0f, Float.parseFloat(pieces[i]));
        				break;
        			case "z":
        				// reset where we are
        				currentAbsolute = start;
        				// step back from the next field
        				i--;
        				point = new XYcoords(0.0f, 0.0f);
        				break;
        			case "c":  // relative bezier - 3 xy pairs, last is next node
        				newPieces.add(pieces[i++]);
        				newPieces.add(pieces[i++]);
        				point =  new XYcoords(pieces[i]);
        				break;
        			case "s":  // shortcut bezier - 2 xy pairs
        			case "q":  // quadratic
        				newPieces.add(pieces[i++]);
        				point =  new XYcoords(pieces[i]);
        				break;
        				
        			case "a":  // arc segment - centre (x,y) _ x-rotation _ largearcflag _ sweep_flag _ destinatioin (x,y)
        				newPieces.add(pieces[i++]);
        				newPieces.add(pieces[i++]);
        				newPieces.add(pieces[i++]);
        				newPieces.add(pieces[i++]);
        				point = new XYcoords(pieces[i]);
        				break;
        			default:
        				System.out.println("unsupported relative path command " + cmd);
        				break;
        			}
        			if (! cmd.equals("z")) {
        				newPieces.add(pieces[i]);
        			}
        			currentAbsolute = currentAbsolute.add(point);
    			} else {
    				changed = true;
        			switch (cmd) {
        			case "M":  // absolute move
        				cmd = "l";  // move is only for one segment
        			case "L":  // absolute line
        			case "T":  // shortcut quadratic
        				point = new XYcoords(pieces[i]).subtract(currentAbsolute);
        				newPieces.add(point.toString());
        				break;
        			case "H":  // absolute horizontal
        				float relX = Float.parseFloat(pieces[i]) - currentAbsolute.getX();
						point = new XYcoords(relX ,0.0f);
						newPieces.add(Float.toString(relX));
        				break;
        			case "V":
        				float relY = Float.parseFloat(pieces[i]) - currentAbsolute.getY();
						point = new XYcoords(0.0f, relY);
						newPieces.add(Float.toString(relY));
        				break;
        			case "Z":
        				// reset where we are
        				currentAbsolute = start;
        				i--;
        				point = new XYcoords(0.0f, 0.0f);
        				break;
        			case "C":  // absolute bezier - 3 xy pairs, last is next node 
        				newPieces.add( new XYcoords(pieces[i++]).subtract(currentAbsolute).toString());
        				newPieces.add( new XYcoords(pieces[i++]).subtract(currentAbsolute).toString());
        				point =  new XYcoords(pieces[i]).subtract(currentAbsolute);
        				newPieces.add(point.toString());
        				break;
        			case "S":  // shortcut bezier - 2 xy pairs
        			case "Q":  // quadratic
        				newPieces.add( new XYcoords(pieces[i++]).subtract(currentAbsolute).toString());
        				point =  new XYcoords(pieces[i]).subtract(currentAbsolute);
        				newPieces.add(point.toString());
        				break;
        				
        			case "A":  // arc segment - centre (x,y) _ x-rotation _ largearcflag _ sweep_flag _ destinatioin (x,y)
        				newPieces.add( new XYcoords(pieces[i++]).subtract(currentAbsolute).toString());
        				newPieces.add(pieces[i++]);
        				newPieces.add(pieces[i++]);
        				newPieces.add(pieces[i++]);
        				point = new XYcoords(pieces[i]).subtract(currentAbsolute);
        				newPieces.add(point.toString());
        				break;
        			default:
        				System.out.println("unsupported absolute path command " + cmd);
        				break;
        			}
        			currentAbsolute = currentAbsolute.add(point);
    			}
    		}
    		if (changed) {
    			pathElement.setAttribute("d", newPieces.stream().collect(Collectors.joining(" ")));
    		}
		}
	}

	private List<Element> getPaths() {
		List<Element> pathElements;
		if (isPath) {
			pathElements = Collections.singletonList(element);
		} else {
			pathElements = XPU.findElements(element,  "path");
		}
		return pathElements;
	}



	public Element getElement() {
		return element;
	}

	public String getId() {
		return id;
	}
}
