package uk.co.furniss.draw.piecemaker;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
class PieceMaker implements SvgWriter {

	private static final String TYPE_TEXT = "text";

	private static final String TYPE_IMAGE = "image";

	private static final String TYPE_COLOUR = "colour";

	private static final String TYPE_NUMBER = "number";
	private static final String TYPE_FACE   = "face";
	private static final String PROPER_COLOUR = "proper";

	// special field names
	private static final String FIELD_NUMBER = TYPE_NUMBER;
	private static final String FIELD_FACE = TYPE_FACE;
	private static final String BACKGROUND_COLOUR_FIELD_NAME = "back";

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
	private static final String PARAM_OB = "ob";  // outputting order of battle is orthogonal to main params
	
	// the columns of a field definition sheet
	private static final String FIELD_NAME_COL = "field";
	private static final String FIELD_TYPE_COL = "type";
	private static final String FIELD_PARENTBOX_COL = "parentbox";
	private static final String FIELD_COLOURCHOICE_COL = TYPE_COLOUR;
	private static final String FIELD_INCREMENT_COL = "increment";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(PieceMaker.class.getName());
	private static final double COS30 = Math.cos(Math.PI/ 6.0);
	private static final String SVG_SUFFIX = ".svg";

	public static void main( String[] args ) throws FileNotFoundException {
		final String specFileName;
		final String specSheetName = PARAMETER_SHEET_NAME;
		final String game;
		final Set<String> options = new HashSet<>();

		List<String> argList = new ArrayList<>(Arrays.asList(args));
		if (argList.size() < 2) {
			throw new IllegalArgumentException("two arguments required - filename of spec sheet, game identifier (and options)");
		}
		
		// take the true arguments from the en
		game = argList.remove(argList.size()-1);
		specFileName = argList.remove(argList.size()-1);

		while (argList.size() > 0) {
			String option = argList.remove(0);
			switch (option) {
			case "-ob" : // order of battle (battalia)
				options.add(PARAM_OB);
				break;
			case "-battalia":
				options.add(PARAM_OB);
				break;
			default:
				throw new IllegalArgumentException("Unknown option " + option);
			}
		}
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
		
		if (options.contains("ob")) {
			parameters.put(PARAM_OB, PARAM_OB);
		}
		PieceMaker instance = new PieceMaker(parameters, tbook);

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
	private final OutputArranger pageArranger;

	private float gapBetweenPieces;

	private String specSheetName;

	private String outputFilePath;

	private static final Pattern LABEL_CRACKER = Pattern.compile("label(\\d*)");

	private static final String HEX_ROW_FIELD = "row";

	private static final String HEX_COL_FIELD = "column";

	private static final String TYPE_ROTATION = "rotation";

	private static final Object COMMENT_INDICATOR = "#";

	private PieceType pieceType;

	private boolean outline;

	
	private PieceMaker( Map<String, String> parameters, ExcelBook tbook ) {
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
		switch (pieceType) {
		case SQUARE:
			if (parameters.get(PARAM_OB) != null) {
				pageArranger = new BattaliaArranger(pieceSize);
				outline = true;
			} else {
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
        		outline = gapBetweenPieces == 0.0f;
			}
			break;
		case CUBE:
			pageArranger = new VerticalCubeArranger(pieceSize, gapBetweenPieces);
			break;
		case MAPHEX:
			// the parameters are re-purposed for maphex
			// pieceSize will be width of hex = hexSide * 2
			// gapBetweenPieces  = overlap on each page
			pageArranger = new HexArranger(pieceSize / 2, gapBetweenPieces);
			break;
		default:
			throw new IllegalStateException("No arranger defined for piecetype " + pieceType);
		}
		if (pieceType == PieceType.SQUARE) {
		} else {
		}
		
		outputFilePath = directoryName + "/" + parameters.get(PARAM_OUTPUTFILE) + (parameters.containsKey(PARAM_OB) ? "_ob" : "") + SVG_SUFFIX;

		

		List<Map<String, String>> fieldDefinitions = tbook.readCellsAsStrings(fieldSheetName, Arrays.asList(
				FIELD_NAME_COL, // columns of the spec  sheet
				FIELD_TYPE_COL,   // image, colour, text,number
		        FIELD_PARENTBOX_COL, // surrounding box to define this image/textfield position
		        FIELD_COLOURCHOICE_COL,  // which fore ground colour is this
		        FIELD_INCREMENT_COL)); // this value is incremented for multiple pieces of same spec if there is a starting value

		fieldNames = fieldDefinitions.stream().map(c -> c.get(FIELD_NAME_COL)).collect(Collectors.toList());
		
		organiseFields(fieldDefinitions);

	}


	

	private void organiseFields( List<Map<String, String>> fieldDefinitions ) {
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
			case TYPE_ROTATION:
				break;
			default:
				throw new IllegalArgumentException("Unrecognised field type " + type + " for " + name);
			}
		}
	}


	private void drawPieces( ) {
		

		
		 Map<String, IncrementType> incrementingFields = textFields.stream().filter(TextField::isIncrement)
		        .collect(Collectors.toMap(tf -> tf.getName(), tf-> tf.getIncrementType()));
				
		String startTransformScaling = "matrix(" + Float.toString(scaling) + ",0,0," + Float.toString(scaling) + ",";
		float antiScale = 1.0f - scaling;

		
		List<Map<String, String>> specs = tbook.readCellsAsStrings(specSheetName, fieldNames);
		int totalPieces = 0;
		Map<String, String> foreColours = new HashMap<>();
		// set the default colourChoices to something unlikely (other than black)
		for (String choice : colourChoices) {
			// can't be bothered looking up the stream for this
			foreColours.put(choice, "green");
		}
		pageArranger.start(this);

		switch (pieceType) {
		case SQUARE:
			totalPieces = drawSquarePieces(incrementingFields, startTransformScaling, antiScale, specs, totalPieces, foreColours);
			break;
		case CUBE:
			totalPieces = drawCubePieces(incrementingFields, startTransformScaling, antiScale, specs, totalPieces, foreColours);
			break;
		case MAPHEX:
			totalPieces = drawMapHexes(incrementingFields, startTransformScaling, antiScale, specs, totalPieces, foreColours);
		default:
			break;
		}

		pageArranger.finish();

		for (Map.Entry<String, Integer> tally : imageTally.getCounts().entrySet()) {
			System.out.println("   " + tally.getValue() + " of " + tally.getKey());

		}
		System.out
		        .println("Created " + pageArranger.getPageCount() + " pages of " + totalPieces + " pieces with size " 
		        		+ pieceSize + "mm");
		
	}




	private int drawSquarePieces( Map<String, IncrementType> incrementingFields, String transformStart, float antiScale,
	        List<Map<String, String>> specs, int totalPieces, Map<String, String> foreColours ) {
		Map<String, Image> images = new HashMap<>();
		PieceArranger arranger = (PieceArranger) pageArranger;
		
		String oldBack = "black";
		for (Map<String, String> spec : specs) {
			arranger.setGroup(spec);
			String numberString = spec.get(TYPE_NUMBER);
			if (! numberString.equals(COMMENT_INDICATOR)) {
				int number = getAsInteger(TYPE_NUMBER, spec);
				setImages(spec, images, number);
				Map<String, Integer> incrementers = setIncrementors(spec, incrementingFields);
				String backColour = setColours(spec, foreColours, oldBack);
				oldBack = backColour;
				
				for (int item = 0; item < number; item++) {

					// topleft of piece
					XYcoords location = arranger.getNextLocation();
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
    						if (! imageField.retainColour) {
        						pic.setAttribute("fill", foreColours.get(imageField.getColourChoice()));
    						}
    						pic.setAttribute("transform", transformStart + Float.toString(offset.getX() * antiScale) + ","
    						        + Float.toString(offset.getY() * antiScale) + ")");
						}
					}
					for (TextField tf : textFields) {
						String name = tf.getName();
						final String text;
						if (tf.isIncrement()) {
							Integer value = incrementers.get(name);
							if (value > 0) {
								final String idString;
								switch (tf.getIncrementType()) {
								case INTEGER:
									idString = Integer.toString(value);
									break;
								case PAD3:
									idString = (value < 100 ? "0" : "") + Integer.toString(value);
									break;
								case ROMAN:
									idString = PieceUtils.RomanNumber(value);
									break;
								default:
									throw new IllegalStateException("Surprise increment attempt for " + name);
								}
    							text = idString;
    							incrementers.put(name, ++value);
							} else {
								text = "";
							}
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
					// do the outline
					if (outline) {
						piecesDoc.makeEdgeRectangle(outputLayer, x , y , 
								pieceSize, pieceSize,(backColour.equals("black") ? "white" : "black"));
					}
					totalPieces++;
				}
			}
		}
		return totalPieces;
	}
	

	private int drawMapHexes( Map<String, IncrementType> incrementingFields, String startTransformScaling, float antiScale,
	        List<Map<String, String>> specs, int totalPieces, Map<String, String> foreColours ) {
		Map<String, Image> images = new HashMap<>();
		HexArranger arranger = (HexArranger) pageArranger;
		float hexSide = pieceSize / 2;
		float hexHalfHeight = (float) ( hexSide * COS30 );

		for (Map<String, String> spec : specs) {
			int row = getAsInteger(HEX_ROW_FIELD, spec);
			int col = getAsInteger(HEX_COL_FIELD, spec);
			setImages(spec, images, 1);
			setForeColours(spec, foreColours);
			// Map<String, Integer> incrementers = setIncrementors(spec,
			// incrementingFields);
			XYcoords location = arranger.getHexLocation(row, col);

			float x = location.getX();
			float y = location.getY();

			// could separate the hex boundary here
			// and possibly need to have a rotation per image, as with colours
			int rotation = getAsInteger("rotation", spec);

			for (ImageField imageField : imageFields) {
				if (imageField.hasCurrentImage()) {
					XYcoords offset = location.add(imageField.getCurrentOffset());
					Element pic = piecesDoc.addCloneOfTemplate(outputLayer, imageField.getTemplateName(), offset.getX(),
					        offset.getY());
					// pic.setAttribute("fill", foreColours.get(imageField.getColourChoice()));
					
					String rotationTransform = rotation != 0 ? "rotate(" + Integer.toString(- 60 * rotation) + "," + 
						Float.toString(x + hexSide) + "," + Float.toString(y + hexHalfHeight) + ")," : "";
					pic.setAttribute("transform", rotationTransform + startTransformScaling + Float.toString(offset.getX() * antiScale) + ","
					        + Float.toString(offset.getY() * antiScale) + ")");
				}
			}
			for (TextField tf : textFields) {
				String name = tf.getName();
				final String text = spec.get(name);
				if (text.length() > 0) {
					piecesDoc.addText(outputLayer, text, tf.getFontSize(),
					        tf.transformForward(new XYcoords(x + tf.getXoffset(), y + tf.getYoffset())),
					        tf.getFontmod(), foreColours.get(tf.getColourChoice()), tf.getJustification(),
					        tf.getOrientation());
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


	private int drawCubePieces( Map<String, IncrementType> incrementingFields, String transformStart, float antiScale,
	        List<Map<String, String>> specs, int totalPieces, Map<String, String> foreColours ) {
		// transform the single list of specs into cube sets
		CubeArranger arranger = (CubeArranger) pageArranger;
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
				
//				XYcoords cubeLocation = arranger.getNextLocation();
				for (CubeFace face : CubeFace.values()) {
					Map<String, String> spec = cubeSpec.getFace(face);
				
					setImages(spec, images, i == 0 ? number : 0);
					Map<String, Integer> incrementers = setIncrementors(spec, incrementingFields);
					
					String backColour = spec.get(BACKGROUND_COLOUR_FIELD_NAME);
					for (String choice : colourChoices) {
						foreColours.put(choice,  spec.get(choice));
					}

					// topleft of piece
					XYcoords location = arranger.getFaceLocation(face);
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




	private Map<String, Integer> setIncrementors( Map<String, String> spec, Map<String, IncrementType> incrementingFields ) {
		Map<String, Integer> incrementers = new HashMap<>();
		for (Map.Entry<String, IncrementType> entry : incrementingFields.entrySet()) {
			if (entry.getValue().isIncrementing()) {
				String incrementer = entry.getKey();
				incrementers.put(incrementer, getAsInteger(incrementer, spec));
			}
			
		}
		return incrementers;
	}




	private String setColours( Map<String, String> spec, Map<String, String> foreColours, String oldBack ) {
		setForeColours(spec, foreColours);

		String backColour = spec.get(BACKGROUND_COLOUR_FIELD_NAME);
		if (backColour.equals("")) {
			backColour = oldBack;
		}
		return backColour;
	}




	private void setForeColours( Map<String, String> spec, Map<String, String> foreColours ) {
		for (String choice : colourChoices) {
			String colour = spec.get(choice);
			
			if (! colour.equals("")) { 
				foreColours.put(choice,  colour);
			}
		}
	}




	private void setImages( Map<String, String> spec, Map<String, Image> images, int number ) {
		for (ImageField imageField : imageFields) {
			String imageName = spec.get(imageField.getName());
			if (imageName.equals("")) {
				// this image field isn't used for this item
				imageField.setSpecific(null);
			} else {
				Image image = images.get(imageName);
				if (image == null) {
					
					LOGGER.debug("getting offset for {}", imageName);
					XYcoords offset = imageField.getSpecificOffset(imageName, piecesDoc);
   
					
					String templateName = piecesDoc.ensureTemplate(imageName, imageField.retainColour);
					image = new Image(imageName, templateName, offset);
				}
				imageField.setSpecific(image);
   
				imageTally.increment(imageName, number);
			}
		}
	}

	


	private void writeOutputSvg(  ) {
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
		public final boolean retainColour;
		private final String name;
		private final String boxNamePattern;
		
		private Image currentImage;
		private final String colourChoice;

		ImageField(Map<String, String> defn, String imageFile) {
			name = defn.get(FIELD_NAME_COL);
			this.boxNamePattern = defn.get(PieceMaker.FIELD_PARENTBOX_COL);
			this.colourChoice = defn.get(TYPE_COLOUR) != null ? defn.get(TYPE_COLOUR) : "fore";
			retainColour = colourChoice.equalsIgnoreCase(PROPER_COLOUR);

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
	

	private XYcoords findOffsetInBox( SvgObject image, SvgObject box) {
		XYcoords boxTL = box.getTopLeft();
		LOGGER.debug("box {}, TL {}", box.getId(), boxTL);
		XYcoords boxBR = box.getBottomRight();
		LOGGER.debug("box {}, BR {}", box.getId(), boxBR);
		XYcoords imageTL = image.getTopLeft();
		LOGGER.debug("item {}, TL {}", image.getId(), imageTL);
		XYcoords imageBR = image.getBottomRight();
		LOGGER.debug("item {}, BR {}", image.getId(), imageBR);
		// check its in bounds
		if (  (  boxTL.getX() <= imageTL.getX() 
			&&	boxTL.getY() <= imageTL.getY()
			&&	boxBR.getX() >= imageBR.getX()
			&&	boxBR.getY() >= imageBR.getY() )
				|| image.getId().startsWith("border") ) {
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
		private final IncrementType increment;
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
			increment = IncrementType.getFromKey(defn.get(FIELD_INCREMENT_COL).toUpperCase());
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
			LOGGER.debug("text :  br {}", imageBR);

			// check its in bounds
			if (boxTL.getX() <= imageBR.getX() && boxTL.getY() <= imageBR.getY() && boxBR.getX() >= imageBR.getX()
			        && boxBR.getY() >= imageBR.getY()) {
				// inkscape always uses baseline, and the x will be the value

				return new XYcoords(( imageBR.getX() - boxTL.getX() ) * scaling,
				        ( imageBR.getY() - boxTL.getY() ) * scaling);
			} else {
				throw new IllegalArgumentException("image " + textModel.getId() + " is not inside its box " + box.getId());
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
			return increment.isIncrementing();
		}
		
		public IncrementType getIncrementType() {
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


	private static int getAsInteger( String key, Map<String, String> spec ) {
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
