import React, { createContext, useContext, useState, useEffect } from "react";

const SettingsContext = createContext();

export const SettingsProvider = ({ children }) => {
  const [darkMode, setDarkMode] = useState(false);
  const [safeSearch, setSafeSearch] = useState(true);
  const [themeColor, setThemeColor] = useState("#f3f4f6");
  const [language, setLanguage] = useState("English");

  // Apply/remove dark class to <html>
  useEffect(() => {
    const html = document.documentElement;
    darkMode ? html.classList.add("dark") : html.classList.remove("dark");
  }, [darkMode]);

  return (
    <SettingsContext.Provider
      value={{
        darkMode,
        setDarkMode,
        safeSearch,
        setSafeSearch,
        themeColor,
        setThemeColor,
        language,
        setLanguage,
      }}
    >
      {children}
    </SettingsContext.Provider>
  );
};

export const useSettings = () => useContext(SettingsContext);
