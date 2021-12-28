package uk.co.furniss.draw.piecemaker;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import uk.co.furniss.draw.dom.Orientation;
import uk.co.furniss.draw.dom.PiecesDocument;
import uk.co.furniss.draw.dom.SvgObject;
import uk.co.furniss.draw.dom.SvgText;
import uk.co.furniss.draw.dom.XYcoords;
import uk.co.furniss.xlsx.ExcelBook;

/**
 * make a sheet of pieces specifiation file says how many of each, corner
 * numbers, which silhouette and colours (that will take som designing). piece
 * silhouette is an image from an input svg file. object has a specific name
 */
class HexMapMaker implements SvgWriter {


	private static final String TYPE_TEXT = "text";
	private static final String TYPE_IMAGE = "image";
	private static final String TYPE_COLOUR = "colour";
	private static final String TYPE_NUMBER = "number";
	private static final String TYPE_FACE   = "face";

	
	// special field names
	private static final String FIELD_NUMBER = TYPE_NUMBER;
	private static final String FIELD_FACE = TYPE_FACE;
	private static final String BACKGROUND_COLOUR_FIELD_NAME = "back";
	private static final String PROPER_COLOUR = "proper";

	private static final String PARAMETER_SHEET_NAME = "param";
	
	// the coloumns of the parameter sheet
	private static final String PARAM_DEFNID = "defnid";
	private static final String PARAM_PIECETYPE = "type";
	private static final String PARAM_FIELDSHEET = "fieldsheet";
	private static final String PARAM_SPECSHEET = "specsheet";
	private static final String PARAM_DIRECTORY = "directory";
	private static final String PARAM_OUTPUTFILE = "outputfile";
	private static final String PARAM_IMAGEFILE = "imagefile";
	private static final String PARAM_IMAGELAYER = "imagelayer";
	private static final String PARAM_IMAGESIZE = "imagesize";
	private static final String PARAM_PIECESIZE = "piecesize";
	private static final String PARAM_GAP = "gap";
	private static final String PARAM_PAPER = "paper";
	
	// the columns of a field definition sheet
	private static final String FIELD_NAME_COL = "field";
	private static final String FIELD_TYPE_COL = "type";
	private static final String FIELD_PARENTBOX_COL = "parentbox";
	private static final String FIELD_COLOURCHOICE_COL = TYPE_COLOUR;
	private static final String FIELD_INCREMENT_COL = "increment";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(HexMapMaker.class.getName());
	private static final String SVG_SUFFIX = ".svg";

	public static void main( String[] args ) throws FileNotFoundException {
		final String specFileName;
		final String specSheetName = PARAMETER_SHEET_NAME;
		final String game;

		if (args.length != 2) {
			throw new IllegalArgumentException("two arguments required - filename of spec sheet, game identifier");
		}
		specFileName = args[0];
		game = args[1];

		String fullSpecFileName = new File(specFileName).getAbsolutePath().replaceAll("\\\\","/");
		
		String specFileDirectory = fullSpecFileName.replaceFirst("/[^/]*$","");
		System.out.println("Will read specification file " + fullSpecFileName + " in " + specFileDirectory + ".");
		ExcelBook tbook = new ExcelBook(specFileName);
		
		List<Map<String, String>> specParams = tbook.readCellsAsStrings(specSheetName, Arrays.asList(
				PARAM_DEFNID, // allow multiple sets of params
				PARAM_PIECETYPE,  // what sort of piece this is
		        PARAM_FIELDSHEET, // where the field definitions are
		        PARAM_SPECSHEET, // says what pieces to make
		        PARAM_DIRECTORY, // where the image and output files are. if relative, relative to the spec sheet
		        PARAM_IMAGEFILE, // svg file containing images
		        PARAM_IMAGELAYER, // layer in imagefile with the images
		        PARAM_IMAGESIZE, // size of images as in imagefile
		        PARAM_OUTPUTFILE, // where to write the piece pictures
		        PARAM_PIECESIZE, // how big to make the pieces
		        PARAM_GAP,       // margin between the pieces
		        PARAM_PAPER      // whether to allow for labels

		));
		
		Map<String, String> parameters = null;
		for (Map<String, String> line : specParams) {
			String defnId = line.get(PARAM_DEFNID);
			if (defnId.equals(game)) {
				parameters = line;
				break;
			}
		}
		if (parameters == null) {
			throw new IllegalArgumentException("Game " + game + " doesn't have a line in spec sheet");
		}
		String dir = parameters.get(PARAM_DIRECTORY);
		if (dir.startsWith("..")) {
			parameters.put(PARAM_DIRECTORY, dir.replaceFirst("..", new File(specFileDirectory).getParent()));
		} else if (dir.startsWith(".")) {
			parameters.put(PARAM_DIRECTORY, dir.replaceFirst(".", specFileDirectory));
		}
		
		HexMapMaker instance = new HexMapMaker(parameters, tbook);

		instance.drawPieces();
		instance.writeOutputSvg();
	}

	private final ExcelBook tbook;
	
	private final String imageFile;
	private final PiecesDocument piecesDoc;

	private float pieceSize;
	private final float pieceSpacing;
	private final float scaling;
	private List<String> fieldNames;
	private final List<ImageField> imageFields = new ArrayList<>();
	private final List<TextField> textFields = new ArrayList<>();
	private final TallyCounter imageTally = new TallyCounter();
	private final List<String> colourChoices = new ArrayList<>();
	private Element outputLayer;
	private final PieceArranger pageArranger;

	private float gapBetweenPieces;

	private String specSheetName;

	private String outputFilePath;

	private static final Pattern LABEL_CRACKER = Pattern.compile("label(\\d*)");

	private PieceType pieceType;

	
	private HexMapMaker( Map<String, String> parameters, ExcelBook tbook ) {
		pieceType = PieceType.valueOf(parameters.get(PARAM_PIECETYPE).toUpperCase());
		if (pieceType == null) {
			throw new IllegalArgumentException("Unknown piece type " + parameters.get(PARAM_PIECETYPE));
		}

		this.tbook = tbook;	
		String fieldSheetName = parameters.get(PARAM_FIELDSHEET);
		specSheetName = parameters.get(PARAM_SPECSHEET);
		
		String directoryName = parameters.get(PARAM_DIRECTORY);
		imageFile = directoryName + "/" + parameters.get(PARAM_IMAGEFILE) + SVG_SUFFIX;
		System.out.println("Will read image file " + imageFile);

		piecesDoc = new PiecesDocument(imageFile);

		piecesDoc.setLibraryLayer(parameters.get(PARAM_IMAGELAYER));
		
		float imageDefinitionSize = Float.parseFloat(parameters.get(PARAM_IMAGESIZE));
		pieceSize = Float.parseFloat(parameters.get(PARAM_PIECESIZE));

		scaling = pieceSize / imageDefinitionSize;
		
		gapBetweenPieces = Float.parseFloat(parameters.get(PARAM_GAP));
		pieceSpacing = pieceSize + gapBetweenPieces;

		String paperType = parameters.get(PARAM_PAPER);
		Matcher matchLabel  = LABEL_CRACKER.matcher(paperType);
		if (pieceType == PieceType.SQUARE) {
    		if (paperType.equalsIgnoreCase("A4")) {
    			pageArranger = new FullPageArranger(pieceSize, gapBetweenPieces);
    		} else if (matchLabel.matches()) {
    			if (matchLabel.group(1).equals("")) {
    				pageArranger = new LabelArranger(pieceSize, gapBetweenPieces);
    			} else {
    				pageArranger = new LabelArranger(pieceSize, gapBetweenPieces, Integer.parseInt(matchLabel.group(1)));
    			}
    		} else {
    			throw new IllegalArgumentException("Paper type must be A4 or label or label#, "
    					+ "where # is the first label to be used (1..21)");
    		}
		} else {
			pageArranger = new VerticalCubeArranger(pieceSize, gapBetweenPieces);
		}
		
		outputFilePath = directoryName + "/" + parameters.get(PARAM_OUTPUTFILE)  + SVG_SUFFIX;

		

		List<Map<String, String>> fieldDefinitions = tbook.readCellsAsStrings(fieldSheetName, Arrays.asList(
				FIELD_NAME_COL, // columns of the spec  sheet
				FIELD_TYPE_COL,   // image, colour, text,number
		        FIELD_PARENTBOX_COL, // surrounding box to define this image/textfield position
		        FIELD_COLOURCHOICE_COL,  // which fore ground colour is this
		        FIELD_INCREMENT_COL)); // this value is incremented for multiple pieces of same spec

		fieldNames = fieldDefinitions.stream().map(c -> c.get(FIELD_NAME_COL)).collect(Collectors.toList());
		
		organiseFields(fieldDefinitions);

	}


	

	public void organiseFields( List<Map<String, String>> fieldDefinitions ) {
		// organise the text and image fields.  
		//    image fields are questions of where
		//    text fields are all the attribues of the text except its content

		for (Map<String, String> fieldDefn : fieldDefinitions) {
			String name = fieldDefn.get(FIELD_NAME_COL);
			if (name.equals("")) {
				// in case ExcelBook has returned rows that are now deleted
				break;
			}
			String type = fieldDefn.get(FIELD_TYPE_COL);
			switch (type) {
			case TYPE_NUMBER:
			case TYPE_FACE:
				// nothing to set up
				break;
			case TYPE_COLOUR:
				if (name.equals(BACKGROUND_COLOUR_FIELD_NAME)) {
					// nothing to do
				} else {
					colourChoices.add(name);
				}
				break;
			case TYPE_IMAGE:
				imageFields.add(new ImageField(fieldDefn, imageFile));
				break;
			case TYPE_TEXT:
				textFields.add(new TextField(fieldDefn));
				break;
			default:
				throw new IllegalArgumentException("Unrecognised field type " + type + " for " + name);
			}
		}
	}


	public void drawPieces( ) {
		
		List<String> incrementingFields = textFields.stream().filter(TextField::isIncrement).map(TextField::getName)
		        .collect(Collectors.toList());

		String transformStart = "matrix(" + Float.toString(scaling) + ",0,0," + Float.toString(scaling) + ",";
		float antiScale = 1.0f - scaling;

		
		List<Map<String, String>> specs = tbook.readCellsAsStrings(specSheetName, fieldNames);
		int totalPieces = 0;
		Map<String, String> foreColours = new HashMap<>();
		for (String choice : colourChoices) {
			// can't be bothered looking up the stream for this
			foreColours.put(choice, "green");
		}
		pageArranger.start(this);

		if (pieceType == PieceType.SQUARE) { 
			totalPieces = drawSquarePieces(incrementingFields, transformStart, antiScale, specs, totalPieces, foreColours);
		} else {
			totalPieces = drawCubePieces(incrementingFields, transformStart, antiScale, specs, totalPieces, foreColours);
		}
		pageArranger.finish();

		for (Map.Entry<String, Integer> tally : imageTally.getCounts().entrySet()) {
			System.out.println("   " + tally.getValue() + " of " + tally.getKey());

		}
		System.out
		        .println("Created " + pageArranger.getPageCount() + " pages of " + totalPieces + " pieces with size " 
		        		+ pieceSize + "mm");
		
	}

	public int drawSquarePieces( List<String> incrementingFields, String transformStart, float antiScale,
	        List<Map<String, String>> specs, int totalPieces, Map<String, String> foreColours ) {
		Map<String, Image> images = new HashMap<>();
		
		String oldBack = "black";
		for (Map<String, String> spec : specs) {

				int number = getAsInteger(TYPE_NUMBER, spec);
				setImages(spec, images, number);
				Map<String, Integer> incrementers = setIncrementors(spec, incrementingFields);
				String backColour = setColours(spec, foreColours, oldBack);
				oldBack = backColour;
				
				for (int item = 0; item < number; item++) {

					// topleft of piece
					XYcoords location = pageArranger.getNextLocation();
					float x = location.getX();
					float y = location.getY();

					// do the background
					piecesDoc.makeRectangle(outputLayer, x - gapBetweenPieces * 0.5f, y - gapBetweenPieces * 0.5f, pieceSpacing, pieceSpacing,
					        backColour);

					for (ImageField imageField : imageFields) {
						if (imageField.hasCurrentImage()) {
    						XYcoords offset = location.add(imageField.getCurrentOffset());
    						Element pic = piecesDoc.addCloneOfTemplate(outputLayer, imageField.getTemplateName(), offset.getX(),
    						        offset.getY());
    						pic.setAttribute("fill", foreColours.get(imageField.getColourChoice()));
    
    						pic.setAttribute("transform", transformStart + Float.toString(offset.getX() * antiScale) + ","
    						        + Float.toString(offset.getY() * antiScale) + ")");
						}
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
									tf.transformForward(new XYcoords(x + tf.getXoffset(), y + tf.getYoffset())),
									tf.getFontmod(), foreColours.get(tf.getColourChoice()), 
							        tf.getJustification(), tf.getOrientation());
						}
					}

					totalPieces++;
				}
			
		}
		return totalPieces;
	}
	
	private class CubeSpec {
		private final int number;
		private final Map<CubeFace, Map<String, String>> faces = new EnumMap<>(CubeFace.class);
		
		public CubeSpec(int number) {
			this.number = number;
		}

		public int getNumber() {
			return number;
		}
		
		void addFace( Map<String, String> faceSpec) {
			String faceName = faceSpec.get(FIELD_FACE);
			final CubeFace face;
			if (faceName.length() == 1) {
				face = CubeFace.getByLetter(faceName.toUpperCase());
			} else {
				face = CubeFace.valueOf(faceName.toUpperCase());
			}
			faces.put(face,   faceSpec);
		}
		
		Map<String, String> getFace(CubeFace face) {
			return faces.get(face);
		}
		
	}

	public int drawCubePieces( List<String> incrementingFields, String transformStart, float antiScale,
	        List<Map<String, String>> specs, int totalPieces, Map<String, String> foreColours ) {
		// transform the single list of specs into cube sets
		List<CubeSpec> cubies = new ArrayList<>();
		CubeSpec cube = null;
		Map<String, String> previous = null;
		for (Map<String, String> spec : specs) {
			String numberString = spec.get(FIELD_NUMBER);
			if (numberString.length() > 0) {
				// using the number column to indicate start of a cube
				int number = Integer.parseInt(numberString);
				cube = new CubeSpec(number);
				cubies.add(cube);
			}
			// copy the colours now
			if (previous != null) {
    			if (spec.get(BACKGROUND_COLOUR_FIELD_NAME).equals("")) {
    				spec.put(BACKGROUND_COLOUR_FIELD_NAME, previous.get(BACKGROUND_COLOUR_FIELD_NAME));
    			}
    			for (String foreColour : colourChoices) {
        			if (spec.get(foreColour).equals("")) {
        				spec.put(foreColour, previous.get(foreColour));
        			}
				}
			}
			previous = spec;
			cube.addFace(spec);
		}
		
		
		Map<String, Image> images = new HashMap<>();
		
		for (CubeSpec cubeSpec : cubies) {
			int number = cubeSpec.getNumber();
			totalPieces += number;
			for (int i = 0; i < number; i++) {
				
//				XYcoords cubeLocation = pageArranger.getNextLocation();
				for (CubeFace face : CubeFace.values()) {
					Map<String, String> spec = cubeSpec.getFace(face);
				
					setImages(spec, images, i == 0 ? number : 0);
					Map<String, Integer> incrementers = setIncrementors(spec, incrementingFields);
					
					String backColour = spec.get(BACKGROUND_COLOUR_FIELD_NAME);
					for (String choice : colourChoices) {
						foreColours.put(choice,  spec.get(choice));
					}

					// topleft of piece
					XYcoords location = ((CubeArranger) pageArranger).getFaceLocation(face);
					float x = location.getX();
					float y = location.getY();

					// do the background
					piecesDoc.makeRectangle(outputLayer, x, y , pieceSize, pieceSize,
					        backColour);

					for (ImageField imageField : imageFields) {
						if (imageField.hasCurrentImage()) {
    						XYcoords offset = location.add(imageField.getCurrentOffset());
    						Element pic = piecesDoc.addCloneOfTemplate(outputLayer, imageField.getTemplateName(), offset.getX(),
    						        offset.getY());
    						pic.setAttribute("fill", foreColours.get(imageField.getColourChoice()));
    
    						pic.setAttribute("transform", transformStart + Float.toString(offset.getX() * antiScale) + ","
    						        + Float.toString(offset.getY() * antiScale) + ")");
						}
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
									tf.transformForward(new XYcoords(x + tf.getXoffset(), y + tf.getYoffset())),
									tf.getFontmod(), foreColours.get(tf.getColourChoice()), 
							        tf.getJustification(), tf.getOrientation());
						}
					}

				}
			}
			
		}
		return totalPieces;
	}

	public Map<String, Integer> setIncrementors( Map<String, String> spec, List<String> incrementingFields ) {
		Map<String, Integer> incrementers = new HashMap<>();
		for (String incrementer : incrementingFields) {
			incrementers.put(incrementer, getAsInteger(incrementer, spec));
		}
		return incrementers;
	}

	public String setColours( Map<String, String> spec, Map<String, String> foreColours, String oldBack ) {
		for (String choice : colourChoices) {
			String colour = spec.get(choice);
			
			if (! colour.equals("")) { 
				foreColours.put(choice,  colour);
			}
		}

		String backColour = spec.get(BACKGROUND_COLOUR_FIELD_NAME);
		if (backColour.equals("")) {
			backColour = oldBack;
		}
		return backColour;
	}


	public void setImages( Map<String, String> spec, Map<String, Image> images, int number ) {
		for (ImageField imageField : imageFields) {
			String imageName = spec.get(imageField.getName());
			if (imageName.equals("")) {
				imageField.setSpecific(null);
			} else {
				Image image = images.get(imageName);
				if (image == null) {
					LOGGER.debug("getting offset for {}", imageName);
					XYcoords offset = imageField.getSpecificOffset(imageName, piecesDoc);
   
					
					String templateName = piecesDoc.ensureTemplate(imageName, imageField.colourProper);
					image = new Image(imageName, templateName, offset);
				}
				imageField.setSpecific(image);
   
				imageTally.increment(imageName, number);
			}
		}
	}

	public void writeOutputSvg(  ) {
		piecesDoc.writeToFile(outputFilePath);
	}

	
	@Override
	public PiecesDocument getOutputDocument() {
		return piecesDoc;
	}


	@Override
	public void setOutputLayer(Element outputLayer) {
		this.outputLayer = outputLayer;
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
		
		private Image currentImage;
		private final String colourChoice;
		private final boolean colourProper;

		ImageField(Map<String, String> defn, String imageFile) {
			// special case to allow an image to extend outside the box
			// (didn't work)
//			String inputName = defn.get(FIELD_NAME_COL);
//			if (inputName.contains("/")) {
//				name = inputName.replaceFirst("/.*","");
//				this.boxNamePattern = inputName.replaceFirst(".*/","");
//			} else {
//				name = inputName;
//				this.boxNamePattern = defn.get(HexMapMaker.FIELD_PARENTBOX_COL);
//			}
			
			name = defn.get(FIELD_NAME_COL);
			this.boxNamePattern = defn.get(HexMapMaker.FIELD_PARENTBOX_COL);
			this.colourChoice = defn.get(TYPE_COLOUR) != null ? defn.get(TYPE_COLOUR) : "fore";
			colourProper = colourChoice.equalsIgnoreCase(PROPER_COLOUR);
		}

		public boolean hasCurrentImage() {
			return currentImage != null;
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
			return findOffsetInBox(image, box);
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

		public String getColourChoice() {
			return colourChoice;
		}

	}
	

	public XYcoords findOffsetInBox( SvgObject image, SvgObject box) {
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
			final float imageX, imageY;
			XYcoords imageCentre = image.getCentre();  	
			imageX = imageCentre.getX();
			imageY = imageCentre.getY();

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
		private final Orientation orientation; 
		private final boolean increment;
		private final String colourChoice;

		TextField(Map<String, String> defn) {
			name = defn.get(FIELD_NAME_COL);
			
			SvgObject textModel = piecesDoc.findSvgObject(name);
			final SvgText model;
			if (textModel instanceof SvgText) {
				model = (SvgText) textModel;
			} else {
				throw new IllegalStateException(name + " is not a SvgText, but " + textModel.getClass().getSimpleName());
			}
			fontSize = model.getFontSizePx() * scaling;  
			// these will be "" due to behaviour of excel reading
			fontfamily = model.getFontFamily();
			fontmod = model.getFontMod();
			justify = model.getAlignment();
			orientation = model.getRotation();
			offset = determineOffset(defn.get(FIELD_PARENTBOX_COL), model);
			String incrementStr = defn.get(FIELD_INCREMENT_COL);
			increment = incrementStr != null && incrementStr.equalsIgnoreCase("TRUE");
			colourChoice = defn.get(TYPE_COLOUR) != null ? defn.get(TYPE_COLOUR) : "fore";

		}

		public XYcoords determineOffset( String boxName, SvgObject textModel ) {
			SvgObject box = piecesDoc.findSvgObject(boxName);
			if (box == null) {
				throw new IllegalArgumentException("Cannot find box " + boxName);
			}
			// text has funny rules
			XYcoords boxTL = box.getTopLeft();
			XYcoords boxBR = box.getBottomRight();
			LOGGER.debug("box for text : tl {}, br {}", boxTL, boxBR);
			// text BR is actually its coordinates - which always
			XYcoords imageBR = transformBackward(textModel.getBottomRight());

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
		
		public String getColourChoice() {
			return colourChoice;
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
			return orientation.getRotation();
		}

		public boolean isIncrement() {
			return increment;
		}
		

		public XYcoords transformForward(XYcoords original) {
			final XYcoords result;
			switch (orientation) {
			case BOTTOM:
				result = original;
				break;
			case RIGHT:
				result = new XYcoords(-original.getY(), original.getX());
				break;
			case TOP:
				result = new XYcoords(-original.getX(), -original.getY());
				break;
			case LEFT:
				result = new XYcoords(original.getY(), -original.getX());
				break;
			default:
				throw new IllegalStateException("Unknown orientation " + orientation);
			}
			return result;
			
		}
		
		public XYcoords transformBackward(XYcoords original) {
			// rotational changes are not removed by internalise transformation
			// these changes will be inexact
			final XYcoords result;
			switch (orientation) {
			case BOTTOM:
				result = original;
				break;
			case RIGHT:
				result = new XYcoords(original.getY(), -original.getX());
				break;
			case TOP:
				result = new XYcoords(-original.getX(), -original.getY());
				break;
			case LEFT:
				result = new XYcoords(-original.getY(), original.getX());
				break;
			default:
				throw new IllegalStateException("Unknown orientation " + orientation);
			}
			return result;
			
		}

	}


	public static int getAsInteger( String key, Map<String, String> spec ) {
		String asString = spec.get(key);
		if (asString.equals("")) {
			return 0;
		}
		try {
			return Integer.parseInt(asString);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Non-integer for " + key + " in " + spec, e);
		}
	}


}
