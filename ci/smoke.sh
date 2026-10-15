#!/usr/bin/env bash
# Abre la app en un Android virtual y guarda el registro de lo ocurrido.
adb install -r NioroLife.apk > install.txt 2>&1
adb logcat -c
adb shell am start -W -n com.nioro.life/.MainActivity > start.txt 2>&1
sleep 25
adb logcat -d > full.log 2>&1
adb exec-out screencap -p > screen.png 2>/dev/null
{
  echo "== instalacion"; cat install.txt
  echo "== arranque"; cat start.txt
  echo "== actividad en primer plano"; adb shell dumpsys activity activities | grep -E "topResumedActivity|mResumedActivity" | head -3
  echo "== proceso vivo?"; adb shell pidof com.nioro.life || echo "NO ESTA VIVO"
  echo "== errores graves"
  grep -E "FATAL EXCEPTION|ANR in|Process: com.nioro.life|Input dispatching timed out|Skipped [0-9]+ frames|Davey" full.log | head -40
  echo "== detalle (AndroidRuntime / nioro)"
  grep -E -A25 "FATAL EXCEPTION" full.log | head -120
  grep -E "nioro|Nioro" full.log | head -60
} > report.txt 2>&1
