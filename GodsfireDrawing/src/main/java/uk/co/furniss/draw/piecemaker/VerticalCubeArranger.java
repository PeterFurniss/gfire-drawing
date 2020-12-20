package uk.co.furniss.draw.piecemaker;

import java.util.Map;

import org.w3c.dom.Element;

import uk.co.furniss.draw.dom.PiecesDocument;
import uk.co.furniss.draw.dom.XYcoords;

// draw cube nets
public class VerticalCubeArranger implements CubeArranger {
	// count is for cubes, which are laid out as
	//   top 

	private static final float PAGE_WIDTH = 210.0f;
	private static final float PAGE_HEIGHT = 297.0f;
	private static final float MARGIN = 10.0f;
	private static final String OUTPUT_LAYER_BASE_NAME = "output";
	private static final String FIRST_OUTPUT_LAYER = OUTPUT_LAYER_BASE_NAME + "1";

	protected final float gap;
	private final int rowsPerPage;
	private final int colsPerRow;
	private final int piecesPerPage;
	protected int pieceNumber;
	protected int pageNumber;
	private int currentRow;
	protected PiecesDocument piecesDoc;
	protected Element outputLayer;
	private SvgWriter writer;
	private final float horizSpacing;
	private final float vertSpacing;
	private int currentCol;
	private float pieceSize;
	private float evenExtra;
	
	//  pieceSize is the edge of the cube
	//  gap only applies between the cubes
	
	public VerticalCubeArranger(float pieceSize, float gap) {
		this.pieceSize = pieceSize;
		// non-interlocking
		this.gap = gap;
		horizSpacing = pieceSize * 2 + gap;
		vertSpacing  = pieceSize * 5 + gap;
	
//		// interlocking - this isn't right.  needs a longer cycle and must ensure first and last are still
//		// inbounds, which changes number per page
//		this.evenExtra = pieceSize / 2;
//		this.gap = 0;
//		horizSpacing = pieceSize * 2 + gap + evenExtra;
//		vertSpacing  = pieceSize * 3.5f + gap;

		colsPerRow = (int) ( ( PAGE_WIDTH - 2 * ( MARGIN - gap ) ) / horizSpacing ) ;
		rowsPerPage = (int) ( ( PAGE_HEIGHT - 2 * ( MARGIN - gap ) ) / vertSpacing ) ;
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

	public void newOutputPage() {
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
		currentCol = numberOnPage % colsPerRow;
		return getCurrentLocation();
	}

	public XYcoords getCurrentLocation() {
		return getCubeLocation(currentRow, currentCol);
	}

	public XYcoords getCubeLocation( int cubeRow, int cubeCol ) {
		return new XYcoords( MARGIN + cubeCol * horizSpacing + ( cubeRow % 2 == 0 ? evenExtra : 0), MARGIN + cubeRow * vertSpacing);
	}

	/* (non-Javadoc)
	 * @see uk.co.furniss.draw.piecemaker.CubeArranger#getFaceLocation(uk.co.furniss.draw.piecemaker.CubeFace)
	 */
	@Override
	public XYcoords getFaceLocation(CubeFace face) {
		return getCurrentLocation().add(face.getOffsetVertical(pieceSize));
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

	public void hideOtherLayers() {
		piecesDoc.hideAllLayersButOne(FIRST_OUTPUT_LAYER);
	}
	
	@Override
	public int getPageCount() {
		return pageNumber + 1;
	}
	
	protected void drawFiducialLines() {
		// row and cols counts are in cubes, not faces
		float extra = 2.0f;
		for (int r = 0; r <= currentRow ; r++) {
			for (int c = 0; c < colsPerRow ; c++) {
				XYcoords cubeLocation = getCubeLocation(r, c);
				// vertical lines
				XYcoords start = cubeLocation.deltaY( -extra);
				XYcoords end   = cubeLocation.deltaY( 3 * pieceSize + 2.0f);
				piecesDoc.drawLine(outputLayer, start, end);
				
				start = start.deltaX(pieceSize);
				end =     end.deltaX(pieceSize).deltaY(2 * pieceSize);
				piecesDoc.drawLine(outputLayer, start, end);
				
				start = start.deltaX(pieceSize).deltaY(2 * pieceSize);
				end   =   end.deltaX(pieceSize);
				piecesDoc.drawLine(outputLayer, start, end);
				
				// horizontal lines
				start = cubeLocation.deltaX(-extra);
				end   = cubeLocation.deltaX(pieceSize + extra);
				piecesDoc.drawLine(outputLayer, start, end);
	
				start = start.deltaY(pieceSize);
				end   =   end.deltaY(pieceSize);
				piecesDoc.drawLine(outputLayer, start, end);
				
				start = start.deltaY(pieceSize);
				end   =   end.deltaY(pieceSize).deltaX(pieceSize);
				piecesDoc.drawLine(outputLayer, start, end);
				
				start = start.deltaY(pieceSize);
				end   =   end.deltaY(pieceSize);
				piecesDoc.drawLine(outputLayer, start, end);
				
				start = start.deltaY(pieceSize).deltaX(pieceSize);
				end   =   end.deltaY(pieceSize);
				piecesDoc.drawLine(outputLayer, start, end);
				
				start = start.deltaY(pieceSize);
				end   =   end.deltaY(pieceSize);
				piecesDoc.drawLine(outputLayer, start, end);
				
			}
		}
	}

	@Override
	public void setGroup( Map<String, String> specRow ) {
		// do nothing
		
	}

}
