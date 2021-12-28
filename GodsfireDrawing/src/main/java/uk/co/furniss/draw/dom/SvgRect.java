package uk.co.furniss.draw.dom;

import org.w3c.dom.Element;

class SvgRect extends SvgObject {

	private  float x;
	private  float y;
	private  float width;
	private  float height;
	
	
	public SvgRect(Element rectElement) {
		super(rectElement);
		this.x = Float.parseFloat(rectElement.getAttribute("x"));
		this.y = Float.parseFloat(rectElement.getAttribute("y"));
		this.width = Float.parseFloat(rectElement.getAttribute("width"));
		this.height = Float.parseFloat(rectElement.getAttribute("height"));
	}

	// clone constructor
	private SvgRect(SvgRect original, String newId) {
		super(original.element, newId, true);
		this.x = original.x;
		this.y = original.y;
		this.width = original.width;
		this.height = original.height;
	}

	@Override
	public void transform(Trans trans) {
		
		XYcoords newTL = trans.apply(getTopLeft());
		XYcoords newBR = trans.apply(getBottomRight());
		x = newTL.getX();
		y = newTL.getY();
		width = newBR.getX() - x;
		height = newBR.getY() - y;
		setXY();
		setWH();
	}
	
//	@Override
//	public void applyTransform(XYcoords base, Transform trans) {
//		XYcoords newTL = trans.apply(getTopLeft());
//		XYcoords newBR = trans.apply(getBottomRight());
//		x = newTL.getX();
//		y = newTL.getY();
//		width = newBR.getX() - x;
//		height = newBR.getY() - y;
//		setXY();
//		setWH();
//	}

	@Override
	public void scale(Trans trans) {
		width = trans.scale(width);
		height = trans.scale(height);
		setWH();
	}
	
	@Override
	public void scaleTo( XYcoords base, Trans trans ) {
		XYcoords newXY = trans.scaleTo(base, getTopLeft());
		setXY(newXY);
		XYcoords newSize = trans.scale(new XYcoords(width, height));
		setWH(newSize);
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
		return new SvgRect(this, newId);
	}

	@Override
	public XYcoords getTopLeft() {
		return new XYcoords(x, y);
	}

	
	@Override
	public XYcoords getStart() {
		return getTopLeft();
	}

	@Override
	public XYcoords getBottomRight() {
		return new XYcoords(x + width, y + height);
	}


	private void setXY() {
		element.setAttribute("x", Float.toString(x));
		element.setAttribute("y", Float.toString(y));
	}

	private void setWH() {
		element.setAttribute("width", Float.toString(width));
		element.setAttribute("height", Float.toString(height));
	}

	public void setWH(XYcoords size) {
		width  = size.getX();
		height = size.getY();
		setWH();
	}
	
	@Override
	public void move( XYcoords movement ) {
		 x += movement.getX();
		 y += movement.getY();
		 setXY();

	}

	@Override
	public String toString() {
		return "rect " + getId() + "  x=" + x + ", y=" + y + ", w=" + width + ", h=" + height + ", trans=" + getTransformAttribute() + ".";
	}

	
}
