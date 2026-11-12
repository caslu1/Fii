#!/bin/bash
# Captura telas do app no emulador (dados reais da B3/CVM, carteira de demonstração)
set -x
mkdir -p shots
adb shell settings put global window_animation_scale 0
adb install -r Cotista.apk
adb shell am start -n br.com.cotista/.MainActivity --ez demo true
sleep 95
adb exec-out screencap -p > shots/1_inicio.png
adb shell input swipe 540 1900 540 700 500; sleep 3
adb exec-out screencap -p > shots/1_inicio_b.png
for t in carteira agenda explorar; do
  adb shell am start -S -n br.com.cotista/.MainActivity --es tab $t
  sleep 30
  adb exec-out screencap -p > shots/2_$t.png
done
adb shell am start -S -n br.com.cotista/.MainActivity --es fund MXRF11
sleep 32
adb exec-out screencap -p > shots/3_fundo.png
adb shell input swipe 540 1900 540 700 500; sleep 3
adb exec-out screencap -p > shots/3_fundo_b.png
adb shell am start -S -n br.com.cotista/.MainActivity --es balanco MXRF11
sleep 32
adb exec-out screencap -p > shots/4_balanco.png
adb shell input swipe 540 1900 540 600 500; sleep 3
adb exec-out screencap -p > shots/4_balanco_b.png
adb logcat -d -s AndroidRuntime:E > shots/crash.txt || true
ls -la shots
