import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Notificacion } from '../models/models';

@Injectable({ providedIn: 'root' })
export class NotificacionesService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/notificaciones`;

  /** Cantidad de no leídas (globito de la barra superior) */
  readonly pendientes = signal(0);

  listar() {
    return this.http.get<Notificacion[]>(this.url);
  }

  /** Refresca el contador; los errores se ignoran (es un dato decorativo) */
  refrescarContador(): void {
    this.http.get<{ cantidad: number }>(`${this.url}/no-leidas/cantidad`).subscribe({
      next: (r) => this.pendientes.set(r.cantidad),
      error: () => {},
    });
  }

  marcarLeida(id: number) {
    return this.http.put<Notificacion>(`${this.url}/${id}/leer`, {}).pipe(
      tap(() => this.refrescarContador()),
    );
  }
}
