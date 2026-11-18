import { Component, Injectable, signal } from '@angular/core';

export interface ConfirmOpts {
  titulo: string;
  mensaje: string;
  confirmar?: string;
  peligro?: boolean;
}

@Injectable({ providedIn: 'root' })
export class ConfirmService {
  readonly abierto = signal<ConfirmOpts | null>(null);
  private resolver: ((v: boolean) => void) | null = null;

  preguntar(opts: ConfirmOpts): Promise<boolean> {
    this.abierto.set(opts);
    return new Promise<boolean>((res) => (this.resolver = res));
  }

  responder(v: boolean) {
    this.abierto.set(null);
    this.resolver?.(v);
    this.resolver = null;
  }
}

@Component({
  selector: 'app-confirm-host',
  template: `
    @if (svc.abierto(); as c) {
      <div class="backdrop" (click)="svc.responder(false)">
        <div class="modal modal-sm" role="alertdialog" aria-modal="true" (click)="$event.stopPropagation()">
          <div class="modal-head"><h3>{{ c.titulo }}</h3></div>
          <div class="modal-body"><p class="m0">{{ c.mensaje }}</p></div>
          <div class="modal-foot">
            <button type="button" class="btn" (click)="svc.responder(false)">Cancelar</button>
            <button type="button" class="btn" [class.btn-danger]="c.peligro" [class.btn-primary]="!c.peligro"
                    (click)="svc.responder(true)">{{ c.confirmar || 'Aceptar' }}</button>
          </div>
        </div>
      </div>
    }
  `,
})
export class ConfirmHost {
  constructor(public svc: ConfirmService) {}
}
