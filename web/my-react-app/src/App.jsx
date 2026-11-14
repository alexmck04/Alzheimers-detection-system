import {
  Routes,
  Route,
  Link,
  useLocation,
  useNavigate,
  Navigate,
} from "react-router-dom";

import { useState, useEffect } from "react";

import { auth, db } from "./firebase";
import { doc, getDoc } from "firebase/firestore";
import { onAuthStateChanged } from "firebase/auth";

import LoginPage from "./pages/LoginPage";
import SignupPage from "./pages/SignupPage";
import HomePage from "./pages/HomePage";
import EditProfile from "./pages/EditProfile";
import NewsPage from "./pages/NewsPage";

import PatientsPage from "./pages/PatientsPage";
import PatientProfile from "./pages/PatientProfile";
import AdminDashboard from "./pages/AdminDashboard";
import RisksPage from "./pages/RisksPage";

import ReportsPage from "./pages/ReportsPage";
import PatientReportDetail from "./pages/PatientReportDetail";

import RiskDetailPage from "./pages/RiskDetailPage";

import NotificationsPage from "./pages/NotificationsPage";
import RatingsPage from "./pages/RatingsPage";

import SupportMessagesPage from "./pages/SupportMessagesPage";

import "./styles.css";

function App() {
  const location = useLocation();
  const navigate = useNavigate();

  const [collapsed, setCollapsed] = useState(false);
  const [role, setRole] = useState(null);
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 600);

  const isAuthPage =
    location.pathname === "/login" || location.pathname === "/signup";

  useEffect(() => {
    const onResize = () => setIsMobile(window.innerWidth <= 600);
    window.addEventListener("resize", onResize);
    return () => window.removeEventListener("resize", onResize);
  }, []);

  useEffect(() => {
    if (isMobile) setCollapsed(false);
  }, [isMobile]);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (user) => {
      if (user) {
        try {
          const docRef = doc(db, "doctors", user.uid);
          const snap = await getDoc(docRef);
          if (snap.exists()) {
            setRole(snap.data().role);
          } else {
            setRole(null);
          }
        } catch (err) {
          console.error("Error fetching role:", err);
          setRole(null);
        }
      } else {
        setRole(null);
      }
    });

    return () => unsubscribe();
  }, []);

  const handleSignOut = async () => {
    await auth.signOut();
    navigate("/login");
  };

  const isAdmin = role === "admin";

  return (
    <div className="app-wrapper">
      {!isAuthPage && (
        <aside className={`sidebar ${collapsed ? "collapsed" : ""}`}>
          <button
            className="sidebar-toggle"
            onClick={() => {
              if (!isMobile) setCollapsed(!collapsed);
            }}
          >
            {collapsed ? "☰" : "✖"}
          </button>

          {(isMobile || !collapsed) && (
            <>
              <h1 className="sidebar-logo">NeuroMind</h1>

              <nav className="sidebar-nav">
                <Link to="/home" className="sidebar-link">
                  Home
                </Link>

                <Link to="/news" className="sidebar-link">
                  News
                </Link>

                {isAdmin ? (
                  <Link to="/admin" className="sidebar-link">
                    Admin Panel
                  </Link>
                ) : (
                  <>
                    <Link to="/patients" className="sidebar-link">
                      Patients
                    </Link>
                    <Link to="/risks" className="sidebar-link">
                      Risks
                    </Link>
                    <Link to="/reports" className="sidebar-link">
                      Reports
                    </Link>
                    <Link to="/notifications" className="sidebar-link">
                      Notifications
                    </Link>
                  </>
                )}

                <Link to="/ratings" className="sidebar-link">
                  Ratings
                </Link>

                {isAdmin && (
                  <Link to="/support-messages" className="sidebar-link">
                    Support Messages
                  </Link>
                )}
              </nav>

              <button className="sidebar-signout" onClick={handleSignOut}>
                Sign Out
              </button>
            </>
          )}
        </aside>
      )}

      <main className={isAuthPage ? "fullpage" : "dashboard"}>
        <Routes>
          <Route path="/" element={<Navigate to="/login" replace />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />

          <Route path="/home" element={<HomePage />} />
          <Route path="/edit-profile" element={<EditProfile />} />
          <Route path="/news" element={<NewsPage />} />

          <Route
            path="/patients"
            element={isAdmin ? <Navigate to="/admin" replace /> : <PatientsPage />}
          />
          <Route
            path="/patients/:id"
            element={
              isAdmin ? <Navigate to="/admin" replace /> : <PatientProfile />
            }
          />

          <Route path="/admin" element={<AdminDashboard />} />

          <Route
            path="/risks"
            element={isAdmin ? <Navigate to="/admin" replace /> : <RisksPage />}
          />
          <Route
            path="/risks/:id"
            element={isAdmin ? <Navigate to="/admin" replace /> : <RiskDetailPage />}
          />

          <Route
            path="/reports"
            element={isAdmin ? <Navigate to="/admin" replace /> : <ReportsPage />}
          />
          <Route
            path="/reports/:id"
            element={
              isAdmin ? <Navigate to="/admin" replace /> : <PatientReportDetail />
            }
          />

          <Route
            path="/notifications"
            element={
              isAdmin ? <Navigate to="/admin" replace /> : <NotificationsPage />
            }
          />

          <Route path="/ratings" element={<RatingsPage />} />

          <Route
            path="/support-messages"
            element={
              isAdmin ? <SupportMessagesPage /> : <Navigate to="/home" replace />
            }
          />
        </Routes>
      </main>
    </div>
  );
}

export default App;
