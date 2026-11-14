import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { auth, db } from "../firebase";
import { doc, getDoc } from "firebase/firestore";
import { onAuthStateChanged } from "firebase/auth";

export default function HomePage() {
  const [doctor, setDoctor] = useState(null);
  const [message, setMessage] = useState("");
  const [time, setTime] = useState("");

  const navigate = useNavigate();

  // CLOCK UPDATES
  useEffect(() => {
    const updateClock = () => {
      const now = new Date();
      const formatted = now.toLocaleTimeString([], {
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
      });
      setTime(formatted);
    };
    updateClock();
    const interval = setInterval(updateClock, 1000);
    return () => clearInterval(interval);
  }, []);

  // LOAD DOCTOR
  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (user) => {
      if (user) {
        try {
          const docRef = doc(db, "doctors", user.uid);
          const docSnap = await getDoc(docRef);

          if (docSnap.exists()) {
            setDoctor(docSnap.data());
          } else {
            setMessage("Doctor profile not found.");
          }
        } catch (error) {
          console.error(error);
          setMessage("Error loading doctor data.");
        }
      } else {
        navigate("/login");
      }
    });

    return () => unsubscribe();
  }, [navigate]);

  if (message) return <p className="auth-message">{message}</p>;
  if (!doctor) return null;

  const fullName =
    (doctor.firstName || doctor.lastName)
      ? `${doctor.firstName || ""} ${doctor.lastName || ""}`.trim()
      : "Doctor";


  return (
    <div className="dashboard">
      <div className="home-card fade-in">

        {/* TOP ROW: TITLE + CLOCK */}
        <div className="home-header">
          <h2 className="home-title">Welcome, Dr. {fullName}</h2>

          <div className="home-clock">
            <p>{time}</p>
          </div>
        </div>

        <div className="home-profile">
          <img
            src={
              doctor.photoURL ||
              "https://cdn-icons-png.flaticon.com/512/387/387561.png"
            }
            alt="Doctor profile"
            className="home-avatar"
          />

          <div className="home-info">
            <p><strong>Email:</strong> {doctor.email}</p>
            <p><strong>Office:</strong> {doctor.office || "Not set yet"}</p>
            <p><strong>Telephone:</strong> {doctor.phone || "Not set yet"}</p>
            <p><strong>Date of Birth:</strong> {doctor.dob || "Not set yet"}</p>
          </div>
        </div>

        <button
          className="home-edit-btn"
          onClick={() => navigate("/edit-profile")}
        >
          Edit Profile
        </button>
      </div>
    </div>
  );
}
