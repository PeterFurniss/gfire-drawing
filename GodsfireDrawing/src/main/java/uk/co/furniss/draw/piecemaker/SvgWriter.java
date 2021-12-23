package uk.co.furniss.draw.piecemaker;

import org.w3c.dom.Element;

import uk.co.furniss.draw.dom.PiecesDocument;

interface SvgWriter {

	void setOutputLayer( Element outputLayer );

	PiecesDocument getOutputDocument();

}