package uk.co.furniss.draw.dom;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;

public class DomStandarise {

	public DomStandarise() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		String directory = "c:/Users/Peter/Documents/games/godfire_pix/basepix/";
		String svgSuffix = ".svg";
		String fileName = "hexes4";

		
		String filePath = directory + fileName + svgSuffix;
		System.out.println("Reading " + filePath);
        Element svgDoc = XmlUtil.deserialiseXmlFile(filePath);
		   
//        for (Element pathElement : xp.findElements(svgDoc, "//path")) {
//        	System.out.println(pathElement.getNodeName());
//        	NamedNodeMap attrs = pathElement.getAttributes();
//        	for (int i=0; i < attrs.getLength(); i++ ) {
//        		Node attr = attrs.item(i);
//        		System.out.println("    " + attr.getNodeName() + " = " + attr.getNodeValue() );
//        	}
//
//			float [][] dim = SVGdutil.getPathCoords(pathElement);
//			for (float[] xy : dim) {
//				System.out.println("         " + xy[0] + "    " + xy[1]);
//			}
//		}

        // from 1 onwards
		SVGmangler.rgbColours(svgDoc);

        XPathUtil xp = XPathUtil.getSVG();
        
        Element red = xp.findElement(svgDoc, "//g[@id='redPattern']");
        Element blue = xp.findElement(svgDoc, "//g[@id='bluePattern']");
        Element green = xp.findElement(svgDoc, "//g[@id='greenPattern']");
        if (red == null) {
        	throw new IllegalArgumentException("No redPattern");
        }
        if (blue == null) {
        	throw new IllegalArgumentException("No bluePattern");
        }
        if (green == null) {
        	throw new IllegalArgumentException("No greenPattern");
        }
        List<Element> patternElements = Arrays.asList(red, green, blue);
        
        // 1 -> 2 : change colours
//        	SVGdutil.rotateColours(green,1);
//        	SVGdutil.rotateColours(blue,2);
        
        // 3 : 3mod
        SVGmangler.unscaleText(svgDoc);
        
//         remove inner translates first
        for (Element patternElement : patternElements) {
			List<Element> innerTranslates = xp.findElements(patternElement, ".//g[@transform]");
			for (Element inner : innerTranslates) {
				SVGmangler.untranslateGroup(inner);
			}
			SVGmangler.untranslateGroup(patternElement);
		}

      
        // from 3 onwards
        SVGmangler.roundNumbers(svgDoc);
        
        String outFile = directory + fileName + "_mod" + svgSuffix;
        try(  PrintWriter out = new PrintWriter( outFile )  ){
            out.println( XmlUtil.serialiseXml(svgDoc, true) );
            System.out.println("Wrote " + outFile);
        } catch (FileNotFoundException e) {
			throw new IllegalArgumentException("Failed to write to " + fileName, e);
		}
        
	}

}
