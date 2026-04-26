import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router";
import LoginPage from "./pages/AuthPages/LoginPage";
import NotFound from "./pages/OtherPage/NotFound";
import TagManagement from "./pages/TagManagement";
import RoleManagement from "./pages/RoleManagement";
import UserManagement from "./pages/UserManagement";
import Profile from "./pages/Profile";
import AppLayout from "./layout/AppLayout";
import { ScrollToTop } from "./components/common/ScrollToTop";
import Home from "./pages/Dashboard/Home";
import ProtectedRoute from "./components/auth/ProtectedRoute";
import Authorize from "./components/auth/Authorize";
import { useAuth } from "./context/AuthContext";
import OrganizationManagement from "./pages/cmdb/OrganizationManagement";
import ServiceManagement from "./pages/cmdb/ServiceManagement";
import AssetManagement from "./pages/cmdb/AssetManagement";
import CIManagement from "./pages/cmdb/CIManagement";
import ImpactAnalysis from "./pages/cmdb/ImpactAnalysis";
import FileImport from "./pages/cmdb/FileImport";

function LoginRoute() {
  const { isAuthenticated } = useAuth();
  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }
  return <LoginPage />;
}

export default function App() {
  return (
    <Router>
      <ScrollToTop />
      <Routes>
        {/* Protected Dashboard Layout */}
        <Route
          element={
            <ProtectedRoute>
              <AppLayout />
            </ProtectedRoute>
          }
        >
          <Route index path="/" element={<Home />} />
          <Route
            path="/tags"
            element={
              <Authorize roles={["ADMIN"]} fallback={<Navigate to="/" replace />}>
                <TagManagement />
              </Authorize>
            }
          />
          <Route
            path="/roles"
            element={
              <Authorize roles={["ADMIN"]} fallback={<Navigate to="/" replace />}>
                <RoleManagement />
              </Authorize>
            }
          />
          <Route
            path="/users"
            element={
              <Authorize roles={["ADMIN"]} fallback={<Navigate to="/" replace />}>
                <UserManagement />
              </Authorize>
            }
          />
          <Route path="/profile" element={<Profile />} />

          {/* CMDB Routes */}
          <Route path="/cmdb/organizations" element={<OrganizationManagement />} />
          <Route path="/cmdb/services" element={<ServiceManagement />} />
          <Route path="/cmdb/assets" element={<AssetManagement />} />
          <Route path="/cmdb/cis" element={<CIManagement />} />
          <Route path="/cmdb/impact-analysis" element={<ImpactAnalysis />} />
          <Route path="/cmdb/imports" element={<FileImport />} />
        </Route>

        {/* Public: Login (redirects authenticated users to dashboard) */}
        <Route path="/login" element={<LoginRoute />} />

        {/* Fallback */}
        <Route path="*" element={<NotFound />} />
      </Routes>
    </Router>
  );
}
