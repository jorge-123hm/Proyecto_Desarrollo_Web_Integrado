import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Curso, EstadoSolicitud, Solicitud } from '../../core/models/models';
import { AuthService } from '../../core/services/auth.service';
import { CursosService } from '../../core/services/cursos.service';
import { SolicitudesService } from '../../core/services/solicitudes.service';
import { mensajeError } from '../../core/util/format';
import { HoraPipe } from '../../shared/pipes';
import { ToastService } from '../../shared/toast';
import { Cargando, EstadoBadge, Porcentaje, Vacio } from '../../shared/ui';

/** Sirve para ambos roles: el docente ve las suyas y coordinación ve todas */
@Component({
  selector: 'app-solicitudes-lista',
  imports: [DatePipe, FormsModule, RouterLink, HoraPipe, Cargando, Vacio, EstadoBadge, Porcentaje],
  template: `
    <div class="page-head">
      <div>
        <h1>{{ esCoord() ? 'Solicitudes de reprogramación' : 'Mis solicitudes' }}</h1>
        <p class="muted">
          {{ esCoord() ? 'Revise y resuelva las solicitudes enviadas por los docentes.' : 'Historial y estado de sus solicitudes.' }}
        </p>
      </div>
    </div>

    <div class="toolbar">
      <div class="field inline">
        <label for="estado">Estado</label>
        <select id="estado" [ngModel]="estado()" (ngModelChange)="setEstado($event)">
          <option value="">Todos</option>
          <option value="PENDIENTE">Pendientes</option>
          <option value="APROBADA">Aprobadas</option>
          <option value="RECHAZADA">Rechazadas</option>
        </select>
      </div>
      @if (esCoord()) {
        <div class="field inline">
          <label for="curso">Curso</label>
          <select id="curso" [ngModel]="cursoId()" (ngModelChange)="setCurso($event)">
            <option [ngValue]="null">Todos</option>
            @for (c of cursos(); track c.id) {
              <option [ngValue]="c.id">{{ c.nombre }} ({{ c.seccion }})</option>
            }
          </select>
        </div>
      }
      <span class="toolbar-count muted">{{ lista().length }} resultado(s)</span>
    </div>

    @if (cargando()) {
      <app-cargando />
    } @else if (lista().length === 0) {
      <app-vacio texto="No hay solicitudes con los filtros seleccionados." />
    } @else {
      <div class="table-wrap">
        <table class="table table-click">
          <thead>
            <tr>
              <th>Curso</th>
              @if (esCoord()) { <th>Docente</th> }
              <th>Fecha original</th>
              <th>Fecha propuesta</th>
              <th>Coincidencia</th>
              <th>Estado</th>
              <th>Enviada</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (s of lista(); track s.id) {
              <tr>
                <td><strong>{{ s.curso }}</strong><div class="muted small">Sección {{ s.seccion }}</div></td>
                @if (esCoord()) { <td>{{ s.docente }}</td> }
                <td>{{ s.fechaOriginal | date: 'dd/MM/yyyy' }}<div class="muted small">{{ s.horaOriginalInicio | hora }}–{{ s.horaOriginalFin | hora }}</div></td>
                <td>{{ s.fechaPropuesta | date: 'dd/MM/yyyy' }}<div class="muted small">{{ s.horaInicio | hora }}–{{ s.horaFin | hora }}</div></td>
                <td class="w-pct"><app-porcentaje [valor]="s.porcentajeCoincidencia" /></td>
                <td><app-estado-badge [estado]="s.estado" /></td>
                <td class="muted small">{{ s.fechaSolicitud | date: 'dd/MM/yy HH:mm' }}</td>
                <td class="t-right">
                  <a class="btn btn-sm" [routerLink]="['/solicitudes', s.id]">
                    {{ esCoord() && s.estado === 'PENDIENTE' ? 'Revisar' : 'Ver' }}
                  </a>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }
  `,
})
export class SolicitudesLista {
  private readonly svc = inject(SolicitudesService);
  private readonly cursosSvc = inject(CursosService);
  private readonly toast = inject(ToastService);

  private readonly auth = inject(AuthService);
  protected readonly esCoord = computed(() => this.auth.rol() === 'COORDINACION');

  protected readonly lista = signal<Solicitud[]>([]);
  protected readonly cursos = signal<Curso[]>([]);
  protected readonly cargando = signal(true);
  protected readonly estado = signal<EstadoSolicitud | ''>('');
  protected readonly cursoId = signal<number | null>(null);

  constructor() {
    if (this.esCoord()) {
      this.estado.set('PENDIENTE'); // lo primero que necesita coordinación
      this.cursosSvc.listar().subscribe({ next: (c) => this.cursos.set(c), error: () => {} });
    }
    this.cargar();
  }

  protected setEstado(v: EstadoSolicitud | '') {
    this.estado.set(v);
    this.cargar();
  }

  protected setCurso(v: number | null) {
    this.cursoId.set(v);
    this.cargar();
  }

  private cargar() {
    this.cargando.set(true);
    this.svc.historial(this.estado(), this.cursoId()).subscribe({
      next: (l) => {
        this.lista.set(l);
        this.cargando.set(false);
      },
      error: (e) => {
        this.cargando.set(false);
        this.toast.error(mensajeError(e));
      },
    });
  }
}
