#!/bin/bash
# Captura telas do app no emulador (dados reais da B3/CVM, carteira de demonstração)
set -x
mkdir -p shots
adb shell settings put global window_animation_scale 0
adb install -r Cotista.apk
adb shell am start -n br.com.cotista/.MainActivity --ez demo true
sleep 95
adb exec-out screencap -p > shots/1_inicio.png
# detalhe com o resumo do balanço
adb shell am start -S -n br.com.cotista/.MainActivity --es fund HGLG11
sleep 35
adb shell input swipe 540 1900 540 700 500; sleep 3
adb exec-out screencap -p > shots/2_fundo_resumo_balanco.png
# tela de balanço completa (papel e tijolo)
for f in MXRF11 HGLG11; do
  adb shell am start -S -n br.com.cotista/.MainActivity --es balanco $f
  sleep 35
  adb exec-out screencap -p > shots/3_balanco_${f}_a.png
  adb shell input swipe 540 1900 540 500 600; sleep 3
  adb exec-out screencap -p > shots/3_balanco_${f}_b.png
  adb shell input swipe 540 1900 540 500 600; sleep 3
  adb exec-out screencap -p > shots/3_balanco_${f}_c.png
  adb shell input swipe 540 1900 540 500 600; sleep 4
  adb exec-out screencap -p > shots/3_balanco_${f}_d.png
done
adb logcat -d -s AndroidRuntime:E > shots/crash.txt || true
ls -la shots
