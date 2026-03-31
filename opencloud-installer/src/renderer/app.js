/**
 * OpenCloud 安装工具 - 前端主逻辑
 * 负责页面导航、服务器管理、组件选择、安装控制
 */

'use strict';

// ════════════════════════════════════════════════════
// 组件定义（与 ssh-service.js 保持同步）
// ════════════════════════════════════════════════════
const COMPONENTS = [
  { key: 'ceph',       name: 'Ceph 分布式存储',   icon: '🗄️',  desc: '基于 cephadm 部署 Ceph 集群，提供块存储、对象存储和文件存储', requires: [],             order: 1 },
  { key: 'openstack',  name: 'OpenStack 云平台',  icon: '☁️',  desc: '基于 Kolla-Ansible 容器化部署 OpenStack IaaS 平台',            requires: ['ceph'],        order: 2 },
  { key: 'kubernetes', name: 'Kubernetes 容器编排',icon: '⎈',   desc: '基于 kubeadm 部署高可用 Kubernetes 集群（3 Master）',          requires: ['ceph'],        order: 3 },
  { key: 'prometheus', name: 'Prometheus 监控',   icon: '📊',  desc: '部署 kube-prometheus-stack，提供完整监控告警能力',              requires: ['kubernetes'],  order: 4 },
  { key: 'grafana',    name: 'Grafana 可视化',    icon: '📈',  desc: '部署 Grafana，提供监控数据可视化和告警通知',                    requires: ['prometheus'],  order: 5 },
];

// ════════════════════════════════════════════════════
// 应用状态
// ════════════════════════════════════════════════════
const state = {
  servers: [],
  components: {},     // { key: { enabled, order } }
  editingServer: null, // 当前编辑的服务器对象（null = 新建）
  installing: false,
};

// ════════════════════════════════════════════════════
// DOM 快捷引用
// ════════════════════════════════════════════════════
const $ = (id) => document.getElementById(id);
const $$ = (sel) => document.querySelectorAll(sel);

// ════════════════════════════════════════════════════
// 页面导航
// ════════════════════════════════════════════════════
function initNav() {
  $$('.nav-item[data-page]').forEach(item => {
    item.addEventListener('click', () => {
      const page = item.dataset.page;
      $$('.nav-item').forEach(n => n.classList.remove('active'));
      $$('.page').forEach(p => p.classList.remove('active'));
      item.classList.add('active');
      $(`page-${page}`)?.classList.add('active');
      if (page === 'install') refreshInstallPage();
    });
  });

  // 日志按钮
  $('navLogs').addEventListener('click', () => window.api.logs.open());
}

// ════════════════════════════════════════════════════
// 标题栏控制
// ════════════════════════════════════════════════════
function initTitlebar() {
  $('btnMin').addEventListener('click',   () => window.api.window.minimize());
  $('btnMax').addEventListener('click',   () => window.api.window.maximize());
  $('btnClose').addEventListener('click', () => window.api.window.close());
  window.api.app.version().then(v => { $('appVersion').textContent = `v${v}`; }).catch(() => {});
}

// ════════════════════════════════════════════════════
// 服务器管理
// ════════════════════════════════════════════════════
async function loadServers() {
  state.servers = await window.api.config.getServers();
  renderServerGrid();
  refreshTargetServerSelect();
}

function renderServerGrid() {
  const grid = $('serverGrid');
  const empty = $('serverEmpty');

  // 清除旧卡片（保留 empty-state）
  grid.querySelectorAll('.server-card').forEach(c => c.remove());

  if (state.servers.length === 0) {
    empty.style.display = 'block';
    return;
  }
  empty.style.display = 'none';

  state.servers.forEach(srv => {
    const card = document.createElement('div');
    card.className = 'server-card fade-in';
    card.dataset.id = srv.id;
    card.innerHTML = `
      <div class="sc-head">
        <div class="sc-icon">🖥️</div>
        <div class="sc-actions">
          <button class="sc-action-btn edit" title="编辑" data-id="${srv.id}">✏️</button>
          <button class="sc-action-btn del"  title="删除" data-id="${srv.id}">🗑</button>
        </div>
      </div>
      <div class="sc-name">${escHtml(srv.name)}</div>
      <div class="sc-host">
        <span>主机:</span>
        <code>${escHtml(srv.host)}:${srv.port || 22}</code>
      </div>
      <div class="sc-host" style="margin-top:4px">
        <span>用户:</span>
        <code>${escHtml(srv.username)}</code>
      </div>
      ${srv.note ? `<div class="sc-note">${escHtml(srv.note)}</div>` : ''}
      <div>
        <span class="sc-tag ${srv.keyFile ? 'key' : 'pass'}">
          ${srv.keyFile ? '🔑 密钥认证' : '🔒 密码认证'}
        </span>
      </div>
    `;
    card.querySelector('.sc-action-btn.edit').addEventListener('click', (e) => {
      e.stopPropagation();
      openServerModal(srv.id);
    });
    card.querySelector('.sc-action-btn.del').addEventListener('click', (e) => {
      e.stopPropagation();
      deleteServer(srv.id, srv.name);
    });
    card.addEventListener('click', () => {
      $$('.server-card').forEach(c => c.classList.remove('selected'));
      card.classList.add('selected');
    });
    grid.appendChild(card);
  });
}

function openServerModal(serverId = null) {
  state.editingServer = serverId ? state.servers.find(s => s.id === serverId) : null;
  const srv = state.editingServer || {};

  $('modalTitle').textContent = serverId ? '编辑服务器' : '添加服务器';
  $('fName').value  = srv.name     || '';
  $('fHost').value  = srv.host     || '';
  $('fPort').value  = srv.port     || 22;
  $('fUser').value  = srv.username || '';
  $('fPass').value  = srv.password || '';
  $('fKey').value   = srv.keyFile  || '';
  $('fNote').value  = srv.note     || '';
  $('modalTestResult').textContent = '';
  $('modalTestResult').className = 'test-result inline';

  $('modalMask').classList.add('open');
  setTimeout(() => $('fName').focus(), 100);
}

function closeServerModal() {
  $('modalMask').classList.remove('open');
}

async function saveServer() {
  const name = $('fName').value.trim();
  const host = $('fHost').value.trim();
  const user = $('fUser').value.trim();

  if (!name) return showFieldError($('fName'), '请填写服务器名称');
  if (!host) return showFieldError($('fHost'), '请填写主机地址');
  if (!user) return showFieldError($('fUser'), '请填写用户名');

  const server = {
    id:       state.editingServer?.id || null,
    name,
    host,
    port:     parseInt($('fPort').value) || 22,
    username: user,
    password: $('fPass').value,
    keyFile:  $('fKey').value.trim(),
    note:     $('fNote').value.trim(),
  };

  const saved = await window.api.config.saveServer(server);
  await loadServers();
  closeServerModal();
}

async function deleteServer(id, name) {
  if (!confirm(`确定要删除服务器「${name}」吗？`)) return;
  await window.api.config.deleteServer(id);
  await loadServers();
}

function showFieldError(input, msg) {
  input.style.borderColor = 'var(--red)';
  input.focus();
  setTimeout(() => { input.style.borderColor = ''; }, 2000);
  alert(msg);
}

// ════════════════════════════════════════════════════
// 组件选择
// ════════════════════════════════════════════════════
async function loadComponents() {
  const saved = await window.api.config.getComponents();
  // 合并默认值
  COMPONENTS.forEach(c => {
    state.components[c.key] = {
      enabled: saved[c.key]?.enabled ?? true,
      order:   c.order,
    };
  });
  renderCompList();
}

function renderCompList() {
  const list = $('compList');
  list.innerHTML = '';
  COMPONENTS.forEach(comp => {
    const enabled = state.components[comp.key]?.enabled ?? true;
    const item = document.createElement('div');
    item.className = `comp-item ${enabled ? 'enabled' : ''} fade-in`;
    item.dataset.key = comp.key;
    item.innerHTML = `
      <div class="comp-icon-wrap">${comp.icon}</div>
      <div class="comp-info">
        <div class="comp-name">${comp.name}</div>
        <div class="comp-desc">${comp.desc}</div>
        ${comp.requires.length
          ? `<div class="comp-dep">⚡ 依赖: ${comp.requires.join(', ')}</div>`
          : ''}
      </div>
      <div class="comp-toggle" title="${enabled ? '点击关闭' : '点击开启'}"></div>
    `;
    item.addEventListener('click', () => toggleComponent(comp.key));
    list.appendChild(item);
  });
}

function toggleComponent(key) {
  const comp = COMPONENTS.find(c => c.key === key);
  const current = state.components[key]?.enabled ?? true;
  const next = !current;

  state.components[key] = { ...state.components[key], enabled: next };

  if (!next) {
    // 关闭时，级联关闭依赖它的组件
    COMPONENTS.forEach(c => {
      if (c.requires.includes(key) && state.components[c.key]?.enabled) {
        state.components[c.key] = { ...state.components[c.key], enabled: false };
      }
    });
  } else {
    // 开启时，自动开启依赖
    comp.requires.forEach(dep => {
      if (!state.components[dep]?.enabled) {
        state.components[dep] = { ...state.components[dep], enabled: true };
      }
    });
  }

  window.api.config.setComponents(state.components);
  renderCompList();
}

function selectAllComponents(val) {
  COMPONENTS.forEach(c => {
    state.components[c.key] = { ...state.components[c.key], enabled: val };
  });
  window.api.config.setComponents(state.components);
  renderCompList();
}

// ════════════════════════════════════════════════════
// 安装页面
// ════════════════════════════════════════════════════
function refreshTargetServerSelect() {
  const sel = $('targetServer');
  const prev = sel.value;
  sel.innerHTML = '<option value="">── 请选择目标服务器 ──</option>';
  state.servers.forEach(srv => {
    const opt = document.createElement('option');
    opt.value = srv.id;
    opt.textContent = `${srv.name}  (${srv.host}:${srv.port || 22})`;
    sel.appendChild(opt);
  });
  if (prev) sel.value = prev;
}

function refreshInstallPage() {
  refreshTargetServerSelect();
  renderTaskList('selected');  // 默认按选择模式显示
}

function renderTaskList(mode = 'selected') {
  const list = $('taskList');
  list.innerHTML = '';
  // 一键安装模式显示全部组件；分组件模式只显示已勾选的
  const visible = mode === 'all'
    ? COMPONENTS
    : COMPONENTS.filter(c => state.components[c.key]?.enabled);
  if (visible.length === 0) {
    list.innerHTML = '<div style="color:var(--text-muted);font-size:13px;padding:8px 0">未选择任何组件，请先到【组件】页面选择</div>';
    return;
  }
  visible.forEach(comp => {
    const item = document.createElement('div');
    item.className = 'task-item';
    item.id = `task-${comp.key}`;
    item.innerHTML = `
      <span class="task-icon">${comp.icon}</span>
      <span class="task-name">${comp.name}</span>
      <div class="task-bar-wrap"><div class="task-bar" id="bar-${comp.key}"></div></div>
      <span class="task-status-text" id="status-${comp.key}">等待中</span>
    `;
    list.appendChild(item);
  });
}

// 连接测试
async function testConnection(serverId, resultEl) {
  if (!serverId) {
    setTestResult(resultEl, 'error', '请先选择服务器');
    return;
  }
  const srv = state.servers.find(s => s.id === serverId);
  if (!srv) return;
  setTestResult(resultEl, 'testing', '⏳ 正在连接...');
  const res = await window.api.ssh.test(srv);
  if (res.ok) {
    setTestResult(resultEl, 'success', `✅ ${res.msg}`);
  } else {
    setTestResult(resultEl, 'error', `❌ ${res.msg}`);
  }
}

function setTestResult(el, type, msg) {
  el.textContent = msg;
  el.className = `test-result ${type}`;
}

// ════════════════════════════════════════════════════
// 安装控制
// ════════════════════════════════════════════════════
async function startInstall(mode = 'selected') {
  if (state.installing) return;

  const serverId = $('targetServer').value;
  if (!serverId) { alert('请先选择目标服务器'); return; }

  const server = state.servers.find(s => s.id === serverId);
  if (!server) return;

  let selectedComponents;
  let confirmMsg;

  if (mode === 'all') {
    // 一键全部安装：所有组件
    selectedComponents = COMPONENTS.map(c => c.key);
    confirmMsg = `即将在 ${server.host} 上一键安装全部组件：\n\n${COMPONENTS.map(c => c.name).join('、')}\n\n使用 deploy_all.sh 按依赖顺序自动部署。确认开始？`;
  } else {
    // 按选择安装
    selectedComponents = COMPONENTS
      .filter(c => state.components[c.key]?.enabled)
      .sort((a, b) => a.order - b.order)
      .map(c => c.key);
    if (selectedComponents.length === 0) { alert('请先选择要安装的组件'); return; }
    const compNames = selectedComponents.map(k => COMPONENTS.find(c => c.key === k)?.name).join('、');
    confirmMsg = `即将在 ${server.host} 上安装：\n\n${compNames}\n\n确认开始安装？`;
  }

  if (!confirm(confirmMsg)) return;

  // 注册事件监听
  window.api.install.removeAllListeners();
  window.api.install.onLog(({ msg, level }) => appendLog(msg, level));
  window.api.install.onStatus(({ key, status }) => updateTaskStatus(key, status));
  window.api.install.onProgress(({ key, pct }) => updateTaskBar(key, pct));
  window.api.install.onFinished(({ success, summary }) => {
    setInstalling(false);
    if (success) {
      setTimeout(() => alert('🎉 所有组件安装成功！'), 100);
    } else {
      setTimeout(() => alert('⚠️ 部分组件安装失败，请查看日志'), 100);
    }
  });

  clearLog();
  renderTaskList(mode);  // 按当前安装模式渲染任务列表（all=全部, selected=已选）
  resetTaskUI(selectedComponents);
  setInstalling(true);

  await window.api.install.start({ server, components: selectedComponents, mode: mode === 'all' ? 'all' : undefined });
}

async function stopInstall() {
  if (!state.installing) return;
  await window.api.install.stop();
  setInstalling(false);
}

// ════════════════════════════════════════════════════
// 环境预检
// ════════════════════════════════════════════════════
async function runPrecheck() {
  const serverId = $('targetServer').value;
  if (!serverId) { alert('请先选择目标服务器'); return; }
  const server = state.servers.find(s => s.id === serverId);
  if (!server) return;

  const card   = $('precheckCard');
  const result = $('precheckResult');

  card.style.display = 'block';
  result.innerHTML = '<div class="precheck-loading">⏳ 正在检测，请稍候...</div>';

  const res = await window.api.ssh.precheck(server);

  if (!res.items || res.items.length === 0) {
    result.innerHTML = `<div class="precheck-item fail">❌ 预检失败：无法获取结果</div>`;
    return;
  }

  const iconMap = { pass: '✅', warn: '⚠️', fail: '❌' };
  result.innerHTML = res.items.map(item => `
    <div class="precheck-item ${item.status}">
      <span class="precheck-icon">${iconMap[item.status] || '❓'}</span>
      <span class="precheck-label">${escHtml(item.label)}</span>
      <span class="precheck-detail">${escHtml(item.detail)}</span>
    </div>
  `).join('');

  if (res.ok) {
    result.innerHTML += '<div class="precheck-summary pass">✅ 环境检查通过，可以开始安装</div>';
  } else {
    result.innerHTML += '<div class="precheck-summary fail">❌ 环境检查发现问题，请处理后再安装</div>';
  }
}

// ════════════════════════════════════════════════════
// 关于 / 帮助弹窗
// ════════════════════════════════════════════════════
function openAboutModal() {
  window.api.app.version().then(v => {
    $('aboutVersion').textContent = `v${v}`;
  }).catch(() => {});
  $('aboutMask').classList.add('open');
}

function closeAboutModal() {
  $('aboutMask').classList.remove('open');
}

function setInstalling(val) {
  state.installing = val;
  $('btnInstallAll').disabled  = val;
  $('btnStartInstall').disabled = val;
  $('btnPrecheck').disabled    = val;
  $('btnStopInstall').disabled = !val;
}

function resetTaskUI(selectedKeys) {
  COMPONENTS.forEach(c => {
    const item = $(`task-${c.key}`);
    const bar  = $(`bar-${c.key}`);
    const stat = $(`status-${c.key}`);
    if (!item) return;
    item.className = 'task-item';
    if (bar) { bar.style.width = '0'; }
    if (stat) { stat.textContent = selectedKeys.includes(c.key) ? '等待中' : '跳过'; stat.style.color = ''; }
  });
}

function updateTaskStatus(key, status) {
  const item = $(`task-${key}`);
  const stat = $(`status-${key}`);
  const bar  = $(`bar-${key}`);
  if (!item) return;

  item.className = `task-item ${status}`;
  const map = {
    running: '安装中...',
    success: '✅ 成功',
    failed:  '❌ 失败',
    skipped: '⏭ 跳过',
  };
  if (stat) stat.textContent = map[status] || status;
  if (bar && status === 'success') bar.style.width = '100%';
  if (bar && (status === 'failed' || status === 'skipped')) bar.style.width = '0';
}

function updateTaskBar(key, pct) {
  const bar = $(`bar-${key}`);
  if (bar) bar.style.width = `${pct}%`;
}

// ════════════════════════════════════════════════════
// 日志
// ════════════════════════════════════════════════════
function appendLog(msg, level = 'INFO') {
  const container = $('logContainer');
  const line = document.createElement('span');
  line.className = `log-line ${level} fade-in`;
  const ts = new Date().toTimeString().substring(0, 8);
  line.innerHTML = `<span class="log-ts">[${ts}]</span>${escHtml(msg)}`;
  container.appendChild(line);
  // 自动滚动到底部
  container.scrollTop = container.scrollHeight;

  // 限制日志行数，避免内存溢出
  const lines = container.children;
  while (lines.length > 2000) {
    container.removeChild(lines[0]);
  }
}

function clearLog() {
  $('logContainer').innerHTML = '';
}

// ════════════════════════════════════════════════════
// 弹窗
// ════════════════════════════════════════════════════
function initModal() {
  $('btnAddServer').addEventListener('click', () => openServerModal());
  $('btnModalClose').addEventListener('click', closeServerModal);
  $('btnModalCancel').addEventListener('click', closeServerModal);
  $('btnModalSave').addEventListener('click', saveServer);

  // 点击遮罩关闭
  $('modalMask').addEventListener('click', (e) => {
    if (e.target === $('modalMask')) closeServerModal();
  });

  // 浏览密钥文件
  $('btnBrowseKey').addEventListener('click', async () => {
    const file = await window.api.dialog.openFile();
    if (file) $('fKey').value = file;
  });
  $('btnClearKey').addEventListener('click', () => { $('fKey').value = ''; });

  // 弹窗内测试连接
  $('btnTestConn').addEventListener('click', async () => {
    const srv = {
      host:     $('fHost').value.trim(),
      port:     parseInt($('fPort').value) || 22,
      username: $('fUser').value.trim(),
      password: $('fPass').value,
      keyFile:  $('fKey').value.trim(),
    };
    if (!srv.host || !srv.username) {
      setTestResult($('modalTestResult'), 'error', '请先填写主机和用户名');
      return;
    }
    setTestResult($('modalTestResult'), 'testing', '⏳ 连接中...');
    const res = await window.api.ssh.test(srv);
    setTestResult($('modalTestResult'), res.ok ? 'success' : 'error',
      res.ok ? `✅ ${res.msg}` : `❌ ${res.msg}`);
  });

  // Enter 保存
  $('serverModal').addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && e.target.tagName === 'INPUT') saveServer();
    if (e.key === 'Escape') closeServerModal();
  });
}

// ════════════════════════════════════════════════════
// 工具函数
// ════════════════════════════════════════════════════
function escHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

// ════════════════════════════════════════════════════
// 初始化
// ════════════════════════════════════════════════════
async function init() {
  initTitlebar();
  initNav();
  initModal();

  // 组件选择按钮
  $('btnSelectAll').addEventListener('click',  () => selectAllComponents(true));
  $('btnSelectNone').addEventListener('click', () => selectAllComponents(false));

  // 安装页面
  $('btnInstallAll').addEventListener('click',   () => startInstall('all'));
  $('btnStartInstall').addEventListener('click', () => startInstall('selected'));
  $('btnStopInstall').addEventListener('click',  stopInstall);
  $('btnPrecheck').addEventListener('click',     runPrecheck);
  $('btnClearLog').addEventListener('click', clearLog);
  $('btnTestInInstall').addEventListener('click', () => {
    testConnection($('targetServer').value, $('serverTestResult'));
  });

  // 关于弹窗
  $('navAbout').addEventListener('click', openAboutModal);
  $('btnAboutClose').addEventListener('click', closeAboutModal);
  $('btnAboutOk').addEventListener('click', closeAboutModal);
  $('aboutMask').addEventListener('click', (e) => {
    if (e.target === $('aboutMask')) closeAboutModal();
  });

  // 加载数据
  await Promise.all([loadServers(), loadComponents()]);
}

document.addEventListener('DOMContentLoaded', init);
