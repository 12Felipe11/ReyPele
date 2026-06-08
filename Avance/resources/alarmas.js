'use strict';
// ================================================================= ALARMAS VIEW

function renderAlarmas() {
  updateAlarmView();
  var display = document.getElementById('alarmDisplay');
  if (display) display.classList.toggle('active-alarm', alarmState);

  if (!A || !A.auditLog) return;

  var rows = A.auditLog.filter(function(e) { return e.action.indexOf('ALARM') >= 0; });
  document.getElementById('alarmBody').innerHTML = rows.length === 0
    ? '<tr><td colspan="3" class="no-data-cell">Sin historial de alarmas</td></tr>'
    : rows.map(function(r) {
        return '<tr>' +
               '<td style="white-space:nowrap;color:#64748b">' + r.timestamp + '</td>' +
               '<td>' + auditBadge(r.action) + '</td>' +
               '<td>' + (r.description || '') + '</td>' +
               '</tr>';
      }).join('');
}

// ----------------------------------------------------------------- ALARM CONTROL

function toggleAlarm() {
  alarmState = !alarmState;
  updateAlarmView();
  var display = document.getElementById('alarmDisplay');
  if (display) display.classList.toggle('active-alarm', alarmState);
  sendCmd('ALARM ' + (alarmState ? 'ON' : 'OFF'));
}

function updateAlarmView() {
  var big    = document.getElementById('alarmBig');
  var desc   = document.getElementById('alarmDesc');
  var toggle = document.getElementById('alarmToggle');
  if (!big) return;
  if (alarmState) {
    big.textContent    = 'ACTIVA';
    big.className      = 'alarm-big activa';
    desc.textContent   = 'Alarma activa — haga clic para desactivar';
    toggle.textContent = '🔕 Desactivar alarma';
    toggle.className   = 'alarm-toggle activa';
  } else {
    big.textContent    = 'OFF';
    big.className      = 'alarm-big';
    desc.textContent   = 'Sistema normal';
    toggle.textContent = '🔔 Activar alarma';
    toggle.className   = 'alarm-toggle inactiva';
  }
}
