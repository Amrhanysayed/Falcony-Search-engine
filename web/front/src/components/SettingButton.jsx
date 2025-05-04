import React from "react";
import { FaCog } from "react-icons/fa";
function SettingButton({ setIsSettingsOpen, isSettingsOpen }) {
  console.log(isSettingsOpen);
  return (
    <div className="absolute bottom-16 right-6 ">
      <button
        onClick={() => setIsSettingsOpen(!isSettingsOpen)}
        className="p-3 rounded-full shadow-md hover:shadow-lg transition-all"
        title="Customize"
      >
        <FaCog size={20} />
      </button>
    </div>
  );
}

export default SettingButton;
