import React, { createContext, useContext, useState } from "react";

const QueryContext = createContext();

export const QueryProvider = ({ children }) => {
  const [searchQuery, setSearchQuery] = useState("");

  return (
    <QueryContext.Provider
      value={{
        searchQuery,
        setSearchQuery,
      }}
    >
      {children}
    </QueryContext.Provider>
  );
};

export const useQuery = () => useContext(QueryContext);
