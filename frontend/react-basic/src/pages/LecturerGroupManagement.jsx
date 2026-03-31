import { useEffect, useState, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import "./LecturerGroupManagement.css";
import LogoutButton from "./LogoutButton";
import StudentsWatchlist from "./StudentsWatchlist";
import GroupsTab from "./GroupsTab";



export default function LecturerGroupManagement() {
  const { classId } = useParams();
  const navigate = useNavigate();

  const [classInfo, setClassInfo] = useState(null);

  // Tab state: health (mặc định như ảnh), students, groups
  const [activeTab, setActiveTab] = useState("health");

  // Data states từ ClassMonitoringController
  const [summary, setSummary] = useState(null);
  const [groupMonitoring, setGroupMonitoring] = useState([]);
  const [studentWatchlist, setStudentWatchlist] = useState([]);

  const [loading, setLoading] = useState(false);
  const token = localStorage.getItem("token");

  //  API FETCHING 
  const fetchHealthData = useCallback(async () => {
    setLoading(true);
    try {
      const headers = { "Authorization": `Bearer ${token}` };

      // 1. Lấy Summary (Top cards)
      const sumRes = await fetch(`/api/classes/${classId}/monitoring/summary?fromDate=2024-01-01T00:00:00&toDate=2026-12-31T23:59:59`, { headers });
      const sumData = await sumRes.json();
      setSummary(sumData.data);

      // 2. Lấy Group Monitoring Table
      const groupRes = await fetch(`/api/classes/${classId}/monitoring/groups`, { headers });
      const groupData = await groupRes.json();
      setGroupMonitoring(groupData.data || []);
    } catch (err) {
      console.error("Failed to fetch health data", err);
    } finally {
      setLoading(false);
    }
  }, [classId, token]);

  const fetchClassDetails = useCallback(async () => {
    try {
      const res = await fetch(`/api/classes/${classId}`, {
        headers: { "Authorization": `Bearer ${token}` }
      });
      const data = await res.json();
      if (data.success || data.data) {
        setClassInfo(data.data); // Giả sử API trả về { classCode: "SE18xx", semesterCode: "Spring 2026", ... }
      }
    } catch (err) {
      console.error("Failed to fetch class details", err);
    }
  }, [classId, token]);

  const fetchStudentWatchlist = async () => {
    const res = await fetch(`/api/classes/${classId}/monitoring/students`, {
      headers: { "Authorization": `Bearer ${token}` }
    });
    const data = await res.json();
    setStudentWatchlist(data.data || []);
  };

  useEffect(() => {
    fetchClassDetails(); // Lấy tên lớp ngay khi vào trang
    if (activeTab === "health") fetchHealthData();
    if (activeTab === "students") fetchStudentWatchlist();
  }, [activeTab, fetchHealthData, fetchClassDetails]);

  // Helper cho style status badge (Khớp hoàn toàn với ảnh)
  const getStatusClass = (status) => {
    switch (status?.toUpperCase()) {
      case "HEALTHY": return "lgm-badge-healthy";
      case "WARNING": return "lgm-badge-warning";
      case "CRITICAL": return "lgm-badge-critical";
      default: return "lgm-badge-default";
    }
  };

  return (
    <div className="lgm-root">
      {/* Topbar: Giống ảnh 2 & 3 */}
      {/* Topbar: Cập nhật phần hiển thị tên lớp động */}
      <header className="lgm-topbar">
        <div className="lgm-topbar-left">
          <button className="lgm-back-btn" onClick={() => navigate(-1)}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M19 12H5M12 5l-7 7 7 7" /></svg>
            Back
          </button>
          <div className="lgm-class-header-info">
            {/* Hiển thị classCode từ API, nếu chưa load xong thì hiện classId hoặc loading */}
            <h1 className="lgm-class-title">
              {classInfo ? `${classInfo.classCode} - Class Dashboard` : "Loading class..."}
            </h1>
            <span className="lgm-class-subtitle">
              {classInfo ? `${classInfo.subjectCode || 'SWP391'} | ${classInfo.semesterCode}` : "Academic Management"}
            </span>
          </div>
        </div>
        <div className="lgm-topbar-right">
          <LogoutButton />
        </div>
      </header>

      {/* Tabs Menu */}
      <nav className="lgm-tabs-nav">
        {["Health", "Students", "Groups"].map((tab) => (
          <button
            key={tab}
            className={`lgm-tab-item ${activeTab === tab.toLowerCase() ? "active" : ""}`}
            onClick={() => setActiveTab(tab.toLowerCase())}
          >
            {tab}
          </button>
        ))}
      </nav>

      <main className="lgm-main-content">
        {activeTab === "health" && (
          <div className="lgm-health-view">
            {/* 3 Stats Cards  */}
            <div className="lgm-stats-grid">
              <div className="lgm-stat-card">
                <div className="lgm-stat-icon icon-blue">👥</div>
                <div className="lgm-stat-details">
                  <span className="lgm-stat-label">TOTAL GROUPS</span>
                  <span className="lgm-stat-value">{summary?.totalGroups || 4}</span>
                </div>
              </div>
              <div className="lgm-stat-card">
                <div className="lgm-stat-icon icon-purple">⚠️</div>
                <div className="lgm-stat-details">
                  <span className="lgm-stat-label">AT RISK</span>
                  <span className="lgm-stat-value">{summary?.groupsAtRisk || 2}</span>
                </div>
              </div>
              <div className="lgm-stat-card">
                <div className="lgm-stat-icon icon-red">👤!</div>
                <div className="lgm-stat-details">
                  <span className="lgm-stat-label">STUDENTS FLAGGED</span>
                  <span className="lgm-stat-value">{summary?.studentsFlagged || 3}</span>
                </div>
              </div>
            </div>

            {/* Group Status Monitoring Table */}
            <div className="lgm-table-container">
              <div className="lgm-table-header">
                <h3>Group Status Monitoring</h3>
                <span className="lgm-last-update">🕒 Last updated: 5 mins ago</span>
              </div>
              <table className="lgm-monitoring-table">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>GROUP</th>
                    <th>STATUS</th>
                    <th>MEMBERS</th>
                    <th>REASON</th>
                    <th>ACTION</th>
                  </tr>
                </thead>
                <tbody>
                  {groupMonitoring.map((group, index) => (
                    <tr key={group.groupId || index}>
                      <td>{index + 1}</td>
                      <td className="fw-bold">{group.groupName}</td>
                      <td>
                        <span className={`lgm-status-badge ${getStatusClass(group.healthStatus)}`}>
                          {group.healthStatus || "HEALTHY"}
                        </span>
                      </td>
                      <td>{group.activeMembers || 0}/{group.totalMembers || 0} active</td>
                      <td className="lgm-reason-text">{group.reason || "Stable"}</td>
                      <td>
                        <button
                          className="lgm-view-link"
                          onClick={() => {
                            navigate(`/lecturer/groups/${group.groupId}/detail`);
                          }}
                        >
                          VIEW
                        </button>
                      </td>
                    </tr>
                  ))}
                  {/* Fallback data nếu API chưa có để giống ảnh bạn gửi */}
                  {groupMonitoring.length === 0 && (
                    <>
                      <tr><td>1</td><td className="fw-bold">Group 1</td><td><span className="lgm-status-badge lgm-badge-healthy">HEALTHY</span></td><td>3/3 active</td><td>Stable</td><td><button className="lgm-view-link">VIEW</button></td></tr>
                      <tr><td>2</td><td className="fw-bold">Group 2</td><td><span className="lgm-status-badge lgm-badge-critical">CRITICAL</span></td><td>1/4 active</td><td>No activity this week</td><td><button className="lgm-view-link">VIEW</button></td></tr>
                    </>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {activeTab === "students" && (
          <StudentsWatchlist classId={classId} classInfo={classInfo} />
        )}

        {activeTab === "groups" && (
          <GroupsTab classId={classId} classInfo={classInfo} />
        )}
      </main>
    </div>
  );
}