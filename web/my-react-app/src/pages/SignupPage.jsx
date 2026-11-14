import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { auth, db, googleProvider } from "../firebase";
import { createUserWithEmailAndPassword, signInWithPopup } from "firebase/auth";
import { doc, setDoc, getDocs, collection, query, where, deleteDoc } from "firebase/firestore";
import "../auth.css";

export default function SignupPage() {
  const [form, setForm] = useState({
    firstName: "",
    lastName: "",
    email: "",
    password: "",
  });

  const [message, setMessage] = useState("");
  const navigate = useNavigate();

  const handleChange = (e) =>
    setForm({ ...form, [e.target.name]: e.target.value });

  // CHECK INVITATION 
  // Checks if the email is in the doctors collection
  const checkInvitation = async (emailToCheck) => {
    const doctorsRef = collection(db, "doctors");
    // Look for a pending invite with this email
    const q = query(doctorsRef, where("email", "==", emailToCheck), where("isPending", "==", true));
    const querySnapshot = await getDocs(q);
    
    if (querySnapshot.empty) return null; // No invite found
    
    // Return the Invite Data and the ID of the invite doc
    return {
      id: querySnapshot.docs[0].id,
      data: querySnapshot.docs[0].data()
    };
  };

  // EMAIL/PASSWORD SIGNUP
  const handleSubmit = async (e) => {
    e.preventDefault();
    const { firstName, lastName, email, password } = form;

    try {
      const invite = await checkInvitation(email);
      if (!invite) {
        setMessage("Error: This email is not authorized. Contact Admin.");
        return;
      }

      const userCredential = await createUserWithEmailAndPassword(auth, email, password);
      const uid = userCredential.user.uid;

      await createProfile(uid, firstName, lastName, email, invite);
      
    } catch (err) {
      console.error(err);
      if (err.code === 'auth/email-already-in-use') setMessage("Account exists. Login instead.");
      else setMessage("Error creating account.");
    }
  };

  // GOOGLE SIGNUP
  const handleGoogleSignup = async () => {
    try {
      // Sign in with Google first to get the email
      const result = await signInWithPopup(auth, googleProvider);
      const user = result.user;
      
      // Check if this Google email was invited
      const invite = await checkInvitation(user.email);
      
      if (!invite) {
        setMessage("Error: This Google email was not invited by Admin.");
        // If not invited, delete the Auth user we just created
        await user.delete(); 
        return;
      }

      // If invited, split the Google Name into First/Last
      const names = (user.displayName || "").split(" ");
      const fName = names[0] || "Doctor";
      const lName = names.slice(1).join(" ") || "";

      await createProfile(user.uid, fName, lName, user.email, invite);

    } catch (err) {
      console.error(err);
      setMessage("Google Signup Failed.");
    }
  };

  // CREATE DB PROFILE & DELETE INVITE
  const createProfile = async (uid, fName, lName, email, invite) => {
    // Create the real profile
    await setDoc(doc(db, "doctors", uid), {
        firstName: fName,
        lastName: lName,
        email: email,
        role: invite.data.role || "doctor",
        phone: "",
        office: "",
        dob: "",
        photoURL: "",
        createdAt: new Date(),
      });

      // Delete the placeholder invite
      await deleteDoc(doc(db, "doctors", invite.id));

      setMessage("Account activated successfully!");
      setTimeout(() => navigate("/home"), 900);
  }

  return (
    <div className="auth-wrapper">
      <div className="auth-card">
        <h2 className="auth-logo">NeuroMind System</h2>
        <h3 className="auth-title">Activate Account</h3>

        <form onSubmit={handleSubmit} className="auth-form">
          <input name="firstName" type="text" placeholder="First Name" className="auth-input" onChange={handleChange} required />
          <input name="lastName" type="text" placeholder="Last Name" className="auth-input" onChange={handleChange} required />
          <input name="email" type="email" placeholder="Email (Must match invitation)" className="auth-input" onChange={handleChange} required />
          <input name="password" type="password" placeholder="Create Password" className="auth-input" onChange={handleChange} required />
          
          <button type="submit" className="auth-button">Activate with Email</button>
        </form>

        <p style={{textAlign:"center", margin: "10px 0"}}>OR</p>

        {/* GOOGLE BUTTON */}
        <button 
          type="button" 
          onClick={handleGoogleSignup} 
          className="auth-button google-btn"
          style={{backgroundColor: "#DB4437"}}
        >
          Activate with Google
        </button>

        {message && <p className="auth-message">{message}</p>}
        <p className="auth-switch">
          Already active? <Link to="/login" className="auth-link">Login</Link>
        </p>
      </div>
    </div>
  );
}