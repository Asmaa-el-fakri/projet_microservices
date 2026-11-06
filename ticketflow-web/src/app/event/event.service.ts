import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config';
import { EventFilters, EventItem, Page } from './event.model';

@Injectable({ providedIn: 'root' })
export class EventService {
  private readonly http = inject(HttpClient);

  search(f: EventFilters): Observable<Page<EventItem>> {
    let params = new HttpParams()
      .set('sort', f.sort)
      .set('direction', f.direction)
      .set('page', f.page)
      .set('size', f.size);

    const optional: Record<string, string | number | null | undefined> = {
      q: f.q?.trim(),
      city: f.city?.trim(),
      category: f.category?.trim(),
      dateFrom: f.dateFrom,
      dateTo: f.dateTo,
      minPrice: f.minPrice,
      maxPrice: f.maxPrice,
    };
    for (const [key, value] of Object.entries(optional)) {
      if (value !== null && value !== undefined && value !== '') {
        params = params.set(key, value);
      }
    }
    return this.http.get<Page<EventItem>>(`${API_BASE_URL}/events`, { params });
  }

  get(id: number): Observable<EventItem> {
    return this.http.get<EventItem>(`${API_BASE_URL}/events/${id}`);
  }
}
