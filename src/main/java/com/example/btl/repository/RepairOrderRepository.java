package com.example.btl.repository;

import com.example.btl.entity.RepairOrder;
import com.example.btl.entity.RepairStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RepairOrderRepository extends JpaRepository<RepairOrder, Long> {

    List<RepairOrder> findAllByOrderByReceivedAtDesc();

    List<RepairOrder> findTop10ByOrderByReceivedAtDesc();

    List<RepairOrder> findByStatus(RepairStatus status);

    Page<RepairOrder> findByStatus(RepairStatus status, Pageable pageable);

    long countByStatus(RepairStatus status);

    long countByStatusNot(RepairStatus status);

    List<RepairOrder> findByPaidAtBetween(LocalDateTime from, LocalDateTime to);

    Optional<RepairOrder> findByCode(String code);
}