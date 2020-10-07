package uk.co.furniss.draw.piecemaker;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import uk.co.furniss.draw.dom.PiecesDocument;
import uk.co.furniss.draw.dom.XYcoords;

class LabelArrangerTest {
	
	@Test
	void testFirstLabel() {
		SvgWriter mockWriter = mock(SvgWriter.class);
		PiecesDocument mockDoc = mock(PiecesDocument.class);
		Element mockLayer = mock(Element.class)
				;
		when(mockWriter.getOutputDocument()).thenReturn(mockDoc);
		when(mockDoc.obtainEmptyLayer("Output1")).thenReturn(mockLayer);
		
		PieceArranger instance = new LabelArranger(13.0f, 1.0f);
		instance.start(mockWriter);
		for (int c =0; c < 10; c++) {
			XYcoords location = instance.getNextLocation();
			System.out.println("piece " + c + " will have top left at " + location);
		}
		instance.finish();
	}

}
