import { useSearchParams } from "react-router-dom";
import SearchUnit from "../components/SearchUnit";
import { useEffect, useState } from "react";
import SearchResults from "../components/SearchResults";

const Results = () => {
  const [searchParams] = useSearchParams();
  const query = searchParams.get("query");
  const [searchResults, setSearchResults] = useState([
    {
      title: "Example Search Result",
      description:
        "This is a description of the search result with relevant information highlighted...",
      url: "https://example.com/page",
    },
    {
      title: "React useState Guide",
      description:
        "Learn how to use the useState hook in React to manage component state effectively.",
      url: "https://reactjs.org/docs/hooks-state.html",
    },
    {
      title: "JavaScript Array Methods",
      description:
        "Comprehensive guide on JavaScript array methods including map, filter, and reduce.",
      url: "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array",
    },
    {
      title: "Understanding Async/Await",
      description:
        "Async/await makes it easier to write promises in JavaScript. Learn how it works.",
      url: "https://javascript.info/async-await",
    },
  ]);

  return (
    <>
      <div className="flex items-center justify-center gap-24">
        <img
          src="./src/assets/Falcony_logo.png"
          alt="Logo"
          className="w-[92px] h-[92px]"
        />
        <SearchUnit />
      </div>
      <SearchResults results={searchResults} />
    </>
  );
};

export default Results;
