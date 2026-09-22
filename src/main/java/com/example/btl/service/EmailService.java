package com.example.btl.service;

import com.example.btl.entity.Customer;
import com.example.btl.entity.RepairOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final boolean enabled;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.email.from:no-reply@localhost}") String from,
                        @Value("${app.email.enabled:false}") boolean enabled) {
        this.mailSender = mailSender;
        this.from = from;
        this.enabled = enabled;
    }

    public void sendStatusUpdate(RepairOrder order) {
        if (!enabled) {
            log.info("[email-disabled] ({} -> {}) {}", order.getCode(), order.getStatus().getLabel(),
                    emailText(order));
            return;
        }
        Customer customer = order.getCustomer();
        if (customer == null || customer.getEmail() == null || customer.getEmail().isBlank()) {
            log.warn("Đơn {} không có email khách hàng, bỏ qua gửi email.", order.getCode());
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(customer.getEmail());
            msg.setSubject("[Trung tâm bảo hành] " + order.getCode() + " - " + order.getStatus().getLabel());
            msg.setText(emailText(order));
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Gửi email thất bại cho {}: {}", order.getCode(), e.getMessage());
        }
    }

    private String emailText(RepairOrder order) {
        StringBuilder sb = new StringBuilder();
        sb.append("Kính gửi ");
        sb.append(order.getCustomer() != null ? order.getCustomer().getFullName() : "Quý khách");
        sb.append(",\n\n");
        sb.append("Phiếu sửa chữa của bạn vừa được cập nhật:\n\n");
        sb.append("Mã phiếu: ").append(order.getCode()).append("\n");
        sb.append("Thiết bị: ").append(order.getDevice() != null
                ? order.getDevice().getBrand() + " " + order.getDevice().getModel() : "—").append("\n");
        sb.append("Trạng thái hiện tại: ").append(order.getStatus().getLabel()).append("\n");
        sb.append("Ngày tiếp nhận: ")
                .append(order.getReceivedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .append("\n");
        sb.append("\nBạn có thể tra cứu trạng thái tại: http://localhost:8080/track?code=")
                .append(order.getCode()).append("\n\n");
        sb.append("Trân trọng,\nTrung tâm bảo hành & sửa chữa thiết bị điện tử");
        return sb.toString();
    }
}