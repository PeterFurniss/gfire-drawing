package uk.co.furniss.draw.piecemaker;

interface OutputArranger {

	void start( SvgWriter writer );

	void finish();

	int getPageCount();

}