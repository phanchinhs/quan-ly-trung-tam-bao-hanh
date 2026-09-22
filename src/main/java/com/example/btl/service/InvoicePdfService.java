package com.example.btl.service;

import com.example.btl.entity.RepairItem;
import com.example.btl.entity.RepairOrder;
import com.example.btl.util.CurrencyUtils;
import com.example.btl.util.CurrencyToWords;
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
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class InvoicePdfService {

    private static final String FONT_PATH = "fonts/arial.ttf";
    private static final String FONT_BOLD_PATH = "fonts/arialbd.ttf";

    private final CurrencyUtils currencyUtils;

    private final Font titleFont;
    private final Font normal;
    private final Font normalBold;
    private final Font small;
    private final Font smallBold;

    public InvoicePdfService(CurrencyUtils currencyUtils) {
        this.currencyUtils = currencyUtils;
        this.titleFont = createFont(FONT_BOLD_PATH, 18, Font.BOLD);
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

    public byte[] generateInvoice(RepairOrder order) {
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Tiêu đề
            Paragraph centerTitle = new Paragraph(
                    "TRUNG TÂM BẢO HÀNH & SỬA CHỮA THIẾT BỊ ĐIỆN TỬ", titleFont);
            centerTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(centerTitle);

            document.add(new Paragraph(" "));

            Paragraph invoiceTitle = new Paragraph("HÓA ĐƠN SỬA CHỮA",
                    createFont(FONT_BOLD_PATH, 14, Font.BOLD));
            invoiceTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(invoiceTitle);

            Paragraph codeLine = new Paragraph("Số phiếu: " + order.getCode(), normalBold);
            codeLine.setAlignment(Element.ALIGN_CENTER);
            document.add(codeLine);

            Paragraph dateLine = new Paragraph(
                    "Ngày xuất: " + order.getReceivedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    small);
            dateLine.setAlignment(Element.ALIGN_RIGHT);
            document.add(dateLine);

            document.add(new Paragraph(" "));

            // Thông tin khách hàng & thiết bị
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingBefore(6);
            infoTable.setSpacingAfter(6);

            infoTable.addCell(fieldCell("Khách hàng: " + nvl(order.getCustomer().getFullName())));
            infoTable.addCell(fieldCell("Số điện thoại: " + nvl(order.getCustomer().getPhone())));
            infoTable.addCell(fieldCell("Thiết bị: " + nvl(order.getDevice().getBrand() + " " + order.getDevice().getModel())));
            infoTable.addCell(fieldCell("Serial: " + nvl(order.getDevice().getSerialNumber())));
            infoTable.addCell(fieldCell("Kỹ thuật viên: " + (order.getTechnician() != null ? nvl(order.getTechnician().getFullName()) : "—")));
            infoTable.addCell(fieldCell("Sự cố: " + nvl(order.getIssueDescription())));
            document.add(infoTable);

            document.add(new Paragraph(" "));

            // Bảng chi tiết linh kiện / dịch vụ
            PdfPTable itemTable = new PdfPTable(5);
            itemTable.setWidthPercentage(100);
            itemTable.setWidths(new float[]{0.6f, 4.4f, 1.2f, 2.0f, 2.0f});
            itemTable.setSpacingBefore(6);

            itemTable.addCell(headerCell("#", Element.ALIGN_CENTER));
            itemTable.addCell(headerCell("Tên linh kiện / dịch vụ", Element.ALIGN_LEFT));
            itemTable.addCell(headerCell("SL", Element.ALIGN_CENTER));
            itemTable.addCell(headerCell("Đơn giá (VNĐ)", Element.ALIGN_RIGHT));
            itemTable.addCell(headerCell("Thành tiền (VNĐ)", Element.ALIGN_RIGHT));

            List<RepairItem> items = order.getItems();
            if (items == null || items.isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("Không có linh kiện nào.", small));
                emptyCell.setColspan(5);
                emptyCell.setPadding(6);
                itemTable.addCell(emptyCell);
            } else {
                int idx = 1;
                for (RepairItem item : items) {
                    itemTable.addCell(bodyCell(String.valueOf(idx++), Element.ALIGN_CENTER));
                    itemTable.addCell(bodyCell(nvl(item.getName()), Element.ALIGN_LEFT));
                    itemTable.addCell(bodyCell(String.valueOf(item.getQuantity()), Element.ALIGN_CENTER));
                    itemTable.addCell(bodyCell(currencyUtils.format(item.getUnitPrice()), Element.ALIGN_RIGHT));
                    itemTable.addCell(bodyCell(currencyUtils.format(item.getLineTotal()), Element.ALIGN_RIGHT));
                }
            }
            document.add(itemTable);

            document.add(new Paragraph(" "));

            // Tổng cộng
            PdfPTable totalTable = new PdfPTable(2);
            totalTable.setWidthPercentage(60);
            totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            totalTable.addCell(bodyCell("Chi phí linh kiện:", Element.ALIGN_LEFT));
            totalTable.addCell(bodyCell(currencyUtils.format(order.getPartsCost()), Element.ALIGN_RIGHT));
            totalTable.addCell(bodyCell("Tiền công sửa chữa:", Element.ALIGN_LEFT));
            totalTable.addCell(bodyCell(currencyUtils.format(order.getLaborCost()), Element.ALIGN_RIGHT));
            totalTable.addCell(bodyCell("TỔNG CỘNG:", Element.ALIGN_LEFT, normalBold));
            totalTable.addCell(bodyCell(currencyUtils.format(order.getTotalCost()), Element.ALIGN_RIGHT, normalBold));
            document.add(totalTable);

            // Bằng chữ
            document.add(new Paragraph(
                    "Bằng chữ: " + CurrencyToWords.toWords(order.getTotalCost()), small));

            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            // Chữ ký
            PdfPTable signature = new PdfPTable(2);
            signature.setWidthPercentage(100);
            PdfPCell s1 = cell("Kỹ thuật viên", small);
            PdfPCell s2 = cell("Khách hàng", small);
            signature.addCell(s1);
            signature.addCell(s2);
            document.add(signature);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo file PDF hóa đơn: " + e.getMessage(), e);
        }
        return out.toByteArray();
    }

    private String nvl(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }

    private PdfPCell fieldCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, small));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(3);
        return c;
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
        return bodyCell(text, align, small);
    }

    private PdfPCell bodyCell(String text, int align, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setPaddingTop(4);
        c.setPaddingBottom(4);
        c.setPaddingLeft(4);
        c.setPaddingRight(4);
        c.setHorizontalAlignment(align);
        return c;
    }

    private PdfPCell cell(String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPaddingTop(24);
        c.setPadding(4);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        return c;
    }
}