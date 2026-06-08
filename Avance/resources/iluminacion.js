'use strict';
// ================================================================= ILUMINACIÓN VIEW

var zonesInited      = false;
var lastRenderedMode = null;

function renderIluminacion() {
  var d    = S;
  var mode = (d.mode || '').toUpperCase();

  // Highlight the active mode button
  document.querySelectorAll('.mode-btn').forEach(function(b) {
    b.classList.toggle('active', b.dataset.mode === mode);
  });

  // Global slider — skip while user is dragging or recently dragged
  var gs = document.getElementById('globalSlider');
  if (gs && !gs.matches(':active') && !gs._recentInput) {
    gs.value = d.lightIntensity;
    document.getElementById('globalVal').textContent = d.lightIntensity + '%';
  }

  // Rebuild zone section when mode changes (enabled/disabled + color picker differ)
  if (mode !== lastRenderedMode) {
    zonesInited      = false;
    lastRenderedMode = mode;
  }

  // Only the two physical LED strips
  var physicalZones = (d.zones || []).filter(function(z) {
    return z.name === 'NORTE' || z.name === 'SUR';
  });

  var isManual = mode === 'MANUAL';

  if (!zonesInited) {
    buildZoneSliders(physicalZones, isManual);
    zonesInited = true;
  } else {
    updateZoneSliders(physicalZones, isManual);
  }
}

// Builds the zone section (called once per mode transition)
function buildZoneSliders(zones, isManual) {
  var notice = isManual
    ? '<div class="zone-mode-notice zone-mode-manual">Control independiente activo &mdash; cada tira LED se controla por separado</div>'
    : '<div class="zone-mode-notice">Las tiras se controlan en conjunto seg&uacute;n el modo activo</div>';

  var rows = zones.map(function(z) {
    var dis = isManual ? '' : 'disabled';
    var inp = isManual ? 'oninput="onZoneSlider(\'' + z.name + '\',this.value)"' : '';
    var colorPicker = isManual
      ? '<input type="color" class="zone-color-picker" id="zpicker-' + z.name + '" value="' + z.color + '" ' +
        'title="Color tira ' + z.name + '" ' +
        'oninput="onZoneColorPicker(\'' + z.name + '\',this.value)">'
      : '';
    return '<div class="zone-row' + (isManual ? '' : ' zone-row--disabled') + '">' +
           '<div class="zone-color-dot" id="zc-' + z.name + '" style="background:' + z.color + '" title="Color LED: ' + z.color + '"></div>' +
           '<span class="zone-name">' + z.name + '</span>' +
           '<input type="range" min="0" max="100" value="' + z.intensity + '" id="zs-' + z.name + '" ' + dis + ' ' + inp + '>' +
           '<span class="zone-val" id="zv-' + z.name + '">' + z.intensity + '%</span>' +
           colorPicker +
           '</div>';
  }).join('');

  document.getElementById('zoneSliders').innerHTML = notice + rows;
}

// Updates slider values, color dots and color pickers without rebuilding DOM
function updateZoneSliders(zones, isManual) {
  zones.forEach(function(z) {
    var sl = document.getElementById('zs-' + z.name);
    var vl = document.getElementById('zv-' + z.name);
    var cd = document.getElementById('zc-' + z.name);
    var cp = document.getElementById('zpicker-' + z.name);
    if (sl && !sl.matches(':active')) sl.value = z.intensity;
    if (vl) vl.textContent = z.intensity + '%';
    if (cd) { cd.style.background = z.color; cd.title = 'Color LED: ' + z.color; }
    // Only sync color picker if the user is not currently picking a color
    if (cp && !cp.matches(':active') && !cp._recentInput) cp.value = z.color;
  });
}

// ----------------------------------------------------------------- CALLBACKS

function onGlobalSlider(val) {
  document.getElementById('globalVal').textContent = val + '%';
  // Mark slider as recently changed to prevent immediate poll-revert
  var gs = document.getElementById('globalSlider');
  if (gs) {
    gs._recentInput = true;
    clearTimeout(gs._recentTimer);
    gs._recentTimer = setTimeout(function() { gs._recentInput = false; }, 1500);
  }
  sendCmd('LIGHT ' + val);
}

function onZoneSlider(zone, val) {
  var vl = document.getElementById('zv-' + zone);
  if (vl) vl.textContent = val + '%';
  sendCmd('ZONE ' + zone + ' ' + val);
}

// Color picker: convert #rrggbb → r g b and send COLOR command
function onZoneColorPicker(zone, hex) {
  var cp = document.getElementById('zpicker-' + zone);
  if (cp) {
    cp._recentInput = true;
    clearTimeout(cp._recentTimer);
    cp._recentTimer = setTimeout(function() { cp._recentInput = false; }, 2000);
  }
  var r = parseInt(hex.slice(1, 3), 16);
  var g = parseInt(hex.slice(3, 5), 16);
  var b = parseInt(hex.slice(5, 7), 16);
  // Also update the color dot immediately for visual feedback
  var cd = document.getElementById('zc-' + zone);
  if (cd) { cd.style.background = hex; cd.title = 'Color LED: ' + hex; }
  sendCmd('COLOR ' + zone + ' ' + r + ' ' + g + ' ' + b);
}

function apagaTodo() {
  sendCmd('MODE MANUAL');
  sendCmd('LIGHT 0');
  var gs = document.getElementById('globalSlider');
  if (gs) { gs.value = 0; document.getElementById('globalVal').textContent = '0%'; }
  document.querySelectorAll('[id^="zs-"]').forEach(function(sl) { sl.value = 0; });
  document.querySelectorAll('[id^="zv-"]').forEach(function(sp) { sp.textContent = '0%'; });
  document.querySelectorAll('.mode-btn').forEach(function(b) {
    b.classList.toggle('active', b.dataset.mode === 'MANUAL');
  });
}
