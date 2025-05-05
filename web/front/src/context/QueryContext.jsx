import React, { createContext, useContext, useState } from "react";

const QueryContext = createContext();

export const QueryProvider = ({ children }) => {
  const [searchQuery, setSearchQuery] = useState("");
  const [file, setFile] = useState(null);

  return (
    <QueryContext.Provider
      value={{
        searchQuery,
        setSearchQuery,
        file,
        setFile,
      }}
    >
      {children}
    </QueryContext.Provider>
  );
};

export const useQuery = () => useContext(QueryContext);
