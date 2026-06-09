export interface PaginatedResponse<T> {
  items: T[];
  currentPage: number;
  pageSize: number;
  totalPages: number;
  totalItems: number;
}
