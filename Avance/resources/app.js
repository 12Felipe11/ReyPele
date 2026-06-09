'use strict';
// ================================================================= SHARED STATE
// (accessed by all view modules loaded after this file)
var S = null;          // last /api/status payload
var A = null;          // last /api/analytics payload
var currentView    = 'dashboard';
var alarmState     = false;
var liveEvents     = [];   // [{ts, msg}]
var prevEntryCount = -1;

// ================================================================= ROUTING
function showView(name, el) {
  document.querySelectorAll('.view').forEach(function(v) { v.classList.remove('active'); });
  var view = document.getElementById('view-' + name);
  if (view) view.classList.add('active');

  document.querySelectorAll('.nav-item').forEach(function(a) { a.classList.remove('active'); });
  if (el) el.classList.add('active');

  currentView = name;
  renderCurrentView();

  if (name === 'analiticas' || name === 'auditoria' || name === 'alarmas') {
    fetchAnalytics();
  }
  if (name === 'analiticas') {
    fetchAndRenderHistory(); // defined in analytics.js
  }
}

// ================================================================= FETCH LOOPS
function fetchStatus() {
  fetch('/api/status')
    .then(function(r) { return r.json(); })
    .then(function(d) {
      S = d;
      updateHwStatus(d.hardwareConnected);
      syncAlarmFromServer(d.alarmActive);
      trackEntry(d.entryCount);
      renderCurrentView();
    })
    .catch(function() {});
}

function fetchAnalytics() {
  fetch('/api/analytics')
    .then(function(r) { return r.json(); })
    .then(function(d) {
      A = d;
      if (currentView === 'analiticas' || currentView === 'auditoria' || currentView === 'alarmas') {
        renderCurrentView();
      }
    })
    .catch(function() {});
}

// Dispatches to the correct view render function (each defined in its own .js file)
function renderCurrentView() {
  if (!S) return;
  switch (currentView) {
    case 'dashboard':     renderDashboard();     break;
    case 'iluminacion':   renderIluminacion();   break;
    case 'alarmas':       renderAlarmas();       break;
    case 'analiticas':    renderAnaliticas();    break;
    case 'auditoria':     renderAuditoria();     break;
    case 'configuracion': renderConfiguracion(); break;
  }
}

// ================================================================= HW STATUS
function updateHwStatus(connected) {
  var el  = document.getElementById('hwStatus');
  var lbl = el.querySelector('.hw-label');
  el.className = 'hw-status ' + (connected ? 'connected' : 'disconnected');
  lbl.textContent = connected ? 'Conectado' : 'Sin conexión';
}

// ================================================================= ENTRY TRACKING
function trackEntry(count) {
  if (prevEntryCount >= 0 && count > prevEntryCount) {
    var ts = new Date().toLocaleTimeString('es-CO', {hour:'2-digit', minute:'2-digit', second:'2-digit'});
    var delta = count - prevEntryCount;
    for (var i = 1; i <= delta; i++) {
      liveEvents.unshift({ ts: ts, msg: 'Nueva entrada. Total: ' + (prevEntryCount + i) });
    }
    if (liveEvents.length > 20) liveEvents.length = 20;
  }
  prevEntryCount = count;
}

// ================================================================= ALARM SYNC
function syncAlarmFromServer(active) {
  alarmState = active;
  updateAlarmView(); // defined in alarmas.js
}

// ================================================================= SEND COMMAND (fire-and-forget)
function sendCmd(cmd) {
  fetch('/api/command', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ cmd: cmd })
  }).catch(function() {});
}

// ================================================================= INIT
setInterval(fetchStatus,    1000);
setInterval(fetchAnalytics, 5000);
// Refresca gráficas históricas cada 2 min (cubre cambio de período en segundo plano)
setInterval(function() { if (typeof fetchAndRenderHistory === 'function') fetchAndRenderHistory(); }, 120000);
fetchStatus();
fetchAnalytics();
// Carga inicial de datos históricos sin esperar a que el usuario navegue a Analíticas
setTimeout(function() { if (typeof fetchAndRenderHistory === 'function') fetchAndRenderHistory(); }, 1500);
