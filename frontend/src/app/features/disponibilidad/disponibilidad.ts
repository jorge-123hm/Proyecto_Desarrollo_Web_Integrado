import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Bloque, DIAS, DIA_LABEL, DiaSemana } from '../../core/models/models';
import { DisponibilidadService } from '../../core/services/disponibilidad.service';
import { hhmm, mensajeError, minutos } from '../../core/util/format';
import { ConfirmService } from '../../shared/confirm';
import { HoraPipe } from '../../shared/pipes';
import { ToastService } from '../../shared/toast';
import { Cargando } from '../../shared/ui';

const HORA_INICIO = 6;   // el calendario empieza a las 06:00
const HORA_FIN = 23;     // y termina a las 23:00
const PX_POR_HORA = 44;

interface Pintado { b: Bloque; top: number; alto: number; }

@Component({
  selector: 'app-disponibilidad',
  imports: [ReactiveFormsModule, HoraPipe, Cargando],
  template: `
    <div class="page-head">
      <div>
        <h1>Mi disponibilidad</h1>
        <p class="muted">
          Registre los horarios en los que <strong>NO</strong> puede asistir a clase (trabajo, otros cursos, etc.).
          El sistema los usa para sugerir la mejor fecha de reprogramación.
        </p>
      </div>
    </div>

    <div class="split">
      <section class="card">
        <h3>Agregar bloque</h3>
        <form [formGroup]="form" (ngSubmit)="agregar()" novalidate>
          <div class="field">
            <label for="dia">Día</label>
            <select id="dia" formControlName="diaSemana">
              @for (d of dias; track d) { <option [value]="d">{{ label[d] }}</option> }
            </select>
          </div>
          <div class="row2">
            <div class="field">
              <label for="ini">Desde</label>
              <input id="ini" type="time" formControlName="horaInicio" />
            </div>
            <div class="field">
              <label for="fin">Hasta</label>
              <input id="fin" type="time" formControlName="horaFin" />
            </div>
          </div>
          @if (rangoInvalido()) {
            <small class="err">La hora de inicio debe ser anterior a la hora de fin</small>
          }
          <div class="field">
            <label for="motivo">Motivo (opcional)</label>
            <input id="motivo" type="text" formControlName="motivo" maxlength="200" placeholder="Ej: Trabajo, Cálculo II…" />
          </div>
          <button class="btn btn-primary btn-block" type="submit" [disabled]="guardando()">
            {{ guardando() ? 'Guardando…' : 'Agregar' }}
          </button>
        </form>
      </section>

      <section class="card card-flush">
        @if (cargando()) {
          <app-cargando />
        } @else {
          <div class="cal-wrap">
            <div class="cal">
              <div class="cal-corner"></div>
              @for (d of dias; track d) { <div class="cal-dia">{{ label[d] }}</div> }

              <div class="cal-horas" [style.height.px]="alto">
                @for (h of horas; track h) {
                  <span class="cal-hora" [style.top.px]="(h - inicio) * pxHora">{{ h }}:00</span>
                }
              </div>

              @for (d of dias; track d) {
                <div class="cal-col" [style.height.px]="alto">
                  @for (p of porDia()[d]; track p.b.id) {
                    <button type="button" class="cal-bloque" [style.top.px]="p.top" [style.height.px]="p.alto"
                            [title]="'Eliminar: ' + (p.b.motivo || 'Sin motivo') + ' (' + (p.b.horaInicio | hora) + '–' + (p.b.horaFin | hora) + ')'"
                            (click)="eliminar(p.b)">
                      <strong>{{ p.b.horaInicio | hora }}–{{ p.b.horaFin | hora }}</strong>
                      @if (p.alto >= 40) { <span>{{ p.b.motivo || 'Ocupado' }}</span> }
                    </button>
                  }
                </div>
              }
            </div>
          </div>
          <p class="hint">
            {{ bloques().length }} bloque(s) registrado(s). Haga clic en un bloque para eliminarlo.
          </p>
        }
      </section>
    </div>
  `,
})
export class Disponibilidad {
  private readonly svc = inject(DisponibilidadService);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  private readonly confirm = inject(ConfirmService);

  protected readonly dias = DIAS;
  protected readonly label = DIA_LABEL;
  protected readonly inicio = HORA_INICIO;
  protected readonly pxHora = PX_POR_HORA;
  protected readonly alto = (HORA_FIN - HORA_INICIO) * PX_POR_HORA;
  protected readonly horas = Array.from({ length: HORA_FIN - HORA_INICIO }, (_, i) => HORA_INICIO + i);

  protected readonly bloques = signal<Bloque[]>([]);
  protected readonly cargando = signal(true);
  protected readonly guardando = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    diaSemana: ['MONDAY' as DiaSemana, Validators.required],
    horaInicio: ['08:00', Validators.required],
    horaFin: ['10:00', Validators.required],
    motivo: [''],
  });

  protected readonly porDia = computed(() => {
    const mapa = {} as Record<DiaSemana, Pintado[]>;
    for (const d of DIAS) mapa[d] = [];
    const base = HORA_INICIO * 60;
    const tope = HORA_FIN * 60;
    for (const b of this.bloques()) {
      const ini = Math.max(minutos(hhmm(b.horaInicio)), base);
      const fin = Math.min(minutos(hhmm(b.horaFin)), tope);
      if (fin <= ini) continue; // fuera del rango visible
      mapa[b.diaSemana].push({
        b,
        top: ((ini - base) / 60) * PX_POR_HORA,
        alto: Math.max(((fin - ini) / 60) * PX_POR_HORA - 2, 18),
      });
    }
    return mapa;
  });

  constructor() {
    this.cargar();
  }

  protected rangoInvalido(): boolean {
    const { horaInicio, horaFin } = this.form.getRawValue();
    return !!horaInicio && !!horaFin && horaInicio >= horaFin;
  }

  private cargar() {
    this.svc.listar().subscribe({
      next: (l) => {
        this.bloques.set(l);
        this.cargando.set(false);
      },
      error: (e) => {
        this.cargando.set(false);
        this.toast.error(mensajeError(e));
      },
    });
  }

  protected agregar() {
    if (this.form.invalid || this.rangoInvalido()) {
      this.form.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.svc.agregar(this.form.getRawValue()).subscribe({
      next: (b) => {
        this.bloques.update((l) => [...l, b]);
        this.guardando.set(false);
        this.form.patchValue({ motivo: '' });
        this.toast.ok('Bloque agregado');
      },
      error: (e) => {
        this.guardando.set(false);
        this.toast.error(mensajeError(e));
      },
    });
  }

  protected async eliminar(b: Bloque) {
    const ok = await this.confirm.preguntar({
      titulo: 'Eliminar bloque',
      mensaje: `¿Eliminar el bloque del ${DIA_LABEL[b.diaSemana].toLowerCase()} de ${hhmm(b.horaInicio)} a ${hhmm(b.horaFin)}?`,
      confirmar: 'Eliminar',
      peligro: true,
    });
    if (!ok) return;
    this.svc.eliminar(b.id).subscribe({
      next: () => {
        this.bloques.update((l) => l.filter((x) => x.id !== b.id));
        this.toast.ok('Bloque eliminado');
      },
      error: (e) => this.toast.error(mensajeError(e)),
    });
  }
}
