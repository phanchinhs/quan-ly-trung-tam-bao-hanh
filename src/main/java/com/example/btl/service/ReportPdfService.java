package com.example.btl.service;

import com.example.btl.entity.Device;
import com.example.btl.entity.RepairOrder;
import com.example.btl.entity.RepairStatus;
import com.example.btl.util.CurrencyUtils;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportPdfService {

    private static final String FONT_PATH = "fonts/arial.ttf";
    private static final String FONT_BOLD_PATH = "fonts/arialbd.ttf";

    private final RepairService repairService;
    private final DeviceService deviceService;
    private final CurrencyUtils currencyUtils;

    private final Font titleFont;
    private final Font headingFont;
    private final Font normal;
    private final Font normalBold;
    private final Font small;
    private final Font smallBold;

    public ReportPdfService(RepairService repairService,
                            DeviceService deviceService,
                            CurrencyUtils currencyUtils) {
        this.repairService = repairService;
        this.deviceService = deviceService;
        this.currencyUtils = currencyUtils;
        this.titleFont = createFont(FONT_BOLD_PATH, 16, Font.BOLD);
        this.headingFont = createFont(FONT_BOLD_PATH, 12, Font.BOLD);
        this.normal = createFont(FONT_PATH, 11, Font.NORMAL);
        this.normalBold = createFont(FONT_BOLD_PATH, 11, Font.BOLD);
        this.small = createFont(FONT_PATH, 10, Font.NORMAL);
        this.smallBold = createFont(FONT_BOLD_PATH, 10, Font.BOLD);
    }

    private Font createFont(String path, float size, int style) {
        try {
            BaseFont bf = BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            return new Font(bf, size, style);
        } catch (Exception e) {
            throw new IllegalStateException("Không tải được font tiếng Việt: " + path, e);
        }
    }

    public byte[] generateProgressReport() {
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        LocalDate today = LocalDate.now();
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Tiêu đề
            Paragraph header = new Paragraph(
                    "TRUNG TÂM BẢO HÀNH & SỬA CHỮA THIẾT BỊ ĐIỆN TỬ", small);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            Paragraph title = new Paragraph("BÁO CÁO TIẾN ĐỘ XỬ LÝ SỬA CHỮA", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4);
            document.add(title);

            Paragraph dateLine = new Paragraph("Ngày lập báo cáo: " + today.format(dateFmt), small);
            dateLine.setAlignment(Element.ALIGN_CENTER);
            document.add(dateLine);

            document.add(new Paragraph(" "));

            // 1. Tổng quan
            heading(document, "1. TỔNG QUAN");

            long totalOrders = repairService.countAll();
            long activeOrders = repairService.countByStatus(RepairStatus.RECEIVED)
                    + repairService.countByStatus(RepairStatus.IN_PROGRESS)
                    + repairService.countByStatus(RepairStatus.WAITING_PARTS);
            BigDecimal totalRevenue = repairService.findPaidBetween(
                    LocalDateTime.of(2000, 1, 1, 0, 0), LocalDateTime.now()).stream()
                    .map(RepairOrder::getTotalCost)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            document.add(bodyLine("Tổng số phiếu sửa chữa: " + totalOrders));
            document.add(bodyLine("Phiếu đang xử lý: " + activeOrders));
            document.add(bodyLine("Tổng doanh thu (đã thanh toán): " + currencyUtils.format(totalRevenue) + " VNĐ"));

            // 2. Thống kê theo trạng thái
            heading(document, "2. THỐNG KÊ THEO TRẠNG THÁI PHIẾU");

            PdfPTable statusTable = new PdfPTable(2);
            statusTable.setWidthPercentage(60);
            statusTable.setHorizontalAlignment(Element.ALIGN_LEFT);
            statusTable.setWidths(new float[]{3f, 1f});
            statusTable.setSpacingBefore(4);
            statusTable.setSpacingAfter(8);

            statusTable.addCell(headerCell("Trạng thái", Element.ALIGN_LEFT));
            statusTable.addCell(headerCell("Số lượng", Element.ALIGN_CENTER));
            addStatusRow(statusTable, "Đã tiếp nhận", repairService.countByStatus(RepairStatus.RECEIVED));
            addStatusRow(statusTable, "Đang sửa chữa", repairService.countByStatus(RepairStatus.IN_PROGRESS));
            addStatusRow(statusTable, "Chờ linh kiện", repairService.countByStatus(RepairStatus.WAITING_PARTS));
            addStatusRow(statusTable, "Hoàn thành - chờ thanh toán", repairService.countByStatus(RepairStatus.COMPLETED));
            addStatusRow(statusTable, "Đã thanh toán", repairService.countByStatus(RepairStatus.PAID));
            addStatusRow(statusTable, "Đã hủy", repairService.countByStatus(RepairStatus.CANCELLED));
            document.add(statusTable);

            // 3. Doanh thu 12 tháng gần nhất
            heading(document, "3. DOANH THU THEO THÁNG (12 THÁNG GẦN NHẤT)");

            PdfPTable revenueTable = new PdfPTable(3);
            revenueTable.setWidthPercentage(70);
            revenueTable.setHorizontalAlignment(Element.ALIGN_LEFT);
            revenueTable.setWidths(new float[]{1.2f, 1f, 1.6f});
            revenueTable.setSpacingBefore(4);
            revenueTable.setSpacingAfter(8);

            revenueTable.addCell(headerCell("#", Element.ALIGN_CENTER));
            revenueTable.addCell(headerCell("Tháng", Element.ALIGN_CENTER));
            revenueTable.addCell(headerCell("Doanh thu (VNĐ)", Element.ALIGN_RIGHT));

            DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MM/yyyy");
            int rowIdx = 1;
            for (int i = 11; i >= 0; i--) {
                YearMonth ym = YearMonth.now().minusMonths(i);
                LocalDateTime from = ym.atDay(1).atStartOfDay();
                LocalDateTime to = ym.atEndOfMonth().atTime(23, 59, 59);
                BigDecimal revenue = repairService.findPaidBetween(from, to).stream()
                        .map(RepairOrder::getTotalCost)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                revenueTable.addCell(bodyCell(String.valueOf(rowIdx++), Element.ALIGN_CENTER));
                revenueTable.addCell(bodyCell(ym.format(monthFmt), Element.ALIGN_CENTER));
                revenueTable.addCell(bodyCell(currencyUtils.format(revenue), Element.ALIGN_RIGHT));
            }
            document.add(revenueTable);

            // 4. Bảo hành sắp hết hạn
            heading(document, "4. THIẾT BỊ BẢO HÀNH SẮP HẾT HẠN (30 NGÀY TỚI)");

            List<Device> warranties = deviceService.expiringWarranties(today, today.plusDays(30));
            PdfPTable warrantyTable = new PdfPTable(5);
            warrantyTable.setWidthPercentage(100);
            warrantyTable.setWidths(new float[]{1.2f, 1.6f, 1.4f, 1.2f, 1.2f});
            warrantyTable.setSpacingBefore(4);
            warrantyTable.setSpacingAfter(10);

            warrantyTable.addCell(headerCell("Serial", Element.ALIGN_LEFT));
            warrantyTable.addCell(headerCell("Thiết bị", Element.ALIGN_LEFT));
            warrantyTable.addCell(headerCell("Khách hàng", Element.ALIGN_LEFT));
            warrantyTable.addCell(headerCell("SĐT", Element.ALIGN_LEFT));
            warrantyTable.addCell(headerCell("Ngày hết hạn", Element.ALIGN_CENTER));

            if (warranties.isEmpty()) {
                PdfPCell empty = new PdfPCell(new Phrase("Không có thiết bị nào hết hạn bảo hành trong 30 ngày tới.", small));
                empty.setColspan(5);
                empty.setPadding(6);
                warrantyTable.addCell(empty);
            } else {
                for (Device d : warranties) {
                    warrantyTable.addCell(bodyCell(d.getSerialNumber(), Element.ALIGN_LEFT));
                    warrantyTable.addCell(bodyCell(d.getBrand() + " " + d.getModel(), Element.ALIGN_LEFT));
                    warrantyTable.addCell(bodyCell(d.getCustomer().getFullName(), Element.ALIGN_LEFT));
                    warrantyTable.addCell(bodyCell(d.getCustomer().getPhone(), Element.ALIGN_LEFT));
                    warrantyTable.addCell(bodyCell(d.getWarrantyEndDate().format(dateFmt), Element.ALIGN_CENTER));
                }
            }
            document.add(warrantyTable);

            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            // Chữ ký
            PdfPTable signature = new PdfPTable(2);
            signature.setWidthPercentage(100);
            PdfPCell s1 = new PdfPCell(new Phrase("Người lập báo cáo\n(Ký, ghi rõ họ tên)", small));
            s1.setBorder(Rectangle.NO_BORDER);
            s1.setPaddingTop(30);
            s1.setPadding(4);
            s1.setHorizontalAlignment(Element.ALIGN_CENTER);
            signature.addCell(s1);
            signature.addCell(new PdfPCell(new Phrase("", small)));
            document.add(signature);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo file PDF báo cáo: " + e.getMessage(), e);
        }
        return out.toByteArray();
    }

    private void heading(Document document, String text) throws Exception {
        Paragraph p = new Paragraph(text, headingFont);
        p.setSpacingBefore(12);
        p.setSpacingAfter(6);
        document.add(p);
    }

    private Paragraph bodyLine(String text) {
        Paragraph p = new Paragraph(text, normal);
        p.setSpacingBefore(3);
        return p;
    }

    private void addStatusRow(PdfPTable table, String label, long count) {
        table.addCell(bodyCell(label, Element.ALIGN_LEFT));
        table.addCell(bodyCell(String.valueOf(count), Element.ALIGN_CENTER));
    }

    private PdfPCell headerCell(String text, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, smallBold));
        c.setBackgroundColor(new java.awt.Color(230, 230, 230));
        c.setPaddingTop(5);
        c.setPaddingBottom(5);
        c.setPaddingLeft(4);
        c.setPaddingRight(4);
        c.setHorizontalAlignment(align);
        return c;
    }

    private PdfPCell bodyCell(String text, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, small));
        c.setPaddingTop(4);
        c.setPaddingBottom(4);
        c.setPaddingLeft(4);
        c.setPaddingRight(4);
        c.setHorizontalAlignment(align);
        return c;
    }
}