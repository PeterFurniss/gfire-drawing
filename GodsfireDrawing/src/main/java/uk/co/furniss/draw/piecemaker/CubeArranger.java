package uk.co.furniss.draw.piecemaker;

import uk.co.furniss.draw.dom.XYcoords;

public interface CubeArranger extends PageArranger {

	XYcoords getFaceLocation( CubeFace face );

}