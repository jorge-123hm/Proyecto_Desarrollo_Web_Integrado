import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ROL_LABEL, Rol, Usuario } from '../../core/models/models';
import { AuthService } from '../../core/services/auth.service';
import { UsuariosService } from '../../core/services/usuarios.service';
import { mensajeError } from '../../core/util/format';
import { ConfirmService } from '../../shared/confirm';
import { Modal } from '../../shared/modal';
import { RolPipe } from '../../shared/pipes';
import { ToastService } from '../../shared/toast';
import { Cargando, Vacio } from '../../shared/ui';

@Component({
  selector: 'app-usuarios',
  imports: [FormsModule, ReactiveFormsModule, Modal, RolPipe, Cargando, Vacio],
  template: `
    <div class="page-head">
      <div>
        <h1>Usuarios</h1>
        <p class="muted">Administración de alumnos, docentes y coordinadores.</p>
      </div>
      <button type="button" class="btn btn-primary" (click)="nuevo()">+ Nuevo usuario</button>
    </div>

    <div class="toolbar">
      <div class="field inline">
        <label for="rolf">Rol</label>
        <select id="rolf" [ngModel]="rolFiltro()" (ngModelChange)="setRol($event)">
          <option value="">Todos</option>
          @for (r of roles; track r) { <option [value]="r">{{ rolLabel[r] }}</option> }
        </select>
      </div>
      <div class="field inline grow">
        <label for="q">Buscar</label>
        <input id="q" type="search" placeholder="Nombre, CUI o correo" [ngModel]="busqueda()"
               (ngModelChange)="busqueda.set($event)" />
      </div>
      <span class="toolbar-count muted">{{ filtrados().length }} usuario(s)</span>
    </div>

    @if (cargando()) {
      <app-cargando />
    } @else if (filtrados().length === 0) {
      <app-vacio texto="No se encontraron usuarios." />
    } @else {
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr><th>CUI</th><th>Nombre</th><th>Correo</th><th>Rol</th><th>Estado</th><th></th></tr>
          </thead>
          <tbody>
            @for (u of filtrados(); track u.id) {
              <tr>
                <td>{{ u.cui }}</td>
                <td><strong>{{ u.nombres }} {{ u.apellidos }}</strong></td>
                <td>{{ u.correo }}</td>
                <td><span class="badge" [class]="'badge-rol-' + u.rol.toLowerCase()">{{ u.rol | rol }}</span></td>
                <td>
                  <span class="badge" [class.badge-aprobada]="u.activo" [class.badge-neutral]="!u.activo">
                    {{ u.activo ? 'Activo' : 'Inactivo' }}
                  </span>
                </td>
                <td class="t-right nowrap">
                  <button type="button" class="btn btn-sm" (click)="editar(u)">Editar</button>
                  <button type="button" class="btn btn-sm btn-danger-ghost" [disabled]="u.id === miId()"
                          [title]="u.id === miId() ? 'No puede eliminar su propio usuario' : ''"
                          (click)="eliminar(u)">Eliminar</button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }

    @if (formAbierto()) {
      <app-modal [titulo]="editando() ? 'Editar usuario' : 'Nuevo usuario'" (cerrar)="cerrar()">
        <form id="form-usuario" [formGroup]="form" (ngSubmit)="guardar()" novalidate>
          <div class="row2">
            <div class="field">
              <label for="cui">CUI</label>
              <input id="cui" formControlName="cui" maxlength="20" />
              @if (inv('cui')) { <small class="err">Obligatorio (máx. 20)</small> }
            </div>
            <div class="field">
              <label for="rol">Rol</label>
              <select id="rol" formControlName="rol">
                @for (r of roles; track r) { <option [value]="r">{{ rolLabel[r] }}</option> }
              </select>
            </div>
          </div>
          <div class="row2">
            <div class="field">
              <label for="nom">Nombres</label>
              <input id="nom" formControlName="nombres" maxlength="100" />
              @if (inv('nombres')) { <small class="err">Obligatorio</small> }
            </div>
            <div class="field">
              <label for="ape">Apellidos</label>
              <input id="ape" formControlName="apellidos" maxlength="100" />
              @if (inv('apellidos')) { <small class="err">Obligatorio</small> }
            </div>
          </div>
          <div class="field">
            <label for="cor">Correo</label>
            <input id="cor" type="email" formControlName="correo" maxlength="120" />
            @if (inv('correo')) { <small class="err">Ingrese un correo válido</small> }
          </div>
          <div class="field">
            <label for="pwd">Contraseña @if (editando()) { <span class="muted">(vacía = no cambiar)</span> }</label>
            <input id="pwd" type="password" formControlName="password" autocomplete="new-password" />
            @if (inv('password')) { <small class="err">La contraseña es obligatoria</small> }
          </div>
          <label class="check">
            <input type="checkbox" formControlName="activo" /> Usuario activo
          </label>
        </form>
        <ng-container pie>
          <button type="button" class="btn" (click)="cerrar()">Cancelar</button>
          <button type="submit" form="form-usuario" class="btn btn-primary" [disabled]="guardando()">
            {{ guardando() ? 'Guardando…' : 'Guardar' }}
          </button>
        </ng-container>
      </app-modal>
    }
  `,
})
export class Usuarios {
  private readonly svc = inject(UsuariosService);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  private readonly confirm = inject(ConfirmService);

  protected readonly roles: Rol[] = ['ALUMNO', 'DOCENTE', 'COORDINACION'];
  protected readonly rolLabel = ROL_LABEL;
  private readonly auth = inject(AuthService);
  protected readonly miId = computed(() => this.auth.sesion()?.usuarioId);

  protected readonly lista = signal<Usuario[]>([]);
  protected readonly cargando = signal(true);
  protected readonly rolFiltro = signal<Rol | ''>('');
  protected readonly busqueda = signal('');
  protected readonly formAbierto = signal(false);
  protected readonly editando = signal<Usuario | null>(null);
  protected readonly guardando = signal(false);

  protected readonly filtrados = computed(() => {
    const q = this.busqueda().trim().toLowerCase();
    if (!q) return this.lista();
    return this.lista().filter((u) =>
      `${u.nombres} ${u.apellidos} ${u.cui} ${u.correo}`.toLowerCase().includes(q),
    );
  });

  protected readonly form = this.fb.nonNullable.group({
    cui: ['', [Validators.required, Validators.maxLength(20)]],
    nombres: ['', Validators.required],
    apellidos: ['', Validators.required],
    correo: ['', [Validators.required, Validators.email]],
    password: [''],
    rol: ['ALUMNO' as Rol, Validators.required],
    activo: [true],
  });

  constructor() {
    this.cargar();
  }

  protected inv(campo: string): boolean {
    const c = this.form.get(campo);
    return !!c && c.invalid && (c.touched || c.dirty);
  }

  protected setRol(r: Rol | '') {
    this.rolFiltro.set(r);
    this.cargar();
  }

  private cargar() {
    this.cargando.set(true);
    this.svc.listar(this.rolFiltro()).subscribe({
      next: (l) => {
        this.lista.set(l);
        this.cargando.set(false);
      },
      error: (e) => {
        this.cargando.set(false);
        this.toast.error(mensajeError(e));
      },
    });
  }

  protected nuevo() {
    this.editando.set(null);
    this.form.reset({ cui: '', nombres: '', apellidos: '', correo: '', password: '', rol: 'ALUMNO', activo: true });
    this.form.controls.password.setValidators(Validators.required); // obligatoria al crear
    this.form.controls.password.updateValueAndValidity();
    this.formAbierto.set(true);
  }

  protected editar(u: Usuario) {
    this.editando.set(u);
    this.form.reset({ ...u, password: '' });
    this.form.controls.password.clearValidators(); // opcional al editar
    this.form.controls.password.updateValueAndValidity();
    this.formAbierto.set(true);
  }

  protected cerrar() {
    this.formAbierto.set(false);
  }

  protected guardar() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const req = { ...v, password: v.password || undefined };
    const edit = this.editando();
    this.guardando.set(true);
    const op = edit ? this.svc.actualizar(edit.id, req) : this.svc.crear(req);
    op.subscribe({
      next: () => {
        this.guardando.set(false);
        this.formAbierto.set(false);
        this.toast.ok(edit ? 'Usuario actualizado' : 'Usuario creado');
        this.cargar();
      },
      error: (e) => {
        this.guardando.set(false);
        this.toast.error(mensajeError(e));
      },
    });
  }

  protected async eliminar(u: Usuario) {
    const ok = await this.confirm.preguntar({
      titulo: 'Eliminar usuario',
      mensaje: `¿Eliminar a ${u.nombres} ${u.apellidos}? Si tiene datos asociados (cursos, solicitudes) no se podrá eliminar.`,
      confirmar: 'Eliminar',
      peligro: true,
    });
    if (!ok) return;
    this.svc.eliminar(u.id).subscribe({
      next: () => {
        this.toast.ok('Usuario eliminado');
        this.cargar();
      },
      error: (e) => this.toast.error(mensajeError(e)),
    });
  }
}
