import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "./index.css";
import App from "./App.jsx";
import { SettingsProvider } from "./context/SettingsContext.jsx";
import { QueryProvider } from "./context/QueryContext.jsx";

createRoot(document.getElementById("root")).render(
  <StrictMode>
    <SettingsProvider>
      <QueryProvider>
        <App />
      </QueryProvider>
    </SettingsProvider>
  </StrictMode>
);
