package com.example.btl.service;

import com.example.btl.entity.Device;
import com.example.btl.config.Auditable;
import com.example.btl.repository.DeviceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;

    public DeviceService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    public List<Device> findAll() {
        return deviceRepository.findAll();
    }

    public Page<Device> findAll(Pageable pageable) {
        return deviceRepository.findAll(pageable);
    }

    public Optional<Device> findById(Long id) {
        return deviceRepository.findById(id);
    }

    public List<Device> findByCustomer(Long customerId) {
        return deviceRepository.findByCustomerId(customerId);
    }

    public boolean existsBySerialNumber(String serialNumber) {
        return deviceRepository.existsBySerialNumber(serialNumber);
    }

    public boolean existsBySerialNumberExcept(String serialNumber, Long id) {
        return deviceRepository.existsBySerialNumberAndIdNot(serialNumber, id);
    }

    public List<Device> expiringWarranties(LocalDate from, LocalDate to) {
        return deviceRepository.findByWarrantyEndDateBetween(from, to);
    }

    public long count() {
        return deviceRepository.count();
    }

    @Transactional
    @Auditable(action = "SAVE", entity = "Thiết bị", description = "Thêm/sửa thiết bị {name} (serial {serialNumber})")
    public Device save(Device device) {
        return deviceRepository.save(device);
    }

    @Transactional
    @Auditable(action = "DELETE", entity = "Thiết bị", description = "Xóa thiết bị #{id}")
    public void delete(Long id) {
        deviceRepository.deleteById(id);
    }
}