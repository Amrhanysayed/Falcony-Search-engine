import React from "react";
import { Search, ExternalLink } from "lucide-react";

function SearchItem({ item }) {
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
      <p className="text-sm text-gray-800">{item.description}</p>

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
