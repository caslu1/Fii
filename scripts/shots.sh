#!/bin/bash
# Captura telas do app no emulador (dados reais da B3/CVM, carteira de demonstração)
set -x
mkdir -p shots
adb shell settings put global window_animation_scale 0
adb install -r Cotista.apk
adb shell am start -n br.com.cotista/.MainActivity --ez demo true
sleep 25; adb exec-out screencap -p > shots/0_carregando.png
sleep 70
adb exec-out screencap -p > shots/1_inicio.png
adb shell input swipe 540 1700 540 600 400; sleep 3
adb exec-out screencap -p > shots/2_inicio_rolagem.png
for t in carteira agenda explorar; do
  adb shell am start -S -n br.com.cotista/.MainActivity --es tab $t
  sleep 30
  adb exec-out screencap -p > shots/3_$t.png
done
adb shell am start -S -n br.com.cotista/.MainActivity --es fund MXRF11
sleep 30
adb exec-out screencap -p > shots/4_fundo.png
adb shell input swipe 540 1700 540 500 400; sleep 3
adb exec-out screencap -p > shots/5_fundo_rolagem.png
adb logcat -d -s AndroidRuntime:E > shots/crash.txt || true
ls -la shots
