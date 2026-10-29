import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SignalFieldComponent } from './shared/signal-field.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, SignalFieldComponent],
  template: `<app-signal-field /><router-outlet />`
})
export class AppComponent {}
