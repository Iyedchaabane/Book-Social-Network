import { Component } from '@angular/core';
import {Router} from "@angular/router";
import {AuthenticationService} from "../../services/services/authentication.service";

@Component({
  selector: 'app-forget-password',
  templateUrl: './forget-password.component.html',
  styleUrl: './forget-password.component.scss'
})
export class ForgetPasswordComponent {
  email: string = '';
  errorMsg: string[] = [];
  successMsg: string[] = [];

  constructor(
    private router: Router,
    private authService: AuthenticationService
  ) {}

  sendResetLink() {
    this.errorMsg = [];
    this.successMsg = [];

    if (!this.email.trim()) {
      this.errorMsg.push('Email is required');
      return;
    }

    this.authService.forgotPassword({ body: { email: this.email } }).subscribe({
      next: (res: any) => {
        // Ici, res.message contient le message envoyé par le backend
        this.successMsg.push(res.message || 'A reset link has been sent to your email.');

        this.router.navigate(['/activate-account'], {
          queryParams: { context: 'reset' }
        });
      },
      error: (err) => {
        // Ici, err.error.message ou err.error.error contient le message d'erreur du backend
        const backendMsg = err?.error?.message || err?.error?.error;
        this.errorMsg.push(backendMsg || 'No account found with this email.');
      }
    });
  }

  backToLogin() {
    this.router.navigate(['/login']);
  }
}
