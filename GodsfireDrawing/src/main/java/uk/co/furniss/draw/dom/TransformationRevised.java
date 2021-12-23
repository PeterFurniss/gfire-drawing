package uk.co.furniss.draw.dom;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TransformationRevised {

	private final float scaleX;
	private final float scaleY;
	private final float translateX;
	private final float translateY;
	private final boolean internalising;
	private final boolean translating;
	private final boolean scaling;
	
	private final static Pattern FUNCTION_PATTERN = Pattern.compile("(\\w+)\\(([e\\d-\\., ]+)\\)");
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Transformation.class.getName());
	

	public TransformationRevised(String function) {
		Matcher m = FUNCTION_PATTERN.matcher(function);
		if (m.matches()) {
			String method = m.group(1).toLowerCase();
			String[] args = m.group(2).split(",\\s*");
			LOGGER.debug("method {}:  group2 {}", method, m.group(2));
			float [] vals = new float[args.length];
			for (int i=0; i < args.length; i++) {
				vals[i] = Float.parseFloat(args[i]);
			}
			switch (method) {
			case "matrix":
				// can't yet cope with significant skew or rotation
				if (Math.abs(vals[1]) > 0.001f || Math.abs(vals[2]) > 0.001f) {
					throw new IllegalArgumentException("Cannot cope with complex transformation " + function);
				}
				scaleX = vals[0];
				scaleY = vals[3];
				translateX = vals[4];
				translateY = vals[5];
				internalising = true;
				translating = true;
				scaling = true;
				break;
			case "translate":
				scaleX = 1.0f;
				scaleY = 1.0f;
				translateX = vals[0];
				translateY = vals.length > 1 ? vals[1] : translateX;
				internalising = true;
				translating = true;
				scaling = false;
				break;
			case "scale":
				scaleX = vals[0];
				scaleY = vals.length > 1 ? vals[1] : scaleX;
			
				translateX = 0.0f;
				translateY = 0.0f;
				internalising = true;
				translating = false;
				scaling = true;
				break;
			case "rotate":
			case "skewX":
			case "skewY":
				internalising = false;
				translating = false;
				scaling = false;

				scaleX = 1.0f;
				scaleY = 1.0f;
				translateX = 0.0f;
				translateY = 0.0f;
				break;
				
			default:
				throw new IllegalArgumentException("Don't recognise transform " + function);
			}
		} else {
			throw new IllegalArgumentException("failed to match " + function);
		}

	}

	public XYcoords scale(XYcoords original) {
		if (scaling) {
			return  original.scale(scaleX, scaleY);
		} else {
			return original;
		}
	}
	

	public XYcoords scaleTo(XYcoords base, XYcoords original) {
		if (scaling) {
			return original.scaleTo(base,  scaleX, scaleY);
		} else {
			return original;
		}
	}
	
	public float scale(float original) {
		if (Math.abs(scaleX - scaleY) < 1e7 ) {
			return original * scaleX;
		}
		throw new IllegalStateException("Cannot scale single value with scaleX " + scaleX + ", scaleY " + scaleY);
	}
	

	public XYcoords translate(XYcoords original) {
		return new XYcoords(original.getX() + translateX, original.getY() + translateY);
	}
	

	public XYcoords transform( XYcoords original ) {
		float x = original.getX();
		float y = original.getY();

		return new XYcoords(x * scaleX + translateX, y * scaleY + translateY);
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
