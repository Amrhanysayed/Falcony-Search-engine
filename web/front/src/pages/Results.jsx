import { Link, useSearchParams } from "react-router-dom";
import SearchUnit from "../components/SearchUnit";
import { useState } from "react";
import SearchResults from "../components/SearchResults";
import Pagination from "../components/Pagination";
import Footer from "../components/Footer";
import SettingButton from "../components/SettingButton";
import Settings from "../components/Settings";
import { useSettings } from "../context/SettingsContext";

const Results = () => {
  const [searchParams] = useSearchParams();
  const [page, setPage] = useState(1); /// i will send the total number of pages to compoent
  const query = searchParams.get("query");
  console.log(query);

  const { themeColor } = useSettings();

  const totalPages = 10; // to be change to the length of the results array

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

  const [isSettingsOpen, setIsSettingsOpen] = useState(false);

  return (
    <>
      <div
        className="  min-h-screen "
        style={{ backgroundColor: themeColor + "20" }}
      >
        <div className="flex items-center justify-center gap-24">
          <Link to="/">
            <img
              src="./src/assets/Falcony_logo.png"
              alt="Logo"
              className="w-[92px] h-[92px]"
            />
          </Link>
          <SearchUnit />
        </div>

        <div className=" flex flex-col mt-4  gap-12">
          <SearchResults results={searchResults} />

          <Pagination page={page} setPage={setPage} totalPages={totalPages} />
        </div>
      </div>

      <SettingButton
        setIsSettingsOpen={setIsSettingsOpen}
        isSettingsOpen={isSettingsOpen}
      />

      {isSettingsOpen && <Settings setIsSettingsOpen={setIsSettingsOpen} />}

      <Footer />
    </>
  );
};

export default Results;
