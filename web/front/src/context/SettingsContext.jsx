import React, { createContext, useContext, useState, useEffect } from "react";

const SettingsContext = createContext();

export const SettingsProvider = ({ children }) => {
  const [darkMode, setDarkMode] = useState(() => {
    const saved = localStorage.getItem("darkMode");
    return saved !== null ? JSON.parse(saved) : false;
  });

  const [safeSearch, setSafeSearch] = useState(() => {
    const saved = localStorage.getItem("safeSearch");
    return saved !== null ? JSON.parse(saved) : true;
  });

  const [themeColor, setThemeColor] = useState(() => {
    const saved = localStorage.getItem("themeColor");
    return saved !== null ? saved : "#4173d9";
  });

  const [language, setLanguage] = useState(() => {
    const saved = localStorage.getItem("language");
    return saved !== null ? saved : "English";
  });

  // Save to localStorage whenever settings change
  useEffect(() => {
    localStorage.setItem("darkMode", JSON.stringify(darkMode));
  }, [darkMode]);

  useEffect(() => {
    localStorage.setItem("safeSearch", JSON.stringify(safeSearch));
  }, [safeSearch]);

  useEffect(() => {
    localStorage.setItem("themeColor", themeColor);
  }, [themeColor]);

  useEffect(() => {
    localStorage.setItem("language", language);
  }, [language]);

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
