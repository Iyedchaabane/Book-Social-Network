import {Component, OnDestroy, OnInit} from '@angular/core';
import {PageResponseBorrowedBookResponse} from "../../../../services/models/page-response-borrowed-book-response";
import {FeedbackRequest} from "../../../../services/models/feedback-request";
import {BookService} from "../../../../services/services/book.service";
import {FeedBackService} from "../../../../services/services/feed-back.service";
import {BorrowedBookResponse} from "../../../../services/models/borrowed-book-response";
import {ToastrService} from "ngx-toastr";
import {SearchService} from "../../../../services/search/search.service";
import {Subscription} from "rxjs";

@Component({
  selector: 'app-borrowed-book-list',
  templateUrl: './borrowed-book-list.component.html',
  styleUrl: './borrowed-book-list.component.scss'
})
export class BorrowedBookListComponent implements OnInit, OnDestroy {
  page = 0;
  size = 6;
  pages: any = [];
  borrowedBooks: PageResponseBorrowedBookResponse = {};
  allBorrowedBooks: BorrowedBookResponse[] = [];
  filteredBorrowedBooks: BorrowedBookResponse[] = [];
  selectedBook: BorrowedBookResponse | undefined = undefined;
  feedbackRequest: FeedbackRequest = { bookId: 0, comment: '', note: 0 };
  isSearching = false;
  private searchSub!: Subscription;

  constructor(
    private bookService: BookService,
    private feedbackService: FeedBackService,
    private toastService: ToastrService,
    protected searchService: SearchService
  ) {}

  ngOnInit(): void {
    this.loadAllBorrowedBooks();
    this.findAllBorrowedBooks();

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

  // Load all borrowed books for search
  private loadAllBorrowedBooks() {
    this.bookService.findAllBorrowedBooks({ page: 0, size: 10000 })
      .subscribe({
        next: (resp) => {
          this.allBorrowedBooks = resp.content ?? [];
          this.searchService.setBooks(this.allBorrowedBooks);
        },
        error: (err) => {
          console.error('Error loading all borrowed books:', err);
          this.allBorrowedBooks = this.borrowedBooks.content ?? [];
          this.searchService.setBooks(this.allBorrowedBooks);
        }
      });
  }

  private findAllBorrowedBooks() {
    this.bookService.findAllBorrowedBooks({ page: this.page, size: this.size })
      .subscribe({
        next: (resp) => {
          this.borrowedBooks = resp;
          this.pages = Array(this.borrowedBooks.totalPages)
            .fill(0)
            .map((x, i) => i);

          if (!this.isSearching) {
            this.filteredBorrowedBooks = [...(this.borrowedBooks.content ?? [])];
          }
        }
      });
  }

  private applyFilter(query: string) {
    if (!query?.trim()) {
      this.isSearching = false;
      this.filteredBorrowedBooks = [...(this.borrowedBooks.content ?? [])];
    } else {
      this.isSearching = true;
      const q = query.toLowerCase();
      this.filteredBorrowedBooks = this.allBorrowedBooks.filter(book =>
        (book.title?.toLowerCase().includes(q)) ||
        (book.authorName?.toLowerCase().includes(q))
      );
    }
  }

  gotToPage(page: number) {
    this.page = page;
    this.findAllBorrowedBooks();
  }

  goToFirstPage() {
    this.page = 0;
    this.findAllBorrowedBooks();
  }

  goToPreviousPage() {
    this.page--;
    this.findAllBorrowedBooks();
  }

  goToLastPage() {
    this.page = (this.borrowedBooks.totalPages as number) - 1;
    this.findAllBorrowedBooks();
  }

  goToNextPage() {
    this.page++;
    this.findAllBorrowedBooks();
  }

  get isLastPage() {
    return this.page === (this.borrowedBooks.totalPages as number) - 1;
  }

  returnBorrowedBook(book: BorrowedBookResponse) {
    this.selectedBook = book;
    this.feedbackRequest.bookId = book.id as number;
  }

  returnBook(withFeedback: boolean) {
    this.bookService.returnBorrowBook({ 'book-id': this.selectedBook?.id as number })
      .subscribe({
        next: () => {
          if (withFeedback) this.giveFeedback();
          this.toastService.success('Book has been returned and the owner is notified', 'Success');
          this.selectedBook = undefined;
          this.findAllBorrowedBooks();
        }
      });
  }

  private giveFeedback() {
    this.feedbackService.saveFeedback({ body: this.feedbackRequest })
      .subscribe();
  }
}
