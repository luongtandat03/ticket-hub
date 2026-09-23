import { createBrowserRouter } from "react-router";
import Layout from "./components/Layout";
import ManagerPage from "./pages/ManagerPage";

const routes = createBrowserRouter([
  {
    children: [
      {
        Component: Layout,
        children: [
          {
            path: "/manager",
            Component: ManagerPage,
          },
        ],
      },
    ],
  },
]);

export default routes;