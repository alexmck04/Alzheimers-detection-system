import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { db } from "../firebase";
import { doc, getDoc, getDocs, collection } from "firebase/firestore";
import "../styles.css";

export default function RiskDetailPage() {
  const { id } = useParams();

  const [patient, setPatient] = useState(null);
  const [loading, setLoading] = useState(true);

  const [activeTab, setActiveTab] = useState("cognitive");
  const [cognitiveTests, setCognitiveTests] = useState([]);
  const [lifestyleTests, setLifestyleTests] = useState([]);
  const [speechTests, setSpeechTests] = useState([]);

  useEffect(() => {
    loadAll();
  }, [id]);

  const loadAll = async () => {
    setLoading(true);
    try {
      const userSnap = await getDoc(doc(db, "users", id));
      if (userSnap.exists()) {
        setPatient({ id: userSnap.id, ...userSnap.data() });
      } else {
        setPatient(null);
      }

      const [cog, life, speech] = await Promise.all([
        loadTests(id, "cognitiveTests"),
        loadTests(id, "lifestyleTests"),
        loadSpeechPredictions(id),
      ]);

      setCognitiveTests(cog);
      setLifestyleTests(life);
      setSpeechTests(speech);
    } catch (e) {
      console.error("Error loading patient risks:", e);
    } finally {
      setLoading(false);
    }
  };

  const loadTests = async (userId, subcollection) => {
    try {
      const snap = await getDocs(collection(db, "tests", userId, subcollection));
      const list = snap.docs.map((d) => ({
        id: d.id,
        ...d.data(),
      }));
      list.sort((a, b) => (toMillis(b.timestamp) || 0) - (toMillis(a.timestamp) || 0));
      return list;
    } catch (e) {
      return [];
    }
  };

  const loadSpeechPredictions = async (userId) => {
    try {
      const snap = await getDocs(collection(db, "tests", userId, "audioTests"));
      const list = snap.docs.map((d) => ({
        id: d.id,
        ...d.data(),
      }));
      list.sort((a, b) => (toMillis(b.created_at) || 0) - (toMillis(a.created_at) || 0));
      return list;
    } catch (e) {
      console.error("Error loading speech predictions:", e);
      return [];
    }
  };

  const toMillis = (v) => {
    if (!v) return null;
    if (typeof v === "number") return v;
    if (v instanceof Date) return v.getTime();
    if (typeof v?.toMillis === "function") return v.toMillis();
    if (typeof v?.seconds === "number")
      return v.seconds * 1000 + Math.floor((v.nanoseconds || 0) / 1e6);
    return null;
  };

  const formatDate = (v) => {
    const ms = toMillis(v);
    if (!ms) return "Unknown";
    const d = new Date(ms);
    return isNaN(d.getTime()) ? "Unknown" : d.toLocaleString();
  };

  const currentList = useMemo(() => {
    if (activeTab === "cognitive") return cognitiveTests;
    if (activeTab === "lifestyle") return lifestyleTests;
    return speechTests;
  }, [activeTab, cognitiveTests, lifestyleTests, speechTests]);

  const tabStyle = (key) => ({
    padding: "10px 14px",
    borderRadius: "12px",
    border: "1px solid rgba(0,0,0,0.12)",
    background: activeTab === key ? "rgba(120, 81, 169, 0.15)" : "#fff",
    fontWeight: activeTab === key ? 800 : 600,
    cursor: "pointer",
  });

  if (loading) return <div style={{ padding: "2rem" }}>Loading...</div>;
  if (!patient) return <div style={{ padding: "2rem" }}>Patient not found.</div>;

  return (
    <div className="patient-profile-card">
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "20px" }}>
        <Link to="/risks" className="back-btn">← Risks</Link>
      </div>

      <h2 className="patient-profile-title" style={{ marginTop: 0 }}>
        {patient.firstName || "Unknown"} {patient.lastName || ""}
      </h2>

      <div style={{ display: "flex", gap: "10px", marginTop: "14px", marginBottom: "18px", flexWrap: "wrap" }}>
        <button style={tabStyle("cognitive")} onClick={() => setActiveTab("cognitive")}>Cognitive</button>
        <button style={tabStyle("lifestyle")} onClick={() => setActiveTab("lifestyle")}>Lifestyle</button>
        <button style={tabStyle("speech")} onClick={() => setActiveTab("speech")}>Speech</button>
      </div>

      <div className="home-card">
        <h3 style={{ marginTop: 0 }}>
          {activeTab === "cognitive"
            ? "Cognitive Tests"
            : activeTab === "lifestyle"
            ? "Lifestyle Tests"
            : "Speech Tests"}
        </h3>

        {currentList.length === 0 ? (
          <p style={{ margin: 0 }}>No tests found.</p>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
            {activeTab !== "speech" ? (
              currentList.map((t) => {
                const raw = Number(t.score ?? t.rawScore);
                const riskPercent = Number.isNaN(raw) ? null : Math.max(0, Math.min(100, 100 - raw * 2));

                return (
                  <div
                    key={t.id}
                    style={{
                      border: "1px solid rgba(0,0,0,0.08)",
                      borderRadius: "12px",
                      padding: "12px",
                      display: "flex",
                      justifyContent: "space-between",
                      gap: "12px",
                    }}
                  >
                    <div>
                      <div style={{ fontWeight: 800 }}>Score: {t.score ?? "N/A"}</div>
                      <div style={{ fontSize: "0.9rem", color: "#666" }}>
                        {formatDate(t.timestamp)}
                      </div>
                    </div>

                    <div style={{ textAlign: "right", fontSize: "0.9rem", color: "#666" }}>
                      <div>
                        <strong>Risk:</strong>{" "}
                        {riskPercent === null ? "N/A" : `${Math.round(riskPercent)}%`}
                      </div>
                    </div>
                  </div>
                );
              })
            ) : (
              currentList.map((t) => (
                <div
                  key={t.id}
                  style={{
                    border: "1px solid rgba(0,0,0,0.08)",
                    borderRadius: "12px",
                    padding: "12px",
                    display: "flex",
                    justifyContent: "space-between",
                    gap: "12px",
                  }}
                >
                  <div>
                    <div style={{ fontWeight: 800 }}>
                      File: {t.filename || "Unknown"}
                    </div>
                    <div style={{ fontSize: "0.9rem", color: "#666" }}>
                      {formatDate(t.created_at)}
                    </div>
                  </div>

                  <div style={{ textAlign: "right", fontSize: "0.9rem", color: "#666" }}>
                    <div>
                      <strong>Risk:</strong>{" "}
                      {t.risk_score === undefined || t.risk_score === null
                        ? "N/A"
                        : `${Math.round(Number(t.risk_score) * 100)}%`}
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        )}
      </div>
    </div>
  );
}
