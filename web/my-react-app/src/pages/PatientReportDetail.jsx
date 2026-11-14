import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { db } from "../firebase";
import { doc, getDoc, getDocs, collection, updateDoc } from "firebase/firestore";
import "../styles.css";

export default function PatientReportDetail() {
  const { id } = useParams();

  const [patient, setPatient] = useState(null);
  const [tests, setTests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [reportMessage, setReportMessage] = useState("");

  useEffect(() => {
    loadEverything();
  }, [id]);

  const toMillis = (v) => {
    if (!v) return null;
    if (typeof v === "number") return v;
    if (v instanceof Date) return v.getTime();
    if (typeof v?.toMillis === "function") return v.toMillis();
    if (typeof v?.seconds === "number")
      return v.seconds * 1000 + Math.floor((v.nanoseconds || 0) / 1e6);
    return null;
  };

  const clamp = (n, min, max) => Math.max(min, Math.min(max, n));

  const loadEverything = async () => {
    setLoading(true);
    setReportMessage("");
    try {
      const userSnap = await getDoc(doc(db, "users", id));
      if (userSnap.exists()) setPatient({ id: userSnap.id, ...userSnap.data() });

      const allTests = await loadAllTestsForUser(id);
      setTests(allTests);
    } catch (e) {
      console.error("Failed loading report detail:", e);
    } finally {
      setLoading(false);
    }
  };

  const loadAllTestsForUser = async (userId) => {
    const cogSnap = await getDocs(collection(db, "tests", userId, "cognitiveTests"));
    const lifeSnap = await getDocs(collection(db, "tests", userId, "lifestyleTests"));
    const speechSnap = await getDocs(collection(db, "tests", userId, "audioTests"));

    const cog = cogSnap.docs.map((d) => {
      const data = d.data();
      const rawScore = Number(data.score);
      const riskPercent = Number.isNaN(rawScore) ? null : clamp(100 - rawScore * 2, 0, 100);
      return {
        id: d.id,
        type: "Cognitive",
        rawScore: Number.isNaN(rawScore) ? null : rawScore,
        timestamp: data.timestamp,
        riskPercent,
      };
    });

    const life = lifeSnap.docs.map((d) => {
      const data = d.data();
      const rawScore = Number(data.rawScore ?? data.score);
      const riskPercent = Number.isNaN(rawScore) ? null : clamp(100 - rawScore * 2, 0, 100);
      return {
        id: d.id,
        type: "Lifestyle",
        rawScore: Number.isNaN(rawScore) ? null : rawScore,
        timestamp: data.timestamp,
        riskPercent,
      };
    });

    const speech = speechSnap.docs.map((d) => {
      const data = d.data();
      const r = Number(data.risk_score);
      const riskPercent = Number.isNaN(r) ? null : clamp(r * 100, 0, 100);
      return {
        id: d.id,
        type: "Speech",
        filename: data.filename,
        created_at: data.created_at,
        riskPercent,
      };
    });

    const list = [...cog, ...life, ...speech];

    list.sort((a, b) => {
      const ta = toMillis(a.timestamp ?? a.created_at) || 0;
      const tb = toMillis(b.timestamp ?? b.created_at) || 0;
      return tb - ta;
    });

    return list;
  };

  const formatDate = (v) => {
    const ms = toMillis(v);
    if (!ms) return "Unknown";
    const d = new Date(ms);
    return isNaN(d.getTime()) ? "Unknown" : d.toLocaleString();
  };

  const computed = useMemo(() => {
    const valid = tests.filter((t) => t.riskPercent !== null && t.riskPercent !== undefined);
    if (valid.length === 0) {
      return { averageRisk: null, highestRisk: null, lowestRisk: null, riskLevel: "No Data" };
    }

    const risks = valid.map((t) => t.riskPercent);
    const avg = risks.reduce((a, b) => a + b, 0) / risks.length;
    const highest = Math.max(...risks);
    const lowest = Math.min(...risks);
    const level = avg >= 80 ? "High" : avg >= 40 ? "Moderate" : "Low";

    return { averageRisk: avg, highestRisk: highest, lowestRisk: lowest, riskLevel: level };
  }, [tests]);

  const generateGenericText = (level) => {
    if (level === "High") {
      return "Your average risk level is high. Please speak with a healthcare professional for further assessment and support.";
    }
    if (level === "Moderate") {
      return "Your average risk level is moderate. Consider discussing lifestyle and cognitive health with a professional and monitor changes over time.";
    }
    if (level === "Low") {
      return "Your average risk level is low. Keep maintaining healthy habits and continue monitoring your cognitive wellbeing.";
    }
    return "No risk data available yet. Complete questionnaires to generate a report.";
  };

  const handleGenerateReport = async () => {
    setGenerating(true);
    setReportMessage("");

    try {
      const { averageRisk, highestRisk, lowestRisk, riskLevel } = computed;

      if (averageRisk === null) {
        setReportMessage("No test data found to generate a report.");
        return;
      }

      const report = {
        averageRiskPercent: Math.round(averageRisk),
        highestRiskPercent: Math.round(highestRisk),
        lowestRiskPercent: Math.round(lowestRisk),
        riskLevel,
        text: generateGenericText(riskLevel),
        generatedAt: Date.now(),
        basedOnTestCount: tests.filter((t) => t.riskPercent !== null).length,
      };

      await updateDoc(doc(db, "users", id), {
        dementiaScore: report.averageRiskPercent,
        latestReport: report,
        updatedAt: Date.now(),
      });

      setReportMessage("Report generated and saved to patient profile.");
      await loadEverything();
    } catch (e) {
      console.error("Generate report failed:", e);
      setReportMessage("Failed to generate report.");
    } finally {
      setGenerating(false);
    }
  };

  const handleDownloadPDF = () => {
    window.print();
  };

  if (loading) return <div style={{ padding: "2rem" }}>Loading...</div>;
  if (!patient) return <div style={{ padding: "2rem" }}>Patient not found.</div>;

  const fullName = `${patient.firstName || "Unknown"} ${patient.lastName || ""}`.trim();

  return (
    <div className="patient-profile-card">
      <div
        className="no-print"
        style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "20px" }}
      >
        <Link to="/reports" className="back-btn">← Reports</Link>

        <div style={{ display: "flex", gap: "10px", alignItems: "center" }}>
          <button className="home-edit-btn" onClick={handleDownloadPDF} style={{ marginTop: 0 }}>
            Download PDF
          </button>

          <button
            className="home-edit-btn"
            onClick={handleGenerateReport}
            disabled={generating}
            style={{ marginTop: 0 }}
          >
            {generating ? "Generating..." : "Generate Report"}
          </button>
        </div>
      </div>

      <div className="print-area">
        <div className="report-header-min">
          <div className="report-name">{fullName}</div>
          <div className="report-conf-min">CONFIDENTIAL — for patient view only</div>
        </div>

        <div className="report-meta-min">
          <div><strong>Generated:</strong> {new Date().toLocaleString()}</div>
          <div><strong>Patient ID:</strong> {patient.id}</div>
        </div>

        {reportMessage && (
          <div className="home-card" style={{ marginTop: "12px" }}>
            <p style={{ margin: 0 }}>{reportMessage}</p>
          </div>
        )}

        <div className="home-card" style={{ marginTop: "16px" }}>
          <h3 style={{ marginTop: 0 }}>Summary</h3>

          <p style={{ margin: "6px 0" }}>
            <strong>Average Risk:</strong>{" "}
            {computed.averageRisk === null ? "No data" : `${Math.round(computed.averageRisk)}%`}
          </p>

          <p style={{ margin: "6px 0" }}>
            <strong>Highest Risk (biggest concern):</strong>{" "}
            {computed.highestRisk === null ? "No data" : `${Math.round(computed.highestRisk)}%`}
          </p>

          <p style={{ margin: "6px 0" }}>
            <strong>Lowest Risk (smallest concern):</strong>{" "}
            {computed.lowestRisk === null ? "No data" : `${Math.round(computed.lowestRisk)}%`}
          </p>

          <p style={{ margin: "6px 0" }}>
            <strong>Risk Level:</strong> {computed.riskLevel}
          </p>

          <p style={{ marginTop: "10px" }}>{generateGenericText(computed.riskLevel)}</p>
        </div>

        <div className="home-card" style={{ marginTop: "16px" }}>
          <h3 style={{ marginTop: 0 }}>Completed Tests</h3>

          {tests.length === 0 ? (
            <p style={{ margin: 0 }}>No tests found.</p>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
              {tests.map((t) => (
                <div
                  key={`${t.type}-${t.id}`}
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
                    <div style={{ fontWeight: 700 }}>
                      {t.type} {t.type === "Speech" ? "Prediction" : "Test"}
                    </div>

                    {t.type === "Speech" && t.filename ? (
                      <div style={{ fontSize: "0.9rem", color: "#666" }}>{t.filename}</div>
                    ) : null}

                    <div style={{ fontSize: "0.9rem", color: "#666" }}>
                      {formatDate(t.timestamp ?? t.created_at)}
                    </div>
                  </div>

                  <div style={{ textAlign: "right" }}>
                    {t.rawScore !== undefined && t.rawScore !== null ? (
                      <div><strong>Score:</strong> {t.rawScore}</div>
                    ) : null}

                    <div>
                      <strong>Risk:</strong>{" "}
                      {t.riskPercent === null || t.riskPercent === undefined ? "N/A" : `${Math.round(t.riskPercent)}%`}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {patient.latestReport && (
          <div className="home-card" style={{ marginTop: "16px" }}>
            <h3 style={{ marginTop: 0 }}>Latest Saved Report</h3>

            <p style={{ margin: "6px 0" }}>
              <strong>Generated:</strong> {formatDate(patient.latestReport.generatedAt)}
            </p>

            <p style={{ margin: "6px 0" }}>
              <strong>Average Risk:</strong> {patient.latestReport.averageRiskPercent}%
            </p>

            <p style={{ margin: "6px 0" }}>
              <strong>Risk Level:</strong> {patient.latestReport.riskLevel}
            </p>

            <p style={{ marginTop: "10px" }}>{patient.latestReport.text}</p>
          </div>
        )}
      </div>
    </div>
  );
}
