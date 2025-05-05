import React, { useEffect, useState } from "react";
import falcony_api from "../axiosInstance";
import { useNavigate } from "react-router";

const MAX_SUGGESTIONS = 10;

function SuggestionsList({ query, setShowDropdown, setSearchQuery }) {
  const nav = useNavigate();
  const [suggestions, setSuggestions] = useState([]);

  useEffect(() => {
    async function fetchSuggestions() {
      try {
        const response = await falcony_api.get(
          `/suggestions?query=${query} `
        );
        setSuggestions(response.data);
      } catch (error) {
        console.error("Error fetching suggestions:", error);
      }
    }
    if (query) {
      fetchSuggestions();
    }
  }, [query]);

  const handleSuggestionClick = (sugg) => {
    setSearchQuery(sugg);
    nav(`/search?query=${sugg}`);
    setShowDropdown(false);
  };

  return (
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
  );
}

export default SuggestionsList;
