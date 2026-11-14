import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";

import { signInWithEmailAndPassword, signInWithPopup } from "firebase/auth";
import { auth, db, googleProvider } from "../firebase"; 
import { doc, getDoc } from "firebase/firestore";
import "../auth.css";

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");

  const navigate = useNavigate();

  
  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const userCredential = await signInWithEmailAndPassword(auth, email, password);
      checkUserRole(userCredential.user.uid);
    } catch (error) {
      console.error(error);
      setMessage("Invalid email or password.");
    }
  };

 
  const handleGoogleLogin = async () => {
    try {
      const result = await signInWithPopup(auth, googleProvider);
      checkUserRole(result.user.uid);
    } catch (error) {
      console.error(error);
      setMessage("Google sign-in failed.");
    }
  };

  
  const checkUserRole = async (uid) => {
    const profileRef = doc(db, "doctors", uid);
    const profileSnap = await getDoc(profileRef);

    if (!profileSnap.exists()) {
      setMessage("Account not found. Please Sign Up first.");
      auth.signOut();
      return;
    }

    const userData = profileSnap.data();

    if (userData.role === 'admin') {
        setMessage("Admin Login successful!");
        setTimeout(() => navigate("/admin"), 900);
    } else {
        setMessage("Doctor Login successful!");
        setTimeout(() => navigate("/home"), 900);
    }
  };

  return (
    <div className="auth-wrapper">
      <div className="auth-card">
        <h2 className="auth-logo">NeuroMind System</h2>
        <h3 className="auth-title">System Login</h3>

        <form onSubmit={handleSubmit} className="auth-form">
          <input
            type="email"
            placeholder="Email"
            className="auth-input"
            onChange={(e) => setEmail(e.target.value)}
            required
          />
          <input
            type="password"
            placeholder="Password"
            className="auth-input"
            onChange={(e) => setPassword(e.target.value)}
            required
          />
          <button type="submit" className="auth-button">
            Login
          </button>
        </form>

      
        <button 
          type="button" 
          onClick={handleGoogleLogin} 
          className="auth-button google-btn"
          style={{marginTop: "10px", backgroundColor: "#DB4437"}}
        >
          Sign in with Google
        </button>

        {message && <p className="auth-message">{message}</p>}

        <p className="auth-switch">
          First time here?{" "}
          <Link to="/signup" className="auth-link">Activate Account</Link>
        </p>

      </div>
    </div>
  );
}