import { BrowserRouter as Router, Routes, Route } from "react-router";
import NotFound from "./pages/OtherPage/NotFound";
import Profile from "./pages/Profile";
import AppLayout from "./layout/AppLayout";
import { ScrollToTop } from "./components/common/ScrollToTop";
import Home from "./pages/Dashboard/Home";
import ProtectedRoute from "./components/auth/ProtectedRoute";
import OrganizationManagement from "./pages/cmdb/OrganizationManagement";
import ServiceManagement from "./pages/cmdb/ServiceManagement";
import AssetManagement from "./pages/cmdb/AssetManagement";
import CIManagement from "./pages/cmdb/CIManagement";
import ImpactAnalysis from "./pages/cmdb/ImpactAnalysis";
import FileImport from "./pages/cmdb/FileImport";

export default function App() {
  return (
    <Router>
      <ScrollToTop />
      <Routes>
        {/* Protected Dashboard Layout — keycloak-js with onLoad: 'login-required' ensures authentication before rendering */}
        <Route
          element={
            <ProtectedRoute>
              <AppLayout />
            </ProtectedRoute>
          }
        >
          <Route index path="/" element={<Home />} />
          <Route path="/profile" element={<Profile />} />

          {/* CMDB Routes */}
          <Route path="/cmdb/organizations" element={<OrganizationManagement />} />
          <Route path="/cmdb/services" element={<ServiceManagement />} />
          <Route path="/cmdb/assets" element={<AssetManagement />} />
          <Route path="/cmdb/cis" element={<CIManagement />} />
          <Route path="/cmdb/impact-analysis" element={<ImpactAnalysis />} />
          <Route path="/cmdb/imports" element={<FileImport />} />
        </Route>

        {/* Fallback */}
        <Route path="*" element={<NotFound />} />
      </Routes>
    </Router>
  );
}
