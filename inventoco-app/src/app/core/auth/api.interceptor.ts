import { HttpErrorResponse, HttpEvent, HttpHandlerFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, Observable, throwError } from 'rxjs';
import { appConfig } from '../config/app-config';
import { AuthService } from './auth.service';

/**
 * Inventoco API requests carry the authco access token; it is never sent to authco or third
 * parties. A 401 on a signed-in request means the token expired or was revoked: log in again.
 */
export function apiInterceptor(req: HttpRequest<unknown>, next: HttpHandlerFn): Observable<HttpEvent<unknown>> {
  const config = appConfig();
  if (!req.url.startsWith(`${config.apiUrl}/`)) {
    return next(req);
  }

  const auth = inject(AuthService);
  const token = auth.token();
  const request = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(request).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        auth.restartLogin();
      }
      return throwError(() => error);
    })
  );
}
