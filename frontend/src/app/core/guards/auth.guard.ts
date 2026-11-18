import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Rol } from '../models/models';
import { AuthService } from '../services/auth.service';

/** Exige sesión iniciada */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.autenticado() ? true : inject(Router).createUrlTree(['/login']);
};

/** Exige uno de los roles indicados; si no, redirige al inicio del rol del usuario */
export const rolGuard = (...roles: Rol[]): CanActivateFn => () => {
  const auth = inject(AuthService);
  const rol = auth.rol();
  if (rol && roles.includes(rol)) return true;
  return inject(Router).createUrlTree([auth.rutaInicio()]);
};

/** Si ya hay sesión, /login manda al inicio */
export const invitadoGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.autenticado() ? inject(Router).createUrlTree([auth.rutaInicio()]) : true;
};

/** Ruta raíz: redirige según el rol */
export const inicioGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return inject(Router).createUrlTree([auth.rutaInicio()]);
};
