setTimeout(() => {
  const priorOpenTool = window.openTool;
  window.openTool = async id => {
    try {
      const data = await request('/tools');
      const item = (data.items || []).find(entry => entry.id === id);
      if (!item || item.type !== 'COGNITION') return priorOpenTool(id);
      const contents = await request('/cognition-contents');
      const faq = contents.length ? contents.map((entry, index) => `<details ${index === 0 ? 'open' : ''}><summary>${safe(entry.title)}</summary>${entry.summary ? `<p class="cognition-summary">${safe(entry.summary)}</p>` : ''}<p>${safe(entry.content)}</p></details>`).join('') : '<p>暂未配置认知内容。</p>';
      $('#tools-list').innerHTML = `<div class="tool-panel cognition-panel"><button type="button" class="tool-back" onclick="loadTools()">返回工具列表</button><span class="cognition-eyebrow">全家都能看懂的支持小册</span><h3>家长认知小课堂</h3><p>${safe(item.content)}</p><div class="cognition-faq">${faq}</div><div class="cognition-action"><b>全家今天先做三件小事</b><ol><li>把“他怎么又这样”换成“他现在可能哪里不舒服或难理解”。</li><li>对星宝一次只说一件事，并多等一会儿。</li><li>对爸爸妈妈说一句“辛苦了，我来搭把手”。</li></ol></div><p class="notice">内容用于日常认知支持，不替代诊断、治疗或个体化专业建议；若对孩子的发展、情绪或安全有担忧，请咨询合适的专业人员。</p></div>`;
    } catch (error) { alert(error.message); }
  };
  document.head.insertAdjacentHTML('beforeend', '<style>.cognition-summary{padding-bottom:3px!important;color:#bf7186!important;font-weight:700}.cognition-action{margin-top:16px;padding:13px;border-radius:12px;background:#fff2e8;color:#795c52}.cognition-action ol{margin:8px 0 0;padding-left:20px;font-size:13px;line-height:1.75}</style>');
}, 650);
