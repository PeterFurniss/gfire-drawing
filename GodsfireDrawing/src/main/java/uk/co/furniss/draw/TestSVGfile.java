package uk.co.furniss.draw;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestSVGfile {


   

	public static void main(String[] args) throws IOException {

		List<ShapeProvider> shapes = new ArrayList<>();

		final String testName = "hex";
		
//		shapes.add(new RandomPolygons());
		shapes.add(new Hexagons(100));

		JFrameGraphic jf = new JFrameGraphic(testName, shapes);

		SVGgraphic svg = new SVGgraphic(shapes);
		svg.writeToFile("c:/temp/pix/" + testName + ".svg");
		
	}

}
