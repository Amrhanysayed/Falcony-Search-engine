import React from "react";
import { Search, ExternalLink } from "lucide-react";
import SearchItem from "./SearchItem";

function SearchResults({ results = [] }) {
  return (
    <div className="flex flex-col w-full max-w-3xl px-4 py-6">
      {/* Results Count */}
      <p className="text-sm text-gray-500 mb-4">
        About {results.length} results
      </p>

      {/* Results List */}
      <div className="w-full space-y-6">
        {results.map((item, index) => (
          <SearchItem key={index} item={item} />
        ))}
      </div>
    </div>
  );
}

export default SearchResults;
