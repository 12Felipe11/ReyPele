'use strict';
// ================================================================= DASHBOARD VIEW

function renderDashboard() {
  var d   = S;
  var pct = d.occupancyThreshold > 0 ? Math.round(d.entryCount / d.occupancyThreshold * 100) : 0;

  var cards = [
    {
      label: 'Ocupación',
      val:   d.entryCount + ' / ' + d.occupancyThreshold,
      sub:   pct + '% de capacidad',
      cls:   pct >= 100 ? 'red' : pct >= 75 ? 'warn' : ''
    },
    {
      label: 'Distancia',
      val:   d.distanceCm + ' cm',
      sub:   d.distanceDesc,
      cls:   ''
    },
    {
      label: 'Iluminación',
      val:   d.lightIntensity + '%',
      sub:   'Intensidad global',
      cls:   ''
    },
    {
      label: 'Alarma',
      val:   d.alarmActive ? 'ACTIVA' : 'OFF',
      sub:   d.alarmActive ? 'Atención inmediata' : 'Sistema normal',
      cls:   d.alarmActive ? 'red' : ''
    },
    {
      label: 'Modo',
      val:   d.mode,
      sub:   'Modo operación',
      cls:   ''
    },
    {
      label: 'Hardware',
      val:   d.hardwareConnected ? 'Conectado' : 'Desconectado',
      sub:   'Arduino',
      cls:   d.hardwareConnected ? 'green' : 'warn'
    }
  ];

  document.getElementById('dashKpis').innerHTML = cards.map(function(c) {
    return '<div class="kpi-card ' + c.cls + '">' +
           '<div class="kpi-label">' + c.label + '</div>' +
           '<div class="kpi-val">'   + c.val   + '</div>' +
           '<div class="kpi-sub">'   + c.sub   + '</div>' +
           '</div>';
  }).join('');

  var evHtml = liveEvents.length === 0
    ? '<li><span style="color:#475569">Sin eventos recientes</span></li>'
    : liveEvents.map(function(e) {
        return '<li><span class="ev-ts">' + e.ts + '</span>' + e.msg + '</li>';
      }).join('');
  document.getElementById('liveEvents').innerHTML = evHtml;
}
