package uk.co.furniss.draw.dom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class SvgCircle extends SvgObject {

	private float cx;
	private float cy;
	private float r;

	
	private static final Logger LOGGER = LoggerFactory.getLogger(SvgCircle.class.getName());
	
	public SvgCircle(Element circleElement) {
		super(circleElement);
		this.cx = Float.parseFloat(circleElement.getAttribute("cx"));
		this.cy = Float.parseFloat(circleElement.getAttribute("cy"));
		this.r = Float.parseFloat(circleElement.getAttribute("r"));
	}

	// clone constructor
	private SvgCircle(SvgCircle original, String newId) {
		super(original.element, newId, true);
		this.cx = original.cx;
		this.cy = original.cy;
		this.r = original.r;
	}

	@Override
	public SvgCircle clone( String newId ) {
		return new SvgCircle(this, newId);
	}

//	@Override
//	public void applyTransform( XYcoords base, Transform trans ) {
//
//		LOGGER.debug("circle centre before {}", makeCentre());
//		XYcoords newCentre = trans.apply(makeCentre().subtract(base)).add(base);
//		LOGGER.debug("circle centre after {}", newCentre);
//		setXY(newCentre);
//		
//		r = trans.scale(r);
//		setR();
//	}
	
	@Override
	public void scale( Transform trans ) {

		cx = trans.scale(cx);
		cy = trans.scale(cy);
		setXY();
		r = trans.scale(r);
		setR();
	}
	
	@Override
	public void scaleTo(XYcoords base, Transform trans ) {

		setXY(trans.scaleTo(base, makeCentre()));
		r = trans.scale(r);
		setR();
		LOGGER.debug("after scaling, circle {} is centred at {}", getId(), getCentre());
	}

	public XYcoords makeCentre() {
		return new XYcoords(cx, cy);
	}

	public void setXY( XYcoords newCtr ) {
		cx = newCtr.getX();
		cy = newCtr.getY();
		setXY();
	}
	
	
	@Override
	public void translate( Transform trans ) {
		XYcoords newCentre = trans.translate(makeCentre());
		setXY(newCentre);

	}

	@Override
	public XYcoords getStart() {
		return new XYcoords(cx, cy);
	}

	@Override
	public XYcoords getTopLeft() {
		// this is top left of the enclosing box
		return new XYcoords(cx - r, cy - r);
	}

	@Override
	public XYcoords getBottomRight() {
		// this is bottom right of the enclosing box
		return new XYcoords(cx + r, cy + r);
	}

	public void setXY() {
		element.setAttribute("cx", Float.toString(cx));
		element.setAttribute("cy", Float.toString(cy));
	}

	public void setR() {
		element.setAttribute("r", Float.toString(r));
	}
	
	@Override
	public void move( XYcoords movement ) {
		 cx += movement.getX();
		 cy += movement.getY();
		 setXY();

	}

}
