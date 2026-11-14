package edu.aiplatform.identity.service;

import edu.aiplatform.shared.error.ApiException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Đọc tệp nhập của Admin: Excel .xlsx (trang tính đầu, dòng đầu là tiêu đề) hoặc CSV UTF-8 cũ (BR-U01-80, 109).
 * Dùng chung cho nhập tài khoản (F10) và gán MSSV hàng loạt (F16); chỉ đọc, không kiểm nghiệp vụ.
 */
final class ImportTableReader {

    private ImportTableReader() {
    }

    /** Một dòng dữ liệu; {@code rowNumber} tính dòng tiêu đề là dòng 1. Cột thiếu hay ô trống là chuỗi rỗng. */
    record TableRow(long rowNumber, Map<String, String> values) {

        String value(String column) {
            return values.getOrDefault(column, "");
        }
    }

    /**
     * Đọc tối đa {@code maxRows} dòng dữ liệu; cột trong {@code required} phải có ở tiêu đề, cột trong
     * {@code optional} đọc nếu có. Tên cột không phân biệt hoa thường.
     */
    static List<TableRow> readRows(byte[] file, List<String> required, List<String> optional, int maxBytes,
                                   int maxRows) {
        if (file == null || file.length == 0) {
            throw invalidFile("Tệp nhập trống");
        }
        if (file.length > maxBytes) {
            throw invalidFile("Tệp nhập vượt 1 MB");
        }
        Set<String> known = new LinkedHashSet<>(required);
        known.addAll(optional);
        List<TableRow> rows = file.length >= 4 && file[0] == 'P' && file[1] == 'K'
                ? readWorkbook(file, required, known, maxRows) : readCsv(decode(file), required, known, maxRows);
        if (rows.size() > maxRows) {
            throw invalidFile("Tệp nhập có quá " + maxRows + " dòng dữ liệu");
        }
        return rows;
    }

    static String checksum(byte[] file) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(file));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 không khả dụng", e);
        }
    }

    static ApiException invalidFile(String message) {
        return ApiException.badRequest("IMPORT_FILE_INVALID", message);
    }

    private static List<TableRow> readWorkbook(byte[] file, List<String> required, Set<String> known, int maxRows) {
        String columnList = String.join(", ", required);
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(file))) {
            if (workbook.isMacroEnabled() || workbook.getNumberOfSheets() == 0) {
                throw invalidFile("Chỉ nhận workbook .xlsx không có macro");
            }
            var sheet = workbook.getSheetAt(0);
            var header = sheet.getRow(0);
            if (header == null) {
                throw invalidFile("Tệp Excel cần đủ cột " + columnList + " ở dòng đầu");
            }
            var formatter = new DataFormatter(Locale.ROOT);
            Map<String, Integer> columns = new HashMap<>();
            for (Cell cell : header) {
                String name = readCell(cell, formatter).strip().toLowerCase(Locale.ROOT);
                if (known.contains(name) && columns.putIfAbsent(name, cell.getColumnIndex()) != null) {
                    throw invalidFile("Tệp Excel có cột trùng: " + name);
                }
            }
            if (!columns.keySet().containsAll(required)) {
                throw invalidFile("Tệp Excel cần đủ cột " + columnList);
            }
            List<TableRow> rows = new ArrayList<>();
            for (var row : sheet) {
                if (row.getRowNum() == 0) {
                    continue;
                }
                Map<String, String> values = new HashMap<>();
                boolean blank = true;
                for (Map.Entry<String, Integer> column : columns.entrySet()) {
                    String value = readCell(row.getCell(column.getValue()), formatter);
                    values.put(column.getKey(), value);
                    blank &= value.isBlank();
                }
                if (blank) {
                    continue;
                }
                rows.add(new TableRow(row.getRowNum() + 1L, values));
                if (rows.size() > maxRows) {
                    throw invalidFile("Tệp nhập có quá " + maxRows + " dòng dữ liệu");
                }
            }
            return rows;
        } catch (ApiException rejected) {
            throw rejected;
        } catch (IOException | RuntimeException unreadable) {
            throw invalidFile("Không đọc được tệp Excel .xlsx");
        }
    }

    private static String readCell(Cell cell, DataFormatter formatter) {
        if (cell != null && (cell.getCellType() == CellType.FORMULA || cell.getCellType() == CellType.ERROR)) {
            throw invalidFile("Tệp Excel không được chứa ô công thức hoặc ô lỗi trong các cột nhập");
        }
        return formatter.formatCellValue(cell);
    }

    private static List<TableRow> readCsv(String text, List<String> required, Set<String> known, int maxRows) {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setIgnoreHeaderCase(true)
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();
        try (CSVParser parser = CSVParser.parse(new StringReader(text), format)) {
            List<String> header = parser.getHeaderNames().stream()
                    .map(name -> name.strip().toLowerCase(Locale.ROOT)).toList();
            if (!header.containsAll(required)) {
                throw invalidFile("Tệp CSV cần đủ cột " + String.join(", ", required));
            }
            List<TableRow> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                if (rows.size() > maxRows) {
                    break;
                }
                Map<String, String> values = new HashMap<>();
                for (String column : known) {
                    values.put(column, value(record, column));
                }
                rows.add(new TableRow(record.getRecordNumber() + 1, values));
            }
            return rows;
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            if (e instanceof ApiException apiException) {
                throw apiException;
            }
            throw invalidFile("Không đọc được tệp CSV");
        }
    }

    /** Dòng thiếu cột (ít dấu phẩy hơn tiêu đề) coi như ô trống thay vì lỗi cả tệp. */
    private static String value(CSVRecord record, String column) {
        return record.isMapped(column) && record.isSet(column) ? record.get(column) : "";
    }

    private static String decode(byte[] file) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(file)).toString();
            // Excel thường lưu UTF-8 kèm BOM.
            return text.startsWith("﻿") ? text.substring(1) : text;
        } catch (CharacterCodingException e) {
            throw invalidFile("Tệp CSV phải mã hóa UTF-8");
        }
    }
}
