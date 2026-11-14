import { useState, useEffect } from "react";
import { db } from "../firebase";
import {
  collection,
  addDoc,
  getDocs,
  deleteDoc,
  updateDoc,
  doc,
  query,
  where,
} from "firebase/firestore";
import emailjs from "@emailjs/browser";
import "../styles.css";

export default function AdminDashboard() {
  const [doctors, setDoctors] = useState([]);
  const [patients, setPatients] = useState([]);

  const [newDrName, setNewDrName] = useState("");
  const [newDrEmail, setNewDrEmail] = useState("");
  const [message, setMessage] = useState("");

  useEffect(() => {
    fetchDoctors();
    fetchPatients();
  }, []);

  const fetchDoctors = async () => {
    const q = query(collection(db, "doctors"), where("role", "==", "doctor"));
    const snap = await getDocs(q);
    setDoctors(snap.docs.map((d) => ({ id: d.id, ...d.data() })));
  };

  const fetchPatients = async () => {
    const snap = await getDocs(collection(db, "users"));
    setPatients(snap.docs.map((d) => ({ id: d.id, ...d.data() })));
  };

  const EMAILJS_SERVICE_ID = import.meta.env.VITE_EMAILJS_SERVICE_ID;
  const EMAILJS_PUBLIC_KEY = import.meta.env.VITE_EMAILJS_PUBLIC_KEY;
  const TEMPLATE_INVITE_DOCTOR =
    import.meta.env.VITE_EMAILJS_TEMPLATE_INVITE_DOCTOR;

  const sendInviteEmail = async ({ email, doctor_name }) => {
    if (!EMAILJS_SERVICE_ID || !EMAILJS_PUBLIC_KEY || !TEMPLATE_INVITE_DOCTOR) {
      console.warn("EmailJS env vars missing. Email skipped.");
      return;
    }

    try {
      await emailjs.send(
        EMAILJS_SERVICE_ID,
        TEMPLATE_INVITE_DOCTOR,
        {
          email,
          doctor_name,
        },
        EMAILJS_PUBLIC_KEY
      );
    } catch (err) {
      console.error("Email invite failed:", err);
    }
  };

  const handleCreateDoctor = async (e) => {
    e.preventDefault();

    try {
      // Save doctor invite to Firestore
      await addDoc(collection(db, "doctors"), {
        firstName: newDrName,
        email: newDrEmail,
        role: "doctor",
        isPending: true,
        createdAt: new Date(),
      });

      // Send invite email via EmailJS
      await sendInviteEmail({
        email: newDrEmail,
        doctor_name: newDrName,
      });

      setMessage(`Invited Dr. ${newDrName}`);
      setNewDrName("");
      setNewDrEmail("");
      fetchDoctors();
      setTimeout(() => setMessage(""), 3000);
    } catch (err) {
      console.error(err);
      setMessage("Error adding doctor.");
    }
  };

  const handleAssignDoctor = async (patientId, doctorId) => {
    try {
      const patientRef = doc(db, "users", patientId);
      await updateDoc(patientRef, {
        assignedDoctorId: doctorId,
      });
      fetchPatients();
    } catch (err) {
      console.error("Error assigning doctor:", err);
      alert("Failed to assign doctor");
    }
  };

  const handleDeleteDoctor = async (id) => {
    if (!window.confirm("Are you sure? This removes their access.")) return;
    await deleteDoc(doc(db, "doctors", id));
    fetchDoctors();
  };

  const handleDeletePatient = async (id) => {
    if (!window.confirm("Delete this patient record?")) return;
    await deleteDoc(doc(db, "users", id));
    fetchPatients();
  };

  return (
    <div className="dashboard">
      <h1 className="page-title">Admin Dashboard</h1>

      {message && <p className="admin-success-msg">{message}</p>}

      <div className="admin-grid">
        {/* LEFT: MANAGE DOCTORS */}
        <div className="home-card">
          <h2 className="home-title">Manage Doctors</h2>

          <form onSubmit={handleCreateDoctor} className="admin-form">
            <input
              className="admin-input"
              placeholder="First Name"
              value={newDrName}
              onChange={(e) => setNewDrName(e.target.value)}
              required
            />
            <input
              className="admin-input"
              placeholder="Email"
              type="email"
              value={newDrEmail}
              onChange={(e) => setNewDrEmail(e.target.value)}
              required
            />
            <button type="submit" className="admin-invite-btn">
              Invite
            </button>
          </form>

          <ul className="admin-list">
            {doctors.map((docObj) => (
              <li key={docObj.id} className="admin-list-item">
                <div className="admin-user-info">
                  <span className="admin-user-name">
                    {docObj.firstName} {docObj.lastName}
                    {docObj.isPending && (
                      <span className="status-pending">Pending</span>
                    )}
                  </span>
                  <span className="admin-user-email">{docObj.email}</span>
                </div>

                <button
                  onClick={() => handleDeleteDoctor(docObj.id)}
                  className="admin-delete-btn"
                >
                  Remove
                </button>
              </li>
            ))}
          </ul>
        </div>

        <div className="home-card">
          <h2 className="home-title">Assign Patients</h2>

          <ul className="admin-list">
            {patients.map((p) => (
              <li
                key={p.id}
                className="admin-list-item"
                style={{
                  flexDirection: "column",
                  alignItems: "flex-start",
                  gap: "10px",
                }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    width: "100%",
                  }}
                >
                  <div className="admin-user-info">
                    <span className="admin-user-name">
                      {p.firstName} {p.lastName}
                    </span>
                    <span className="admin-user-email">{p.email}</span>
                  </div>

                  <button
                    onClick={() => handleDeletePatient(p.id)}
                    className="admin-delete-btn"
                  >
                    Remove
                  </button>
                </div>

                <div style={{ width: "100%" }}>
                  <select
                    className="admin-input"
                    style={{ width: "100%", padding: "5px", fontSize: "0.9rem" }}
                    value={p.assignedDoctorId || ""}
                    onChange={(e) => handleAssignDoctor(p.id, e.target.value)}
                  >
                    <option value="">-- Unassigned --</option>
                    {doctors.map((d) => (
                      <option key={d.id} value={d.id}>
                        Dr. {d.firstName} {d.lastName}
                      </option>
                    ))}
                  </select>
                </div>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  );
}
