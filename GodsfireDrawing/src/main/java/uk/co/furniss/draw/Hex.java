package uk.co.furniss.draw;

import java.awt.Color;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Shape;

/**
 * a hexagon of defined size, with a shaded colour on the centre
 */
public class Hex implements TintedShape {

		private ColouredShape shape;
		private final RadialGradientPaint gradient;
		
		public Hex(int x, int y, int side, Color colour)  {
			Point[] points = new Point[6];
			
			
			double thirtyDegrees = Math.toRadians(30);
			double yOffset = Math.cos(thirtyDegrees) * side;

			points[0] = new Point(x - side, y);
			points[1] = new Point(x - side/2, (int)(y + yOffset));
			points[2] = new Point(x + side/2,(int)(y + yOffset));
			points[3] = new Point(x + side, y);
			points[4] = new Point(x + side/2,(int)(y - yOffset));
			points[5] = new Point(x - side/2, (int)(y - yOffset));
			
			shape = new ColouredShape(points, colour);
			
			
			float[] fracs = new float[] {0.1f, 1.0f };
			Color[] colours = new Color[] {Color.WHITE, colour};
			gradient = new RadialGradientPaint(x, y, side, fracs ,colours);
		}

		@Override
		public Color getColor() {
			return shape.getColor();
		}

		@Override
		public Shape getOutline() {
			return shape.getOutline();
		}
	
		public RadialGradientPaint getGradient() {
			return gradient;
		}

}
