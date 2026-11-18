import { Routes } from '@angular/router';
import { authGuard, inicioGuard, invitadoGuard, rolGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [invitadoGuard],
    loadComponent: () => import('./features/auth/login').then((m) => m.Login),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell').then((m) => m.Shell),
    children: [
      // Raíz: redirige según el rol
      { path: '', pathMatch: 'full', canActivate: [inicioGuard], children: [] },

      // ----- Alumno / Docente -----
      {
        path: 'disponibilidad',
        canActivate: [rolGuard('ALUMNO', 'DOCENTE')],
        loadComponent: () =>
          import('./features/disponibilidad/disponibilidad').then((m) => m.Disponibilidad),
      },

      // ----- Todos -----
      {
        path: 'notificaciones',
        loadComponent: () =>
          import('./features/notificaciones/notificaciones').then((m) => m.Notificaciones),
      },

      // ----- Docente -----
      {
        path: 'docente/clases',
        canActivate: [rolGuard('DOCENTE')],
        loadComponent: () => import('./features/docente/clases').then((m) => m.MisClases),
      },
      {
        path: 'docente/clases/:id/proponer',
        canActivate: [rolGuard('DOCENTE')],
        loadComponent: () => import('./features/docente/proponer').then((m) => m.Proponer),
      },
      {
        path: 'docente/solicitudes',
        canActivate: [rolGuard('DOCENTE')],
        loadComponent: () =>
          import('./features/solicitudes/solicitudes-lista').then((m) => m.SolicitudesLista),
      },

      // ----- Coordinación -----
      {
        path: 'coordinacion/solicitudes',
        canActivate: [rolGuard('COORDINACION')],
        loadComponent: () =>
          import('./features/solicitudes/solicitudes-lista').then((m) => m.SolicitudesLista),
      },
      {
        path: 'coordinacion/usuarios',
        canActivate: [rolGuard('COORDINACION')],
        loadComponent: () => import('./features/coordinacion/usuarios').then((m) => m.Usuarios),
      },
      {
        path: 'coordinacion/cursos',
        canActivate: [rolGuard('COORDINACION')],
        loadComponent: () => import('./features/coordinacion/cursos').then((m) => m.Cursos),
      },

      // ----- Docente y Coordinación -----
      {
        path: 'solicitudes/:id',
        canActivate: [rolGuard('DOCENTE', 'COORDINACION')],
        loadComponent: () =>
          import('./features/solicitudes/solicitud-detalle').then((m) => m.SolicitudDetalle),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
