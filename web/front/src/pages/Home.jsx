import React, { useEffect, useState } from "react";

import SearchUnit from "../components/SearchUnit";
import Footer from "../components/Footer";
import Settings from "../components/Settings";
import { useSettings } from "../context/SettingsContext";
import { useQuery } from "../context/QueryContext";
import SettingButton from "../components/SettingButton";

export default function Homepage() {
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const { themeColor } = useSettings();
  console.log(themeColor, "themeColor from home page");
  const { setSearchQuery } = useQuery();

  useEffect(() => {
    setSearchQuery("");
  }, []);

  return (
    <div
      className="min-h-screen flex flex-col items-center relative transition-all"
      style={{ backgroundColor: themeColor + "20" }} // light background using alpha
    >
      <div className="mt-32 mb-8 text-center">
        <img
          src="../../src/assets/Falcony_logo.png"
          alt="Falcony Logo"
          className="size-[200px]"
        />
      </div>

      <SearchUnit />

      <SettingButton
        setIsSettingsOpen={setIsSettingsOpen}
        isSettingsOpen={isSettingsOpen}
      />

      {isSettingsOpen && <Settings />}

      <Footer />
    </div>
  );
}
