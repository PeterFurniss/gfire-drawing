package uk.co.furniss.draw.piecemaker;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import uk.co.furniss.draw.dom.XYcoords;

enum CubeFace {
	TOP(0,0), FRONT(0,1), UNDER(0,2), LEFT(1,2), BACK(1,3), RIGHT(1,4);
	
	
	// arrange as
	//    top   front   under
	//                  left    back   right
	
	// orientation is interesting
	// for the sides (not top, back)  the top is
	//          front - west
	//              lower row, south
	

	private final int columnOffset;
	private final int rowOffset;
	private final String letter;
	
	private CubeFace(int rowOffset, int columnOffset) {
		this.columnOffset = columnOffset;
		this.rowOffset = rowOffset;
		this.letter = name().substring(0,1);
	}

	public String letter() {
		return letter;
	}

	public int getColumnOffset() {
		return columnOffset;
	}

	public int getRowOffset() {
		return rowOffset;
	}

	private static final Map<String, CubeFace> letterMap;

	static {
		letterMap = Stream.of(values()).collect(Collectors.toMap(it -> it.letter, it -> it));
	}

	public static CubeFace getByLetter( String value ) {
		return letterMap.get(value);
	}

	public XYcoords getOffset(float cubeSide) {
		return new XYcoords(cubeSide * columnOffset, cubeSide * rowOffset);
	}
	
	public XYcoords getOffsetVertical(float cubeSide) {
		return new XYcoords(cubeSide * rowOffset, cubeSide * columnOffset);
		
	}
}
