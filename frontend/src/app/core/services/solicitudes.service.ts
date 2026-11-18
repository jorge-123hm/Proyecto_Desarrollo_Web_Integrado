import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { EstadoSolicitud, Solicitud } from '../models/models';

@Injectable({ providedIn: 'root' })
export class SolicitudesService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/solicitudes`;

  /** Docente: solo las suyas · Coordinación: todas */
  historial(estado?: EstadoSolicitud | '', cursoId?: number | null) {
    let params = new HttpParams();
    if (estado) params = params.set('estado', estado);
    if (cursoId) params = params.set('cursoId', cursoId);
    return this.http.get<Solicitud[]>(this.url, { params });
  }
  detalle(id: number) {
    return this.http.get<Solicitud>(`${this.url}/${id}`);
  }
  enviar(propuestaId: number) {
    return this.http.post<Solicitud>(this.url, { propuestaId });
  }
  resolver(id: number, decision: 'APROBADA' | 'RECHAZADA', observacion: string | null) {
    return this.http.put<Solicitud>(`${this.url}/${id}/resolver`, { decision, observacion });
  }
}
