package uk.co.furniss.draw.dom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

class SvgPath extends SvgObject {

	private XYcoords bottomRightOffset = null;
	private XYcoords topLeftOffset = null;
	private XYcoords definedCentre = null;

	private static final Logger LOGGER = LoggerFactory.getLogger(SvgPath.class.getName());
	private static final Map<String, List<ParamType>> paramTypes;

	private enum ParamType { XABS, YABS, XREL, YREL, SCALED, FIXED, HORIZ, VERT }
	static {
		paramTypes = new HashMap<>();
		List<ParamType> xy = Arrays.asList(ParamType.XREL, ParamType.YREL);
		paramTypes.put("M", Arrays.asList(ParamType.XABS, ParamType.YABS));
		paramTypes.put("m", xy);
		paramTypes.put("l", xy);
		paramTypes.put("h", Collections.singletonList(ParamType.HORIZ));
		paramTypes.put("v", Collections.singletonList(ParamType.VERT));
		paramTypes.put("z", Collections.emptyList());
		paramTypes.put("c", Arrays.asList(ParamType.XREL, ParamType.YREL, ParamType.XREL, ParamType.YREL, ParamType.XREL, ParamType.YREL));
		paramTypes.put("s", Arrays.asList(ParamType.XREL, ParamType.YREL, ParamType.XREL, ParamType.YREL));
		paramTypes.put("q", Arrays.asList(ParamType.XREL, ParamType.YREL, ParamType.XREL, ParamType.YREL));
		paramTypes.put("t", xy);
		paramTypes.put("a", Arrays.asList(ParamType.XREL, ParamType.YREL, ParamType.FIXED, ParamType.FIXED, ParamType.FIXED, ParamType.XREL, ParamType.YREL));

	}
	
	public SvgPath(Element element) {
		super(element);
		// remove any absolute paths
		LOGGER.info("constructing for path {}", element.getAttribute("id"));
		makePathsRelative();
		String xCentre = element.getAttribute("inkscape:transform-center-x");
		String yCentre = element.getAttribute("inkscape:transform-center-y");
		if (xCentre != "" || yCentre != "") {
			if (xCentre == "") {
				xCentre = "0.0";
			}
			if (yCentre == "") {
				yCentre = "0.0";
			}
			definedCentre = new XYcoords(xCentre, yCentre);
			LOGGER.info("defined centre is {}", definedCentre);
		}
		unscale();
		if (hasDefinedCentre()) {
			LOGGER.info("unscaled defined centre is {}", definedCentre);
		}
	}

	private SvgPath(Element original, String newId) {
		super(original, newId, true);
	}

	@Override
	public SvgObject clone( String newId ) {
		return new SvgPath(getElement(), newId);
	}

	private void unscale() {
		String tformAttr = element.getAttribute("transform");
		if (tformAttr.startsWith("scale")) {
			LOGGER.debug("applying and removing path {} scale of {}", getId(), tformAttr);
			Trans tform = new Transform2(tformAttr);
			scale(tform);
			element.removeAttribute("transform");
		}
	}
	
	private static final Pattern FIRST_IN_PATH = Pattern.compile("m\\s+(\\S*?,\\S*)(.*)");

	@Override
	protected boolean hasDefinedCentre() {
		return definedCentre != null;
	}

	@Override
	protected XYcoords getDefinedCentre() {
		return definedCentre;
	}

	private static final Pattern FIRST_IN_PATH_REPLACE = Pattern.compile("m\\s+\\S*?,\\S*");

	
	
	private void setStart( XYcoords newStart ) {
		String dString = element.getAttribute("d");
		Matcher m = FIRST_IN_PATH_REPLACE.matcher(dString);
		String newD = m.replaceFirst("m " + newStart.toString());
		LOGGER.trace("changing d from \n{}\nto\n{}", dString, newD);
		// System.out.println("Changing \n " + dString + "\nto\n " + newD);
		element.setAttribute("d", newD);
	}

	@Override
	public void move( XYcoords movement ) {
		if (hasDefinedCentre()) {
			definedCentre = definedCentre.add(movement);
			LOGGER.info("moved defined centre is {}", definedCentre);
		}
		XYcoords oldStart = getStart();
		XYcoords newStart = oldStart.add(movement);
		LOGGER.trace("moving from {} to {}", oldStart, newStart);
		setStart(newStart);
	}

	

	@Override
	public void translate( Trans trans ) {
		if (hasDefinedCentre()) {
			definedCentre = trans.translate(definedCentre);
			LOGGER.info("translated defined centre is {}", definedCentre);
		}
		setStart(trans.translate(getStart()));
	}

	@Override
	public void transform(Trans trans ) {
		if (hasDefinedCentre()) {
			definedCentre = trans.translate(definedCentre);
			LOGGER.info("transformed defined centre is {}", definedCentre);
		}
		XYcoords base = XYcoords.ORIGIN;
		Dattribute dIn = new Dattribute();
		LOGGER.info("pre transform by {}, d={}", trans, dIn);
		Dbuilder dOut = new Dbuilder();
		String cmd = dIn.next();
		if (!cmd.equalsIgnoreCase("m")) {
			throw new IllegalStateException("Path starts with " + cmd + ". Can't cope");
		}
		dOut.add(cmd.toLowerCase());
		// first one is the only absolute
		if (base != null) {
			XYcoords start = dIn.nextPair();
			XYcoords scaledStart = trans.apply(start);
			dOut.add(scaledStart);
		}
		while (dIn.hasNext()) {
			String next = dIn.next();
			if (next.matches("[A-Za-z]")) {
				// its a command
				cmd = next;
				dOut.add(cmd.toLowerCase());
			} else {
				// it wasn't a command, so go back one to make the next next() give the same
				// thing
				dIn.backOne();
			}
			if (cmd.equals(cmd.toLowerCase())) {
		
				List<ParamType> ptypes = paramTypes.get(cmd);
				if (ptypes == null) {
					throw new IllegalStateException("No defined paramtypes for " + cmd);
				}
				Iterator<ParamType> ptIt = ptypes.iterator();
				while (ptIt.hasNext()) {
					ParamType ptype = ptIt.next();
					switch (ptype) {
					case XREL:
						dOut.add(trans.rotate(dIn.nextPair()));
						ptIt.next();
						break;
					case HORIZ:
						dOut.replace("l");
						dOut.add(trans.rotate(new XYcoords(dIn.nextValue(), 0.0f)));
						break;
					case VERT:
						dOut.replace("l");
						dOut.add(trans.rotate(new XYcoords(0.0f, dIn.nextValue())));
						break;
					case SCALED:
						dOut.add(trans.scale(dIn.nextValue()));
						break;
					case FIXED:
						dOut.copy(dIn, 1);
						break;
					default:
						throw new IllegalStateException("Surprise paramtype " + ptype + " for " + cmd);
					}
					
				}
			
			}
		}
		LOGGER.info("   {} after transform {}", getId(), dOut.getD());
		LOGGER.debug("   {} centre is now {}", getId(), getCentre());
		element.setAttribute("d", dOut.getD());
		LOGGER.debug("   {} centre is now {}", getId(), getCentre());
	}

	
	@Override
	public void scaleTo(XYcoords base, Trans trans ) {

		if (definedCentre != null) {
			definedCentre = trans.scaleTo(base, definedCentre);
		}
		Dattribute dIn = new Dattribute();
		LOGGER.debug("pre scaling to {}, d={}", base, dIn);
		Dbuilder dOut = new Dbuilder();
		String cmd = dIn.next();
		if (!cmd.equalsIgnoreCase("m")) {
			throw new IllegalStateException("Path starts with " + cmd + ". Can't cope");
		}
		dOut.add(cmd.toLowerCase());
		// first one is the only absolute
		if (base != null) {
			XYcoords start = dIn.nextPair();
			XYcoords scaledStart = trans.scaleTo(base, start);
			dOut.add(scaledStart);
		}
		while (dIn.hasNext()) {
			String next = dIn.next();
			if (next.matches("[A-Za-z]")) {
				// its a command
				cmd = next;
				dOut.add(cmd.toLowerCase());
			} else {
				// it wasn't a command, so go back one to make the next next() give the same
				// thing
				dIn.backOne();
			}
			if (cmd.equals(cmd.toLowerCase())) {
				switch (cmd) {
				case "m": // relative move
					cmd = "l"; // move is only for one segment
				case "l": // relative line
				case "t": // shortcut quadratic
					dOut.add(trans.scale(dIn.nextPair()));
					break;
				case "h": // relative horizontal
				case "v":
					dOut.add(trans.scale(dIn.nextValue()));
					break;
				case "z":
					break;
				case "c": // relative bezier - 3 xy pairs, last is next node
					dOut.add(trans.scale(dIn.nextPair()));
					dOut.add(trans.scale(dIn.nextPair()));
					dOut.add(trans.scale(dIn.nextPair()));
					break;
				case "s": // shortcut bezier - 2 xy pairs
				case "q": // quadratic
					dOut.add(trans.scale(dIn.nextPair()));
					dOut.add(trans.scale(dIn.nextPair()));
					break;

				case "a": // arc segment - centre (x,y) _ x-rotation _ largearcflag _ sweep_flag _
				          // destinatioin (x,y)
					// i think this probably requires proportionality
					dOut.add(trans.scale(dIn.nextPair()));
					dOut.copy(dIn, 3);
					dOut.add(trans.scale(dIn.nextPair()));
					break;
				default:
					throw new IllegalStateException(
					        "unsupported relative path command " + cmd + " from " + dIn);
				}
			}
		}
		LOGGER.debug("   {} after scaling {}", getId(), dOut.getD());
		LOGGER.debug("   {} centre is now {}", getId(), getCentre());
		element.setAttribute("d", dOut.getD());
		LOGGER.debug("   {} centre is now {}", getId(), getCentre());
	}

	
	@Override
	public void scale( Trans trans ) {
		scaleTo(null, trans);
	}

	@Override
	public XYcoords getTopLeft() {
		calculateOffsetsFromStart();
		return getStart().add(topLeftOffset);
	}

	@Override
	public XYcoords getBottomRight() {
		calculateOffsetsFromStart();

		return getStart().add(bottomRightOffset);
	}

	@Override
	public XYcoords getStart() {
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
	 * 
	 * @param pathElement
	 * @return
	 */
	private void calculateOffsetsFromStart() {
			Dattribute pieces = new Dattribute();
			XYcoords tl = XYcoords.ORIGIN;
			XYcoords br = XYcoords.ORIGIN;
			XYcoords currentRelative = XYcoords.ORIGIN;
			// first subpath starts at origin in relative
			XYcoords pathStart = XYcoords.ORIGIN;
			// clear the initial absolute move
			pieces.next();
			pieces.nextPair();
			// initialise in case we start with h or v
			XYcoords deltaXY = currentRelative;
			String cmd = "l"; // after the initial move
			while (pieces.hasNext()) {

				String next = pieces.next();
				if (next.matches("[a-z]")) {
					cmd = next;
				} else {
					pieces.backOne();
				}
				switch (cmd) {
				case "m": // relative move
					// pathStart = currentRelative;
				case "l": // relative line
				case "t": // shortcut quadratic
					deltaXY = pieces.nextPair();
					break;
				case "h": // relative horizontal
					deltaXY = new XYcoords(pieces.nextValue(), 0.0f);
					break;
				case "v":
					deltaXY = new XYcoords(0.0f, pieces.nextValue());
					break;
				case "z":
					// line back to start of subpath
					deltaXY = pathStart.subtract(currentRelative);
					break;
				case "c": // relative bezier - 3 xy pairs, last is next node
					pieces.skip(4);
					deltaXY = pieces.nextPair();
					break;
				case "s": // shortcut bezier - 2 xy pairs
				case "q": // quadratic
					pieces.skip(2);
					deltaXY = pieces.nextPair();
					break;

				case "a": // arc segment - centre (x,y) _ x-rotation _ largearcflag _ sweep_flag _
				          // destinatioin (x,y)
					pieces.skip(5);
					deltaXY = pieces.nextPair();
					break;
				default:
					throw new IllegalStateException("unsupported relative path command " + cmd + " from " + pieces);
				}
				currentRelative = currentRelative.add(deltaXY);
				LOGGER.trace(cmd + " delta {}  now at {}", deltaXY, currentRelative);
				tl = tl.topLeftMost(currentRelative);
				br = br.bottomRightMost(currentRelative);
				if (cmd.equals("m")) {
					// starting a new subpath will reset the destination for z
					pathStart = currentRelative;
					cmd = "l";
				}
				// System.out.println(cmd + "\t" + deltaXY.tabbed() + "\t" +
				// currentRelative.tabbed()
				// +"\t" + pathStart.tabbed() + "\t" + tl.tabbed() );
			}
			// System.out.println(tl.toString() + " from " + dString);
			topLeftOffset = tl;
			bottomRightOffset = br;
			LOGGER.trace("from start for " + getId()  + ": top left {}, bottom right {}", tl, br);

	}

	private class Dattribute implements Iterator<String> {
		private final String[] fields;
		private int posn;

		Dattribute() {
			String dString = element.getAttribute("d");
			fields = dString.split(",\\s*|\\s+");
			posn = 0;
		}

		public void skip( int skip ) {
			posn += skip;

		}

		private void backOne() {
			posn--;
		}

		private float nextValue() {
			return Float.parseFloat(next());
		}

		@Override
		public String next() {
			return fields[posn++];
		}

		@Override
		public boolean hasNext() {
			return posn < fields.length;
		}

		XYcoords nextPair() {
			return new XYcoords(next(), next());
		}

		@Override
		public String toString() {
			return "Dattribute: at " + posn + " in " + Arrays.toString(fields);
		}
	}

	private class Dbuilder {
		private final List<String> fields = new ArrayList<>();

		void add( String one ) {
			fields.add(one);
		}

		void add( XYcoords xy ) {
			fields.add(xy.toString());
		}

		void copy( Dattribute old, int number ) {
			for (int i = 0; i < number; i++) {
				fields.add(old.next());
			}
		}

		String getD() {
			return fields.stream().collect(Collectors.joining(" "));
		}

		public void add( float x ) {
			add(Float.toString(x));
		}
		
		public void replace(String newcmd) {
			fields.set(fields.size()-1, newcmd);
		}

	}

	// get the points of the path (not necessarily the bounds in the case of curves
	private void makePathsRelative() {
		boolean changed = false;
		Dattribute dAsReceived = new Dattribute();

		Dbuilder dRelative = new Dbuilder();
		LOGGER.debug("d for {} as received {} ", getId(), dAsReceived);
		// this assumes the commands and their parameters are separated by spaces (which
		// is what
		// inkscape does. svg allows all sorts of variations
		// special case the begining
		String cmd = dAsReceived.next();
		if (!cmd.equalsIgnoreCase("m")) {
			throw new IllegalStateException("Path starts with " + cmd + ". Can't cope");
		}
		dRelative.add(cmd.toLowerCase());
		XYcoords currentAbsolute = dAsReceived.nextPair();
		XYcoords start = currentAbsolute;
		dRelative.add(start);
		// default to line after a move, but of the same kind
		cmd = cmd.equals("M") ? "L" : "l";

		XYcoords point = new XYcoords(0.0f, 0.0f);
		boolean lastZ = false;
		while (dAsReceived.hasNext()) {
			// what's next ?
			String next = dAsReceived.next();
			if (next.matches("[A-Za-z]")) {
				// its a command
				cmd = next;
				dRelative.add(cmd.toLowerCase());
			} else {
				// it wasn't a command, so go back one to make the next next() give the same
				// thing
				dAsReceived.backOne();
			}
			if (cmd.equals(cmd.toLowerCase())) {
				switch (cmd) {
				case "m": // relative move
					point = dAsReceived.nextPair();
					dRelative.add(point);
					if (lastZ) {
						start = currentAbsolute.add(point);
					}
					cmd = "l"; // move is only for one segment
					break;
				case "l": // relative line
				case "t": // shortcut quadratic
					point = dAsReceived.nextPair();
					dRelative.add(point);
					break;
				case "h": // relative horizontal
					point = new XYcoords(dAsReceived.nextValue(), 0.0f);
					dRelative.add(point.getX());
					break;
				case "v":
					point = new XYcoords(0.0f, dAsReceived.nextValue());
					dRelative.add(point.getY());
					break;
				case "z":
					// reset where we are
					currentAbsolute = start;

					point = new XYcoords(0.0f, 0.0f);
					break;
				case "c": // relative bezier - 3 xy pairs, last is next node
					dRelative.copy(dAsReceived, 4);
					point = dAsReceived.nextPair();
					dRelative.add(point);
					break;
				case "s": // shortcut bezier - 2 xy pairs
				case "q": // quadratic
					dRelative.copy(dAsReceived, 2);
					point = dAsReceived.nextPair();
					dRelative.add(point);
					break;

				case "a": // arc segment - centre (x,y) _ x-rotation _ largearcflag _ sweep_flag _
				          // destinatioin (x,y)
					dRelative.copy(dAsReceived, 5);
					point = dAsReceived.nextPair();
					dRelative.add(point);
					break;
				default:
					throw new IllegalStateException(
					        "unsupported relative path command " + cmd + " from " + dAsReceived);
				}

				currentAbsolute = currentAbsolute.add(point);
			} else {
				changed = true;
				switch (cmd) {
				case "M": // absolute move
					cmd = "l"; // move is only for one segment
					point = dAsReceived.nextPair().subtract(currentAbsolute);
					dRelative.add(point);
					if (lastZ) {
						start = currentAbsolute.add(point);
					}
					break;
					case "L": // absolute line
				case "T": // shortcut quadratic
					point = dAsReceived.nextPair().subtract(currentAbsolute);
					dRelative.add(point);
					break;
				case "H": // absolute horizontal
					float relX = dAsReceived.nextValue() - currentAbsolute.getX();
					point = new XYcoords(relX, 0.0f);
					dRelative.add(relX);
					break;
				case "V":
					float relY = dAsReceived.nextValue() - currentAbsolute.getY();
					point = new XYcoords(0.0f, relY);
					dRelative.add(relY);
					break;
				case "Z":
					// reset where we are
					currentAbsolute = start;

					point = new XYcoords(0.0f, 0.0f);
					break;
				case "C": // absolute bezier - 3 xy pairs, last is next node
					dRelative.add(dAsReceived.nextPair().subtract(currentAbsolute));
					dRelative.add(dAsReceived.nextPair().subtract(currentAbsolute));
					point = dAsReceived.nextPair().subtract(currentAbsolute);
					dRelative.add(point);
					break;
				case "S": // shortcut bezier - 2 xy pairs
				case "Q": // quadratic
					dRelative.add(dAsReceived.nextPair().subtract(currentAbsolute));
					point = dAsReceived.nextPair().subtract(currentAbsolute);
					dRelative.add(point);
					break;

				case "A": // arc segment - centre (x,y) _ x-rotation _ largearcflag _ sweep_flag _
				          // destinatioin (x,y)
//					dRelative.add(dAsReceived.nextPair().subtract(currentAbsolute));
					dRelative.copy(dAsReceived, 5);
					point = dAsReceived.nextPair().subtract(currentAbsolute);
					dRelative.add(point);
					break;
				default:
					throw new IllegalStateException(
					        "unsupported absolute path command " + cmd + " from " + dAsReceived);
				}
				LOGGER.debug("before cmd {} abs was {}", cmd, currentAbsolute);
				currentAbsolute = currentAbsolute.add(point);
				LOGGER.debug("after cmd " + cmd + " rel XY is {}, current abs is {}", point, currentAbsolute);
			}
			LOGGER.debug("after cmd " + cmd + " rel XY is {}, current abs is {}", point, currentAbsolute);
			lastZ = cmd.equalsIgnoreCase("z");
		}
		if (changed) {
			LOGGER.debug("d for {} made relative {}", getId(), dRelative.getD());
			element.setAttribute("d", dRelative.getD());
		}

	}

	@Override
	public String toString() {
		return "path " + getId() + " trans=" + getTransformAttribute() 
			+ "\nstart " + getStart() + "  TLo " + topLeftOffset	 + " BRo " + bottomRightOffset;
	}

}
