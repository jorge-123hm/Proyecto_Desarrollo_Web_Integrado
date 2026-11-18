import { Component, computed, input } from '@angular/core';
import { EstadoSolicitud } from '../core/models/models';

@Component({
  selector: 'app-estado-badge',
  template: `<span class="badge" [class]="'badge-' + estado().toLowerCase()">{{ texto() }}</span>`,
})
export class EstadoBadge {
  readonly estado = input.required<EstadoSolicitud>();
  readonly texto = computed(() => {
    const e = this.estado();
    return e === 'PENDIENTE' ? 'Pendiente' : e === 'APROBADA' ? 'Aprobada' : 'Rechazada';
  });
}

/** Barra de porcentaje de coincidencia (0-100) */
@Component({
  selector: 'app-porcentaje',
  template: `
    <div class="pct" [title]="valor() + '%'">
      <div class="pct-bar"><div class="pct-fill" [class]="nivel()" [style.width.%]="valor()"></div></div>
      <span class="pct-num">{{ valor() }}%</span>
    </div>
  `,
})
export class Porcentaje {
  readonly valor = input.required<number>();
  readonly nivel = computed(() => {
    const v = this.valor();
    return v >= 80 ? 'alto' : v >= 50 ? 'medio' : 'bajo';
  });
}

@Component({
  selector: 'app-vacio',
  template: `<div class="vacio"><p>{{ texto() }}</p><ng-content /></div>`,
})
export class Vacio {
  readonly texto = input.required<string>();
}

@Component({
  selector: 'app-cargando',
  template: `<div class="cargando"><span class="spinner"></span> Cargando…</div>`,
})
export class Cargando {}
