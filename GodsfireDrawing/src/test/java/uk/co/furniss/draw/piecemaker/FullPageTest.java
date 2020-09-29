package uk.co.furniss.draw.piecemaker;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.w3c.dom.Element;

import uk.co.furniss.draw.dom.PiecesDocument;

class FullPageTest {
	
	@Test
	void testOne() {
		SvgWriter mockWriter = mock(SvgWriter.class);
		PiecesDocument mockDoc = mock(PiecesDocument.class);
		Element mockLayer = mock(Element.class)
				;
		when(mockWriter.getOutputDocument()).thenReturn(mockDoc);
		when(mockDoc.obtainEmptyLayer("Output1")).thenReturn(mockLayer);
		PageArranger instance = new FullPage(20.0f, 2.0f);
		instance.start(mockWriter);
		for (int c =0; c < 1000; c++) {
			
		}
		instance.finish();
	}

}
