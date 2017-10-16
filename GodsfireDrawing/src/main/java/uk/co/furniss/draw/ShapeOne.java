package uk.co.furniss.draw;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;


public class ShapeOne extends JFrame {
    private static final int NUM_SHAPES = 5;
    private static final int NUM_POINTS_PER_SHAPE = 5;

    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;
    private List<ColouredShape> shapes;
    private Random randomGenerator;

    
	public ShapeOne(String title) {
        super(title);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(WIDTH, HEIGHT);        
        randomGenerator = new Random();
        initShapes();
    }

	
    private void initShapes() {
        shapes = new ArrayList<ColouredShape>(NUM_SHAPES);
        for (int i = 0; i < NUM_SHAPES; i++) {
            Point[] points = getRandomPoints();
            Color color = getRandomColor();

            shapes.add(i, new ColouredShape(points, color));
        }

    }
    
    private Point[] getRandomPoints() {
        Point[] points = new Point[NUM_POINTS_PER_SHAPE];
        for (int i = 0; i < points.length; i++) {
            int x = randomGenerator.nextInt(WIDTH);
            int y = randomGenerator.nextInt(HEIGHT);
            points[i] = new Point(x, y);
        }
        return points;
    }

    /**
    * @return a Color with random values for alpha, red, green, and blue values
    */
    private Color getRandomColor() {
        float alpha = randomGenerator.nextFloat();
        float red = randomGenerator.nextFloat();
        float green = randomGenerator.nextFloat();
        float blue = randomGenerator.nextFloat();

        return new Color(red, green, blue, alpha);
    }
    
//	public static void main(String[] args) {
//
//		try {
//			int size = Integer.parseInt(args[0]);
//			ShapeOne b = new ShapeOne("Testing shape");
//
//
//			File file = HexGraphicsUtil.write(args[1], b);
//			if (file == null)
//				System.out.println("Error creating file");
//			else
//				System.out.println("Hexagon tile created: " + file.getAbsolutePath());
//		} catch (Exception e) {
//			System.out.format("Exception: %s", e.getMessage());
//			e.printStackTrace();
//		}
//	}

}
