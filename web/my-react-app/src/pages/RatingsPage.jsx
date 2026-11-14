import { useEffect, useMemo, useState } from "react";
import { collection, getDocs, orderBy, query } from "firebase/firestore";
import { db } from "../firebase"; 

function Stars({ value }) {
  const full = Math.floor(value);
  const half = value - full >= 0.5;
  const stars = Array.from({ length: 5 }, (_, i) => {
    const idx = i + 1;
    if (idx <= full) return "★";
    if (idx === full + 1 && half) return "☆"; 
    return "☆";
  });

  return (
    <span style={{ fontSize: 18, letterSpacing: 2 }}>
      {stars.join("")}
    </span>
  );
}

export default function RatingsPage() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  useEffect(() => {
    (async () => {
      try {
        const q = query(collection(db, "ratings"), orderBy("timestamp", "desc"));
        const snap = await getDocs(q);
        const rows = snap.docs.map((d) => {
          const data = d.data();
          return {
            id: d.id,
            rating: Number(data.rating ?? 0),
            review: String(data.review ?? "").trim(),
            timestamp: Number(data.timestamp ?? 0),
          };
        });
        setItems(rows);
      } catch (e) {
        setErr(e?.message || "Failed to load ratings");
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const avg = useMemo(() => {
    const valid = items.map((x) => x.rating).filter((n) => n >= 1 && n <= 5);
    if (!valid.length) return 0;
    const sum = valid.reduce((a, b) => a + b, 0);
    return sum / valid.length;
  }, [items]);

  const total = items.filter((x) => x.rating >= 1 && x.rating <= 5).length;

  if (loading) return <div style={{ padding: 16 }}>Loading ratings…</div>;
  if (err) return <div style={{ padding: 16, color: "crimson" }}>{err}</div>;

  return (
    <div style={{ padding: 24, maxWidth: 900, margin: "0 auto" }}>
      <h2 style={{ marginBottom: 8 }}>Ratings</h2>

      <div
        style={{
          display: "flex",
          gap: 12,
          alignItems: "center",
          padding: 16,
          border: "1px solid #e5e5e5",
          borderRadius: 12,
          marginBottom: 20,
        }}
      >
        <div style={{ fontSize: 36, fontWeight: 700 }}>
          {avg.toFixed(1)}
        </div>
        <div>
          <Stars value={avg} />
          <div style={{ color: "#666", marginTop: 4 }}>
            {total} review{total === 1 ? "" : "s"} • out of 5
          </div>
        </div>
      </div>

      <h3 style={{ marginBottom: 10 }}>Comments</h3>

      {items
        .filter((x) => x.review.length > 0)
        .map((x) => (
          <div
            key={x.id}
            style={{
              padding: 16,
              border: "1px solid #eee",
              borderRadius: 12,
              marginBottom: 12,
              background: "white",
            }}
          >
            <div style={{ display: "flex", justifyContent: "space-between" }}>
              <div style={{ fontWeight: 600 }}>
                {x.rating}/5
              </div>
              <div style={{ color: "#888", fontSize: 12 }}>
                {x.timestamp ? new Date(x.timestamp).toLocaleString() : ""}
              </div>
            </div>
            <div style={{ marginTop: 8, whiteSpace: "pre-wrap" }}>
              {x.review}
            </div>
          </div>
        ))}

      {items.filter((x) => x.review.length > 0).length === 0 && (
        <div style={{ color: "#666" }}>No comments yet.</div>
      )}
    </div>
  );
}
