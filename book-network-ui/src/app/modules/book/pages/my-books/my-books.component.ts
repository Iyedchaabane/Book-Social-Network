import {Component, OnDestroy, OnInit} from '@angular/core';
import {PageResponseBookResponse} from "../../../../services/models/page-response-book-response";
import {BookService} from "../../../../services/services/book.service";
import {Router} from "@angular/router";
import {BookResponse} from "../../../../services/models/book-response";
import { Subscription } from 'rxjs';
import {SearchService} from "../../../../services/search/search.service";

@Component({
  selector: 'app-my-books',
  templateUrl: './my-books.component.html',
  styleUrl: './my-books.component.scss'
})
export class MyBooksComponent implements OnInit, OnDestroy {
  bookResponse: PageResponseBookResponse = {};
  allBooks: BookResponse[] = [];
  filteredBooks: BookResponse[] = [];
  page = 0;
  size = 3;
  pages: any = [];
  isSearching = false;
  private searchSub!: Subscription;

  constructor(
    private bookService: BookService,
    private router: Router,
    protected searchService: SearchService
  ) {}

  ngOnInit(): void {
    this.loadAllBooks();
    this.findAllBooks();

    // Subscribe to search query changes
    this.searchSub = this.searchService.currentQuery.subscribe(query => {
      this.applyFilter(query);
    });
  }

  ngOnDestroy(): void {
    if (this.searchSub) {
      this.searchSub.unsubscribe();
    }
  }

  private loadAllBooks() {
    this.bookService.findAllBooksByOwner({ page: 0, size: 10000 })
      .subscribe({
        next: (response) => {
          this.allBooks = response.content ?? [];
          this.searchService.setBooks(this.allBooks);
        },
        error: (err) => {
          console.error('Error loading all books:', err);
          this.allBooks = this.bookResponse.content ?? [];
          this.searchService.setBooks(this.allBooks);
        }
      });
  }

  private findAllBooks() {
    this.bookService.findAllBooksByOwner({ page: this.page, size: this.size })
      .subscribe({
        next: (books) => {
          this.bookResponse = books;
          this.pages = Array(this.bookResponse.totalPages)
            .fill(0)
            .map((x, i) => i);

          if (!this.isSearching) {
            this.filteredBooks = [...(this.bookResponse.content ?? [])];
          }
        }
      });
  }

  private applyFilter(query: string) {
    if (!query?.trim()) {
      this.isSearching = false;
      this.filteredBooks = [...(this.bookResponse.content ?? [])];
    } else {
      this.isSearching = true;
      const q = query.toLowerCase();
      this.filteredBooks = this.allBooks.filter(book =>
        (book.title?.toLowerCase().includes(q)) ||
        (book.synopsis?.toLowerCase().includes(q))
      );
    }
  }

  gotToPage(page: number) {
    this.page = page;
    this.findAllBooks();
  }

  goToFirstPage() {
    this.page = 0;
    this.findAllBooks();
  }

  goToPreviousPage() {
    this.page--;
    this.findAllBooks();
  }

  goToLastPage() {
    this.page = (this.bookResponse.totalPages as number) - 1;
    this.findAllBooks();
  }

  goToNextPage() {
    this.page++;
    this.findAllBooks();
  }

  get isLastPage() {
    return this.page === (this.bookResponse.totalPages as number) - 1;
  }

  archiveBook(book: BookResponse) {
    this.bookService.updateArchivedStatus({ 'book-id': book.id as number })
      .subscribe(() => book.archived = !book.archived);
  }

  shareBook(book: BookResponse) {
    this.bookService.updateShareableStatus({ 'book-id': book.id as number })
      .subscribe(() => book.shareable = !book.shareable);
  }

  editBook(book: BookResponse) {
    this.router.navigate(['books', 'manage', book.id]);
  }
}
