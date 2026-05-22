package com.trading.service;

import com.trading.entity.OperationLog;
import com.trading.repository.OperationLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OperationLogService {

    private static final Logger log = LoggerFactory.getLogger(OperationLogService.class);

    private final OperationLogRepository operationLogRepository;

    @Async
    public void saveAsync(Long userId, String username, String module, String action,
                          String resourceId, String detail, String ip) {
        try {
            OperationLog entry = OperationLog.builder()
                    .userId(userId)
                    .username(username)
                    .module(module)
                    .action(action)
                    .resourceId(resourceId)
                    .detail(detail)
                    .ip(ip)
                    .build();
            operationLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("操作日志写入失败: {}", e.getMessage());
        }
    }

    public Page<OperationLog> getLogs(String module, String username,
                                      LocalDateTime startTime, LocalDateTime endTime,
                                      int page, int size) {
        Specification<OperationLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (module != null && !module.isBlank()) {
                predicates.add(cb.equal(root.get("module"), module));
            }
            if (username != null && !username.isBlank()) {
                predicates.add(cb.like(root.get("username"), "%" + username + "%"));
            }
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endTime));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return operationLogRepository.findAll(spec, pageable);
    }
}
