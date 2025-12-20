import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class SearchService {

  private searchQuery = new BehaviorSubject<string>('');
  currentQuery = this.searchQuery.asObservable();

  private books: any[] = [];

  updateSearch(query: string): void {
    this.searchQuery.next(query.toLowerCase().trim());
  }

  getFilteredBooks(query: string): any[] {
    if (!query) return this.books;

    return this.books.filter(book => {
      return book.title.toLowerCase().includes(query) ||
        book.description.toLowerCase().includes(query);
    });
  }

  setBooks(books: any[]): void {
    this.books = books;
  }
}
