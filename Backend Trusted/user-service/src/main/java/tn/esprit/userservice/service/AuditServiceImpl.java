package tn.esprit.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.userservice.entity.AuditEventType;
import tn.esprit.userservice.entity.AuditLog;
import tn.esprit.userservice.repository.AuditLogRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements IAuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    public void log(AuditEventType eventType,
                    String performedBy,
                    String targetUser,
                    String details,
                    String ipAddress) {

        AuditLog entry = new AuditLog();
        entry.setEventType(eventType);
        entry.setPerformedBy(performedBy);
        entry.setTargetUser(targetUser);
        entry.setDetails(details);
        entry.setIpAddress(ipAddress);

        auditLogRepository.save(entry);

        log.info("AUDIT [{}] performedBy={} target={} ip={}",
                eventType, performedBy, targetUser, ipAddress);
    }
}