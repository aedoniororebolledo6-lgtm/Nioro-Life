# Nioro Life — app Android nativa (Kotlin + Jetpack Compose)

App 100 % nativa: sin web, sin WebView. Funciona sin internet.

- **Inicio:** horas, minutos y segundos que te quedan para dormir, con anillo y luna de vidrio.
- **Reloj:** hora de dormir y de despertar, alarma de despertar, aviso previo y ciclos de sueño.
- **Notas:** botón flotante "+" para crear las que quieras, con recordatorio opcional (fecha y hora).
- **Alarmas del sistema (AlarmManager):** suenan con la app cerrada y el teléfono bloqueado, y se reprograman tras reiniciar.

## Descargar el APK
Cada vez que se sube un cambio, GitHub lo compila solo (pestaña **Actions**, 5–10 min) y publica:

`https://github.com/aedoniororebolledo6-lgtm/Nioro-Life/releases/latest/download/NioroLife.apk`

Convierte ese enlace en QR y escanéalo con el S21. Al abrir el APK, Android pide permitir
"Instalar apps desconocidas". Acepta las notificaciones cuando la app las pida.

## Compilar en Android Studio
Abre esta carpeta con Android Studio → espera a que Gradle sincronice →
**Build → Build APK(s)** (o el botón ▶ con el móvil conectado).
