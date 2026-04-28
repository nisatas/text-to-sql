import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { QueryPage } from './features/query/query-page/query-page';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, QueryPage, ],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly title = signal('text-to-sql-frontend');
}
