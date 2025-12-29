import { Component } from '@angular/core';
import {RegistrationRequest} from "../../services/models/registration-request";
import {Router} from "@angular/router";
import {AuthenticationService} from "../../services/services/authentication.service";
import {ToastrService} from "ngx-toastr";

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {

  registerRequest: RegistrationRequest = {firstname: '', lastname: '', email: '', password: ''}
  errorMsg: Array<string> = [];

  constructor(
    private router: Router,
    private authService: AuthenticationService,
    private toastr: ToastrService
  ) {
  }

  register() {
    this.errorMsg = [];
    this.authService.register({
        body: this.registerRequest
    }).subscribe({
      next: () => {
        this.router.navigate(['activate-account'])
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

  login() {
    this.router.navigate(['login'])
  }
}
