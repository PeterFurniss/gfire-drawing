package uk.co.furniss.draw.piecemaker;

import uk.co.furniss.draw.dom.XYcoords;

public interface PageArranger {

	void start( SvgWriter writer );

	XYcoords getNextLocation();

	void finish();

	int getPageCount();

}