package uk.co.furniss.draw.dom;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.w3c.dom.Element;


public class SvgPath extends SvgObject {

	private XYcoords bottomRightOffset = null;
	private XYcoords topLeftOffset = null;
	
	public SvgPath (Element element) {
		super(element);
		// remove any absolute paths
		makePathsRelative();
	}
	
	private SvgPath(Element original, String newId) {
		super(original, newId, true);
	}
	
	@Override
	public SvgObject clone( String newId ) {
		return new SvgPath(getElement(), newId);
	}

	@Override
	public void setTopLeft(XYcoords absoluteTopLeft) {
		ensureOffsets();
		XYcoords tl = topLeftOffset;
		setStart(absoluteTopLeft.subtract(tl));
	}
	
	
	private static final Pattern FIRST_IN_PATH = Pattern.compile("m\\s+(\\S*?,\\S*)(.*)");
	private static final Pattern FIRST_IN_PATH_REPLACE = Pattern.compile("m\\s+\\S*?,\\S*");
	
	
	void setStart (XYcoords newStart) {
		String dString = element.getAttribute("d");
		Matcher m = FIRST_IN_PATH_REPLACE.matcher(dString);
		String newD = m.replaceFirst("m " + newStart.toString());
//		System.out.println("Changing \n  " + dString + "\nto\n  " + newD);
		element.setAttribute("d", newD);
	}

	@Override
	public void move(XYcoords movement) {
		XYcoords oldStart = getStart();
		setStart(oldStart.add(movement));
	}
	
	@Override
	public XYcoords getTopLeft() {
		ensureOffsets();
		return getStart().add(topLeftOffset);
	}
	
	
	@Override
	public XYcoords getBottomRight() {
		ensureOffsets();

		return getStart().add(bottomRightOffset);
	}
	

	private XYcoords getStart () {
		String dString = element.getAttribute("d");
		Matcher m = FIRST_IN_PATH.matcher(dString);
		if (m.matches()) {
			return new XYcoords(m.group(1));
		} else {
			throw new IllegalStateException("Can't find first path step in " + dString + " from a path");
		}
	}

	/**
	 * get the offset from the start to the top-left of the object
	 * @param pathElement
	 * @return
	 */
	private void ensureOffsets() {
		if (topLeftOffset == null) {
    		String dString = element.getAttribute("d");
    		// could do this with a hairy regex
    		String[] pieces = dString.split("\\s+");
    		XYcoords tl = XYcoords.ORIGIN;
    		XYcoords br = XYcoords.ORIGIN;
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
    			br = br.bottomRightMost(currentRelative);
    			if (cmd.equals("m")) {
    				// starting a new subpath will reset the destination for z
    				pathStart = currentRelative;
    				cmd = "l";
    			}
    //			System.out.println(cmd + "\t" + deltaXY.tabbed() + "\t" + currentRelative.tabbed()
    //				+"\t" + pathStart.tabbed() + "\t" + tl.tabbed() );
    		}
    //		System.out.println(tl.toString() + " from " + dString);
    		topLeftOffset = tl;
    		bottomRightOffset = br;
		}
	}
	

	
	// get the points of the path (not necessarily the bounds in the case of curves
	private void makePathsRelative() {
			boolean changed = false;
    		String dString = element.getAttribute("d");
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
    			element.setAttribute("d", newPieces.stream().collect(Collectors.joining(" ")));
    		}
		
	}


}
