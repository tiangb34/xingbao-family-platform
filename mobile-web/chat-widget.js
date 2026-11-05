(function() {
  const API_BASE = '/api/v1';
  let sessionId = '';
  let isOpen = false;
  let mode = 'rag';           // 'rag' = 知识库问答，'agent' = 干预计划 Agent
  let children = [];          // 缓存孩子档案列表

  const styles = `
    #xingbao-chat-fab {
      position: fixed; bottom: 24px; right: 24px; z-index: 9999;
      width: 56px; height: 56px; border-radius: 50%; border: none;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white; font-size: 24px; cursor: pointer;
      box-shadow: 0 4px 15px rgba(102,126,234,0.4);
      transition: transform 0.2s, box-shadow 0.2s;
      display: flex; align-items: center; justify-content: center;
    }
    #xingbao-chat-fab:hover { transform: scale(1.08); box-shadow: 0 6px 20px rgba(102,126,234,0.5); }
    #xingbao-chat-fab .fab-icon { transition: transform 0.3s; }
    #xingbao-chat-fab.active .fab-icon { transform: rotate(90deg); }

    #xingbao-chat-modal {
      position: fixed; bottom: 90px; right: 24px; z-index: 9998;
      width: 380px; max-width: calc(100vw - 48px); height: 560px; max-height: calc(100vh - 120px);
      border-radius: 16px; background: white;
      box-shadow: 0 8px 32px rgba(0,0,0,0.15);
      display: flex; flex-direction: column;
      transform: scale(0) translateY(20px); opacity: 0;
      transform-origin: bottom right;
      transition: transform 0.25s ease, opacity 0.25s ease;
      overflow: hidden;
    }
    #xingbao-chat-modal.open { transform: scale(1) translateY(0); opacity: 1; }

    .chat-header {
      padding: 14px 16px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white; display: flex; align-items: center; justify-content: space-between;
    }
    .chat-header h3 { margin: 0; font-size: 15px; font-weight: 600; }
    .chat-header small { font-size: 11px; opacity: 0.85; }
    .chat-close {
      width: 28px; height: 28px; border-radius: 50%; border: none;
      background: rgba(255,255,255,0.2); color: white; font-size: 16px;
      cursor: pointer; display: flex; align-items: center; justify-content: center;
    }
    .chat-close:hover { background: rgba(255,255,255,0.3); }

    /* 模式切换 */
    .chat-mode-tabs {
      display: flex; gap: 4px; padding: 8px 12px; background: #f8f9fa;
      border-bottom: 1px solid #e9ecef;
    }
    .chat-mode-tabs button {
      flex: 1; padding: 7px 10px; border: 1px solid #dee2e6; border-radius: 14px;
      background: white; font-size: 12px; color: #495057; cursor: pointer;
      transition: all 0.15s; font-weight: 500;
    }
    .chat-mode-tabs button.active {
      background: linear-gradient(135deg, #667eea, #764ba2);
      color: white; border-color: transparent;
    }

    /* Agent 模式：孩子选择栏 */
    .chat-agent-bar {
      padding: 8px 12px; background: #f0f4ff;
      border-bottom: 1px solid #e0e7ff; display: flex; gap: 8px; align-items: center;
      font-size: 12px; color: #475569;
    }
    .chat-agent-bar select {
      flex: 1; padding: 6px 10px; border: 1px solid #cbd5e1; border-radius: 8px;
      font-size: 12px; outline: none; background: white;
    }

    .chat-messages {
      flex: 1; overflow-y: auto; padding: 12px;
      display: flex; flex-direction: column; gap: 10px;
      background: #f8f9fa;
    }
    .chat-messages::-webkit-scrollbar { width: 4px; }
    .chat-messages::-webkit-scrollbar-thumb { background: #ccc; border-radius: 2px; }

    .chat-msg { display: flex; gap: 8px; max-width: 85%; }
    .chat-msg.user { align-self: flex-end; flex-direction: row-reverse; }
    .chat-msg .avatar {
      width: 28px; height: 28px; border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      font-size: 13px; flex-shrink: 0;
    }
    .chat-msg.bot .avatar { background: linear-gradient(135deg, #667eea, #764ba2); color: white; }
    .chat-msg.user .avatar { background: #e9ecef; color: #495057; }
    .chat-msg .bubble {
      padding: 10px 12px; border-radius: 12px; font-size: 13px; line-height: 1.5;
      word-break: break-word;
    }
    .chat-msg.bot .bubble { background: white; color: #333; border-bottom-left-radius: 4px; }
    .chat-msg.user .bubble { background: #667eea; color: white; border-bottom-right-radius: 4px; }

    /* Agent 执行轨迹（可折叠） */
    .trace-collapsible {
      margin-top: 6px; border: 1px solid #e5e7eb; border-radius: 8px;
      background: #fafbfc; overflow: hidden;
    }
    .trace-header {
      padding: 7px 10px; font-size: 11px; font-weight: 600;
      color: #6b7280; background: #f3f4f6; cursor: pointer;
      display: flex; justify-content: space-between; align-items: center;
      user-select: none;
    }
    .trace-header:hover { background: #e5e7eb; }
    .trace-toggle { font-size: 10px; transition: transform 0.2s; }
    .trace-expanded .trace-toggle { transform: rotate(90deg); }
    .trace-body {
      max-height: 0; overflow: hidden; transition: max-height 0.3s ease;
      padding: 0 10px;
    }
    .trace-expanded .trace-body {
      max-height: 400px; overflow-y: auto; padding: 8px 10px;
    }

    /* Agent 工具调用轨迹 */
    .trace-list { margin-top: 8px; padding-top: 8px; border-top: 1px dashed #e9ecef; }
    .trace-item {
      font-size: 11px; padding: 3px 0; display: flex; gap: 6px;
    }
    .trace-item .trace-type {
      font-weight: 600; flex-shrink: 0; padding: 1px 6px; border-radius: 8px;
      font-size: 10px;
    }
    .trace-item.think .trace-type { background: #fef3c7; color: #92400e; }
    .trace-item.tool .trace-type { background: #dbeafe; color: #1e40af; }
    .trace-item.final .trace-type { background: #dcfce7; color: #166534; }
    .trace-item.guard .trace-type { background: #fee2e2; color: #991b1b; }
    .trace-item.error .trace-type { background: #f3f4f6; color: #6b7280; }
    .trace-item .trace-detail { color: #6b7280; overflow: hidden; text-overflow: ellipsis; }

    /* Agent 计划卡片（家长友好版） */
    .plan-card { margin-top: 10px; }
    .plan-summary-banner {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white; padding: 10px 14px; border-radius: 10px; margin-bottom: 10px;
      font-size: 13px; font-weight: 600; line-height: 1.5;
    }
    .plan-section { margin-bottom: 12px; }
    .plan-section-label {
      font-size: 11px; font-weight: 700; color: #8b5cf6; margin-bottom: 6px;
      text-transform: none; letter-spacing: 0;
      display: flex; align-items: center; gap: 4px;
    }
    .plan-section-label .emoji { font-size: 14px; }

    /* 每日安排 - 时间轴 */
    .daily-item {
      display: flex; gap: 8px; padding: 6px 0;
      border-left: 2px solid #e0e7ff; margin-left: 4px; padding-left: 10px;
    }
    .daily-item .plan-time {
      flex-shrink: 0; background: #ede9fe; color: #5b21b6;
      padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 600;
      height: fit-content;
    }
    .daily-item .daily-body { flex: 1; min-width: 0; }
    .daily-item .daily-task {
      font-size: 13px; font-weight: 600; color: #1f2937; margin-bottom: 2px;
    }
    .daily-item .daily-details {
      font-size: 12px; color: #6b7280; line-height: 1.5;
    }

    /* 本周重点 - checklist */
    .focus-list { list-style: none; padding: 0; margin: 0; }
    .focus-list li {
      font-size: 12.5px; color: #374151; padding: 4px 0 4px 18px;
      position: relative; line-height: 1.5;
    }
    .focus-list li::before {
      content: '✅'; position: absolute; left: 0; top: 4px; font-size: 11px;
    }

    /* 推荐教具 - chips */
    .tools-chips { display: flex; flex-wrap: wrap; gap: 6px; }
    .tools-chip {
      background: #fef3c7; color: #92400e; border: 1px solid #fde68a;
      padding: 3px 10px; border-radius: 12px; font-size: 11.5px; font-weight: 500;
    }

    /* 温馨提示 - 提示框 */
    .notes-box {
      background: #fff7ed; border: 1px solid #fed7aa; border-radius: 8px;
      padding: 10px 12px; font-size: 12px; color: #9a3412; line-height: 1.6;
    }

    .plan-iterations {
      margin-top: 8px; font-size: 11px; color: #9ca3af; font-style: italic;
    }

    .chat-welcome {
      text-align: center; padding: 24px 16px; color: #6c757d;
    }
    .chat-welcome .welcome-icon { font-size: 32px; margin-bottom: 8px; }
    .chat-welcome p { margin: 0 0 4px; font-size: 13px; }
    .chat-welcome small { font-size: 11px; color: #adb5bd; }

    .chat-suggestions {
      display: flex; flex-wrap: wrap; gap: 6px; padding: 0 12px 10px;
    }
    .chat-suggestions button {
      padding: 6px 10px; border: 1px solid #dee2e6; border-radius: 16px;
      background: white; font-size: 12px; color: #495057; cursor: pointer;
      transition: all 0.15s;
    }
    .chat-suggestions button:hover { background: #667eea; color: white; border-color: #667eea; }

    .chat-input-area {
      padding: 10px 12px; border-top: 1px solid #e9ecef;
      display: flex; gap: 8px; background: white;
    }
    .chat-input-area input {
      flex: 1; padding: 10px 12px; border: 1px solid #dee2e6; border-radius: 20px;
      font-size: 13px; outline: none; transition: border-color 0.2s;
    }
    .chat-input-area input:focus { border-color: #667eea; }
    .chat-send {
      width: 38px; height: 38px; border-radius: 50%; border: none;
      background: linear-gradient(135deg, #667eea, #764ba2);
      color: white; font-size: 16px; cursor: pointer;
      display: flex; align-items: center; justify-content: center;
      transition: opacity 0.2s;
    }
    .chat-send:disabled { opacity: 0.5; cursor: not-allowed; }

    .typing-indicator { display: flex; gap: 4px; padding: 4px 0; }
    .typing-indicator span {
      width: 6px; height: 6px; border-radius: 50%; background: #adb5bd;
      animation: typing 1.2s infinite;
    }
    .typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
    .typing-indicator span:nth-child(3) { animation-delay: 0.4s; }
    @keyframes typing {
      0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
      30% { transform: translateY(-6px); opacity: 1; }
    }

    @media (max-width: 480px) {
      #xingbao-chat-modal {
        width: calc(100vw - 16px); right: 8px; bottom: 80px;
        height: calc(100vh - 100px);
      }
      #xingbao-chat-fab { bottom: 16px; right: 16px; }
    }
  `;

  function currentUser() {
    try { return JSON.parse(localStorage.getItem('xingbao-user') || 'null'); }
    catch (e) { return null; }
  }

  async function loadChildren() {
    const u = currentUser();
    if (!u) return [];
    try {
      const resp = await fetch(API_BASE + '/children?userId=' + u.id);
      const data = await resp.json();
      return (data.code === 200 || data.code === 0) ? data.data : [];
    } catch (e) { return []; }
  }

  function init() {
    const styleEl = document.createElement('style');
    styleEl.textContent = styles;
    document.head.appendChild(styleEl);

    document.body.insertAdjacentHTML('beforeend', `
      <button id="xingbao-chat-fab" aria-label="打开AI助手">
        <span class="fab-icon">💬</span>
      </button>
      <div id="xingbao-chat-modal">
        <div class="chat-header">
          <div>
            <h3>星宝AI助手</h3>
            <small id="chat-subtitle">基于知识库的智能问答</small>
          </div>
          <button class="chat-close" aria-label="关闭">×</button>
        </div>
        <div class="chat-mode-tabs">
          <button data-mode="rag" class="active">💬 知识问答</button>
          <button data-mode="agent">🤖 干预计划</button>
        </div>
        <div class="chat-agent-bar" id="chat-agent-bar" style="display:none;">
          <span>档案：</span>
          <select id="chat-child-select">
            <option value="">加载中...</option>
          </select>
        </div>
        <div class="chat-messages" id="chat-messages">
          <div class="chat-welcome">
            <div class="welcome-icon">🌟</div>
            <p>你好！我是星宝AI助手</p>
            <small id="chat-welcome-sub">可以问我关于自闭症干预、家庭训练等问题</small>
          </div>
        </div>
        <div class="chat-suggestions" id="chat-suggestions">
          <button onclick="window.xingbaoChat.ask('什么是ABA干预？')">ABA干预是什么？</button>
          <button onclick="window.xingbaoChat.ask('如何培养孩子的语言能力？')">培养语言能力</button>
          <button onclick="window.xingbaoChat.ask('孩子情绪崩溃怎么办？')">情绪崩溃应对</button>
          <button onclick="window.xingbaoChat.ask('有哪些家庭训练方法？')">家庭训练方法</button>
        </div>
        <div class="chat-input-area">
          <input id="chat-input" placeholder="输入你的问题..." autocomplete="off"/>
          <button class="chat-send" id="chat-send" aria-label="发送">➤</button>
        </div>
      </div>
    `);

    const fab = document.getElementById('xingbao-chat-fab');
    const modal = document.getElementById('xingbao-chat-modal');
    const closeBtn = modal.querySelector('.chat-close');
    const input = document.getElementById('chat-input');
    const sendBtn = document.getElementById('chat-send');

    // 模式切换
    modal.querySelectorAll('.chat-mode-tabs button').forEach(btn => {
      btn.addEventListener('click', async () => {
        modal.querySelectorAll('.chat-mode-tabs button').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        const newMode = btn.dataset.mode;
        switchMode(newMode);
      });
    });

    fab.addEventListener('click', () => {
      isOpen = !isOpen;
      fab.classList.toggle('active', isOpen);
      modal.classList.toggle('open', isOpen);
      if (isOpen) {
        setTimeout(() => input.focus(), 200);
        if (mode === 'agent') refreshChildren();
      }
    });

    closeBtn.addEventListener('click', () => {
      isOpen = false;
      fab.classList.remove('active');
      modal.classList.remove('open');
    });

    sendBtn.addEventListener('click', sendMessage);
    input.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
      }
    });
  }

  function switchMode(newMode) {
    mode = newMode;
    const subtitle = document.getElementById('chat-subtitle');
    const agentBar = document.getElementById('chat-agent-bar');
    const suggestions = document.getElementById('chat-suggestions');
    const input = document.getElementById('chat-input');
    const welcome = document.getElementById('chat-welcome-sub');

    if (mode === 'agent') {
      subtitle.textContent = 'AI Agent · 多工具推理生成干预计划';
      agentBar.style.display = 'flex';
      input.placeholder = '描述需求，如 "4岁语言发育慢，每天能做点什么？"';
      welcome.textContent = '描述孩子情况，AI会自动查档案、匹配知识、生成计划';
      // 换 Agent 模式的快捷建议
      suggestions.innerHTML = `
        <button onclick="window.xingbaoChat.ask('4岁语言发育慢，每天在家能做点什么？')">4岁语言训练</button>
        <button onclick="window.xingbaoChat.ask('帮我做一个社交技能干预计划')">社交技能计划</button>
        <button onclick="window.xingbaoChat.ask('最近打卡少，怎么调整干预节奏？')">调整干预节奏</button>
      `;
      refreshChildren();
    } else {
      subtitle.textContent = '基于知识库的智能问答';
      agentBar.style.display = 'none';
      input.placeholder = '输入你的问题...';
      welcome.textContent = '可以问我关于自闭症干预、家庭训练等问题';
      suggestions.innerHTML = `
        <button onclick="window.xingbaoChat.ask('什么是ABA干预？')">ABA干预是什么？</button>
        <button onclick="window.xingbaoChat.ask('如何培养孩子的语言能力？')">培养语言能力</button>
        <button onclick="window.xingbaoChat.ask('孩子情绪崩溃怎么办？')">情绪崩溃应对</button>
        <button onclick="window.xingbaoChat.ask('有哪些家庭训练方法？')">家庭训练方法</button>
      `;
    }
  }

  async function refreshChildren() {
    children = await loadChildren();
    const select = document.getElementById('chat-child-select');
    if (!select) return;
    if (children.length === 0) {
      select.innerHTML = '<option value="">请先去首页创建孩子档案</option>';
      select.disabled = true;
    } else {
      select.innerHTML = children.map(c =>
        `<option value="${c.id}">${c.nickname || '档案#' + c.id}（${c.gender === 'FEMALE' ? '女' : c.gender === 'MALE' ? '男' : '未设'}）</option>`
      ).join('');
      select.disabled = false;
    }
  }

  function addMessage(role, content, extra) {
    const messages = document.getElementById('chat-messages');
    const welcome = messages.querySelector('.chat-welcome');
    if (welcome) welcome.remove();

    const avatar = role === 'bot' ? '🤖' : '👤';
    const msg = document.createElement('div');
    msg.className = `chat-msg ${role}`;

    let bubbleHTML = escapeHtml(content);
    if (role === 'bot' && extra) {
      // Agent 模式额外渲染：计划卡片 + 轨迹
      bubbleHTML += renderAgentResult(extra);
    }

    msg.innerHTML = `
      <div class="avatar">${avatar}</div>
      <div class="bubble">${bubbleHTML}</div>
    `;
    messages.appendChild(msg);
    messages.scrollTop = messages.scrollHeight;
    return msg;
  }

  function renderAgentResult(data) {
    let html = '';

    // ── 执行轨迹（移到上面，支持折叠）─────────────────────────
    if (data.trace && data.trace.length) {
      html += '<div class="trace-list trace-collapsible">';
      html += '<div class="trace-header" onclick="this.parentElement.classList.toggle(\'trace-expanded\')">';
      html += '<span>⚙ Agent 执行轨迹（共 ' + data.trace.length + ' 步）</span>';
      html += '<span class="trace-toggle">▸</span></div>';
      html += '<div class="trace-body">';
      data.trace.forEach(t => {
        html += `<div class="trace-item ${t.type}">
          <span class="trace-type">${t.type}</span>
          <span class="trace-detail">${escapeHtml(t.detail)}</span>
        </div>`;
      });
      html += '</div></div>';
    }

    if (data.iterations) {
      html += `<div class="plan-iterations">共 ${data.iterations} 轮推理</div>`;
    }

    // ── 计划卡片 ──────────────────────────────────────────────
    let plan = null;
    try { plan = JSON.parse(data.plan); } catch (e) { /* 不是合法 JSON，跳过卡片 */ }

    if (plan) {
      html += '<div class="plan-card">';

      // 核心目标：渐变横幅
      if (plan.planSummary) {
        html += `<div class="plan-summary-banner">${escapeHtml(plan.planSummary)}</div>`;
      }

      // 每日安排：时间轴
      if (plan.dailySchedule && plan.dailySchedule.length) {
        html += '<div class="plan-section">';
        html += '<div class="plan-section-label"><span class="emoji">🕐</span>每日安排</div>';
        plan.dailySchedule.forEach(item => {
          html += `<div class="daily-item">
            <span class="plan-time">${escapeHtml(item.time || '')}</span>
            <div class="daily-body">
              <div class="daily-task">${escapeHtml(item.task || '')}</div>
              <div class="daily-details">${escapeHtml(item.details || '')}</div>
            </div>
          </div>`;
        });
        html += '</div>';
      }

      // 本周重点：checklist
      if (plan.weeklyFocus && plan.weeklyFocus.length) {
        html += '<div class="plan-section">';
        html += '<div class="plan-section-label"><span class="emoji">🎯</span>本周重点</div>';
        html += '<ul class="focus-list">';
        plan.weeklyFocus.forEach(f => {
          html += `<li>${escapeHtml(stringifyItem(f))}</li>`;
        });
        html += '</ul></div>';
      }

      // 推荐教具：chips
      if (plan.tools && plan.tools.length) {
        html += '<div class="plan-section">';
        html += '<div class="plan-section-label"><span class="emoji">🧰</span>推荐教具</div>';
        html += '<div class="tools-chips">';
        plan.tools.forEach(t => {
          html += `<span class="tools-chip">${escapeHtml(stringifyItem(t))}</span>`;
        });
        html += '</div></div>';
      }

      // 温馨提示：提示框
      if (plan.notes) {
        html += '<div class="plan-section">';
        html += '<div class="plan-section-label"><span class="emoji">💡</span>温馨提示</div>';
        html += `<div class="notes-box">${escapeHtml(stringifyItem(plan.notes))}</div>`;
        html += '</div>';
      }

      html += '</div>';
    }

    return html;
  }

  // 把 LLM 可能返回的各种类型统一转字符串
  function stringifyItem(item) {
    if (item == null) return '';
    if (typeof item === 'string') return item;
    if (typeof item === 'number' || typeof item === 'boolean') return String(item);
    // 对象：优先取常见字段，都没有就 JSON
    if (typeof item === 'object') {
      return item.text || item.title || item.name || item.content || JSON.stringify(item);
    }
    return String(item);
  }

  function addTypingIndicator() {
    const messages = document.getElementById('chat-messages');
    const msg = document.createElement('div');
    msg.className = 'chat-msg bot';
    msg.id = 'typing-indicator';
    msg.innerHTML = `
      <div class="avatar">🤖</div>
      <div class="bubble"><div class="typing-indicator"><span></span><span></span><span></span></div></div>
    `;
    messages.appendChild(msg);
    messages.scrollTop = messages.scrollHeight;
  }

  function removeTypingIndicator() {
    const indicator = document.getElementById('typing-indicator');
    if (indicator) indicator.remove();
  }

  async function sendMessage() {
    const input = document.getElementById('chat-input');
    const sendBtn = document.getElementById('chat-send');
    const message = input.value.trim();
    if (!message) return;

    // Agent 模式：检查档案选择
    if (mode === 'agent') {
      const u = currentUser();
      if (!u) {
        addMessage('bot', '请先登录后再使用干预计划 Agent。');
        return;
      }
      const select = document.getElementById('chat-child-select');
      const childId = select ? select.value : '';
      if (!childId) {
        addMessage('bot', '请先在「星宝档案」创建孩子档案后再使用。');
        return;
      }
    }

    input.value = '';
    sendBtn.disabled = true;
    addMessage('user', message);

    addTypingIndicator();

    try {
      let response, result;

      if (mode === 'agent') {
        // Agent 模式：调 /api/v1/agent/plan
        const u = currentUser();
        const childId = document.getElementById('chat-child-select').value;
        response = await fetch(API_BASE + '/agent/plan', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ childId: Number(childId), userId: u.id, userMessage: message })
        });
        result = await response.json();
        removeTypingIndicator();
        if (result.code === 200 || result.code === 0) {
          addMessage('bot', '', result.data);  // result.data = { plan, iterations, trace }
        } else {
          addMessage('bot', '抱歉，Agent 请求失败：' + (result.message || '未知错误'));
        }
      } else {
        // RAG 模式：调 /api/v1/ai/chat（保持原有逻辑）
        response = await fetch(API_BASE + '/ai/chat', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ message, sessionId })
        });
        result = await response.json();
        removeTypingIndicator();
        if (result.code === 200 || result.code === 0) {
          const reply = result.data.reply || result.data.plan || JSON.stringify(result.data);
          addMessage('bot', reply);
          if (result.data.sessionId) sessionId = result.data.sessionId;
        } else {
          addMessage('bot', '抱歉，请求失败：' + (result.message || '未知错误'));
        }
      }
    } catch (error) {
      removeTypingIndicator();
      addMessage('bot', '抱歉，网络连接失败，请稍后再试。');
    } finally {
      sendBtn.disabled = false;
      input.focus();
    }
  }

  function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML.replace(/\n/g, '<br/>');
  }

  window.xingbaoChat = {
    ask: function(question) {
      if (!isOpen) {
        isOpen = true;
        document.getElementById('xingbao-chat-fab').classList.add('active');
        document.getElementById('xingbao-chat-modal').classList.add('open');
      }
      document.getElementById('chat-input').value = question;
      sendMessage();
    }
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
