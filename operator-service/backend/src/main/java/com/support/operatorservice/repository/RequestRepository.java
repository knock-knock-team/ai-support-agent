package com.support.operatorservice.repository;

import com.support.operatorservice.entity.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findByStatus(Request.Status status);
    List<Request> findByStatusInOrderByCreatedAtDesc(List<Request.Status> statuses);
    List<Request> findByOperatorId(Long operatorId);
    List<Request> findByStatusOrderByCreatedAtDesc(Request.Status status);
    
    @Query("SELECT COUNT(r) FROM Request r WHERE r.status = :status")
    Long countByStatus(Request.Status status);
    
    @Query("SELECT COUNT(r) FROM Request r WHERE r.category = :category")
    Long countByCategory(Request.Category category);
    
    @Query("SELECT r FROM Request r WHERE r.createdAt >= :startDate ORDER BY r.createdAt DESC")
    List<Request> findRecentRequests(LocalDateTime startDate);
}
