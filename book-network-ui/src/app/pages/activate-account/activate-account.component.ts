import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {AuthenticationService} from "../../services/services/authentication.service";
import {CodeVerificationRequest} from "../../services/models/code-verification-request";

@Component({
  selector: 'app-activate-account',
  templateUrl: './activate-account.component.html',
  styleUrl: './activate-account.component.scss'
})
export class ActivateAccountComponent implements OnInit{

  message:string = '';
  isOkay:boolean = true;
  submitted:boolean = false;
  codeRequest: CodeVerificationRequest = {token: ''};
  context: 'activation' | 'reset' = 'activation';


  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthenticationService
  ) {
  }

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.context = params['context'] || 'activation';
    });
  }
  onCodeCompleted(token: string) {
    this.codeRequest.token = token;

    if (this.context === 'activation') {
      this.confirmActivation();
    } else {
      this.verifyResetToken();
    }
  }

  private confirmActivation() {
    console.log('Request sent:', this.codeRequest);

    this.authService.activateAccount({ body: this.codeRequest }).subscribe({
      next: (res) => {
        console.log('Backend response:', res);
        this.message = 'Your account has been successfully activated.\nNow you can proceed to login';
        this.submitted = true;
        this.isOkay = true;
      },
      error: (err) => {
        console.error('Backend error:', err);
        this.message = err?.error?.error || 'Token has expired or is invalid';
        this.submitted = true;
        this.isOkay = false;
      }
    });
  }

  private verifyResetToken() {
    this.authService.verifyResetCode({  body: this.codeRequest }).subscribe({
      next: (res: any) => {
        this.message = 'Code verified! You can now reset your password.';
        this.submitted = true;
        this.isOkay = true;
      },
      error: (err) => {
        this.message = err?.error?.error || 'Invalid or expired token';
        this.submitted = true;
        this.isOkay = false;
      }
    });
  }

  goToResetPassword() {
    this.router.navigate(['/reset-password'], {
      queryParams: { token: this.codeRequest.token }
    });
  }
}
