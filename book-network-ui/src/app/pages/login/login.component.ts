import { Component } from '@angular/core';
import {AuthenticationRequest} from "../../services/models/authentication-request";
import {Router} from "@angular/router";
import {AuthenticationService} from "../../services/services/authentication.service";
import {error} from "@angular/compiler-cli/src/transformers/util";
import {TokenService} from "../../services/token/token.service";
import {ToastrService} from "ngx-toastr";

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {

  authRequest : AuthenticationRequest = {email: '', password: ''};
  errorMsg: Array<string> = [];

  constructor(
    private router: Router,
    private authService: AuthenticationService,
    private tokenService: TokenService,
    private toastr: ToastrService
  ) {
  }

  login() {
    this.errorMsg = [];
    this.authService.authenticate({
      body: this.authRequest
    }).subscribe({
      next: (res) => {
        this.tokenService.token = res.token as string;
        this.router.navigate(['books']);
      },
      error: (err) => {
        if (err.status === 0) {
          this.toastr.error('Server is unreachable', 'Connection error');
          return;
        }
        if (err.error?.validationErrors) {
          err.error.validationErrors.forEach((msg: string) =>
            this.toastr.error(msg, 'Validation error')
          );
        } else if (err.error?.error) {
          this.toastr.error(err.error.error, 'Authentication failed');
        } else {
          this.toastr.error('Unexpected error occurred');
        }
      }
    })

  }

  register() {
    this.router.navigate(['register'])
  }
}
