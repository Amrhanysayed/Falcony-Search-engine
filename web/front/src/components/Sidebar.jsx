import React from "react";

function Sidebar({ status, setStatus }) {
  const handleStatusChange = (newStatus) => {
    setStatus(newStatus);
  };

  return (
    <div className="bg-white shadow-md rounded-lg p-1 w-1/4 mx-auto">
      <div className="flex flex-row gap-4 items-center justify-center">
        <div
          className={`px-1 font-medium rounded-md cursor-pointer transition-all 
            ${
              status === "all"
                ? "bg-blue-100 text-blue-800 hover:bg-blue-200"
                : "hover:bg-gray-100"
            }`}
          onClick={() => handleStatusChange("all")}
        >
          All
        </div>
        <div
          className={`px-1 font-medium rounded-md cursor-pointer transition-all 
            ${
              status === "images"
                ? "bg-blue-100 text-blue-800 hover:bg-blue-200"
                : "hover:bg-gray-100"
            }`}
          onClick={() => handleStatusChange("images")}
        >
          Images
        </div>
      </div>
    </div>
  );
}

export default Sidebar;
