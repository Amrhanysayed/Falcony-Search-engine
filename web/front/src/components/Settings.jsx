import React from "react";
import { useSettings } from "../context/SettingsContext";
import { HexColorPicker } from "react-colorful";

function Settings() {
  const {
    darkMode,
    setDarkMode,
    safeSearch,
    setSafeSearch,
    themeColor,
    setThemeColor,
  } = useSettings();

  console.log(themeColor, "themeColor");

  return (
    <div className="absolute bottom-28 right-0 bg-white dark:bg-gray-800 text-black dark:text-white shadow-lg rounded-lg p-4 w-72">
      <h3 className="text-lg font-medium mb-3">Customize Falcony</h3>

      <div className="space-y-4">
        {/* Dark Mode Toggle */}
        <div className="flex items-center justify-between">
          <span>Dark mode</span>
          <button
            onClick={() => setDarkMode(!darkMode)}
            className={`w-12 h-6 rounded-full relative transition-colors ${
              darkMode ? "bg-green-500" : "bg-gray-300"
            }`}
          >
            <span
              className={`block w-5 h-5 bg-white rounded-full absolute top-0.5 transition-all ${
                darkMode ? "right-0.5" : "left-0.5"
              }`}
            />
          </button>
        </div>

        {/* Safe Search Toggle */}
        <div className="flex items-center justify-between">
          <span>Safe search</span>
          <button
            onClick={() => setSafeSearch(!safeSearch)}
            className={`w-12 h-6 rounded-full relative transition-colors ${
              safeSearch ? "bg-blue-500" : "bg-gray-300"
            }`}
          >
            <span
              className={`block w-5 h-5 bg-white rounded-full absolute top-0.5 transition-all ${
                safeSearch ? "right-0.5" : "left-0.5"
              }`}
            />
          </button>
        </div>

        {/* Language Selector */}
        <div className="flex items-center justify-between">
          <span>Language</span>
          <select className="border rounded p-1 text-sm dark:bg-gray-700">
            <option>English</option>
            <option>العربيه</option>
          </select>
        </div>

        {/* Color Picker */}
        <div>
          <span className="block text-sm mb-1 font-medium">Theme Color</span>
          <HexColorPicker color={themeColor} onChange={setThemeColor} />
          <div
            className="mt-2 w-full h-6 rounded border"
            style={{ backgroundColor: themeColor }}
          />
        </div>
      </div>
    </div>
  );
}

export default Settings;
