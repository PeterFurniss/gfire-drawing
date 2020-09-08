package uk.co.furniss.xlsx;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelBook {
	private final String filePath;
	private final String bookName;
	private final Workbook workbook;
	private FileInputStream inputStream;

	public ExcelBook(String filePath) throws FileNotFoundException {
		this.filePath = filePath;

		inputStream = new FileInputStream(new File(filePath));

		try {
			workbook = new XSSFWorkbook(inputStream);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read " + filePath, e);
		}
		bookName = filePath.replaceFirst(".*(?:/|\\\\)", "").replaceFirst("\\.\\w+", "");
	}

	public void close() {
		try {
			workbook.close();
			inputStream.close();
		} catch (IOException e) {
			throw new IllegalStateException("Failed to close Excel workbook " + bookName + " properly", e);
		}
	}

	public void showSheet( String sheetName ) {
		Sheet aSheet = getSheet(sheetName);

		for (Row row : aSheet) {
			for (Cell cell : row) {
				switch (cell.getCellType()) {
				case STRING:
					System.out.print(cell.getStringCellValue());
					break;
				case BOOLEAN:
					System.out.print("B:" + cell.getBooleanCellValue());
					break;
				case NUMERIC:
					System.out.print("N:" + cell.getNumericCellValue());
					break;
				case BLANK:
					System.out.print("blank");
					break;
				default:
					System.out.print("Unrecognised cell type " + cell.getCellType());
					break;
				}
				System.out.print(", ");
			}
			System.out.println();
		}
	}

	private Sheet getSheet( String sheetName ) {
		Sheet aSheet = workbook.getSheet(sheetName);
		if (aSheet == null) {
			throw new IllegalArgumentException(
			        "Workbook '" + bookName + "' does not contain a sheet '" + sheetName + "'");
		}
		return aSheet;
	}

	public List<Map<String, String>> readCellsAsStrings( String sheetName, List<String> colNames ) {
		Sheet aSheet = getSheet(sheetName);
		boolean firstRow = true;
		List<Map<String, String>> rows = new ArrayList<>();

		for (Row row : aSheet) {
			Map<String, String> line = new HashMap<>();
			// tried to do this with iterator, but an empty cell is undefined, so row.next() skips it !
			// doing it this way also allows comment or parked columns
			for (int i=0; i < colNames.size() ; i++) {
				String colName = colNames.get(i);
				Cell cell = row.getCell(i);
				if (firstRow) {
					if (cell == null || !colName.equals(cell.getStringCellValue())) {
							throw new IllegalArgumentException("Mismatched header line, expected " + colName 
									+ " but was " + cell.getStringCellValue());
					}
				} else {
					if (cell != null) {
    					switch (cell.getCellType()) {
    					case STRING:
    						line.put(colName, cell.getStringCellValue());
    						break;
    					case BOOLEAN:
    						line.put(colName, Boolean.toString(cell.getBooleanCellValue()));
    						break;
    					case NUMERIC:
    						line.put(colName, convertAsNumber(cell));
    						break;
    					case BLANK:
    						line.put(colName, "");
    						break;
    					case FORMULA:
    						switch (cell.getCachedFormulaResultType()) {
        					case STRING:
        						line.put(colName, cell.getStringCellValue());
        						break;
        					case BOOLEAN:
        						line.put(colName, Boolean.toString(cell.getBooleanCellValue()));
        						break;
        					case NUMERIC:
        						line.put(colName, convertAsNumber(cell));
        						break;
        					case ERROR:
        						throw new IllegalArgumentException("Error in formula cell for column " + colName);
    						default:
    							throw new IllegalStateException("Surprise formula cell type " + 
    									cell.getCachedFormulaResultType() + " for column " + colName);
    						}
    						break;
    					default:
							throw new IllegalStateException("Surprise  cell type " + 
									cell.getCellType() + " for column " + colName);
    					}
					} else {
						// treat empty/missing cells as empty string
						line.put(colName, "");
					}
					
				}
			}

			if (firstRow) {
				firstRow = false;
			} else {
				rows.add(line);
			}

		}
		return rows;

	}

	public String convertAsNumber( Cell cell ) {
		double asDouble = cell.getNumericCellValue();
		if (asDouble == (int) asDouble) {
			return Integer.toString((int) asDouble);
		} else {
			return Double.toString(asDouble);
		}
	}
		
	public List<Map<String, String>> readCellsAsStringsOld( String sheetName, List<String> colNames ) {
		Sheet aSheet = getSheet(sheetName);
		boolean firstRow = true;
		List<Map<String, String>> rows = new ArrayList<>();
		for (Row row : aSheet) {
			Map<String, String> line = new HashMap<>();
			Iterator<String> header = colNames.iterator();
			for (Cell cell : row) {
				if (header.hasNext()) {
					String colName = header.next();
					if (firstRow) {
						if (!colName.equals(cell.getStringCellValue())) {
							throw new IllegalArgumentException("Mismatched header line, expected " + colName 
									+ " but was " + cell.getStringCellValue());
						}
					} else {
						switch (cell.getCellType()) {
						case STRING:
							line.put(colName, cell.getStringCellValue());
							break;
						case BOOLEAN:
							line.put(colName, Boolean.toString(cell.getBooleanCellValue()));
							break;
						case NUMERIC:
							line.put(colName, convertAsNumber(cell));
							break;
						case BLANK:
							line.put(colName, "FALSE");
							break;
						case FORMULA:
							line.put(colName, cell.getStringCellValue());
							break;
						default:
							System.out.print("Unrecognised cell type " + cell.getCellType());
							break;
						}
					}
				} else {
					// extra cell in the row
					if (firstRow) {
						// this is wrong - probably
						throw new IllegalArgumentException("Extra header(s) " + cell.getStringCellValue());

					}
				}
			}

			if (firstRow) {
				firstRow = false;
			} else {
				// fill in any trailing empty columns
				while (header.hasNext()) {
					line.put(header.next(), "");
				}
			
				rows.add(line);
			}

		}
		return rows;

	}
	

}
