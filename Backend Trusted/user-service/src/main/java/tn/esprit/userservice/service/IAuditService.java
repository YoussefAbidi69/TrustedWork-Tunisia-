package tn.esprit.userservice.service;

import tn.esprit.userservice.entity.AuditEventType;

public interface IAuditService {

    void log(AuditEventType eventType,
             String performedBy,
             String targetUser,
             String details,
             String ipAddress);
}