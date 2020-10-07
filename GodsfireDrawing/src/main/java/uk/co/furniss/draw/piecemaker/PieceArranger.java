package uk.co.furniss.draw.piecemaker;

import uk.co.furniss.draw.dom.XYcoords;

public interface PieceArranger extends OutputArranger {

	XYcoords getNextLocation();

}