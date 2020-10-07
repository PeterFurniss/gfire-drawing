package uk.co.furniss.draw.piecemaker;

public interface OutputArranger {

	void start( SvgWriter writer );

	void finish();

	int getPageCount();

}