'use strict';
// ================================================================= CONFIGURACIÓN VIEW

function renderConfiguracion() {
  var dist   = document.getElementById('cfgDist');
  var cap    = document.getElementById('cfgCap');
  var modeEl = document.getElementById('cfgMode');
  if (!dist || !cap || !modeEl || !S) return;

  if (!dist.matches(':focus'))   dist.value  = S.distanceThreshold  || 30;
  if (!cap.matches(':focus'))    cap.value   = S.occupancyThreshold || 1000;
  if (!modeEl.matches(':focus')) {
    var m   = (S.mode || 'MANUAL').toUpperCase();
    var opt = Array.from(modeEl.options).find(function(o) { return o.value === m; });
    if (opt) modeEl.value = m;
  }
}

function saveConfig() {
  var dist = parseFloat(document.getElementById('cfgDist').value);
  var cap  = parseInt(document.getElementById('cfgCap').value, 10);
  var mode = document.getElementById('cfgMode').value;

  if (!isNaN(dist) && dist > 0) sendCmd('SET DISTANCE ' + dist);
  if (!isNaN(cap)  && cap  > 0) sendCmd('SET THRESHOLD ' + cap);
  if (mode)                     sendCmd('MODE ' + mode);

  var msg = document.getElementById('cfgMsg');
  msg.textContent = '✓ Configuración guardada';
  setTimeout(function() { msg.textContent = ''; }, 3000);
}
