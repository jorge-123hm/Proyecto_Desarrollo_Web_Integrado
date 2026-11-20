import { Component, computed, input, linkedSignal, output } from '@angular/core';
import { DIAS, DIA_LABEL, DiaSemana } from '../core/models/models';
import { hhmm, hoyIso, minutos, sumarDias, fechaLocal, lunesDe } from '../core/util/format';
import { HoraPipe } from './pipes';

const HORA_INICIO = 6;
const HORA_FIN = 23;
const PX_POR_HORA = 44;

export type TonoHorario = 'pendiente' | 'aprobada' | 'rechazada' | 'original' | 'alta' | 'media' | 'baja';

export interface EventoHorario {
  id: string;
  fecha: string; // yyyy-MM-dd
  horaInicio: string;
  horaFin: string;
  titulo: string;
  detalle?: string;
  tono: TonoHorario;
  clicable?: boolean;
  ref?: number;
}

export interface FondoHorario {
  dia: DiaSemana;
  horaInicio: string;
  horaFin: string;
}

interface Pintado { e: EventoHorario; top: number; alto: number; izq: number; ancho: number; }
interface FondoPintado { top: number; alto: number; }

@Component({
  selector: 'app-horario-semanal',
  imports: [HoraPipe],
  template: `
    <div class="cal-nav">
      <div class="cal-nav-btns">
        <button type="button" class="btn btn-sm" (click)="mover(-7)" aria-label="Semana anterior">‹</button>
        <button type="button" class="btn btn-sm" (click)="irHoy()">Hoy</button>
        <button type="button" class="btn btn-sm" (click)="mover(7)" aria-label="Semana siguiente">›</button>
      </div>
      <strong class="cal-rango">{{ rango() }}</strong>
      <span class="cal-nav-extra"><ng-content /></span>
    </div>

    <div class="cal-wrap">
      <div class="cal">
        <div class="cal-corner"></div>
        @for (d of dias(); track d.iso) {
          <div class="cal-dia" [class.cal-hoy]="d.hoy">
            {{ d.corto }}<small>{{ d.num }}</small>
          </div>
        }

        <div class="cal-horas" [style.height.px]="alto">
          @for (h of horas; track h) {
            <span class="cal-hora" [style.top.px]="(h - inicio) * pxHora">{{ h }}:00</span>
          }
        </div>

        @for (d of dias(); track d.iso) {
          <div class="cal-col" [class.cal-col-hoy]="d.hoy" [style.height.px]="alto">
            @for (f of fondoDe(d.dia); track $index) {
              <div class="cal-fondo" [style.top.px]="f.top" [style.height.px]="f.alto"></div>
            }
            @for (p of eventosDe(d.iso); track p.e.id) {
              <button type="button" class="cal-bloque" [class]="'tono-' + p.e.tono"
                      [class.sin-click]="p.e.clicable === false"
                      [style.top.px]="p.top" [style.height.px]="p.alto"
                      [style.left]="'calc(' + p.izq + '% + 2px)'" [style.width]="'calc(' + p.ancho + '% - 4px)'"
                      [title]="p.e.titulo + (p.e.detalle ? ' · ' + p.e.detalle : '') + ' (' + (p.e.horaInicio | hora) + '–' + (p.e.horaFin | hora) + ')'"
                      (click)="clic(p.e)">
                <strong>{{ p.e.titulo }}</strong>
                @if (p.alto >= 36 && p.e.detalle) { <span>{{ p.e.detalle }}</span> }
                @if (p.alto >= 52) { <span>{{ p.e.horaInicio | hora }}–{{ p.e.horaFin | hora }}</span> }
              </button>
            }
          </div>
        }
      </div>
    </div>
  `,
})
export class HorarioSemanal {
  readonly eventos = input<EventoHorario[]>([]);
  readonly fondo = input<FondoHorario[]>([]);
  readonly semanaInicial = input<string | null>(null);
  readonly elegir = output<EventoHorario>();

  protected readonly inicio = HORA_INICIO;
  protected readonly pxHora = PX_POR_HORA;
  protected readonly alto = (HORA_FIN - HORA_INICIO) * PX_POR_HORA;
  protected readonly horas = Array.from({ length: HORA_FIN - HORA_INICIO }, (_, i) => HORA_INICIO + i);

  protected readonly lunes = linkedSignal(() => lunesDe(this.semanaInicial() ?? hoyIso()));

  protected readonly dias = computed(() => {
    const hoy = hoyIso();
    return DIAS.map((dia, i) => {
      const iso = sumarDias(this.lunes(), i);
      return { dia, iso, corto: DIA_LABEL[dia].slice(0, 3), num: fechaLocal(iso).getDate(), hoy: iso === hoy };
    });
  });

  protected readonly rango = computed(() => {
    const f = new Intl.DateTimeFormat('es-PE', { day: 'numeric', month: 'short' });
    const ini = fechaLocal(this.lunes());
    const fin = fechaLocal(sumarDias(this.lunes(), 6));
    return `${f.format(ini)} – ${f.format(fin)} ${fin.getFullYear()}`;
  });

  private readonly porFecha = computed(() => {
    const semana = new Set(this.dias().map((d) => d.iso));
    const mapa = new Map<string, EventoHorario[]>();
    for (const e of this.eventos()) {
      if (!semana.has(e.fecha)) continue;
      mapa.set(e.fecha, [...(mapa.get(e.fecha) ?? []), e]);
    }
    const pintados = new Map<string, Pintado[]>();
    for (const [fecha, lista] of mapa) pintados.set(fecha, this.distribuir(lista));
    return pintados;
  });

  private readonly fondoPorDia = computed(() => {
    const base = HORA_INICIO * 60;
    const tope = HORA_FIN * 60;
    const mapa = new Map<DiaSemana, FondoPintado[]>();
    for (const f of this.fondo()) {
      const ini = Math.max(minutos(hhmm(f.horaInicio)), base);
      const fin = Math.min(minutos(hhmm(f.horaFin)), tope);
      if (fin <= ini) continue;
      mapa.set(f.dia, [
        ...(mapa.get(f.dia) ?? []),
        { top: ((ini - base) / 60) * PX_POR_HORA, alto: ((fin - ini) / 60) * PX_POR_HORA },
      ]);
    }
    return mapa;
  });

  protected eventosDe(iso: string): Pintado[] {
    return this.porFecha().get(iso) ?? [];
  }

  protected fondoDe(dia: DiaSemana): FondoPintado[] {
    return this.fondoPorDia().get(dia) ?? [];
  }

  protected mover(dias: number) {
    this.lunes.set(sumarDias(this.lunes(), dias));
  }

  protected irHoy() {
    this.lunes.set(lunesDe(hoyIso()));
  }

  protected clic(e: EventoHorario) {
    if (e.clicable !== false) this.elegir.emit(e);
  }

  private distribuir(lista: EventoHorario[]): Pintado[] {
    const base = HORA_INICIO * 60;
    const tope = HORA_FIN * 60;
    const items = lista
      .map((e) => ({
        e,
        ini: Math.max(minutos(hhmm(e.horaInicio)), base),
        fin: Math.min(minutos(hhmm(e.horaFin)), tope),
      }))
      .filter((x) => x.fin > x.ini)
      .sort((a, b) => a.ini - b.ini || b.fin - a.fin);

    const salida: Pintado[] = [];
    let grupo: typeof items = [];
    let finGrupo = -1;

    const cerrar = () => {
      const columnas: number[] = [];
      const asignada = grupo.map((it) => {
        let c = columnas.findIndex((fin) => fin <= it.ini);
        if (c < 0) { c = columnas.length; columnas.push(it.fin); } else { columnas[c] = it.fin; }
        return c;
      });
      const n = columnas.length;
      grupo.forEach((it, i) =>
        salida.push({
          e: it.e,
          top: ((it.ini - base) / 60) * PX_POR_HORA,
          alto: Math.max(((it.fin - it.ini) / 60) * PX_POR_HORA - 2, 18),
          izq: (asignada[i] / n) * 100,
          ancho: 100 / n,
        }),
      );
      grupo = [];
    };

    for (const it of items) {
      if (grupo.length && it.ini >= finGrupo) { cerrar(); finGrupo = -1; }
      grupo.push(it);
      finGrupo = Math.max(finGrupo, it.fin);
    }
    if (grupo.length) cerrar();
    return salida;
  }
}