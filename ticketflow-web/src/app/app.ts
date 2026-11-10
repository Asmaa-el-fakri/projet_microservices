import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SiteHeader } from './layout/site-header';
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, SiteHeader],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected readonly title = signal('ticketflow-web');
}
