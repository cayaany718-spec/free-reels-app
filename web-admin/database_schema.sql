-- --- SHORTFLIX SUPABASE DATABASE SCHEMA ---
-- Copy and run this script in the Supabase SQL Editor to create your database tables.

-- 1. Profiles Table (Stores user profile information)
CREATE TABLE IF NOT EXISTS public.profiles (
    id TEXT PRIMARY KEY, -- Can be Firebase UID or Supabase Auth UID
    nickname TEXT NOT NULL,
    avatar_emoji TEXT DEFAULT '🦊',
    phone_number TEXT,
    vip_level TEXT DEFAULT 'THÀNH VIÊN THƯỜNG',
    is_vip BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Enable Row Level Security (RLS) or leave it disabled for development
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Allow public read access to profiles" ON public.profiles FOR SELECT USING (true);
CREATE POLICY "Allow individuals/admin to manage profiles" ON public.profiles FOR ALL USING (true);

-- 2. User Balances Table (Tracks coins and spin quotas)
CREATE TABLE IF NOT EXISTS public.user_balances (
    user_id TEXT PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
    coins INTEGER DEFAULT 50 NOT NULL,
    spins INTEGER DEFAULT 3 NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

ALTER TABLE public.user_balances ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Allow public read access to balances" ON public.user_balances FOR SELECT USING (true);
CREATE POLICY "Allow users/admin to manage balances" ON public.user_balances FOR ALL USING (true);

-- 3. Dramas Table (Stores movie series metadata)
CREATE TABLE IF NOT EXISTS public.dramas (
    id SERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT,
    cover_url TEXT,
    genre TEXT DEFAULT 'Hiện đại',
    episodes_count INTEGER DEFAULT 10 NOT NULL,
    views INTEGER DEFAULT 0 NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

ALTER TABLE public.dramas ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Allow public read access to dramas" ON public.dramas FOR SELECT USING (true);
CREATE POLICY "Allow admin to manage dramas" ON public.dramas FOR ALL USING (true);

-- 4. Episodes Table (Stores episodes of each drama series)
CREATE TABLE IF NOT EXISTS public.episodes (
    id SERIAL PRIMARY KEY,
    drama_id INTEGER REFERENCES public.dramas(id) ON DELETE CASCADE NOT NULL,
    episode_number INTEGER NOT NULL,
    title TEXT,
    video_url TEXT NOT NULL,
    is_free BOOLEAN DEFAULT false NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    CONSTRAINT unique_drama_episode UNIQUE (drama_id, episode_number)
);

ALTER TABLE public.episodes ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Allow public read access to episodes" ON public.episodes FOR SELECT USING (true);
CREATE POLICY "Allow admin to manage episodes" ON public.episodes FOR ALL USING (true);

-- 5. Unlocked Episodes Table (Tracks which user unlocked which episode)
CREATE TABLE IF NOT EXISTS public.unlocked_episodes (
    id SERIAL PRIMARY KEY,
    user_id TEXT REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    drama_id INTEGER REFERENCES public.dramas(id) ON DELETE CASCADE NOT NULL,
    episode_number INTEGER NOT NULL,
    unlocked_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    CONSTRAINT unique_user_drama_episode UNIQUE (user_id, drama_id, episode_number)
);

ALTER TABLE public.unlocked_episodes ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Allow public read access to unlocks" ON public.unlocked_episodes FOR SELECT USING (true);
CREATE POLICY "Allow users/admin to manage unlocks" ON public.unlocked_episodes FOR ALL USING (true);


-- 6. App Config Table (Stores dynamic application configuration like latest version)
CREATE TABLE IF NOT EXISTS public.app_config (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

ALTER TABLE public.app_config ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Allow public read access to app_config" ON public.app_config FOR SELECT USING (true);
CREATE POLICY "Allow admin to manage app_config" ON public.app_config FOR ALL USING (true);

INSERT INTO public.app_config (key, value)
VALUES 
('latest_version_code', '1'),
('latest_version_name', '1.0'),
('latest_download_url', 'https://example.com/latest.apk'),
('latest_release_notes', 'Phiên bản mới nhất!'),
('latest_force_update', 'false')
ON CONFLICT (key) DO NOTHING;

INSERT INTO public.profiles (id, nickname, avatar_emoji, phone_number, vip_level, is_vip)
VALUES 
('FR_11111', 'Lâm Phong', '🦊', '0912345678', 'THÀNH VIÊN VIP PREMIUM', true),
('FR_22222', 'Kiều Vy', '🐱', '0987654321', 'THÀNH VIÊN THƯỜNG', false)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.user_balances (user_id, coins, spins)
VALUES 
('FR_11111', 1500, 10),
('FR_22222', 40, 2)
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO public.dramas (id, title, description, cover_url, genre, episodes_count, views)
VALUES 
(1, 'Hợp Đồng Hôn Nhân Của Tổng Tài', 'Bộ phim truyền hình lãng mạn kể về bản hợp đồng hôn nhân đầy bất ngờ giữa chàng tổng tài lạnh lùng và cô gái nghèo cá tính.', 'https://picsum.photos/400/600?random=1', 'Ngôn Tình', 5, 1250),
(2, 'Vương Gia Trở Lại', 'Màn trả thù gay cấn của Vương gia sau 10 năm ẩn tính chôn mình tìm kiếm kẻ hãm hại gia tộc năm xưa.', 'https://picsum.photos/400/600?random=2', 'Cổ Trang', 6, 850)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.episodes (drama_id, episode_number, title, video_url, is_free)
VALUES 
(1, 1, 'Tập 1: Định Mệnh Gặp Gỡ', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4', true),
(1, 2, 'Tập 2: Bản Hợp Đồng 10 Tỷ', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4', true),
(1, 3, 'Tập 3: Về Chung Một Nhà', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4', true),
(1, 4, 'Tập 4: Thử Thách Đầu Tiên', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4', false),
(1, 5, 'Tập 5: Trái Tim Rung Động', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4', false),

(2, 1, 'Tập 1: Ẩn Danh Sơn Lâm', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4', true),
(2, 2, 'Tập 2: Trở Lại Kinh Thành', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4', true),
(2, 3, 'Tập 3: Đối Mặt Kẻ Thù', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4', true),
(2, 4, 'Tập 4: Thu Phục Lòng Dân', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4', false),
(2, 5, 'Tập 5: Trận Chiến Sinh Tử', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4', false),
(2, 6, 'Tập 6: Vương Quyền Đăng Cơ', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4', false)
ON CONFLICT (drama_id, episode_number) DO NOTHING;
