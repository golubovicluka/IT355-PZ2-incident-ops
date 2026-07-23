import { createBrowserRouter } from "react-router-dom"

import App from "@/app/App"
import { NotFoundPage } from "@/app/pages/NotFoundPage"
import { DashboardPage } from "@/features/dashboard"

export const router = createBrowserRouter([
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
