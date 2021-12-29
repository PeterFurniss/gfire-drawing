package uk.co.furniss.draw.piecemaker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import uk.co.furniss.draw.dom.PiecesDocument;
import uk.co.furniss.draw.dom.XYcoords;

class BattaliaArranger implements PieceArranger {

	private static final float PAGE_WIDTH = 210.0f;
	private static final float PAGE_HEIGHT = 297.0f;
	private static final float MARGIN = 10.0f;
	private static final String OUTPUT_LAYER_BASE_NAME = "output";
	private static final String FIRST_OUTPUT_LAYER = OUTPUT_LAYER_BASE_NAME + "1";

	private final float pieceSize;
	private final int rowsPerPage;
	private final int colsPerRow;
//	private final float piecePixels;
	private int pieceNumber;

	private int pageNumber;
	private int currentRow;
	private int groupCol;

	private PiecesDocument piecesDoc;

	private Element outputLayer;
	private SvgWriter writer;
	private int indexInGroup;
	private boolean justStarted;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(BattaliaArranger.class.getName());
	
	public BattaliaArranger(float pieceSize) {
		this.pieceSize = pieceSize;
		colsPerRow = (int) ( ( PAGE_WIDTH - 2 * ( MARGIN  ) ) / pieceSize ) ;
		rowsPerPage = (int) ( ( PAGE_HEIGHT - 2 * ( MARGIN  ) ) / pieceSize ) ;
//		piecePixels = pieceSize / 0.264583f;
		pageNumber = -1;
		currentRow = 0;
		groupCol = 0;
		pieceNumber = 0;
	}

	private static final Pattern LAYOUT_PATTERN = Pattern.compile("(\\d+),(\\d+)");
	
	@Override
	public void setGroup( Map<String, String> specRow ) {
		String layout = specRow.get("Layout");
		if (layout == null) {
			throw new IllegalArgumentException("No column 'Layout' found in definition sheet");
		}
		Matcher layMatch = LAYOUT_PATTERN.matcher(layout);
		if (specRow.get("number").equals ("#")) {
			String title = specRow.get("name1").trim();
			if (title == null) {
				throw new IllegalArgumentException("No column 'name1' found in definition sheet");
			}
			String textColour = specRow.get("textcol");
			if (textColour == null) {
				textColour = specRow.get("fore");
			}
			if (textColour.equals("")) {
				textColour = "black";
			}
			if (layout.equalsIgnoreCase("page")) {
				if (! justStarted) {
					newOutputPage();
				} else {
					justStarted = false;
				}
				currentRow = 0;
				groupCol = 0;
				
				if ( ! title.equals("")) {
					XYcoords textCentre = location(0, colsPerRow / 2).deltaY(3.0f);
					
					piecesDoc.addText(outputLayer, title, 7, 
								textCentre,
								"bold", textColour, 
						        Justification.CENTRE, "");
					currentRow++;
				}
			} else {
				// group label
				if (layMatch.matches()) {
					currentRow = Integer.parseInt(layMatch.group(1));
					groupCol = Integer.parseInt(layMatch.group(2));
					XYcoords textLeft = location(currentRow, groupCol).deltaY(-2.0f);
					piecesDoc.addText(outputLayer, title, 5, 
							textLeft,
							"bold", textColour, 
					        Justification.LEFT, "");
					currentRow--;
				} else {
					LOGGER.warn("Failed to match layout {} for {}", layout, title);
				}
			}
			
		} else {
			if (! layout.equals("")) {
				LOGGER.debug("we have {} for {}", layout, specRow.get("name1").trim());
			}
    		currentRow++;
    		if (currentRow >= rowsPerPage) {
    			newOutputPage();
    			currentRow = 0;
    		}
    		indexInGroup = -1;
    		if (layMatch.matches()) {
    			currentRow += Integer.parseInt(layMatch.group(1));
   				indexInGroup += Integer.parseInt(layMatch.group(2));
    		}
    		// write name, in appropriate size
    		List<String> names = new ArrayList<>();
    		for (int i=1; i < 8; i++) {
    			String name;
				name = specRow.get("name" + Integer.toString(i));
    			if (name == null || name.trim().equals("") ) {
    				break;
    			}
    			names.add(name);
    		}
    		int nNames = names.size();
    		if (nNames > 0) {
    			LOGGER.debug("names are {}", names);
    			float lineSize = pieceSize / (nNames + 1);
    			float fontSize = (nNames < 3)? (pieceSize / 4) : (pieceSize / 5) ;
    			XYcoords textBase = location(currentRow, groupCol + Integer.parseInt(specRow.get("number")) + indexInGroup + 1)
    					.deltaX(pieceSize/4).deltaY(lineSize);
    			for (String name : names) {
    				piecesDoc.addText(outputLayer, name, fontSize, 
    						textBase,
    						"", "black", 
    				        Justification.LEFT, "");
    				textBase = textBase.deltaY(lineSize);
    			}
    			
    		}
		}
		
	}

	private XYcoords location(int row, int col) {
		return new XYcoords( MARGIN + col * pieceSize, MARGIN + row * pieceSize);
	}
	
	/* (non-Javadoc)
	 * @see uk.co.furniss.draw.piecemaker.PageArranger#getNextLocation()
	 */
	@Override
	public XYcoords getNextLocation() {
		indexInGroup++;
		int currentCol = indexInGroup + groupCol;
		return location(currentRow, currentCol);
	}

	/* (non-Javadoc)
	 * @see uk.co.furniss.draw.piecemaker.PageArranger#start(uk.co.furniss.draw.piecemaker.SvgWriter)
	 */
	@Override
	public void start(SvgWriter writer) {
		this.writer = writer;
		this.piecesDoc = writer.getOutputDocument();
		newOutputPage();
		justStarted = true;
	}

	private void newOutputPage() {
		pageNumber++;
		outputLayer = piecesDoc.obtainEmptyLayer(OUTPUT_LAYER_BASE_NAME + Integer.toString(pageNumber+1));
		writer.setOutputLayer(outputLayer);
	}


	protected int findNumberInPage(int actualPiecesPerPage) {
		int page = pieceNumber / actualPiecesPerPage;

		if (page != pageNumber) {
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
		hideOtherLayers();
	}


	private void hideOtherLayers() {
		piecesDoc.hideAllLayersButOne(FIRST_OUTPUT_LAYER);
	}
	
	@Override
	public int getPageCount() {
		return pageNumber + 1;
	}
	
	
}
