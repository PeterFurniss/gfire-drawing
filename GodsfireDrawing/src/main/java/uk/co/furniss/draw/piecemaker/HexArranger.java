package uk.co.furniss.draw.piecemaker;

import org.w3c.dom.Element;

import uk.co.furniss.draw.dom.PiecesDocument;
import uk.co.furniss.draw.dom.XYcoords;

/**
 * place hexes with points e-w, odd number column below the even
 */
public class HexArranger implements OutputArranger {

	private static final float PAGE_WIDTH = 210.0f;
	private static final float PAGE_HEIGHT = 297.0f;
	private static final float MARGIN = 10.0f;
	private static final String OUTPUT_LAYER_NAME = "output";

	protected final float hexSide;
	private final float halfHeight;
	private final float overlap;
	
	protected int pieceNumber;
	private int maxRow;
	private int maxCol;
	protected PiecesDocument piecesDoc;
	protected Element outputLayer;
	private SvgWriter writer;
	private static final double COS30 = Math.cos(Math.PI/ 6.0);

	public HexArranger(float hexSide, float overlap) {
		this.hexSide = hexSide;
		this.halfHeight = (float) (hexSide * COS30);
		this.overlap = overlap;
	}

	/* (non-Javadoc)
	 * @see uk.co.furniss.draw.piecemaker.PageArranger#start(uk.co.furniss.draw.piecemaker.SvgWriter)
	 */
	@Override
	public void start(SvgWriter writer) {
		this.writer = writer;
		this.piecesDoc = writer.getOutputDocument();
		maxRow = 0;
		maxCol = 0;
		outputLayer = piecesDoc.obtainEmptyLayer(OUTPUT_LAYER_NAME);
		writer.setOutputLayer(outputLayer);
	}

	
	/**
	 * get the upper left corner of the enclosing rectangle for a hex
	 */

	/**
	 * 
	 * @param row hex row (one-based)
	 * @param col hex column (one-based)
	 * @return
	 */
	public XYcoords getHexLocation(int row, int col) {
		maxRow = Math.max(maxRow,  row);
		maxCol = Math.max(maxCol,  col);
		int rowZeroBase = row - 1;
		if (col % 2 == 1) {
			// odd col (one based)- upper side of the wriggly line
			return new XYcoords( MARGIN + hexSide * (col - 1) * 1.5f, MARGIN + halfHeight * rowZeroBase * 2);
		} else {
			return new XYcoords(MARGIN + hexSide * (col - 2) * 1.5f + hexSide * 1.5f, MARGIN + halfHeight * rowZeroBase * 2 + halfHeight);
		}
	}

	
	/* (non-Javadoc)
	 * @see uk.co.furniss.draw.piecemaker.PageArranger#finish()
	 */
	@Override
	public void finish() {
		hideOtherLayers();
	}

	public void hideOtherLayers() {
		piecesDoc.hideAllLayersButOne(OUTPUT_LAYER_NAME);
	}
	
	@Override
	public int getPageCount() {
		// this has a rather different meaning to the method on a piece arranger
		// per page counts can be fractional
		float rowsPerPage = (PAGE_HEIGHT - overlap) / (halfHeight * 3);
		int pagesDown = (int) Math.ceil(maxRow / rowsPerPage);
		// this is slightly approximate
		float colsPerPage = (PAGE_WIDTH - overlap) / (hexSide * 1.5f);
		int pagesAcross = (int) Math.ceil(maxCol / colsPerPage);
		return pagesDown * pagesAcross;
		
	}
	

	
}
