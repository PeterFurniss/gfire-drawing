package uk.co.furniss.draw.piecemaker;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;

import uk.co.furniss.draw.dom.PiecesDocument;
import uk.co.furniss.draw.dom.SvgObject;
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
		float x = 20.0f;
		float y = 20.0f;
		for (List<String> spec : specs) {
			String originalId = spec.get(0);
			SvgObject image = piecesDoc.findSvgObject(originalId);
			if (image == null) {
				throw new IllegalArgumentException("Cannot find image " + spec.get(0) + " in image file");
			}
			SvgObject template = image.clone(originalId + PiecesDocument.TEMPLATE_SUFFIX );
			// if testing move
			//   image.moveTopLeft
			// otherwise these
			template.moveTopLeft();
			piecesDoc.addDefObject(template);
			
			piecesDoc.addCloneOfTemplate(outputLayer, originalId, x, y);
			x += 15.0f;
			y += 20.0f;
			
		}

		piecesDoc.writeToFile(directory + outName + svgSuffix);
        
	}

}
