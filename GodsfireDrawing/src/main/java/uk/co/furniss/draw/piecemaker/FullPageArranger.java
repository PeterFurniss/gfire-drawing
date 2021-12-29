package uk.co.furniss.draw.piecemaker;

import java.util.Map;

import org.w3c.dom.Element;

import uk.co.furniss.draw.dom.PiecesDocument;
import uk.co.furniss.draw.dom.XYcoords;

class FullPageArranger implements PieceArranger {

	private static final float PAGE_WIDTH = 210.0f;
	private static final float PAGE_HEIGHT = 297.0f;
	private static final float MARGIN = 10.0f;
	private static final String OUTPUT_LAYER_BASE_NAME = "output";
	private static final String FIRST_OUTPUT_LAYER = OUTPUT_LAYER_BASE_NAME + "1";

	protected final float gap;
	protected final float pieceSpacing;
	private final int rowsPerPage;
	private final int colsPerRow;
	private final int piecesPerPage;
	protected int pieceNumber;
	private int pageNumber;
	private int currentRow;
	protected PiecesDocument piecesDoc;
	protected Element outputLayer;
	private SvgWriter writer;
	
	public FullPageArranger(float pieceSize, float gap) {
		this.gap = gap;
		pieceSpacing = pieceSize + gap;
		colsPerRow = (int) ( ( PAGE_WIDTH - 2 * ( MARGIN - gap ) ) / pieceSpacing ) ;
		rowsPerPage = (int) ( ( PAGE_HEIGHT - 2 * ( MARGIN - gap ) ) / pieceSpacing ) ;
		piecesPerPage = rowsPerPage * colsPerRow;
		pageNumber = -1;
		currentRow = 0;
		pieceNumber = 0;
	}

	/* (non-Javadoc)
	 * @see uk.co.furniss.draw.piecemaker.PageArranger#start(uk.co.furniss.draw.piecemaker.SvgWriter)
	 */
	@Override
	public void start(SvgWriter writer) {
		this.writer = writer;
		this.piecesDoc = writer.getOutputDocument();
		newOutputPage();
	}

	private void newOutputPage() {
		pageNumber++;
		outputLayer = piecesDoc.obtainEmptyLayer(OUTPUT_LAYER_BASE_NAME + Integer.toString(pageNumber+1));
		writer.setOutputLayer(outputLayer);
	}
	
	/* (non-Javadoc)
	 * @see uk.co.furniss.draw.piecemaker.PageArranger#getNextLocation()
	 */
	@Override
	public XYcoords getNextLocation() {
		int numberOnPage = findNumberInPage(piecesPerPage);
		currentRow = numberOnPage / colsPerRow;
		int currentCol = numberOnPage % colsPerRow;
		return new XYcoords( MARGIN + currentCol * pieceSpacing, MARGIN + currentRow * pieceSpacing);
	}

	protected int findNumberInPage(int actualPiecesPerPage) {
		int page = pieceNumber / actualPiecesPerPage;

		if (page != pageNumber) {
			drawFiducialLines();
			newOutputPage();
		}
		int numberOnPage = pieceNumber % actualPiecesPerPage;
		// pieceNumber is how many we've done, but first one is at zero
		pieceNumber++;
		return numberOnPage;
	}
	
	/* (non-Javadoc)
	 * @see uk.co.furniss.draw.piecemaker.PageArranger#finish()
	 */
	@Override
	public void finish() {
		drawFiducialLines();
		hideOtherLayers();
	}

	private void hideOtherLayers() {
		piecesDoc.hideAllLayersButOne(FIRST_OUTPUT_LAYER);
	}
	
	@Override
	public int getPageCount() {
		return pageNumber + 1;
	}
	
	protected void drawFiducialLines() {
		// cols per row is actually the limit for zero-base
		boolean withGap = gap > 0.1f;
		if (! withGap) {
			return;
		}
		float x1 = MARGIN - gap;
		float x2 = x1 + (colsPerRow ) * pieceSpacing + gap;
		
		for (int r = 0; r < currentRow + 2; r++) {
			float y = MARGIN + r * pieceSpacing;
			piecesDoc.drawLine(outputLayer, new XYcoords(x1, y), new XYcoords(x2, y));
			if (withGap) {
				y -= gap;
				piecesDoc.drawLine(outputLayer, new XYcoords(x1, y), new XYcoords(x2, y));
			}

		}
		float y1 = MARGIN - gap;
		float y2 = y1 + ( currentRow + 1 ) * pieceSpacing + gap;
		for (int c = 0; c <= colsPerRow ; c++) {
			float x = MARGIN + c * pieceSpacing;
			piecesDoc.drawLine(outputLayer, new XYcoords(x, y1), new XYcoords(x, y2));
			if (withGap) {
				x -= gap;
				piecesDoc.drawLine(outputLayer, new XYcoords(x, y1), new XYcoords(x, y2));
			}

		}
	}

	@Override
	public void setGroup( Map<String, String> specRow ) {
		// do nothing
		
	}

	
}
