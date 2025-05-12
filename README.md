Mô tả bài toán:
- 1, Chức năng đăng nhập (sử dụng email và mật khẩu để đăng nhập thì có thể tự tạo).
- 2, Chức năng đăng ký (đăng ký chỉ cần nhập tên người dùng, email, mật khẩu và xác nhận mật khẩu), sau đó sẽ gửi về email để xác minh, tại email đó ấn vào đường link sẽ xác thực thành công và có thể quay trở lại để đăng nhập. 
- 3, Ở màn hình dashboard sẽ có thông tin về sản phẩm (Layout mô tả: thông tin về sản phẩm, có chức năng tìm kiếm (tìm kiếm theo tên, hoặc theo giá) , các offical brand hay các popular brand, thanh điều hướng có explorer, wishlist ,cart,profile). Khi bấm vào 1 sản phẩm sẽ hiển thị chi tiết sản phẩm : tên sản phẩm, đánh giá mấy sao, size là gì , bình luận (các người dùng có thể bình luận và bình luận sẽ được liệt kê ở trang web, đối với các người dùng khác tất nhiên khi vào sản phẩm đó có thể thấy sản phẩm), mô tả sản phẩm, mục sold (đã bán bao nhiêu còn lại bao nhiêu), đánh giá sản phẩm thì khi đặt mua xong có thể đánh giá sản phẩm
 + Xây dựng profile đầu tiên, tại đây hệ thống sẽ bao gồm các nút Thông tin cá nhân, lịch sử đặt hàng, Thống kê, đổi mật khẩu ,đăng xuất(xong rồi trong từng nút này ta mới vào layout của từng chức năng để thực hiện chức năng, đối với đăng xuất sẽ có hiển thị xác nhận đăng xuất).
   . Đối với thông tin cá nhân thì sẽ có: Tên người dùng (có thể sửa),email (không thể sửa), số điện thoại, địa chỉ giao hàng (có thể thêm địa chỉ giao hàng), ngày sinh, giới tính
   . Đối với lịch sử đặt hàng sẽ có: thông tin về lịch sử đặt hàng bao gồm các mặt hàng giá tiền từng mặt hàng , tổng bao nhiêu, có mã giảm giá không.
   .  Đối với thống kê  sẽ mô tả chi tiết ở phần dưới
   . Đối với đổi mật khẩu: bao gồm việc nhập mật khẩu cũ và nhập mật khẩu mới, sau khi đổi mật khẩu mới sẽ có thông báo ở mail người dùng.
- 4, Đối với chức năng cart thì có thể hiển thị giỏ hàng hiện tại đang thêm bớt cái gì như nào (layout mô tả khái quát sẽ có từng sản phẩm theo dòng, số lượng ,dưới có ô nhập mã giảm giá (nếu có có thể nhập), subtotal,fee delivery, totaltax, total). Khi bấm đặt hàng có thể hiển thị thông tin hóa đơn đã đặt hàng và các thông tin chi tiết đi kèm.
- 5, Đối với wishlist sẽ hiển thị các mục mà mình có ấn thích sản phẩm.
- 6, Đối với phía admin sẽ có thêm chức năng thống kê riêng.
“

Mô tả chi tiết chức năng thống kê:
 + 1 ô chọn ngày tháng năm bắt đầu, 1 ô chọn ngày tháng năm kết thúc (hiển thị 1 ô bé để chọn thôi không cần to toàn bộ màn hình đâu)
+ 1 ô chọn nhiều nhất hoặc ít nhất
+ 1 ô chọn số lượng hiển thị (chỉ cố một số thống kê mới có thể sử dụng, nếu không sử dụng được sẽ  không thể chọn nghĩa là cấm người dùng chọn luôn, người dùng không thể chọn ô này)
+ Thống kê số lượng đơn hàng: chọn tiêu chí là “Số lượng đơn hàng”, kết hợp với ngày tháng năm bắt đầu và ngày tháng năm kết thúc, chiều ngang biểu đồ sẽ là ngày (không cần hiển thị năm và tháng) do ta đã chọn từ ngày bao nhiêu đến ngày bao nhiêu theo định dạng (DD/MM/YYYY), chiều dọc sẽ là số lượng đơn hàng, tại mỗi điểm trên đồ thị, chỉ cần hiển thị ngày thôi, , chú ý có thể kết hợp với ô chọn nhiều nhất hoặc ít nhất để hiển thị
+ Thống kê doanh thu: chọn tiêu chí là Doanh thu”, kết hợp với ngày tháng năm bắt đầu và ngày tháng năm kết thúc. Vì doanh thu của ta có đơn vị là “đồng” nên là chiều dọc của đồ thị sẽ là doanh thu tổng trong ngày đấy, còn chiều ngang sẽ là ngày, tại mỗi điểm trên đồ thị, chỉ cần hiển thị ngày thôi, chú ý có thể kết hợp với ô chọn nhiều nhất hoặc ít nhất để hiển thị
+ Thống kê số lượng tồn kho: chọn tiêu chí hàng tồn kho, chiều ngang sẽ là số sản phẩm của cửa hàng (có thể chọn số lượng hiển thị), chiều dọc là số lượng tồn kho của hàng tồn kho đó, chú ý có thể kết hợp với ô chọn nhiều nhất hoặc ít nhất để hiển thị.
+ Thống kê số lượng sản phẩm bán chạy : kết hợp với ô chọn ngày tháng năm bắt đầu và kết thúc, kết hợp với ô chọn nhiều nhất hoặc ít nhất để hiển thị, nhiều nhất ở đây sẽ liệt kê từ cao đến thấp các mặt hàng bán chạy và ít nhất thì ngược lại. trục ngang sẽ là tên sản phẩm (có thể chọn số lượng hiển thị), trục dọc là số lượng sản phẩm.
+ Thống kê số lượng người dùng đăng ký mới: chọn tiêu chí “người dùng đăng ký”chọn ngày tháng năm bắt đầu và kết thúc để thống kê, cột ngang hiển thị ngày (bỏ tháng và năm), cột dọc hiển thị số lượng đăng ký.
+ Thống kê sản phẩm với số lượng bình luận: chọn tiêu chí “số lượng bình luận”, và chọn ô nhiều nhất hoặc ít nhất, trục dọc sẽ là tên sản phẩm, trục ngang hiển thị số sản phẩm, có thể chọn ô số lượng hiển thị.
 + Thống kê sản phẩm được yêu thích nhiều nhất: nghĩa là sản phẩm được cho vào trong wishlist, chọn tiêu chí “sản phẩm được yêu thích”,  chọn nhiều nhất hoặc ít nhất, có thể chọn số lượng hiển thị sản phẩm, chiều ngang là tên sản phẩm, cột dọc sẽ là số lượng tương ứng.
+ Thống kê số tiền bán được theo từng sản phẩm: chiều ngang là tên sản phẩm, chiều dọc là số tiền kiếm được từ sản phẩm, có thể chọn tiêu chí nhiều nhất hoặc ít nhất, có thể chọn số lượng hiển thị.


 
 


