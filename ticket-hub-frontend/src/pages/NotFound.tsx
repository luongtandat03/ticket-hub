export default function NotFound() {
  return (
    <div className="not-found-page">
      <div className="not-found-content">
        <h1 className="not-found-title">404</h1>
        <h2 className="not-found-subtitle">Trang không tồn tại</h2>
        <p className="not-found-description">
          Xin lỗi, trang bạn đang tìm kiếm không tồn tại hoặc đã bị di chuyển.
        </p>
        <a href="/" className="not-found-button">
          Quay về trang chủ
        </a>
      </div>
    </div>
  );
}