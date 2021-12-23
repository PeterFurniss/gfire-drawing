package uk.co.furniss.draw.piecemaker;

import uk.co.furniss.draw.dom.XYcoords;

interface CubeArranger extends PieceArranger {

	XYcoords getFaceLocation( CubeFace face );

}