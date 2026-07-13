// --- SHORTFLIX WEB ADMIN PORTAL - CORE JAVASCRIPT CONNECTIVITY ---

let supabaseClient = null;
let currentTab = 'dashboard';

// Cache arrays for filtering and search
let cachedDramas = [];
let cachedEpisodes = [];
let cachedUsers = [];

// Safe Storage Helper to avoid issues in Private Browsing / Incognito mode or strict cookie settings
function getStorageItem(key) {
    try {
        const val = localStorage.getItem(key);
        if (!val && key === 'supabase_url') {
            return 'https://jwzqxbzncuotjrjujhkf.supabase.co';
        }
        return val;
    } catch (e) {
        console.error("Storage access error:", e);
        if (key === 'supabase_url') {
            return 'https://jwzqxbzncuotjrjujhkf.supabase.co';
        }
        return null;
    }
}

function setStorageItem(key, value) {
    try {
        localStorage.setItem(key, value);
        return true;
    } catch (e) {
        console.error("Storage access error:", e);
        return false;
    }
}

// DOM Loaded / Safe Ready Check (handles dynamic/cached loading)
function runInit() {
    initializeConnection();
    switchTab('dashboard');
}

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", runInit);
} else {
    runInit();
}

// 1. Connection & Initializations
function initializeConnection() {
    const url = getStorageItem('supabase_url');
    const key = getStorageItem('supabase_key');

    const statusDot = document.getElementById('supabaseStatusDot');
    const statusText = document.getElementById('supabaseStatusText');

    if (url && key) {
        try {
            // Initialize Supabase Client
            supabaseClient = supabase.createClient(url, key);
            
            // Success indicators
            statusDot.className = "w-2.5 h-2.5 rounded-full bg-emerald-500 shadow-md shadow-emerald-500/50";
            statusText.innerText = "Đã liên kết thành công với dự án. Sẵn sàng hoạt động!";
            statusText.classList.remove('text-rose-400');
            statusText.classList.add('text-emerald-400');
            
            // Trigger refresh
            refreshData();
        } catch (e) {
            console.error("Initialization error: ", e);
            statusDot.className = "w-2.5 h-2.5 rounded-full bg-rose-500";
            statusText.innerText = "Cấu hình sai hoặc lỗi kết nối. Hãy kiểm tra lại Key!";
        }
    } else {
        // Missing keys
        statusDot.className = "w-2.5 h-2.5 rounded-full bg-rose-500 animate-pulse";
        statusText.innerText = "Chưa kết nối. Nhấp vào 'Cấu hình kết nối' để điền API Key.";
        statusText.classList.remove('text-emerald-400');
        statusText.classList.add('text-rose-400');
    }
}

// Save Connection Details
function saveConnectionSettings() {
    const url = document.getElementById('inputUrl').value.trim();
    const key = document.getElementById('inputKey').value.trim();

    if (!url || !key) {
        alert("Vui lòng điền đầy đủ Supabase URL và API Key!");
        return;
    }

    setStorageItem('supabase_url', url);
    setStorageItem('supabase_key', key);

    closeConfigModal();
    initializeConnection();
    
    alert("Đã lưu thông tin cấu hình! Đang nạp lại dữ liệu...");
}

// 2. Tab Navigation
function switchTab(tabId) {
    currentTab = tabId;
    
    // Hide all tabs
    document.querySelectorAll('.tab-content').forEach(el => el.classList.add('hidden'));
    // Show selected tab
    const selectedTabEl = document.getElementById(`tab-${tabId}`);
    if (selectedTabEl) selectedTabEl.classList.remove('hidden');

    // Handle Active Button styling
    document.querySelectorAll('aside nav button').forEach(btn => {
        btn.classList.remove('nav-active', 'text-white');
        btn.classList.add('text-slate-400');
    });

    const activeBtn = document.getElementById(`btn-${tabId}`);
    if (activeBtn) {
        activeBtn.classList.add('nav-active', 'text-white');
        activeBtn.classList.remove('text-slate-400');
    }

    // Load data specific to tab if connection is active
    if (supabaseClient) {
        if (tabId === 'dashboard') loadStats();
        else if (tabId === 'dramas') loadDramas();
        else if (tabId === 'episodes') {
            loadDramasDropdowns();
            loadEpisodesForDrama();
        }
        else if (tabId === 'users') loadUsersAndBalances();
    }
}

// Progress loading animation helper
function triggerLoadingAnimation(percent) {
    const loader = document.getElementById('topLoading');
    if (loader) {
        loader.style.width = `${percent}%`;
        if (percent >= 100) {
            setTimeout(() => { loader.style.width = '0%'; }, 500);
        }
    }
}

// Global Refresh Command
async function refreshData() {
    if (!supabaseClient) {
        openConfigModal();
        return;
    }

    const refreshIcon = document.getElementById('refreshIcon');
    if (refreshIcon) refreshIcon.classList.add('animate-spin');

    triggerLoadingAnimation(30);

    try {
        if (currentTab === 'dashboard') {
            await loadStats();
        } else if (currentTab === 'dramas') {
            await loadDramas();
        } else if (currentTab === 'episodes') {
            await loadDramasDropdowns();
            await loadEpisodesForDrama();
        } else if (currentTab === 'users') {
            await loadUsersAndBalances();
        }
    } catch (err) {
        console.error("Refresh failed:", err);
    } finally {
        triggerLoadingAnimation(100);
        if (refreshIcon) {
            setTimeout(() => { refreshIcon.classList.remove('animate-spin'); }, 500);
        }
    }
}

// 3. Data Fetching & Operations

// Tab: Dashboard (Statistics)
async function loadStats() {
    if (!supabaseClient) return;

    try {
        triggerLoadingAnimation(20);
        
        // Parallel fetching queries
        const [dramasRes, episodesRes, profilesRes, unlocksRes] = await Promise.all([
            supabaseClient.from('dramas').select('id', { count: 'exact' }),
            supabaseClient.from('episodes').select('id', { count: 'exact' }),
            supabaseClient.from('profiles').select('id', { count: 'exact' }),
            supabaseClient.from('unlocked_episodes').select('id', { count: 'exact' })
        ]);

        document.getElementById('stat-dramas').innerText = dramasRes.count ?? 0;
        document.getElementById('stat-episodes').innerText = episodesRes.count ?? 0;
        document.getElementById('stat-users').innerText = profilesRes.count ?? 0;
        document.getElementById('stat-unlocks').innerText = unlocksRes.count ?? 0;

        triggerLoadingAnimation(100);
    } catch (e) {
        console.error("Failed to load statistics: ", e);
    }
}

// Tab: Dramas (List, Add, Edit, Delete)
async function loadDramas() {
    if (!supabaseClient) return;
    
    try {
        triggerLoadingAnimation(40);
        const { data, error } = await supabaseClient
            .from('dramas')
            .select('*')
            .order('id', { ascending: true });

        if (error) throw error;

        cachedDramas = data || [];
        renderDramasTable(cachedDramas);
        triggerLoadingAnimation(100);
    } catch (e) {
        console.error("Load dramas error: ", e);
        alert("Lỗi tải danh sách phim: " + e.message);
    }
}

function renderDramasTable(dramas) {
    const tbody = document.getElementById('dramasTableBody');
    if (!tbody) return;

    if (dramas.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="px-6 py-12 text-center text-slate-500">
                    <i class="fa-solid fa-folder-open text-2xl mb-2 block"></i>
                    Chưa có bộ phim nào trên server. Vui lòng thêm phim mới!
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = dramas.map(d => `
        <tr class="hover:bg-[#16151B] transition-colors border-b border-[#1F1E24]/50">
            <td class="px-6 py-4 text-center font-mono font-bold text-slate-400">${d.id}</td>
            <td class="px-6 py-4">
                <img src="${d.cover_url || 'https://picsum.photos/100/150'}" alt="cover" class="w-12 h-18 object-cover rounded-lg border border-[#2E2B38] shadow-md">
            </td>
            <td class="px-6 py-4">
                <div class="font-extrabold text-white text-sm">${d.title}</div>
                <div class="text-slate-500 text-[11px] mt-1 line-clamp-1 max-w-sm">${d.description || 'Không có mô tả.'}</div>
            </td>
            <td class="px-6 py-4">
                <span class="px-2.5 py-1 bg-amber-500/10 border border-amber-500/20 text-amber-500 rounded text-[10px] font-bold uppercase tracking-wider">${d.genre || 'Phổ thông'}</span>
            </td>
            <td class="px-6 py-4 text-center font-mono font-semibold text-slate-300">${d.episodes_count} tập</td>
            <td class="px-6 py-4 text-center font-mono font-semibold text-rose-500">${d.views.toLocaleString()} lượt xem</td>
            <td class="px-6 py-4">
                <div class="flex items-center justify-center gap-2">
                    <button onclick="editDrama(${d.id})" class="p-2 bg-[#1A191E] hover:bg-amber-500 hover:text-black border border-[#2E2B38] rounded-lg text-amber-500 transition-all text-xs" title="Chỉnh sửa">
                        <i class="fa-solid fa-pen-to-square"></i>
                    </button>
                    <button onclick="deleteDrama(${d.id}, '${d.title}')" class="p-2 bg-[#1A191E] hover:bg-rose-500 hover:text-white border border-[#2E2B38] rounded-lg text-rose-500 transition-all text-xs" title="Xóa phim">
                        <i class="fa-solid fa-trash"></i>
                    </button>
                </div>
            </td>
        </tr>
    `).join('');
}

function filterDramasTable() {
    const query = document.getElementById('dramaSearch').value.toLowerCase().trim();
    if (!query) {
        renderDramasTable(cachedDramas);
        return;
    }

    const filtered = cachedDramas.filter(d => 
        d.title.toLowerCase().includes(query) || 
        (d.genre && d.genre.toLowerCase().includes(query)) ||
        (d.description && d.description.toLowerCase().includes(query))
    );
    renderDramasTable(filtered);
}

// Save/Update Drama
async function saveDrama(event) {
    event.preventDefault();
    if (!supabaseClient) return;

    const id = document.getElementById('formDramaId').value;
    const title = document.getElementById('formDramaTitle').value.trim();
    const genre = document.getElementById('formDramaGenre').value.trim();
    const cover_url = document.getElementById('formDramaCover').value.trim();
    const episodes_count = parseInt(document.getElementById('formDramaCount').value) || 10;
    const views = parseInt(document.getElementById('formDramaViews').value) || 0;
    const description = document.getElementById('formDramaDesc').value.trim();

    const payload = { title, genre, cover_url, episodes_count, views, description };

    try {
        triggerLoadingAnimation(50);
        let result;

        if (id) {
            // Edit
            result = await supabaseClient
                .from('dramas')
                .update(payload)
                .eq('id', id);
        } else {
            // Add
            result = await supabaseClient
                .from('dramas')
                .insert([payload]);
        }

        if (result.error) throw result.error;

        closeDramaModal();
        await loadDramas();
        alert("Lưu thông tin phim thành công!");
    } catch (e) {
        console.error("Save drama failed:", e);
        alert("Lỗi lưu phim: " + e.message);
    }
}

function editDrama(id) {
    const drama = cachedDramas.find(d => d.id === id);
    if (!drama) return;

    document.getElementById('formDramaId').value = drama.id;
    document.getElementById('formDramaTitle').value = drama.title;
    document.getElementById('formDramaGenre').value = drama.genre || '';
    document.getElementById('formDramaCover').value = drama.cover_url;
    document.getElementById('formDramaCount').value = drama.episodes_count;
    document.getElementById('formDramaViews').value = drama.views;
    document.getElementById('formDramaDesc').value = drama.description || '';

    document.getElementById('dramaModalTitle').innerText = "Chỉnh Sửa Bộ Phim #" + drama.id;
    openDramaModal(true); // Open edit style
}

async function deleteDrama(id, title) {
    if (!confirm(`Bạn có chắc chắn muốn XÓA phim "${title}" và toàn bộ tập phim liên quan? Hành động này không thể hoàn tác!`)) {
        return;
    }

    try {
        triggerLoadingAnimation(50);
        const { error } = await supabaseClient
            .from('dramas')
            .delete()
            .eq('id', id);

        if (error) throw error;

        await loadDramas();
        alert("Đã xóa bộ phim thành công!");
    } catch (e) {
        console.error("Delete failed:", e);
        alert("Lỗi khi xóa phim: " + e.message);
    }
}


// Tab: Episodes (Load dropdowns, Filter, Add/Edit/Delete episodes)
async function loadDramasDropdowns() {
    if (!supabaseClient) return;

    try {
        const { data, error } = await supabaseClient
            .from('dramas')
            .select('id, title')
            .order('title', { ascending: true });

        if (error) throw error;

        const dropdownFilter = document.getElementById('episodeDramaSelectFilter');
        const dropdownForm = document.getElementById('formEpisodeDramaId');

        // Render to filters
        const optionsHtml = data.map(d => `<option value="${d.id}">${d.title} (ID: ${d.id})</option>`).join('');
        
        dropdownFilter.innerHTML = `<option value="">-- Chọn Phim Bộ --</option>` + optionsHtml;
        dropdownForm.innerHTML = `<option value="">-- Chọn Phim Bộ --</option>` + optionsHtml;
    } catch (e) {
        console.error("Load dropdowns failed:", e);
    }
}

async function loadEpisodesForDrama() {
    if (!supabaseClient) return;

    const dramaId = document.getElementById('episodeDramaSelectFilter').value;
    const tbody = document.getElementById('episodesTableBody');
    if (!tbody) return;

    if (!dramaId) {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="px-6 py-12 text-center text-slate-500">
                    <i class="fa-solid fa-hand-pointer text-2xl mb-2 block animate-bounce"></i>
                    Vui lòng chọn một phim bộ ở danh sách bên trên để tải các tập phim.
                </td>
            </tr>
        `;
        return;
    }

    try {
        triggerLoadingAnimation(40);
        const { data, error } = await supabaseClient
            .from('episodes')
            .select('*')
            .eq('drama_id', dramaId)
            .order('episode_number', { ascending: true });

        if (error) throw error;

        cachedEpisodes = data || [];
        renderEpisodesTable(cachedEpisodes);
        triggerLoadingAnimation(100);
    } catch (e) {
        console.error("Failed to load episodes:", e);
        tbody.innerHTML = `<tr><td colspan="5" class="px-6 py-8 text-center text-rose-500">Lỗi tải tập phim: ${e.message}</td></tr>`;
    }
}

function renderEpisodesTable(episodes) {
    const tbody = document.getElementById('episodesTableBody');
    if (!tbody) return;

    if (episodes.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="px-6 py-12 text-center text-slate-500">
                    <i class="fa-solid fa-face-meh text-2xl mb-2 block"></i>
                    Phim bộ này chưa có tập phim nào được tạo. Nhấn "Thêm tập phim mới" để bắt đầu!
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = episodes.map(ep => `
        <tr class="hover:bg-[#16151B] transition-colors border-b border-[#1F1E24]/50">
            <td class="px-6 py-4 text-center font-mono font-extrabold text-amber-500">Tập ${ep.episode_number}</td>
            <td class="px-6 py-4">
                <div class="font-bold text-white text-sm">${ep.title || `Tập số ${ep.episode_number}`}</div>
            </td>
            <td class="px-6 py-4">
                <div class="font-mono text-slate-400 text-[11px] truncate max-w-md" title="${ep.video_url}">${ep.video_url}</div>
            </td>
            <td class="px-6 py-4 text-center">
                ${ep.is_free ? 
                    `<span class="px-2 py-0.5 bg-emerald-500/10 border border-emerald-500/20 text-emerald-500 rounded text-[10px] font-bold uppercase tracking-wider">Miễn Phí</span>` : 
                    `<span class="px-2 py-0.5 bg-rose-500/10 border border-rose-500/20 text-rose-500 rounded text-[10px] font-bold uppercase tracking-wider">Có Khóa</span>`
                }
            </td>
            <td class="px-6 py-4">
                <div class="flex items-center justify-center gap-2">
                    <button onclick="editEpisode(${ep.id})" class="p-2 bg-[#1A191E] hover:bg-amber-500 hover:text-black border border-[#2E2B38] rounded-lg text-amber-500 transition-all text-xs" title="Sửa tập">
                        <i class="fa-solid fa-pen-to-square"></i>
                    </button>
                    <button onclick="deleteEpisode(${ep.id})" class="p-2 bg-[#1A191E] hover:bg-rose-500 hover:text-white border border-[#2E2B38] rounded-lg text-rose-500 transition-all text-xs" title="Xóa tập">
                        <i class="fa-solid fa-trash"></i>
                    </button>
                </div>
            </td>
        </tr>
    `).join('');
}

// Save / Update Episode
async function saveEpisode(event) {
    event.preventDefault();
    if (!supabaseClient) return;

    const id = document.getElementById('formEpisodeId').value;
    const drama_id = parseInt(document.getElementById('formEpisodeDramaId').value);
    const episode_number = parseInt(document.getElementById('formEpisodeNumber').value);
    const title = document.getElementById('formEpisodeTitle').value.trim();
    const video_url = document.getElementById('formEpisodeVideoUrl').value.trim();
    const is_free = document.getElementById('freeYes').checked; // true / false

    const payload = { drama_id, episode_number, title, video_url, is_free };

    try {
        triggerLoadingAnimation(50);
        let result;

        if (id) {
            result = await supabaseClient
                .from('episodes')
                .update(payload)
                .eq('id', id);
        } else {
            result = await supabaseClient
                .from('episodes')
                .insert([payload]);
        }

        if (result.error) throw result.error;

        closeEpisodeModal();
        
        // Match filter selector to view changes immediately
        document.getElementById('episodeDramaSelectFilter').value = drama_id;
        await loadEpisodesForDrama();
        
        alert("Đã lưu tập phim thành công!");
    } catch (e) {
        console.error("Save episode failed:", e);
        alert("Lỗi lưu tập phim: " + e.message);
    }
}

function editEpisode(id) {
    const ep = cachedEpisodes.find(e => e.id === id);
    if (!ep) return;

    document.getElementById('formEpisodeId').value = ep.id;
    document.getElementById('formEpisodeDramaId').value = ep.drama_id;
    document.getElementById('formEpisodeNumber').value = ep.episode_number;
    document.getElementById('formEpisodeTitle').value = ep.title || '';
    document.getElementById('formEpisodeVideoUrl').value = ep.video_url;

    if (ep.is_free) {
        document.getElementById('freeYes').checked = true;
    } else {
        document.getElementById('freeNo').checked = true;
    }

    document.getElementById('episodeModalTitle').innerText = `Chỉnh Sửa Tập ${ep.episode_number}`;
    openEpisodeModal(true);
}

async function deleteEpisode(id) {
    if (!confirm("Bạn có chắc chắn muốn xóa tập phim này?")) return;

    try {
        triggerLoadingAnimation(50);
        const { error } = await supabaseClient
            .from('episodes')
            .delete()
            .eq('id', id);

        if (error) throw error;

        await loadEpisodesForDrama();
        alert("Đã xóa tập phim thành công!");
    } catch (e) {
        console.error("Delete episode error:", e);
        alert("Lỗi khi xóa tập: " + e.message);
    }
}


// Tab: Users & Balances (List, Adjust Coins, Spins, VIP status)
async function loadUsersAndBalances() {
    if (!supabaseClient) return;

    try {
        triggerLoadingAnimation(30);
        
        // Fetch profiles join balances
        const { data, error } = await supabaseClient
            .from('profiles')
            .select(`
                *,
                user_balances (
                    coins,
                    spins
                )
            `)
            .order('created_at', { ascending: false });

        if (error) throw error;

        cachedUsers = data || [];
        renderUsersTable(cachedUsers);
        triggerLoadingAnimation(100);
    } catch (e) {
        console.error("Load users failed:", e);
        alert("Lỗi tải thông tin tài khoản: " + e.message);
    }
}

function renderUsersTable(users) {
    const tbody = document.getElementById('usersTableBody');
    if (!tbody) return;

    if (users.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="px-6 py-12 text-center text-slate-500">
                    <i class="fa-solid fa-users-slash text-2xl mb-2 block"></i>
                    Không tìm thấy tài khoản người dùng nào đã đăng ký.
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = users.map(u => {
        const balance = u.user_balances || { coins: 50, spins: 3 };
        return `
            <tr class="hover:bg-[#16151B] transition-colors border-b border-[#1F1E24]/50">
                <td class="px-6 py-4">
                    <div class="flex items-center gap-3">
                        <span class="text-2xl bg-[#1D1C24] p-2 rounded-full border border-[#2E2B38]">${u.avatar_emoji || '🦊'}</span>
                        <div>
                            <div class="font-extrabold text-white text-sm">${u.nickname || 'Người dùng mới'}</div>
                            <div class="text-[10px] text-slate-400 font-mono mt-1">${u.id}</div>
                        </div>
                    </div>
                </td>
                <td class="px-6 py-4 font-mono text-slate-500 text-xs">${u.id}</td>
                <td class="px-6 py-4 font-semibold text-white">${u.phone_number || '<Chưa đăng ký>'}</td>
                <td class="px-6 py-4 text-center">
                    ${u.is_vip ? 
                        `<span class="px-2.5 py-1 bg-gradient-to-r from-amber-500 to-rose-500 text-black text-[10px] font-black rounded-lg shadow shadow-amber-500/10 uppercase tracking-tight">${u.vip_level || 'VIP'}</span>` : 
                        `<span class="px-2.5 py-1 bg-[#1A191E] border border-[#2E2B38] text-slate-400 text-[10px] font-bold rounded-lg uppercase">Thường</span>`
                    }
                </td>
                <td class="px-6 py-4 text-center font-mono font-black text-amber-500 text-sm">🪙 ${balance.coins}</td>
                <td class="px-6 py-4 text-center font-mono font-black text-rose-500 text-sm">🎯 ${balance.spins}</td>
                <td class="px-6 py-4">
                    <div class="flex items-center justify-center">
                        <button onclick="editUser('${u.id}')" class="flex items-center gap-1.5 px-3 py-1.5 bg-[#1A191E] hover:bg-amber-500 hover:text-black border border-[#2E2B38] rounded-lg text-amber-500 transition-all text-xs font-bold">
                            <i class="fa-solid fa-wallet"></i>
                            <span>Nạp Xu / Sửa VIP</span>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function filterUsersTable() {
    const query = document.getElementById('userSearch').value.toLowerCase().trim();
    if (!query) {
        renderUsersTable(cachedUsers);
        return;
    }

    const filtered = cachedUsers.filter(u => 
        u.id.toLowerCase().includes(query) || 
        u.nickname.toLowerCase().includes(query) ||
        (u.phone_number && u.phone_number.includes(query))
    );
    renderUsersTable(filtered);
}

function editUser(id) {
    const user = cachedUsers.find(u => u.id === id);
    if (!user) return;

    const balance = user.user_balances || { coins: 50, spins: 3 };

    document.getElementById('formUserId').value = user.id;
    document.getElementById('formUserAvatar').innerText = user.avatar_emoji || '🦊';
    document.getElementById('formUserNickname').innerText = user.nickname || 'Người dùng';
    document.getElementById('formUserDisplayId').innerText = "UID: " + user.id;
    document.getElementById('formUserCoins').value = balance.coins;
    document.getElementById('formUserSpins').value = balance.spins;
    document.getElementById('formUserVipLevel').value = user.vip_level || '';
    document.getElementById('formUserIsVip').checked = user.is_vip || false;

    openUserModal();
}

async function saveUserAdjustments(event) {
    event.preventDefault();
    if (!supabaseClient) return;

    const id = document.getElementById('formUserId').value;
    const coins = parseInt(document.getElementById('formUserCoins').value) || 0;
    const spins = parseInt(document.getElementById('formUserSpins').value) || 0;
    const vip_level = document.getElementById('formUserVipLevel').value.trim();
    const is_vip = document.getElementById('formUserIsVip').checked;

    try {
        triggerLoadingAnimation(50);
        
        // 1. Update Profile (VIP status and label)
        const profileUpdate = await supabaseClient
            .from('profiles')
            .update({ is_vip, vip_level })
            .eq('id', id);

        if (profileUpdate.error) throw profileUpdate.error;

        // 2. Upsert balance (Ensure row exists in user_balances table)
        const balanceUpdate = await supabaseClient
            .from('user_balances')
            .upsert({ user_id: id, coins, spins }, { onConflict: 'user_id' });

        if (balanceUpdate.error) throw balanceUpdate.error;

        closeUserModal();
        await loadUsersAndBalances();
        alert("Cập nhật số dư tài khoản người dùng thành công!");
    } catch (e) {
        console.error("Save balance adjustment failed:", e);
        alert("Lỗi cập nhật ví: " + e.message);
    }
}


// --- MODAL TOGGLES ---

function openConfigModal() {
    document.getElementById('inputUrl').value = getStorageItem('supabase_url') || '';
    document.getElementById('inputKey').value = getStorageItem('supabase_key') || '';
    document.getElementById('configModal').classList.remove('hidden');
}

function closeConfigModal() {
    document.getElementById('configModal').classList.add('hidden');
}

function openDramaModal(isEdit = false) {
    if (!isEdit) {
        // Clear form for add mode
        document.getElementById('dramaForm').reset();
        document.getElementById('formDramaId').value = '';
        document.getElementById('dramaModalTitle').innerText = "Thêm Phim Mới";
    }
    document.getElementById('dramaModal').classList.remove('hidden');
}

function closeDramaModal() {
    document.getElementById('dramaModal').classList.add('hidden');
}

function openEpisodeModal(isEdit = false) {
    if (!isEdit) {
        // Clear form for add mode
        document.getElementById('episodeForm').reset();
        document.getElementById('formEpisodeId').value = '';
        document.getElementById('episodeModalTitle').innerText = "Thêm Tập Phim";
        
        // auto select drama in dropdown if one is selected in filter
        const selectedFilterDramaId = document.getElementById('episodeDramaSelectFilter').value;
        if (selectedFilterDramaId) {
            document.getElementById('formEpisodeDramaId').value = selectedFilterDramaId;
        }
    }
    document.getElementById('episodeModal').classList.remove('hidden');
}

function closeEpisodeModal() {
    document.getElementById('episodeModal').classList.add('hidden');
}

function openUserModal() {
    document.getElementById('userModal').classList.remove('hidden');
}

function closeUserModal() {
    document.getElementById('userModal').classList.add('hidden');
}
