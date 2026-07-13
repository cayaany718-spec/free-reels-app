# 🛠️ HƯỚNG DẪN CẤU HÌNH & TRIỂN KHAI HỆ THỐNG WEB ADMIN TRÊN HOSTING

Chào bạn! Dưới đây là tài liệu chi tiết (bằng tiếng Việt) hướng dẫn bạn cài đặt, kết nối cơ sở dữ liệu đám mây **Supabase** + **Firebase** và triển khai trang Web Admin này lên hosting riêng của bạn.

---

## 📋 TỔNG QUAN HỆ THỐNG

Trang Web Admin này được xây dựng dưới dạng **Single Page Application (SPA)** siêu nhẹ và bảo mật, sử dụng:
1. **HTML5 & CSS3 (Tailwind CSS CDN)** - Giao diện tối màu (Dark Slate) hiện đại, sang trọng, tương thích hoàn toàn với điện thoại, máy tính bảng và PC.
2. **Font Awesome 6** - Thư viện biểu tượng phong phú.
3. **Supabase JS Client SDK v2 (CDN)** - Kết nối thời gian thực đến PostgreSQL đám mây của bạn.

---

## ⚡ BƯỚC 1: KHỞI TẠO VÀ CẤU HÌNH SUPABASE (POSTGRESQL)

Supabase đóng vai trò là cơ sở dữ liệu chính lưu trữ các thông tin về Phim, Tập phim, Người dùng, Ví Xu và Lượt quay.

1. Truy cập [https://supabase.com/](https://supabase.com/) và đăng nhập/đăng ký tài khoản (Miễn phí).
2. Tạo một Dự án mới (**New Project**), đặt tên (ví dụ: `ShortFlix Database`) và lưu mật khẩu Database.
3. Chờ 1-2 phút cho đến khi hệ thống khởi tạo xong.
4. **Chạy tập lệnh SQL tạo bảng:**
   - Trong bảng điều khiển dự án Supabase, tìm mục **SQL Editor** ở thanh menu bên trái.
   - Bấm **New Query** (hoặc bấm biểu tượng dấu cộng `+`).
   - Mở tệp `/web-admin/database_schema.sql` trong dự án này, sao chép toàn bộ nội dung của nó.
   - Dán nội dung đó vào khung soạn thảo của Supabase SQL Editor và bấm nút **RUN** ở góc dưới cùng bên phải.
   - Bạn sẽ nhận được thông báo thành công. Toàn bộ các bảng `dramas`, `episodes`, `profiles`, `user_balances`, `unlocked_episodes` cùng với một số dữ liệu phim mẫu đã được tạo tự động!

---

## 🔥 BƯỚC 2: LIÊN KẾT VỚI WEB ADMIN & KIỂM TRA

1. Tìm thông tin khóa kết nối trong Supabase:
   - Vào mục **Project Settings** (biểu tượng bánh răng ở góc dưới bên trái) -> Chọn mục **API**.
   - Tìm mục **Project URL** (Ví dụ: `https://abcdxyzabcd.supabase.co`).
   - Tìm mục **Project API Keys** -> Tìm khóa `service_role` (Khóa bí mật có toàn quyền ghi đè bảo mật) hoặc `anon` key (Khóa công khai). 
     - *Khuyên dùng:* Sử dụng `service_role` cho trang Admin để bạn có toàn quyền chỉnh sửa trực tiếp số dư xu và thêm phim thoải mái.
2. Mở file `index.html` trong thư mục `/web-admin` trực tiếp trên máy tính hoặc trình duyệt.
3. Bấm vào nút màu vàng **"CẤU HÌNH KẾT NỐI"** ở góc dưới thanh Sidebar.
4. Dán **Supabase URL** và **API Key** của bạn vào ô trống -> bấm **LƯU CẤU HÌNH**.
5. Đèn chỉ báo trên Sidebar sẽ chuyển sang màu **XANH LÁ** báo hiệu kết nối thành công. Lúc này, bạn có thể:
   - Quản lý danh sách phim bộ, thêm phim mới, tải ảnh bìa.
   - Tạo tập phim mới, dán link phát video (định dạng MP4 hoặc HLS `.m3u8`).
   - Điều chỉnh số lượng Xu, lượt quay, bật/tắt VIP cho người dùng.

---

## 🌐 BƯỚC 3: TRIỂN KHAI LÊN HOSTING CỦA BẠN (CÓ SẴN)

Vì hệ thống Web Admin này được tối ưu hóa thành dạng tệp tĩnh thuần túy (Static Files gồm `index.html` và `app.js`), việc tải lên hosting vô cùng đơn giản và **an toàn**:

### Cách 1: Sử dụng CPanel / DirectAdmin (Phương pháp FTP truyền thống)
1. Nén thư mục `web-admin` (chứa `index.html`, `app.js` và `database_schema.sql`) thành một tệp `.zip`.
2. Đăng nhập vào bảng điều khiển Hosting (CPanel / DirectAdmin) của bạn.
3. Truy cập vào mục **File Manager** (Trình quản lý tệp) -> Truy cập thư mục `public_html`.
4. Tạo một thư mục con tên là `admin` (hoặc dán trực tiếp tại thư mục gốc tùy nhu cầu).
5. Tải tệp `.zip` lên và giải nén trực tiếp tại đây.
6. Truy cập đường dẫn của bạn (Ví dụ: `http://ten-mien-cua-ban.com/admin`) là trang Web quản trị đã hoạt động hoàn hảo!

### Cách 2: Triển khai Miễn phí lên Netlify / Vercel / GitHub Pages
Nếu bạn muốn tiết kiệm băng thông hosting chính, bạn có thể triển khai miễn phí 100% lên các nền tảng sau trong 30 giây:
- **Netlify:** Kéo thả trực tiếp thư mục `web-admin` vào mục "Drag & Drop" trên trang chủ Netlify.
- **GitHub Pages:** Đẩy thư mục này lên một repository công khai trên GitHub, kích hoạt tính năng GitHub Pages trong phần Settings.

---

## 📲 BƯỚC 4: HƯỚNG DẪN ĐỒNG BỘ ANDROID APP VỚI SUPABASE & FIREBASE

Để chuyển ứng dụng Android của bạn từ chế độ Offline (lưu Room SQLite trên thiết bị) sang Online đồng bộ toàn diện với Web Admin:

1. **Thêm Thư viện Supabase SDK vào Android project:**
   Trong tệp `gradle/libs.versions.toml` và `app/build.gradle.kts`, bạn chỉ cần khai báo thêm thư viện Supabase:
   ```toml
   # libs.versions.toml
   io-supabase-postgrest-kt = "2.1.0"
   ```
2. **Khai báo khóa API trong `BuildConfig`:**
   Dán Supabase URL và Anon Key vào Secrets Panel trong AI Studio hoặc tệp cấu hình để ứng dụng Android gọi kết nối trực tuyến.
3. **Thay đổi DramaRepository trong App:**
   Chuyển các hàm đọc danh sách phim `getDramas()`, `getEpisodes()`, `getUserBalance()` từ việc truy vấn Room SQLite sang gọi API tương ứng của Supabase, ví dụ:
   ```kotlin
   // Mẫu gọi đọc dữ liệu từ Supabase thay cho Local Room:
   val client = SupabaseClient(...)
   val dramas = client.from("dramas").select().decodeList<Drama>()
   ```
4. **Cấu hình Firebase (Tùy chọn):**
   - Đăng nhập vào [Firebase Console](https://console.firebase.google.com/), tạo một dự án mới và tải xuống tệp `google-services.json` đặt vào thư mục `/app`.
   - Sử dụng **Firebase Auth** để quản lý đăng nhập bằng Số điện thoại hoặc Google. ID đăng nhập từ Firebase Auth sẽ khớp 100% với khóa ngoại `id` trong bảng `profiles` của Supabase.
   - Sử dụng **Firebase Cloud Messaging (FCM)** để gửi thông báo đẩy đến người dùng ngay tại Web Admin khi bạn thêm phim mới.

---

Chúc bạn có một hệ thống quản trị và truyền dữ liệu phim chất lượng cao! Nếu cần hỗ trợ thêm, hãy nhắn tin ngay cho tôi nhé! 🦊
