package com.example.btl.repository;

import com.example.btl.entity.RepairItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepairItemRepository extends JpaRepository<RepairItem, Long> {

    void deleteByRepairOrderId(Long repairOrderId);
}