import React, { useRef, useState, useEffect } from "react";
import { FaSearch, FaCamera, FaMicrophone } from "react-icons/fa";
import { useNavigate } from "react-router";
import { useQuery } from "../context/QueryContext";

const MAX_SUGGESTIONS = 5;

function SearchUnit() {
  const { searchQuery, setSearchQuery } = useQuery();
  const [suggestions, setSuggestions] = useState([]);
  const [showDropdown, setShowDropdown] = useState(false);
  const nav = useNavigate();
  const fileInputRef = useRef(null);

  useEffect(() => {
    const stored = JSON.parse(localStorage.getItem("searchSuggestions")) || [];
    setSuggestions(stored);
  }, []);

  const handleSearch = (e) => {
    e.preventDefault();
    if (!searchQuery.trim()) return;

    nav(`/search?query=${searchQuery}`);
    setShowDropdown(false);

    // Save to localStorage
    let updated = [
      searchQuery,
      ...suggestions.filter((s) => s !== searchQuery),
    ];
    if (updated.length > MAX_SUGGESTIONS) {
      updated = updated.slice(0, MAX_SUGGESTIONS);
    }

    localStorage.setItem("searchSuggestions", JSON.stringify(updated));
    setSuggestions(updated);
  };

  const handleSuggestionClick = (sugg) => {
    setSearchQuery(sugg);
    nav(`/search?query=${sugg}`);
    setShowDropdown(false);
  };

  const handleImageUpload = () => {
    fileInputRef.current.click();
  };

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      console.log("Image selected:", file.name);
    }
  };

  return (
    <div className="w-full max-w-2xl px-4 relative">
      <form onSubmit={handleSearch} className="relative">
        <div className="flex items-center w-full bg-white rounded-full shadow-md hover:shadow-lg transition-shadow border border-gray-300">
          <div className="pl-4 text-gray-500">
            <FaSearch
              onClick={handleSearch}
              className="cursor-pointer hover:text-blue-600 transition-colors"
            />
          </div>

          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onFocus={() => setShowDropdown(true)}
            onBlur={() => setTimeout(() => setShowDropdown(false), 150)} // delay so clicks register
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

      {/* Dropdown Suggestions */}
      {showDropdown && suggestions.length > 0 && (
        <ul className="absolute z-10 w-full mt-2 bg-white border border-gray-300 rounded-md shadow-md max-h-48 overflow-y-auto">
          {suggestions.map((s, i) => (
            <li
              key={i}
              onClick={() => handleSuggestionClick(s)}
              className="px-4 py-2 hover:bg-gray-100 cursor-pointer text-sm"
            >
              {s}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default SearchUnit;
