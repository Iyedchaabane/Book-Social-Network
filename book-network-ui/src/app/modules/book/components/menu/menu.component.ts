import {Component, OnInit} from '@angular/core';
import {TokenService} from "../../../../services/token/token.service";
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import {Notification} from "./notification";
import {ToastrService} from "ngx-toastr";
import {SearchService} from "../../../../services/search/search.service";
import {UserControllerService} from "../../../../services/services/user-controller.service";
import {NotificationResponse} from "../../../../services/models/notification-response";

@Component({
  selector: 'app-menu',
  templateUrl: './menu.component.html',
  styleUrl: './menu.component.scss'
})
export class MenuComponent implements OnInit {

  fullName: string | null = null;
  socketClient: any = null;
  private notificationSubscription: any;
  unreadNotificationCount = 0;
  notifications: Array<NotificationResponse> = [];
  searchQuery: string = '';



  constructor(
    private tokenService: TokenService,
    private toastService: ToastrService,
    private searchService: SearchService,
    private notificationService: UserControllerService
  ) {}

  ngOnInit(): void {
    this.fullName = this.tokenService.fullName;

    // 1️⃣ Charger notifications existantes depuis la DB
    this.notificationService.getUserNotifications().subscribe(data => {
      this.notifications = data;
      this.unreadNotificationCount = data.filter(n => !n.read).length;
    });

    // 2️⃣ Connecter WebSocket pour nouvelles notifications
    if (this.tokenService.token) {
      const ws = new SockJS('http://localhost:8088/api/v1/ws');
      this.socketClient = Stomp.over(ws);

      this.socketClient.connect(
        { Authorization: 'Bearer ' + this.tokenService.token },
        () => {
          const uid = this.tokenService.userId;
          if (uid) {
            this.notificationSubscription = this.socketClient.subscribe(
              `/user/${uid}/notifications`,
              (message: any) => {
                const notification: Notification = JSON.parse(message.body);
                this.notifications.unshift(notification);
                this.unreadNotificationCount++;
              }
            );
          }
        }
      );
    }
  }

  private navigationHandler() {
    const linkColor = document.querySelectorAll('.nav-link');
    linkColor.forEach(link => {
      if(window.location.href.endsWith(link.getAttribute('href') || '')) {
        link.classList.add('active');
      }
      link.addEventListener('click', () => {
        linkColor.forEach(l => l.classList.remove('active'));
        link.classList.add('active');
      });
    });
  }
  logout() {
    localStorage.removeItem('token');
    window.location.reload();
  }

  onSearchChange(): void {
    this.searchService.updateSearch(this.searchQuery);
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.searchService.updateSearch('');
  }

  markNotificationAsRead(notification: NotificationResponse) {
    if (!notification.read && notification.id != null) {
      this.notificationService.markAsRead({ id: notification.id }).subscribe(updated => {
        notification.read = true;
        this.updateUnreadCount();
      });
    }
  }

  markAllNotificationsAsRead() {
    this.notificationService.markAllAsRead().subscribe(() => {
      this.notifications.forEach(n => n.read = true);
      this.updateUnreadCount();
    });
  }

  private updateUnreadCount() {
    this.unreadNotificationCount = this.notifications.filter(n => !n.read).length;
  }
}
