package uk.co.furniss.draw.piecemaker;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
public class PieceMakerMainBox {

	private static final String PARAM_DEFNID = "defnid";
	private static final String PARAM_FIELDSHEET = "fieldsheet";
	private static final String PARAM_SPECSHEET = "specsheet";
	private static final String PARAM_OUTPUTFILE = "outputfile";
	private static final String PARAM_IMAGEFILE = "imagefile";
	private static final String PARAM_IMAGELAYER = "imagelayer";
	private static final String PARAM_IMAGESIZE = "imagesize";
	private static final String PARAM_PIECESIZE = "piecesize";
	private static final String PARAM_GAP = "gap";
	
	private static final String FIELD_NAME_COL = "field";
	private static final String FIELD_TYPE_COL = "type";
	static final String FIELD_PARENTBOX_COL = "parentbox";
	static final String FIELD_JUSTIFY_COL = "justify";
	private static final String FIELD_FONTSIZE_COL = "fontsize";
	private static final String FIELD_FONTFAMILY_COL = "fontfamily";
	private static final String FIELD_FONTMOD_COL = "fontmod";
	private static final String FIELD_ORIENTATION_COL = "orientation";
	private static final String FIELD_INCREMENT_COL = "increment";
	
	private static final float PAGE_WIDTH = 210.0f;
	private static final float PAGE_HEIGHT = 297.0f;
	private static final String OUTPUT_LAYER_BASE_NAME = "output";
	private static final String FIRST_OUTPUT_LAYER = OUTPUT_LAYER_BASE_NAME + "1";

	private static final Logger LOGGER = LoggerFactory.getLogger(PieceMakerMainBox.class.getName());
	private static final String SVG_SUFFIX = ".svg";
	private static final String EXCEL_SUFFIX = ".xlsx";
	private static final String PARAM_DIRECTORY = "directory";

	public static void main( String[] args ) throws FileNotFoundException {
		final String directory;
		final String specFileName;
		final String specSheetName;
		String game = "gf";
		boolean testing = false;

		if (game.equals("gf")) {
			directory = "c:/Users/Peter/Documents/games/piece_maker/godsfire/";
			specFileName = "gf_spec";
			specSheetName = "new_param";
		} else if (game.equals("bk")) {

			directory = "c:/Users/Peter/Documents/games/piece_maker/barbkings/";
			specFileName = "bk_spec";
			specSheetName = "bk_spec";
		} else {
			throw new IllegalArgumentException("Which game are making pieces for ?  we have" + game);
		}

		String specFile = directory + specFileName + EXCEL_SUFFIX;
		System.out.println("Will read specification file " + specFile);
		ExcelBook tbook = new ExcelBook(specFile);
		
		List<Map<String, String>> specParams = tbook.readCellsAsStrings(specSheetName, Arrays.asList(
				PARAM_DEFNID, // allow multiple sets of params
		        PARAM_FIELDSHEET, // where the field definitions are
		        PARAM_SPECSHEET, // says what pieces to make
		        PARAM_IMAGEFILE, // svg file containing images
		        PARAM_IMAGELAYER, // layer in imagefile with the images
		        PARAM_IMAGESIZE, // size of images as in imagefile
		        PARAM_OUTPUTFILE, // where to write the piece pictures
		        PARAM_PIECESIZE, // how big to make the pieces
		        PARAM_GAP

		));
		
		Map<String, String> parameters = specParams.get(0);
		parameters.put(PARAM_DIRECTORY, directory);
		
		
		PieceMakerMainBox instance = new PieceMakerMainBox(parameters, tbook, testing);

	}

	private final Map<String, String> parameters;
	private final String imageFile;
	private final PiecesDocument piecesDoc;
	private final float pieceSpacing;
	private final List<ImageField> imageFields = new ArrayList<>();
	private final List<TextField> textFields = new ArrayList<>();
	private final TallyCounter imageTally = new TallyCounter();
	private static final float MARGIN = 10.0f;
	private List<String> fieldNames;
	private final float scaling;

	private PieceMakerMainBox( Map<String, String> parameters, ExcelBook tbook, boolean testing ) {
		
		this.parameters = parameters;
		
		String defnSheet = parameters.get(PARAM_FIELDSHEET);

		List<Map<String, String>> fieldDefinitions = tbook.readCellsAsStrings(defnSheet, Arrays.asList(
				FIELD_NAME_COL, // columns of the spec  sheet
				FIELD_TYPE_COL,   // image, colour, text,number
		        FIELD_PARENTBOX_COL, // surrounding box to define this image/textfield position
		        FIELD_JUSTIFY_COL, // which point of image/text to key on (N, NE, E ... or C)
		        FIELD_FONTSIZE_COL, // iff this is text, fontsize in original
		        FIELD_FONTFAMILY_COL, // defaults to sans serif
		        FIELD_FONTMOD_COL, // bold, italic etc
		        FIELD_ORIENTATION_COL, // defaults to zero - normal left->right
		        FIELD_INCREMENT_COL)); // this value is incremented for multiple pieces of same spec

		fieldNames = fieldDefinitions.stream().map(c -> c.get(FIELD_NAME_COL)).collect(Collectors.toList());

		imageFile = parameters.get(PARAM_DIRECTORY) + parameters.get(PARAM_IMAGEFILE) + SVG_SUFFIX;
		System.out.println("Will read image file " + imageFile);

		piecesDoc = new PiecesDocument(imageFile);

		// where are the prototype images ?  - do we really need this ?
		piecesDoc.setLibraryLayer(parameters.get(PARAM_IMAGELAYER));

		float imageDefinitionSize = Float.parseFloat(parameters.get(PARAM_IMAGESIZE));
		float pieceSize = Float.parseFloat(parameters.get(PARAM_PIECESIZE));

		scaling = pieceSize / imageDefinitionSize;
		float gap = Float.parseFloat(parameters.get(PARAM_GAP));
		boolean withGap = gap > 0.01f;
		pieceSpacing = pieceSize + gap;
		
		// saving this for later
		String outName = parameters.get(PARAM_OUTPUTFILE);

		// organise the text and image fields.  
		//    image fields are questions of where
		//    text fields are all the attribues of the text except its content


		for (Map<String, String> fieldDefn : fieldDefinitions) {
			String name = fieldDefn.get(FIELD_NAME_COL);
			String type = fieldDefn.get(FIELD_TYPE_COL);
			switch (type) {
			case "number":
				// nothing to set up
				break;
			case "colour":
				// nothing heee either
				break;
			case "image":
				imageFields.add(new ImageField(fieldDefn, imageFile));
				break;
			case "text":
				textFields.add(new TextField(fieldDefn));
				break;
			default:
				throw new IllegalArgumentException("Unrecognised field type " + type + " for " + name);
			}


		}
		List<String> incrementingFields = textFields.stream().filter(TextField::isIncrement).map(TextField::getName)
		        .collect(Collectors.toList());

		
		int colsPerRow = columnsPerRow(gap);
		int rowsPerPage = (int) ( ( PAGE_HEIGHT - 2 * ( MARGIN - gap ) ) / pieceSpacing ) - 1;

		String transformStart = "matrix(" + Float.toString(scaling) + ",0,0," + Float.toString(scaling) + ",";
		float antiScale = 1.0f - scaling;

		List<Map<String, String>> specs = tbook.readCellsAsStrings(parameters.get(PARAM_SPECSHEET), fieldNames);
		int totalPieces = 0;
		String oldFore = "white";
		String oldBack = "black";
		int row = 0;
		int col = 0;
		int page = 1;

		Element outputLayer = piecesDoc.obtainEmptyLayer(OUTPUT_LAYER_BASE_NAME + Integer.toString(page));
		Map<String, Image> images = new HashMap<>();
		
		for (Map<String, String> spec : specs) {
			if (testing) {

				doTestOutput(spec);
			} else {
				int number = getAsInteger("number", spec);
				for (ImageField imageField : imageFields) {
					String imageName = spec.get(imageField.getName());
					Image image = images.get(imageName);
					if (image == null) {
						XYcoords offset = imageField.getSpecificOffset(imageName, piecesDoc);

						
						String templateName = piecesDoc.ensureTemplate(imageName);
						image = new Image(imageName, templateName, offset);
					}
					imageField.setSpecific(image);

					imageTally.increment(imageName, number);
				}
				Map<String, Integer> incrementers = new HashMap<>();
				for (String incrementer : incrementingFields) {
					incrementers.put(incrementer, getAsInteger(incrementer, spec));
				}

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
				
				for (int item = 0; item < number; item++) {

					// topleft of piece
					float x = MARGIN + col * pieceSpacing;
					float y = MARGIN + row * pieceSpacing;
					XYcoords location = new XYcoords(x, y);
					// do the background
					piecesDoc.makeRectangle(outputLayer, x - gap * 0.5f, y - gap * 0.5f, pieceSpacing, pieceSpacing,
					        backColour);

					for (ImageField imageField : imageFields) {
						
						XYcoords offset = location.add(imageField.getCurrentOffset());
						Element pic = piecesDoc.addCloneOfTemplate(outputLayer, imageField.getTemplateName(), offset.getX(),
						        offset.getY());
						pic.setAttribute("fill", foreColour);

						pic.setAttribute("transform", transformStart + Float.toString(offset.getX() * antiScale) + ","
						        + Float.toString(offset.getY() * antiScale) + ")");
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
							piecesDoc.addText(outputLayer, text, tf.getFontSize(), x + tf.getXoffset(),
							        y + tf.getYoffset(), tf.getFontmod(), foreColour, tf.getJustification());
						}
					}
					col++;
					if (col > colsPerRow) {
						row++;
						col = 0;
						if (row > rowsPerPage) {
							drawFiducialLines( gap, row - 1, outputLayer);
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
			drawFiducialLines(gap, row, outputLayer);

			for (Map.Entry<String, Integer> tally : imageTally.getCounts().entrySet()) {
				System.out.println("   " + tally.getValue() + " of " + tally.getKey());

			}
			System.out
			        .println("Created " + page + " pages of " + totalPieces + " pieces with size " + pieceSize + "mm");
			piecesDoc.hideAllLayersButOne(FIRST_OUTPUT_LAYER);
		}
		piecesDoc.writeToFile(parameters.get(PARAM_DIRECTORY) + outName + SVG_SUFFIX);
	}

	public int columnsPerRow( float gap ) {
		return (int) ( ( PAGE_WIDTH - 2 * ( MARGIN - gap ) ) / pieceSpacing ) - 1;
	}

	public void doTestOutput( Map<String, String> spec ) {
		
//		for (ImageField imageField : imageFields) {
//			String imageName = spec.get(imageField.getName());
//			if (!imageTally.knownKey(imageName)) {
//				SvgObject image = piecesDoc.findSvgObject(imageName);
//				if (image == null) {
//					throw new IllegalArgumentException("Cannot find image " + imageName + " in image file");
//				}
//
//				imageTally.increment(imageName);
//			}
//		}
		Map<String, Image> images = new HashMap<>();

//		SvgObject boxOne = piecesDoc.findSvgObject("box_sqd1");
//		XYcoords testLocation = new XYcoords(40.0f, 40.0f);
		
		for (ImageField imageField : imageFields) {
			String imageName = spec.get(imageField.getName());
			Image image = images.get(imageName);
			if (image == null) {
				XYcoords offset = imageField.getSpecificOffset(imageName, piecesDoc);

				
				String templateName = piecesDoc.ensureTemplate(imageName);
				image = new Image(imageName, templateName, offset);
			}
			imageField.setSpecific(image);

			imageTally.increment(imageName);
		}

		
	}

	private class Image {
		final String imageName;
		final String templateName;
		final XYcoords offset;
		public Image(String imageName, String templateName, XYcoords offset) {
			this.imageName = imageName;
			this.templateName = templateName;
			this.offset = offset;
		}
		
		public String getImageName() {
			return imageName;
		}

		public String getTemplateName() {
			return templateName;
		}
		
		public XYcoords getOffset() {
			return offset;
		}
		
	}
	
	private class ImageField {
		private final String name;
		private final String boxNamePattern;
		private final Justification justification;
		
		private Image currentImage;

		ImageField(Map<String, String> defn, String imageFile) {
			name = defn.get(FIELD_NAME_COL);
			this.boxNamePattern = defn.get(PieceMakerMainBox.FIELD_PARENTBOX_COL);

			this.justification = Justification.valueOf(defn.get(PieceMakerMainBox.FIELD_JUSTIFY_COL).toUpperCase());
		}

		public String getBoxName(String imageName) {
			return boxNamePattern.replaceFirst("\\*",imageName);
		}
		
		public XYcoords getSpecificOffset(String imageName, PiecesDocument piecesDoc) {
			SvgObject image = piecesDoc.findSvgObject(imageName);
			String boxName = getBoxName(image.getId());
			SvgObject box = piecesDoc.findSvgObject(boxName);
			if (box == null) {
				throw new IllegalArgumentException("Cannot find box " + boxName);
			}
			LOGGER.debug("image {}", image);
			LOGGER.debug("box   {}", box);
			return findOffsetInBox(image, box, justification);
		}

		
		public void setSpecific( Image image) {
			this.currentImage = image;

		}

		public String getTemplateName() {
			return currentImage.getTemplateName();
		}

		public String getName() {
			return name;
		}

		public XYcoords getCurrentOffset() {
			return currentImage.getOffset();
		}

	}
	
	public XYcoords findOffsetInBox( SvgObject image, SvgObject box , Justification justification) {
		XYcoords boxTL = box.getTopLeft();
		LOGGER.debug("box {}, TL {}", box.getId(), boxTL);
		XYcoords boxBR = box.getBottomRight();
		LOGGER.debug("box {}, BR {}", box.getId(), boxBR);
		XYcoords imageTL = image.getTopLeft();
		LOGGER.debug("item {}, TL {}", image.getId(), imageTL);
		XYcoords imageBR = image.getBottomRight();
		LOGGER.debug("item {}, BR {}", image.getId(), imageBR);
		// check its in bounds
		if (    boxTL.getX() <= imageTL.getX() 
			&&	boxTL.getY() <= imageTL.getY()
			&&	boxBR.getX() >= imageBR.getX()
			&&	boxBR.getY() >= imageBR.getY() ) {
			final float imageX;
			final float imageY;
			XYcoords imageCentre = image.getCentre();   // might not need this
			switch (justification) {
			case NW:
			case W:
			case SW:
				imageX = imageTL.getX();
				break;
			case N:
			case C:
			case S:
				imageX = imageCentre.getX();
				break;
			case NE:
			case E:
			case SE:
				imageX = imageBR.getX();
				break;
			default:
				throw new IllegalStateException("Invalid justification code " + justification);
			}
			switch (justification) {
			case NW:
			case N:
			case NE:
				imageY = imageTL.getY();
				break;
			case W:
			case C:
			case E:
				imageY = imageCentre.getY();
				break;
			case SW:
			case S:
			case SE:
				imageY = imageBR.getY();
				break;
			default:
				throw new IllegalStateException("Invalid justification code " + justification);
			}
			return new XYcoords((imageX - boxTL.getX()) * scaling, (imageY - boxTL.getY()) * scaling);
		} else {
			throw new IllegalArgumentException("image "+ image.getId() + " is not inside its box");
		}
	}

	
	private class TextField {

		private final String name;
		private final XYcoords offset;
		private final float fontSize;
		private final String fontfamily;
		private final String fontmod;
		private final Justification justify;
		private final String orientation; // should be enum
		private final boolean increment;

		TextField(Map<String, String> defn) {
			name = defn.get(FIELD_NAME_COL);
			fontSize = Float.parseFloat(defn.get(FIELD_FONTSIZE_COL)) * scaling;
			// these will be "" due to behaviour of excel reading
			fontfamily = defn.get(FIELD_FONTFAMILY_COL);
			fontmod = defn.get(FIELD_FONTMOD_COL);
			Justification justifyRequest = Justification.valueOf(defn.get(FIELD_JUSTIFY_COL) != null ? defn.get(FIELD_JUSTIFY_COL).toUpperCase() : "C");

			switch (justifyRequest) {
			case NW: case W: case SW : justify = Justification.SW; break;
			case N: case C: case S : justify = Justification.S; break;
			case NE: case E: case SE : justify = Justification.SE; break;
			default: throw new IllegalStateException("Unknown justification code " + justifyRequest);
			}
			offset = getOffset(name, defn.get(FIELD_PARENTBOX_COL));
			orientation = defn.get(FIELD_ORIENTATION_COL);
			String incrementStr = defn.get(FIELD_INCREMENT_COL);
			increment = incrementStr != null && incrementStr.equalsIgnoreCase("TRUE");

		}

		private XYcoords getOffset( String textModelName, String boxName ) {
			SvgObject textModel = piecesDoc.findSvgObject(textModelName);
			SvgObject box = piecesDoc.findSvgObject(boxName);
			if (box == null) {
				throw new IllegalArgumentException("Cannot find box " + boxName);
			}
			// text has funny rules
			XYcoords boxTL = box.getTopLeft();
			XYcoords boxBR = box.getBottomRight();
			// text BR is actually its coordinates - which always
			XYcoords imageBR = textModel.getBottomRight();
			// check its in bounds
			if (boxTL.getX() <= imageBR.getX() && boxTL.getY() <= imageBR.getY() && boxBR.getX() >= imageBR.getX()
			        && boxBR.getY() >= imageBR.getY()) {
				// inkscape always uses baseline, and the x will be the value

				return new XYcoords(( imageBR.getX() - boxTL.getX() ) * scaling,
				        ( imageBR.getY() - boxTL.getY() ) * scaling);
			} else {
				throw new IllegalArgumentException("image " + textModel.getId() + " is not inside its box");
			}

		}
		
		public String getName() {
			return name;
		}

		public float getXoffset() {
			return offset.getX();
		}

		public float getYoffset() {
			return offset.getY();
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

		public Justification getJustification() {
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

	public void drawFiducialLines( float gap, int rows, Element outputLayer ) {
		int colsPerRow = columnsPerRow(pieceSpacing);
		boolean withGap = gap > 0.1f;
		float x1 = MARGIN - gap;
		float x2 = x1 + ( colsPerRow) * pieceSpacing + gap;
		
		for (int r = 0; r < rows + 2; r++) {
			float y = MARGIN + r * pieceSpacing;
			piecesDoc.drawLine(outputLayer, new XYcoords(x1, y), new XYcoords(x2, y));
			if (withGap) {
				y -= gap;
				piecesDoc.drawLine(outputLayer, new XYcoords(x1, y), new XYcoords(x2, y));
			}

		}
		float y1 = MARGIN - gap;
		float y2 = x1 + ( rows + 1 ) * pieceSpacing + gap;
		for (int c = 0; c < colsPerRow + 1; c++) {
			float x = MARGIN + c * pieceSpacing;
			piecesDoc.drawLine(outputLayer, new XYcoords(x, y1), new XYcoords(x, y2));
			if (withGap) {
				x -= gap;
				piecesDoc.drawLine(outputLayer, new XYcoords(x, y1), new XYcoords(x, y2));
			}

		}
	}

}
