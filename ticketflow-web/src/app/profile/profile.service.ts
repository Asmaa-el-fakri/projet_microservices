import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config';

export interface Profile {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
}

export interface ProfileUpdate {
  firstName: string;
  lastName: string;
  phone: string | null;
}

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly http = inject(HttpClient);

  getMe(): Observable<Profile> {
    return this.http.get<Profile>(`${API_BASE_URL}/customers/me`);
  }

  updateMe(body: ProfileUpdate): Observable<Profile> {
    return this.http.put<Profile>(`${API_BASE_URL}/customers/me`, body);
  }

  requestClosure(): Observable<void> {
    return this.http.post<void>(`${API_BASE_URL}/customers/me/close`, null);
  }
}
