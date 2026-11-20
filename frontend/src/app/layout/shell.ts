import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, timer } from 'rxjs';
import { AuthService } from '../core/services/auth.service';
import { NotificacionesService } from '../core/services/notificaciones.service';
import { TiempoRealService } from '../core/services/tiempo-real.service';
import { RolPipe } from '../shared/pipes';
import { ToastService } from '../shared/toast';

interface Item { ruta: string; texto: string; icono: string; }

const MENU: Record<string, Item[]> = {
  COORDINACION: [
    { ruta: '/coordinacion/solicitudes', texto: 'Solicitudes', icono: '📋' },
    { ruta: '/coordinacion/cursos', texto: 'Cursos y matrículas', icono: '📚' },
    { ruta: '/coordinacion/usuarios', texto: 'Usuarios', icono: '👥' },
  ],
  DOCENTE: [
    { ruta: '/docente/clases', texto: 'Mis clases', icono: '🗓️' },
    { ruta: '/docente/solicitudes', texto: 'Mis solicitudes', icono: '📋' },
    { ruta: '/disponibilidad', texto: 'Mi disponibilidad', icono: '⏰' },
  ],
  ALUMNO: [{ ruta: '/disponibilidad', texto: 'Mi disponibilidad', icono: '⏰' }],
};

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, RolPipe],
  template: `
    <div class="shell" [class.nav-abierto]="menuAbierto()">
      <aside class="sidebar">
        <div class="brand">
          <span class="brand-logo">↻</span>
          <div>
            <strong>Reprogramación</strong>
            <small>de clases</small>
          </div>
        </div>

        <nav>
          @for (it of items(); track it.ruta) {
            <a [routerLink]="it.ruta" routerLinkActive="activo" class="nav-link">
              <span class="nav-ico">{{ it.icono }}</span>{{ it.texto }}
            </a>
          }
          <a routerLink="/notificaciones" routerLinkActive="activo" class="nav-link">
            <span class="nav-ico">🔔</span>Notificaciones
            @if (notif.pendientes() > 0) { <span class="pill">{{ notif.pendientes() }}</span> }
          </a>
        </nav>

        <div class="sidebar-foot">
          <div class="user">
            <div class="avatar">{{ inicial() }}</div>
            <div class="user-info">
              <strong>{{ auth.sesion()?.nombres }}</strong>
              <small>{{ auth.rol() | rol }}</small>
            </div>
          </div>
          <button type="button" class="btn btn-block btn-ghost" (click)="auth.logout()">Cerrar sesión</button>
        </div>
      </aside>

      <div class="main">
        <header class="topbar">
          <button type="button" class="icon-btn menu-btn" (click)="menuAbierto.set(!menuAbierto())"
                  aria-label="Menú">☰</button>
          <span class="topbar-title">{{ auth.sesion()?.correo }}</span>
          <span class="en-vivo" [class.off]="!rt.conectado()" [title]="rt.conectado() ? 'Actualizaciones en tiempo real' : 'Reconectando…'">
            {{ rt.conectado() ? 'En vivo' : 'Reconectando…' }}
          </span>
          <a routerLink="/notificaciones" class="bell" aria-label="Notificaciones" title="Notificaciones">
            🔔
            @if (notif.pendientes() > 0) { <span class="bell-badge">{{ notif.pendientes() }}</span> }
          </a>
        </header>
        <main class="content"><router-outlet /></main>
      </div>

      @if (menuAbierto()) { <div class="scrim" (click)="menuAbierto.set(false)"></div> }
    </div>
  `,
})
export class Shell {
  protected readonly auth = inject(AuthService);
  protected readonly notif = inject(NotificacionesService);
  protected readonly rt = inject(TiempoRealService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  protected readonly menuAbierto = signal(false);
  protected readonly items = computed(() => MENU[this.auth.rol() ?? ''] ?? []);
  protected readonly inicial = computed(() =>
    (this.auth.sesion()?.nombres ?? '?').charAt(0).toUpperCase(),
  );

  constructor() {
    // Contador de notificaciones: al entrar y, como respaldo, cada 2 min (lo normal es que llegue por SSE)
    timer(0, 120_000).pipe(takeUntilDestroyed()).subscribe(() => this.notif.refrescarContador());

    // Tiempo real: una sola conexión mientras haya sesión
    this.rt.conectar();
    inject(DestroyRef).onDestroy(() => this.rt.desconectar());

    this.rt.de('notificacion').pipe(takeUntilDestroyed()).subscribe((e) => {
      this.notif.refrescarContador();
      if (e.datos?.mensaje) this.toast.ok(e.datos.mensaje);
    });
    this.rt.de('reconectado').pipe(takeUntilDestroyed()).subscribe(() => this.notif.refrescarContador());

    // Cierra el menú móvil al navegar
    this.router.events
      .pipe(filter((e) => e instanceof NavigationEnd), takeUntilDestroyed())
      .subscribe(() => this.menuAbierto.set(false));
  }
}