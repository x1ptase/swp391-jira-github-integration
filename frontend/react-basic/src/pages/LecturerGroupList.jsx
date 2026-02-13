import { useEffect, useState } from "react";
import "./LecturerGroupList.css";

const API_URL = "/api/lecturer/groups";

function LecturerGroupList() {
  const [groups, setGroups] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const token = localStorage.getItem("token");

  useEffect(() => {
    fetchMyGroups();
  }, []);

  const fetchMyGroups = async () => {
    setLoading(true);
    setError("");
    
    try {
      const res = await fetch(API_URL, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!res.ok) {
        throw new Error("Failed to fetch groups");
      }

      const data = await res.json();
      setGroups(data.data || []);
    } catch (err) {
      setError(err.message || "Error occurred");
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="lecturer-container">
        <h3>My Assigned Groups</h3>
        <p>Loading...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="lecturer-container">
        <h3>My Assigned Groups</h3>
        <p className="error">{error}</p>
      </div>
    );
  }

  return (
    <div className="lecturer-container">
      <h3>My Assigned Groups</h3>
      <p className="subtitle">You are assigned to {groups.length} group(s)</p>

      {groups.length === 0 ? (
        <div className="empty-state">
          <span className="empty-icon">📋</span>
          <p>No groups assigned yet</p>
          <p className="empty-hint">Contact your admin to assign you to groups</p>
        </div>
      ) : (
        <div className="groups-grid">
          {groups.map((group) => (
            <div key={group.groupId} className="group-card">
              <div className="group-header">
                <h4>{group.groupName}</h4>
                <span className="group-code">{group.groupCode}</span>
              </div>
              <div className="group-details">
                <div className="detail-item">
                  <span className="detail-label">Course:</span>
                  <span className="detail-value">{group.courseCode}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Semester:</span>
                  <span className="detail-value">{group.semester}</span>
                </div>
              </div>
              <button className="view-btn">View Details</button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default LecturerGroupList;