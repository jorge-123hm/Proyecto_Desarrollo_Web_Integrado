import { Pipe, PipeTransform } from '@angular/core';
import { DIA_LABEL, DiaSemana, ROL_LABEL, Rol } from '../core/models/models';
import { hhmm } from '../core/util/format';

@Pipe({ name: 'hora' })
export class HoraPipe implements PipeTransform {
  transform(v: string | null | undefined): string { return hhmm(v); }
}

@Pipe({ name: 'dia' })
export class DiaPipe implements PipeTransform {
  transform(v: DiaSemana | null | undefined): string { return v ? DIA_LABEL[v] : ''; }
}

@Pipe({ name: 'rol' })
export class RolPipe implements PipeTransform {
  transform(v: Rol | null | undefined): string { return v ? ROL_LABEL[v] : ''; }
}
