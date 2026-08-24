import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileOutputStream;

public class CreateExcel {
    public static void main(String[] args) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Gia Pha");
        
        String[] headers = {"STT", "Họ và Tên", "Giới tính (Nam/Nữ)", "Ngày sinh (DD/MM/YYYY)", "Ngày mất (DD/MM/YYYY)", "Là con của STT", "Là Vợ/Chồng của STT"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }
        
        Object[][] data = {
            {1, "Phùng Văn Gốc", "Nam", "01/01/1930", "", null, null},
            {2, "Vợ Phùng Văn Gốc", "Nữ", "01/01/1935", "", null, 1},
            {3, "Phùng Văn Bôn", "Nam", "01/01/1960", "", 1, null},
            {4, "Vợ ông Bôn", "Nữ", "01/01/1962", "", null, 3},
            {5, "Con ông Bôn", "Nam", "01/01/1985", "", 3, null},
            {6, "Phùng Văn Chuyển", "Nam", "01/01/1962", "", 1, null},
            {7, "Vợ ông Chuyển", "Nữ", "01/01/1965", "", null, 6},
            {8, "Con 1 ông Chuyển", "Nam", "01/01/1986", "", 6, null},
            {9, "Con 2 ông Chuyển", "Nam", "01/01/1988", "", 6, null},
            {10, "Con 3 ông Chuyển", "Nam", "01/01/1990", "", 6, null},
            {11, "Phùng Văn Bừng", "Nam", "01/01/1964", "", 1, null},
            {12, "Con ông Bừng", "Nam", "01/01/1988", "", 11, null},
            {13, "Phùng Văn Bảo", "Nam", "01/01/1966", "", 1, null},
            {14, "Con 1 ông Bảo", "Nam", "01/01/1990", "", 13, null},
            {15, "Con 2 ông Bảo", "Nam", "01/01/1992", "", 13, null},
            {16, "Phùng Văn Để", "Nam", "01/01/1968", "", 1, null},
            {17, "Con 1 ông Để", "Nam", "01/01/1992", "", 16, null},
            {18, "Con 2 ông Để", "Nam", "01/01/1994", "", 16, null},
            {19, "Con 3 ông Để", "Nam", "01/01/1996", "", 16, null},
            {20, "Con 4 ông Để", "Nam", "01/01/1998", "", 16, null},
            {21, "Con 5 ông Để", "Nam", "01/01/2000", "", 16, null}
        };
        
        int rowNum = 1;
        for (Object[] rowData : data) {
            Row row = sheet.createRow(rowNum++);
            for (int i = 0; i < rowData.length; i++) {
                if (rowData[i] == null) continue;
                if (rowData[i] instanceof Integer) {
                    row.createCell(i).setCellValue((Integer) rowData[i]);
                } else {
                    row.createCell(i).setCellValue((String) rowData[i]);
                }
            }
        }
        
        try (FileOutputStream out = new FileOutputStream("du_lieu_nhap_thu.xlsx")) {
            workbook.write(out);
        }
        workbook.close();
    }
}
