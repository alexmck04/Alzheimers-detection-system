import { useEffect, useMemo, useState } from "react";
import {
  collection,
  onSnapshot,
  orderBy,
  query,
  updateDoc,
  doc,
} from "firebase/firestore";
import { db } from "../firebase";

export default function SupportMessagesPage() {
  const [items, setItems] = useState([]);
  const [statusFilter, setStatusFilter] = useState("all");
  const [search, setSearch] = useState("");
  const [message, setMessage] = useState("");

  useEffect(() => {
    // listen to support requests
    const q = query(
      collection(db, "supportRequests"),
      orderBy("timestamp", "desc")
    );

    const unsub = onSnapshot(
      q,
      (snap) => {
        const rows = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
        setItems(rows);
        setMessage(rows.length ? "" : "No support messages yet.");
      },
      (err) => {
        console.error(err);
        setMessage("Failed to load support messages.");
      }
    );

    return () => unsub();
  }, []);

  const filtered = useMemo(() => {
    const s = search.trim().toLowerCase();

    return items
      .filter((x) => {
        if (statusFilter === "all") return true;
        return (x.status || "").toLowerCase() === statusFilter;
      })
      .filter((x) => {
        if (!s) return true;
        const subject = (x.subject || "").toLowerCase();
        const body = (x.message || "").toLowerCase();
        return subject.includes(s) || body.includes(s);
      });
  }, [items, statusFilter, search]);

  const setStatus = async (id, nextStatus) => {
    try {
      await updateDoc(doc(db, "supportRequests", id), {
        status: nextStatus,
      });
    } catch (e) {
      console.error(e);
      alert("Failed to update status.");
    }
  };

  const formatDate = (ts) => {
    if (!ts) return "";
    const n = typeof ts === "number" ? ts : Number(ts);
    if (!Number.isFinite(n)) return "";
    return new Date(n).toLocaleString();
  };

  return (
    <div className="dashboard fade-in">
      <h2 className="page-title">Support Messages (Admin)</h2>

      
      <div style={{ display: "flex", gap: 12, flexWrap: "wrap", marginBottom: 16 }}>
        <input
          className="input"
          placeholder="Search subject/message…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          style={{
            padding: 10,
            borderRadius: 10,
            border: "1px solid rgba(255,255,255,0.2)",
            width: 280,
            background: "rgba(0,0,0,0.2)",
            color: "white",
          }}
        />

        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          style={{
            padding: 10,
            borderRadius: 10,
            border: "1px solid rgba(255,255,255,0.2)",
            background: "rgba(0,0,0,0.2)",
            color: "white",
          }}
        >
          <option value="all">All</option>
          <option value="new">New</option>
          <option value="resolved">Resolved</option>
        </select>

        <div style={{ opacity: 0.85, alignSelf: "center" }}>
          Showing <b>{filtered.length}</b> of <b>{items.length}</b>
        </div>
      </div>

      
      {message && filtered.length === 0 && (
        <p className="auth-message">{message}</p>
      )}

       
      <div className="news-grid">
        {filtered.map((x) => {
          const status = (x.status || "new").toLowerCase();
          const isResolved = status === "resolved";

          return (
            <div key={x.id} className="news-card">
              <h3 className="news-headline" style={{ marginBottom: 6 }}>
                {x.subject || "No subject"}
              </h3>

              <p className="news-description" style={{ whiteSpace: "pre-wrap" }}>
                {x.message || "No message"}
              </p>

              <p className="news-source" style={{ marginTop: 10 }}>
                Status:{" "}
                <b style={{ color: isResolved ? "#8ff0a4" : "#ffd37a" }}>
                  {status}
                </b>{" "}
                — {formatDate(x.timestamp)}
              </p>

              <div style={{ display: "flex", gap: 10, marginTop: 12 }}>
                {!isResolved ? (
                  <button
                    className="sidebar-signout"
                    onClick={() => setStatus(x.id, "resolved")}
                    style={{
                      padding: "10px 14px",
                      borderRadius: 12,
                      cursor: "pointer",
                    }}
                  >
                    Mark Resolved
                  </button>
                ) : (
                  <button
                    className="sidebar-signout"
                    onClick={() => setStatus(x.id, "new")}
                    style={{
                      padding: "10px 14px",
                      borderRadius: 12,
                      cursor: "pointer",
                    }}
                  >
                    Mark New
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
