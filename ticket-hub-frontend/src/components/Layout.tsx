import { Avatar, Button, Dropdown, Header, Label } from "@heroui/react";
import { Outlet } from "react-router";

const Layout = () => {
  return (
    <>
      <Header className="header">
        <div className="header-inner">
          <a href="/" className="logo">
            <div className="logo-icon">🎫</div>
            <div className="logo-text">
              <span className="logo-name">TICKET</span>
              <span className="logo-accent">PRO</span>
            </div>
          </a>

          <nav className="header-nav" aria-label="Điều hướng chính">
            <a href="/manager" className="nav-link nav-link-active">
              Trang Chủ
            </a>
            <a href="/manager" className="nav-link">
              Sự kiện
            </a>
            <a href="/manager" className="nav-link">
              Vé hot
            </a>
            <a href="/manager" className="nav-link">
              Về chúng tôi 
            </a>
            <a href="/manager" className="nav-link">
              Liên hệ
            </a>
          </nav>

          <div className="header-actions">
            <Button
              className="notification-button"
              isIconOnly
              aria-label="Thông báo"
              variant="ghost"
            >
              <span aria-hidden="true">♢</span>
              <span className="notification-dot" aria-hidden="true" />
            </Button>

            <Dropdown>
                <Button className="Menu" variant="ghost">
                  <Avatar className="profile-avatar">
                    <Avatar.Fallback>MN</Avatar.Fallback>
                  </Avatar>
                  <span className="profile-copy">
                    <strong>Minh Nguyễn</strong>
                    <small>Quản lý</small>
                  </span>
                  <span className="chevron" aria-hidden="true">
                    ⌄
                  </span>
                </Button>
              <Dropdown.Popover>
                <Dropdown.Menu aria-label="Tùy chọn tài khoản">
                  <Dropdown.Item id="profile">
                    <Label>Hồ sơ cá nhân</Label>
                  </Dropdown.Item>
                  <Dropdown.Item id="settings">
                    <Label>Cài đặt</Label>
                  </Dropdown.Item>
                  <Dropdown.Item id="logout">
                    <Label>Đăng xuất</Label>
                  </Dropdown.Item>
                </Dropdown.Menu>
              </Dropdown.Popover>
            </Dropdown>
          </div>
        </div>
      </Header>
      <main>
        <Outlet />
      </main>
    </>
  );
};

export default Layout;
