// import { useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";

export default function Pagination({ page, setPage, totalPages }) {
  // Function to handle page change
  const handlePageChange = (page) => {
    setPage(page);
  };

  // Function to render page numbers
  const renderPageNumbers = () => {
    const pages = [];

    // Always show first page
    pages.push(
      <button
        key={1}
        onClick={() => handlePageChange(1)}
        className={`h-8 w-8 flex items-center justify-center rounded-full text-sm mx-1 ${
          page === 1 ? "bg-blue-500 text-white" : "hover:bg-gray-100"
        }`}
      >
        1
      </button>
    );

    // Logic for showing ellipsis and page numbers
    if (totalPages <= 7) {
      // If total pages are 7 or less, show all page numbers
      for (let i = 2; i <= totalPages; i++) {
        pages.push(
          <button
            key={i}
            onClick={() => handlePageChange(i)}
            className={`h-8 w-8 flex items-center justify-center rounded-full text-sm mx-1 ${
              page === i ? "bg-blue-500 text-white" : "hover:bg-gray-100"
            }`}
          >
            {i}
          </button>
        );
      }
    } else {
      // Show ellipsis and selective page numbers
      if (page > 3) {
        pages.push(
          <span key="ellipsis1" className="mx-1">
            ...
          </span>
        );
      }

      // Logic for middle pages
      const startPage = Math.max(2, page - 1);
      const endPage = Math.min(totalPages - 1, page + 1);

      for (let i = startPage; i <= endPage; i++) {
        pages.push(
          <button
            key={i}
            onClick={() => handlePageChange(i)}
            className={`h-8 w-8 flex items-center justify-center rounded-full text-sm mx-1 ${
              page === i ? "bg-blue-500 text-white" : "hover:bg-gray-100"
            }`}
          >
            {i}
          </button>
        );
      }

      if (page < totalPages - 2) {
        pages.push(
          <span key="ellipsis2" className="mx-1">
            ...
          </span>
        );
      }

      // Always show last page
      if (totalPages > 1) {
        pages.push(
          <button
            key={totalPages}
            onClick={() => handlePageChange(totalPages)}
            className={`h-8 w-8 flex items-center justify-center rounded-full text-sm mx-1 ${
              page === totalPages
                ? "bg-blue-500 text-white"
                : "hover:bg-gray-100"
            }`}
          >
            {totalPages}
          </button>
        );
      }
    }

    return pages;
  };

  return (
    <div
      className=" self-end
    flex flex-col items-center w-full max-w-xl  mx-auto"
    >
      {/* Search results message */}
      <div className="text-sm text-gray-600 mb-4  text-center self-center">
        Page {page} of about {totalPages} pages
      </div>

      {/* Pagination component */}
      <div className="flex items-center justify-center py-4 w-full">
        {/* Previous button */}
        <button
          onClick={() => page > 1 && handlePageChange(page - 1)}
          disabled={page === 1}
          className={`flex items-center justify-center h-8 px-3 mr-2 rounded-full ${
            page === 1 ? "text-gray-300" : "text-blue-500 hover:bg-gray-100"
          }`}
        >
          <ChevronLeft size={16} />
          <span className="ml-1">Previous</span>
        </button>

        {/* Page numbers */}
        <div className="flex items-center">{renderPageNumbers()}</div>

        {/* Next button */}
        <button
          onClick={() => page < totalPages && handlePageChange(page + 1)}
          disabled={page === totalPages}
          className={`flex items-center justify-center h-8 px-3 ml-2 rounded-full ${
            page === totalPages
              ? "text-gray-300"
              : "text-blue-500 hover:bg-gray-100"
          }`}
        >
          <span className="mr-1">Next</span>
          <ChevronRight size={16} />
        </button>
      </div>
    </div>
  );
}
