import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Solicitud } from '../../core/models/models';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionesService } from '../../core/services/notificaciones.service';
import { SolicitudesService } from '../../core/services/solicitudes.service';
import { mensajeError } from '../../core/util/format';
import { ConfirmService } from '../../shared/confirm';
import { HoraPipe } from '../../shared/pipes';
import { ToastService } from '../../shared/toast';
import { Cargando, EstadoBadge, Porcentaje } from '../../shared/ui';

@Component({
  selector: 'app-solicitud-detalle',
  imports: [DatePipe, ReactiveFormsModule, RouterLink, HoraPipe, Cargando, EstadoBadge, Porcentaje],
  template: `
    <a [routerLink]="volver()" class="back">← Volver</a>

    @if (cargando()) {
      <app-cargando />
    } @else if (s(); as s) {
      <div class="page-head">
        <div>
          <h1>Solicitud #{{ s.id }}</h1>
          <p class="muted">{{ s.curso }} · Sección {{ s.seccion }} · Docente: {{ s.docente }}</p>
        </div>
        <app-estado-badge [estado]="s.estado" />
      </div>

      <div class="compare">
        <section class="card">
          <span class="eyebrow">Clase original</span>
          <h2>{{ s.fechaOriginal | date: "EEEE d 'de' MMMM 'de' y" }}</h2>
          <p class="big-time">{{ s.horaOriginalInicio | hora }} – {{ s.horaOriginalFin | hora }}</p>
        </section>
        <div class="compare-arrow" aria-hidden="true">→</div>
        <section class="card card-accent">
          <span class="eyebrow">Fecha propuesta</span>
          <h2>{{ s.fechaPropuesta | date: "EEEE d 'de' MMMM 'de' y" }}</h2>
          <p class="big-time">{{ s.horaInicio | hora }} – {{ s.horaFin | hora }}</p>
        </section>
      </div>

      <section class="card">
        <h3>Impacto en los alumnos</h3>
        <app-porcentaje [valor]="s.porcentajeCoincidencia" />
        <p class="muted m0">
          {{ s.alumnosConConflicto === 0
            ? 'Ningún alumno tiene cruce de horario en la fecha propuesta.'
            : s.alumnosConConflicto + ' alumno(s) tienen cruce de horario en la fecha propuesta.' }}
        </p>
      </section>

      <section class="card">
        <h3>Seguimiento</h3>
        <dl class="dl">
          <dt>Enviada</dt><dd>{{ s.fechaSolicitud | date: 'dd/MM/yyyy HH:mm' }}</dd>
          @if (s.fechaResolucion) {
            <dt>Resuelta</dt><dd>{{ s.fechaResolucion | date: 'dd/MM/yyyy HH:mm' }}</dd>
            <dt>Resuelta por</dt><dd>{{ s.resueltoPor }}</dd>
          }
          @if (s.observacion) {
            <dt>Observación</dt><dd>{{ s.observacion }}</dd>
          }
        </dl>
      </section>

      @if (esCoord() && s.estado === 'PENDIENTE') {
        <section class="card">
          <h3>Resolver solicitud</h3>
          <div class="field">
            <label for="obs">Observación <span class="muted">(obligatoria al rechazar)</span></label>
            <textarea id="obs" rows="3" maxlength="500" [formControl]="observacion"
                      placeholder="Motivo o comentario para el docente"></textarea>
            @if (errorObs()) { <small class="err">{{ errorObs() }}</small> }
          </div>
          <div class="actions">
            <button type="button" class="btn btn-danger" [disabled]="resolviendo()" (click)="resolver(s, 'RECHAZADA')">
              Rechazar
            </button>
            <button type="button" class="btn btn-success" [disabled]="resolviendo()" (click)="resolver(s, 'APROBADA')">
              Aprobar
            </button>
          </div>
          <p class="hint">Al aprobar se notificará al docente y a todos los alumnos matriculados.</p>
        </section>
      }
    }
  `,
})
export class SolicitudDetalle implements OnInit {
  /** Viene del parámetro :id de la ruta */
  readonly id = input.required<string>();

  private readonly svc = inject(SolicitudesService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly confirm = inject(ConfirmService);
  private readonly notif = inject(NotificacionesService);

  private readonly auth = inject(AuthService);
  protected readonly esCoord = computed(() => this.auth.rol() === 'COORDINACION');
  protected readonly volver = computed(() =>
    this.esCoord() ? '/coordinacion/solicitudes' : '/docente/solicitudes',
  );

  protected readonly s = signal<Solicitud | null>(null);
  protected readonly cargando = signal(true);
  protected readonly resolviendo = signal(false);
  protected readonly errorObs = signal('');
  protected readonly observacion = new FormControl('', { nonNullable: true });

  ngOnInit() {
    this.svc.detalle(Number(this.id())).subscribe({
      next: (s) => {
        this.s.set(s);
        this.cargando.set(false);
      },
      error: (e) => {
        this.cargando.set(false);
        this.toast.error(mensajeError(e));
        this.router.navigateByUrl(this.volver());
      },
    });
  }

  protected async resolver(s: Solicitud, decision: 'APROBADA' | 'RECHAZADA') {
    const obs = this.observacion.value.trim();
    if (decision === 'RECHAZADA' && !obs) {
      this.errorObs.set('Debe indicar el motivo del rechazo');
      return;
    }
    this.errorObs.set('');

    const aprobar = decision === 'APROBADA';
    const ok = await this.confirm.preguntar({
      titulo: aprobar ? 'Aprobar solicitud' : 'Rechazar solicitud',
      mensaje: aprobar
        ? 'La clase se reprogramará y se notificará al docente y a los alumnos.'
        : 'Se notificará al docente el motivo del rechazo.',
      confirmar: aprobar ? 'Aprobar' : 'Rechazar',
      peligro: !aprobar,
    });
    if (!ok) return;

    this.resolviendo.set(true);
    this.svc.resolver(s.id, decision, obs || null).subscribe({
      next: (r) => {
        this.s.set(r);
        this.resolviendo.set(false);
        this.toast.ok(aprobar ? 'Solicitud aprobada' : 'Solicitud rechazada');
        this.notif.refrescarContador();
      },
      error: (e) => {
        this.resolviendo.set(false);
        this.toast.error(mensajeError(e));
      },
    });
  }
}
