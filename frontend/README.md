# Frontend — Reprogramación de clases (Angular)

Consume la API Spring Boot del proyecto (`http://localhost:8080/api`).

## Requisitos
- Node 22 y npm
- Backend corriendo en el puerto 8080 (CORS del backend permite `http://localhost:4200`)

## Ejecutar
```
npm install
npm start
```
Abrir http://localhost:4200

## Configuración
La URL del backend está en `src/environments/environment.ts` (desarrollo) y `environment.prod.ts` (producción).

## Usuarios de prueba (DataInitializer, clave 123456)
coordinacion@utp.edu.pe · docente@utp.edu.pe · alumno1@utp.edu.pe … alumno4@utp.edu.pe

## Estructura
- `core/`: modelos, servicios HTTP, interceptores (JWT/401), guards por rol
- `layout/`: shell con menú por rol y campana de notificaciones
- `features/`: auth, disponibilidad, docente, solicitudes, coordinacion, notificaciones
- `shared/`: modal, confirmación, toasts, pipes y componentes de UI
