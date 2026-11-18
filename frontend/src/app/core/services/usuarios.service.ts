import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Rol, Usuario, UsuarioRequest } from '../models/models';

@Injectable({ providedIn: 'root' })
export class UsuariosService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/usuarios`;

  listar(rol?: Rol | '') {
    let params = new HttpParams();
    if (rol) params = params.set('rol', rol);
    return this.http.get<Usuario[]>(this.url, { params });
  }
  crear(req: UsuarioRequest) {
    return this.http.post<Usuario>(this.url, req);
  }
  actualizar(id: number, req: UsuarioRequest) {
    return this.http.put<Usuario>(`${this.url}/${id}`, req);
  }
  eliminar(id: number) {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
