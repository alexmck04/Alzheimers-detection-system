import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { db, auth } from "../firebase";
import { collection, getDocs, query, where } from "firebase/firestore";
import { onAuthStateChanged } from "firebase/auth";
import "../styles.css";

export default function ReportsPage() {
  const [patients, setPatients] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (user) => {
      if (user?.uid) {
        await loadAssignedUsers(user.uid);
      } else {
        setPatients([]);
        setLoading(false);
      }
    });

    return () => unsubscribe();
  }, []);

  const loadAssignedUsers = async (doctorUid) => {
    setLoading(true);
    try {
      const q = query(
        collection(db, "users"),
        where("assignedDoctorId", "==", doctorUid)
      );

      const snap = await getDocs(q);

      const list = snap.docs.map((docSnap) => ({
        ...docSnap.data(),
        id: docSnap.id,
      }));

      list.sort((a, b) => {
        const aName = `${a.firstName || ""} ${a.lastName || ""}`.trim().toLowerCase();
        const bName = `${b.firstName || ""} ${b.lastName || ""}`.trim().toLowerCase();
        return aName.localeCompare(bName);
      });

      setPatients(list);
    } catch (error) {
      console.error("Error loading assigned users:", error);
      setPatients([]);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div style={{ padding: "2rem" }}>Loading your patients...</div>;

  return (
    <div className="dashboard">
      <h2 className="page-title">Reports</h2>

      {patients.length === 0 ? (
        <div className="home-card">
          <p>No patients have been assigned to you yet.</p>
        </div>
      ) : (
        <div className="patient-list">
          {patients.map((p) => (
            <Link
              key={p.id}
              to={`/reports/${p.id}`}
              style={{ textDecoration: "none", color: "inherit" }}
            >
              <div className="patient-card">
                <h3 style={{ marginTop: 0 }}>
                  {p.firstName || "Unknown"} {p.lastName || ""}
                </h3>

                <p style={{ margin: "6px 0" }}>
                  <strong>Email:</strong> {p.email || "Not recorded"}
                </p>

                <p style={{ margin: "6px 0" }}>
                  <strong>Address:</strong> {p.address || "Not recorded"}
                </p>

                <p style={{ margin: "6px 0" }}>
                  <strong>Phone:</strong> {p.phone || "Not recorded"}
                </p>

                <div style={{ marginTop: "10px", textAlign: "right" }}>
                  <span style={{ color: "#ff7a00", fontWeight: 600 }}>
                    Open Report →
                  </span>
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
