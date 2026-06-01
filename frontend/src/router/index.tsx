import { createBrowserRouter, Navigate } from "react-router-dom";
import { RequireAdmin } from "@/components/auth/RequireAdmin";
import { RequireAuth } from "@/components/auth/RequireAuth";
import { AdminLayout } from "@/layouts/AdminLayout";
import { GuestLayout } from "@/layouts/GuestLayout";
import { UserAppLayout } from "@/layouts/UserAppLayout";
import { BootstrapStatusPage } from "@/pages/BootstrapStatusPage";
import { AuthCallbackPage } from "@/pages/AuthCallbackPage";
import { HomePage } from "@/pages/HomePage";
import { LoginPage } from "@/pages/LoginPage";
import { PasswordResetConfirmPage } from "@/pages/PasswordResetConfirmPage";
import { PasswordResetRequestPage } from "@/pages/PasswordResetRequestPage";
import { AdminDashboardPage } from "@/pages/AdminDashboardPage";
import { AdminMcpToolsPage } from "@/pages/AdminMcpToolsPage";
import { AdminOpenAIConfigPage } from "@/pages/AdminOpenAIConfigPage";
import { AdminPromptsPage } from "@/pages/AdminPromptsPage";
import { AdminVectorDbPage } from "@/pages/AdminVectorDbPage";
import { CompanyDetailPage } from "@/pages/CompanyDetailPage";
import { AnalysisHistoryDetailPage } from "@/pages/AnalysisHistoryDetailPage";
import { WatchlistPage } from "@/pages/WatchlistPage";
import { MyPage } from "@/pages/MyPage";
import { AnalysisHistoryListPage } from "@/pages/AnalysisHistoryListPage";
import { GraphVisualizationPage } from "@/pages/GraphVisualizationPage";
import { SearchResultPage } from "@/pages/SearchResultPage";
import { TermsConsentPage } from "@/pages/TermsConsentPage";

export const router = createBrowserRouter([
  {
    element: <GuestLayout />,
    children: [
      { path: "/", element: <HomePage /> },
      { path: "/bootstrap", element: <BootstrapStatusPage /> },
      { path: "/login", element: <LoginPage /> },
      { path: "/auth/callback", element: <AuthCallbackPage /> },
      { path: "/terms", element: <TermsConsentPage /> },
      {
        path: "/password-reset",
        element: <PasswordResetRequestPage />,
      },
      {
        path: "/password-reset/confirm",
        element: <PasswordResetConfirmPage />,
      },
      {
        path: "/search",
        element: (
          <RequireAuth>
            <SearchResultPage />
          </RequireAuth>
        ),
      },
      {
        path: "/companies/:companyId",
        element: (
          <RequireAuth>
            <CompanyDetailPage />
          </RequireAuth>
        ),
      },
      {
        path: "/companies/:companyId/graph",
        element: (
          <RequireAuth>
            <GraphVisualizationPage />
          </RequireAuth>
        ),
      },
    ],
  },
  {
    path: "/app",
    element: <UserAppLayout />,
    children: [
      { index: true, element: <Navigate to="/history" replace /> },
      { path: "history", element: <AnalysisHistoryListPage /> },
      { path: "history/:sessionId", element: <AnalysisHistoryDetailPage /> },
      { path: "watchlist", element: <WatchlistPage /> },
      { path: "mypage", element: <MyPage /> },
    ],
  },
  {
    path: "/history",
    element: <Navigate to="/app/history" replace />,
  },
  {
    path: "/watchlist",
    element: <Navigate to="/app/watchlist" replace />,
  },
  {
    path: "/mypage",
    element: <Navigate to="/app/mypage" replace />,
  },
  {
    path: "/admin",
    element: (
      <RequireAdmin>
        <AdminLayout />
      </RequireAdmin>
    ),
    children: [
      { index: true, element: <AdminDashboardPage /> },
      { path: "mcp", element: <AdminMcpToolsPage /> },
      { path: "prompts", element: <AdminPromptsPage /> },
      { path: "openai", element: <AdminOpenAIConfigPage /> },
      { path: "vectordb", element: <AdminVectorDbPage /> },
    ],
  },
]);
