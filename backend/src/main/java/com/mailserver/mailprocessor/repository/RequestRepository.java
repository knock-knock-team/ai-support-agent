package com.mailserver.mailprocessor.repository;

import com.mailserver.mailprocessor.model.entity.Request;
import com.mailserver.mailprocessor.model.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RequestRepository extends JpaRepository<Request, UUID> {
    
    List<Request> findByStatus(RequestStatus status);
    
    List<Request> findByEmail(String email);
}
