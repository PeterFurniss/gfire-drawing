package uk.co.furniss.draw;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.svggen.SVGGraphics2DIOException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

public class SVGgraphic {

	private final SVGGraphics2D svgGenerator;

	public Graphics2D getGraphics2D() {
		return svgGenerator;
	}


	public SVGgraphic(Iterable<ShapeProvider> shapeProviders) {
		// Get a DOMImplementation.
		DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();

		// Create an instance of org.w3c.dom.Document.
		String svgNS = "http://www.w3.org/2000/svg";
		Document document = domImpl.createDocument(svgNS, "svg", null);

		svgGenerator = new SVGGraphics2D(document);
		
		Dimension dim = new Dimension(744, 1072);
		svgGenerator.setSVGCanvasSize(dim);
//		System.out.println("dimension is " + dim);
//		System.out.println("Canvas is " + dim.getWidth() + " by " + dim.getHeight());
		// have to write it now to match JFrame version
        for (ShapeProvider shapes : shapeProviders) {
            shapes.paintShapes(svgGenerator);
        }

	}

	
	
	public void writeToFile(String pathName) {
		// Finally, stream out SVG to the standard output using
		// UTF-8 encoding.
		boolean useCSS = true; // we want to use CSS style attributes
		try {
			File outFile = new File(pathName);
			outFile.getParentFile().mkdirs();
			
			Writer out = new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8");
			svgGenerator.stream(out, useCSS);
		} catch (UnsupportedEncodingException|SVGGraphics2DIOException | FileNotFoundException e) {
			throw new IllegalStateException("Failed to write svg file", e);
		}

	}
}
