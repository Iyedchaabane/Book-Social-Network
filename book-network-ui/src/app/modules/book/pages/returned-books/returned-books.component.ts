import {Component, OnDestroy, OnInit} from '@angular/core';
import {PageResponseBorrowedBookResponse} from "../../../../services/models/page-response-borrowed-book-response";
import {BookService} from "../../../../services/services/book.service";
import {BorrowedBookResponse} from "../../../../services/models/borrowed-book-response";
import {ToastrService} from "ngx-toastr";
import {Subscription} from "rxjs";
import {SearchService} from "../../../../services/search/search.service";

@Component({
  selector: 'app-returned-books',
  templateUrl: './returned-books.component.html',
  styleUrl: './returned-books.component.scss'
})

export class ReturnedBooksComponent implements OnInit, OnDestroy {
  page = 0;
  size = 6;
  pages: any = [];
  returnedBooks: PageResponseBorrowedBookResponse = {};
  allReturnedBooks: BorrowedBookResponse[] = [];
  filteredReturnedBooks: BorrowedBookResponse[] = [];
  message = '';
  level: 'success' | 'error' = 'success';
  isSearching = false;
  private searchSub!: Subscription;

  constructor(
    private bookService: BookService,
    private toastService: ToastrService,
    protected searchService: SearchService
  ) {}

  ngOnInit(): void {
    this.loadAllReturnedBooks();
    this.findAllReturnedBooks();

    this.searchSub = this.searchService.currentQuery.subscribe(query => {
      this.applyFilter(query);
    });
  }

  ngOnDestroy(): void {
    if (this.searchSub) {
      this.searchSub.unsubscribe();
    }
  }

  private loadAllReturnedBooks() {
    this.bookService.findAllReturnedBooks({ page: 0, size: 10000 })
      .subscribe({
        next: (resp) => {
          this.allReturnedBooks = resp.content ?? [];
          this.searchService.setBooks(this.allReturnedBooks);
        },
        error: (err) => {
          console.error('Error loading returned books:', err);
          this.allReturnedBooks = this.returnedBooks.content ?? [];
          this.searchService.setBooks(this.allReturnedBooks);
        }
      });
  }

  private findAllReturnedBooks() {
    this.bookService.findAllReturnedBooks({ page: this.page, size: this.size })
      .subscribe({
        next: (resp) => {
          this.returnedBooks = resp;
          this.pages = Array(this.returnedBooks.totalPages)
            .fill(0)
            .map((x, i) => i);

          if (!this.isSearching) {
            this.filteredReturnedBooks = [...(this.returnedBooks.content ?? [])];
          }
        }
      });
  }

  private applyFilter(query: string) {
    if (!query?.trim()) {
      this.isSearching = false;
      this.filteredReturnedBooks = [...(this.returnedBooks.content ?? [])];
    } else {
      this.isSearching = true;
      const q = query.toLowerCase();
      this.filteredReturnedBooks = this.allReturnedBooks.filter(book =>
        (book.title?.toLowerCase().includes(q)) ||
        (book.authorName?.toLowerCase().includes(q))
      );
    }
  }

  gotToPage(page: number) {
    this.page = page;
    this.findAllReturnedBooks();
  }

  goToFirstPage() {
    this.page = 0;
    this.findAllReturnedBooks();
  }

  goToPreviousPage() {
    this.page--;
    this.findAllReturnedBooks();
  }

  goToLastPage() {
    this.page = (this.returnedBooks.totalPages as number) - 1;
    this.findAllReturnedBooks();
  }

  goToNextPage() {
    this.page++;
    this.findAllReturnedBooks();
  }

  get isLastPage() {
    return this.page === (this.returnedBooks.totalPages as number) - 1;
  }

  approveBookReturn(book: BorrowedBookResponse) {
    if (!book.returned) return;

    this.bookService.approveReturnBorrowBook({ 'book-id': book.id as number })
      .subscribe({
        next: () => {
          this.toastService.success('Book return approved', 'Done!');
          this.findAllReturnedBooks();
        }
      });
  }
}
