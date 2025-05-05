import { Link, useSearchParams } from "react-router-dom";
import SearchUnit from "../components/SearchUnit";
import { useEffect, useState } from "react";
import SearchResults from "../components/SearchResults";
import Pagination from "../components/Pagination";
import Footer from "../components/Footer";
import SettingButton from "../components/SettingButton";
import Settings from "../components/Settings";
import { useSettings } from "../context/SettingsContext";
import falcony_api from "../axiosInstance";
import Loading from "../components/Loading";

const Results = () => {
  const [searchParams] = useSearchParams();
  const [page, setPage] = useState(1); /// i will send the total number of pages to compoent
  const [timeTaken, setTimeTaken] = useState(0); // to be changed to the time taken for the results
  const query = searchParams.get("query");

  const { themeColor } = useSettings();
  const [loading, setloading] = useState(false);

  const totalPages = 10; // to be change to the length of the results array

  const [searchResults, setSearchResults] = useState([]);

  useEffect(() => {
    const fetchResults = async () => {
      setloading(true);
      setTimeTaken(0);
      const startTime = Date.now(); // Start time
      try {
        const response = await falcony_api.get(`/search?query=${query}`);
        console.log(response.data);
        setSearchResults(response.data);
      } catch (error) {
        console.error("Error fetching search results:", error);
      } finally {
        setloading(false);
        // Convert milliseconds to seconds
        setTimeTaken((Date.now() - startTime) / 1000);
      }
    };

    fetchResults();
  }, [query]);

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

        <div className=" flex flex-col mt-4 lg:px-[100px] gap-12">
          {loading ? (
            <Loading />
          ) : (
            <>
              <SearchResults results={searchResults} timeTaken={timeTaken} />

              <Pagination
                page={page}
                setPage={setPage}
                totalPages={totalPages}
              />
            </>
          )}
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
