export type EventStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED' | 'ARCHIVED';

export interface EventItem {
  id: number;
  title: string;
  summary: string;
  eventDate: string;
  city: string;
  venue: string;
  category: string;
  price: number;
  capacity: number;
  status: EventStatus;
}

// Forme de la réponse paginée de Spring Data
export interface Page<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

export interface EventFilters {
  q?: string;
  city?: string;
  category?: string;
  dateFrom?: string;
  dateTo?: string;
  minPrice?: number | null;
  maxPrice?: number | null;
  sort: string;
  direction: 'asc' | 'desc';
  page: number;
  size: number;
}
