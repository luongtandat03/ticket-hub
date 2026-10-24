import { useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { ShoppingCart, UserRound, Menu, X } from "lucide-react";

export default function Header() {
  const location = useLocation();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const isActive = (path: string) => {
    return location.pathname === path;
  };

  const navLinks = [
    { label: "Home", path: "/" },
    { label: "Concerts", path: "/concerts" },
    { label: "Singers", path: "/singers" },
  ];

  return (
    <header className="ticketer-header sticky top-0 z-50 w-full select-none">
      <div className="w-full max-w-7xl mx-auto px-4 sm:px-8 lg:px-12 h-[72px] sm:h-[76px] flex items-center justify-between">
        {/* Left Section: Navigation Links (Desktop) & Hamburger (Mobile) */}
        <div className="flex-1 flex items-center justify-start">
          {/* Mobile Menu Button */}
          <button
            type="button"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="md:hidden text-[#c5b8e8] hover:text-white p-2 -ml-2 rounded-lg transition-colors focus:outline-none"
            aria-label="Toggle navigation menu"
          >
            {mobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
          </button>

          {/* Desktop Nav Items */}
          <nav className="hidden md:flex items-center gap-7 lg:gap-9">
            {navLinks.map((item) => {
              const active = isActive(item.path);
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  className={`text-[15px] tracking-wide transition-colors duration-200 ${
                    active
                      ? "text-white font-semibold"
                      : "text-[#c3b7e4] hover:text-white font-medium"
                  }`}
                >
                  {item.label}
                </Link>
              );
            })}
          </nav>
        </div>

        {/* Center Section: TICKETER Brand Logo */}
        <div className="flex-shrink-0 flex items-center justify-center">
          <Link
            to="/"
            className="flex items-center group transition-transform duration-200 hover:scale-[1.03]"
            aria-label="TICKETER Trang chủ"
          >
            <img
              src="/logo.png"
              alt="TICKETER"
              className="h-6 sm:h-7 md:h-[29px] w-auto object-contain brightness-0 invert drop-shadow-[0_2px_8px_rgba(255,255,255,0.18)] select-none"
            />
          </Link>
        </div>

        {/* Right Section: Shopping Cart & Login/Register Button */}
        <div className="flex-1 flex items-center justify-end gap-5 sm:gap-7">
          {/* Shopping Cart Icon */}
          <Link
            to="/cart"
            className="text-white hover:text-white/80 transition-all duration-200 hover:scale-110 active:scale-95 p-1 flex items-center justify-center focus:outline-none"
            aria-label="Giỏ hàng"
          >
            <ShoppingCart size={23} strokeWidth={1.8} className="text-white" />
          </Link>

          {/* Login/Register Pill Box */}
          <div
            className="login-register-box flex items-center gap-2 px-4 py-1.5 sm:px-5 sm:py-2 rounded-full select-none"
            aria-label="Đăng nhập hoặc đăng ký"
          >
            <UserRound
              size={17}
              strokeWidth={2.2}
              className="text-[#6d28d9] flex-shrink-0"
            />
            <div className="flex items-center gap-1.5 text-[13.5px] sm:text-[14px] font-semibold text-[#6d28d9] tracking-tight whitespace-nowrap ">
              <Link
                to="/login"
                className="hover:underline underline-offset-4 decoration-2 decoration-[#6d28d9] transition-colors cursor-pointer hover:scale-[1.1]"
              >
                Login
              </Link>
              <span className="text-[#6d28d9]/60 select-none">/</span>
              <Link
                to="/register"
                className="hover:underline underline-offset-4 decoration-2 decoration-[#6d28d9] transition-colors cursor-pointer hover:scale-[1.1]"
              >
                Register
              </Link>
            </div>
          </div>
        </div>
      </div>

      {/* Mobile Drawer Menu */}
      {mobileMenuOpen && (
        <div className="md:hidden border-t border-white/10 bg-[#0c0824]/98 backdrop-blur-2xl px-6 py-5 shadow-2xl animate-in fade-in slide-in-from-top-2 duration-200">
          <nav className="flex flex-col gap-4">
            {navLinks.map((item) => (
              <Link
                key={item.path}
                to={item.path}
                onClick={() => setMobileMenuOpen(false)}
                className={`text-[15px] py-1.5 transition-colors ${
                  isActive(item.path)
                    ? "text-white font-semibold"
                    : "text-[#c3b7e4] hover:text-white font-medium"
                }`}
              >
                {item.label}
              </Link>
            ))}

            <div className="pt-4 mt-2 border-t border-white/10 flex items-center justify-between">
              <Link
                to="/cart"
                onClick={() => setMobileMenuOpen(false)}
                className="flex items-center gap-2 text-white text-sm py-1 hover:text-purple-200 transition-colors"
              >
                <ShoppingCart size={19} />
                <span>Giỏ hàng</span>
              </Link>

              <div className="login-register-box flex items-center gap-2 px-3.5 py-1.5 rounded-full">
                <UserRound
                  size={15}
                  strokeWidth={2.2}
                  className="text-[#6d28d9] flex-shrink-0"
                />
                <div className="flex items-center gap-1 text-xs font-semibold text-[#6d28d9]">
                  <Link
                    to="/login"
                    onClick={() => setMobileMenuOpen(false)}
                    className="hover:underline underline-offset-4 decoration-2 decoration-[#6d28d9] transition-colors cursor-pointer hover:scale-[1.1]"
                  >
                    Login
                  </Link>
                  <span className="text-[#6d28d9]/60 select-none">/</span>
                  <Link
                    to="/register"
                    onClick={() => setMobileMenuOpen(false)}
                    className="hover:underline underline-offset-4 decoration-2 decoration-[#6d28d9] transition-colors cursor-pointer hover:scale-[1.1]"
                  >
                    Register
                  </Link>
                </div>
              </div>
            </div>
          </nav>
        </div>
      )}
    </header>
  );
}
