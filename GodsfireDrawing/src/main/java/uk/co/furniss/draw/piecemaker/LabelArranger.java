package uk.co.furniss.draw.piecemaker;

import uk.co.furniss.draw.dom.XYcoords;

public class LabelArranger extends FullPage {

	private static final float LABEL_WIDTH = 63.5f;
	private static final float LABEL_HEIGHT = 38.1f;
	private static final int LABELS_PER_ROW = 3;
	private static final int LABELROWS_PER_PAGE = 7;
	// top and side margins are the label papers plus the apparent inaccurace of the printer
	private static final float TOP_MARGIN =15.1f + 3.1f;
	private static final float SIDE_MARGIN = 7.7f + 1.3f;
	private static final float MID_MARGIN = 2.6f;
	private static final float CURVE_MARGIN = 2.5f;
	
	private static final float EFFECTIVE_WIDTH = LABEL_WIDTH - 2 * CURVE_MARGIN;
	private static final float EFFECTIVE_HEIGHT = LABEL_HEIGHT - 2 * CURVE_MARGIN;

	private final int piecesPerLabel;
	private final int rowsPerLabel;
	private final int colsPerLabel;
	private final int piecesPerPage;
	private int currentLabel;
	
	public LabelArranger(float pieceSize, float gap) {
		this(pieceSize, gap, 1);
	}


	public LabelArranger(float pieceSize, float gap, int firstLabelNumber) {
		super(pieceSize, gap);
		if (pieceSpacing > EFFECTIVE_HEIGHT) {
			if (pieceSize > EFFECTIVE_HEIGHT) {
				throw new IllegalStateException("Cannot write pieces " + pieceSize + " high on these labels");
			} else {
				 rowsPerLabel = 1;
				 
			}
		} else {
			rowsPerLabel = (int) (EFFECTIVE_HEIGHT / pieceSpacing);
			
		}
		colsPerLabel = (int)(EFFECTIVE_WIDTH / pieceSpacing);
		piecesPerLabel = rowsPerLabel * colsPerLabel;
		piecesPerPage = piecesPerLabel * LABELS_PER_ROW * LABELROWS_PER_PAGE;
		currentLabel = firstLabelNumber - 1;
		if (firstLabelNumber != 1) {
			pieceNumber = currentLabel * piecesPerLabel;
		}
	}


	@Override
	public XYcoords getNextLocation() {
		int numberInPage = findNumberInPage(piecesPerPage);
		if (numberInPage == 0) {
			currentLabel = 0;
		}
		int labelNumber = numberInPage / piecesPerLabel;
		if (labelNumber != currentLabel) {
			drawFiducialLines();
			currentLabel++;
		}
		int labelRow = labelNumber / LABELS_PER_ROW;
		int labelCol = labelNumber % LABELS_PER_ROW;
		int positionInLabel = numberInPage % piecesPerLabel;
		int rowInLabel = positionInLabel / colsPerLabel;
		int colInLabel = positionInLabel % colsPerLabel;
		return new XYcoords( SIDE_MARGIN + labelCol * (LABEL_WIDTH + MID_MARGIN) + CURVE_MARGIN + colInLabel*pieceSpacing,
				TOP_MARGIN + labelRow * LABEL_HEIGHT + CURVE_MARGIN + rowInLabel*pieceSpacing);
	}

	@Override
	protected void drawFiducialLines() {
		// only do current label
		boolean withGap = gap > 0.1f;
		int labelRow = currentLabel / LABELS_PER_ROW;
		int labelCol = currentLabel % LABELS_PER_ROW;
		
		float x1 = SIDE_MARGIN + labelCol * (LABEL_WIDTH + MID_MARGIN);
		float x2 = x1 + colsPerLabel * pieceSpacing + gap + CURVE_MARGIN;
		
		for (int r = 0; r < rowsPerLabel + 1; r++) {
			float y = TOP_MARGIN + labelRow * LABEL_HEIGHT + r * pieceSpacing + CURVE_MARGIN;
			piecesDoc.drawLine(outputLayer, new XYcoords(x1, y), new XYcoords(x2, y));
			if (withGap) {
				y -= gap;
				piecesDoc.drawLine(outputLayer, new XYcoords(x1, y), new XYcoords(x2, y));
			}

		}
		
		float y1 = TOP_MARGIN + labelRow * LABEL_HEIGHT - gap ;
		float y2 = y1 + rowsPerLabel * pieceSpacing + CURVE_MARGIN;
		for (int c = 0; c <= colsPerLabel ; c++) {
			float x = SIDE_MARGIN + labelCol * (LABEL_WIDTH + MID_MARGIN) + c * pieceSpacing + CURVE_MARGIN;
			piecesDoc.drawLine(outputLayer, new XYcoords(x, y1), new XYcoords(x, y2));
			if (withGap) {
				x -= gap;
				piecesDoc.drawLine(outputLayer, new XYcoords(x, y1), new XYcoords(x, y2));
			}

		}		
	}




	
	
}
