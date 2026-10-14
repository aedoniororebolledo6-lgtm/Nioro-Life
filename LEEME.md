# Nioro Life — APK por enlace / QR

Este proyecto se compila solo en GitHub (gratis) y publica un APK con enlace fijo de descarga.

## Pasos (≈10 minutos, una sola vez)
1. Crea una cuenta en https://github.com (gratis).
2. Nuevo repositorio → nombre `nioro-life` → **Public** → Create.
3. En el repositorio: **Add file → Upload files** y arrastra TODO el contenido de esta carpeta
   (incluida la carpeta oculta `.github`; si no la ves, en Windows activa "Elementos ocultos",
   en Mac pulsa Cmd+Shift+.). Pulsa **Commit changes**.
4. Pestaña **Actions**: verás "Compilar APK" ejecutándose (tarda 5–8 min). Espera el check verde.
5. Tu enlace permanente de descarga directa es:
   `https://github.com/TU_USUARIO/nioro-life/releases/latest/download/NioroLife.apk`
6. Conviértelo en QR (por ejemplo en https://www.qr-code-generator.com o pídemelo y te lo genero)
   y escanéalo con la cámara del S21: se descarga el APK.

## Instalar en el S21
Al abrir el APK, Android pide permitir "Instalar apps desconocidas" para Chrome/Archivos: acéptalo.
Es un APK de prueba (debug), seguro para uso personal. En la primera apertura acepta las notificaciones
y, si Android lo pide, "Alarmas y recordatorios", para que suene con el teléfono bloqueado.

## Actualizar la app
Sube cambios al repositorio y se recompila solo; el mismo enlace/QR siempre baja la última versión.
