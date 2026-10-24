import {getApiUrl} from "../config/api.ts";

const API_URL = getApiUrl();

export const fetchTicketById = async (id: string) => {
    const res = await fetch(`${API_URL}/ticket/${id}`);
    return await res.json();
}

export const fetchTicketByStatus = async () => {
    const res = await fetch(`${API_URL}/ticket/`);
    return await res.json();
}