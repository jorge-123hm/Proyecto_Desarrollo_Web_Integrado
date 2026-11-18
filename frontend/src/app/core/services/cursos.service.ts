import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Curso, CursoRequest, Usuario } from '../models/models';

@Injectable({ providedIn: 'root' })
export class CursosService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/cursos`;

  listar(docenteId?: number | null) {
    let params = new HttpParams();
    if (docenteId) params = params.set('docenteId', docenteId);
    return this.http.get<Curso[]>(this.url, { params });
  }
  crear(req: CursoRequest) {
    return this.http.post<Curso>(this.url, req);
  }
  actualizar(id: number, req: CursoRequest) {
    return this.http.put<Curso>(`${this.url}/${id}`, req);
  }
  eliminar(id: number) {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
  // Matrículas
  alumnos(cursoId: number) {
    return this.http.get<Usuario[]>(`${this.url}/${cursoId}/alumnos`);
  }
  matricular(cursoId: number, alumnoId: number) {
    return this.http.post<void>(`${this.url}/${cursoId}/alumnos/${alumnoId}`, {});
  }
  desmatricular(cursoId: number, alumnoId: number) {
    return this.http.delete<void>(`${this.url}/${cursoId}/alumnos/${alumnoId}`);
  }
}
