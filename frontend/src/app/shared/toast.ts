import { Component, Injectable, signal } from '@angular/core';

export interface ToastMsg { id: number; tipo: 'ok' | 'error'; texto: string; }

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly mensajes = signal<ToastMsg[]>([]);
  private seq = 0;

  ok(texto: string) { this.mostrar('ok', texto, 3500); }
  error(texto: string) { this.mostrar('error', texto, 6000); }

  quitar(id: number) {
    this.mensajes.update((l) => l.filter((m) => m.id !== id));
  }

  private mostrar(tipo: ToastMsg['tipo'], texto: string, ms: number) {
    const id = ++this.seq;
    this.mensajes.update((l) => [...l, { id, tipo, texto }]);
    setTimeout(() => this.quitar(id), ms);
  }
}

@Component({
  selector: 'app-toast-host',
  template: `
    <div class="toasts" aria-live="polite">
      @for (m of toast.mensajes(); track m.id) {
        <div class="toast" [class.toast-error]="m.tipo === 'error'" role="status">
          <span>{{ m.texto }}</span>
          <button type="button" class="toast-x" (click)="toast.quitar(m.id)" aria-label="Cerrar">×</button>
        </div>
      }
    </div>
  `,
})
export class ToastHost {
  constructor(public toast: ToastService) {}
}
