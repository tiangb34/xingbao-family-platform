(function() {
  const API_BASE = '/api/v1';
  let sessionId = '';
  let isOpen = false;

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
      width: 380px; max-width: calc(100vw - 48px); height: 520px; max-height: calc(100vh - 120px);
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
            <small>基于知识库的智能问答</small>
          </div>
          <button class="chat-close" aria-label="关闭">×</button>
        </div>
        <div class="chat-messages" id="chat-messages">
          <div class="chat-welcome">
            <div class="welcome-icon">🌟</div>
            <p>你好！我是星宝AI助手</p>
            <small>可以问我关于自闭症干预、家庭训练等问题</small>
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

    fab.addEventListener('click', () => {
      isOpen = !isOpen;
      fab.classList.toggle('active', isOpen);
      modal.classList.toggle('open', isOpen);
      if (isOpen) setTimeout(() => input.focus(), 200);
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

  function addMessage(role, content) {
    const messages = document.getElementById('chat-messages');
    const welcome = messages.querySelector('.chat-welcome');
    if (welcome) welcome.remove();

    const avatar = role === 'bot' ? '🤖' : '👤';
    const msg = document.createElement('div');
    msg.className = `chat-msg ${role}`;
    msg.innerHTML = `
      <div class="avatar">${avatar}</div>
      <div class="bubble">${escapeHtml(content)}</div>
    `;
    messages.appendChild(msg);
    messages.scrollTop = messages.scrollHeight;
    return msg;
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

    input.value = '';
    sendBtn.disabled = true;
    addMessage('user', message);

    addTypingIndicator();

    try {
      const response = await fetch(API_BASE + '/ai/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message, sessionId })
      });
      const result = await response.json();
      removeTypingIndicator();

      if (result.code === 200) {
        const reply = result.data.reply;
        addMessage('bot', reply);
        // 保存后端返回的 sessionId，后续请求携带
        if (result.data.sessionId) {
          sessionId = result.data.sessionId;
        }
      } else {
        addMessage('bot', '抱歉，请求失败：' + (result.message || '未知错误'));
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