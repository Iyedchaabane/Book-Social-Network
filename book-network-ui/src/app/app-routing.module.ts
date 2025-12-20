import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import {LoginComponent} from "./pages/login/login.component";
import {AppComponent} from "./app.component";
import {RegisterComponent} from "./pages/register/register.component";
import {ActivateAccountComponent} from "./pages/activate-account/activate-account.component";
import {authGuard} from "./services/guard/auth.guard";
import {ForgetPasswordComponent} from "./pages/forget-password/forget-password.component";
import {ResetPasswordComponent} from "./pages/reset-password/reset-password.component";

const routes: Routes = [
  {
    path: '',
    redirectTo: 'books',
    pathMatch: 'full'
  },
  {
    path: "login",
    component: LoginComponent
  },
  {
    path: "activate-account",
    component: ActivateAccountComponent,
    data: { context: 'activation' }
  },
  {
    path: "register",
    component: RegisterComponent
  },
  {
    path: "forget-password",
    component: ForgetPasswordComponent,
    data: { context: 'reset' }
  },
  {
    path: "reset-password",
    component: ResetPasswordComponent
  },
  {
    path: "books",
    loadChildren: () => import("./modules/book/book.module").then(m => m.BookModule),
    canActivate: [authGuard]
  },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
