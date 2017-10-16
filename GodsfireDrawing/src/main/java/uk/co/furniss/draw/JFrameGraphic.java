package uk.co.furniss.draw;

import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JFrame;

public class JFrameGraphic extends JFrame {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;
	private Iterable<ShapeProvider> shapeProviders;

	public JFrameGraphic(String title,  Iterable<ShapeProvider> shapeProviders) {
        super(title);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        this.shapeProviders = shapeProviders;
	}

    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        
        for (ShapeProvider shapes : shapeProviders) {
            shapes.paintShapes(g2);
        }
    }
}
