import React from "react";
import { useSettings } from "../context/SettingsContext";

function Footer() {
  const { themeColor } = useSettings();

  return (
    <div
      className="mt-auto w-full p-4 transition-colors"
      style={{
        backgroundColor: themeColor + "33", // light background with alpha
      }}
    >
      <div className="flex justify-center text-sm">
        <span>&copy; {new Date().getFullYear()} Falcony Search</span>
      </div>
    </div>
  );
}

export default Footer;
