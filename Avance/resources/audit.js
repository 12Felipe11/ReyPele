'use strict';
// ================================================================= AUDITORÍA VIEW

var auditFilter  = 'todos';
var auditOffset  = 0;
var auditLimit   = 100;
var auditTotal   = 0;
var auditRecords = [];
var auditLoading = false;

function setAuditFilter(f, btn) {
  auditFilter  = f;
  auditOffset  = 0;
  auditRecords = [];
  document.querySelectorAll('.filter-btn').forEach(function(b) { b.classList.remove('active'); });
  if (btn) btn.classList.add('active');
  loadAudit();
}

// Called when the user navigates to the Auditoría view
function renderAuditoria() {
  if (auditRecords.length === 0 && !auditLoading) {
    loadAudit();
  } else {
    drawAuditTable();
  }
}

function loadAudit() {
  auditLoading = true;
  drawAuditTable(); // show loading state immediately

  var f = (auditFilter === 'todos') ? '' : auditFilter;
  var url = '/api/audit?filter=' + encodeURIComponent(f) +
            '&limit=' + auditLimit +
            '&offset=' + auditOffset;

  fetch(url)
    .then(function(r) { return r.json(); })
    .then(function(data) {
      auditLoading = false;
      auditTotal   = data.total || 0;
      if (auditOffset === 0) {
        auditRecords = data.records || [];
      } else {
        auditRecords = auditRecords.concat(data.records || []);
      }
      drawAuditTable();
    })
    .catch(function() {
      auditLoading = false;
      drawAuditTable();
    });
}

function loadMoreAudit() {
  auditOffset += auditLimit;
  loadAudit();
}

function drawAuditTable() {
  var tbody = document.getElementById('auditBody');
  var footer = document.getElementById('auditFooter');

  if (auditLoading && auditRecords.length === 0) {
    tbody.innerHTML = '<tr><td colspan="3" class="no-data-cell">Cargando auditoría...</td></tr>';
    if (footer) footer.innerHTML = '';
    return;
  }

  if (auditRecords.length === 0) {
    tbody.innerHTML = '<tr><td colspan="3" class="no-data-cell">Sin registros para este filtro</td></tr>';
    if (footer) footer.innerHTML = '';
    return;
  }

  tbody.innerHTML = auditRecords.map(function(r) {
    return '<tr>' +
           '<td class="audit-ts">' + (r.timestamp || '') + '</td>' +
           '<td>' + auditBadge(r.action) + '</td>' +
           '<td class="audit-desc">' + (r.description || '') + '</td>' +
           '</tr>';
  }).join('');

  // Footer: count + "Cargar más" button
  if (footer) {
    var shown = auditRecords.length;
    var info  = '<span class="audit-count">Mostrando ' + shown + ' de ' + auditTotal + ' registros</span>';
    var btn   = '';
    if (shown < auditTotal) {
      btn = '<button class="load-more-btn" onclick="loadMoreAudit()">' +
            (auditLoading ? 'Cargando...' : 'Cargar más') + '</button>';
    }
    footer.innerHTML = info + btn;
  }
}

function auditBadge(action) {
  var cls = 'badge-default';
  if      (action.indexOf('ALARM')   >= 0) cls = 'badge-alarm';
  else if (action.indexOf('MODE')    >= 0) cls = 'badge-mode';
  else if (action.indexOf('LIGHT')   >= 0) cls = 'badge-light';
  else if (action.indexOf('CONFIG')  >= 0) cls = 'badge-config';
  else if (action.indexOf('SESSION') >= 0 || action.indexOf('SYSTEM') >= 0) cls = 'badge-session';
  return '<span class="badge ' + cls + '">' + action + '</span>';
}
