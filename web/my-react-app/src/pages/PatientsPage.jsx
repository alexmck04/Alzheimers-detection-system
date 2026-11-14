import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { db, auth } from "../firebase";
import { collection, getDocs, query, where } from "firebase/firestore";
import { onAuthStateChanged } from "firebase/auth";

export default function PatientsPage() {
  const [patients, setPatients] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (user) => {
      if (user) {
        await loadAssignedPatients(user.uid);
      } else {
        setLoading(false);
      }
    });

    return () => unsubscribe();
  }, []);

  const loadAssignedPatients = async (doctorUid) => {
    try {
      const q = query(
        collection(db, "users"),
        where("assignedDoctorId", "==", doctorUid)
      );

      const snap = await getDocs(q);

      const list = snap.docs.map((doc) => ({
        id: doc.id,
        ...doc.data(),
      }));

      setPatients(list);
    } catch (error) {
      console.error("Error loading patients:", error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div style={{ padding: "2rem" }}>Loading your patients...</div>;

  return (
    <div>
      <h2 className="patients-page-title">My Assigned Patients</h2>

      {patients.length === 0 ? (
        <div className="home-card">
          <p>No patients have been assigned to you yet.</p>
        </div>
      ) : (
        <div className="patient-list">
          {patients.map((p) => (
            <Link
              key={p.id}
              to={`/patients/${p.id}`}
              style={{ textDecoration: "none", color: "inherit" }}
            >
              <div className="patient-card">
                <h3>
                  {p.firstName || "Unknown"} {p.lastName || ""}
                </h3>

                <p><strong>Email:</strong> {p.email || "No email"}</p>

                <p><strong>Address:</strong> {p.address || "No address"}</p>

                <p>
                  <strong>Emergency Contact:</strong>{" "}
                  {(p.emergencyContactName || "N/A")} — {(p.emergencyContactNumber || "N/A")}
                </p>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
