setTimeout(() => {
  const baseRenderTraining = window.renderTraining;
  if (!baseRenderTraining) return;

  window.renderTraining = async () => {
    await baseRenderTraining();
    const root = $('#training-content');
    const childId = root.dataset.childId;
    const form = $('#cue-form');
    if (!form) return;
    const cues = [...root.querySelectorAll('.cue-grid .cue-card')].map(card => ({
      id: +card.dataset.cueId,
      title: card.textContent.trim(),
      color: card.dataset.cueColor,
      imageUrl: card.querySelector('img')?.getAttribute('src') || ''
    }));

    form.innerHTML = `<input name="title" placeholder="提示语，如：先洗手" required/>
      <div class="cue-form-row"><label class="cue-color">卡片颜色<input name="color" type="color" value="#FFE1EB"/></label>
      <label class="cue-upload"><input id="cue-image" type="file" accept="image/jpeg,image/png" onchange="previewCueImage(this)" hidden/><span>＋ 选择配图</span><small id="cue-file-name">支持 JPG、PNG</small></label></div>
      <div id="cue-image-preview" class="cue-image-preview"></div><button type="submit">新增提示卡</button>`;

    const grid = root.querySelector('.cue-grid');
    grid.innerHTML = cues.map(c => `<article class="cue-card" style="background:${safe(c.color)}" onclick="speakCue(this.querySelector('strong').textContent)">
      ${c.imageUrl ? `<img src="${safe(c.imageUrl)}" alt="提示卡配图"/>` : ''}<strong>${safe(c.title)}</strong>
      <button class="cue-more" type="button" aria-label="提示卡操作" onclick="event.stopPropagation();toggleCueActions(this)">···</button>
      <span class="cue-action-menu" hidden><button type="button" onclick="deleteCue(${c.id})">删除</button></span></article>`).join('') || '<p>可创建文字或图片提示卡。</p>';

    form.onsubmit = async event => {
      event.preventDefault();
      try {
        const data = Object.fromEntries(new FormData(form));
        const file = $('#cue-image').files[0];
        const imageUrl = file ? await uploadPostImage(file) : '';
        await request('/home-training/cues', {method: 'POST', body: JSON.stringify({childId: +childId, title: data.title, color: data.color, imageUrl})});
        await renderTraining();
      } catch (error) { alert(error.message); }
    };
  };

  window.previewCueImage = input => {
    const file = input.files[0];
    if (!file) return;
    if (!['image/jpeg', 'image/png'].includes(file.type)) { alert('仅支持 JPG、PNG 图片'); input.value = ''; return; }
    $('#cue-file-name').textContent = file.name;
    $('#cue-image-preview').innerHTML = `<img src="${URL.createObjectURL(file)}" alt="图片预览"/>`;
  };
  window.toggleCueActions = button => {
    document.querySelectorAll('.cue-action-menu').forEach(menu => { if (menu !== button.nextElementSibling) menu.hidden = true; });
    button.nextElementSibling.hidden = !button.nextElementSibling.hidden;
  };
  window.deleteCue = async id => {
    if (!confirm('确认删除这张视觉提示卡吗？')) return;
    try { await request(`/home-training/cues/${id}`, {method: 'DELETE'}); await renderTraining(); }
    catch (error) { alert(error.message); }
  };

  document.head.insertAdjacentHTML('beforeend', `<style>
    .cue-form-row{display:flex;gap:8px;align-items:stretch}.cue-color{display:flex;align-items:center;gap:5px;padding:0 10px;border:1px solid #f1dbe2;border-radius:12px;color:#80656d;font-size:12px;background:#fff}.cue-color input{width:28px;height:28px;padding:0;border:0;background:none}.cue-upload{display:flex;flex:1;min-width:0;align-items:center;gap:6px;padding:9px 11px;border:1px dashed #ef9eb5;border-radius:12px;color:#ce607f;background:#fff8fa;cursor:pointer}.cue-upload span{font-size:13px;font-weight:700}.cue-upload small{overflow:hidden;color:#9e8790;white-space:nowrap;text-overflow:ellipsis}.cue-image-preview img{width:100%;max-height:120px;object-fit:cover;border-radius:12px}.cue-card{position:relative;cursor:pointer}.cue-card strong{display:block;padding-right:24px}.cue-more{position:absolute;right:8px;bottom:7px;width:auto!important;min-height:0!important;padding:2px 6px!important;border:0!important;color:#9d7b84!important;background:rgba(255,255,255,.55)!important}.cue-action-menu{position:absolute;right:8px;bottom:31px;z-index:2;padding:4px;border-radius:9px;background:#fff;box-shadow:0 6px 18px rgba(107,65,76,.18)}.cue-action-menu button{width:auto!important;min-height:0!important;padding:5px 8px!important;border:0!important;color:#a45d70!important;background:transparent!important}
  </style>`);
}, 700);
