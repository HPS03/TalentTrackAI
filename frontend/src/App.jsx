import { Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import { homeFor, useAuth } from './context/AuthContext';
import AdminDashboard from './pages/admin/AdminDashboard';
import AdminJobs from './pages/admin/AdminJobs';
import AdminUsers from './pages/admin/AdminUsers';
import AtsChecker from './pages/candidate/AtsChecker';
import CandidateDashboard from './pages/candidate/CandidateDashboard';
import MyApplications from './pages/candidate/MyApplications';
import Profile from './pages/candidate/Profile';
import JobDetail from './pages/JobDetail';
import Jobs from './pages/Jobs';
import Landing from './pages/Landing';
import Login from './pages/Login';
import NotFound from './pages/NotFound';
import KanbanBoard from './pages/recruiter/KanbanBoard';
import RecruiterDashboard from './pages/recruiter/RecruiterDashboard';
import RecruiterJobs from './pages/recruiter/RecruiterJobs';
import Register from './pages/Register';

function GuestOnly({ children }) {
  const { user } = useAuth();
  return user ? <Navigate to={homeFor(user.role)} replace /> : children;
}

export default function App() {
  const { user } = useAuth();
  return (
    <Routes>
      <Route path="/" element={user ? <Navigate to={homeFor(user.role)} replace /> : <Landing />} />
      <Route path="/login" element={<GuestOnly><Login /></GuestOnly>} />
      <Route path="/register" element={<GuestOnly><Register /></GuestOnly>} />

      {/* Job browsing is public, but logged-in users see it inside their app shell */}
      {user ? (
        <Route element={<Layout />}>
          <Route path="/jobs" element={<Jobs />} />
          <Route path="/jobs/:id" element={<JobDetail />} />
        </Route>
      ) : (
        <>
          <Route path="/jobs" element={<Jobs publicView />} />
          <Route path="/jobs/:id" element={<JobDetail publicView />} />
        </>
      )}

      <Route element={<ProtectedRoute roles={['CANDIDATE']} />}>
        <Route element={<Layout />}>
          <Route path="/candidate" element={<CandidateDashboard />} />
          <Route path="/candidate/profile" element={<Profile />} />
          <Route path="/candidate/applications" element={<MyApplications />} />
          <Route path="/candidate/ats-checker" element={<AtsChecker />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute roles={['RECRUITER']} />}>
        <Route element={<Layout />}>
          <Route path="/recruiter" element={<RecruiterDashboard />} />
          <Route path="/recruiter/jobs" element={<RecruiterJobs />} />
          <Route path="/recruiter/jobs/:jobId/board" element={<KanbanBoard />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute roles={['ADMIN']} />}>
        <Route element={<Layout />}>
          <Route path="/admin" element={<AdminDashboard />} />
          <Route path="/admin/users" element={<AdminUsers />} />
          <Route path="/admin/jobs" element={<AdminJobs />} />
        </Route>
      </Route>

      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}
