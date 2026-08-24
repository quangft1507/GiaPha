import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.util.*;

public class ReadAndConvert {

    static String cellStr(Cell c) {
        if (c == null) return "";
        if (c.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(c)) {
                return c.getLocalDateTimeCellValue().toLocalDate()
                       .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }
            double v = c.getNumericCellValue();
            if (v == Math.floor(v)) return String.valueOf((long) v);
            return String.valueOf(v);
        }
        if (c.getCellType() == CellType.STRING) return c.getStringCellValue().trim();
        return "";
    }

    public static void main(String[] args) throws Exception {
        Workbook wb = new XSSFWorkbook(new FileInputStream("input.xlsx"));
        Sheet sheet = wb.getSheetAt(1); // "Thành viên"

        // --- PASS 1: Build map from Mã GP -> sequential STT ---
        // Col[0]=Mã GP, Col[1]=Họ tên, Col[4]=Giới tính,
        // Col[5]=Ngày Sinh, Col[15]=Ngày mất, Col[22]=Mã GP Bố, Col[24]=Mã GP Mẹ, Col[26]=Mã GP Vợ/Chồng
        
        Map<String, Integer> maGpToStt = new LinkedHashMap<>();
        int sttCounter = 1;
        
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row r = sheet.getRow(i);
            if (r == null) continue;
            String maGp = cellStr(r.getCell(0));
            if (maGp.isEmpty()) continue;
            maGpToStt.put(maGp, sttCounter++);
        }

        // --- PASS 2: Build output rows ---
        // Output cols: STT | Họ và Tên | Giới tính | Ngày sinh | Ngày mất | Là con của STT | Là Vợ/Chồng của STT
        
        Workbook out = new XSSFWorkbook();
        Sheet outSheet = out.createSheet("Gia Pha");

        // Header
        CellStyle headerStyle = out.createCellStyle();
        Font font = out.createFont();
        font.setBold(true);
        headerStyle.setFont(font);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        String[] headers = {"STT", "Họ và Tên", "Giới tính (Nam/Nữ)", "Ngày sinh (DD/MM/YYYY)",
                            "Ngày mất (DD/MM/YYYY)", "Là con của STT", "Là Vợ/Chồng của STT"};
        Row hRow = outSheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell c = hRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
            outSheet.setColumnWidth(i, 22 * 256);
        }

        // Track which Mã GP is a "spouse" to avoid duplicating as children
        // If col[26] (Mã GP Vợ/Chồng) is set, then this person is spouse of that person
        // If col[22] (Mã GP Bố) is set, then this person is child of Bố
        // If col[24] (Mã GP Mẹ) is set and no Bố, then this person is child of Mẹ
        
        int outRow = 1;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row r = sheet.getRow(i);
            if (r == null) continue;
            
            String maGp    = cellStr(r.getCell(0));
            String hoTen   = cellStr(r.getCell(1));
            String gioiTinh = cellStr(r.getCell(4));
            String ngaySinh = cellStr(r.getCell(5));
            String ngayMat  = cellStr(r.getCell(15));
            String maGpBo   = cellStr(r.getCell(22));
            String maGpMe   = cellStr(r.getCell(24));
            String maGpVoChong = cellStr(r.getCell(26));
            
            if (maGp.isEmpty()) continue;
            
            // Normalize gender
            if ("Nam".equals(gioiTinh)) gioiTinh = "Nam";
            else if ("Nữ".equals(gioiTinh) || "N?".equals(gioiTinh) || "N\u1EEF".equals(gioiTinh)) gioiTinh = "Nữ";
            
            // Normalize date (already dd/MM/yyyy from POI, but may have time)
            if (!ngaySinh.isEmpty() && ngaySinh.length() > 10) ngaySinh = ngaySinh.substring(0, 10);
            if (!ngayMat.isEmpty() && ngayMat.length() > 10) ngayMat = ngayMat.substring(0, 10);
            // If name is empty or "CHƯA XÁC ĐỊNH", just use empty name
            if (hoTen.isEmpty() || hoTen.contains("CH?A") || hoTen.contains("CHƯA")) hoTen = "";
            
            // Find parent STT (prefer Bố, fall back to Mẹ)
            String parentStt = "";
            if (!maGpBo.isEmpty() && maGpToStt.containsKey(maGpBo)) {
                parentStt = String.valueOf(maGpToStt.get(maGpBo));
            } else if (!maGpMe.isEmpty() && maGpToStt.containsKey(maGpMe)) {
                parentStt = String.valueOf(maGpToStt.get(maGpMe));
            }
            
            // Find spouse STT
            String spouseStt = "";
            if (!maGpVoChong.isEmpty() && maGpToStt.containsKey(maGpVoChong)) {
                spouseStt = String.valueOf(maGpToStt.get(maGpVoChong));
            }
            
            int stt = maGpToStt.get(maGp);
            
            Row row = outSheet.createRow(outRow++);
            row.createCell(0).setCellValue(stt);
            row.createCell(1).setCellValue(hoTen);
            row.createCell(2).setCellValue(gioiTinh);
            row.createCell(3).setCellValue(ngaySinh);
            row.createCell(4).setCellValue(ngayMat);
            if (!parentStt.isEmpty())  row.createCell(5).setCellValue(Integer.parseInt(parentStt));
            if (!spouseStt.isEmpty()) row.createCell(6).setCellValue(Integer.parseInt(spouseStt));
        }

        // Save output
        try (FileOutputStream fos = new FileOutputStream("giapha_converted.xlsx")) {
            out.write(fos);
        }
        out.close();
        wb.close();
        
        System.out.println("=== DONE! Converted " + (outRow - 1) + " members ===");
        System.out.println("Output saved to: giapha_converted.xlsx");
    }
}
