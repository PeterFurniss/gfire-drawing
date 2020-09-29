package uk.co.furniss.draw.piecemaker;

import org.w3c.dom.Element;

import uk.co.furniss.draw.dom.PiecesDocument;

public interface SvgWriter {

	void setOutputLayer( Element outputLayer );

	PiecesDocument getOutputDocument();

}