import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Clase } from '../../core/models/models';
import { ClasesService } from '../../core/services/clases.service';
import { hoyIso, mensajeError } from '../../core/util/format';
import { HoraPipe } from '../../shared/pipes';
import { ToastService } from '../../shared/toast';
import { Cargando, Vacio } from '../../shared/ui';

@Component({
  selector: 'app-mis-clases',
  imports: [DatePipe, RouterLink, HoraPipe, Cargando, Vacio],
  template: `
    <div class="page-head">
      <div>
        <h1>Mis clases</h1>
        <p class="muted">Seleccione la clase que desea reprogramar.</p>
      </div>
    </div>

    @if (cargando()) {
      <app-cargando />
    } @else if (clases().length === 0) {
      <app-vacio texto="No tiene clases programadas en sus cursos." />
    } @else {
      <div class="cards">
        @for (c of clases(); track c.id) {
          <article class="card clase">
            <div class="clase-top">
              <span class="badge badge-info">Sección {{ c.seccion }}</span>
              @if (c.fechaOriginal < hoy) { <span class="badge badge-neutral">Fecha pasada</span> }
            </div>
            <h3>{{ c.curso }}</h3>
            @if (c.tema) { <p class="muted">{{ c.tema }}</p> }
            <div class="clase-fecha">
              <strong>{{ c.fechaOriginal | date: "EEEE d 'de' MMMM" }}</strong>
              <span>{{ c.horaInicio | hora }} – {{ c.horaFin | hora }}</span>
            </div>
            <a class="btn btn-primary btn-block" [routerLink]="['/docente/clases', c.id, 'proponer']">
              Reprogramar clase
            </a>
          </article>
        }
      </div>
    }
  `,
})
export class MisClases {
  private readonly svc = inject(ClasesService);
  private readonly toast = inject(ToastService);

  protected readonly hoy = hoyIso();
  protected readonly cargando = signal(true);
  private readonly _clases = signal<Clase[]>([]);
  protected readonly clases = computed(() =>
    [...this._clases()].sort((a, b) => a.fechaOriginal.localeCompare(b.fechaOriginal)),
  );

  constructor() {
    this.svc.mias().subscribe({
      next: (l) => {
        this._clases.set(l);
        this.cargando.set(false);
      },
      error: (e) => {
        this.cargando.set(false);
        this.toast.error(mensajeError(e));
      },
    });
  }
}
