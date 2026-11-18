import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Clase, OpcionHorario, Propuesta, PropuestaRequest } from '../models/models';
import { toApiTime } from '../util/format';

@Injectable({ providedIn: 'root' })
export class ClasesService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  mias() {
    return this.http.get<Clase[]>(`${this.api}/clases/mias`);
  }

  /** RF004: franjas sugeridas (todos los parámetros son opcionales) */
  opciones(claseId: number, f: { desde?: string; hasta?: string; limite?: number | null }) {
    let params = new HttpParams();
    if (f.desde) params = params.set('desde', f.desde);
    if (f.hasta) params = params.set('hasta', f.hasta);
    if (f.limite) params = params.set('limite', f.limite);
    return this.http.get<OpcionHorario[]>(`${this.api}/clases/${claseId}/opciones`, { params });
  }

  /** RF005: evalúa una franja concreta (fecha manual) */
  evaluar(claseId: number, fecha: string, horaInicio: string, horaFin: string) {
    const params = new HttpParams()
      .set('claseId', claseId)
      .set('fecha', fecha)
      .set('horaInicio', toApiTime(horaInicio))
      .set('horaFin', toApiTime(horaFin));
    return this.http.get<OpcionHorario>(`${this.api}/propuestas/evaluar`, { params });
  }

  crearPropuesta(req: PropuestaRequest) {
    return this.http.post<Propuesta>(`${this.api}/propuestas`, {
      ...req,
      horaInicio: toApiTime(req.horaInicio),
      horaFin: toApiTime(req.horaFin),
    });
  }
}
