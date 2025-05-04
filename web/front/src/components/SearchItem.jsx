import React from "react";
import { Search, ExternalLink } from "lucide-react";
import { useSearchParams } from "react-router";

function highlightMatch(text, query) {
  if (!query) return text;

  const regex = new RegExp(`(${query})`, "gi");
  const parts = text.split(regex);

  return parts.map((part, i) =>
    regex.test(part) ? <strong key={i}>{part}</strong> : part
  );
}

function SearchItem({ item }) {
  const [searchParams] = useSearchParams();
  const query = searchParams.get("query");
  return (
    <div className="flex flex-col  pb-4 mb-4">
      {/* URL/Source */}
      <div className="flex items-center mb-1">
        <span className="text-sm text-gray-600 truncate">
          {item.url || "https://example.com/result"}
        </span>
      </div>

      {/* Title with link */}
      <a href={item.url} target="blank" className="group">
        <h3 className="text-xl text-blue-600 font-medium mb-1 group-hover:underline">
          {item.title}
        </h3>
      </a>

      {/* Description */}
      <p className="text-sm text-gray-800">
        {highlightMatch(item.description, query)}
      </p>

      {/* Additional links if available */}
      {/* {item.sublinks && (
        <div className="mt-2 flex flex-wrap">
          {item.sublinks.map((sublink, i) => (
            <a
              key={i}
              href="#"
              className="flex items-center mr-4 mt-1 text-sm text-blue-600 hover:underline"
            >
              <ExternalLink size={12} className="mr-1" />
              {sublink.text}
            </a>
          ))}
        </div>
      )} */}
    </div>
  );
}

export default SearchItem;
