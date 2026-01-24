# API CONTRACT
## SWP391 – Jira & GitHub Integration Backend

---

## 1. Purpose

This document defines the **API contract** between **Backend** and **Frontend** for the SWP391 project.

API contract helps:
- Frontend developers know **which APIs exist**
- Backend developers know **what data to return**
- Both sides work **independently without blocking**

---

## 2. Base URL

```
http://localhost:8080/api
```

When using Docker:
```
http://backend:8080/api
```

---

## 3. Common Response Format

All APIs return JSON in the following format:

```json
{
  "success": true,
  "message": "Optional message",
  "data": {}
}
```

---

## 4. Health Check API

### 4.1 Check Backend Status

| Item | Value |
|---|---|
| URL | `/health` |
| Method | GET |
| Description | Check if backend server is running |

#### Response Example

```json
{
  "success": true,
  "message": "Backend is running",
  "data": null
}
```

---

## 5. Group Management APIs (Mock)

### 5.1 Get All Student Groups

| Item | Value |
|---|---|
| URL | `/groups` |
| Method | GET |
| Role | Admin, Lecturer |
| Description | Get list of student groups |

#### Response Example

```json
{
  "success": true,
  "data": [
    {
      "groupId": 1,
      "groupName": "Group 1",
      "leader": "Nguyen Van A",
      "memberCount": 5
    }
  ]
}
```

---

## 6. Task & Progress APIs (Mock)

### 6.1 Project Progress Summary

| Item | Value |
|---|---|
| URL | `/reports/summary` |
| Method | GET |
| Role | Lecturer, Team Leader |
| Description | Get overall project progress |

#### Response Example

```json
{
  "success": true,
  "data": {
    "totalTasks": 40,
    "completedTasks": 30,
    "completionRate": 75
  }
}
```

---

## 7. GitHub Statistics APIs (Mock)

### 7.1 Commit Statistics by User

| Item | Value |
|---|---|
| URL | `/github/commits/stats` |
| Method | GET |
| Role | Lecturer, Team Leader |
| Description | View GitHub commit statistics |

#### Response Example

```json
{
  "success": true,
  "data": [
    {
      "username": "student01",
      "commitCount": 25
    }
  ]
}
```

---

## 8. Notes

- All APIs currently return **mock data**
- Jira & GitHub integration will be implemented later
- Authentication will be added in future phases

---

## 9. Versioning

| Version | Date | Description |
|---|---|---|
| 1.0 | 2026-01-17 | Initial API contract for frontend development |

