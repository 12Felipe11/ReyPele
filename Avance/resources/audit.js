'use strict';
// ================================================================= AUDITORÍA VIEW

var auditFilter = 'todos';

function setAuditFilter(f, btn) {
  auditFilter = f;
  document.querySelectorAll('.filter-btn').forEach(function(b) { b.classList.remove('active'); });
  if (btn) btn.classList.add('active');
  renderAuditoria();
}

function renderAuditoria() {
  if (!A || !A.auditLog) {
    document.getElementById('auditBody').innerHTML =
      '<tr><td colspan="3" class="no-data-cell">Cargando auditoría...</td></tr>';
    return;
  }

  var rows = auditFilter === 'todos'
    ? A.auditLog
    : A.auditLog.filter(function(e) { return e.action.indexOf(auditFilter) >= 0; });

  document.getElementById('auditBody').innerHTML = rows.length === 0
    ? '<tr><td colspan="3" class="no-data-cell">Sin registros para este filtro</td></tr>'
    : rows.map(function(r) {
        return '<tr>' +
               '<td style="white-space:nowrap;color:#64748b">' + r.timestamp + '</td>' +
               '<td>' + auditBadge(r.action) + '</td>' +
               '<td>' + (r.description || '') + '</td>' +
               '</tr>';
      }).join('');
}

function auditBadge(action) {
  var cls = 'badge-default';
  if (action.indexOf('ALARM')   >= 0) cls = 'badge-alarm';
  else if (action.indexOf('MODE')    >= 0) cls = 'badge-mode';
  else if (action.indexOf('LIGHT')   >= 0) cls = 'badge-light';
  else if (action.indexOf('CONFIG')  >= 0) cls = 'badge-config';
  else if (action.indexOf('SESSION') >= 0 || action.indexOf('SYSTEM') >= 0) cls = 'badge-session';
  return '<span class="badge ' + cls + '">' + action + '</span>';
}
