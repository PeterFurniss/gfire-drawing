package uk.co.furniss.draw.dom;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Transform {

	private final List<TransformationRevised> functions;
	private static final Pattern TRANS_PATTERN = Pattern.compile("(\\w+.*\\))");
	private final boolean internalising;
	private final boolean translating;
	private final boolean scaling;

	public Transform(String attr) {
		functions = new ArrayList<>();
		Matcher m = TRANS_PATTERN.matcher(attr);

		while (m.find()) {
			TransformationRevised trans = new TransformationRevised(m.group(1));
			functions.add(trans);
		}
		scaling = functions.stream().anyMatch(TransformationRevised::isScaling);
		translating = functions.stream().anyMatch(TransformationRevised::isTranslating);
		int interning = (int) functions.stream().filter(TransformationRevised::isInternalising).count();
		if (interning == 0) {
			internalising = false;
		} else if (interning == functions.size()) {
			internalising = true;
		} else {
			throw new IllegalStateException("Partial internalising for transform " + attr);
		}
	}

	public XYcoords apply( XYcoords original ) {
		XYcoords result = original;
		for (TransformationRevised transformation : functions) {
			result = transformation.transform(result);
		}
		return result;
	}

	
	public XYcoords scale( XYcoords original ) {
		if (scaling) {
			XYcoords result = original;
			for (TransformationRevised transformation : functions) {
				result = transformation.scale(result);
			}
			return result;
		} else {
			return original;
		}
	}

	public XYcoords scaleTo( XYcoords base, XYcoords original ) {
		if (scaling) {
			XYcoords result = original;
			for (TransformationRevised transformation : functions) {
				result = transformation.scaleTo(base, result);
			}
			return result;
		} else {
			return original;
		}
	}
	
	public float scale( float original ) {
		if (scaling) {
			float result = original;
			for (TransformationRevised transformation : functions) {
				result = transformation.scale(result);
			}
			return result;
		} else {
			return original;
		}
	}
	
	public XYcoords translate( XYcoords original ) {
//		return original;
		if (translating) {
			XYcoords result = original;
			for (TransformationRevised transformation : functions) {
				result = transformation.translate(result);
			}
			return result;
		} else {
			return original;
		}
	}

	public boolean isInternalising() {
		return internalising;
	}

	public boolean isTranslating() {
		return translating;
	}

	public boolean isScaling() {
		return scaling;
	}

}
