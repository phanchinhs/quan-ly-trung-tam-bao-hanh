package com.example.btl.service;

import com.example.btl.entity.Device;
import com.example.btl.entity.RepairOrder;
import com.example.btl.entity.RepairStatus;
import com.example.btl.util.CurrencyUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportExcelService {

    private final RepairService repairService;
    private final DeviceService deviceService;
    private final CurrencyUtils currencyUtils;

    public ReportExcelService(RepairService repairService,
                              DeviceService deviceService,
                              CurrencyUtils currencyUtils) {
        this.repairService = repairService;
        this.deviceService = deviceService;
        this.currencyUtils = currencyUtils;
    }

    public byte[] generateProgressReport() {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            LocalDate today = LocalDate.now();
            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter dateTimeFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle moneyStyle = wb.createCellStyle();
            moneyStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0"));
            moneyStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle titleStyle = wb.createCellStyle();
            Font titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            CellStyle dateCellStyle = wb.createCellStyle();
            dateCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // ===== Sheet 1: Tổng quan =====
            Sheet overview = wb.createSheet("Tổng quan");
            overview.setColumnWidth(0, 45 * 256);
            overview.setColumnWidth(1, 25 * 256);

            long totalOrders = repairService.countAll();
            long activeOrders = repairService.countByStatus(RepairStatus.RECEIVED)
                    + repairService.countByStatus(RepairStatus.IN_PROGRESS)
                    + repairService.countByStatus(RepairStatus.WAITING_PARTS);
            BigDecimal totalRevenue = repairService.findPaidBetween(
                    LocalDateTime.of(2000, 1, 1, 0, 0), LocalDateTime.now()).stream()
                    .map(RepairOrder::getTotalCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Row r0 = overview.createRow(0);
            setCell(r0.createCell(0), "BÁO CÁO TIẾN ĐỘ XỬ LÝ SỬA CHỮA", titleStyle);
            Row r1 = overview.createRow(1);
            setCell(r1.createCell(0), "Ngày lập báo cáo: " + today.format(dateFmt));

            Row r3 = overview.createRow(3);
            setCell(r3.createCell(0), "Tổng số phiếu sửa chữa", headerStyle);
            setCell(r3.createCell(1), totalOrders, headerStyle);
            Row r4 = overview.createRow(4);
            setCell(r4.createCell(0), "Phiếu đang xử lý");
            setCell(r4.createCell(1), activeOrders, moneyStyle);
            Row r5 = overview.createRow(5);
            setCell(r5.createCell(0), "Phiếu đã hoàn thành");
            setCell(r5.createCell(1), repairService.countByStatus(RepairStatus.PAID), moneyStyle);
            Row r6 = overview.createRow(6);
            setCell(r6.createCell(0), "Tổng doanh thu (VNĐ)");
            setCell(r6.createCell(1), totalRevenue, moneyStyle);
            Row r7 = overview.createRow(7);
            setCell(r7.createCell(0), "Doanh thu bằng chữ");
            setCell(r7.createCell(1), currencyUtils.format(totalRevenue));

            // ===== Sheet 2: Thống kê theo trạng thái =====
            Sheet status = wb.createSheet("Trạng thái phiếu");
            status.setColumnWidth(0, 35 * 256);
            status.setColumnWidth(1, 15 * 256);

            Row s0 = status.createRow(0);
            setCell(s0.createCell(0), "Trạng thái", headerStyle);
            setCell(s0.createCell(1), "Số lượng", headerStyle);

            addStatusRow(status, 1, "Đã tiếp nhận", repairService.countByStatus(RepairStatus.RECEIVED), moneyStyle);
            addStatusRow(status, 2, "Đang sửa chữa", repairService.countByStatus(RepairStatus.IN_PROGRESS), moneyStyle);
            addStatusRow(status, 3, "Chờ linh kiện", repairService.countByStatus(RepairStatus.WAITING_PARTS), moneyStyle);
            addStatusRow(status, 4, "Hoàn thành - chờ thanh toán", repairService.countByStatus(RepairStatus.COMPLETED), moneyStyle);
            addStatusRow(status, 5, "Đã thanh toán", repairService.countByStatus(RepairStatus.PAID), moneyStyle);
            addStatusRow(status, 6, "Đã hủy", repairService.countByStatus(RepairStatus.CANCELLED), moneyStyle);

            // ===== Sheet 3: Doanh thu theo tháng =====
            Sheet revenue = wb.createSheet("Doanh thu theo tháng");
            revenue.setColumnWidth(0, 8 * 256);
            revenue.setColumnWidth(1, 15 * 256);
            revenue.setColumnWidth(2, 22 * 256);

            Row v0 = revenue.createRow(0);
            setCell(v0.createCell(0), "#", headerStyle);
            setCell(v0.createCell(1), "Tháng", headerStyle);
            setCell(v0.createCell(2), "Doanh thu (VNĐ)", headerStyle);

            DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MM/yyyy");
            int rowIdx = 1;
            for (int i = 11; i >= 0; i--) {
                YearMonth ym = YearMonth.now().minusMonths(i);
                LocalDateTime from = ym.atDay(1).atStartOfDay();
                LocalDateTime to = ym.atEndOfMonth().atTime(23, 59, 59);
                BigDecimal monthRevenue = repairService.findPaidBetween(from, to).stream()
                        .map(RepairOrder::getTotalCost)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                Row vRow = revenue.createRow(rowIdx);
                setCell(vRow.createCell(0), rowIdx);
                setCell(vRow.createCell(1), ym.format(monthFmt));
                setCell(vRow.createCell(2), monthRevenue, moneyStyle);
                rowIdx++;
            }

            // ===== Sheet 4: Danh sách phiếu =====
            Sheet ordersSheet = wb.createSheet("Danh sách phiếu");
            String[] orderHeaders = {"Mã phiếu", "Khách hàng", "SĐT", "Thiết bị", "Serial",
                    "Trạng thái", "Ngày tiếp nhận", "Tổng tiền (VNĐ)"};
            for (int i = 0; i < orderHeaders.length; i++) {
                ordersSheet.setColumnWidth(i, 22 * 256);
            }
            Row o0 = ordersSheet.createRow(0);
            for (int i = 0; i < orderHeaders.length; i++) {
                setCell(o0.createCell(i), orderHeaders[i], headerStyle);
            }

            List<RepairOrder> orders = repairService.findAll();
            int oIdx = 1;
            for (RepairOrder order : orders) {
                Row oRow = ordersSheet.createRow(oIdx);
                setCell(oRow.createCell(0), order.getCode());
                setCell(oRow.createCell(1), order.getCustomer() != null ? order.getCustomer().getFullName() : "");
                setCell(oRow.createCell(2), order.getCustomer() != null ? order.getCustomer().getPhone() : "");
                setCell(oRow.createCell(3), order.getDevice() != null ? order.getDevice().getBrand() + " " + order.getDevice().getModel() : "");
                setCell(oRow.createCell(4), order.getDevice() != null ? order.getDevice().getSerialNumber() : "");
                setCell(oRow.createCell(5), order.getStatus().getLabel());
                setCell(oRow.createCell(6), order.getReceivedAt() != null ? order.getReceivedAt().format(dateTimeFmt) : "", dateCellStyle);
                setCell(oRow.createCell(7), order.getTotalCost(), moneyStyle);
                oIdx++;
            }

            // ===== Sheet 5: Bảo hành sắp hết hạn =====
            Sheet warranty = wb.createSheet("Bảo hành sắp hết hạn");
            double[] warrantyWidths = {22, 22, 22, 15, 18};
            for (int i = 0; i < warrantyWidths.length; i++) {
                warranty.setColumnWidth(i, (int) (warrantyWidths[i] * 256));
            }
            String[] warrantyHeaders = {"Serial", "Thiết bị", "Khách hàng", "SĐT", "Ngày hết hạn"};
            Row w0 = warranty.createRow(0);
            for (int i = 0; i < warrantyHeaders.length; i++) {
                setCell(w0.createCell(i), warrantyHeaders[i], headerStyle);
            }

            List<Device> warrantyDevices = deviceService.expiringWarranties(today, today.plusDays(30));
            if (warrantyDevices.isEmpty()) {
                Row wRow = warranty.createRow(1);
                setCell(wRow.createCell(0), "Không có thiết bị nào hết hạn bảo hành trong 30 ngày tới.");
            } else {
                int wIdx = 1;
                for (Device d : warrantyDevices) {
                    Row wRow = warranty.createRow(wIdx);
                    setCell(wRow.createCell(0), d.getSerialNumber());
                    setCell(wRow.createCell(1), d.getBrand() + " " + d.getModel());
                    setCell(wRow.createCell(2), d.getCustomer() != null ? d.getCustomer().getFullName() : "");
                    setCell(wRow.createCell(3), d.getCustomer() != null ? d.getCustomer().getPhone() : "");
                    setCell(wRow.createCell(4), d.getWarrantyEndDate().format(dateFmt));
                    wIdx++;
                }
            }

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo file Excel báo cáo: " + e.getMessage(), e);
        }
    }

    private void addStatusRow(Sheet sheet, int index, String label, long count, CellStyle style) {
        Row row = sheet.createRow(index);
        setCell(row.createCell(0), label);
        setCell(row.createCell(1), count, style);
    }

    private void setCell(Cell cell, Object value, CellStyle style) {
        if (value instanceof BigDecimal) {
            cell.setCellValue(((BigDecimal) value).doubleValue());
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else {
            cell.setCellValue(value == null ? "" : value.toString());
        }
        cell.setCellStyle(style);
    }

    private void setCell(Cell cell, Object value) {
        if (value instanceof BigDecimal) {
            cell.setCellValue(((BigDecimal) value).doubleValue());
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else {
            cell.setCellValue(value == null ? "" : value.toString());
        }
    }
}