import { Injectable, inject, signal } from '@angular/core';
import { Subject, filter } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

export interface EventoServidor {
  tipo: 'solicitud' | 'notificacion' | 'disponibilidad' | 'reconectado' | string;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  datos: any;
}

@Injectable({ providedIn: 'root' })
export class TiempoRealService {
  private readonly auth = inject(AuthService);
  private readonly url = `${environment.apiUrl}/eventos/stream`;

  private readonly _eventos = new Subject<EventoServidor>();
  readonly eventos$ = this._eventos.asObservable();
  readonly conectado = signal(false);

  private activo = false;
  private abort: AbortController | null = null;
  private espera: ReturnType<typeof setTimeout> | null = null;

  de(...tipos: string[]) {
    return this.eventos$.pipe(filter((e) => tipos.includes(e.tipo)));
  }

  conectar(): void {
    if (this.activo) return;
    this.activo = true;
    void this.bucle();
  }

  desconectar(): void {
    this.activo = false;
    this.abort?.abort();
    if (this.espera) clearTimeout(this.espera);
    this.conectado.set(false);
  }

  private async bucle(): Promise<void> {
    let intentos = 0;
    let huboConexion = false;

    while (this.activo) {
      const token = this.auth.token;
      if (!token) break;

      this.abort = new AbortController();
      try {
        const resp = await fetch(this.url, {
          headers: { Authorization: `Bearer ${token}`, Accept: 'text/event-stream' },
          signal: this.abort.signal,
        });
        if (resp.status === 401) {
          this.activo = false;
          this.auth.logout();
          break;
        }
        if (!resp.ok || !resp.body) throw new Error(`SSE ${resp.status}`);

        intentos = 0;
        this.conectado.set(true);
        if (huboConexion) this._eventos.next({ tipo: 'reconectado', datos: {} });
        huboConexion = true;

        await this.leer(resp.body);
      } catch {
        // cancelado o caída de red: se reintenta abajo
      }

      this.conectado.set(false);
      if (!this.activo) break;
      if (!huboConexion) huboConexion = true;
      await new Promise<void>((ok) => {
        this.espera = setTimeout(ok, Math.min(1000 * 2 ** intentos++, 15_000));
      });
    }
  }

  private async leer(body: ReadableStream<Uint8Array>): Promise<void> {
    const reader = body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    let vigilante = setTimeout(() => this.abort?.abort(), 60_000);

    try {
      while (true) {
        const { value, done } = await reader.read();
        if (done) break;

        clearTimeout(vigilante);
        vigilante = setTimeout(() => this.abort?.abort(), 60_000);

        buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n');
        let corte: number;
        while ((corte = buffer.indexOf('\n\n')) >= 0) {
          this.procesar(buffer.slice(0, corte));
          buffer = buffer.slice(corte + 2);
        }
      }
    } finally {
      clearTimeout(vigilante);
    }
  }

  private procesar(bloque: string): void {
    let tipo = 'message';
    const datos: string[] = [];
    for (const linea of bloque.split('\n')) {
      if (linea.startsWith('event:')) tipo = linea.slice(6).trim();
      else if (linea.startsWith('data:')) datos.push(linea.slice(5).trim());
    }
    if (!datos.length || tipo === 'conectado') return;
    try {
      this._eventos.next({ tipo, datos: JSON.parse(datos.join('\n')) });
    } catch {
    }
  }
}