import axios from "axios";
import {getApiUrl} from "../config/api.ts";


const api = axios.create({
    baseURL: getApiUrl(),
    headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${localStorage.getItem("token")}`,
    },
});

export default api;