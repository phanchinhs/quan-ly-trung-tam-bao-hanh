package com.example.btl.service;

import com.example.btl.entity.SparePart;
import com.example.btl.config.Auditable;
import com.example.btl.repository.SparePartRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SparePartService {

    private final SparePartRepository sparePartRepository;

    public SparePartService(SparePartRepository sparePartRepository) {
        this.sparePartRepository = sparePartRepository;
    }

    public List<SparePart> findAll() {
        return sparePartRepository.findAll();
    }

    public List<SparePart> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return sparePartRepository.findAll();
        }
        String k = keyword.trim();
        return sparePartRepository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(k, k);
    }

    public Page<SparePart> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return sparePartRepository.findAll(pageable);
        }
        String k = keyword.trim();
        return sparePartRepository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(k, k, pageable);
    }

    public Optional<SparePart> findById(Long id) {
        return sparePartRepository.findById(id);
    }

    public boolean existsByCode(String code) {
        return sparePartRepository.existsByCode(code);
    }

    public boolean existsByCodeExcept(String code, Long id) {
        return sparePartRepository.existsByCodeAndIdNot(code, id);
    }

    public List<SparePart> lowStockParts() {
        return sparePartRepository.findLowStock();
    }

    public long countLowStock() {
        return sparePartRepository.countLowStock();
    }

    @Transactional
    @Auditable(action = "SAVE", entity = "Linh kiện", description = "Thêm/sửa linh kiện {name} (mã {code})")
    public SparePart save(SparePart part) {
        part.setUpdatedAt(LocalDateTime.now());
        return sparePartRepository.save(part);
    }

    @Transactional
    @Auditable(action = "UPDATE", entity = "Linh kiện", description = "Điều chỉnh tồn kho #{id}")
    public void adjustStock(Long id, int delta) {
        SparePart part = sparePartRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy linh kiện"));
        int newQty = part.getQuantityInStock() + delta;
        if (newQty < 0) {
            throw new IllegalStateException("Tồn kho không đủ. Hiện có: " + part.getQuantityInStock());
        }
        part.setQuantityInStock(newQty);
        part.setUpdatedAt(LocalDateTime.now());
        sparePartRepository.save(part);
    }

    @Transactional
    @Auditable(action = "DELETE", entity = "Linh kiện", description = "Xóa linh kiện #{id}")
    public void delete(Long id) {
        sparePartRepository.deleteById(id);
    }

    public long count() {
        return sparePartRepository.count();
    }
}