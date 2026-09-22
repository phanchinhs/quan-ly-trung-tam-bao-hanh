package com.example.btl.service;

import com.example.btl.entity.RepairItem;
import com.example.btl.entity.RepairOrder;
import com.example.btl.entity.RepairStatus;
import com.example.btl.entity.SparePart;
import com.example.btl.entity.UserAccount;
import com.example.btl.config.Auditable;
import com.example.btl.repository.RepairItemRepository;
import com.example.btl.repository.RepairOrderRepository;
import com.example.btl.repository.SparePartRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RepairService {

    private final RepairOrderRepository repairOrderRepository;
    private final RepairItemRepository repairItemRepository;
    private final SparePartRepository sparePartRepository;
    private final EmailService emailService;

    public RepairService(RepairOrderRepository repairOrderRepository,
                         RepairItemRepository repairItemRepository,
                         SparePartRepository sparePartRepository,
                         EmailService emailService) {
        this.repairOrderRepository = repairOrderRepository;
        this.repairItemRepository = repairItemRepository;
        this.sparePartRepository = sparePartRepository;
        this.emailService = emailService;
    }

    public List<RepairOrder> findAll() {
        return repairOrderRepository.findAllByOrderByReceivedAtDesc();
    }

    public Page<RepairOrder> findAll(Pageable pageable) {
        return repairOrderRepository.findAll(pageable);
    }

    public List<RepairOrder> findByStatus(RepairStatus status) {
        return repairOrderRepository.findByStatus(status);
    }

    public Page<RepairOrder> findByStatus(RepairStatus status, Pageable pageable) {
        return repairOrderRepository.findByStatus(status, pageable);
    }

    public List<RepairOrder> recent10() {
        return repairOrderRepository.findTop10ByOrderByReceivedAtDesc();
    }

    public Optional<RepairOrder> findById(Long id) {
        return repairOrderRepository.findById(id);
    }

    public long countAll() {
        return repairOrderRepository.count();
    }

    public long countByStatus(RepairStatus status) {
        return repairOrderRepository.countByStatus(status);
    }

    public long countActive() {
        return repairOrderRepository.countByStatusNot(RepairStatus.PAID) -
                repairOrderRepository.countByStatus(RepairStatus.CANCELLED);
    }

    public List<RepairOrder> findPaidBetween(LocalDateTime from, LocalDateTime to) {
        return repairOrderRepository.findByPaidAtBetween(from, to);
    }

    public List<TimelineStep> statusTimeline(RepairOrder order) {
        List<TimelineStep> steps = new ArrayList<>();
        RepairStatus current = order.getStatus();
        int indexOfCurrent = -1;
        RepairStatus[] ordered = {
                RepairStatus.RECEIVED,
                RepairStatus.IN_PROGRESS,
                RepairStatus.WAITING_PARTS,
                RepairStatus.COMPLETED,
                RepairStatus.PAID
        };
        for (int i = 0; i < ordered.length; i++) {
            if (ordered[i] == current) {
                indexOfCurrent = i;
                break;
            }
        }
        if (current == RepairStatus.CANCELLED) {
            indexOfCurrent = -1;
        }
        for (int i = 0; i < ordered.length; i++) {
            steps.add(new TimelineStep(ordered[i], i <= indexOfCurrent, i == indexOfCurrent));
        }
        return steps;
    }

    public record TimelineStep(RepairStatus status, boolean done, boolean current) {
        public String getLabel() {
            return status.getLabel();
        }
    }

    @Transactional
    @Auditable(action = "CREATE", entity = "Phiếu sửa chữa", description = "Tạo mới phiếu sửa chữa {code}")
    public RepairOrder create(RepairOrder order) {
        order.setCode(generateCode());
        order.setStatus(RepairStatus.RECEIVED);
        order.setReceivedAt(LocalDateTime.now());
        if (order.getLaborCost() == null) {
            order.setLaborCost(BigDecimal.ZERO);
        }
        return repairOrderRepository.save(order);
    }

    @Transactional
    @Auditable(action = "UPDATE", entity = "Phiếu sửa chữa", description = "Cập nhật thông tin phiếu #{id}")
    public RepairOrder updateDetails(RepairOrder order) {
        RepairOrder existing = repairOrderRepository.findById(order.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu sửa chữa"));
        existing.setIssueDescription(order.getIssueDescription());
        existing.setDiagnosis(order.getDiagnosis());
        existing.setProductCondition(order.getProductCondition());
        existing.setAccessories(order.getAccessories());
        existing.setNote(order.getNote());
        existing.setExpectedFinishDate(order.getExpectedFinishDate());
        existing.setLaborCost(order.getLaborCost() == null ? BigDecimal.ZERO : order.getLaborCost());
        existing.setTechnician(order.getTechnician());
        return repairOrderRepository.save(existing);
    }

    @Transactional
    @Auditable(action = "UPDATE", entity = "Phiếu sửa chữa", description = "Chuyển trạng thái phiếu #{id}")
    public RepairOrder changeStatus(Long id, RepairStatus newStatus) {
        RepairOrder order = repairOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu sửa chữa"));
        if (order.getStatus() == RepairStatus.CANCELLED) {
            throw new IllegalStateException("Phiếu đã hủy không thể thay đổi trạng thái");
        }
        order.setStatus(newStatus);
        if (newStatus == RepairStatus.COMPLETED) {
            order.setCompletedAt(LocalDateTime.now());
        }
        if (newStatus == RepairStatus.PAID) {
            order.setCompletedAt(order.getCompletedAt() != null ? order.getCompletedAt() : LocalDateTime.now());
            order.setPaidAt(LocalDateTime.now());
        }
        RepairOrder saved = repairOrderRepository.save(order);
        emailService.sendStatusUpdate(saved);
        return saved;
    }

    @Transactional
    @Auditable(action = "CREATE", entity = "Linh kiện phiếu", description = "Thêm linh kiện vào phiếu #{id}")
    public RepairItem addItem(Long orderId, Long sparePartId, String name, int quantity, BigDecimal unitPrice) {
        RepairOrder order = repairOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu sửa chữa"));
        if (quantity <= 0) {
            quantity = 1;
        }
        SparePart part = null;
        if (sparePartId != null) {
            part = sparePartRepository.findById(sparePartId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy linh kiện trong kho"));
            if (part.getQuantityInStock() < quantity) {
                throw new IllegalStateException("Tồn kho không đủ: " + part.getName()
                        + " chỉ còn " + part.getQuantityInStock());
            }
            part.setQuantityInStock(part.getQuantityInStock() - quantity);
            part.setUpdatedAt(LocalDateTime.now());
            sparePartRepository.save(part);
            name = part.getName();
            unitPrice = part.getUnitPrice();
        }
        RepairItem item = new RepairItem();
        item.setRepairOrder(order);
        item.setSparePart(part);
        item.setName(name);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice == null ? BigDecimal.ZERO : unitPrice);
        order.getItems().add(item);
        return repairItemRepository.save(item);
    }

    @Transactional
    @Auditable(action = "DELETE", entity = "Linh kiện phiếu", description = "Xóa linh kiện #{id}")
    public void removeItem(Long orderId, Long itemId) {
        RepairOrder order = repairOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu sửa chữa"));
        RepairItem item = repairItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy linh kiện"));
        if (!item.getRepairOrder().getId().equals(orderId)) {
            throw new IllegalArgumentException("Linh kiện không thuộc phiếu này");
        }
        restoreStock(item);
        order.getItems().remove(item);
        repairItemRepository.delete(item);
    }

    @Transactional
    @Auditable(action = "DELETE", entity = "Phiếu sửa chữa", description = "Xóa phiếu sửa chữa #{id}")
    public void delete(Long id) {
        RepairOrder order = repairOrderRepository.findById(id).orElse(null);
        if (order != null) {
            for (RepairItem item : new ArrayList<>(order.getItems())) {
                restoreStock(item);
            }
        }
        repairOrderRepository.deleteById(id);
    }

    private void restoreStock(RepairItem item) {
        SparePart part = item.getSparePart();
        if (part != null) {
            part.setQuantityInStock(part.getQuantityInStock() + item.getQuantity());
            part.setUpdatedAt(LocalDateTime.now());
            sparePartRepository.save(part);
        }
    }

    private String generateCode() {
        return String.format("SR-%d-%04d", Year.now().getValue(), repairOrderRepository.count() + 1);
    }
}