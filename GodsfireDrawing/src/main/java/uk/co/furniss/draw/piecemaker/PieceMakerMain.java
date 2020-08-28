package uk.co.furniss.draw.piecemaker;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

import uk.co.furniss.draw.dom.SVGdocument;
import uk.co.furniss.draw.dom.SvgObject;
import uk.co.furniss.draw.gfmap.GfMap;
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

		List<List<String>> defns = tbook.readCellsAsStrings("all", Arrays.asList("image", "topleft", "topright", "botleft", "botright", "botmid"));

		String silhouFile = directory + silhouName + svgSuffix;
		System.out.println("Will read silhouette file " + silhouFile);
		
		SVGdocument images = new SVGdocument(silhouFile);
		System.out.println(images.getLayerNames());
		
		images.setLibraryLayer("angular");
		for (List<String> defn : defns) {
			SvgObject image = images.findSvgObject(defn.get(0));
			if (image == null) {
				throw new IllegalArgumentException("Cannot find image " + defn.get(0) + " in image file");
			}
			image.moveTopLeft();
		}

		images.writeToFile(directory + outName + svgSuffix);
        
	}

}
