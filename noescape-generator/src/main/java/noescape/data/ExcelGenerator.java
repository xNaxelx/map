package noescape.data;

import noescape.model.CourtCase;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Year;
import java.util.List;

import static java.lang.Integer.parseInt;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static org.apache.poi.ss.usermodel.HorizontalAlignment.RIGHT;

public class ExcelGenerator {

    public static byte[] generateExcel(List<CourtCase> courtCases) throws IOException {
        try (var workbook = new XSSFWorkbook()) {
            for (int year = Year.now().getValue(); year >= 2022; year--) {
                int finalYear = year;
                var selectedCases = courtCases.stream()
                        .filter(courtCase -> getYear(courtCase) == finalYear)
                        .toList();
                addSheet(workbook, String.valueOf(year), selectedCases);
            }
            var baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    private static int getYear(CourtCase courtCase) {
        if (courtCase.arrestDate() != null) {
            return courtCase.arrestDate().getYear();
        }
        if (courtCase.registrationDate() != null) {
            return courtCase.registrationDate().getYear();
        }
        if (courtCase.publicationDate() != null) {
            return courtCase.publicationDate().getYear();
        }
        return -1;
    }

    private static void addSheet(XSSFWorkbook workbook, String sheetName, List<CourtCase> courtCases) {
        Sheet sheet = workbook.createSheet(sheetName);
        Row header = sheet.createRow(0);

        Cell headerCell = header.createCell(0);
        headerCell.setCellValue("Кордон");

        headerCell = header.createCell(1);
        headerCell.setCellValue("Прикордонний знак");

        headerCell = header.createCell(2);
        headerCell.setCellValue("Дата затримання");

        headerCell = header.createCell(3);
        headerCell.setCellValue("Час затримання");

        headerCell = header.createCell(4);
        headerCell.setCellValue("Дата оприлюднення");

        headerCell = header.createCell(5);
        headerCell.setCellValue("Відстань до кордону");

        headerCell = header.createCell(6);
        headerCell.setCellValue("Прикордонний наряд");

        headerCell = header.createCell(7);
        headerCell.setCellValue("Населенний пункт");

        headerCell = header.createCell(8);
        headerCell.setCellValue("Громада");

        headerCell = header.createCell(9);
        headerCell.setCellValue("Район");

        headerCell = header.createCell(10);
        headerCell.setCellValue("Область");

        headerCell = header.createCell(11);
        headerCell.setCellValue("Штраф, грн");

        headerCell = header.createCell(12);
        headerCell.setCellValue("Номер справи");

        headerCell = header.createCell(13);
        headerCell.setCellValue("Координати");

        headerCell = header.createCell(14);
        headerCell.setCellValue("Посилання");

        var format = workbook.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd");
        var dataCellStyle = workbook.createCellStyle();
        dataCellStyle.setDataFormat(format);
        var alignRightCellStyle = workbook.createCellStyle();
        alignRightCellStyle.setAlignment(RIGHT);

        int rowNum = 1;
        for (var courtCase : courtCases) {
            Row row = sheet.createRow(rowNum++);

            Cell cell = row.createCell(0);
            cell.setCellValue(StringUtils.replace(courtCase.country(), "BY2", "BY"));

            cell = row.createCell(1);
            cell.setCellValue(courtCase.borderSign());

            if (courtCase.arrestDate() != null) {
                cell = row.createCell(2);
                cell.setCellStyle(dataCellStyle);
                cell.setCellValue(courtCase.arrestDate());
            }

            if (courtCase.arrestTime() != null) {
                cell = row.createCell(3);
                cell.setCellValue(courtCase.arrestTime().toString());
                cell.setCellStyle(alignRightCellStyle);
            }

            cell = row.createCell(4);
            cell.setCellValue(courtCase.publicationDate());
            cell.setCellStyle(dataCellStyle);

            if (courtCase.distance() != null) {
                cell = row.createCell(5);
                cell.setCellValue(courtCase.distance());
            }

            cell = row.createCell(6);
            cell.setCellValue(courtCase.guard());

            cell = row.createCell(7);
            cell.setCellValue(courtCase.locality().settlement());

            cell = row.createCell(8);
            cell.setCellValue(courtCase.locality().gromada());

            cell = row.createCell(9);
            cell.setCellValue(courtCase.locality().rayon());

            cell = row.createCell(10);
            cell.setCellValue(courtCase.locality().oblast());

            cell = row.createCell(11);
            if (isNumeric(courtCase.fine())) {
                int fine = parseInt(courtCase.fine());
                cell.setCellValue(fine);
            } else if (courtCase.fine().equals("-1")) {
                cell.setCellValue("-");
            } else if (!courtCase.fine().isEmpty()) {
                cell.setCellValue(courtCase.fine());
            }

            cell = row.createCell(12);
            cell.setCellValue(courtCase.caseNumber());

            if (courtCase.position() != null) {
                cell = row.createCell(13);
                cell.setCellValue(courtCase.position().round(5).toString());
            }

            cell = row.createCell(14);
            cell.setCellValue("https://reyestr.court.gov.ua/Review/" + courtCase.caseId());
        }

        sheet.createFreezePane(0, 1);

        var range = CellRangeAddress.valueOf("A1:L" + sheet.getLastRowNum());
        sheet.setAutoFilter(range);

        for (int i = 0; i < 15; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
