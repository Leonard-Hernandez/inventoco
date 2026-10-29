import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { AuthorizationResponse, OAuthError } from '../../core/auth/oauth.models';
import { t } from '../../core/i18n/i18n';

/**
 * redirect_uri of the authco client. In the top window it exchanges the code and resumes
 * the navigation that needed the login. Inside the silent-renewal iframe it only hands the
 * response to the parent window.
 */
@Component({
  selector: 'app-auth-callback',
  template: `
    @if (!inIframe) {
      <main class="screen">
        @if (errorMessage(); as message) {
          <h1>{{ t('No pudimos iniciar sesión', 'We could not sign you in') }}</h1>
          <p>{{ message }}</p>
          <button type="button" class="btn primary" (click)="retry()">{{ t('Intentar de nuevo', 'Try again') }}</button>
        } @else {
          <p class="wait">{{ t('Entrando…', 'Signing in…') }}</p>
        }
      </main>
    }
  `,
  styles: `
    .screen { position: relative; z-index: 1; min-height: 100svh; display: grid; place-content: center; justify-items: center; gap: 1rem; padding: 2rem; text-align: center; }
    h1 { font: 300 32px/36px var(--display); letter-spacing: .01em; }
    p { max-width: 28rem; color: var(--muted); }
  `
})
export default class AuthCallbackComponent implements OnInit {
  readonly inIframe = window.parent !== window;
  readonly errorMessage = signal<string | null>(null);
  readonly t = t;

  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  ngOnInit() {
    const query = this.route.snapshot.queryParamMap;
    const response: AuthorizationResponse = {
      code: query.get('code'),
      state: query.get('state'),
      error: query.get('error'),
      error_description: query.get('error_description')
    };

    if (this.inIframe) {
      window.parent.postMessage(response, window.location.origin);
      return;
    }

    this.auth.completeLogin(response).subscribe({
      next: (returnUrl) => this.router.navigateByUrl(returnUrl, { replaceUrl: true }),
      error: (error: unknown) => this.errorMessage.set(error instanceof OAuthError
        ? t('La respuesta del inicio de sesión no es válida o expiró.', 'The sign-in response is not valid or expired.')
        : t('No pudimos completar el inicio de sesión. Inténtalo de nuevo.', 'We could not complete the sign-in. Try again.'))
    });
  }

  retry() {
    this.errorMessage.set(null);
    this.auth.login('/');
  }
}
