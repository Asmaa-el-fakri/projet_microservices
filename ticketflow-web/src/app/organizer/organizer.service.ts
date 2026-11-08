import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config';
import { EventItem, EventStatus, Page } from '../event/event.model';

export interface EventPayload {
  title: string;
  summary: string;
  eventDate: string;
  city: string;
  venue: string;
  category: string;
  price: number;
  capacity: number;
}

@Injectable({ providedIn: 'root' })
export class OrganizerService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE_URL}/events`;

  list(status: EventStatus | '', page: number, size: number): Observable<Page<EventItem>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<Page<EventItem>>(`${this.base}/manage`, { params });
  }

  get(id: number): Observable<EventItem> {
    return this.http.get<EventItem>(`${this.base}/manage/${id}`);
  }

  create(body: EventPayload): Observable<EventItem> {
    return this.http.post<EventItem>(this.base, body);
  }

  update(id: number, body: EventPayload): Observable<EventItem> {
    return this.http.put<EventItem>(`${this.base}/${id}`, body);
  }

  publish(id: number): Observable<EventItem> {
    return this.http.post<EventItem>(`${this.base}/${id}/publish`, null);
  }

  close(id: number): Observable<EventItem> {
    return this.http.post<EventItem>(`${this.base}/${id}/close`, null);
  }

  archive(id: number): Observable<EventItem> {
    return this.http.post<EventItem>(`${this.base}/${id}/archive`, null);
  }
}
