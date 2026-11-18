import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginRequest, LoginResponse, Rol } from '../models/models';

const KEY = 'reprog.sesion';

export interface Sesion {
  token: string;
  usuarioId: number;
  nombres: string;
  correo: string;
  rol: Rol;
}

/** Lee el `exp` del JWT (en segundos) sin validar firma: solo para descartar sesiones vencidas */
function expiro(token: string): boolean {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    return typeof payload.exp === 'number' && payload.exp * 1000 <= Date.now();
  } catch {
    return true;
  }
}

function leerSesion(): Sesion | null {
  try {
    const raw = localStorage.getItem(KEY);
    if (!raw) return null;
    const s = JSON.parse(raw) as Sesion;
    return s?.token && !expiro(s.token) ? s : null;
  } catch {
    return null;
  }
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _sesion = signal<Sesion | null>(leerSesion());

  readonly sesion = this._sesion.asReadonly();
  readonly autenticado = computed(() => this._sesion() !== null);
  readonly rol = computed(() => this._sesion()?.rol ?? null);

  get token(): string | null {
    return this._sesion()?.token ?? null;
  }

  login(req: LoginRequest) {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/auth/login`, req).pipe(
      tap((r) => {
        const s: Sesion = {
          token: r.token,
          usuarioId: r.usuarioId,
          nombres: r.nombres,
          correo: r.correo,
          rol: r.rol,
        };
        localStorage.setItem(KEY, JSON.stringify(s));
        this._sesion.set(s);
      }),
    );
  }

  logout(redirigir = true): void {
    localStorage.removeItem(KEY);
    this._sesion.set(null);
    if (redirigir) {
      this.router.navigate(['/login']);
    }
  }

  /** Ruta de inicio según el rol */
  rutaInicio(rol: Rol | null = this.rol()): string {
    switch (rol) {
      case 'COORDINACION': return '/coordinacion/solicitudes';
      case 'DOCENTE': return '/docente/clases';
      case 'ALUMNO': return '/disponibilidad';
      default: return '/login';
    }
  }
}
