export const getApiUrl = () => {
    const hostname = window.location.hostname;

    if (hostname == 'localhost') {
        return import.meta.env.VITE_API_URL;
    }
}