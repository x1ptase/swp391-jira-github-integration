import { useState } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import GitHubConfig from "./GitHubConfig";
import JiraConfig from "./JiraConfig";
import MyWork from "./MyWork";
import "./LeaderDashboard.css";
import GroupMemberList from "./GroupMemberList";

function useMemberRole() {
  const location = useLocation();
  const params = new URLSearchParams(location.search);
  return params.get("memberRole") || "MEMBER";
}

const ALL_TABS = [
  {
    id: "members",
    label: "Members",
    leaderOnly: false,
    icon: (
      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
        <circle cx="9" cy="7" r="4"/>
        <path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>
      </svg>
    ),
  },
  {
    id: "mywork",
    label: "My Tasks",
    leaderOnly: false,
    icon: (
      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
        <path d="M9 11l3 3L22 4"/>
        <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>
      </svg>
    ),
  },
  {
    id: "github",
    label: "GitHub",
    leaderOnly: true,
    icon: (
      <svg width="15" height="15" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0 0 24 12c0-6.63-5.37-12-12-12z"/>
      </svg>
    ),
  },
  {
    id: "jira",
    label: "Jira",
    leaderOnly: true,
    icon: (
      <svg width="15" height="15" viewBox="0 0 24 24" fill="none">
        <path d="M11.53 2.034a.9.9 0 0 1 .94 0l8.1 4.95a.9.9 0 0 1 .43.768v9.496a.9.9 0 0 1-.43.768l-8.1 4.95a.9.9 0 0 1-.94 0l-8.1-4.95A.9.9 0 0 1 3 17.248V7.752a.9.9 0 0 1 .43-.768l8.1-4.95z" fill="#0052CC"/>
        <path d="M12 7.5L8 12l4 4.5 4-4.5L12 7.5z" fill="white"/>
      </svg>
    ),
  },
];

export default function LeaderDashboard() {
  const { groupId } = useParams();
  const navigate = useNavigate();
  const memberRole = useMemberRole();
  const isLeader = memberRole === "LEADER";

  const tabs = ALL_TABS.filter(t => !t.leaderOnly || isLeader);
  const [activeTab, setActiveTab] = useState("members");

  return (
    <div className="ld-root">
      {/* Topbar */}
      <div className="ld-topbar">
        <button className="ld-back-btn" onClick={() => navigate(-1)}>
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <path d="M19 12H5M12 5l-7 7 7 7"/>
          </svg>
          Back
        </button>
        <div className="ld-group-info">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
            <circle cx="9" cy="7" r="4"/>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
          Group #{groupId}
        </div>
        <span className={`ld-role-chip ${isLeader ? "ld-role-leader" : "ld-role-member"}`}>
          {isLeader ? (
            <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            </svg>
          ) : (
            <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
              <circle cx="12" cy="7" r="4"/>
            </svg>
          )}
          {isLeader ? "Leader" : "Member"}
        </span>
      </div>

      {/* Page title */}
      <div className="ld-page-header">
        <h1 className="ld-page-title">Group Detail</h1>
        <p className="ld-page-desc">
          {isLeader ? "Manage integrations and view group info" : "View group members and your tasks"}
        </p>
      </div>

      {/* Tabs */}
      <div className="ld-tabs">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            className={`ld-tab ${activeTab === tab.id ? "ld-tab-active" : ""}`}
            onClick={() => setActiveTab(tab.id)}
          >
            <span className="ld-tab-icon">{tab.icon}</span>
            {tab.label}
          </button>
        ))}
      </div>

      {/* Content */}
      <div className="ld-content">
        {activeTab === "members" && <GroupMemberList groupId={groupId} />}
        {activeTab === "mywork"  && <MyWork groupId={groupId} />}
        {activeTab === "github"  && isLeader && <GitHubConfig />}
        {activeTab === "jira"    && isLeader && <JiraConfig />}
      </div>
    </div>
  );
}