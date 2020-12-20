package uk.co.furniss.draw.piecemaker;

import java.util.Map;

import org.w3c.dom.Element;

import uk.co.furniss.draw.dom.PiecesDocument;
import uk.co.furniss.draw.dom.XYcoords;

// draw cube nets
public class FullPageCubeArranger implements CubeArranger {
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
	
	//  pieceSize is the edge of the cube
	//  gap only applies between the cubes
	
	public FullPageCubeArranger(float pieceSize, float gap) {
		this.pieceSize = pieceSize;
		this.gap = gap;
		horizSpacing = pieceSize * 5 + gap;
		vertSpacing  = pieceSize * 2 + gap;
		
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
		int cubeCol = currentCol;
		int cubeRow = currentRow;
		return getCubeLocation(cubeRow, cubeCol);
	}

	public XYcoords getCubeLocation( int cubeRow, int cubeCol ) {
		return new XYcoords( MARGIN + cubeCol * horizSpacing, MARGIN + cubeRow * vertSpacing);
	}

	/* (non-Javadoc)
	 * @see uk.co.furniss.draw.piecemaker.CubeArranger#getFaceLocation(uk.co.furniss.draw.piecemaker.CubeFace)
	 */
	@Override
	public XYcoords getFaceLocation(CubeFace face) {
		return getCurrentLocation().add(face.getOffset(pieceSize));
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
				// horizontal lines
				XYcoords start = cubeLocation.deltaX( -extra);
				XYcoords end   = cubeLocation.deltaX( 3 * pieceSize + 2.0f);
				piecesDoc.drawLine(outputLayer, start, end);
				
				start = start.deltaY(pieceSize);
				end =     end.deltaY(pieceSize).deltaX(2 * pieceSize);
				piecesDoc.drawLine(outputLayer, start, end);
				
				start = start.deltaY(pieceSize).deltaX(2 * pieceSize);
				end   =   end.deltaY(pieceSize);
				piecesDoc.drawLine(outputLayer, start, end);
				
				// vertical lines
				start = cubeLocation.deltaY(-extra);
				end   = cubeLocation.deltaY(pieceSize + extra);
				piecesDoc.drawLine(outputLayer, start, end);
	
				start = start.deltaX(pieceSize);
				end   =   end.deltaX(pieceSize);
				piecesDoc.drawLine(outputLayer, start, end);
				
				start = start.deltaX(pieceSize);
				end   =   end.deltaX(pieceSize).deltaY(pieceSize);
				piecesDoc.drawLine(outputLayer, start, end);
				
				start = start.deltaX(pieceSize);
				end   =   end.deltaX(pieceSize);
				piecesDoc.drawLine(outputLayer, start, end);
				
				start = start.deltaX(pieceSize).deltaY(pieceSize);
				end   =   end.deltaX(pieceSize);
				piecesDoc.drawLine(outputLayer, start, end);
				
				start = start.deltaX(pieceSize);
				end   =   end.deltaX(pieceSize);
				piecesDoc.drawLine(outputLayer, start, end);
				
			}
		}
	}

	@Override
	public void setGroup( Map<String, String> specRow ) {
		// do nothing
		
	}

}
