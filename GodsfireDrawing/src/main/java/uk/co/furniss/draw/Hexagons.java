package uk.co.furniss.draw;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * make a grid of tinted hexagons. Not sure about the colour 
 */
public class Hexagons implements ShapeProvider {

	private final List<Hex> hexes;
	private Random randomGenerator;
	private static final int X_GAP = 3;
	private static final double THIRTYDEGREES = Math.toRadians(30);
	private static final double YGAP = 	Math.cos(THIRTYDEGREES);
	private static final double X_OFFSET = Math.sin(THIRTYDEGREES) + 1;
	
	private static final float HIGH = 1.0f;
	private static final float LOW = 0.7f;
	
	private static final Color[] COLOURS = new Color[] {
			new Color(HIGH, LOW, LOW, 1.0f),
			new Color(LOW, HIGH, LOW, 1.0f),
			new Color(LOW, LOW, HIGH, 1.0f)
				
	};
	
	/**
	 * create a grid of hexagons of defined side.
	 * Colour cycles by row r - g - b (pastel)
	 * @param side
	 */
	public Hexagons(int side) {
        randomGenerator = new Random();

		hexes = new ArrayList<>();
		int yGap = (int) (side * YGAP);
		int y = 0;
		int colour = 0;
		for (int rows = 0 ; rows < 12; rows++) {
			int x = side + ( rows % 2 != 0 ? (int) (side * X_OFFSET) : 0);
			y += yGap;
			for (int cols = 0 ; cols < 3; cols ++) {
				hexes.add(new Hex(x, y, side, COLOURS[colour]));
				x += side * X_GAP;
			}
			colour = (++colour) % 3;
		}
	}

    /**
    * @return a Color with random values for alpha, red, green, and blue values
    */
    private Color getRandomColor() {
        float alpha = 1.0f; //randomGenerator.nextFloat();
        float red = randomGenerator.nextFloat();
        float green = randomGenerator.nextFloat();
        float blue = randomGenerator.nextFloat();

        return new Color(red, green, blue, alpha);
    }
	
	@Override
	public void paintShapes(Graphics2D graphic) {
        for (Hex shape : hexes) {
//        	graphic.setPaint(shape.getGradient());
        	graphic.setColor(shape.getColor());
        	graphic.fill(shape.getOutline());
        }
	}

}
