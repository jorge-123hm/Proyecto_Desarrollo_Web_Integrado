import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Curso, Usuario } from '../../core/models/models';
import { CursosService } from '../../core/services/cursos.service';
import { UsuariosService } from '../../core/services/usuarios.service';
import { mensajeError } from '../../core/util/format';
import { ConfirmService } from '../../shared/confirm';
import { Modal } from '../../shared/modal';
import { ToastService } from '../../shared/toast';
import { Cargando, Vacio } from '../../shared/ui';

@Component({
  selector: 'app-cursos',
  imports: [FormsModule, ReactiveFormsModule, Modal, Cargando, Vacio],
  template: `
    <div class="page-head">
      <div>
        <h1>Cursos y matrículas</h1>
        <p class="muted">Cree cursos, asigne docentes y matricule alumnos.</p>
      </div>
      <button type="button" class="btn btn-primary" (click)="nuevo()">+ Nuevo curso</button>
    </div>

    <div class="toolbar">
      <div class="field inline">
        <label for="doc">Docente</label>
        <select id="doc" [ngModel]="docenteFiltro()" (ngModelChange)="setDocente($event)">
          <option [ngValue]="null">Todos</option>
          @for (d of docentes(); track d.id) {
            <option [ngValue]="d.id">{{ d.nombres }} {{ d.apellidos }}</option>
          }
        </select>
      </div>
      <span class="toolbar-count muted">{{ lista().length }} curso(s)</span>
    </div>

    @if (cargando()) {
      <app-cargando />
    } @else if (lista().length === 0) {
      <app-vacio texto="No hay cursos registrados." />
    } @else {
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr><th>Curso</th><th>Sección</th><th>Periodo</th><th>Docente</th><th></th></tr>
          </thead>
          <tbody>
            @for (c of lista(); track c.id) {
              <tr>
                <td><strong>{{ c.nombre }}</strong></td>
                <td>{{ c.seccion }}</td>
                <td>{{ c.periodo }}</td>
                <td>{{ c.docenteNombre }}</td>
                <td class="t-right nowrap">
                  <button type="button" class="btn btn-sm" (click)="abrirMatriculas(c)">Alumnos</button>
                  <button type="button" class="btn btn-sm" (click)="editar(c)">Editar</button>
                  <button type="button" class="btn btn-sm btn-danger-ghost" (click)="eliminar(c)">Eliminar</button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }

    <!-- ===== Formulario de curso ===== -->
    @if (formAbierto()) {
      <app-modal [titulo]="editando() ? 'Editar curso' : 'Nuevo curso'" (cerrar)="formAbierto.set(false)">
        <form id="form-curso" [formGroup]="form" (ngSubmit)="guardar()" novalidate>
          <div class="field">
            <label for="n">Nombre</label>
            <input id="n" formControlName="nombre" maxlength="150" />
            @if (inv('nombre')) { <small class="err">Obligatorio (máx. 150)</small> }
          </div>
          <div class="row2">
            <div class="field">
              <label for="s">Sección</label>
              <input id="s" formControlName="seccion" maxlength="20" />
              @if (inv('seccion')) { <small class="err">Obligatoria</small> }
            </div>
            <div class="field">
              <label for="p">Periodo</label>
              <input id="p" formControlName="periodo" maxlength="20" placeholder="2026-II" />
              @if (inv('periodo')) { <small class="err">Obligatorio</small> }
            </div>
          </div>
          <div class="field">
            <label for="d">Docente</label>
            <select id="d" formControlName="docenteId">
              <option [ngValue]="null" disabled>Seleccione…</option>
              @for (d of docentes(); track d.id) {
                <option [ngValue]="d.id">{{ d.nombres }} {{ d.apellidos }}</option>
              }
            </select>
            @if (inv('docenteId')) { <small class="err">Seleccione un docente</small> }
          </div>
        </form>
        <ng-container pie>
          <button type="button" class="btn" (click)="formAbierto.set(false)">Cancelar</button>
          <button type="submit" form="form-curso" class="btn btn-primary" [disabled]="guardando()">
            {{ guardando() ? 'Guardando…' : 'Guardar' }}
          </button>
        </ng-container>
      </app-modal>
    }

    <!-- ===== Matrículas ===== -->
    @if (cursoMat(); as c) {
      <app-modal [titulo]="'Alumnos de ' + c.nombre + ' (' + c.seccion + ')'" ancho="lg" (cerrar)="cursoMat.set(null)">
        <div class="matricular">
          <select [ngModel]="alumnoSel()" (ngModelChange)="alumnoSel.set($event)" aria-label="Alumno a matricular">
            <option [ngValue]="null">Seleccione un alumno para matricular…</option>
            @for (a of disponibles(); track a.id) {
              <option [ngValue]="a.id">{{ a.nombres }} {{ a.apellidos }} — {{ a.cui }}</option>
            }
          </select>
          <button type="button" class="btn btn-primary" [disabled]="alumnoSel() === null" (click)="matricular(c)">
            Matricular
          </button>
        </div>

        @if (cargandoMat()) {
          <app-cargando />
        } @else if (matriculados().length === 0) {
          <app-vacio texto="Este curso aún no tiene alumnos matriculados." />
        } @else {
          <div class="table-wrap">
            <table class="table">
              <thead><tr><th>CUI</th><th>Alumno</th><th>Correo</th><th></th></tr></thead>
              <tbody>
                @for (a of matriculados(); track a.id) {
                  <tr>
                    <td>{{ a.cui }}</td>
                    <td><strong>{{ a.nombres }} {{ a.apellidos }}</strong></td>
                    <td>{{ a.correo }}</td>
                    <td class="t-right">
                      <button type="button" class="btn btn-sm btn-danger-ghost" (click)="desmatricular(c, a)">Quitar</button>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
        <ng-container pie>
          <span class="muted grow">{{ matriculados().length }} alumno(s) matriculado(s)</span>
          <button type="button" class="btn" (click)="cursoMat.set(null)">Cerrar</button>
        </ng-container>
      </app-modal>
    }
  `,
})
export class Cursos {
  private readonly svc = inject(CursosService);
  private readonly usuarios = inject(UsuariosService);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  private readonly confirm = inject(ConfirmService);

  protected readonly lista = signal<Curso[]>([]);
  protected readonly docentes = signal<Usuario[]>([]);
  protected readonly todosAlumnos = signal<Usuario[]>([]);
  protected readonly cargando = signal(true);
  protected readonly docenteFiltro = signal<number | null>(null);

  // formulario
  protected readonly formAbierto = signal(false);
  protected readonly editando = signal<Curso | null>(null);
  protected readonly guardando = signal(false);
  protected readonly form = this.fb.group({
    nombre: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(150)]),
    seccion: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(20)]),
    periodo: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(20)]),
    docenteId: this.fb.control<number | null>(null, Validators.required),
  });

  // matrículas
  protected readonly cursoMat = signal<Curso | null>(null);
  protected readonly matriculados = signal<Usuario[]>([]);
  protected readonly cargandoMat = signal(false);
  protected readonly alumnoSel = signal<number | null>(null);
  protected readonly disponibles = computed(() => {
    const ya = new Set(this.matriculados().map((a) => a.id));
    return this.todosAlumnos().filter((a) => a.activo && !ya.has(a.id));
  });

  constructor() {
    this.usuarios.listar('DOCENTE').subscribe({ next: (d) => this.docentes.set(d), error: () => {} });
    this.usuarios.listar('ALUMNO').subscribe({ next: (a) => this.todosAlumnos.set(a), error: () => {} });
    this.cargar();
  }

  protected inv(campo: string): boolean {
    const c = this.form.get(campo);
    return !!c && c.invalid && (c.touched || c.dirty);
  }

  protected setDocente(id: number | null) {
    this.docenteFiltro.set(id);
    this.cargar();
  }

  private cargar() {
    this.cargando.set(true);
    this.svc.listar(this.docenteFiltro()).subscribe({
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

  // ---------- CRUD ----------
  protected nuevo() {
    this.editando.set(null);
    this.form.reset({ nombre: '', seccion: '', periodo: '', docenteId: null });
    this.formAbierto.set(true);
  }

  protected editar(c: Curso) {
    this.editando.set(c);
    this.form.reset({ nombre: c.nombre, seccion: c.seccion, periodo: c.periodo, docenteId: c.docenteId });
    this.formAbierto.set(true);
  }

  protected guardar() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const req = { nombre: v.nombre, seccion: v.seccion, periodo: v.periodo, docenteId: v.docenteId! };
    const edit = this.editando();
    this.guardando.set(true);
    const op = edit ? this.svc.actualizar(edit.id, req) : this.svc.crear(req);
    op.subscribe({
      next: () => {
        this.guardando.set(false);
        this.formAbierto.set(false);
        this.toast.ok(edit ? 'Curso actualizado' : 'Curso creado');
        this.cargar();
      },
      error: (e) => {
        this.guardando.set(false);
        this.toast.error(mensajeError(e));
      },
    });
  }

  protected async eliminar(c: Curso) {
    const ok = await this.confirm.preguntar({
      titulo: 'Eliminar curso',
      mensaje: `¿Eliminar "${c.nombre}" (sección ${c.seccion})? Si tiene clases o matrículas asociadas no se podrá eliminar.`,
      confirmar: 'Eliminar',
      peligro: true,
    });
    if (!ok) return;
    this.svc.eliminar(c.id).subscribe({
      next: () => {
        this.toast.ok('Curso eliminado');
        this.cargar();
      },
      error: (e) => this.toast.error(mensajeError(e)),
    });
  }

  // ---------- Matrículas ----------
  protected abrirMatriculas(c: Curso) {
    this.cursoMat.set(c);
    this.alumnoSel.set(null);
    this.matriculados.set([]);
    this.cargandoMat.set(true);
    this.svc.alumnos(c.id).subscribe({
      next: (l) => {
        this.matriculados.set(l);
        this.cargandoMat.set(false);
      },
      error: (e) => {
        this.cargandoMat.set(false);
        this.toast.error(mensajeError(e));
      },
    });
  }

  protected matricular(c: Curso) {
    const id = this.alumnoSel();
    if (id === null) return;
    this.svc.matricular(c.id, id).subscribe({
      next: () => {
        const a = this.todosAlumnos().find((x) => x.id === id);
        if (a) this.matriculados.update((l) => [...l, a]);
        this.alumnoSel.set(null);
        this.toast.ok('Alumno matriculado');
      },
      error: (e) => this.toast.error(mensajeError(e)),
    });
  }

  protected async desmatricular(c: Curso, a: Usuario) {
    const ok = await this.confirm.preguntar({
      titulo: 'Quitar alumno',
      mensaje: `¿Quitar a ${a.nombres} ${a.apellidos} de ${c.nombre}?`,
      confirmar: 'Quitar',
      peligro: true,
    });
    if (!ok) return;
    this.svc.desmatricular(c.id, a.id).subscribe({
      next: () => {
        this.matriculados.update((l) => l.filter((x) => x.id !== a.id));
        this.toast.ok('Alumno retirado del curso');
      },
      error: (e) => this.toast.error(mensajeError(e)),
    });
  }
}
