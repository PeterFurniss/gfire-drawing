package uk.co.furniss.draw.piecemaker;

import java.util.Map;

import uk.co.furniss.draw.dom.PiecesDocument;
import uk.co.furniss.draw.dom.SvgObject;
import uk.co.furniss.draw.dom.XYcoords;

class ImageField {
	private final String name;
	private final float scaling;
	private final String boxNamePattern;
	private final Justification justification;
	
	private String templateName; // set late

	ImageField(Map<String, String> defn, float pieceSize, float scaling, String imageFile) {
		this.scaling = scaling;
		name = defn.get("column");
		this.boxNamePattern = defn.get(PieceMakerMainBox.FIELD_PARENTBOX_COL);

		this.justification = Justification.valueOf(defn.get(PieceMakerMainBox.FIELD_JUSTIFY_COL).toUpperCase());
	}

	public String getBoxName(String imageName) {
		return boxNamePattern.replaceFirst("\\*",imageName);
	}
	
	public XYcoords getImageOffset(SvgObject image, PiecesDocument piecesDoc) {
		String boxName = getBoxName(image.getId());
		SvgObject box = piecesDoc.findSvgObject(boxName);
		if (box == null) {
			throw new IllegalArgumentException("Cannot find box " + boxName);
		}
		XYcoords boxTL = box.getTopLeft();
		XYcoords boxBR = box.getBottomRight();
		XYcoords imageTL = image.getTopLeft();
		XYcoords imageBR = image.getBottomRight();
		// check its in bounds
		if (boxTL.getX() < imageTL.getX() 
			&&	boxTL.getY() < imageTL.getY()
			&&	boxBR.getX() < imageBR.getX()
			&&	boxBR.getY() < imageBR.getY() ) {
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
			return new XYcoords((imageX - imageTL.getX()) * scaling, (imageY - boxTL.getY()) * scaling);
	} else {
		throw new IllegalArgumentException("image "+ image.getId() + " is not inside " + boxName);
	}
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

}