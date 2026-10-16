#!/usr/bin/env bash
# Abre la app en un Android virtual, navega por las pantallas y guarda informe + capturas.
set +e
P=com.nioro.life
adb install -r NioroLife.apk > install.txt 2>&1
adb shell pm grant $P android.permission.POST_NOTIFICATIONS
adb logcat -c
adb shell am start -W -n $P/.MainActivity > start.txt 2>&1
sleep 10

ui()   { adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1; adb pull /sdcard/ui.xml ui.xml > /dev/null 2>&1; }
shot() { adb exec-out screencap -p > "$1.png"; }
tap()  { ui; XY=$(python3 ci/tap.py "$1") && { adb shell input tap $XY; echo "  tap '$1' ($XY)"; } || echo "  NO ENCONTRADO: '$1'"; sleep "${2:-2}"; }
type_in() { tap "$1" 1; adb shell input text "$2"; sleep 1; }
look() { ui; echo "[$1] $(python3 ci/texts.py)"; shot "$1"; }
back() { adb shell input keyevent 4; sleep 2; }

{
  echo "== PASOS"
  look 01_home_noche

  tap "=Finanzas"; look 02_finanzas_vacio
  tap "+ Ingreso"; look 03_dialogo_ingreso
  type_in "Monto o cuenta" "450000+35000"; look 04_calculadora
  type_in "Concepto" "Sueldo"
  type_in "Por qu" "Trabajo"
  back; tap "=Guardar" 2; look 05_ingreso_guardado
  tap "+ Gasto"; type_in "Monto o cuenta" "120000"; type_in "Concepto" "Arriendo"; type_in "Por qu" "Vivienda"
  back; tap "=Guardar" 2
  tap "+ Deuda"; type_in "Monto o cuenta" "30000"; type_in "debes" "Prestamo"; type_in "Por qu" "Amigo"
  back; tap "=Guardar" 2; look 06_finanzas_noche
  back; look 07_home_con_saldo

  tap "Men" 2; look 08_menu_noche
  tap "=Día"; look 09_menu_dia
  tap "Buscar actualiz" 9; look 10_actualizacion
  back; look 11_home_dia
  tap "=Reloj"; look 12_reloj_dia; back
  tap "=Notas"; look 13_notas_dia; back
  tap "=Finanzas"; look 14_finanzas_dia; back
} > steps.txt 2>&1

adb logcat -d > full.log 2>&1
{
  echo "== instalacion"; cat install.txt
  echo "== arranque"; cat start.txt
  echo "== proceso vivo?"; adb shell pidof $P || echo "NO ESTA VIVO"
  echo "== errores graves"
  grep -E "FATAL EXCEPTION|ANR in|Process: $P" full.log | head -20
  grep -E -A14 "FATAL EXCEPTION" full.log | head -80
  echo; cat steps.txt
} > report.txt 2>&1
