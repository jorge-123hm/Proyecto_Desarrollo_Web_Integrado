import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Bloque, BloqueRequest } from '../models/models';
import { toApiTime } from '../util/format';

@Injectable({ providedIn: 'root' })
export class DisponibilidadService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/disponibilidad`;

  listar() {
    return this.http.get<Bloque[]>(this.url);
  }
  agregar(req: BloqueRequest) {
    return this.http.post<Bloque>(this.url, {
      ...req,
      horaInicio: toApiTime(req.horaInicio),
      horaFin: toApiTime(req.horaFin),
      motivo: req.motivo?.trim() || null,
    });
  }
  eliminar(id: number) {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
