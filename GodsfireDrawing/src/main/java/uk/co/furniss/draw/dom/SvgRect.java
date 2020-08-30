package uk.co.furniss.draw.dom;

import org.w3c.dom.Element;

public class SvgRect extends SvgObject {

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
	public SvgObject clone(String newId ) {
		return new SvgRect(this, newId);
	}

	@Override
	public XYcoords getTopLeft() {
		return new XYcoords(x, y);
	}

	@Override
	public XYcoords getBottomRight() {
		return new XYcoords(x + width, y + height);
	}

	@Override
	public void setTopLeft( XYcoords absoluteTopLeft ) {
		 x = absoluteTopLeft.getX();
		 y = absoluteTopLeft.getY();
		 setXY();

	}

	public void setXY() {
		element.setAttribute("x", Float.toString(x));
		element.setAttribute("y", Float.toString(y));
	}

	@Override
	public void move( XYcoords movement ) {
		 x += movement.getX();
		 y += movement.getY();
		 setXY();

	}

}
