package uk.co.furniss.draw.piecemaker;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PieceUtilsTest {

	@Test
	void some() {
		assertEquals("CCCLXIII", PieceUtils.RomanNumber(363));
		assertEquals("VI", PieceUtils.RomanNumber(6));
		assertEquals("MCMLXXXIV", PieceUtils.RomanNumber(1984));
		assertEquals("IX", PieceUtils.RomanNumber(9));
	}

}
