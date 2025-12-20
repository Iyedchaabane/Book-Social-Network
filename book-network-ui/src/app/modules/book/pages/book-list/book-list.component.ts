import {Component, OnDestroy, OnInit} from '@angular/core';
import {BookService} from "../../../../services/services/book.service";
import {Router} from "@angular/router";
import {PageResponseBookResponse} from "../../../../services/models/page-response-book-response";
import {BookResponse} from "../../../../services/models/book-response";
import {ToastrService} from "ngx-toastr";
import {Subscription} from "rxjs";
import {SearchService} from "../../../../services/search/search.service";

@Component({
  selector: 'app-book-list',
  templateUrl: './book-list.component.html',
  styleUrl: './book-list.component.scss'
})
export class BookListComponent implements OnInit, OnDestroy {
  bookResponse: PageResponseBookResponse = {};
  filteredBooks: BookResponse[] = [];
  allBooks: BookResponse[] = []; // Store all books from all pages
  page = 0;
  size = 6;
  pages: any = [];
  message = '';
  level: 'success' |'error' = 'success';
  isSearching = false; // Track if we're in search mode
  private searchSub!: Subscription;

  constructor(
    private bookService: BookService,
    private router: Router,
    private toastService: ToastrService,
    protected searchService: SearchService
  ) {}

  ngOnInit(): void {
    this.loadAllBooks(); // Load all books initially
    this.findAllBooks(); // Load first page for display

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

  // Load all books from all pages for searching
  private loadAllBooks() {
    this.bookService.findAllBooks({
      page: 0,
      size: 10000 // Set a large size to get all books at once
    }).subscribe({
      next: (response) => {
        this.allBooks = response.content || [];
        this.searchService.setBooks(this.allBooks);
      },
      error: (err) => {
        console.error('Error loading all books:', err);
        // Fallback: use the current page books
        this.allBooks = this.bookResponse.content || [];
        this.searchService.setBooks(this.allBooks);
      }
    });
  }

  private findAllBooks() {
    this.bookService.findAllBooks({
      page: this.page,
      size: this.size
    }).subscribe({
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
    if (!query || query.trim() === '') {
      // If search is empty, show current page and disable search mode
      this.isSearching = false;
      this.filteredBooks = [...(this.bookResponse.content ?? [])];
      this.message = '';
    } else {
      this.isSearching = true;
      const q = query.toLowerCase();

      this.filteredBooks = this.allBooks.filter(book =>
        (book.title?.toLowerCase().includes(q)) ||
        (book.synopsis?.toLowerCase().includes(q))
      );

      this.message = this.filteredBooks.length > 0
        ? ``
        : `No books found matching "${query}"`;
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

  borrowBook(book: BookResponse) {
    this.message = '';
    this.level = 'success';
    this.bookService.borrowBook({
      'book-id': book.id as number
    }).subscribe({
      next: () => {
        this.toastService.success('Book successfully added to your list', 'Done!');
      },
      error: (err) => {
        this.toastService.error(err.error.error, 'Oups!!');
      }
    });
  }

  displayBookDetails(book: BookResponse) {
    this.router.navigate(['books', 'details', book.id]);
  }

  addToWaitingBookList(book: BookResponse) {
    this.message = '';
    this.level = 'success';
    this.bookService.reserveBook({
      'book-id': book.id as number
    }).subscribe({
      next: () => {
        this.toastService.success('Book reserved successfully', 'Done!');
      },
      error: (err) => {
        this.toastService.error(err.error.error, 'Oups!!');
      }
    });
  }
}
