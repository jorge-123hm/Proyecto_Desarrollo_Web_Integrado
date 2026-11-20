import { HttpErrorResponse } from '@angular/common/http';

/** "08:00:00" -> "08:00" */
export function hhmm(t: string | null | undefined): string {
  return t ? t.slice(0, 5) : '';
}

/** "08:00" -> "08:00:00" (formato que siempre acepta el backend) */
export function toApiTime(t: string): string {
  return t.length === 5 ? `${t}:00` : t;
}

/** Minutos desde medianoche */
export function minutos(t: string): number {
  const [h, m] = t.split(':').map(Number);
  return h * 60 + (m || 0);
}

/** Fecha local en formato yyyy-MM-dd (offset en días desde hoy) */
export function hoyIso(offsetDias = 0): string {
  const d = new Date();
  d.setDate(d.getDate() + offsetDias);
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mm}-${dd}`;
}

/** Convierte cualquier error HTTP del backend en un texto legible */
export function mensajeError(err: unknown): string {
  if (!(err instanceof HttpErrorResponse)) {
    return 'Ocurrió un error inesperado';
  }
  if (err.status === 0) {
    return 'No se pudo conectar con el servidor. Verifique que el backend esté corriendo en el puerto 8080.';
  }
  const body = err.error;
  if (body && typeof body === 'object') {
    if (typeof body['error'] === 'string') {
      return body['error'];
    }
    // Errores de validación: { campo: "mensaje" }
    const partes = Object.entries(body)
      .filter(([, v]) => typeof v === 'string')
      .map(([k, v]) => `${k}: ${v}`);
    if (partes.length) {
      return partes.join(' · ');
    }
  }
  switch (err.status) {
    case 401: return 'Su sesión expiró o las credenciales no son válidas';
    case 403: return 'No tiene permisos para realizar esta acción';
    case 404: return 'No se encontró el recurso solicitado';
    case 409: return 'La operación viola una restricción de datos';
    default: return `Error del servidor (${err.status})`;
  }
}
export function fechaLocal(iso: string): Date {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d);
}

export function isoDe(d: Date): string {
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mm}-${dd}`;
}

export function sumarDias(iso: string, n: number): string {
  const d = fechaLocal(iso);
  d.setDate(d.getDate() + n);
  return isoDe(d);
}

/** Lunes de la semana a la que pertenece la fecha */
export function lunesDe(iso: string): string {
  const d = fechaLocal(iso);
  return sumarDias(iso, -((d.getDay() + 6) % 7));
}
