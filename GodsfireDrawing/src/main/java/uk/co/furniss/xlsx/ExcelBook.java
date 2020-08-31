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

	void showSheet( String sheetName ) {
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

	public List<Map<String, String>> readCellsAsStrings( String sheetName, List<String> expectedHeader ) {
		Sheet aSheet = getSheet(sheetName);
		boolean firstRow = true;
		List<Map<String, String>> rows = new ArrayList<>();
		List<String> colNames = new ArrayList<>();
		for (Row row : aSheet) {
			Map<String, String> line = new HashMap<>();
			Iterator<String> header = expectedHeader.iterator();
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
							line.put(colName, Integer.toString((int) cell.getNumericCellValue()));
							break;
						case BLANK:
							line.put(colName, "");
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
