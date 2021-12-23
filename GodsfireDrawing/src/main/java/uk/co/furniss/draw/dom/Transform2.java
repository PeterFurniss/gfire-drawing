package uk.co.furniss.draw.dom;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Transform2 implements Trans {

	private static final Pattern TRANS_PATTERN = Pattern.compile("(\\w+.*\\))");
	private final Transformation combined;

	public Transform2(String attr) {
		Matcher m = TRANS_PATTERN.matcher(attr);
		Transformation sofar = null;
		while (m.find()) {
			Transformation trans = new Transformation(m.group(1));
			if (sofar == null) {
				sofar = trans;
			} else {
				sofar = sofar.combine(trans);
			}
		}
		combined = sofar;
	}

	
	@Override
	public XYcoords rotate( XYcoords original ) {
		return combined.rotate(original);
	}


	@Override
	public XYcoords apply( XYcoords original ) {
		return combined.transform(original);
	}

	
	@Override
	public XYcoords scale( XYcoords original ) {
		return combined.scale(original);
	}

	@Override
	public XYcoords scaleTo( XYcoords base, XYcoords original ) {
		if (combined.isScaling()) {
			return combined.scaleTo(base, original);
		} else {
			return original;
		}
	}
	
	@Override
	public float scale( float singleValue ) {
		return combined.scale(singleValue);
	}
	
	@Override
	public XYcoords translate( XYcoords original ) {
		return combined.translate(original);
	}

	@Override
	public boolean isInternalising() {
		return true;
	}

	@Override
	public boolean isTranslating() {
		return true;
	}

	@Override
	public boolean isScaling() {
		return combined.isScaling();
	}


	@Override
	public String toString() {
		return  combined.toString();
	}

}
