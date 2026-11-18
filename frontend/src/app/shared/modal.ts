import { Component, HostListener, input, output } from '@angular/core';

/** Modal genérico. Úselo con @if en el padre y escuche (cerrar). */
@Component({
  selector: 'app-modal',
  template: `
    <div class="backdrop" (click)="cerrar.emit()">
      <div class="modal" [class.modal-lg]="ancho() === 'lg'" role="dialog" aria-modal="true"
           (click)="$event.stopPropagation()">
        <div class="modal-head">
          <h3>{{ titulo() }}</h3>
          <button type="button" class="icon-btn" (click)="cerrar.emit()" aria-label="Cerrar">×</button>
        </div>
        <div class="modal-body"><ng-content /></div>
        <div class="modal-foot"><ng-content select="[pie]" /></div>
      </div>
    </div>
  `,
})
export class Modal {
  readonly titulo = input.required<string>();
  readonly ancho = input<'md' | 'lg'>('md');
  readonly cerrar = output<void>();

  @HostListener('document:keydown.escape')
  onEsc() { this.cerrar.emit(); }
}
