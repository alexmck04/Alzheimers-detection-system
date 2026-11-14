import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { db, auth } from "../firebase";
import { collection, getDocs, query, where } from "firebase/firestore";
import { onAuthStateChanged } from "firebase/auth";
import "../styles.css";

export default function RisksPage() {
  const [patients, setPatients] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (user) => {
      if (user) {
        await loadAssignedPatientsWithRisk(user.uid);
      } else {
        setLoading(false);
      }
    });
    return () => unsubscribe();
  }, []);

  const loadAssignedPatientsWithRisk = async (doctorUid) => {
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

      const withRisk = await Promise.all(
        list.map(async (u) => {
          const avgRaw = await getAverageRawScoreForUser(u.id);
          const riskPercent =
            avgRaw === null ? null : Math.max(0, Math.min(100, 100 - avgRaw * 2));
          return { ...u, avgRawScore: avgRaw, riskPercent };
        })
      );

      const sortedList = withRisk.sort((a, b) => {
        const scoreA = a.riskPercent ?? -1;
        const scoreB = b.riskPercent ?? -1;
        return scoreB - scoreA;
      });

      setPatients(sortedList);
    } catch (error) {
      console.error("Error loading risks:", error);
    } finally {
      setLoading(false);
    }
  };

  const getAverageRawScoreForUser = async (userId) => {
    try {
      const cogSnap = await getDocs(
        collection(db, "tests", userId, "cognitiveTests")
      );
      const lifeSnap = await getDocs(
        collection(db, "tests", userId, "lifestyleTests")
      );

      const scores = [];

      cogSnap.forEach((d) => {
        const data = d.data();
        const s = Number(data.score);
        if (!Number.isNaN(s)) scores.push(s);
      });

      lifeSnap.forEach((d) => {
        const data = d.data();
        const s = Number(data.rawScore ?? data.score);
        if (!Number.isNaN(s)) scores.push(s);
      });

      if (scores.length === 0) return null;

      const avg = scores.reduce((a, b) => a + b, 0) / scores.length;
      return avg;
    } catch (e) {
      console.error("Error reading tests for user:", userId, e);
      return null;
    }
  };

  // CALCULATE AGE FROM DOB
  const calculateAge = (dobString) => {
    if (!dobString) return "N/A";

    let birthDate = new Date(dobString);

    if (isNaN(birthDate.getTime())) {
      if (/^\d{1,2}[\/-]\d{1,2}[\/-]\d{4}$/.test(dobString)) {
        const parts = dobString.split(/[\/-]/);
        birthDate = new Date(`${parts[2]}-${parts[1]}-${parts[0]}`);
      }
    }

    if (isNaN(birthDate.getTime())) {
      return dobString;
    }

    const today = new Date();
    let age = today.getFullYear() - birthDate.getFullYear();
    const m = today.getMonth() - birthDate.getMonth();

    if (m < 0 || (m === 0 && today.getDate() < birthDate.getDate())) {
      age--;
    }
    return age;
  };

  const getRiskInfo = (score) => {
    if (score === undefined || score === null)
      return { color: "#777", bg: "#f0f0f0", label: "No Data" };
    if (score >= 80)
      return { color: "#d32f2f", bg: "rgba(211, 47, 47, 0.1)", label: "High Risk" };
    if (score >= 40)
      return { color: "#ed6c02", bg: "rgba(237, 108, 2, 0.1)", label: "Medium Risk" };
    return { color: "#2e7d32", bg: "rgba(46, 125, 50, 0.1)", label: "Low Risk" };
  };

  if (loading) return <div style={{ padding: "2rem" }}>Loading risk analysis...</div>;

  return (
    <div className="dashboard">
      <h2 className="page-title">Patient Risk Overview</h2>

      {patients.length === 0 ? (
        <div className="home-card">
          <p>No patients found. Assign patients to view their risk scores.</p>
        </div>
      ) : (
        <div className="news-grid">
          {patients.map((p) => {
            const score = p.riskPercent;
            const risk = getRiskInfo(score);
            const age = calculateAge(p.dob);

            return (
              <Link
                key={p.id}
                to={`/risks/${p.id}`}
                style={{ textDecoration: "none", color: "inherit" }}
              >
                <div
                  className="home-card"
                  style={{
                    padding: "1.5rem",
                    borderTop: `6px solid ${risk.color}`,
                    display: "flex",
                    flexDirection: "column",
                    justifyContent: "space-between",
                    height: "100%",
                  }}
                >
                  <div
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "start",
                      marginBottom: "15px",
                    }}
                  >
                    <div>
                      <h3 style={{ margin: "0 0 5px 0", color: "#0a2540" }}>
                        {p.firstName || "Unknown"} {p.lastName || ""}
                      </h3>
                      <span style={{ fontSize: "0.9rem", color: "#666" }}>
                        Age: {age}
                      </span>
                    </div>

                    <span
                      style={{
                        background: risk.bg,
                        color: risk.color,
                        padding: "5px 10px",
                        borderRadius: "12px",
                        fontWeight: "bold",
                        fontSize: "0.85rem",
                      }}
                    >
                      {risk.label}
                    </span>
                  </div>

                  <div style={{ textAlign: "center", margin: "10px 0" }}>
                    <span
                      style={{
                        fontSize: "3rem",
                        fontWeight: "800",
                        color: risk.color,
                      }}
                    >
                      {score === null || score === undefined
                        ? "--"
                        : `${Math.round(score)}%`}
                    </span>
                    <p style={{ margin: 0, fontSize: "0.9rem", color: "#888" }}>
                      Dementia Probability
                    </p>

                    {p.avgRawScore !== null && p.avgRawScore !== undefined && (
                      <p style={{ margin: "6px 0 0 0", fontSize: "0.85rem", color: "#888" }}>
                        Avg raw score: {p.avgRawScore.toFixed(1)}
                      </p>
                    )}
                  </div>

                  <div style={{ marginTop: "15px", textAlign: "right" }}>
                    <span style={{ color: "#ff7a00", fontWeight: "600", fontSize: "0.9rem" }}>
                      View Profile →
                    </span>
                  </div>
                </div>
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
}
