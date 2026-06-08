'use strict';
// ================================================================= ANALÍTICAS VIEW

var histType   = 'occupancy';
var histPeriod = 'day';
var histData   = null;   // cached last fetch result

// ----------------------------------------------------------------- MAIN RENDER (KPIs + flow + modes)

function renderAnaliticas() {
  if (!A) {
    document.getElementById('anaKpis').innerHTML = '<span class="no-data">Cargando analíticas...</span>';
    return;
  }
  var ts = A.todayStats || {};
  var fl = A.flow       || {};

  var opH   = Math.floor((ts.operatingMinutes || 0) / 60);
  var opM   = (ts.operatingMinutes || 0) % 60;
  var opStr = opH > 0 ? (opH + 'h ' + opM + 'm') : (opM + ' min');

  var cards = [
    { label: 'Ingresos hoy',     val: ts.totalEntries  || 0,   sub: 'personas registradas' },
    { label: 'Hora pico',        val: ts.peakHour      || '--', sub: (ts.peakHourEntries || 0) + ' entradas' },
    { label: 'Alarmas hoy',      val: ts.totalAlarms   || 0,   sub: 'activaciones' },
    { label: 'Tiempo operativo', val: opStr,                    sub: 'esta sesión' }
  ];
  document.getElementById('anaKpis').innerHTML = cards.map(function(c) {
    return '<div class="kpi-card">' +
           '<div class="kpi-label">' + c.label + '</div>' +
           '<div class="kpi-val">'   + c.val   + '</div>' +
           '<div class="kpi-sub">'   + c.sub   + '</div>' +
           '</div>';
  }).join('');

  // Flow stats
  document.getElementById('flowStats').innerHTML = [
    { label: 'Por minuto (últ. 5m)', val: (fl.perMinute || 0) + '/min' },
    { label: 'Última hora',          val: (fl.perHour   || 0) + ' personas' },
    { label: 'Prom. histórico',      val: (fl.historicalAvgPerMinute || 0) + '/min' }
  ].map(function(r) {
    return '<div class="flow-row"><span>' + r.label + '</span><span class="flow-val">' + r.val + '</span></div>';
  }).join('');

  // Prediction
  var pm = fl.capacityPredictionMinutes;
  var predHtml;
  if (pm === undefined || pm < 0) {
    predHtml = '<div class="predict-box"><div class="predict-num">--</div><div class="predict-sub">Sin flujo suficiente</div></div>';
  } else if (pm === 0) {
    predHtml = '<div class="predict-box full"><div class="predict-num">LLENO</div><div class="predict-sub">Aforo máximo alcanzado</div></div>';
  } else {
    predHtml = '<div class="predict-box"><div class="predict-num">' + pm + ' min</div><div class="predict-sub">para alcanzar aforo máximo</div></div>';
  }
  document.getElementById('prediction').innerHTML = predHtml;

  // Mode distribution
  var modes = A.modeStats || [];
  var total = modes.reduce(function(s, m) { return s + m.count; }, 0) || 1;
  document.getElementById('modeChart').innerHTML = modes.length === 0
    ? '<span class="no-data">Sin cambios de modo registrados</span>'
    : modes.map(function(m) {
        var pct = Math.round(m.count / total * 100);
        return '<div class="mode-bar-row">' +
               '<span class="mode-bar-label">' + m.mode + '</span>' +
               '<div class="mode-bar-track"><div class="mode-bar-fill" style="width:' + pct + '%"></div></div>' +
               '<span class="mode-bar-count">' + m.count + '</span>' +
               '</div>';
      }).join('');

  // Historical chart: only render cached data if already loaded; first load triggers fetch
  if (histData) renderHistoryChart(histData);
  // else: fetchAndRenderHistory() is called from showView() on first visit
}

// ----------------------------------------------------------------- HISTORICAL DATA

function changeHistType(type, btn) {
  histType = type;
  document.querySelectorAll('.hist-type-btn').forEach(function(b) { b.classList.remove('active'); });
  if (btn) btn.classList.add('active');
  histData = null;
  fetchAndRenderHistory();
}

function changeHistPeriod(period, btn) {
  histPeriod = period;
  document.querySelectorAll('.hist-period-btn').forEach(function(b) { b.classList.remove('active'); });
  if (btn) btn.classList.add('active');
  histData = null;
  fetchAndRenderHistory();
}

function fetchAndRenderHistory() {
  var chartEl = document.getElementById('histChart');
  if (chartEl) chartEl.innerHTML = '<span class="no-data">Cargando...</span>';

  fetch('/api/history?type=' + histType + '&period=' + histPeriod)
    .then(function(r) { return r.json(); })
    .then(function(d) {
      histData = d;
      renderHistoryChart(d);
    })
    .catch(function() {
      var el = document.getElementById('histChart');
      if (el) el.innerHTML = '<span class="no-data">Error al cargar datos históricos</span>';
    });
}

function renderHistoryChart(d) {
  var labels = d.labels || [];
  var values = d.values || [];
  var chartEl = document.getElementById('histChart');
  var summEl  = document.getElementById('histSummary');
  if (!chartEl) return;

  if (labels.length === 0) {
    chartEl.innerHTML = '<span class="no-data">Sin datos para el período seleccionado</span>';
    if (summEl) summEl.innerHTML = '';
    return;
  }

  var color = histType === 'alarms' ? '#ef4444' : '#3b82f6';
  chartEl.innerHTML = buildHistogram(labels, values, color);

  if (summEl) {
    var total  = values.reduce(function(a, b) { return a + b; }, 0);
    var maxV   = Math.max.apply(null, values);
    var maxIdx = values.indexOf(maxV);
    var avg    = (total / labels.length).toFixed(1);
    summEl.innerHTML =
      '<div class="hist-stat"><span class="hist-stat-val">' + total + '</span><span class="hist-stat-label">Total período</span></div>' +
      '<div class="hist-stat"><span class="hist-stat-val">' + maxV + '</span><span class="hist-stat-label">Máximo (' + shortLabel(labels[maxIdx], histPeriod) + ')</span></div>' +
      '<div class="hist-stat"><span class="hist-stat-val">' + avg + '</span><span class="hist-stat-label">Promedio</span></div>';
  }
}

// ----------------------------------------------------------------- CSS HISTOGRAM

var BAR_MAX_PX = 90;   // max bar height in pixels (fixed, regardless of container width)

function buildHistogram(labels, values, color) {
  var n   = labels.length;
  var max = Math.max.apply(null, values.concat([1]));

  var cols = labels.map(function(lbl, i) {
    var v      = values[i] || 0;
    var barPx  = v > 0 ? Math.max(3, Math.round(v / max * BAR_MAX_PX)) : 0;
    var valLbl = v > 0 ? v : '';
    return '<div class="hcol">' +
           '<div class="hcol-val">' + valLbl + '</div>' +
           '<div class="hcol-bar" style="height:' + barPx + 'px;background:' + color + '"></div>' +
           '<div class="hcol-lbl">' + shortLabel(lbl, histPeriod) + '</div>' +
           '</div>';
  }).join('');

  return '<div class="histo-outer"><div class="histo-inner">' + cols + '</div></div>';
}

// Shortens axis labels based on period
function shortLabel(lbl, period) {
  if (!lbl) return '';
  if (period === 'hour')  return lbl + 'h';          // "14" → "14h"
  if (period === 'day')   return lbl.substring(5);   // "2026-06-08" → "06-08"
  if (period === 'week')  return lbl.substring(5);   // "2026-W23" → "W23"
  if (period === 'month') return lbl.substring(0, 7); // "2026-06" → "2026-06"
  return lbl;
}
