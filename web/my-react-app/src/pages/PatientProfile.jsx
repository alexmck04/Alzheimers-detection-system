import { useParams, Link, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import { doc, getDoc, updateDoc, deleteDoc } from "firebase/firestore";
import { db } from "../firebase";
import "../styles.css";

export default function PatientProfile() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [patient, setPatient] = useState(null);
  const [loading, setLoading] = useState(true);

  // Edit State
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({});

  useEffect(() => {
    loadPatient();
  }, [id]);

  const loadPatient = async () => {
    try {
      const snap = await getDoc(doc(db, "users", id));
      if (snap.exists()) {
        const data = snap.data();
        setPatient(data);
        setFormData(data); // Pre-fill form
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  // Handle Typing in Input Boxes
  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  // Save Changes to Firebase
  const handleSave = async () => {
    try {
      const docRef = doc(db, "users", id);

      const { assignedDoctorId, dementiaScore, createdAt, ...rest } = formData;

      await updateDoc(docRef, {
        ...rest,
        updatedAt: Date.now(),
      });

      const nextPatient = { ...patient, ...rest, updatedAt: Date.now() };
      setPatient(nextPatient);
      setFormData(nextPatient);
      setIsEditing(false);
      alert("Profile updated!");
    } catch (error) {
      console.error("Error updating:", error);
      alert("Failed to save.");
    }
  };

  // Delete Patient from Firebase
  const handleDelete = async () => {
    if (!window.confirm("Are you sure you want to delete this patient? This cannot be undone.")) return;
    try {
      await deleteDoc(doc(db, "users", id));
      navigate("/patients"); // Send doctor back to list
    } catch (error) {
      console.error("Error deleting:", error);
      alert("Failed to delete.");
    }
  };

  if (loading) return <p style={{padding:"2rem"}}>Loading...</p>;
  if (!patient) return <p style={{padding:"2rem"}}>Patient not found.</p>;

  return (
    <div className="patient-profile-card">

      
      <div style={{display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "20px"}}>
        <Link to="/patients" className="back-btn">
          ← Patient List
        </Link>
        
        {!isEditing && (
          <button 
            className="home-edit-btn" 
            onClick={() => setIsEditing(true)}
            style={{marginTop: 0, padding: "0.5rem 1rem", fontSize: "1rem"}}
          >
            Edit Profile
          </button>
        )}
      </div>

      
      {isEditing ? (
        <div style={{display: "flex", gap: "10px", marginBottom: "20px"}}>
          <input 
            className="admin-input" 
            name="firstName" 
            value={formData.firstName || ""} 
            onChange={handleChange} 
            placeholder="First Name"
          />
          <input 
            className="admin-input" 
            name="lastName" 
            value={formData.lastName || ""} 
            onChange={handleChange} 
            placeholder="Last Name"
          />
        </div>
      ) : (
        <h2 className="patient-profile-title">
          {patient.firstName} {patient.lastName}
        </h2>
      )}

      <div className="patient-profile-section">

       
        <div className="patient-info-block">
          <h3 className="patient-section-header">Personal Info</h3>
          
          {isEditing ? (
            <div style={{display:"flex", flexDirection:"column", gap:"10px"}}>
                <label>Email:</label>
                <input className="admin-input" name="email" value={formData.email || ""} onChange={handleChange} />
                
                <label>Address:</label>
                <input className="admin-input" name="address" value={formData.address || ""} onChange={handleChange} />
                
                <label>Date of Birth:</label>
                <input className="admin-input" name="dob" value={formData.dob || ""} onChange={handleChange} />

                <label>Phone:</label>
                <input className="admin-input" name="phone" value={formData.phone || ""} onChange={handleChange} />
            </div>
          ) : (
            <>
                <p><strong>Email:</strong> {patient.email}</p>
                <p><strong>Address:</strong> {patient.address || "Not recorded"}</p>
                <p><strong>Date of Birth:</strong> {patient.dob || "Not recorded"}</p>
                <p><strong>Phone:</strong> {patient.phone || "Not recorded"}</p>
            </>
          )}
        </div>

        
        <div className="patient-info-block">
          <h3 className="patient-section-header">Emergency Contact</h3>
          
          {isEditing ? (
            <div style={{display:"flex", flexDirection:"column", gap:"10px"}}>
                <label>Contact Name:</label>
                <input 
                    className="admin-input" 
                    name="emergencyContactName" 
                    value={formData.emergencyContactName || ""} 
                    onChange={handleChange} 
                />
                
                <label>Contact Number:</label>
                <input 
                    className="admin-input" 
                    name="emergencyContactNumber" 
                    value={formData.emergencyContactNumber || ""} 
                    onChange={handleChange} 
                />
            </div>
          ) : (
            <>
                <p><strong>Name:</strong> {patient.emergencyContactName || "Not recorded"}</p>
                <p><strong>Phone:</strong> {patient.emergencyContactNumber || "Not recorded"}</p>
            </>
          )}
        </div>

      </div>

      {isEditing && (
        <div style={{marginTop: "30px", borderTop: "1px solid #ccc", paddingTop: "20px", display: "flex", gap: "15px"}}>
            
            <button 
                className="home-edit-btn" 
                onClick={handleSave}
                style={{marginTop: 0}}
            >
                Save Changes
            </button>

            <button 
                className="home-edit-btn" 
                onClick={() => {setIsEditing(false); setFormData(patient);}}
                style={{marginTop: 0, background: "#777"}}
            >
                Cancel
            </button>

            <button 
                className="home-edit-btn" 
                onClick={handleDelete}
                style={{marginTop: 0, marginLeft: "auto", background: "#d32f2f"}}
            >
                Delete Patient
            </button>
        </div>
      )}

    </div>
  );
}
