package tools;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ExcelUtilities {
    public static String getCellValue(File file, String sheetName, int rowNum, int colNum) {
        try(FileInputStream objFileInputStream = new FileInputStream(file)) {
            XSSFWorkbook objXSSFWorkbook = new XSSFWorkbook(objFileInputStream);
            XSSFSheet objXSSFSheet = objXSSFWorkbook.getSheet(sheetName);
            XSSFRow objXSSFRow = objXSSFSheet.getRow(rowNum);
            XSSFCell objXSSFCell = objXSSFRow.getCell(colNum);
            return objXSSFCell.getRawValue();
        } catch (IOException e) {
            Log.fail("Failed to get cell{" + rowNum + "," + colNum + "} value! - " + e.getMessage());
            return null;
        }
    }

    public static void setCellValue(File file, String sheetName, int rowNum, int colNum, String value) {
        try (FileInputStream objFileInputStream = new FileInputStream(file)) {
            XSSFWorkbook objXSSFWorkbook = new XSSFWorkbook(objFileInputStream);
            XSSFSheet objXSSFSheet = objXSSFWorkbook.getSheet(sheetName);
            XSSFRow objXSSFRow = objXSSFSheet.getRow(rowNum);
            if (objXSSFRow == null) {
                objXSSFRow = objXSSFSheet.createRow(rowNum);
            }
            XSSFCell objXSSFCell = objXSSFRow.getCell(colNum);
            if (objXSSFCell == null) {
                objXSSFCell = objXSSFRow.createCell(colNum);
            }
            objXSSFCell.setCellValue(value);
            try (FileOutputStream objFileOutputStream = new FileOutputStream(file)) {
                objXSSFWorkbook.write(objFileOutputStream);
            }
        } catch (IOException e) {
            Log.fail("Failed to set cell{" + rowNum + "," + colNum + "} value (\"" + value + "\")! - " + e.getMessage());
        }
    }
}
