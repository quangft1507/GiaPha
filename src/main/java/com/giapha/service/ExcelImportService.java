package com.giapha.service;

import com.giapha.dto.PersonCreateRequest;
import com.giapha.enums.Gender;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class ExcelImportService {
    
    @Autowired
    private PersonService personService;
    
    private static final String[] HEADERS = {
        "STT", "Họ và Tên", "Giới tính (Nam/Nữ)", "Ngày sinh (DD/MM/YYYY)", 
        "Ngày mất (DD/MM/YYYY)", "Là con của STT", "Là Vợ/Chồng của STT"
    };

    public byte[] generateTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Gia Pha");
            Row headerRow = sheet.createRow(0);
            
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 20 * 256);
            }
            
            // Add a sample row
            Row sample1 = sheet.createRow(1);
            sample1.createCell(0).setCellValue(1);
            sample1.createCell(1).setCellValue("Nguyễn Văn A");
            sample1.createCell(2).setCellValue("Nam");
            sample1.createCell(3).setCellValue("01/01/1950");
            
            Row sample2 = sheet.createRow(2);
            sample2.createCell(0).setCellValue(2);
            sample2.createCell(1).setCellValue("Trần Thị B");
            sample2.createCell(2).setCellValue("Nữ");
            sample2.createCell(3).setCellValue("15/02/1955");
            sample2.createCell(6).setCellValue(1);
            
            Row sample3 = sheet.createRow(3);
            sample3.createCell(0).setCellValue(3);
            sample3.createCell(1).setCellValue("Nguyễn Văn C");
            sample3.createCell(2).setCellValue("Nam");
            sample3.createCell(3).setCellValue("10/10/1980");
            sample3.createCell(5).setCellValue(1);

            workbook.write(out);
            return out.toByteArray();
        }
    }
    
    @Transactional
    public void importExcel(Long treeId, MultipartFile file) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            Map<Integer, PersonCreateRequest> requestMap = new HashMap<>();
            Map<Integer, Integer> childToParent = new HashMap<>();
            Map<Integer, Integer> spouseToSpouse = new HashMap<>();
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                Cell sttCell = row.getCell(0);
                if (sttCell == null || sttCell.getCellType() == CellType.BLANK) continue;
                
                int stt = (int) sttCell.getNumericCellValue();
                
                String fullName = getCellString(row.getCell(1));
                if (fullName == null || fullName.trim().isEmpty()) continue;
                
                String genderStr = getCellString(row.getCell(2));
                Gender gender = "Nam".equalsIgnoreCase(genderStr) ? Gender.NAM : Gender.NU;
                
                String birthDateStr = getCellString(row.getCell(3));
                LocalDate birthDate = parseDate(birthDateStr, formatter);
                
                String deathDateStr = getCellString(row.getCell(4));
                LocalDate deathDate = parseDate(deathDateStr, formatter);
                Boolean isDeceased = deathDate != null || (deathDateStr != null && !deathDateStr.trim().isEmpty());
                
                Integer parentStt = getCellInteger(row.getCell(5));
                Integer spouseStt = getCellInteger(row.getCell(6));
                
                PersonCreateRequest req = new PersonCreateRequest();
                
                // Parse names
                String[] nameParts = fullName.trim().split("\\s+");
                if (nameParts.length > 0) {
                    req.setHo(nameParts[0]);
                    req.setTen(nameParts[nameParts.length - 1]);
                    if (nameParts.length > 2) {
                        StringBuilder tenDem = new StringBuilder();
                        for (int j = 1; j < nameParts.length - 1; j++) {
                            tenDem.append(nameParts[j]).append(" ");
                        }
                        req.setTenDem(tenDem.toString().trim());
                    } else if (nameParts.length == 2) {
                        req.setHo(nameParts[0]);
                        req.setTen(nameParts[1]);
                        req.setTenDem("");
                    }
                }
                
                req.setGender(gender);
                req.setBirthDate(birthDate);
                req.setDeathDate(deathDate);
                req.setIsDeceased(isDeceased);
                
                requestMap.put(stt, req);
                if (parentStt != null) childToParent.put(stt, parentStt);
                if (spouseStt != null) spouseToSpouse.put(stt, spouseStt);
            }
            
            Map<Integer, Long> sttToRealId = new HashMap<>();
            List<Integer> roots = new ArrayList<>();
            
            for (Integer stt : requestMap.keySet()) {
                if (!childToParent.containsKey(stt) && !spouseToSpouse.containsKey(stt)) {
                    roots.add(stt);
                }
            }
            
            if (roots.isEmpty() && !requestMap.isEmpty()) {
                roots.add(requestMap.keySet().iterator().next());
            }
            
            for (Integer rootStt : roots) {
                Long id = personService.createPerson(treeId, requestMap.get(rootStt)).getId();
                sttToRealId.put(rootStt, id);
            }
            
            boolean progress = true;
            int maxIterations = requestMap.size() * 2;
            int iter = 0;
            
            while(progress && iter < maxIterations) {
                progress = false;
                iter++;
                for (Integer stt : new ArrayList<>(requestMap.keySet())) {
                    if (sttToRealId.containsKey(stt)) continue; 
                    
                    if (spouseToSpouse.containsKey(stt)) {
                        Integer partnerStt = spouseToSpouse.get(stt);
                        if (sttToRealId.containsKey(partnerStt)) {
                            Long newId = personService.addSpouse(sttToRealId.get(partnerStt), requestMap.get(stt)).getId();
                            sttToRealId.put(stt, newId);
                            progress = true;
                        }
                    } else if (childToParent.containsKey(stt)) {
                        Integer parentStt = childToParent.get(stt);
                        if (sttToRealId.containsKey(parentStt)) {
                            Long newId = personService.addChild(sttToRealId.get(parentStt), requestMap.get(stt)).getId();
                            sttToRealId.put(stt, newId);
                            progress = true;
                        }
                    }
                }
            }
            
            if (sttToRealId.size() < requestMap.size()) {
                throw new RuntimeException("Một số người chưa được tạo do mối quan hệ vòng lặp hoặc dữ liệu không hợp lệ. Vui lòng kiểm tra lại cột STT Cha/Vợ chồng.");
            }
        }
    }
    
    private String getCellString(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue();
        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }
            return String.valueOf((long)cell.getNumericCellValue());
        }
        return null;
    }
    
    private Integer getCellInteger(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) return (int) cell.getNumericCellValue();
        if (cell.getCellType() == CellType.STRING) {
            try {
                return Integer.parseInt(cell.getStringCellValue().trim());
            } catch(Exception e) { return null; }
        }
        return null;
    }
    
    private LocalDate parseDate(String str, DateTimeFormatter formatter) {
        if (str == null || str.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(str.trim(), formatter);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
