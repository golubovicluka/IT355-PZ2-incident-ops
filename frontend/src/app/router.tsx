import { Navigate, Outlet, createBrowserRouter } from "react-router-dom"

import App from "@/app/App"
import { NotFoundPage } from "@/app/pages/NotFoundPage"
import { AdminPage } from "@/features/administration"
import {
  AdminRoute,
  AuthenticationProvider,
  ProtectedRoute,
  RegisterPage,
  SignInPage,
} from "@/features/authentication"
import { DashboardPage } from "@/features/dashboard"

export const router = createBrowserRouter([
  {
    element: (
      <AuthenticationProvider>
        <Outlet />
      </AuthenticationProvider>
    ),
    children: [
      {
        path: "/sign-in",
        element: <SignInPage />,
      },
      {
        path: "/register",
        element: <RegisterPage />,
      },
      {
        element: <ProtectedRoute />,
        children: [
          {
            path: "/",
            element: <App />,
            children: [
              {
                index: true,
                element: <Navigate replace to="/dashboard" />,
              },
              {
                path: "dashboard",
                element: <DashboardPage />,
              },
              {
                element: <AdminRoute />,
                children: [
                  {
                    path: "admin",
                    element: <AdminPage />,
                  },
                ],
              },
              {
                path: "*",
                element: <NotFoundPage />,
              },
            ],
          },
        ],
      },
    ],
  },
])
