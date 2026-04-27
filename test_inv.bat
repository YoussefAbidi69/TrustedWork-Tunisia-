@echo off
set "PAYLOAD={\"senderId\": 1, \"receiverId\": 2, \"proposedRole\": \"MEMBER\"}"
curl -v -X POST http://localhost:8081/api/agency-invitations/agency/14 -H "Content-Type: application/json" -d "%PAYLOAD%"
