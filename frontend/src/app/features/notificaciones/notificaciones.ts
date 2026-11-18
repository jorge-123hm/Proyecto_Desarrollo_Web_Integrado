import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Notificacion } from '../../core/models/models';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionesService } from '../../core/services/notificaciones.service';
import { mensajeError } from '../../core/util/format';
import { ToastService } from '../../shared/toast';
import { Cargando, Vacio } from '../../shared/ui';

@Component({
  selector: 'app-notificaciones',
  imports: [DatePipe, RouterLink, Cargando, Vacio],
  template: `
    <div class="page-head">
      <div>
        <h1>Notificaciones</h1>
        <p class="muted">{{ noLeidas() }} sin leer</p>
      </div>
      @if (noLeidas() > 0) {
        <button type="button" class="btn" (click)="marcarTodas()" [disabled]="trabajando()">
          Marcar todas como leídas
        </button>
      }
    </div>

    @if (cargando()) {
      <app-cargando />
    } @else if (lista().length === 0) {
      <app-vacio texto="No tiene notificaciones." />
    } @else {
      <ul class="notif-list">
        @for (n of lista(); track n.id) {
          <li class="notif" [class.no-leida]="!n.leida">
            <span class="dot" [class.on]="!n.leida"></span>
            <div class="notif-body">
              <p>{{ n.mensaje }}</p>
              <small class="muted">{{ n.fecha | date: "EEE d MMM y, HH:mm" }}</small>
            </div>
            <div class="notif-actions">
              @if (n.solicitudId && !esAlumno()) {
                <a class="btn btn-sm" [routerLink]="['/solicitudes', n.solicitudId]">Ver solicitud</a>
              }
              @if (!n.leida) {
                <button type="button" class="btn btn-sm btn-ghost" (click)="marcar(n)">Marcar leída</button>
              }
            </div>
          </li>
        }
      </ul>
    }
  `,
})
export class Notificaciones {
  private readonly svc = inject(NotificacionesService);
  private readonly toast = inject(ToastService);
  private readonly auth = inject(AuthService);
  protected readonly esAlumno = computed(() => this.auth.rol() === 'ALUMNO');

  protected readonly lista = signal<Notificacion[]>([]);
  protected readonly cargando = signal(true);
  protected readonly trabajando = signal(false);
  protected readonly noLeidas = computed(() => this.lista().filter((n) => !n.leida).length);

  constructor() {
    this.svc.listar().subscribe({
      next: (l) => {
        this.lista.set(l);
        this.cargando.set(false);
        this.svc.refrescarContador();
      },
      error: (e) => {
        this.cargando.set(false);
        this.toast.error(mensajeError(e));
      },
    });
  }

  protected marcar(n: Notificacion) {
    this.svc.marcarLeida(n.id).subscribe({
      next: (u) => this.lista.update((l) => l.map((x) => (x.id === u.id ? u : x))),
      error: (e) => this.toast.error(mensajeError(e)),
    });
  }

  protected marcarTodas() {
    const pendientes = this.lista().filter((n) => !n.leida);
    if (!pendientes.length) return;
    this.trabajando.set(true);
    forkJoin(pendientes.map((n) => this.svc.marcarLeida(n.id))).subscribe({
      next: (res) => {
        const porId = new Map(res.map((n) => [n.id, n]));
        this.lista.update((l) => l.map((x) => porId.get(x.id) ?? x));
        this.trabajando.set(false);
      },
      error: (e) => {
        this.trabajando.set(false);
        this.toast.error(mensajeError(e));
      },
    });
  }
}
