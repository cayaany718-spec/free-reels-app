# HƯỚNG DẪN VẬN HÀNH HỆ THỐNG TỰ ĐỘNG CẬP NHẬT TRONG ỨNG DỤNG (AUTO-UPDATE)

Hệ thống cập nhật tự động trong ứng dụng đã được thiết lập hoàn chỉnh. Khi bạn cập nhật phiên bản mới trong cơ sở dữ liệu Supabase, ứng dụng sẽ tự động phát hiện, hiển thị thông báo, tự tải file APK về thiết bị và mở trình cài đặt mà người dùng **không cần phải vào GitHub**.

---

## 1. Giải thích lỗi "404 Not Found" vừa gặp
Lỗi **"Lỗi tải xuống: Mã phản hồi từ máy chủ: 404 Not Found"** xuất hiện trên màn hình là do:
* Giá trị của khóa `latest_download_url` trong bảng `app_config` trên Supabase của bạn hiện đang để mặc định là `https://example.com/latest.apk`.
* URL này không tồn tại thật nên máy chủ trả về lỗi 404 (Không tìm thấy).
* **Cách khắc phục:** Bạn chỉ cần thay thế URL này bằng **đường dẫn tải trực tiếp file APK thật** từ mục Releases trên GitHub của bạn (như hướng dẫn ở Mục 2 dưới đây).

---

## 2. Cách lấy Link Tải Trực Tiếp (Direct Download Link) từ Release bạn vừa tạo

Bạn đã tạo thành công Release `V2.0` tại địa chỉ:  
👉 `https://github.com/cayaany718-spec/free-reels-app/releases/tag/V2.0`

Để lấy đường link trực tiếp cho vào Supabase, bạn làm theo các bước sau:

1. **Truy cập vào trang Release đó**: `https://github.com/cayaany718-spec/free-reels-app/releases/tag/V2.0` trên trình duyệt.
2. **Tìm đến mục "Assets"**: Thường nằm ở dưới cùng của bài viết Release đó. Bạn sẽ thấy file APK mà bạn đã upload lên (ví dụ: `app-debug.apk` hoặc tên file tương tự).
3. **Sao chép liên kết tải xuống**:
   * **Nếu dùng Máy tính**: Click chuột phải vào tên file `.apk` -> Chọn **Copy link address** (Sao chép địa chỉ liên kết).
   * **Nếu dùng Điện thoại**: Nhấn giữ lâu vào tên file `.apk` -> Chọn **Sao chép địa chỉ liên kết** (Copy link).
4. **Đường link đúng sẽ có định dạng như sau**:  
   `https://github.com/cayaany718-spec/free-reels-app/releases/download/V2.0/app-debug.apk` (hoặc tên file APK bạn đã tải lên).

---

## 3. Khi nào cần cập nhật file APK? (Giải đáp thắc mắc)

Bạn hỏi: *"Khi mình fix lỗi hay gì đó ko phải app người dùng tự chạy update hả? Trừ khi thêm tính năng mới thì mới cập nhật APK chứ?"*

Để hiểu rõ cách vận hành, chúng ta chia các thay đổi làm **2 loại**:

### LOẠI A: Thay đổi trực tiếp trên cơ sở dữ liệu Supabase (KHÔNG CẦN cập nhật APK)
Khi bạn chỉ sửa đổi dữ liệu hiển thị, ứng dụng sẽ **tự động cập nhật tức thì** cho tất cả người dùng ngay khi họ mở app, **hoàn toàn không cần bạn phải build APK mới hay bắt người dùng cập nhật**:
* Thêm phim mới, sửa phim, đổi tập phim, đổi link video.
* Thay đổi hình ảnh thumbnail, poster, tên phim, danh mục.
* Sửa các giá trị cấu hình đơn giản mà app đọc từ bảng `app_config` (ví dụ: thay đổi thông báo, bật/tắt quảng cáo, thay đổi link hỗ trợ).

### LOẠI B: Thay đổi mã nguồn ứng dụng (BẮT BUỘC phải build APK mới và nâng phiên bản)
Hệ điều hành Android chạy mã nguồn đã biên dịch. Do đó, bất cứ khi nào bạn thay đổi cấu trúc bên trong ứng dụng, bạn **phải build APK mới và cập nhật version** để kích hoạt tính năng tự động tải & cài đặt:
* **Sửa lỗi crash (lỗi văng ứng dụng), lỗi logic code Kotlin** (như sửa lỗi trình phát video, lỗi giao diện, lỗi tải dữ liệu).
* **Thay đổi thiết kế giao diện (UI/UX)**: đổi vị trí nút bấm, đổi màu sắc chủ đạo, thêm màn hình mới, thay đổi menu điều hướng.
* **Tích hợp tính năng mới**: thêm thanh toán, thêm hệ thống đăng nhập mới, tích hợp API mới, thay đổi thư viện bên thứ ba.

Mỗi khi có thay đổi thuộc **Loại B**, bạn chỉ cần build bản APK mới, up lên GitHub Release mới (ví dụ `V2.1`), lấy link APK dán vào Supabase và tăng `latest_version_code` lên `3` là xong!

---

## 4. Quy trình cấu hình Supabase cho bản cập nhật mới nhất

Sau khi có link tải trực tiếp ở Mục 2, hãy đăng nhập vào trang quản trị **Supabase** -> Vào mục **Table Editor** -> Chọn bảng `app_config` và cập nhật chính xác các dòng sau:

1. **`latest_version_code`**: Đổi thành số `2` (để khớp với `versionCode = 2` tôi vừa cập nhật trong ứng dụng).
2. **`latest_version_name`**: Đổi thành `2.0` (tên phiên bản hiển thị).
3. **`latest_download_url`**: Dán đường link trực tiếp đã copy ở Mục 2 (ví dụ: `https://github.com/cayaany718-spec/free-reels-app/releases/download/V2.0/app-debug.apk`).
4. **`latest_release_notes`**: Ghi nội dung thay đổi (Ví dụ: `Cập nhật hệ thống tự động tải và cài đặt phiên bản mới nhanh chóng!`).
5. **`latest_force_update`**:
   * Chọn `true` nếu bạn muốn bắt người dùng phải nâng cấp mới được dùng tiếp.
   * Chọn `false` nếu muốn cho phép họ bỏ qua.

---

## 5. Cơ chế hoạt động của ứng dụng khi có cập nhật

1. **Kiểm tra phiên bản**: Khi mở ứng dụng, app sẽ tự động lấy thông tin từ bảng `app_config` trên Supabase về so sánh. Nếu `latest_version_code` trên Supabase lớn hơn `versionCode` hiện tại của app trên máy, hộp thoại cập nhật sẽ xuất hiện.
2. **Tải xuống trực tiếp**: Khi người dùng nhấn **"Cập nhật ngay"**, ứng dụng sẽ tự động tải file APK chạy ngầm và hiển thị thanh tiến trình tải (`%` chạy từ 0% đến 100%) ngay trong hộp thoại để người dùng an tâm theo dõi.
3. **Mở trình cài đặt tự động**: 
   * Ngay khi tải xong 100%, ứng dụng sử dụng `FileProvider` (đã được cấu hình bảo mật trong `AndroidManifest.xml`) để tự động kích hoạt trình cài đặt mặc định của Android.
   * Người dùng chỉ cần nhấn **"Cập nhật"** (Update) trên màn hình hệ thống Android để hoàn tất việc nâng cấp.

---

## 6. Sửa lỗi: "Chưa cài đặt được ứng dụng do gói xung đột với một gói hiện có"

Khi bạn cài đặt bản cập nhật, nếu gặp thông báo lỗi **"Chưa cài đặt được ứng dụng do gói xung đột với một gói hiện có"** (như ảnh bạn chụp), đây là hành vi bảo mật hoàn toàn bình thường của hệ điều hành Android.

### Nguyên nhân:
* Ứng dụng hiện tại đang có trên điện thoại của bạn được cài đặt bằng một **chữ ký bảo mật (Keystore/Signing Key) khác** với file APK mới mà bạn vừa build trên GitHub Actions.
* Android yêu cầu tất cả các phiên bản cập nhật đè phải có **cùng một chữ ký bảo mật** để bảo vệ dữ liệu người dùng không bị ứng dụng giả mạo ghi đè lên.

### Cách khắc phục:
1. **Gỡ cài đặt (Uninstall) hoàn toàn** ứng dụng cũ hiện có trên điện thoại của bạn.
2. **Cài đặt lại ứng dụng** bằng file APK mới tải về từ GitHub Release `V2.0` (`app-debug.apk`).
3. **Kể từ lúc này trở đi**, tất cả các bản cập nhật tiếp theo (ví dụ bản `V2.1`, `V3.0`...) được tải thông qua cơ chế tự động cập nhật của ứng dụng sẽ **tự động cài đè lên nhau mượt mà** mà không bao giờ bị báo lỗi xung đột nữa, vì chúng được build chung từ một nguồn GitHub Actions của bạn và có cùng một chữ ký bảo mật!

---

## 7. Cấu hình tính năng Đăng nhập bằng Google thật (Real Google Sign-In)
 
Bạn thấy danh sách các tài khoản Google giả lập (như "bi tran", "chi dao"...) xuất hiện là do **ứng dụng chưa được cấu hình Google Web Client ID thật** từ tài khoản Google Cloud Console của bạn. 
 
Tôi vừa cập nhật mã nguồn để ứng dụng **tự động đọc Web Client ID thật từ Supabase**. Khi bạn cấu hình xong, ứng dụng sẽ gọi trực tiếp màn hình đăng nhập Google chính thức của hệ thống Android để lấy dữ liệu thật của người dùng!
 
### Quy trình 3 bước để kích hoạt Đăng nhập Google thật:
 
#### BƯỚC 1: Lấy mã dấu vân tay chữ ký SHA-1 của ứng dụng
Để Google cho phép ứng dụng của bạn đăng nhập, bạn cần đăng ký mã SHA-1 của file APK lên Google Cloud Console:
* Khi bạn build ứng dụng qua GitHub Actions (hoặc lấy từ Keystore ký ứng dụng của bạn), hãy tìm mã **SHA-1** của chứng chỉ ký APK.
 
#### BƯỚC 2: Tạo OAuth Client ID trên Google Cloud Console
1. Truy cập vào **[Google Cloud Console](https://console.cloud.google.com/)** -> Tạo một Project mới (hoặc chọn project có sẵn).
2. Tìm kiếm **APIs & Services** (API và Dịch vụ) -> Chọn **Credentials** (Thông tin xác thực).
3. Nhấp vào **Create Credentials** -> Chọn **OAuth client ID**:
   * **Application type**: Chọn **Web application** (Mặc dù chạy trên Android, thư viện bảo mật mới Credential Manager của Android yêu cầu sử dụng Client ID loại Web làm máy chủ xác thực).
   * **Name**: Đặt tên dễ nhớ (Ví dụ: `FreeReels Web Client`).
4. Nhấn **Create**. Một hộp thoại sẽ hiện lên cung cấp **Client ID** của bạn (có dạng kết thúc bằng `.apps.googleusercontent.com`, ví dụ: `12345678-abcde.apps.googleusercontent.com`). Hãy copy chuỗi này lại.
5. Tạo tiếp một Client ID khác với loại **Android**:
   * Nhập chính xác **Package Name** của ứng dụng: `com.moviebox.app` (như được khai báo trong `build.gradle.kts`).
   * Nhập chính xác **SHA-1 certificate fingerprint** lấy ở Bước 1.
 
#### BƯỚC 3: Cập nhật Client ID lên Supabase để kích hoạt tức thì
Sau khi có Web Client ID ở Bước 2, bạn không cần phải sửa code app nữa! Bạn chỉ cần thêm nó vào cơ sở dữ liệu để app tự động áp dụng:
1. Đăng nhập vào trang quản trị **Supabase** -> Vào mục **Table Editor** -> Chọn bảng `app_config`.
2. Tạo thêm một dòng mới (Insert Row) với thông tin:
   * **`key`**: `google_web_client_id` (viết chính xác chữ thường).
   * **`value`**: Dán mã **Web Client ID** bạn đã copy ở Bước 2 vào đây (Ví dụ: `12345678-abcde.apps.googleusercontent.com`).
3. Lưu lại (Save).
 
👉 **Kết quả:** Ngay lập tức, khi người dùng mở ứng dụng và nhấn **"Đăng nhập bằng Google"**, ứng dụng sẽ tự động tải cấu hình thật này từ cơ sở dữ liệu của bạn, bỏ qua danh sách mô phỏng, và hiển thị hộp thoại chọn tài khoản Google thật trên thiết bị của họ một cách bảo mật và an toàn!
 
---
 
Chúc bạn vận hành ứng dụng thành công và mượt mà! Nếu gặp bất kỳ khó khăn nào trong quá trình cấu hình, hãy nhắn cho tôi biết nhé.
