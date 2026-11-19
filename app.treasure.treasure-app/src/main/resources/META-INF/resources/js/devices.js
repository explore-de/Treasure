/* Behaviour for the device pages (list, create, edit).
   Served as a static file on purpose: Qute would otherwise try to parse the
   braces in this script as template expressions. */
(function () {
  'use strict';

  // query parameter and the key of its chip label in the #i18n texts
  var FILTERS = [
    ['name', 'filterName'],
    ['status', 'filterStatus'],
    ['assignedTo', 'filterAssignedTo'],
    ['serial', 'filterSerial'],
    ['group', 'filterGroup'],
    ['model', 'filterModel'],
    ['damage', 'filterDamage'],
    ['company', 'filterCompany'],
    ['deviceNumber', 'filterDeviceNumber'],
    ['deviceProzessor', 'filterProcessor'],
    ['deviceHDDStorage', 'filterStorage'],
    ['deviceRAM', 'filterRam'],
    ['deviceModelDate', 'filterModelDate']
  ];

  // Every text this script shows comes from the data attributes of #i18n in
  // main.html, already in the language of the page (messages*.properties).
  // t('key', a, b) fills the %s places of the text in order.
  function t(key) {
    var holder = document.getElementById('i18n');
    var text = holder && holder.dataset[key] ? holder.dataset[key] : key;
    var args = Array.prototype.slice.call(arguments, 1);
    return text.replace(/%s/g, function () { return args.length ? String(args.shift()) : ''; });
  }

  function $(selector, root) { return (root || document).querySelector(selector); }
  function $$(selector, root) { return Array.prototype.slice.call((root || document).querySelectorAll(selector)); }
  function params() { return new URLSearchParams(window.location.search); }
  function currentPath() { return window.location.pathname + window.location.search; }
  function navigate(p) {
    // a changed filter changes the list, so paging starts over
    p.delete('page');
    var qs = p.toString();
    window.location.href = window.location.pathname + (qs ? '?' + qs : '');
  }
  function removeValue(p, key, value) {
    var all = p.getAll(key);
    p.delete(key);
    var removed = false;
    all.forEach(function (v) {
      if (!removed && v === value) { removed = true; return; }
      p.append(key, v);
    });
  }
  function clearAllFilters() {
    var p = params();
    FILTERS.forEach(function (f) { p.delete(f[0]); });
    navigate(p);
  }

  /* ---------- quick search ---------- */

  function initQuickSearch() {
    var form = $('#quickSearchForm');
    var input = $('#quickSearchInput');
    var clear = $('#quickSearchClear');
    if (!form || !input) return;

    function sync() { form.classList.toggle('has-value', input.value.trim() !== ''); }
    input.addEventListener('input', sync);
    sync();

    form.addEventListener('submit', function (e) {
      e.preventDefault();
      var value = input.value.trim();
      if (!value) return;
      var p = params();
      if (p.getAll('name').indexOf(value) === -1) p.append('name', value);
      navigate(p);
    });
    if (clear) {
      clear.addEventListener('click', function () {
        input.value = '';
        sync();
        input.focus();
      });
    }
  }

  /* ---------- filter panel ---------- */

  function initFilters() {
    var toggle = $('#filterToggle');
    var panel = $('#filterPanel');
    if (!toggle || !panel) return;

    function setOpen(open) {
      panel.classList.toggle('is-open', open);
      toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
      if (open) {
        var first = panel.querySelector('[data-filter]');
        if (first) first.focus();
      }
    }
    function apply() {
      var p = params();
      var changed = 0;
      $$('[data-filter]', panel).forEach(function (el) {
        var key = el.dataset.filter;
        var value = el.value.trim();
        var initial = el.dataset.initial || '';
        if (value === initial) return;
        // a field shown with its active filter replaces that filter when edited
        if (initial) removeValue(p, key, initial);
        if (value && p.getAll(key).indexOf(value) === -1) p.append(key, value);
        changed++;
      });
      if (changed > 0) navigate(p); else setOpen(false);
    }

    // show a filter in its field when it is the only value for that key,
    // so the panel agrees with the chips (e.g. after /devices?status=available)
    var current = params();
    $$('[data-filter]', panel).forEach(function (el) {
      var values = current.getAll(el.dataset.filter);
      if (values.length === 1) el.value = values[0];
      el.dataset.initial = el.value;
    });

    toggle.addEventListener('click', function () { setOpen(!panel.classList.contains('is-open')); });
    $('#applyFilters').addEventListener('click', apply);
    $('#clearFilters').addEventListener('click', clearAllFilters);
    panel.addEventListener('keydown', function (e) {
      if (e.key === 'Enter' && e.target.matches('input, select')) {
        e.preventDefault();
        apply();
      }
      if (e.key === 'Escape') setOpen(false);
    });

    var active = params();
    var count = FILTERS.reduce(function (n, f) { return n + active.getAll(f[0]).length; }, 0);
    $('#filterCount').textContent = count > 0 ? String(count) : '';
  }

  /* ---------- active filter chips ---------- */

  // A filter chosen in a select (status, condition) is shown with the text of
  // its option, so the chip reads like the panel and not like the URL value.
  function displayValue(key, value) {
    var options = $$('select[data-filter="' + key + '"] option');
    for (var i = 0; i < options.length; i++) {
      if (options[i].value === value) return options[i].textContent.trim();
    }
    return value;
  }

  function makeChip(label, key, value) {
    var shown = displayValue(key, value);
    var chip = document.createElement('span');
    chip.className = 'chip';

    var keyEl = document.createElement('span');
    keyEl.className = 'chip-key';
    keyEl.textContent = label + ':';

    var valueEl = document.createElement('span');
    valueEl.textContent = shown;

    var remove = document.createElement('button');
    remove.type = 'button';
    remove.setAttribute('aria-label', t('removeFilter', label, shown));
    remove.textContent = '×';
    remove.addEventListener('click', function () {
      var p = params();
      removeValue(p, key, value);
      navigate(p);
    });

    chip.appendChild(keyEl);
    chip.appendChild(valueEl);
    chip.appendChild(remove);
    return chip;
  }

  function initChips() {
    var bar = $('#activeFilters');
    var list = $('#chipList');
    if (!bar || !list) return;

    var p = params();
    var total = 0;
    FILTERS.forEach(function (f) {
      p.getAll(f[0]).forEach(function (value) {
        total++;
        list.appendChild(makeChip(t(f[1]), f[0], value));
      });
    });
    bar.hidden = total === 0;
    $('#clearChips').addEventListener('click', clearAllFilters);
    $$('[data-clear-filters]').forEach(function (button) {
      button.addEventListener('click', clearAllFilters);
    });

    var emptyFiltered = $('#emptyFiltered');
    var emptyNone = $('#emptyNone');
    if (emptyFiltered && emptyNone) {
      emptyFiltered.hidden = total === 0;
      emptyNone.hidden = total > 0;
    }
  }

  /* ---------- row selection and bulk bar ---------- */

  function selectedIds() {
    return $$('.row-select:checked').map(function (cb) { return cb.dataset.id; });
  }

  function updateBulk() {
    var bar = $('#bulkBar');
    if (!bar) return;
    var ids = selectedIds();
    var boxes = $$('.row-select');

    bar.classList.toggle('is-open', ids.length > 0);
    $('#bulkCount').textContent = String(ids.length);
    boxes.forEach(function (cb) { cb.closest('tr').classList.toggle('is-selected', cb.checked); });

    var all = $('#selectAll');
    if (all) {
      all.checked = boxes.length > 0 && ids.length === boxes.length;
      all.indeterminate = ids.length > 0 && ids.length < boxes.length;
    }

    ['#bulkAssignForm', '#bulkUnassignForm', '#bulkDeleteForm'].forEach(function (selector) {
      var form = $(selector);
      if (!form) return;
      form.querySelector('[name=ids]').value = ids.join(',');
      form.querySelector('[name=redirectUrl]').value = currentPath();
    });
  }

  function initSelection() {
    document.addEventListener('change', function (e) {
      if (e.target.matches('.row-select')) updateBulk();
      if (e.target.id === 'selectAll') {
        $$('.row-select').forEach(function (cb) { cb.checked = e.target.checked; });
        updateBulk();
      }
    });
    var clear = $('#clearSelection');
    if (clear) {
      clear.addEventListener('click', function () {
        $$('.row-select').forEach(function (cb) { cb.checked = false; });
        updateBulk();
      });
    }
    document.body.addEventListener('htmx:afterSwap', updateBulk);
    updateBulk();
  }

  /* ---------- dropdown menus (assign) ---------- */

  function closeMenus() {
    $$('.menu.is-open').forEach(function (m) { m.classList.remove('is-open'); });
    $$('[data-menu][aria-expanded="true"]').forEach(function (b) { b.setAttribute('aria-expanded', 'false'); });
  }

  function initMenus() {
    document.addEventListener('click', function (e) {
      var trigger = e.target.closest('[data-menu]');
      if (trigger) {
        var menu = document.getElementById(trigger.dataset.menu);
        var wasOpen = menu && menu.classList.contains('is-open');
        closeMenus();
        if (menu && !wasOpen) {
          // a menu shared by many rows is moved next to the button that opens
          // it, so it is positioned there and its items find that row's form
          if (menu.dataset.shared !== undefined) trigger.closest('.menu-wrap').appendChild(menu);
          menu.classList.add('is-open');
          trigger.setAttribute('aria-expanded', 'true');
          var first = menu.querySelector('button');
          if (first) first.focus();
        }
        return;
      }
      var pick = e.target.closest('[data-pick-name]');
      if (pick) {
        var pickForm = pick.closest('form');
        var field = pickForm.querySelector('[name="' + pick.dataset.pickName + '"]');
        if (field) field.value = pick.dataset.pickValue;
        closeMenus();
        pickForm.requestSubmit();
        return;
      }
      var item = e.target.closest('[data-assign]');
      if (item) {
        var form = item.closest('form');
        form.querySelector('[name=assignedTo]').value = item.dataset.assign;
        if (form.id === 'bulkAssignForm') updateBulk();
        closeMenus();
        form.requestSubmit();
        return;
      }
      if (!e.target.closest('.menu')) closeMenus();
    });
    document.addEventListener('keydown', function (e) { if (e.key === 'Escape') closeMenus(); });
  }

  /* ---------- dialogs ---------- */

  function initDialogs() {
    document.addEventListener('click', function (e) {
      var opener = e.target.closest('[data-open]');
      if (opener) {
        var target = document.getElementById(opener.dataset.open);
        if (target && typeof target.showModal === 'function') target.showModal();
        return;
      }
      var closer = e.target.closest('[data-close]');
      if (closer) {
        var dlg = closer.closest('dialog');
        if (dlg) dlg.close();
      }
    });
    $$('dialog.modal').forEach(function (dlg) {
      dlg.addEventListener('click', function (e) { if (e.target === dlg) dlg.close(); });
    });
  }

  function initDetails() {
    var dlg = $('#detailsDialog');
    if (!dlg) return;
    document.addEventListener('click', function (e) {
      var btn = e.target.closest('[data-details]');
      if (!btn) return;
      $$('[data-field]', dlg).forEach(function (el) {
        var raw = (btn.dataset[el.dataset.field] || '').trim();
        el.textContent = raw || '—';
        el.classList.toggle('is-empty', raw === '');
      });
      var status = $('#detailStatus');
      if (status) {
        var available = (btn.dataset.status || '').trim() === 'available';
        status.textContent = t(available ? 'available' : 'notAvailable');
        status.className = 'badge ' + (available ? 'badge-available' : 'badge-unavailable');
      }
      $('#detailsEdit').setAttribute('href', '/devices/' + btn.dataset.id + '/edit');
      dlg.showModal();
    });
  }

  function initConfirm() {
    var dlg = $('#confirmDialog');
    if (!dlg) return;
    var pending = null;

    document.addEventListener('submit', function (e) {
      var form = e.target;
      if (!form.matches('form[data-confirm]')) return;
      if (form.dataset.confirmed === 'true') {
        form.dataset.confirmed = '';
        return;
      }
      e.preventDefault();

      var redirect = form.querySelector('[name=redirectUrl]');
      if (redirect && form.dataset.confirm === 'single') redirect.value = currentPath();

      var text;
      if (form.dataset.confirm === 'bulk') {
        var ids = selectedIds();
        if (ids.length === 0) return;
        var names = ids.map(function (id) {
          var row = document.getElementById('device-row-' + id);
          var name = row ? row.querySelector('.device-name') : null;
          return name ? name.textContent.trim() : id;
        });
        text = t(ids.length === 1 ? 'confirmBulkOne' : 'confirmBulkMany', ids.length, names.join(', '));
      } else {
        text = t('confirmSingle', form.dataset.deviceName || t('thisDevice'));
      }
      $('#confirmText').textContent = text + '\n' + t('cannotUndo');
      pending = form;
      dlg.showModal();
    });

    $('#confirmOk').addEventListener('click', function () {
      if (!pending) return;
      var form = pending;
      pending = null;
      dlg.close();
      form.dataset.confirmed = 'true';
      form.requestSubmit();
    });
    dlg.addEventListener('close', function () { pending = null; });
  }

  /* ---------- group chips on the device form ---------- */

  function initGroupChips() {
    var input = $('#group');
    var chips = $$('.chip-button[data-group]');
    if (!input || chips.length === 0) return;

    function sync() {
      var current = input.value.trim().toLowerCase();
      chips.forEach(function (chip) {
        chip.classList.toggle('is-current', chip.dataset.group.toLowerCase() === current);
      });
    }
    chips.forEach(function (chip) {
      chip.addEventListener('click', function () {
        input.value = chip.dataset.group;
        input.focus();
        sync();
      });
    });
    input.addEventListener('input', sync);
    sync();
  }

  /* ---------- flash messages ---------- */

  function initFlash() {
    var stack = $('#flashStack');
    if (!stack || stack.children.length === 0) return;
    window.setTimeout(function () {
      $$('cds-inline-notification', stack).forEach(function (n) { n.remove(); });
    }, 7000);
  }

  /* ---------- failed htmx requests ---------- */

  // An htmx response replaces a table row, so a request that fails (session
  // expired, server error, no connection) would otherwise change nothing at
  // all on screen. Say so in the same place the flash messages appear.
  function initRequestErrors() {
    var stack = $('#flashStack');
    if (!stack) return;
    function report() {
      var notice = document.createElement('cds-inline-notification');
      notice.setAttribute('kind', 'error');
      notice.setAttribute('title', t('notSavedTitle'));
      notice.setAttribute('subtitle', t('notSavedText'));
      notice.setAttribute('low-contrast', '');
      stack.appendChild(notice);
      window.setTimeout(function () { notice.remove(); }, 7000);
    }
    ['htmx:responseError', 'htmx:sendError', 'htmx:timeout'].forEach(function (name) {
      document.body.addEventListener(name, report);
    });
  }

  /* ---------- copy buttons (API keys page) ---------- */

  function initCopy() {
    document.addEventListener('click', function (e) {
      var button = e.target.closest('[data-copy]');
      if (!button) return;
      var source = document.getElementById(button.dataset.copy);
      if (!source || !navigator.clipboard) return;
      navigator.clipboard.writeText(source.textContent.trim()).then(function () {
        var label = button.textContent;
        button.textContent = t('copied');
        window.setTimeout(function () { button.textContent = label; }, 1500);
      });
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    initQuickSearch();
    initFilters();
    initChips();
    initSelection();
    initMenus();
    initDialogs();
    initDetails();
    initConfirm();
    initGroupChips();
    initFlash();
    initRequestErrors();
    initCopy();
  });
})();
