package uk.co.furniss.draw.dom;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Transformation {

	private final float scaleX;
	private final float b;
	private final float c;
	private final float d;
	private final float e;
	private final float f;
	private final boolean proportionate;
	
	private final static Pattern FUNCTION_PATTERN = Pattern.compile("(\\w+)\\(([\\d-\\., ]+)\\)");
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Transformation.class.getName());
	
	public Transformation(String function) {
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
				scaleX = vals[0];
				b = vals[1];
				c = vals[2];
				d = vals[3];
				e = vals[4];
				f = vals[5];
				proportionate = scaleX == d && b == -c;
				break;
			case "translate":
				scaleX = 1.0f;
				b = 0.0f;
				c = 0.0f;
				d = 1.0f;
				e = vals[0];
				f = vals.length > 1 ? vals[1] : 0.0f;
				proportionate = true;
				break;
			case "rotate":
				double angle = Math.toRadians(vals[0]);
				float cos = (float) Math.cos(angle);
				float sin = (float) Math.sin(angle);
				scaleX = cos;
				b = sin;
				c = -sin;
				d = cos;
				e = 0.0f;
				f = 0.0f;
				proportionate = true;
				break;
			case "scale":
				scaleX = vals[0];
				b = 0.0f;
				c = 0.0f;
				d = vals.length > 1 ? vals[1] : scaleX;
				e = 0.0f;
				f = 0.0f;
				proportionate =  scaleX == d;
				break;
			case "skewX":
				scaleX = 1.0f;
				b = 0.0f;
				c = (float) Math.tan(Math.toRadians(vals[0]));
				d = 1.0f;
				e = 0.0f;
				f = 0.0f;
				proportionate = false;
				break;
			case "skewY":
				scaleX = 1.0f;
				b = (float) Math.tan(Math.toRadians(vals[0]));
				c = 0.0f;
				d = 1.0f;
				e = 0.0f;
				f = 0.0f;
				proportionate = false;
				break;
				
			default:
				throw new IllegalArgumentException("Don't recognise transform " + function);
			}
		} else {
			throw new IllegalArgumentException("failed to match " + function);
		}

	}

	public XYcoords transform( XYcoords original ) {
		float x = original.getX();
		float y = original.getY();

		return new XYcoords(x * scaleX + y * c + e, x * b + y * d + f);
	}

	public boolean isProportionate() {
		return proportionate;
	}

}
