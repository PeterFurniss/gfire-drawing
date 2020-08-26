package uk.co.furniss.xlsx;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

public class ExcelTest {

	public static void main( String[] args ) throws FileNotFoundException {
		if (args.length < 1) {
			throw new IllegalArgumentException("Must provide a filename");
		}
		String filePath = args[0];
		ExcelBook tbook = new ExcelBook(filePath);
//		tbook.showFirstSheet();
		List<List<String>> defn = tbook.readCellsAsStrings("Sheet1", Arrays.asList("image", "topleft", "topright", "botleft", "botright", "botmid"));
		tbook.close();
		System.out.println(defn);
	}

}
