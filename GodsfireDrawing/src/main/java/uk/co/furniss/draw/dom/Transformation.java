package uk.co.furniss.draw.dom;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Transformation {

	private final float a;
	private final float b;
	private final float c;
	private final float d;
	private final float e;
	private final float f;
	private final boolean proportionate;
	private final boolean scaling;
	private final String method;
	private final float scaleFactor;
	private final XYcoords translation;
	
	private final static Pattern FUNCTION_PATTERN = Pattern.compile("(\\w+)\\(([\\d-\\.,e ]+)\\)");
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Transformation.class.getName());
	

	public Transformation(String function) {
		Matcher m = FUNCTION_PATTERN.matcher(function);
		if (m.matches()) {
			method = m.group(1).toLowerCase();
			String[] args = m.group(2).split(",\\s*");
			LOGGER.debug("method {}:  group2 {}", method, m.group(2));
			float [] vals = new float[args.length];
			for (int i=0; i < args.length; i++) {
				float v = Float.parseFloat(args[i]);
				if (significantlyEqual(v, 0f)) {
					v = 0f;
				}
				if (significantlyEqual(v, 1f)) {
					v = 1.0f;
				}
				vals[i] = v;
			}
			switch (method) {
			case "matrix":
				a = vals[0];
				b = vals[1];
				c = vals[2];
				d = vals[3];
				e = vals[4];
				f = vals[5];
				proportionate = a == d && b == -c;
				scaling = a == d && a != 1;
				scaleFactor = 1.0f;
				break;
			case "translate":
				a = 1.0f;
				b = 0.0f;
				c = 0.0f;
				d = 1.0f;
				e = vals[0];
				f = vals.length > 1 ? vals[1] : 0.0f;
				proportionate = true;
				scaling = true;
				scaleFactor = 1.0f;
				break;
			case "rotate":
				double angle = Math.toRadians(vals[0]);
				float cos = (float) Math.cos(angle);
				float sin = (float) Math.sin(angle);
				a = cos;
				b = sin;
				c = -sin;
				d = cos;
				if (vals.length == 1) {
					e = 0.0f;
					f = 0.0f;
				} else {
					float px = vals[1];
					float py = vals[2];
					e = px - px*cos + py*sin;
					f = py - py*cos - px*sin;
				}
				// not sure about this in latter case
				proportionate = true;
				scaling = false;
				scaleFactor = 1.0f;
				break;
			case "scale":
				a = vals[0];
				b = 0.0f;
				c = 0.0f;
				d = vals.length > 1 ? vals[1] : a;
				e = 0.0f;
				f = 0.0f;
				proportionate =  a == d;
				scaling = true;
				scaleFactor = a;
				break;
			case "skewX":
				a = 1.0f;
				b = 0.0f;
				c = (float) Math.tan(Math.toRadians(vals[0]));
				d = 1.0f;
				e = 0.0f;
				f = 0.0f;
				proportionate = false;
				scaling = false;
				scaleFactor = 1.0f;
				break;
			case "skewY":
				a = 1.0f;
				b = (float) Math.tan(Math.toRadians(vals[0]));
				c = 0.0f;
				d = 1.0f;
				e = 0.0f;
				f = 0.0f;
				proportionate = false;
				scaling = false;
				scaleFactor = 1.0f;
				break;
				
			default:
				throw new IllegalArgumentException("Don't recognise transform " + function);
			}
		} else {
			throw new IllegalArgumentException("failed to match " + function);
		}
		this.translation = new XYcoords(e, f);
	}
	

	@Override
	public String toString() {
		return method + ": " + a + ", " + b + ", "+ c + ", "+ d + ", "+ e + ", "+ f + ", prop " + proportionate + ", scale " + scaling;
	}


	private boolean significantlyEqual(float a, float b) {
		return Math.abs(a - b) < 0.001f;
	}
	

	private Transformation(String method, float a, float b, float c, float d, float e, float f) {
		super();
		this.method = method;
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.e = e;
		this.f = f;
		this.proportionate = significantlyEqual(a,d);
		this.scaling = 	significantlyEqual(a,d); 
		scaleFactor = a;
		this.translation = new XYcoords(e, f);

	}



	public Transformation combine(Transformation other) {
		return new Transformation(
					"combine", 
					a * other.a + b * other.c,
					a * other.b + b * other.d,
					a * other.c + c * other.d,
					b * other.c + d * other.d,
					a * other.e + c * other.f + e, b * other.e + d * other.f + f);
	}
	

	public XYcoords transform( XYcoords original ) {
		float x = original.getX();
		float y = original.getY();

		return new XYcoords(x * a + y * c + e, x * b + y * d + f);
	}
	
	public XYcoords rotate( XYcoords original ) {
		float x = original.getX();
		float y = original.getY();

		return new XYcoords(x * a + y * c , x * b + y * d );
	}
	

	public XYcoords translate(XYcoords original) {
		return original.add(translation);
	}

	public XYcoords scale(XYcoords original) {
		return new XYcoords(scaleFactor * original.getX(), scaleFactor * original.getY());
	}
	public boolean isProportionate() {
		return proportionate;
	}

	public boolean isScaling() {
		return scaling;
	}

	float scale( float singleValue ) {
		if (! scaling) {
			LOGGER.warn("scaling single value for non-scaling transform {}", this);
		}
		return scaleFactor * singleValue;
	}


	public XYcoords scaleTo(XYcoords base, XYcoords original) {
		if (scaling) {
			return original.scaleTo(base,  a, d);
		} else {
			return original;
		}
	}
	
}
