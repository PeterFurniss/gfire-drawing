package uk.co.furniss.draw;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Shape;

// extracted from https://stackoverflow.com/questions/1557835/rendering-vector-shapes-in-java?rq=1
class ColouredShape implements TintedShape {

    private Polygon outline;
    private Color color;

    public ColouredShape(Point[] points, Color color) {
        this.color = color;
        // Would be better to separate out into xpoints, ypoints, npoints
        // but I'm lazy
        outline = new Polygon();
        for (Point p : points) {
            outline.addPoint((int) p.getX(), (int) p.getY());
        }
    }

    /* (non-Javadoc)
	 * @see uk.co.furniss.draw.TintedShape#getColor()
	 */
    @Override
	public Color getColor() {
        return color;
    }

    /* (non-Javadoc)
	 * @see uk.co.furniss.draw.TintedShape#getOutline()
	 */
    @Override
	public Shape getOutline() { return outline; }

}