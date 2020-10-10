package uk.co.furniss.draw.dom;

public interface Trans {

	XYcoords apply( XYcoords original );

	XYcoords scale( XYcoords original );

	XYcoords scaleTo( XYcoords base, XYcoords original );

	float scale( float original );

	XYcoords translate( XYcoords original );

	boolean isInternalising();

	boolean isTranslating();

	boolean isScaling();

	XYcoords rotate( XYcoords nextPair );

}