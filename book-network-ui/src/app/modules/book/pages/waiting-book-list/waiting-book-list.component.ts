import {Component, OnDestroy, OnInit} from '@angular/core';
import {PageResponseBookResponse} from "../../../../services/models/page-response-book-response";
import {BookResponse} from "../../../../services/models/book-response";
import {Subscription} from "rxjs";
import {BookService} from "../../../../services/services/book.service";
import {Router} from "@angular/router";
import {ToastrService} from "ngx-toastr";
import {SearchService} from "../../../../services/search/search.service";

@Component({
  selector: 'app-waiting-book-list',
  templateUrl: './waiting-book-list.component.html',
  styleUrl: './waiting-book-list.component.scss'
})
export class WaitingBookListComponent implements OnInit, OnDestroy {
  bookResponse: PageResponseBookResponse = {};
  filteredBooks: BookResponse[] = [];
  allBooks: BookResponse[] = []; // all reserved books (for search)
  page = 0;
  size = 6;
  pages: any = [];
  message = '';
  level: 'success' | 'error' = 'success';
  isSearching = false;
  private searchSub!: Subscription;

  constructor(
    private readonly bookService: BookService,
    private readonly router: Router,
    private readonly toastService: ToastrService,
    protected searchService: SearchService
  ) {}

  ngOnInit(): void {
    this.loadAllReservations();
    this.findAllReservations();

    // react to search query changes
    this.searchSub = this.searchService.currentQuery.subscribe(query => {
      this.applyFilter(query);
    });
  }

  ngOnDestroy(): void {
    if (this.searchSub) {
      this.searchSub.unsubscribe();
    }
  }

  /** Load all reservations (for global search) */
  private loadAllReservations() {
    this.bookService.getMyReservations({
      page: 0,
      size: 10000
    }).subscribe({
      next: (response) => {
        this.allBooks = response.content || [];
        this.searchService.setBooks(this.allBooks);
      },
      error: (err) => {
        console.error('Error loading all reservations:', err);
        this.allBooks = this.bookResponse.content || [];
        this.searchService.setBooks(this.allBooks);
      }
    });
  }

  /** Load reservations with pagination */
  private findAllReservations() {
    this.bookService.getMyReservations({
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

  /** Apply search filter */
  private applyFilter(query: string) {
    if (!query || query.trim() === '') {
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
        : `No reserved books found matching "${query}"`;
    }
  }

  /** Pagination helpers */
  gotToPage(page: number) {
    this.page = page;
    this.findAllReservations();
  }

  goToFirstPage() {
    this.page = 0;
    this.findAllReservations();
  }

  goToPreviousPage() {
    this.page--;
    this.findAllReservations();
  }

  goToLastPage() {
    this.page = (this.bookResponse.totalPages as number) - 1;
    this.findAllReservations();
  }

  goToNextPage() {
    this.page++;
    this.findAllReservations();
  }

  get isLastPage() {
    return this.page === (this.bookResponse.totalPages as number) - 1;
  }
  /** Navigate to details */
  displayBookDetails(book: BookResponse) {
    this.router.navigate(['books', 'details', book.id]);
  }

  removeBook(book: BookResponse) {
    this.message = '';
    this.level = 'success';
    this.bookService.cancelReservation({
      'book-id': book.id as number
    }).subscribe({
      next: () => {
        this.toastService.success('Reservation cancelled successfully', 'Done!');
        this.findAllReservations();
        this.loadAllReservations();
      },
      error: (err) => {
        this.toastService.error(err.error.error, 'Oups!!');
      }
    });
  }

}
