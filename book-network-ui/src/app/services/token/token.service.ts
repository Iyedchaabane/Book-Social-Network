import { Injectable } from '@angular/core';
import {JwtHelperService} from "@auth0/angular-jwt";

@Injectable({
  providedIn: 'root'
})
export class TokenService {
  private jwtHelper = new JwtHelperService();

  set token(token: string) {
    localStorage.setItem('token', token);
  }

  get token() {
    return localStorage.getItem('token') as string;
  }

  isTokenValid() {
    const token = this.token;
    if (!token) {
      return false;
    }
    // check expiry date
    const isTokenExpired = this.jwtHelper.isTokenExpired(token);
    if (isTokenExpired) {
      localStorage.clear();
      return false;
    }
    return true;
  }

  isTokenNotValid() {
    return !this.isTokenValid();
  }

  get userRoles(): string[] {
    const token = this.token;
    if (token) {
      const decodedToken = this.jwtHelper.decodeToken(token);
      console.log(decodedToken.authorities);
      return decodedToken.authorities;
    }
    return [];
  }

  get fullName(): string | null {
    const token = this.token;
    if (!token) return null;
    const decodedToken = this.jwtHelper.decodeToken(token);
    const fullName = decodedToken?.fullName;
    if (!fullName) return null;
    return fullName.split(' ')[0];
  }

  get userId(): string | null {
    const token = this.token;
    if (!token) return null;
    const decodedToken = this.jwtHelper.decodeToken(token);
    const userId = decodedToken?.userId;
    if (!userId) return null;
    return userId.toString();
  }
}
