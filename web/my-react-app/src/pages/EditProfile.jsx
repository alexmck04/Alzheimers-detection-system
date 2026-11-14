import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { auth, db, storage } from "../firebase";
import { doc, getDoc, updateDoc, setDoc } from "firebase/firestore";
import { ref, uploadBytes, getDownloadURL, deleteObject } from "firebase/storage";

export default function EditProfilePage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    firstName: "",
    lastName: "",
    email: "",
    phone: "",
    office: "",
    dob: "",
    photoURL: "",
  });
  const [file, setFile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  const [errors, setErrors] = useState({
    firstName: "",
    lastName: "",
    phone: "",
  });

  const user = auth.currentUser;

  const isValidName = (value) => {
    const v = (value ?? "").trim();
    if (!v) return true;
    return !/^\d+$/.test(v);
  };

  const digitsOnly = (value) => (value ?? "").replace(/\D/g, "");

  const isValidPhone = (value) => {
    const digits = digitsOnly(value);
    if (!digits) return true;
    return digits.length <= 10;
  };

  const validateAll = (nextForm) => {
    const nextErrors = { firstName: "", lastName: "", phone: "" };

    if (!isValidName(nextForm.firstName)) {
      nextErrors.firstName = "First name cannot be numbers only.";
    }

    if (!isValidName(nextForm.lastName)) {
      nextErrors.lastName = "Last name cannot be numbers only.";
    }

    if (!isValidPhone(nextForm.phone)) {
      nextErrors.phone = "Phone number cannot be more than 10 digits.";
    }

    setErrors(nextErrors);
    return !Object.values(nextErrors).some(Boolean);
  };

  // Load profile data
  useEffect(() => {
    const fetchProfile = async () => {
      if (!user) return;
      try {
        const docRef = doc(db, "doctors", user.uid);
        const snap = await getDoc(docRef);

        if (snap.exists()) {
          const data = snap.data();
          const next = { ...form, ...data };
          setForm(next);
          validateAll(next);
        } else {
          await setDoc(docRef, {
            email: user.email,
            firstName: "",
            lastName: "",
            phone: "",
            office: "",
            dob: "",
            photoURL: "",
          });
        }
      } catch (err) {
        console.error("Error loading profile:", err);
      }
    };

    fetchProfile();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user]);

  // DELETE PROFILE IMAGE
  const handleDeletePhoto = async () => {
    if (!user || !form.photoURL) return;

    try {
      setLoading(true);

      // Remove from Firebase Storage
      const imageRef = ref(storage, `doctors/${user.uid}.jpg`);
      await deleteObject(imageRef).catch(() => {});

      // Remove from Firestore
      await updateDoc(doc(db, "doctors", user.uid), { photoURL: "" });

      // Update UI
      const next = { ...form, photoURL: "" };
      setForm(next);
      validateAll(next);
      setMessage("Profile photo deleted.");
    } catch (err) {
      console.error(err);
      setMessage("Error deleting photo.");
    } finally {
      setLoading(false);
    }
  };

  // Save updates
  const handleSave = async (e) => {
    e.preventDefault();
    if (!user) return;

    setMessage("");
    const ok = validateAll(form);
    if (!ok) {
      setMessage("Please fix the highlighted fields.");
      return;
    }

    try {
      setLoading(true);
      let imageURL = form.photoURL;

      // Upload new profile photo
      if (file) {
        const imageRef = ref(storage, `doctors/${user.uid}.jpg`);
        await uploadBytes(imageRef, file);
        imageURL = await getDownloadURL(imageRef);
      }

      const cleanPhone = digitsOnly(form.phone);

      const cleanData = Object.fromEntries(
        Object.entries({ ...form, phone: cleanPhone, photoURL: imageURL }).filter(
          ([, v]) => v !== undefined
        )
      );

      await updateDoc(doc(db, "doctors", user.uid), cleanData);

      setMessage("Profile updated successfully!");
      setTimeout(() => navigate("/home"), 1000);
    } catch (err) {
      console.error(err);
      setMessage("Error updating profile.");
    } finally {
      setLoading(false);
    }
  };

  if (!user) return <p className="auth-message">Please log in first.</p>;

  return (
    <div className="dashboard">
      <div className="profile-card fade-in">
        <h2 className="profile-title">Edit Profile</h2>

        <form onSubmit={handleSave} className="profile-form">
          <div className="profile-row">
            <div className="profile-field">
              <label>First Name</label>
              <input
                type="text"
                value={form.firstName}
                onChange={(e) => {
                  const next = { ...form, firstName: e.target.value };
                  setForm(next);
                  validateAll(next);
                }}
              />
              {errors.firstName && (
                <p className="auth-message" style={{ color: "crimson", marginTop: 6 }}>
                  {errors.firstName}
                </p>
              )}
            </div>

            <div className="profile-field">
              <label>Last Name</label>
              <input
                type="text"
                value={form.lastName}
                onChange={(e) => {
                  const next = { ...form, lastName: e.target.value };
                  setForm(next);
                  validateAll(next);
                }}
              />
              {errors.lastName && (
                <p className="auth-message" style={{ color: "crimson", marginTop: 6 }}>
                  {errors.lastName}
                </p>
              )}
            </div>
          </div>

          <div className="profile-row">
            <div className="profile-field">
              <label>Email</label>
              <input type="email" value={form.email} disabled />
            </div>

            <div className="profile-field">
              <label>Phone</label>
              <input
                type="text"
                value={form.phone}
                onChange={(e) => {
                  const next = { ...form, phone: e.target.value };
                  setForm(next);
                  validateAll(next);
                }}
              />
              {errors.phone && (
                <p className="auth-message" style={{ color: "crimson", marginTop: 6 }}>
                  {errors.phone}
                </p>
              )}
            </div>
          </div>

          <div className="profile-row">
            <div className="profile-field">
              <label>Office</label>
              <input
                type="text"
                value={form.office}
                onChange={(e) => {
                  const next = { ...form, office: e.target.value };
                  setForm(next);
                  validateAll(next);
                }}
              />
            </div>

            <div className="profile-field">
              <label>Date of Birth</label>
              <input
                type="date"
                value={form.dob}
                onChange={(e) => {
                  const next = { ...form, dob: e.target.value };
                  setForm(next);
                  validateAll(next);
                }}
              />
            </div>
          </div>

          {/* Profile Photo Upload */}
          <div className="profile-field">
            <label>Profile Photo</label>
            <input
              type="file"
              accept="image/*"
              onChange={(e) => setFile(e.target.files[0])}
            />

            {form.photoURL && (
              <div className="profile-photo-wrapper">
                <img
                  src={form.photoURL}
                  alt="Profile"
                  className="profile-preview"
                />

                <button
                  type="button"
                  className="delete-photo-btn"
                  onClick={handleDeletePhoto}
                  disabled={loading}
                >
                  Remove Photo
                </button>
              </div>
            )}
          </div>

          <button type="submit" className="profile-save-btn" disabled={loading}>
            {loading ? "Saving..." : "Save Changes"}
          </button>
        </form>

        {message && <p className="auth-message">{message}</p>}
      </div>
    </div>
  );
}

