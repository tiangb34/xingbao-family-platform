setTimeout(() => {
  const baseRenderTraining = window.renderTraining;
  if (!baseRenderTraining) return;
  let selectedTrendDate = '';
  const stateLabel = {DONE: '完成', ASSISTED: '需协助', MISSED: '未完成', PENDING: '未记录'};

  window.renderTraining = async () => {
    await baseRenderTraining();
    const root = $('#training-content');
    if (!root || !root.dataset.childId) return;
    const childId = root.dataset.childId;
    const trend = {days: [...root.querySelectorAll('.trend-row')].map(row => ({date: row.dataset.date, done: +row.dataset.done, total: +row.dataset.total}))};
    const todayTasks = await request('/home-training/trend/day?childId=' + childId + '&date=' + localDate() + '&includeEnded=false');
    const todayTitle = [...root.querySelectorAll('h3')].find(node => node.textContent === '今日任务');
    const taskHolder = todayTitle && todayTitle.nextElementSibling;
    if (taskHolder) taskHolder.innerHTML = todayTasks.map(task => `<article class="card training-task status-${task.result.toLowerCase()}">
      <b>${safe(task.title)}${task.result !== 'PENDING' ? ` <i class="task-status">${stateLabel[task.result]}</i>` : ''}<button class="training-task-more" type="button" aria-label="训练任务操作" onclick="event.stopPropagation();toggleTrainingTaskActions(this)">···</button><span class="training-task-menu" hidden><button type="button" onclick="endTrainingTask(${task.id})">结束训练</button></span></b><p>${safe(task.category)} · 每日 ${task.dailyTarget} ${task.dailyUnit === 'HOUR' ? '小时' : '天'}</p>
      ${task.result === 'PENDING' ? `<div class="actions"><button onclick="recordTraining(${task.id},'DONE')">完成</button><button onclick="recordTraining(${task.id},'ASSISTED')">需协助</button><button onclick="recordTraining(${task.id},'MISSED')">未完成</button></div>` : ''}</article>`).join('') || '<p>暂无任务，先新增一个小目标。</p>';

    const trendTitle = [...root.querySelectorAll('h3')].find(node => node.textContent === '近 7 天使用趋势');
    const trendHolder = trendTitle && trendTitle.nextElementSibling;
    if (!trendHolder) return;
    if (!selectedTrendDate || !trend.days.some(day => day.date === selectedTrendDate)) selectedTrendDate = trend.days.at(-1)?.date || '';
    trendHolder.innerHTML = `<div id="training-trend-days">${trend.days.map(day => `<button type="button" class="trend-day-line ${day.date === selectedTrendDate ? 'selected' : ''}" onclick="showTrainingDay('${day.date}')"><span>${day.date.slice(5)}</span><i><em style="width:${Math.min(100, day.done * 25)}%"></em></i><b>${day.done}/${day.total}</b></button>`).join('')}</div><div id="trend-day-detail"></div>`;
    if (selectedTrendDate) await window.showTrainingDay(selectedTrendDate, false);
  };

  window.showTrainingDay = async (date, repaint = true) => {
    selectedTrendDate = date;
    const root = $('#training-content');
    if (!root) return;
    if (repaint) { await renderTraining(); return; }
    try {
      const rows = await request('/home-training/trend/day?childId=' + root.dataset.childId + '&date=' + date);
      const detail = $('#trend-day-detail');
      if (!detail) return;
      detail.innerHTML = `<p class="trend-detail-title">${date.replaceAll('-', '年').replace(/年(\d{2})$/, '月$1日')} 的训练记录</p>${rows.map(row => `<div class="trend-detail-row"><span>${safe(row.title)}</span><i class="status-${row.result.toLowerCase()}">${stateLabel[row.result]}</i></div>`).join('') || '<p>当天暂无训练任务。</p>'}`;
    } catch (error) { alert(error.message); }
  };
  window.toggleTrainingTaskActions = button => {
    document.querySelectorAll('.training-task-menu').forEach(menu => { if (menu !== button.nextElementSibling) menu.hidden = true; });
    button.nextElementSibling.hidden = !button.nextElementSibling.hidden;
  };
  window.endTrainingTask = async id => {
    if (!confirm('结束后，该任务将不再出现在每日训练中，历史记录会保留。确认结束吗？')) return;
    try { await request(`/home-training/tasks/${id}/end`, {method: 'POST'}); await renderTraining(); }
    catch (error) { alert(error.message); }
  };
  function localDate() { const date = new Date(), month = String(date.getMonth() + 1).padStart(2, '0'), day = String(date.getDate()).padStart(2, '0'); return `${date.getFullYear()}-${month}-${day}`; }

  document.head.insertAdjacentHTML('beforeend', `<style>
    .training-task b{position:relative}.training-task .task-status{margin-left:5px;padding:2px 6px;border-radius:9px;color:#9b7880;background:#f7e8eb;font-size:11px;font-style:normal;font-weight:600}.training-task.status-done .task-status,.trend-detail-row .status-done{color:#4f9074;background:#e5f4ea}.training-task.status-assisted .task-status,.trend-detail-row .status-assisted{color:#b47b38;background:#fff1d9}.training-task.status-missed .task-status,.trend-detail-row .status-missed{color:#b76b77;background:#fde7eb}.training-task-more{float:right;width:auto!important;min-height:0!important;padding:0 4px!important;border:0!important;color:#a4828b!important;background:transparent!important}.training-task-menu{position:absolute;right:0;top:22px;z-index:3;padding:4px;border-radius:9px;background:#fff;box-shadow:0 6px 18px rgba(107,65,76,.18)}.training-task-menu button{width:auto!important;min-height:0!important;padding:5px 8px!important;border:0!important;color:#a45d70!important;background:transparent!important}#training-trend-days{display:grid;gap:7px;padding:2px 0 9px}.trend-day-line{display:grid;grid-template-columns:62px 1fr 38px;align-items:center;gap:8px;width:100%;padding:4px 2px!important;border:0!important;border-radius:8px!important;color:#75646a!important;background:transparent!important;font-size:12px!important;text-align:left}.trend-day-line>i{height:8px;overflow:hidden;border-radius:9px;background:#f0e8ea}.trend-day-line>i em{display:block;height:100%;background:#ff7498}.trend-day-line.selected{padding:6px 7px!important;color:#c15575!important;background:#fff2f5!important}.trend-day-line.selected>i{background:#f8d3dd}.trend-day-line b{text-align:right;font-size:12px}.trend-detail-title{margin:4px 0 6px;color:#805f68;font-size:12px}.trend-detail-row{display:flex;align-items:center;justify-content:space-between;padding:8px 10px;border-radius:9px;background:#fff8fa;font-size:13px}.trend-detail-row+.trend-detail-row{margin-top:5px}.trend-detail-row i{padding:3px 7px;border-radius:9px;color:#9b7880;background:#f1e8ea;font-size:11px;font-style:normal}
  </style>`);
}, 800);
