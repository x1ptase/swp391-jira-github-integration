-- ============================
-- CREATE DATABASE
-- ============================
IF DB_ID(N'SWP391_JIRA_GITHUB_TOOL_SUPPORT') IS NULL
BEGIN
    CREATE DATABASE SWP391_JIRA_GITHUB_TOOL_SUPPORT;
END
GO
USE SWP391_JIRA_GITHUB_TOOL_SUPPORT;
GO

-- ===== Lookup tables =====
CREATE TABLE Role (
    role_id INT IDENTITY(1,1) PRIMARY KEY,
    role_code NVARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE MemberRole (
    member_role_id INT IDENTITY(1,1) PRIMARY KEY,
    code NVARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE TaskStatus (
    status_id INT IDENTITY(1,1) PRIMARY KEY,
    code NVARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE RequirementStatus (
    status_id INT IDENTITY(1,1) PRIMARY KEY,
    code NVARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE Priority (
    priority_id INT IDENTITY(1,1) PRIMARY KEY,
    code NVARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE IntegrationType (
    integration_type_id INT IDENTITY(1,1) PRIMARY KEY,
    code NVARCHAR(30) NOT NULL UNIQUE
);

-- ===== Seed =====
INSERT INTO Role(role_code) VALUES (N'ADMIN'), (N'LECTURER'), (N'STUDENT');

SET IDENTITY_INSERT MemberRole ON;
INSERT INTO MemberRole(member_role_id, code) VALUES (1, N'LEADER'), (2, N'MEMBER');
SET IDENTITY_INSERT MemberRole OFF;

INSERT INTO TaskStatus(code) VALUES (N'TODO'), (N'IN_PROGRESS'), (N'DONE');
INSERT INTO RequirementStatus(code) VALUES (N'ACTIVE'), (N'DONE');
INSERT INTO Priority(code) VALUES (N'LOW'), (N'MEDIUM'), (N'HIGH');
INSERT INTO IntegrationType(code) VALUES (N'JIRA'), (N'GITHUB');

-- ===== Core tables =====
CREATE TABLE Users (
    user_id INT IDENTITY(1,1) PRIMARY KEY,
    username NVARCHAR(50) NOT NULL UNIQUE,
    full_name NVARCHAR(100) NOT NULL,
    email NVARCHAR(120) NOT NULL UNIQUE,
    github_username NVARCHAR(100) NULL UNIQUE,
    jira_email NVARCHAR(120) NULL UNIQUE,
    password_hash NVARCHAR(255) NOT NULL,
    role_id INT NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_User_Role FOREIGN KEY (role_id) REFERENCES Role(role_id)
);

CREATE TABLE StudentGroup (
    group_id INT IDENTITY(1,1) PRIMARY KEY,
    group_code NVARCHAR(50) NOT NULL UNIQUE,
    group_name NVARCHAR(120) NOT NULL,
    course_code NVARCHAR(30) NOT NULL,
    semester NVARCHAR(30) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),

    CONSTRAINT UQ_Group_GroupCourseSemester UNIQUE (group_id, course_code, semester)
);

CREATE TABLE GroupMember (
    group_id INT NOT NULL,
    user_id INT NOT NULL,
    member_role_id INT NOT NULL,

    course_code NVARCHAR(30) NOT NULL,
    semester NVARCHAR(30) NOT NULL,

    joined_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),

    PRIMARY KEY (group_id, user_id),

    CONSTRAINT FK_GroupMember_User
        FOREIGN KEY (user_id) REFERENCES Users(user_id),

    CONSTRAINT FK_GroupMember_MemberRole
        FOREIGN KEY (member_role_id) REFERENCES MemberRole(member_role_id),

    CONSTRAINT FK_GroupMember_Group
        FOREIGN KEY (group_id) REFERENCES StudentGroup(group_id) ON DELETE CASCADE,

    CONSTRAINT FK_GroupMember_Group_CourseSemester
        FOREIGN KEY (group_id, course_code, semester)
        REFERENCES StudentGroup(group_id, course_code, semester),

    CONSTRAINT UQ_OneGroupPerStudentPerTerm
        UNIQUE (user_id, course_code, semester)
);

CREATE UNIQUE INDEX UX_Group_OneLeader
ON GroupMember(group_id)
WHERE member_role_id = 1;

CREATE TABLE LecturerAssignment (
    group_id INT PRIMARY KEY,
    lecturer_id INT NOT NULL,
    assigned_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_LectAssign_Group FOREIGN KEY (group_id) REFERENCES StudentGroup(group_id) ON DELETE CASCADE,
    CONSTRAINT FK_LectAssign_Lecturer FOREIGN KEY (lecturer_id) REFERENCES Users(user_id)
);

CREATE TABLE IntegrationConfig (
    config_id INT IDENTITY(1,1) PRIMARY KEY,
    group_id INT NOT NULL,
    integration_type_id INT NOT NULL,
    base_url NVARCHAR(255) NULL,        -- Jira
    project_key NVARCHAR(50) NULL,      -- Jira
    repo_full_name NVARCHAR(200) NULL,  -- GitHub
    token_encrypted VARBINARY(MAX) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_Config_Group FOREIGN KEY (group_id) REFERENCES StudentGroup(group_id) ON DELETE CASCADE,
    CONSTRAINT FK_Config_Type FOREIGN KEY (integration_type_id) REFERENCES IntegrationType(integration_type_id),
    CONSTRAINT UX_Group_Integration UNIQUE (group_id, integration_type_id)
);

CREATE TABLE Requirement (
    requirement_id INT IDENTITY(1,1) PRIMARY KEY,
    group_id INT NOT NULL,
    req_code NVARCHAR(50) NOT NULL,
    title NVARCHAR(200) NOT NULL,
    description NVARCHAR(MAX) NULL,
    priority_id INT NOT NULL,
    status_id INT NOT NULL,
    created_by INT NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),

    jira_issue_key NVARCHAR(50) NULL,

    CONSTRAINT FK_Req_Group FOREIGN KEY (group_id) REFERENCES StudentGroup(group_id) ON DELETE CASCADE,
    CONSTRAINT FK_Req_Priority FOREIGN KEY (priority_id) REFERENCES Priority(priority_id),
    CONSTRAINT FK_Req_Status FOREIGN KEY (status_id) REFERENCES RequirementStatus(status_id),
    CONSTRAINT FK_Req_CreatedBy FOREIGN KEY (created_by) REFERENCES Users(user_id),

    CONSTRAINT UX_Req_Code UNIQUE (group_id, req_code),
    CONSTRAINT UX_Req_Jira UNIQUE (jira_issue_key)
);

CREATE TABLE Task (
    task_id INT IDENTITY(1,1) PRIMARY KEY,
    requirement_id INT NOT NULL,
    group_id INT NOT NULL,
    title NVARCHAR(200) NOT NULL,
    description NVARCHAR(MAX) NULL,
    assignee_id INT NULL,
    status_id INT NOT NULL,
    estimate_hours DECIMAL(6,2) NULL,
    start_date DATE NULL,
    due_date DATE NULL,
    completed_at DATETIME2 NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),

    jira_issue_key NVARCHAR(50) NULL,

    CONSTRAINT FK_Task_Req FOREIGN KEY (requirement_id) REFERENCES Requirement(requirement_id) ON DELETE CASCADE,

    CONSTRAINT FK_Task_Group FOREIGN KEY (group_id) REFERENCES StudentGroup(group_id) ON DELETE NO ACTION,
    CONSTRAINT FK_Task_Assignee FOREIGN KEY (assignee_id) REFERENCES Users(user_id),
    CONSTRAINT FK_Task_Status FOREIGN KEY (status_id) REFERENCES TaskStatus(status_id),
    CONSTRAINT UX_Task_Jira UNIQUE (jira_issue_key)
);

CREATE TABLE TaskStatusHistory (
    history_id INT IDENTITY(1,1) PRIMARY KEY,
    task_id INT NOT NULL,
    from_status_id INT NULL,
    to_status_id INT NOT NULL,
    changed_by INT NOT NULL,
    changed_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    note NVARCHAR(255) NULL,
    CONSTRAINT FK_Hist_Task FOREIGN KEY (task_id) REFERENCES Task(task_id) ON DELETE CASCADE,
    CONSTRAINT FK_Hist_FromStatus FOREIGN KEY (from_status_id) REFERENCES TaskStatus(status_id),
    CONSTRAINT FK_Hist_ToStatus FOREIGN KEY (to_status_id) REFERENCES TaskStatus(status_id),
    CONSTRAINT FK_Hist_ChangedBy FOREIGN KEY (changed_by) REFERENCES Users(user_id)
);

CREATE TABLE Repository (
    repo_id INT IDENTITY(1,1) PRIMARY KEY,
    group_id INT NOT NULL,
    full_name NVARCHAR(200) NOT NULL UNIQUE,
    default_branch NVARCHAR(100) NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_Repo_Group FOREIGN KEY (group_id) REFERENCES StudentGroup(group_id) ON DELETE CASCADE
);

CREATE TABLE GitCommit (
    commit_id INT IDENTITY(1,1) PRIMARY KEY,
    repo_id INT NOT NULL,
    sha NVARCHAR(64) NOT NULL,
    author_user_id INT NULL,
    author_name NVARCHAR(120) NULL,
    author_email NVARCHAR(120) NULL,
    author_login NVARCHAR(100) NULL,
    commit_date DATETIME2 NOT NULL,
    message NVARCHAR(MAX) NULL,
    additions INT NULL,
    deletions INT NULL,
    files_changed INT NULL,
    CONSTRAINT FK_GitCommit_Repo FOREIGN KEY (repo_id) REFERENCES Repository(repo_id) ON DELETE CASCADE,
    CONSTRAINT FK_GitCommit_AuthorUser FOREIGN KEY (author_user_id) REFERENCES Users(user_id),
    CONSTRAINT UX_Repo_SHA UNIQUE (repo_id, sha)
);

CREATE TABLE SyncLog (
    sync_id INT IDENTITY(1,1) PRIMARY KEY,
    group_id INT NOT NULL,
    source NVARCHAR(20) NOT NULL, -- JIRA/GITHUB
    started_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    ended_at DATETIME2 NULL,
    status NVARCHAR(20) NOT NULL, -- SUCCESS/FAILED
    detail_message NVARCHAR(MAX) NULL,
    CONSTRAINT FK_Sync_Group FOREIGN KEY (group_id) REFERENCES StudentGroup(group_id) ON DELETE CASCADE
);

-- Helpful indexes
CREATE INDEX IX_Task_GroupStatus ON Task(group_id, status_id);
CREATE INDEX IX_Task_DueDate ON Task(group_id, due_date);
CREATE INDEX IX_GitCommit_Date ON GitCommit(repo_id, commit_date);
CREATE INDEX IX_Sync_GroupDate ON SyncLog(group_id, started_at);