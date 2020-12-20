package uk.co.furniss.draw.piecemaker;

import java.util.Map;

import uk.co.furniss.draw.dom.XYcoords;

public interface PieceArranger extends OutputArranger {

	XYcoords getNextLocation();

	/**
	 * set up a group if order of battle. this is ignored for regular piece arrangement, but
	 * it's (arguably) cleaner than testing whether its needed
	 * @param specRow
	 */
	void setGroup(Map<String, String> specRow);
}