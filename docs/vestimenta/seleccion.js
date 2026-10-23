(() => {
  'use strict';
  const catalog = JSON.parse(document.getElementById('catalog').textContent);
  const $ = id => document.getElementById(id);
  const KEY = 'apogee-vestimenta-selection-v1';
  const uses = new Set(['Conjunto base', 'Casco', 'Torso y brazos', 'Piernas', 'Botas', 'Detalles']);
  const imageExt = /\.(png|jpe?g|webp|gif)$/i;
  const esc = s => String(s).replace(/[&<>"']/g, c => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[c]));
  const slug = s => s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '') || 'sin-nombre';
  const titulo = s => s.replace(/[-_]+/g, ' ').replace(/\b\p{L}/gu, c => c.toUpperCase());
  const clean = s => s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
  const encodePath = p => p.split('/').map(encodeURIComponent).join('/');

  // La galería se reconstruye entera cada vez que cambia la fuente de imágenes (catálogo base o carpeta elegida).
  let names = {}, byId = new Map(), cards = [], objectUrls = [];
  let onlySelected = false;
  let state = {version: 1, updatedAt: null, confirmedAt: null, selections: {}};
  const message = text => { $('message').textContent = text; };
  const folderStatus = (text, warn) => { $('folder-status').textContent = text; $('folder-status').classList.toggle('warn', !!warn); };
  const factionLabel = f => names[f] || f;

  function validate(raw) {
    if (!raw || raw.version !== 1 || !raw.selections || typeof raw.selections !== 'object' || Array.isArray(raw.selections)) {
      throw new Error('El archivo no es una selección de referencias de Apogee.');
    }
    const selections = {};
    for (const [id, item] of Object.entries(raw.selections)) {
      if (!item || !uses.has(item.use) || typeof item.note !== 'string' || item.note.length > 2000) {
        throw new Error('Hay una referencia con un uso o una nota no válidos. No se importó ningún cambio.');
      }
      selections[id] = {use: item.use, note: item.note};
    }
    const date = value => typeof value === 'string' && Number.isFinite(Date.parse(value)) ? value : null;
    return {state: {version: 1, updatedAt: date(raw.updatedAt), confirmedAt: date(raw.confirmedAt), selections}};
  }
  function save() {
    try {
      localStorage.setItem(KEY, JSON.stringify(state));
      $('storage-status').textContent = 'Guardado en este navegador y esta dirección. Exporta una copia para conservarla o compartirla.';
      $('storage-status').classList.remove('storage-warning');
    } catch {
      $('storage-status').textContent = 'El navegador no permite guardar aquí. Tu selección sigue en esta pestaña: expórtala antes de cerrar.';
      $('storage-status').classList.add('storage-warning');
    }
  }
  try {
    const saved = localStorage.getItem(KEY);
    if (saved) state = validate(JSON.parse(saved)).state;
  } catch {
    $('storage-status').textContent = 'No se pudo recuperar el guardado anterior. Puedes importar una copia JSON; no se ha sobrescrito el guardado.';
    $('storage-status').classList.add('storage-warning');
  }

  // Elecciones vigentes: solo las de imágenes cargadas ahora; las demás se conservan en el guardado.
  const chosenIds = () => Object.keys(state.selections).filter(id => byId.has(id));

  function filter() {
    let n = 0;
    for (const card of cards) {
      const show = ($('faction').value === 'all' || $('faction').value === card.dataset.faction)
        && ($('era').value === 'all' || $('era').value === card.dataset.era)
        && clean(card.dataset.text).includes(clean($('search').value))
        && (!onlySelected || !!state.selections[card.dataset.id]);
      card.classList.toggle('hidden', !show);
      n += Number(show);
    }
    $('count').textContent = `${n} de ${cards.length} referencias${onlySelected ? ' · solo elegidas' : ''}`;
    $('empty').hidden = n !== 0;
  }
  function summary() {
    const ids = chosenIds();
    const count = ids.length;
    $('selection-title').textContent = `Tus referencias elegidas · ${count}`;
    $('selection-status').textContent = state.confirmedAt
      ? `Selección confirmada el ${new Date(state.confirmedAt).toLocaleString('es-CL')}. Lista para exportar y modelar.`
      : count ? 'En revisión. Cuando estés conforme, confirma la selección.' : 'Todavía no has elegido referencias. Marca las imágenes que quieras usar.';
    $('confirm').disabled = count === 0 || !!state.confirmedAt;
    $('confirm').textContent = state.confirmedAt ? '✓ Selección confirmada' : 'Confirmar selección';
    $('export').disabled = count === 0;
    $('markdown').disabled = count === 0;
    $('selection-summary').replaceChildren();
    for (const [faction, name] of Object.entries(names)) {
      const n = ids.filter(id => byId.get(id).faction === faction).length;
      const badge = document.createElement('span');
      badge.className = `badge${n ? ' has' : ''}`;
      badge.textContent = `${name}: ${n}`;
      $('selection-summary').append(badge);
    }
  }
  function renderCards() {
    for (const card of cards) {
      const id = card.dataset.id, selected = state.selections[id], pick = card.querySelector('.pick');
      card.classList.toggle('selected', !!selected);
      pick.setAttribute('aria-pressed', String(!!selected));
      pick.textContent = selected ? '✓ Elegida · quitar' : '＋ Elegir referencia';
      pick.setAttribute('aria-label', `${selected ? 'Quitar' : 'Elegir'} referencia ${id}`);
      card.querySelector('.details').hidden = !selected;
      card.querySelector('.use').value = selected?.use || 'Conjunto base';
      card.querySelector('.note').value = selected?.note || '';
    }
    summary(); filter();
  }
  function changed() {
    state.updatedAt = new Date().toISOString(); state.confirmedAt = null;
    message(''); save(); summary(); filter();
  }

  function cardHtml(r) {
    const text = `${factionLabel(r.faction)} ${r.piece} ${r.note || ''} ${r.angle || ''} ${r.id}`;
    const dims = [r.width && r.height ? `${r.width} × ${r.height}` : '', r.angle || ''].filter(Boolean).join(' · ');
    const links = [r.source ? `<a href="${esc(r.source)}" target="_blank" rel="noreferrer">Fuente ↗</a>` : '',
      r.url ? `<a href="${esc(r.url)}" target="_blank" rel="noreferrer">Imagen remota ↗</a>` : ''].join('');
    return `<article class="card" id="${esc(r.id)}" data-id="${esc(r.id)}" data-faction="${esc(r.faction)}" data-era="${esc(r.era)}" data-text="${esc(text)}">` +
      `<a class="picture" href="${esc(r.src)}" target="_blank"><img src="${esc(r.src)}" alt="${esc(r.piece)}" loading="lazy"></a>` +
      `<div class="info"><small>${esc(factionLabel(r.faction))} · ${esc(r.era)} · ${esc(r.id)}</small><h2>${esc(r.piece)}</h2>${r.note ? `<p>${esc(r.note)}</p>` : ''}` +
      `<p class="meta">${esc(r.author || '')}<br>${esc(dims)}</p>${links ? `<div class="links">${links}</div>` : ''}` +
      `<div class="choose"><button type="button" class="pick" aria-pressed="false">＋ Elegir referencia</button><div class="details" hidden>` +
      `<label>Usar como<select class="use" aria-label="Uso de ${esc(r.id)}">${[...uses].map(u => `<option>${u}</option>`).join('')}</select></label>` +
      `<label>Notas para modelar<textarea class="note" aria-label="Notas de ${esc(r.id)}" maxlength="2000" placeholder="Qué conservar, combinar o cambiar…"></textarea></label></div></div></div></article>`;
  }

  // Reconstruye filtros, tarjetas y selección a partir de una lista de imágenes.
  function build(list, groupNames) {
    names = Object.fromEntries(Object.entries(groupNames).filter(([f]) => list.some(r => r.faction === f)));
    byId = new Map(list.map(r => [r.id, r]));
    const faction = $('faction').value, era = $('era').value;
    const eras = [...new Set(list.map(r => r.era))].sort();
    $('faction').replaceChildren(new Option('Todos los grupos', 'all'), ...Object.entries(names).map(([f, n]) => new Option(n, f)));
    $('era').replaceChildren(new Option('Todas las épocas', 'all'), ...eras.map(e => new Option(e, e)));
    $('faction').value = names[faction] ? faction : 'all';
    $('era').value = eras.includes(era) ? era : 'all';
    $('resumen').textContent = `${list.length} imágenes de ${Object.keys(names).length} grupos.`;
    $('grid').innerHTML = list.map(cardHtml).join('');
    cards = [...document.querySelectorAll('.card')];
    for (const card of cards) {
      const id = card.dataset.id;
      card.querySelector('.pick').addEventListener('click', () => {
        if (state.selections[id]) delete state.selections[id];
        else state.selections[id] = {use: 'Conjunto base', note: ''};
        changed(); renderCards();
      });
      card.querySelector('.use').addEventListener('change', event => {
        if (state.selections[id]) {state.selections[id].use = event.target.value; changed();}
      });
      card.querySelector('.note').addEventListener('input', event => {
        if (state.selections[id]) {state.selections[id].note = event.target.value; changed();}
      });
    }
    renderCards();
  }

  // Respaldo: el catálogo ya catalogado, usado mientras no se elija carpeta.
  function loadCatalog() {
    build(catalog.images.map(r => ({...r, src: encodePath(r.local)})), catalog.names);
  }

  // Las imágenes ya catalogadas se reconocen por su ruta local; el resto recibe datos mínimos.
  const known = new Map(catalog.images.map(r => [r.local, r]));
  function fromFolder(groups) {
    const groupNames = {...catalog.names}, list = [], used = new Set();
    for (const group of groups) {
      const faction = slug(group.carpeta || 'general');
      if (!groupNames[faction]) groupNames[faction] = group.carpeta || 'Sin grupo';
      for (const file of group.imagenes) {
        const src = URL.createObjectURL(file.file);
        objectUrls.push(src);
        const meta = known.get('imagenes/' + file.ruta);
        const base = file.nombre.replace(/\.[^.]+$/, '');
        let id = meta ? meta.id : `${faction}-${slug(base)}`;
        for (let n = 2; used.has(id); n++) id = `${id}-${n}`;
        used.add(id);
        list.push(meta ? {...meta, faction, src} : {
          id, faction, src, piece: titulo(base), angle: '', source: '', url: '',
          local: 'imagenes/' + file.ruta, note: '', era: 'Sin época', author: 'Sin crédito registrado (imagen añadida a mano)'
        });
      }
    }
    return {list, groupNames};
  }

  // Cada subcarpeta directa de la carpeta elegida es un grupo; las imágenes sueltas van a "Sin grupo".
  const byName = (a, b) => a.name.localeCompare(b.name, undefined, {numeric: true});
  async function entriesOf(dir) {
    const entries = [];
    for await (const entry of dir.values()) entries.push(entry);
    return entries.sort(byName);
  }
  async function collect(dir, prefix, out) {
    for (const entry of await entriesOf(dir)) {
      if (entry.kind === 'directory') await collect(entry, `${prefix}${entry.name}/`, out);
      else if (imageExt.test(entry.name)) out.push({ruta: prefix + entry.name, nombre: entry.name, file: await entry.getFile()});
    }
  }
  async function scan(handle) {
    const groups = [], loose = [];
    for (const entry of await entriesOf(handle)) {
      if (entry.kind === 'directory') {
        const files = [];
        await collect(entry, `${entry.name}/`, files);
        if (files.length) groups.push({carpeta: entry.name, imagenes: files});
      } else if (imageExt.test(entry.name)) loose.push({ruta: entry.name, nombre: entry.name, file: await entry.getFile()});
    }
    if (loose.length) groups.push({carpeta: '', imagenes: loose});
    return groups;
  }

  // El handle de la carpeta se recuerda en IndexedDB para no tener que elegirla cada vez.
  const db = {
    async run(mode, fn) {
      const conn = await new Promise((resolve, reject) => {
        const req = indexedDB.open('apogee-vestimenta', 1);
        req.onupgradeneeded = () => req.result.createObjectStore('handles');
        req.onsuccess = () => resolve(req.result);
        req.onerror = () => reject(req.error);
      });
      return new Promise((resolve, reject) => {
        const req = fn(conn.transaction('handles', mode).objectStore('handles'));
        req.onsuccess = () => { conn.close(); resolve(req.result); };
        req.onerror = () => { conn.close(); reject(req.error); };
      });
    },
    get: () => db.run('readonly', s => s.get('carpeta-imagenes')),
    put: handle => db.run('readwrite', s => s.put(handle, 'carpeta-imagenes'))
  };

  let folder = null;
  async function loadFolder(handle) {
    folderStatus(`Leyendo «${handle.name}»…`);
    const groups = await scan(handle);
    if (!groups.length) {
      folderStatus(`«${handle.name}» no tiene imágenes (png, jpg, jpeg, webp o gif) en sus subcarpetas. Se mantiene lo que había.`, true);
      return false;
    }
    for (const url of objectUrls) URL.revokeObjectURL(url);
    objectUrls = [];
    const {list, groupNames} = fromFolder(groups);
    folder = handle;
    build(list, groupNames);
    $('rescan').hidden = false;
    $('pick-folder').textContent = 'Cambiar carpeta';
    folderStatus(`Carpeta «${handle.name}»: ${groups.length} grupos y ${list.length} imágenes. Si añades carpetas o imágenes, pulsa «Volver a leer carpeta».`);
    return true;
  }
  async function chooseFolder() {
    try {
      const handle = await window.showDirectoryPicker({id: 'apogee-imagenes', mode: 'read'});
      if (!await loadFolder(handle)) return;
      try { await db.put(handle); } catch { folderStatus('No se pudo recordar la carpeta; tendrás que elegirla de nuevo la próxima vez.', true); }
    } catch (error) {
      if (error.name !== 'AbortError') folderStatus(`No se pudo leer la carpeta: ${error.message}`, true);
    }
  }

  for (const id of ['faction', 'era', 'search']) $(id).addEventListener('input', filter);
  $('show-selected').addEventListener('click', () => {
    onlySelected = !onlySelected;
    $('show-selected').setAttribute('aria-pressed', String(onlySelected));
    $('show-selected').textContent = onlySelected ? 'Ver todas' : 'Ver elegidas';
    if (onlySelected) { $('faction').value = 'all'; $('era').value = 'all'; $('search').value = ''; }
    filter();
  });
  $('confirm').addEventListener('click', () => {
    if (!chosenIds().length) return;
    state.confirmedAt = new Date().toISOString(); state.updatedAt = state.confirmedAt;
    save(); summary(); message('Confirmada. Puedes exportar la selección o descargar la ficha de modelado.');
  });
  function download(filename, text, mime) {
    const url = URL.createObjectURL(new Blob([text], {type: mime}));
    const link = document.createElement('a'); link.href = url; link.download = filename;
    document.body.append(link); link.click(); link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 30000);
  }
  $('export').addEventListener('click', () => {
    const references = chosenIds().map(id => { const {src, ...r} = byId.get(id); return {...r, ...state.selections[id]}; });
    download('apogee-seleccion-referencias.json', JSON.stringify({...state, project: 'Apogee', exportedAt: new Date().toISOString(), references}, null, 2), 'application/json');
    message('Exportación solicitada. Guarda el JSON como copia recuperable de tus elecciones.');
  });
  $('markdown').addEventListener('click', () => {
    const lines = ['# Apogee — referencias elegidas', '', `Estado: ${state.confirmedAt ? 'confirmado' : 'en revisión'}.`, `Actualización: ${state.updatedAt || 'sin fecha'}.`, '', 'Rutas de imagen relativas a docs/vestimenta. Las notas son decisiones del usuario.', ''];
    for (const [faction, name] of Object.entries(names)) {
      const ids = chosenIds().filter(id => byId.get(id).faction === faction);
      if (!ids.length) continue;
      lines.push(`## ${name}`, '');
      for (const id of ids) {
        const r = byId.get(id), choice = state.selections[id];
        lines.push(`### ${id} — ${choice.use}`, '', r.piece, '', `- Imagen local: \`${r.local}\``, `- Fuente: ${r.source || 'sin registrar'}`, `- Imagen remota: ${r.url || 'sin registrar'}`, `- Crédito: ${r.author}`, `- Época: ${r.era}`, '', 'Notas:', '', ...(choice.note || 'Sin notas adicionales.').split('\n').map(line => '> ' + line), '');
      }
    }
    download('apogee-referencias-elegidas.md', lines.join('\n'), 'text/markdown;charset=utf-8');
    message('Ficha solicitada. Incluye fuentes, rutas, usos y notas de todas las referencias elegidas.');
  });
  $('import').addEventListener('click', () => $('import-file').click());
  $('import-file').addEventListener('change', async event => {
    const file = event.target.files[0]; if (!file) return;
    try {
      if (file.size > 1024 * 1024) throw new Error('El archivo supera 1 MB. Selecciona una exportación JSON de esta galería.');
      const result = validate(JSON.parse(await file.text()));
      const count = Object.keys(result.state.selections).length;
      if (!count) throw new Error('No hay referencias compatibles para importar. Tu selección actual se conserva.');
      const absent = Object.keys(result.state.selections).filter(id => !byId.has(id)).length;
      state.selections = {...state.selections, ...result.state.selections};
      changed(); renderCards();
      message(`Se importaron ${count} referencias. Revisa y confirma la selección.${absent ? ` ${absent} no corresponden a las imágenes cargadas ahora y quedan guardadas hasta que aparezcan.` : ''}`);
    } catch (error) {
      message(error instanceof SyntaxError ? 'El archivo no contiene JSON válido. Tu selección actual se conserva.' : error.message);
    } finally { event.target.value = ''; }
  });
  window.addEventListener('storage', event => {
    if (event.key !== KEY || !event.newValue) return;
    try { state = validate(JSON.parse(event.newValue)).state; renderCards(); message('Selección actualizada desde otra pestaña.'); } catch { /* Ignore invalid external data. */ }
  });

  loadCatalog();
  if (!window.showDirectoryPicker) {
    $('pick-folder').disabled = true;
    folderStatus('Este navegador no permite leer carpetas. Abre la galería en Chrome o Edge para detectar las carpetas automáticamente. Mientras tanto se muestra el catálogo base.', true);
    return;
  }
  folderStatus('Elige la carpeta «imagenes» (la que contiene las subcarpetas de cada grupo). Mientras tanto se muestra el catálogo base.');
  let remembered = null;
  $('pick-folder').addEventListener('click', async () => {
    if (!remembered) return chooseFolder();
    const handle = remembered;
    try {
      if (await handle.requestPermission({mode: 'read'}) !== 'granted') { folderStatus('Sin permiso para leer la carpeta. Pulsa de nuevo o elige otra.', true); return; }
      remembered = null;
      await loadFolder(handle);
    } catch (error) { folderStatus(`No se pudo reconectar: ${error.message}`, true); }
  });
  $('rescan').addEventListener('click', async () => {
    try { if (folder) await loadFolder(folder); } catch (error) { folderStatus(`No se pudo leer la carpeta: ${error.message}`, true); }
  });
  db.get().then(async handle => {
    if (!handle) return;
    if (await handle.queryPermission({mode: 'read'}) === 'granted') { await loadFolder(handle); return; }
    remembered = handle;
    $('pick-folder').textContent = 'Dar permiso a la carpeta de imágenes';
    folderStatus(`Carpeta recordada: «${handle.name}». Pulsa «Dar permiso a la carpeta de imágenes» y permite el acceso para cargar sus imágenes.`);
  }).catch(() => { /* Sin IndexedDB: se pedirá la carpeta cada vez. */ });
})();
