import {Component, OnInit} from '@angular/core';
import {ResetPasswordRequest} from "../../services/models/reset-password-request";
import {ActivatedRoute, Router} from "@angular/router";
import {AuthenticationService} from "../../services/services/authentication.service";
import {TokenService} from "../../services/token/token.service";

@Component({
  selector: 'app-reset-password',
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.scss'
})
export class ResetPasswordComponent implements OnInit {

  resetRequest: ResetPasswordRequest = { token: '', newPassword: '', confirmPassword: '' };
  message = '';
  isOkay = true;
  submitted = false;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private authService: AuthenticationService,
  ) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.resetRequest.token = params['token'];
    });
  }

  resetPassword() {
    if (!this.resetRequest.newPassword || !this.resetRequest.confirmPassword) {
      this.message = "Please fill both password fields";
      this.isOkay = false;
      this.submitted = true;
      return;
    }

    this.authService.resetPassword({ body: this.resetRequest }).subscribe({
      next: () => {
        this.message = "Password updated successfully! You can now login.";
        this.isOkay = true;
        this.submitted = true;
        //setTimeout(() => this.router.navigate(['login']), 2000);
      },
      error: (err) => {
        this.message = err?.error?.error || "Something went wrong";
        this.isOkay = false;
        this.submitted = true;
      }
    });
  }

  backToLogin() {
    this.router.navigate(['login']);
  }

  tryAgain() {
    this.submitted = false;
    this.message = '';
  }
}
