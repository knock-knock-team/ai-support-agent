package com.support.operatorservice.repository;

import com.support.operatorservice.entity.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
    interface CategoryStatusCountProjection {
        Request.Category getCategory();
        Request.Status getStatus();
        Long getTotal();
    }

    interface DailyCountProjection {
        LocalDate getDay();
        Long getTotal();
    }

    List<Request> findByStatus(Request.Status status);
    List<Request> findByStatusInOrderByCreatedAtDesc(List<Request.Status> statuses);
    List<Request> findByOperatorId(Long operatorId);
    List<Request> findByStatusOrderByCreatedAtDesc(Request.Status status);
    
    @Query("SELECT COUNT(r) FROM Request r WHERE r.status = :status")
    Long countByStatus(Request.Status status);
    
    @Query("SELECT COUNT(r) FROM Request r WHERE r.category = :category")
    Long countByCategory(Request.Category category);

    @Query("SELECT COUNT(r) FROM Request r")
    Long countAllRequests();

    @Query("SELECT COUNT(r) FROM Request r WHERE r.status IN :statuses")
    Long countByStatusIn(@Param("statuses") List<Request.Status> statuses);

    @Query(value = """
            SELECT COUNT(*)
            FROM requests
            WHERE status = 'CLOSED'
              AND (
                    operator_answer IS NULL
                    OR BTRIM(operator_answer) = ''
                    OR operator_answer = ai_generated_answer
                  )
            """, nativeQuery = true)
    Long countApprovedClosedRequests();

    @Query(value = """
            SELECT COUNT(*)
            FROM requests
            WHERE status = 'CLOSED'
              AND operator_answer IS NOT NULL
              AND BTRIM(operator_answer) <> ''
              AND operator_answer <> ai_generated_answer
            """, nativeQuery = true)
    Long countEditedClosedRequests();

    @Query("""
            SELECT r.category as category, r.status as status, COUNT(r) as total
            FROM Request r
            GROUP BY r.category, r.status
            """)
    List<CategoryStatusCountProjection> countByCategoryAndStatus();

    @Query(value = """
            SELECT DATE(created_at) as day, COUNT(*) as total
            FROM requests
            WHERE created_at >= :startDate
            GROUP BY DATE(created_at)
            ORDER BY day
            """, nativeQuery = true)
    List<DailyCountProjection> countDailyFrom(@Param("startDate") OffsetDateTime startDate);
    
    @Query("SELECT r FROM Request r WHERE r.createdAt >= :startDate ORDER BY r.createdAt DESC")
    List<Request> findRecentRequests(OffsetDateTime startDate);
}
