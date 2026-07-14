# HƯỚNG DẪN VẬN HÀNH HỆ THỐNG TỰ ĐỘNG CẬP NHẬT TRONG ỨNG DỤNG (AUTO-UPDATE)

Hệ thống cập nhật tự động trong ứng dụng đã được thiết lập hoàn chỉnh. Khi bạn cập nhật phiên bản mới trong cơ sở dữ liệu Supabase, ứng dụng sẽ tự động phát hiện, hiển thị thông báo, tự tải file APK về thiết bị và mở trình cài đặt mà người dùng **không cần phải vào GitHub**.

---

## 1. Giải thích lỗi "404 Not Found" vừa gặp
Lỗi **"Lỗi tải xuống: Mã phản hồi từ máy chủ: 404 Not Found"** xuất hiện trên màn hình là do:
* Giá trị của khóa `latest_download_url` trong bảng `app_config` trên Supabase của bạn hiện đang để mặc định là `https://example.com/latest.apk`.
* URL này không tồn tại thật nên máy chủ trả về lỗi 404 (Không tìm thấy).
* **Cách khắc phục:** Bạn chỉ cần thay thế URL này bằng **đường dẫn tải trực tiếp file APK thật** (như hướng dẫn ở Mục 3 dưới đây).

---

## 2. Quy trình phát hành phiên bản mới (Workflow)

Mỗi khi bạn muốn cập nhật ứng dụng cho người dùng, hãy thực hiện theo 3 bước sau:

### BƯỚC 1: Tăng số phiên bản trong code (Android Studio / AI Studio)
Trước khi build APK mới, bạn cần tăng chỉ số phiên bản trong file `/app/build.gradle.kts`:
1. Mở file `/app/build.gradle.kts`.
2. Tìm đến phần `defaultConfig` và tăng:
   * `versionCode`: Tăng thêm 1 đơn vị (ví dụ từ `1` lên `2`, từ `120` lên `121`). Đây là số nguyên để hệ thống so sánh.
   * `versionName`: Đặt tên phiên bản dễ đọc (ví dụ từ `"1.0"` lên `"2.0"` hoặc `"1.2.1"`).
3. Build ra file APK mới (ví dụ: `app-release.apk`).

### BƯỚC 2: Upload APK lên một nơi lưu trữ công khai để lấy Link Tải Trực Tiếp (Direct Link)
Bạn cần một đường dẫn tải trực tiếp mà khi click vào hoặc khi code tải về sẽ tải ngay file `.apk`.
Một số cách phổ biến và miễn phí:

* **Cách 1: Sử dụng GitHub Releases (Khuyên dùng - Ổn định nhất)**
  1. Từ trang GitHub Actions trong ảnh của bạn (hoặc truy cập trực tiếp repo: `https://github.com/cayaany718-spec/free-reels-app`):
     * Cuộn xuống phần **Artifacts** ở dưới cùng trang build thành công (như ảnh chụp của bạn).
     * Nhấn vào biểu tượng nút tải xuống ở dòng **`app-debug`** (bên phải dung lượng 26.7 MB).
     * Bạn sẽ tải xuống một file `.zip`, hãy giải nén ra để lấy file `app-debug.apk`.
  2. Vào trang chủ của kho lưu trữ: `https://github.com/cayaany718-spec/free-reels-app`.
  3. Chọn mục **Releases** ở cột bên phải -> Chọn **Create a new release** (hoặc truy cập nhanh qua link: `https://github.com/cayaany718-spec/free-reels-app/releases/new`).
  4. Nhập phiên bản (ví dụ `v2.0`), viết tiêu đề và mô tả thay đổi.
  5. Kéo thả file `app-debug.apk` đã giải nén vào ô đính kèm (Assets).
  6. Nhấn **Publish release** để xuất bản.
  7. Sau khi xuất bản, click chuột phải vào file APK trong mục Assets của bản Release đó -> Chọn **Copy link address** (Sao chép địa chỉ liên kết).
  * *Ví dụ link tải trực tiếp thật của bạn sẽ có dạng:* `https://github.com/cayaany718-spec/free-reels-app/releases/download/v2.0/app-debug.apk`

* **Cách 2: Sử dụng các dịch vụ lưu trữ trực tiếp khác**
  * Bạn có thể upload lên máy chủ của riêng bạn, hoặc các dịch vụ cho phép lấy link tải trực tiếp (Direct Link) kết thúc bằng đuôi `.apk`.
  * *Lưu ý:* Không dùng Google Drive thông thường vì link tải của Google Drive yêu cầu xác nhận virus hoặc chuyển hướng, code sẽ không tải trực tiếp được.

### BƯỚC 3: Cập nhật thông số lên bảng `app_config` trên Supabase
Sau khi có link tải trực tiếp, hãy đăng nhập vào trang quản trị **Supabase** -> Chọn bảng `app_config` và cập nhật các dòng sau:

1. **`latest_version_code`**: Thay đổi thành số `versionCode` mới (Ví dụ: `2`).
2. **`latest_version_name`**: Thay đổi thành tên phiên bản mới (Ví dụ: `2.0`).
3. **`latest_download_url`**: Dán link tải trực tiếp APK đã lấy ở Bước 2 vào đây (Link phải kết thúc bằng `.apk`, ví dụ: `https://github.com/.../app-release.apk`).
4. **`latest_release_notes`**: Viết mô tả các tính năng mới hoặc sửa lỗi để người dùng đọc (Ví dụ: `Sửa lỗi xem phim mượt hơn, cập nhật tự động cực nhanh!`).
5. **`latest_force_update`**: 
   * Nhập `true` nếu muốn **bắt buộc cập nhật** (người dùng không thể tắt hộp thoại thông báo đi, buộc phải cập nhật mới được dùng app).
   * Nhập `false` nếu cập nhật không bắt buộc (người dùng có thể nhấn "Để sau" để tiếp tục dùng bản cũ).

---

## 3. Cơ chế hoạt động của ứng dụng khi có cập nhật

1. **Kiểm tra phiên bản**: Khi mở ứng dụng, app sẽ tự động lấy thông tin từ bảng `app_config` trên Supabase về so sánh. Nếu `latest_version_code` trên Supabase lớn hơn `versionCode` hiện tại của app trên máy, hộp thoại cập nhật sẽ xuất hiện.
2. **Tải xuống trực tiếp**: Khi người dùng nhấn **"Cập nhật ngay"**, ứng dụng sẽ tự động tải file APK chạy ngầm và hiển thị thanh tiến trình tải (`%` chạy từ 0% đến 100%) ngay trong hộp thoại để người dùng an tâm theo dõi.
3. **Mở trình cài đặt tự động**: 
   * Ngay khi tải xong 100%, ứng dụng sử dụng `FileProvider` (đã được cấu hình bảo mật trong `AndroidManifest.xml`) để tự động kích hoạt trình cài đặt mặc định của Android.
   * Người dùng chỉ cần nhấn **"Cập nhật"** (Update) trên màn hình hệ thống Android để hoàn tất việc nâng cấp.

---

Chúc bạn vận hành ứng dụng thành công và mượt mà! Nếu gặp bất kỳ khó khăn nào trong quá trình cấu hình, hãy nhắn cho tôi biết nhé.
