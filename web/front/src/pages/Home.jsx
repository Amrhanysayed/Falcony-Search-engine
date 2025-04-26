import React, { useState } from "react";
import { FaCog } from "react-icons/fa";
import SearchUnit from "../components/SearchUnit";
import Footer from "../components/Footer";
import Settings from "../components/Settings";
import { useSettings } from "../context/SettingsContext";

export default function Homepage() {
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const { themeColor } = useSettings();

  return (
    <div
      className="min-h-screen flex flex-col items-center relative transition-all"
      style={{ backgroundColor: themeColor + "20" }} // light background using alpha
    >
      <div className="mt-32 mb-8 text-center">
        <img
          src="../../src/assets/Falcony_logo.png"
          alt="Falcony Logo"
          className="size-[200px] mb-4"
        />
        <span className="text-lg font-medium" style={{ color: themeColor }}>
          Your AI-Powered Search Engine
        </span>
      </div>

      <SearchUnit />

      <div className="absolute bottom-16 right-6 ">
        <button
          onClick={() => setIsSettingsOpen(!isSettingsOpen)}
          className="p-3 rounded-full shadow-md hover:shadow-lg transition-all"
          title="Customize"
        >
          <FaCog size={20} />
        </button>
      </div>

      {isSettingsOpen && <Settings />}

      <Footer />
    </div>
  );
}
