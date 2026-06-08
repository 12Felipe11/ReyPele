'use strict';
// ================================================================= ILUMINACIÓN VIEW

var zonesInited = false;

function renderIluminacion() {
  var d    = S;
  var mode = (d.mode || '').toUpperCase();

  // Highlight the active mode button
  document.querySelectorAll('.mode-btn').forEach(function(b) {
    b.classList.toggle('active', b.dataset.mode === mode);
  });

  // Global slider — skip while user is dragging
  var gs = document.getElementById('globalSlider');
  if (gs && !gs.matches(':active')) {
    gs.value = d.lightIntensity;
    document.getElementById('globalVal').textContent = d.lightIntensity + '%';
  }

  // Zone sliders + color dots
  if (!zonesInited) {
    buildZoneSliders(d.zones || []);
    zonesInited = true;
  } else {
    updateZoneSliders(d.zones || []);
  }
}

// Builds zone rows with color dot + slider + value label (called once)
function buildZoneSliders(zones) {
  document.getElementById('zoneSliders').innerHTML = zones.map(function(z) {
    return '<div class="zone-row">' +
           '<div class="zone-color-dot" id="zc-' + z.name + '" style="background:' + z.color + '" title="Color LED: ' + z.color + '"></div>' +
           '<span class="zone-name">' + z.name + '</span>' +
           '<input type="range" min="0" max="100" value="' + z.intensity + '" ' +
                  'id="zs-' + z.name + '" ' +
                  'oninput="onZoneSlider(\'' + z.name + '\',this.value)">' +
           '<span class="zone-val" id="zv-' + z.name + '">' + z.intensity + '%</span>' +
           '</div>';
  }).join('');
}

// Updates slider values and color dots without rebuilding DOM
function updateZoneSliders(zones) {
  zones.forEach(function(z) {
    var sl = document.getElementById('zs-' + z.name);
    var vl = document.getElementById('zv-' + z.name);
    var cd = document.getElementById('zc-' + z.name);
    if (sl && !sl.matches(':active')) sl.value = z.intensity;
    if (vl) vl.textContent = z.intensity + '%';
    if (cd) { cd.style.background = z.color; cd.title = 'Color LED: ' + z.color; }
  });
}

// ----------------------------------------------------------------- CALLBACKS

function onGlobalSlider(val) {
  document.getElementById('globalVal').textContent = val + '%';
  sendCmd('LIGHT ' + val);
}

function onZoneSlider(zone, val) {
  var vl = document.getElementById('zv-' + zone);
  if (vl) vl.textContent = val + '%';
  sendCmd('ZONE ' + zone + ' ' + val);
}

function apagaTodo() {
  sendCmd('LIGHT 0');
  // Optimistic UI update so the user sees instant feedback
  var gs = document.getElementById('globalSlider');
  if (gs) { gs.value = 0; document.getElementById('globalVal').textContent = '0%'; }
  document.querySelectorAll('[id^="zs-"]').forEach(function(sl) { sl.value = 0; });
  document.querySelectorAll('[id^="zv-"]').forEach(function(sp) { sp.textContent = '0%'; });
}
