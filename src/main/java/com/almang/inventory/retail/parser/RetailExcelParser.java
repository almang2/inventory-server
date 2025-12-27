package com.almang.inventory.retail.parser;

import com.almang.inventory.retail.dto.excel.RetailExcelRowDto;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RetailExcelParser {

    // Cell 1: 상품 코드 (Product Code)
    // Cell 2: 상품명 (Product Name)
    // Cell 3: 수량 (Quantity)
    // Cell 4: 실매출 (Actual Sales)
    private static final int COLUMN_CODE = 1;
    private static final int COLUMN_NAME = 2;
    private static final int COLUMN_QUANTITY = 3;
    private static final int COLUMN_SALES = 4;

    public List<RetailExcelRowDto> parse(InputStream inputStream) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            List<RetailExcelRowDto> rows = new ArrayList<>();

            // 헤더 행 스킵 (첫 번째 행이 헤더라고 가정)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                String code = getCellValueAsString(row.getCell(COLUMN_CODE), formatter);
                if (code == null || code.isEmpty()) {
                    continue; // 기본 스킵 규칙: 코드 없음
                }

                String productName = getCellValueAsString(row.getCell(COLUMN_NAME), formatter);
                if (productName == null || productName.isEmpty()) {
                    productName = ""; // 기본 정리: 상품명 공백 허용
                }

                BigDecimal quantity = getCellValueAsBigDecimal(row.getCell(COLUMN_QUANTITY), formatter);
                if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                    continue; // 기본 스킵 규칙: 수량 0 이하
                }

                Integer actualSales = getCellValueAsInteger(row.getCell(COLUMN_SALES), formatter);

                // rowIndex는 "엑셀 상의 행 번호"로 유지(헤더 포함 기준 1부터)
                int rowIndex = i + 1;

                rows.add(new RetailExcelRowDto(
                        rowIndex,
                        code,
                        productName,
                        quantity,
                        actualSales
                ));
            }

            return rows;
        }
    }

    private String getCellValueAsString(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().trim();
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return formatter.formatCellValue(cell).trim(); // "00123" 보존
        }
        return "";
    }

    private BigDecimal getCellValueAsBigDecimal(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return BigDecimal.ZERO;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros();
        }
        if (cell.getCellType() == CellType.STRING) {
            try {
                String v = cell.getStringCellValue().trim();
                if (v.isEmpty()) {
                    return BigDecimal.ZERO;
                }
                String cleaned = v.replace(",", "").trim();
                return new BigDecimal(cleaned).stripTrailingZeros();
            } catch (NumberFormatException e) {
                log.warn("[RetailExcelParser] BigDecimal 파싱 실패 - cellValue: {}", cell.getStringCellValue(), e);
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    private Integer getCellValueAsInteger(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            double numericValue = cell.getNumericCellValue();
            if (numericValue != (int) numericValue) {
                log.warn("[RetailExcelParser] 정수로 변환되는 값에 소수점이 있습니다. 반올림 처리합니다. - 원본값: {}, 변환값: {}",
                        numericValue, Math.round(numericValue));
            }
            return (int) Math.round(numericValue);
        }
        if (cell.getCellType() == CellType.STRING) {
            try {
                String v = cell.getStringCellValue().replace(",", "").trim();
                if (v.isEmpty()) {
                    return null;
                }
                if (v.contains(".")) {
                    double d = Double.parseDouble(v);
                    log.warn("[RetailExcelParser] 정수로 변환되는 문자열에 소수점이 있습니다. 반올림 처리합니다. - 원본값: {}, 변환값: {}",
                            v, Math.round(d));
                    return (int) Math.round(d);
                }
                return Integer.parseInt(v);
            } catch (NumberFormatException e) {
                log.warn("[RetailExcelParser] Integer 파싱 실패 - cellValue: {}", cell.getStringCellValue(), e);
                return null;
            }
        }
        return null;
    }
}
