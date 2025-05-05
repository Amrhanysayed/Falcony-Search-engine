import React from "react";

function ImageSearchResult({ result }) {
  if (!Array.isArray(result) || result.length === 0) {
    return (
      <div className="text-center text-gray-500 py-8">
        No search results found
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 p-4">
      {result.map((item, index) => (
        <div
          key={index}
          className="bg-white rounded-lg shadow-md overflow-hidden transition-transform hover:scale-105 flex flex-col"
        >
          {Array.isArray(item.images) &&
            item.images.map((image, i) => (
              <img key={i} src={image} className="w-full h-48 object-cover" />
            ))}
          <div className="p-4 flex-grow flex flex-col">
            <h3 className="font-bold text-lg mb-2 text-gray-800">
              {item.title}
            </h3>
          </div>
        </div>
      ))}
    </div>
  );
}

export default ImageSearchResult;
