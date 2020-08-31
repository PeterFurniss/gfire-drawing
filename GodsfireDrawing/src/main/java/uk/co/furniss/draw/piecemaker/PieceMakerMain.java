package uk.co.furniss.draw.piecemaker;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import uk.co.furniss.draw.dom.PiecesDocument;
import uk.co.furniss.draw.dom.SvgObject;
import uk.co.furniss.draw.dom.XYcoords;
import uk.co.furniss.xlsx.ExcelBook;

/**
 * make a sheet of pieces
 *  specifiation file says how many of each, corner numbers, which silhouette and colours
 *  		(that will take som designing).
 * 	piece silhouette is an image from an input svg file. object has a specific name
 */
public class PieceMakerMain {

	

	private static final float PAGE_WIDTH = 210.0f;
	private static final float PAGE_HEIGHT = 297.0f;
	private static final String OUTPUT_LAYER_BASE_NAME = "output";
	private static final String FIRST_OUTPUT_LAYER = OUTPUT_LAYER_BASE_NAME + "1";

	private PieceMakerMain() {
		
	}

	public static void main(String[] args) throws FileNotFoundException {
		String directory = "c:/Users/Peter/Documents/games/barbkings/";
		String svgSuffix = ".svg";
		String specSuffix = ".xlsx";
		String specName = "piece_spec";
		String silhouName = "new_piecest";
		String outName = "madePieces";
		boolean testing = false;
		
		String specFile = directory + specName + specSuffix;
		System.out.println("Will read specification file " + specFile);
		ExcelBook tbook = new ExcelBook(specFile);

		List<Map<String, String>> specs = tbook.readCellsAsStrings("double", Arrays.asList("image", "number", "topleft", "topright",
				"botleft", "botright", "firstId", "back", "fore", "lines"));

		String silhouFile = directory + silhouName + svgSuffix;
		System.out.println("Will read silhouette file " + silhouFile);
		
		PiecesDocument piecesDoc = new PiecesDocument(silhouFile);
		System.out.println(piecesDoc.getLayerNames());
		
		// where are the prototypes ?
		piecesDoc.setLibraryLayer("angular");
		float pieceSize = 20.0f;
		float imageDefinition = 19.0f;
		
		float scaling = pieceSize / imageDefinition;
		boolean scaleImage = true;
		boolean withGap = true;
		float gap = withGap ? 1.0f : 0.0f;
		
		// find the images, copy the element, move to top left and put it in the defs
		// and add a clone to the output layer
		int row = 0;
		int col = 0;
		int page = 1;

		Element outputLayer = piecesDoc.obtainEmptyLayer(OUTPUT_LAYER_BASE_NAME + Integer.toString(page));
		
		float centreX = pieceSize / 2.0f;
		float centreY = pieceSize / 2.0f;
		float picY = centreY - pieceSize * 0.08f;
		float pieceSpacing = pieceSize + gap;  // can add gap
		
		float bottomNumberHeight = 5.0f * scaling;
		float centreNumberHeight = 3.0f * scaling;
		float topNumberHeight = 4.0f * scaling;
		
		float textToEdgeFraction = 0.2f;
		float leftTextX  = pieceSize * textToEdgeFraction;
		float rightTextX = pieceSize * (1.0f - textToEdgeFraction);
		float topTextY   = pieceSize * textToEdgeFraction;
		float botTextY   = pieceSize * (1.0f - textToEdgeFraction);
		float centreTextY = pieceSize * 0.8f;

		float margin = 10.0f;
		int colsPerRow = (int) ((PAGE_WIDTH - 2 * (margin - gap)) / pieceSpacing) - 1;
		int rowsPerPage = (int) ((PAGE_HEIGHT - 2 * (margin - gap)) / pieceSpacing) - 1;
		
		String transformStart = "matrix(" + Float.toString(scaling) + ",0,0," +  Float.toString(scaling) + ",";
		float antiScale = 1.0f - scaling;
		Map<String, Integer> imageTally = new LinkedHashMap<>();
		
		for (Map<String, String> spec : specs) {
			String imageName = spec.get("image");
			if (testing) {
				SvgObject image = piecesDoc.findSvgObject(imageName);
    			if (image == null) {
    				throw new IllegalArgumentException("Cannot find image " + imageName + " in image file");
    			}
			    image.setCentre(XYcoords.ORIGIN);
			} else {
				String templateName = piecesDoc.ensureTemplate(imageName);
				int number = Integer.parseInt(spec.get("number"));
				int idNumber = Integer.parseInt(spec.get("firstId"));
				Integer prev = imageTally.get(imageName);
				if (prev == null) {
					imageTally.put(imageName, number);
				} else {
					imageTally.put(imageName, prev + number);
				}
				
				for (int item = 0; item < number; item++) {
					String idString = Integer.toString(idNumber);
					if (idString.length() == 2) {
						idString = "0" + idString;
					}
				
					String foreColour = spec.get("fore");
					String backColour = spec.get("back");
					// topleft of piece
        			float x = margin + col * pieceSpacing ;
        			float y = margin + row * pieceSpacing ;
    				piecesDoc.makeRectangle(outputLayer, x - gap * 0.5f, y - gap * 0.5f, pieceSpacing, pieceSpacing, backColour);
    				
        			float deltaX = x + centreX;
					float deltaY = y + picY;
					Element pic = piecesDoc.addCloneOfTemplate(outputLayer, templateName, deltaX, deltaY);
        			if (spec.get("lines").equalsIgnoreCase("true")) {
        				pic.setAttribute("stroke", foreColour);
        				pic.setAttribute("stroke-width", "0.45px");
        				pic.setAttribute("fill", "none");
        			} else {
        				pic.setAttribute("fill", foreColour);
        			}
        			if (scaleImage) {
        				pic.setAttribute("transform", transformStart  + Float.toString(deltaX * antiScale) 
        					+ "," + Float.toString(deltaY * antiScale) + ")");
        			}
       			
        			piecesDoc.addText(outputLayer, spec.get("topleft"), topNumberHeight,   x + leftTextX, y + topTextY, false, foreColour );
        			piecesDoc.addText(outputLayer, spec.get("topright"), topNumberHeight, x + rightTextX, y + topTextY, false, foreColour) ;
    
        			piecesDoc.addText(outputLayer, idString, centreNumberHeight, x + centreX, y + centreTextY, false, foreColour );
    
        			piecesDoc.addText(outputLayer, spec.get("botleft"), bottomNumberHeight,   x + leftTextX,  y + botTextY, true, foreColour );
        			piecesDoc.addText(outputLayer, spec.get("botright"), bottomNumberHeight,   x + rightTextX,  y + botTextY, true, foreColour );
        			
        			col++;
        			idNumber++;
        			if (col > colsPerRow) {
        				row++;
        				col = 0;
        				if (row > rowsPerPage) {
        			   		drawFiducialLines(piecesDoc, withGap, gap, row - 1, outputLayer, pieceSpacing, margin, colsPerRow);
        					row = 0;
        					page++;
        					outputLayer = piecesDoc.obtainEmptyLayer(OUTPUT_LAYER_BASE_NAME + Integer.toString(page));
        				}
        			}
				}
			}
		}
		
		if (! testing) {
    		// now some lines round the pieces
    		drawFiducialLines(piecesDoc, withGap, gap, row, outputLayer, pieceSpacing, margin, colsPerRow);

    		for (Map.Entry<String, Integer> tally : imageTally.entrySet()) {
				String imageName = tally.getKey();
				Integer count = tally.getValue();
				System.out.println("   " + count + " of " + imageName);
				
			}
    		System.out.println("Created " + page + " pages of pieces with size " + pieceSize + "mm");
			piecesDoc.hideAllLayersButOne(FIRST_OUTPUT_LAYER);
		}
		piecesDoc.writeToFile(directory + outName + svgSuffix);
        
	}

	public static void drawFiducialLines( PiecesDocument piecesDoc, boolean withGap, float gap, int row,
	        Element outputLayer, float pieceSpacing, float margin, int colsPerRow ) {
		float x1 = margin - gap;
		float x2 = x1 + ( colsPerRow + 1) * pieceSpacing + gap;
		for (int r = 0; r < row + 2; r++) {
			float y = margin + r * pieceSpacing;
			piecesDoc.drawLine(outputLayer, new XYcoords(x1, y), new XYcoords(x2, y));
			if (withGap) {
				y -= gap;
				piecesDoc.drawLine(outputLayer, new XYcoords(x1, y), new XYcoords(x2, y));
			}
			
		}
		float y1 = margin - gap;
		float y2 = x1 + ( row + 1) * pieceSpacing + gap;
		for (int c = 0; c < colsPerRow + 2; c++) {
			float x = margin + c * pieceSpacing;
			piecesDoc.drawLine(outputLayer, new XYcoords(x, y1), new XYcoords(x, y2));
			if (withGap) {
				x -= gap;
				piecesDoc.drawLine(outputLayer, new XYcoords(x, y1), new XYcoords(x, y2));
			}
			
		}
	}
	


}
