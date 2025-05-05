import axios from "axios";

const falcony_api = axios.create({
  baseURL: "http://localhost:8080",
});

export default falcony_api;
