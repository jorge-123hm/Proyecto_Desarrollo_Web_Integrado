export type Rol = 'ALUMNO' | 'DOCENTE' | 'COORDINACION';
export type EstadoSolicitud = 'PENDIENTE' | 'APROBADA' | 'RECHAZADA';
export type DiaSemana =
  | 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export const DIAS: DiaSemana[] = [
  'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY',
];

export const DIA_LABEL: Record<DiaSemana, string> = {
  MONDAY: 'Lunes',
  TUESDAY: 'Martes',
  WEDNESDAY: 'Miércoles',
  THURSDAY: 'Jueves',
  FRIDAY: 'Viernes',
  SATURDAY: 'Sábado',
  SUNDAY: 'Domingo',
};

export const ROL_LABEL: Record<Rol, string> = {
  ALUMNO: 'Alumno',
  DOCENTE: 'Docente',
  COORDINACION: 'Coordinación',
};

// ---------- Auth ----------
export interface LoginRequest { correo: string; password: string; }
export interface LoginResponse {
  token: string;
  tipo: string;
  usuarioId: number;
  nombres: string;
  correo: string;
  rol: Rol;
}

// ---------- Usuarios ----------
export interface Usuario {
  id: number;
  cui: string;
  nombres: string;
  apellidos: string;
  correo: string;
  rol: Rol;
  activo: boolean;
}
export interface UsuarioRequest {
  cui: string;
  nombres: string;
  apellidos: string;
  correo: string;
  password?: string;
  rol: Rol;
  activo: boolean;
}

// ---------- Cursos ----------
export interface Curso {
  id: number;
  nombre: string;
  seccion: string;
  periodo: string;
  docenteId: number;
  docenteNombre: string;
}
export interface CursoRequest {
  nombre: string;
  seccion: string;
  periodo: string;
  docenteId: number;
}

// ---------- Clases y propuestas ----------
export interface Clase {
  id: number;
  cursoId: number;
  curso: string;
  seccion: string;
  fechaOriginal: string;
  horaInicio: string;
  horaFin: string;
  tema: string | null;
}

export interface OpcionHorario {
  fecha: string;
  diaSemana: DiaSemana;
  horaInicio: string;
  horaFin: string;
  alumnosDisponibles: number;
  totalAlumnos: number;
  /** 0 a 100 */
  porcentaje: number;
  alumnosConConflicto: number[];
  docenteDisponible: boolean;
}

export interface PropuestaRequest {
  claseId: number;
  fecha: string;
  horaInicio: string;
  horaFin: string;
  manual: boolean;
}
export interface Propuesta {
  id: number;
  claseId: number;
  curso: string;
  seccion: string;
  fechaOriginal: string;
  horaOriginalInicio: string;
  horaOriginalFin: string;
  fechaPropuesta: string;
  horaInicio: string;
  horaFin: string;
  porcentajeCoincidencia: number;
  alumnosConConflicto: number;
  manual: boolean;
}

// ---------- Solicitudes ----------
export interface Solicitud {
  id: number;
  propuestaId: number;
  claseId: number;
  curso: string;
  seccion: string;
  docente: string;
  fechaOriginal: string;
  horaOriginalInicio: string;
  horaOriginalFin: string;
  fechaPropuesta: string;
  horaInicio: string;
  horaFin: string;
  porcentajeCoincidencia: number;
  alumnosConConflicto: number;
  estado: EstadoSolicitud;
  observacion: string | null;
  fechaSolicitud: string;
  fechaResolucion: string | null;
  resueltoPor: string | null;
}

// ---------- Disponibilidad ----------
export interface Bloque {
  id: number;
  diaSemana: DiaSemana;
  horaInicio: string;
  horaFin: string;
  motivo: string | null;
}
export interface BloqueRequest {
  diaSemana: DiaSemana;
  horaInicio: string;
  horaFin: string;
  motivo?: string | null;
}

// ---------- Notificaciones ----------
export interface Notificacion {
  id: number;
  solicitudId: number | null;
  mensaje: string;
  leida: boolean;
  fecha: string;
}
