import React from "react";
import Homepage from "./pages/Home";
import Results from "./pages/Results";
import { createBrowserRouter } from "react-router-dom";

const router = createBrowserRouter([
  {
    path: "/",
    element: <Homepage />,
    errorElement: <div>404 Not Found</div>,
  },
  {
    path: "/search",
    element: <Results />,
    errorElement: <div>404 Not Found</div>,
  },
]);

export default router;
