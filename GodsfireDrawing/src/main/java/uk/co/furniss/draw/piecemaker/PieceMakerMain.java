package uk.co.furniss.draw.piecemaker;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import uk.co.furniss.draw.dom.PiecesDocument;
import uk.co.furniss.draw.dom.SvgObject;
import uk.co.furniss.draw.dom.XYcoords;
import uk.co.furniss.xlsx.ExcelBook;

/**
 * make a sheet of pieces specifiation file says how many of each, corner
 * numbers, which silhouette and colours (that will take som designing). piece
 * silhouette is an image from an input svg file. object has a specific name
 */
public class PieceMakerMain {

	private static final float PAGE_WIDTH = 210.0f;
	private static final float PAGE_HEIGHT = 297.0f;
	private static final String OUTPUT_LAYER_BASE_NAME = "output";
	private static final String FIRST_OUTPUT_LAYER = OUTPUT_LAYER_BASE_NAME + "1";

	private static final Logger LOGGER = LoggerFactory.getLogger(PieceMakerMain.class.getName());

	private PieceMakerMain() {

	}

	public static void main( String[] args ) throws FileNotFoundException {
		final String directory;
		final String specName;
		String game = "gf";
		boolean testing = false;

		if (game.equals("gf")) {
			directory = "c:/Users/Peter/Documents/games/piece_maker/godsfire/";
			specName = "gf_spec";
		} else if (game.equals("bk")) {

			directory = "c:/Users/Peter/Documents/games/piece_maker/barbkings/";
			specName = "bk_spec";
		} else {
			throw new IllegalArgumentException("Which game are making pieces for ?  we have" + game);
		}

		String svgSuffix = ".svg";
		String specSuffix = ".xlsx";

		String specFile = directory + specName + specSuffix;
		System.out.println("Will read specification file " + specFile);
		ExcelBook tbook = new ExcelBook(specFile);

		List<Map<String, String>> metaspec = tbook.readCellsAsStrings(specName, Arrays.asList("column", // columns of
		                                                                                                // the spec
		                                                                                                // sheet
		        "specsheet", // this, down to gap, are parameters, don't belong here
		        "imagefile", "imagelayer", "imagesize", "outputfile", "piecesize", "gap", "x%", "y%", // centre of this
		                                                                                              // field in the
		                                                                                              // piece (from top
		                                                                                              // left)
		        "justify", // iff this is text, left, centre, right justification as l, c, r. default c
		        "fontsize", // iff this is text, fontsize in original
		        "fontfamily", // defaults to sans serif
		        "fontmod", // bold, italic etc
		        "orientation", // defaults to zero - normal left->right
		        "increment")); // this value is incremented for multiple pieces of same spec

		List<String> colNames = metaspec.stream().map(c -> c.get("column")).collect(Collectors.toList());

		Map<String, String> parameters = metaspec.get(0);

		String silhouFile = directory + parameters.get("imagefile") + svgSuffix;
		System.out.println("Will read image file " + silhouFile);

		PiecesDocument piecesDoc = new PiecesDocument(silhouFile);

		// where are the prototypes ?
		piecesDoc.setLibraryLayer(parameters.get("imagelayer"));

		float imageDefinition = Float.parseFloat(parameters.get("imagesize"));
		float pieceSize = Float.parseFloat(parameters.get("piecesize"));

		float scaling = pieceSize / imageDefinition;
		float gap = Float.parseFloat(parameters.get("gap"));
		boolean withGap = gap > 0.01f;

		String outName = parameters.get("outputfile");

		// find the images, copy the element, move to top left and put it in the defs
		// and add a clone to the output layer
		int row = 0;
		int col = 0;
		int page = 1;

		Element outputLayer = piecesDoc.obtainEmptyLayer(OUTPUT_LAYER_BASE_NAME + Integer.toString(page));

		float pieceSpacing = pieceSize + gap; // can add gap

		List<ImageField> imageFields = new ArrayList<>();
		List<TextField> textFields = new ArrayList<>();

		for (Map<String, String> fieldDefn : metaspec) {
			String name = fieldDefn.get("column");
			if (name.startsWith("image")) {
				imageFields.add(new ImageField(fieldDefn, pieceSize, scaling));
			} else if (fieldDefn.get("fontsize") != "") {
				textFields.add(new TextField(fieldDefn, pieceSize, scaling));
			} else {
				// other columns are general or apply to the image
			}

		}
		List<String> incrementingFields = textFields.stream().filter(TextField::isIncrement).map(TextField::getName)
		        .collect(Collectors.toList());

		// so far, all the images and writing are the same foreground colour

		float margin = 10.0f;
		int colsPerRow = (int) ( ( PAGE_WIDTH - 2 * ( margin - gap ) ) / pieceSpacing ) - 1;
		int rowsPerPage = (int) ( ( PAGE_HEIGHT - 2 * ( margin - gap ) ) / pieceSpacing ) - 1;

		String transformStart = "matrix(" + Float.toString(scaling) + ",0,0," + Float.toString(scaling) + ",";
		float antiScale = 1.0f - scaling;
		Map<String, Integer> imageTally = new LinkedHashMap<>();
		int totalPieces = 0;

		List<Map<String, String>> specs = tbook.readCellsAsStrings(parameters.get("specsheet"), colNames);
		String oldFore = "white";
		String oldBack = "black";
		for (Map<String, String> spec : specs) {
			if (testing) {

				for (ImageField imageField : imageFields) {
					String imageName = spec.get(imageField.getName());
					if (!imageTally.containsKey(imageName)) {
						SvgObject image = piecesDoc.findSvgObject(imageName);
						if (image == null) {
							throw new IllegalArgumentException("Cannot find image " + imageName + " in image file");
						}
						// SvgObject copy = image.clone(imageName + "_copyA");
						// SvgObject copyB = image.clone(imageName + "_copyB");

						// image.setCentre(image.getCentre().add(new XYcoords(20.0f, 20.0f)));
						LOGGER.debug("internalising original");
						image.internaliseTransformation();
						// LOGGER.debug("scaling original {} by 1.5", imageName);
						// image.scale(new Transform("scale(1.5)"));
						// LOGGER.debug("moving copyA of {} +30. +10 ",imageName);
						// copy.move(new XYcoords(30.0f,10.0f));
						//// clone.setCentre(XYcoords.ORIGIN);
						// outputLayer.appendChild(copy.getElement());
						//
						// LOGGER.debug("get centre of copyB");
						// XYcoords ctr = copyB.getCentre();
						// copyB.move(ctr.minus());
						//// copyB.internaliseTransformation();
						//// copyB.move(ctr);
						// outputLayer.appendChild(copyB.getElement());
						imageTally.put(imageName, 1);
					}
				}
			} else {
				int number = getAsInteger("number", spec);
				for (ImageField imageField : imageFields) {
					String imageName = spec.get(imageField.getName());

					String templateName = piecesDoc.ensureTemplate(imageName);
					imageField.setTemplateName(templateName);

					Integer prev = imageTally.get(imageName);
					if (prev == null) {
						imageTally.put(imageName, number);
					} else {
						imageTally.put(imageName, prev + number);
					}
				}
				Map<String, Integer> incrementers = new HashMap<>();
				for (String incrementer : incrementingFields) {
					incrementers.put(incrementer, getAsInteger(incrementer, spec));
				}

				for (int item = 0; item < number; item++) {

					String foreColour = spec.get("fore");
					String backColour = spec.get("back");
					if (foreColour.equals("")) {
						foreColour = oldFore;
					}
					if (backColour.equals("")) {
						backColour = oldBack;
					}
					oldFore = foreColour;
					oldBack = backColour;
					// topleft of piece
					float x = margin + col * pieceSpacing;
					float y = margin + row * pieceSpacing;
					// do the background
					piecesDoc.makeRectangle(outputLayer, x - gap * 0.5f, y - gap * 0.5f, pieceSpacing, pieceSpacing,
					        backColour);

					for (ImageField imageField : imageFields) {

						float deltaX = x + imageField.getXoffset();
						float deltaY = y + imageField.getYoffset();
						Element pic = piecesDoc.addCloneOfTemplate(outputLayer, imageField.getTemplateName(), deltaX,
						        deltaY);
						pic.setAttribute("fill", foreColour);

						pic.setAttribute("transform", transformStart + Float.toString(deltaX * antiScale) + ","
						        + Float.toString(deltaY * antiScale) + ")");
					}
					for (TextField tf : textFields) {
						String name = tf.getName();
						final String text;
						if (tf.isIncrement()) {
							Integer value = incrementers.get(name);
							String idString = Integer.toString(value);
							if (idString.length() == 2) {
								idString = "0" + idString;
							}
							text = idString;
							incrementers.put(name, ++value);
						} else {
							text = spec.get(name);
						}
						if (text.length() > 0) {
							piecesDoc.addText(outputLayer, text, tf.getFontSize(), 
									new XYcoords(x + tf.getXoffset(), y + tf.getYoffset()),
									tf.getFontmod(), foreColour, Justification.valueOf(tf.getJustification()), "");
						}
					}
					col++;
					if (col > colsPerRow) {
						row++;
						col = 0;
						if (row > rowsPerPage) {
							drawFiducialLines(piecesDoc, withGap, gap, row - 1, outputLayer, pieceSpacing, margin,
							        colsPerRow);
							row = 0;
							page++;
							outputLayer = piecesDoc.obtainEmptyLayer(OUTPUT_LAYER_BASE_NAME + Integer.toString(page));
						}
					}
					totalPieces++;
				}
			}
		}

		if (!testing) {
			// now some lines round the pieces
			drawFiducialLines(piecesDoc, withGap, gap, row, outputLayer, pieceSpacing, margin, colsPerRow);

			for (Map.Entry<String, Integer> tally : imageTally.entrySet()) {
				String imageName = tally.getKey();
				Integer count = tally.getValue();
				System.out.println("   " + count + " of " + imageName);

			}
			System.out
			        .println("Created " + page + " pages of " + totalPieces + " pieces with size " + pieceSize + "mm");
			piecesDoc.hideAllLayersButOne(FIRST_OUTPUT_LAYER);
		}
		piecesDoc.writeToFile(directory + outName + svgSuffix);

	}

	private static class ImageField {
		private final String name;
		private final float xOffset;
		private final float yOffset;
		private String templateName; // set late

		ImageField(Map<String, String> defn, float pieceSize, float scaling) {
			name = defn.get("column");
			xOffset = pieceSize * Integer.parseInt(defn.get("x%")) / 100.0f;
			yOffset = pieceSize * Integer.parseInt(defn.get("y%")) / 100.0f;
		}

		public void setTemplateName( String templateName ) {
			this.templateName = templateName;

		}

		public String getTemplateName() {
			return templateName;
		}

		public String getName() {
			return name;
		}

		public float getXoffset() {
			return xOffset;
		}

		public float getYoffset() {
			return yOffset;
		}
	}

	private static class TextField {

		private final String name;
		private final float xOffset;
		private final float yOffset;
		private final float fontSize;
		private final String fontfamily;
		private final String fontmod;
		private final String justify;
		private final String orientation; // should be enum
		private final boolean increment;

		TextField(Map<String, String> defn, float pieceSize, float scaling) {
			name = defn.get("column");
			xOffset = pieceSize * Integer.parseInt(defn.get("x%")) / 100.0f;
			yOffset = pieceSize * Integer.parseInt(defn.get("y%")) / 100.0f;
			fontSize = Float.parseFloat(defn.get("fontsize")) * scaling;
			// these will be "" due to behaviour of excel reading
			fontfamily = defn.get("fontfamily");
			fontmod = defn.get("fontmod");
			justify = defn.get("justify") != null ? defn.get("justify").toUpperCase() : "C";
			orientation = defn.get("orientation");
			String incrementStr = defn.get("increment");
			increment = incrementStr != null && incrementStr.equalsIgnoreCase("TRUE");

		}

		public String getName() {
			return name;
		}

		public float getXoffset() {
			return xOffset;
		}

		public float getYoffset() {
			return yOffset;
		}

		public float getFontSize() {
			return fontSize;
		}

		public String getFontfamily() {
			return fontfamily;
		}

		public String getFontmod() {
			return fontmod;
		}

		public String getJustification() {
			return justify;
		}

		public String getOrientation() {
			return orientation;
		}

		public boolean isIncrement() {
			return increment;
		}
	}

	public static int getAsInteger( String key, Map<String, String> spec ) {
		String asString = spec.get(key);
		try {
			return Integer.parseInt(asString);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Non-integer for " + key + " in " + spec, e);
		}
	}

	public static void drawFiducialLines( PiecesDocument piecesDoc, boolean withGap, float gap, int row,
	        Element outputLayer, float pieceSpacing, float margin, int colsPerRow ) {
		float x1 = margin - gap;
		float x2 = x1 + ( colsPerRow + 1 ) * pieceSpacing + gap;
		for (int r = 0; r < row + 2; r++) {
			float y = margin + r * pieceSpacing;
			piecesDoc.drawLine(outputLayer, new XYcoords(x1, y), new XYcoords(x2, y));
			if (withGap) {
				y -= gap;
				piecesDoc.drawLine(outputLayer, new XYcoords(x1, y), new XYcoords(x2, y));
			}

		}
		float y1 = margin - gap;
		float y2 = x1 + ( row + 1 ) * pieceSpacing + gap;
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
