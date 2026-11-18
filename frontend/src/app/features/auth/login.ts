import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/services/auth.service';
import { mensajeError } from '../../core/util/format';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  template: `
    <div class="login-page">
      <div class="login-card">
        <div class="login-head">
          <span class="brand-logo lg">↻</span>
          <h1>Reprogramación de clases</h1>
          <p class="muted">Ingrese con su correo institucional</p>
        </div>

        <form [formGroup]="form" (ngSubmit)="entrar()" novalidate>
          <div class="field">
            <label for="correo">Correo</label>
            <input id="correo" type="email" formControlName="correo" autocomplete="username"
                   placeholder="usuario@utp.edu.pe" />
            @if (visto('correo')) {
              <small class="err">Ingrese un correo válido</small>
            }
          </div>

          <div class="field">
            <label for="password">Contraseña</label>
            <input id="password" type="password" formControlName="password" autocomplete="current-password" />
            @if (visto('password')) {
              <small class="err">La contraseña es obligatoria</small>
            }
          </div>

          @if (error()) { <div class="alert alert-error">{{ error() }}</div> }

          <button class="btn btn-primary btn-block" type="submit" [disabled]="cargando()">
            {{ cargando() ? 'Ingresando…' : 'Ingresar' }}
          </button>
        </form>

        @if (!produccion) {
          <div class="demo">
            <small class="muted">Usuarios de prueba (clave 123456) — solo desarrollo</small>
            <div class="chips">
              @for (d of demo; track d.correo) {
                <button type="button" class="chip" (click)="usarDemo(d.correo)">{{ d.rol }}</button>
              }
            </div>
          </div>
        }
      </div>
    </div>
  `,
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly produccion = environment.production;
  protected readonly demo = [
    { rol: 'Coordinación', correo: 'coordinacion@utp.edu.pe' },
    { rol: 'Docente', correo: 'docente@utp.edu.pe' },
    { rol: 'Alumno 1', correo: 'alumno1@utp.edu.pe' },
    { rol: 'Alumno 2', correo: 'alumno2@utp.edu.pe' },
  ];

  protected readonly cargando = signal(false);
  protected readonly error = signal('');

  protected readonly form = this.fb.nonNullable.group({
    correo: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  protected visto(campo: 'correo' | 'password'): boolean {
    const c = this.form.controls[campo];
    return c.invalid && (c.touched || c.dirty);
  }

  protected usarDemo(correo: string) {
    this.form.setValue({ correo, password: '123456' });
  }

  protected entrar() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.cargando.set(true);
    this.error.set('');
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => this.router.navigateByUrl(this.auth.rutaInicio()),
      error: (e) => {
        this.cargando.set(false);
        this.error.set(mensajeError(e));
      },
    });
  }
}
