package uk.co.furniss.draw.piecemaker;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

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

		List<List<String>> specs = tbook.readCellsAsStrings("all", Arrays.asList("image", "topleft", "topright", "botleft", "botright", "botmid"));

		String silhouFile = directory + silhouName + svgSuffix;
		System.out.println("Will read silhouette file " + silhouFile);
		
		PiecesDocument piecesDoc = new PiecesDocument(silhouFile);
		System.out.println(piecesDoc.getLayerNames());
		
		// where are the prototypes ?
		piecesDoc.setLibraryLayer("angular");

		Element outputLayer = piecesDoc.obtainEmptyLayer("output");

		// find the images, copy the element, move to top left and put it in the defs
		// and add a clone to the output layer
		int row = 0;
		int col = 0;
		float pieceSize = 20.0f;
		float imageX = pieceSize / 2.0f;
		float imageY = pieceSize / 2.0f;
		float pieceSpacing = pieceSize;  // can add gap

		float margin = 10.0f;
		int colsPerRow = (int) ((210.0f - 2 * margin) / pieceSize) - 1;
		
		for (List<String> spec : specs) {
			String originalId = spec.get(0);
			if (testing) {
				SvgObject image = piecesDoc.findSvgObject(originalId);
    			if (image == null) {
    				throw new IllegalArgumentException("Cannot find image " + spec.get(0) + " in image file");
    			}
			    image.setCentre(XYcoords.ORIGIN);
			} else {
				String templateName = piecesDoc.ensureTemplate(originalId);
    			float x = margin + col * pieceSpacing + imageX;
    			float y = margin + row * pieceSpacing + imageY;
				
    			piecesDoc.addCloneOfTemplate(outputLayer, templateName, x, y);

    			col++;
    			if (col > colsPerRow) {
    				row++;
    				col = 0;
    			}
			}
		}
		// now some lines round the pieces
		float x1 = margin;
		float x2 = x1 + ( colsPerRow + 1) * pieceSpacing;
		for (int r = 0; r < row + 2; r++) {
			float y = margin + r * pieceSpacing;
			piecesDoc.drawLine(outputLayer, new XYcoords(x1, y), new XYcoords(x2, y));
			
		}
		float y1 = margin;
		float y2 = x1 + ( row + 1) * pieceSpacing;
		for (int c = 0; c < colsPerRow + 2; c++) {
			float x = margin + c * pieceSpacing;
			piecesDoc.drawLine(outputLayer, new XYcoords(x, y1), new XYcoords(x, y2));
			
		}
		
		if (! testing) {
			piecesDoc.hideAllLayersButOne("output");
		}
		piecesDoc.writeToFile(directory + outName + svgSuffix);
        
	}

}
