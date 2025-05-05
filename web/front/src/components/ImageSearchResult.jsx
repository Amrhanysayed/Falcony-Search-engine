import React from "react";

function ImageSearchResult({ results, status }) {
  if (status === "all") return null; // Only render if status is "images"
  if (!results || results.total === 0) {
    return (
      <div className="flex flex-col items-center justify-center text-gray-500 py-16 px-4">
        <svg className="w-16 h-16 mb-4 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
        <h3 className="text-lg font-medium">No image results found</h3>
        <p className="text-sm mt-2">Try adjusting your search terms or filters</p>
      </div>
    );
  }

  // Flatten all images into a single array
  const allImages = [];
  results.docs.forEach(item => {
    if (item.images && item.images.length > 0) {
      item.images.forEach(imageUrl => {
        allImages.push({
          url: imageUrl,
          title: item.title || "Untitled",
          source: item.url || ""
        });
      });
    }
  });

  return (
    <div className="mt-4">
      <p className="text-sm text-gray-500 mb-3 pl-4">
        Found {allImages.length} {allImages.length === 1 ? "image" : "images"}
      </p>
      
      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-3 p-2">
        {allImages.map((image, index) => (
          <a 
            key={index}
            className="group relative cursor-pointer"
            href={image.url}
            target="_blank"
          >
            <div className="bg-gray-100 rounded-lg overflow-hidden aspect-square">
              <img
                src={image.url}
                alt={image.title}
                className="w-full h-full object-cover hover:opacity-95 transition-opacity"
                onError={(e) => {
                  e.target.onerror = null;
                  e.target.src = "/api/placeholder/200/200";
                }}
              />
            </div>
            
            <div className="mt-1 px-1">
              <p className="text-xs text-gray-800 truncate group-hover:text-blue-600 transition-colors">
                {image.title}
              </p>
              {image.source && (
                <a className="text-xs text-gray-500 truncate">
                  {image.source}
                </a>
              )}
            </div>
          </a>
        ))}
      </div>
    </div>
  );
}

export default ImageSearchResult;