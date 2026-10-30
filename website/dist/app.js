(() => {
  const pages = ['home', 'download', 'vote', 'highscores'];
  function route() {
    const hash = location.hash.slice(1);
    const page = pages.includes(hash) ? hash : 'home';
    document.querySelectorAll('.page').forEach(el => { el.hidden = el.id !== page; });
    document.querySelectorAll('[data-page]').forEach(el => {
      if (el.dataset.page === page) el.setAttribute('aria-current', 'page');
      else el.removeAttribute('aria-current');
    });
    document.title = page === 'home' ? 'Lletya — Enter the Wilderness' : `${page.charAt(0).toUpperCase() + page.slice(1)} — Lletya`;
    if (hash !== 'experience') window.scrollTo(0, 0);
  }
  const config = window.LLETYA_CONFIG || {};
  function safeUrl(value) {
    try { const url = new URL(value); return url.protocol === 'https:' ? url.href : null; }
    catch { return null; }
  }
  const download = safeUrl(config.downloadUrl);
  if (download) {
    const link = document.querySelector('#download-link');
    link.href = download; link.hidden = false;
    document.querySelector('#download h2').textContent = 'Your next fight starts here.';
    document.querySelector('#download .notice p').textContent = 'Download the official Lletya launcher to get started.';
  }
  const votes = (config.voteLinks || []).filter(item => safeUrl(item.url));
  if (votes.length) {
    document.querySelector('#vote h2').textContent = 'Cast your vote.';
    document.querySelector('#vote .notice p').textContent = 'Support Lletya on our official server listings.';
    votes.forEach(item => {
      const link = document.createElement('a');
      link.className = 'button'; link.href = safeUrl(item.url);
      link.textContent = item.label; link.target = '_blank'; link.rel = 'noopener noreferrer';
      document.querySelector('#vote-links').append(link);
    });
  }
  document.querySelector('#year').textContent = new Date().getFullYear();
  window.addEventListener('hashchange', route); route();
})();
