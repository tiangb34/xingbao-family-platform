const baseLoadSettings = window.loadSettings;
window.loadSettings = async () => {
  await baseLoadSettings();
  const form = document.querySelector('#settings-form');
  if (!document.querySelector('#learning-categories')) form.insertAdjacentHTML('beforeend', '<fieldset id="learning-category-settings"><legend>成长学习参数</legend><label>成长学习内容分类（逗号分隔）<input id="learning-categories" type="text" placeholder="沟通表达,日常自理,家长支持"/></label></fieldset>');
  const data = await request('/admin/system-settings');
  document.querySelector('#learning-categories').value = (data.learningCategories || []).join(',');
};
document.querySelector('#settings-form').onsubmit = async event => {
  event.preventDefault();
  const maxImages = +document.querySelector('#community-max-images').value;
  try {
    await request('/admin/system-settings/community-max-images', {method:'POST',body:JSON.stringify({maxImages})});
    await request('/admin/system-settings/community-permissions', {method:'POST',body:JSON.stringify({allowCommentEdit:document.querySelector('#allow-comment-edit').checked,allowCommentDelete:document.querySelector('#allow-comment-delete').checked,allowPostDelete:document.querySelector('#allow-post-delete').checked})});
    await request('/admin/system-settings/training-categories', {method:'POST',body:JSON.stringify({categories:document.querySelector('#training-categories').value})});
    await request('/admin/system-settings/learning-categories', {method:'POST',body:JSON.stringify({categories:document.querySelector('#learning-categories').value})});
    alert('系统设置已保存');
  } catch (error) { alert(error.message); }
};
