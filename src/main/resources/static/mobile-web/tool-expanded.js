setTimeout(() => {
  const priorOpenTool = window.openTool;
  const details = (title, body, open = false) => `<details ${open ? 'open' : ''}><summary>${title}</summary><p>${body}</p></details>`;
  const render = (item, html) => { $('#tools-list').innerHTML = `<div class="tool-panel extended-tool"><button type="button" class="tool-back" onclick="loadTools()">返回工具列表</button><span class="tool-eyebrow">居家干预工具</span><h3>${safe(item.title)}</h3><p>${safe(item.content)}</p><div class="extended-faq">${html}</div><p class="notice">内容仅供家庭日常参考，不替代诊断、治疗、法律意见或个体化专业建议。</p></div>`; };
  window.openTool = async id => {
    try {
      const data = await request('/tools');
      const item = (data.items || []).find(entry => entry.id === id);
      if (!item || item.type === 'COGNITION') return priorOpenTool(id);
      if (item.type === 'MANUAL') return render(item, details('第一步：先让现场安全下来','移开尖锐、易碎或可能造成伤害的物品；如有自伤、伤人或紧急医疗风险，优先联系当地急救或专业机构。',true) + details('第二步：降低刺激，不在高峰时讲道理','放低声音、减少围观、暂停追问。可去相对安静、安全的地方，给孩子一点空间，也可提供熟悉的安抚物。') + details('第三步：平静后再复盘','等孩子和大人都缓下来，用短句回顾“刚才太吵了”“突然改变了”。记下前因、当下表现、什么方法有效，为下次提前准备。') + details('家长也需要应急出口','如果自己快撑不住，可先请可信赖的家人接手几分钟；喝水、呼吸、离开刺激源。照顾自己不是自私，是为了更安全地陪伴。'));
      if (item.type === 'RESOURCE') return render(item, details('提示卡什么时候用？','在要开始一件事之前、过渡时和需要提醒时使用。例如“先洗手，再吃点心”“五分钟后收玩具”。一次只呈现一个清楚的信息。',true) + details('怎么制作更容易懂？','用孩子熟悉的真实照片、简单图标或短词；背景干净、字少、重点大。把卡放在看得见、拿得到的位置。') + details('孩子不看卡怎么办？','先由大人拿着卡配合短句和示范，不要强迫盯着看。可从他喜欢的活动、固定流程开始，慢慢建立“看卡就知道下一步”的经验。') + details('怎么判断有没有用？','记录一周：使用场景、是否减少催促、孩子是否更容易完成。有效就继续；无效可换图片、缩短步骤或调整放置位置。'));
      if (item.type === 'POLICY') return render(item, details('先准备哪些信息？','可先准备孩子年龄、居住地、已持有的证明或评估资料，以及想咨询的事项：康复、教育、辅具、照护、补贴或无障碍服务等。',true) + details('去哪里查更可靠？','优先查询当地残联、教育、民政、卫健部门和政务服务平台的官方渠道；政策因地区和时间不同，请以发布机关最新文件及线下答复为准。') + details('咨询时怎样问更有效？','把问题说具体，例如“本区学龄前儿童康复服务如何申请”“需要哪些材料、在哪个窗口办理、是否有时限”。记下答复单位、日期和所需材料。') + details('常见提醒','不要轻信收费代办、承诺包办或要求私下转账的信息。涉及权益、资格或材料争议时，可向当地主管部门进一步核实。'));
      if (item.type === 'SCALE') return render(item, details('量表/清单是做什么的？','它更适合帮助家长整理观察、记录变化、准备与专业人员沟通的具体例子；不能替代诊断，也不能凭一次结果给孩子贴标签。',true) + details('怎样记录才有用？','写下具体情境：什么时候、在哪里、发生了什么、持续多久、孩子怎样回应、什么支持有效。比只写“今天不好”更有帮助。') + details('什么情况建议寻求专业评估？','如果家长持续担心沟通、互动、行为、睡眠、进食或情绪安全，或这些情况影响日常生活，可带着记录咨询儿科、发育行为或相关专业人员。') + details('避免哪些误区？','不要把筛查结果当结论，不要与其他孩子简单比较，也不要因网上测验得分而恐慌或延误求助。'));
      return priorOpenTool(id);
    } catch (error) { alert(error.message); }
  };
  document.head.insertAdjacentHTML('beforeend', '<style>.tool-eyebrow{display:block;margin-bottom:6px;color:#c17282;font-size:12px;font-weight:800}.extended-faq{display:grid;gap:8px;margin-top:14px}.extended-faq details{border:1px solid #f1e2e5;border-radius:12px;background:#fffafa}.extended-faq summary{padding:11px 12px;color:#72545d;font-size:13px;font-weight:800;cursor:pointer}.extended-faq p{margin:0;padding:0 12px 12px;color:#6e6266;font-size:13px;line-height:1.75}.extended-faq details[open]{background:#fff6f8}</style>');
}, 850);
