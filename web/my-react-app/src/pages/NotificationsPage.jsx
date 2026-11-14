import { useEffect, useMemo, useState } from "react";
import { db, auth } from "../firebase";
import {
  collection,
  getDocs,
  query,
  where,
  orderBy,
  doc,
  getDoc,
  updateDoc
} from "firebase/firestore";
import { onAuthStateChanged } from "firebase/auth";
import "../styles.css";

function formatTimestamp(ts) {
  if (!ts) return "Unknown time";
  const d = new Date(Number(ts));
  if (Number.isNaN(d.getTime())) return String(ts);
  return d.toLocaleString();
}

function prettyType(t) {
  const s = (t || "").toLowerCase();
  if (s === "cognitive") return "Cognitive Test";
  if (s === "lifestyle") return "Lifestyle Test";
  if (s === "speech") return "Speech Test";
  return t || "Test";
}

export default function NotificationsPage() {
  const [doctorId, setDoctorId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [notifs, setNotifs] = useState([]);
  const [patientNameById, setPatientNameById] = useState({});
  const [error, setError] = useState("");

  useEffect(() => {
    const unsub = onAuthStateChanged(auth, async (user) => {
      if (!user) {
        setDoctorId(null);
        setNotifs([]);
        setLoading(false);
        return;
      }
      setDoctorId(user.uid);
    });
    return () => unsub();
  }, []);

  useEffect(() => {
    if (!doctorId) return;

    const load = async () => {
      setLoading(true);
      setError("");

      try {
        const q = query(
          collection(db, "notifications"),
          where("assignedDoctorId", "==", doctorId),
          where("seen", "==", false)
        );

        const snap = await getDocs(q);
        const list = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
        list.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));

        setNotifs(list);
      } catch (e) {
        console.error(e);
        setError(
          e?.message ||
            "Failed to load notifications. (You might need a Firestore index for this query.)"
        );
      } finally {
        setLoading(false);
      }
    };

    load();
  }, [doctorId]);

  const uniquePatientIds = useMemo(() => {
    const set = new Set();
    notifs.forEach((n) => {
      if (n.patientId) set.add(n.patientId);
    });
    return Array.from(set);
  }, [notifs]);

  useEffect(() => {
    if (uniquePatientIds.length === 0) return;

    const loadPatients = async () => {
      const updates = {};

      await Promise.all(
        uniquePatientIds.map(async (pid) => {
          if (patientNameById[pid]) return;

          try {
            const snap = await getDoc(doc(db, "users", pid));
            if (snap.exists()) {
              const u = snap.data();
              const first = u.firstName || "Unknown";
              const last = u.lastName || "";
              updates[pid] = `${first} ${last}`.trim();
            } else {
              updates[pid] = "Unknown Patient";
            }
          } catch {
            updates[pid] = "Unknown Patient";
          }
        })
      );

      if (Object.keys(updates).length > 0) {
        setPatientNameById((prev) => ({ ...prev, ...updates }));
      }
    };

    loadPatients();
  }, [uniquePatientIds]);

  const markSeen = async (notifId) => {
    try {
      await updateDoc(doc(db, "notifications", notifId), { seen: true });
      setNotifs((prev) => prev.filter((n) => n.id !== notifId));
    } catch (e) {
      console.error(e);
      alert("Failed to mark as seen.");
    }
  };

  if (loading) return <div style={{ padding: "2rem" }}>Loading notifications...</div>;

  return (
    <div className="dashboard">
      <h2 className="page-title">Notifications</h2>

      {error && (
        <div className="home-card" style={{ borderLeft: "6px solid #d32f2f" }}>
          <p style={{ margin: 0 }}>{error}</p>
        </div>
      )}

      {!error && notifs.length === 0 ? (
        <div className="home-card">
          <p style={{ margin: 0 }}>No new notifications.</p>
        </div>
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
          {notifs.map((n) => {
            const patientName = patientNameById[n.patientId] || n.patientId || "Unknown Patient";

            return (
              <div key={n.id} className="home-card" style={{ padding: "1.25rem" }}>
                <div style={{ display: "flex", justifyContent: "space-between", gap: "14px" }}>
                  <div style={{ minWidth: 0 }}>
                    <h3 style={{ margin: "0 0 6px 0" }}>{patientName}</h3>

                    <div style={{ display: "flex", gap: "10px", flexWrap: "wrap" }}>
                      <span style={{ fontWeight: 700 }}>{prettyType(n.type)}</span>
                      <span>Risk: <strong>{n.riskPercent ?? "--"}%</strong></span>
                      <span>Raw: <strong>{n.rawScore ?? "--"}</strong></span>
                    </div>

                    <div style={{ marginTop: "8px", color: "#666", fontSize: "0.9rem" }}>
                      {formatTimestamp(n.timestamp)}
                    </div>
                  </div>

                  <button
                    className="home-edit-btn"
                    onClick={() => markSeen(n.id)}
                    style={{ marginTop: 0, height: "fit-content", whiteSpace: "nowrap" }}
                  >
                    Mark as seen
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
