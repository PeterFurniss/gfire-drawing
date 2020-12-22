package uk.co.furniss.draw.piecemaker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PieceUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(PieceUtils.class.getName());
	
	private PieceUtils() {
		// utility class
	}

	private static final String [][]  numerals;
	static {
		String [][] letters = new String[] [] {
			{ "I", "V"},
			{ "X", "L"},
			{ "C", "D"},
			{ "M", "#"},
			{"#", "#"}};
		numerals = new String [4][10];
		for (int power = 0; power < 4; power++) {
			String unit = letters[power][0];
			String five = letters[power][1];
			String ten =  letters [power+1][0];
			numerals[power][0] = "!";
			numerals[power][1] = unit;
			numerals[power][2] = unit + unit;
			numerals[power][3] = unit + unit + unit;
			numerals[power][4] = unit + five;
			numerals[power][5] = five;
			numerals[power][6] = five + unit;
			numerals[power][7] = five + unit + unit;
			numerals[power][8] = five + unit + unit + unit;
			numerals[power][9] = unit + ten;
		}

	}

	public static String RomanNumber(int i) {
		if (i >= 4000) {
			LOGGER.warn("Can't handle numbers greater than 4000");
			return Integer.toString(i);
		}
		int rem = i;
		int tens = 1000;
		int power = 3;
		
		StringBuilder b = new StringBuilder();
		
		while (rem > 0) {
			int c = rem / tens;
			if (c > 0) {
				b.append(numerals[power][c]);
			}
			rem = rem % tens;
			tens = tens / 10;
			power--;
		}
		return b.toString();
	}
}
