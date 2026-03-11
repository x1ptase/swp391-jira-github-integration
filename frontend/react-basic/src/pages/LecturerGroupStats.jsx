import { useParams, useNavigate } from "react-router-dom";
import CommitStats from "./CommitStats";
import RequirementDashboard from "./RequirementDashboard";
import "./LecturerGroupStats.css";

export default function LecturerGroupStats() {
  const { groupId } = useParams();
  const navigate = useNavigate();

  return (
    <div className="lgs-root">
      <div className="lgs-topbar">
        <button className="lgs-back-btn" onClick={() => navigate(-1)}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <path d="M19 12H5M12 5l-7 7 7 7"/>
          </svg>
          Back to Groups
        </button>
        <span className="lgs-group-id">Group #{groupId}</span>
      </div>

      <CommitStats groupId={groupId} />
      <RequirementDashboard groupId={groupId} />
    </div>
  );
}