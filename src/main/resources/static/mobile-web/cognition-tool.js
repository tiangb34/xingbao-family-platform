setTimeout(() => {
  const baseOpenTool = window.openTool;
  window.openTool = async id => {
    try {
      const data = await request('/tools');
      const item = (data.items || []).find(entry => entry.id === id);
      if (!item || item.type !== 'COGNITION') return baseOpenTool(id);
      $('#tools-list').innerHTML = `<div class="tool-panel cognition-panel"><button type="button" class="tool-back" onclick="loadTools()">返回工具列表</button><span class="cognition-eyebrow">给星宝爸爸妈妈的一份理解</span><h3>家长认知小课堂</h3><p>不急着给孩子贴标签，也不把困难归咎于谁。先多一份理解，再选择适合家庭节奏的支持方式。</p><div class="cognition-list"><article><b>误解：这是“教得不好”造成的。</b><p>更接近的理解：孤独症谱系的表现具有复杂的个体与发展差异，不是由父母的爱与努力不足造成的。</p></article><article><b>误解：所有星宝都会以同一种方式表现。</b><p>更接近的理解：每个孩子的沟通、感受、兴趣和支持需要都不同，比较不如观察孩子自己。</p></article><article><b>误解：不回应就是故意不听。</b><p>更接近的理解：孩子可能正在处理声音、信息或情绪；用更清晰、短一些的提示，并留出等待时间。</p></article><article><b>误解：支持就是让孩子“变得和别人一样”。</b><p>更接近的理解：支持的目标是帮助孩子更安全、更舒适地参与生活，并发展适合自己的表达与能力。</p></article></div><div class="cognition-action"><b>今天可以试的一小步</b><ol><li>把“他为什么不配合”换成“现在什么让他不舒服或难理解”。</li><li>和家人统一一句温和提示，减少反复催促。</li><li>记录一个孩子做到了的小变化，也肯定自己已经付出的陪伴。</li></ol></div><p class="notice">内容用于家长日常学习与沟通参考，不替代诊断、治疗或专业建议；有具体困扰时可咨询合适的专业人员。</p></div>`;
    } catch (error) { alert(error.message); }
  };
  document.head.insertAdjacentHTML('beforeend', '<style>.cognition-eyebrow{display:inline-block;padding:4px 8px;border-radius:10px;color:#af6578;background:#fff0f3;font-size:11px;font-weight:700}.cognition-panel h3{margin-top:10px}.cognition-list{display:grid;gap:9px;margin:15px 0}.cognition-list article{padding:12px;border:1px solid #f0e1e5;border-radius:12px;background:#fffafa}.cognition-list b{color:#73545d;font-size:13px}.cognition-list p{margin-top:6px;font-size:13px;line-height:1.6}.cognition-action{padding:13px;border-radius:12px;color:#725e64;background:#fff5e7}.cognition-action ol{margin:8px 0 0;padding-left:20px;font-size:13px;line-height:1.8}</style>');
}, 300);
