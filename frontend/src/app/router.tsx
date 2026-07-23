import { createBrowserRouter } from "react-router-dom"

import App from "@/app/App"
import { NotFoundPage } from "@/app/pages/NotFoundPage"
import { RegisterPage, SignInPage } from "@/features/authentication"
import { DashboardPage } from "@/features/dashboard"

export const router = createBrowserRouter([
  {
    path: "/sign-in",
    element: <SignInPage />,
  },
  {
    path: "/register",
    element: <RegisterPage />,
  },
  {
    path: "/",
    element: <App />,
    children: [
      {
        index: true,
        element: <DashboardPage />,
      },
      {
        path: "*",
        element: <NotFoundPage />,
      },
    ],
  },
])
