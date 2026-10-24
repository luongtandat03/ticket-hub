export interface ApiResponse<T> {
    status: number,
    message: string,
    data: T,
    timestamp: number
};

export interface PageResponse<T> {
    content: T[],
    nextCursor: string,
    hasMore: boolean
}