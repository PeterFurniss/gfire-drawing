package uk.co.furniss.draw.gfmap;

import java.util.ArrayList;
import java.util.List;

class Cell {

	private final int row;
	private final int col;
	private final int level;
	private final HexColour cellColour;
	
	private static final int bottom = -6;
	private static final int top = 5;
	
	Cell(int row, int col, int level) {
		super();
		this.row = row;
		this.col = col;
		this.level = level;
		cellColour = Levels.cellLevel(level);
	}

	public int getRow() {
		return row;
	}

	public int getCol() {
		return col;
	}

	public int getLevel() {
		return level;
	}
	
	public HexColour getCellColour() {
		return cellColour;
	}

	public List<Cell> neighbours(int maxRows, int maxCols) {
		List<Cell> answer = new ArrayList<>(12);
//		System.out.println("Row " + row + ", Col " + col + ", Level " + level);
		int p = cellColour == HexColour.BLUE ? 1 : 0;
		int q = cellColour != HexColour.RED ? 1 : 0;
		int r = cellColour == HexColour.GREEN ? row % 2 : 1- row % 2;
//		System.out.println("p " + p + ", q " + q + ", r " + r);
		// same level
		// west , east
		add(row, col-1, level, answer, maxRows, maxCols);
		add(row, col+1, level, answer, maxRows, maxCols);
		// nw, ne, se, sw
		add(row-1, col-1+r , level, answer, maxRows, maxCols);
		add(row-1, col  +r , level, answer, maxRows, maxCols);
		add(row+1, col  +r, level, answer, maxRows, maxCols);
		add(row+1, col-1+r, level, answer, maxRows, maxCols);
		
		// up level
		// ne
		add(row -1 + q, col+r , level +1, answer, maxRows, maxCols);
		// south
		add(row    +q, col,   level +1, answer, maxRows, maxCols);
		// nw 
		add(row -1 + q, col-1+r, level+1, answer, maxRows, maxCols);
		
		// down level
		// n
		add(row -1 + p, col      , level -1, answer, maxRows, maxCols);
		// se
		add(row     +p, col    +r, level -1, answer, maxRows, maxCols);
		// sw 
		add(row    + p, col -1 +r, level-1, answer, maxRows, maxCols);

		
		return answer;
	}

	private List<Cell> add(int ro, int co, int lvl, List<Cell> list, int maxRows, int maxCols) {
//		System.out.println("ro " + ro + ", col " + co + ", level " + lvl);
		
		if (ro > 0 && ro <= maxRows && co > 0 && co <= maxCols && lvl >= bottom && lvl <= top) {
			list.add(new Cell(ro, co, lvl));
		}
		return list;
	}

	@Override
	public String toString() {
		return "Cell " + row + ", " + col + ((level > 0) ? ", +" : ", ") + level + " (" + cellColour + ")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + col;
		result = prime * result + level;
		result = prime * result + row;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Cell other = (Cell) obj;
		if (col != other.col)
			return false;
		if (level != other.level)
			return false;
		if (row != other.row)
			return false;
		return true;
	}

}
