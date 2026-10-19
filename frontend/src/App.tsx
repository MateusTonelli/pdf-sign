import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AppLayout } from './components/AppLayout';
import { ProtectedRoute } from './components/ProtectedRoute';
import { CertificateSettingsPage } from './pages/CertificateSettingsPage';
import { DocumentListPage } from './pages/DocumentListPage';
import { DocumentWizardPage } from './pages/DocumentWizardPage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { SignPublicPage } from './pages/SignPublicPage';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/app" replace />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/sign/:token" element={<SignPublicPage />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route path="/app" element={<DocumentListPage />} />
            <Route path="/app/documents/new" element={<DocumentWizardPage />} />
            <Route
              path="/app/settings/certificate"
              element={<CertificateSettingsPage />}
            />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/app" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
