import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';
import { Clase, OpcionHorario } from '../../core/models/models';
import { ClasesService } from '../../core/services/clases.service';
import { SolicitudesService } from '../../core/services/solicitudes.service';
import { hhmm, hoyIso, mensajeError } from '../../core/util/format';
import { ConfirmService } from '../../shared/confirm';
import { DiaPipe, HoraPipe } from '../../shared/pipes';
import { ToastService } from '../../shared/toast';
import { Cargando, Porcentaje } from '../../shared/ui';

@Component({
  selector: 'app-proponer',
  imports: [DatePipe, ReactiveFormsModule, RouterLink, HoraPipe, DiaPipe, Cargando, Porcentaje],
  template: `
    <a routerLink="/docente/clases" class="back">← Mis clases</a>

    @if (cargandoClase()) {
      <app-cargando />
    } @else if (clase(); as c) {
      <div class="page-head">
        <div>
          <h1>Reprogramar clase</h1>
          <p class="muted">
            {{ c.curso }} · Sección {{ c.seccion }} · actualmente el
            <strong>{{ c.fechaOriginal | date: 'EEEE d MMM y' }}</strong>
            de {{ c.horaInicio | hora }} a {{ c.horaFin | hora }}
          </p>
        </div>
      </div>

      <div class="tabs" role="tablist">
        <button type="button" role="tab" class="tab" [class.activo]="modo() === 'sugeridas'"
                (click)="modo.set('sugeridas')">Fechas sugeridas</button>
        <button type="button" role="tab" class="tab" [class.activo]="modo() === 'manual'"
                (click)="modo.set('manual')">Elegir fecha manualmente</button>
      </div>

      <!-- ============ SUGERIDAS ============ -->
      @if (modo() === 'sugeridas') {
        <section class="card">
          <form class="filtros" [formGroup]="filtros" (ngSubmit)="buscar()">
            <div class="field">
              <label for="desde">Desde</label>
              <input id="desde" type="date" formControlName="desde" [min]="manana" />
            </div>
            <div class="field">
              <label for="hasta">Hasta</label>
              <input id="hasta" type="date" formControlName="hasta" [min]="manana" />
            </div>
            <div class="field">
              <label for="limite">Cantidad</label>
              <select id="limite" formControlName="limite">
                <option [ngValue]="5">5</option>
                <option [ngValue]="10">10</option>
                <option [ngValue]="20">20</option>
              </select>
            </div>
            <button class="btn btn-primary" type="submit" [disabled]="buscando()">
              {{ buscando() ? 'Buscando…' : 'Buscar mejores fechas' }}
            </button>
          </form>
          <p class="hint">
            El sistema revisa de lunes a sábado (07:00–22:00) y ordena por la cantidad de alumnos que están libres.
          </p>
        </section>

        @if (opciones(); as ops) {
          @if (ops.length === 0) {
            <div class="vacio"><p>No hay franjas disponibles en ese rango. Pruebe ampliar las fechas.</p></div>
          } @else {
            <div class="table-wrap">
              <table class="table">
                <thead>
                  <tr>
                    <th>Fecha</th><th>Horario</th><th>Alumnos libres</th><th>Coincidencia</th><th></th>
                  </tr>
                </thead>
                <tbody>
                  @for (o of ops; track o.fecha + o.horaInicio) {
                    <tr>
                      <td>
                        <strong>{{ o.fecha | date: 'EEE d MMM' }}</strong>
                        <div class="muted small">{{ o.diaSemana | dia }}</div>
                      </td>
                      <td>{{ o.horaInicio | hora }} – {{ o.horaFin | hora }}</td>
                      <td>{{ o.alumnosDisponibles }} de {{ o.totalAlumnos }}</td>
                      <td class="w-pct"><app-porcentaje [valor]="o.porcentaje" /></td>
                      <td class="t-right">
                        <button type="button" class="btn btn-sm btn-primary" [disabled]="enviando()"
                                (click)="solicitar(c, o, false)">Solicitar</button>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        }
      }

      <!-- ============ MANUAL ============ -->
      @if (modo() === 'manual') {
        <section class="card">
          <form class="filtros" [formGroup]="manual" (ngSubmit)="evaluar()" novalidate>
            <div class="field">
              <label for="mf">Fecha</label>
              <input id="mf" type="date" formControlName="fecha" [min]="manana" />
            </div>
            <div class="field">
              <label for="mi">Inicio</label>
              <input id="mi" type="time" formControlName="horaInicio" />
            </div>
            <div class="field">
              <label for="mfi">Fin</label>
              <input id="mfi" type="time" formControlName="horaFin" />
            </div>
            <button class="btn btn-primary" type="submit" [disabled]="evaluando()">
              {{ evaluando() ? 'Evaluando…' : 'Evaluar cruces' }}
            </button>
          </form>
          @if (manual.touched && manual.invalid) {
            <small class="err">Complete la fecha y las horas</small>
          }
          <p class="hint">La fecha debe ser posterior a hoy. Se mostrará cuántos alumnos tendrían cruce de horario.</p>
        </section>

        @if (evaluacion(); as ev) {
          <section class="card resultado">
            <div class="resultado-head">
              <div>
                <h3>{{ ev.fecha | date: "EEEE d 'de' MMMM" }}</h3>
                <p class="muted">{{ ev.horaInicio | hora }} – {{ ev.horaFin | hora }}</p>
              </div>
              <app-porcentaje [valor]="ev.porcentaje" />
            </div>

            <p>
              <strong>{{ ev.alumnosDisponibles }} de {{ ev.totalAlumnos }}</strong> alumnos están libres ·
              <strong>{{ ev.alumnosConConflicto.length }}</strong> con cruce de horario.
            </p>

            @if (!ev.docenteDisponible) {
              <div class="alert alert-error">
                Usted tiene un bloque de no disponibilidad en esa franja, por lo que no puede proponerla.
              </div>
            } @else if (ev.alumnosConConflicto.length > 0) {
              <div class="alert alert-warn">Hay alumnos con cruce. Puede continuar, pero coordinación lo verá en la solicitud.</div>
            } @else {
              <div class="alert alert-ok">Ningún alumno tiene cruce en esta franja.</div>
            }

            <button type="button" class="btn btn-primary" [disabled]="!ev.docenteDisponible || enviando()"
                    (click)="solicitar(c, ev, true)">Solicitar esta fecha</button>
          </section>
        }
      }
    }
  `,
})
export class Proponer implements OnInit {
  /** Viene del parámetro :id de la ruta */
  readonly id = input.required<string>();

  private readonly fb = inject(FormBuilder);
  private readonly clases = inject(ClasesService);
  private readonly solicitudes = inject(SolicitudesService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  private readonly confirm = inject(ConfirmService);

  protected readonly manana = hoyIso(1);

  protected readonly clase = signal<Clase | null>(null);
  protected readonly cargandoClase = signal(true);
  protected readonly modo = signal<'sugeridas' | 'manual'>('sugeridas');

  protected readonly opciones = signal<OpcionHorario[] | null>(null);
  protected readonly buscando = signal(false);
  protected readonly evaluacion = signal<OpcionHorario | null>(null);
  protected readonly evaluando = signal(false);
  protected readonly enviando = signal(false);

  protected readonly filtros = this.fb.nonNullable.group({
    desde: [hoyIso(1), Validators.required],
    hasta: [hoyIso(15), Validators.required],
    limite: [10],
  });

  protected readonly manual = this.fb.nonNullable.group({
    fecha: ['', Validators.required],
    horaInicio: ['', Validators.required],
    horaFin: ['', Validators.required],
  });

  ngOnInit() {
    this.clases.mias().subscribe({
      next: (lista) => {
        const c = lista.find((x) => x.id === Number(this.id())) ?? null;
        this.clase.set(c);
        this.cargandoClase.set(false);
        if (!c) {
          this.toast.error('No se encontró la clase');
          this.router.navigateByUrl('/docente/clases');
          return;
        }
        this.manual.patchValue({ horaInicio: hhmm(c.horaInicio), horaFin: hhmm(c.horaFin) });
        this.buscar();
      },
      error: (e) => {
        this.cargandoClase.set(false);
        this.toast.error(mensajeError(e));
      },
    });
  }

  protected buscar() {
    const c = this.clase();
    if (!c || this.filtros.invalid) return;
    const { desde, hasta, limite } = this.filtros.getRawValue();
    if (hasta < desde) {
      this.toast.error('La fecha final no puede ser anterior a la inicial');
      return;
    }
    this.buscando.set(true);
    this.clases.opciones(c.id, { desde, hasta, limite }).subscribe({
      next: (ops) => {
        this.opciones.set(ops);
        this.buscando.set(false);
      },
      error: (e) => {
        this.opciones.set(null);
        this.buscando.set(false);
        this.toast.error(mensajeError(e));
      },
    });
  }

  protected evaluar() {
    const c = this.clase();
    if (!c) return;
    if (this.manual.invalid) {
      this.manual.markAllAsTouched();
      return;
    }
    const { fecha, horaInicio, horaFin } = this.manual.getRawValue();
    if (horaInicio >= horaFin) {
      this.toast.error('La hora de inicio debe ser anterior a la hora de fin');
      return;
    }
    this.evaluando.set(true);
    this.evaluacion.set(null);
    this.clases.evaluar(c.id, fecha, horaInicio, horaFin).subscribe({
      next: (ev) => {
        this.evaluacion.set(ev);
        this.evaluando.set(false);
      },
      error: (e) => {
        this.evaluando.set(false);
        this.toast.error(mensajeError(e));
      },
    });
  }

  /** Crea la propuesta y la envía como solicitud a coordinación (CU005) */
  protected async solicitar(c: Clase, o: OpcionHorario, manual: boolean) {
    const ok = await this.confirm.preguntar({
      titulo: 'Enviar solicitud de reprogramación',
      mensaje:
        `Se solicitará mover la clase de ${c.curso} al ${o.fecha.split('-').reverse().join('/')} ` +
        `de ${hhmm(o.horaInicio)} a ${hhmm(o.horaFin)} (${o.alumnosDisponibles} de ${o.totalAlumnos} alumnos libres). ` +
        `Coordinación recibirá la solicitud para su aprobación.`,
      confirmar: 'Enviar solicitud',
    });
    if (!ok) return;

    this.enviando.set(true);
    this.clases
      .crearPropuesta({
        claseId: c.id,
        fecha: o.fecha,
        horaInicio: o.horaInicio,
        horaFin: o.horaFin,
        manual,
      })
      .pipe(switchMap((p) => this.solicitudes.enviar(p.id)))
      .subscribe({
        next: (s) => {
          this.enviando.set(false);
          this.toast.ok('Solicitud enviada a coordinación');
          this.router.navigate(['/solicitudes', s.id]);
        },
        error: (e) => {
          this.enviando.set(false);
          this.toast.error(mensajeError(e));
        },
      });
  }
}
