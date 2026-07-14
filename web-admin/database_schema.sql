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
