import React, { useState, useRef } from "react";
import { FaSearch, FaCamera, FaMicrophone } from "react-icons/fa";
function SearchUnit() {
  const handleSearch = (e) => {
    e.preventDefault();
    console.log("Searching for:", searchQuery);
  };

  const [searchQuery, setSearchQuery] = useState("");

  const fileInputRef = useRef(null);

  const handleImageUpload = () => {
    fileInputRef.current.click();
  };

  const handleFileChange = (e) => {
    // Handle the file upload logic here
    const file = e.target.files[0];
    if (file) {
      console.log("Image selected:", file.name);
      // You would typically process the image here
    }
  };

  return (
    <div className="w-full max-w-2xl px-4">
      <form onSubmit={handleSearch} className="relative">
        <div className="flex items-center w-full bg-white rounded-full shadow-md hover:shadow-lg transition-shadow border border-gray-300">
          <div className="pl-4 text-gray-500">
            <FaSearch />
          </div>

          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full py-3 px-4 rounded-full focus:outline-none"
            placeholder="Search the web"
          />

          <div className="flex space-x-3 pr-4">
            <button
              type="button"
              className="text-gray-500 hover:text-blue-600 transition-colors"
              title="Search by voice"
            >
              <FaMicrophone />
            </button>

            <button
              type="button"
              className="text-gray-500 hover:text-blue-600 transition-colors"
              onClick={handleImageUpload}
              title="Search by image"
            >
              <FaCamera />
              <input
                type="file"
                ref={fileInputRef}
                onChange={handleFileChange}
                accept="image/*"
                className="hidden"
              />
            </button>
          </div>
        </div>
      </form>
    </div>
  );
}

export default SearchUnit;
