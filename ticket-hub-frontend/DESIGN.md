---
name: Ticket Hub
description: Design system cho nền tảng đặt và quản lý vé sự kiện Ticket Hub.
version: "1.0.0"
colors:
  primary: "#3337BF"
  secondary: "#1C6FE5"
  tertiary: "#F59E0B"
  neutral-100: "#F6F6F6"
  neutral-200: "#E7E7E7"
  neutral-300: "#CECECE"
  neutral-400: "#B6B6B6"
  neutral-500: "#9D9D9D"
  neutral-600: "#858585"
  neutral-700: "#5D5D5D"
  neutral-800: "#505050"
  neutral-900: "#242B35"
  surface: "#FFFFFF"
  black: "#222222"
  white: "#FFFFFF"
  text-main: "#222222"
  text-muted: "#5D5D5D"
  border: "#E7E7E7"
  success: "#1ABD8C"
  danger: "#E70404"
  warning: "#F2A853"
typography:
  h1:
    fontFamily: Inter
    fontSize: 2.25rem
    fontWeight: 700
    lineHeight: 2.5rem
  h2:
    fontFamily: Inter
    fontSize: 1.875rem
    fontWeight: 600
    lineHeight: 2.25rem
  body-md:
    fontFamily: Inter
    fontSize: 1rem
    fontWeight: 400
    lineHeight: 1.5rem
  body-sm:
    fontFamily: Inter
    fontSize: 0.875rem
    fontWeight: 400
    lineHeight: 1.25rem
  label-caps:
    fontFamily: Inter
    fontSize: 0.75rem
    fontWeight: 600
    letterSpacing: 0.05em
rounded:
  sm: 8px
  md: 12px
  lg: 16px
  full: 9999px
spacing:
  "4": 4px
  "8": 8px
  "16": 16px
  "24": 24px
  "28": 28px
  "32": 32px
  "40": 40px
  "48": 48px
  "56": 56px
  "64": 64px
  "72": 72px
  "80": 80px
  "88": 88px
  "96": 96px
  "104": 104px
  "124": 124px
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.white}"
    rounded: "{rounded.md}"
  button-primary-hover:
    backgroundColor: "#1F2173"
  card:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.lg}"
---

## Overview

Ticket Hub là nền tảng đặt vé hiện đại, nhanh chóng và đáng tin cậy. Thiết kế của dự án hướng đến sự rõ ràng, tối ưu hóa trải nghiệm người dùng (UX) để quá trình tìm kiếm sự kiện và thanh toán diễn ra mượt mà nhất.

## Colors

Bảng màu được thiết kế dựa trên sự tin cậy và kích thích hành động, theo đúng Style Guide:

- **Primary:** `#{colors.primary}` làm chủ đạo với các dải màu từ Tint (sáng) đến Shade (tối).
- **Secondary:** Các sắc thái màu xanh dương (blue) như `#{colors.secondary}`.
- **Neutral:** Dải màu xám đa dạng từ `#F6F6F6` đến `#242B35`, kết hợp với Đen (`#222222`) và Trắng (`#FFFFFF`).
- **Semantic Colors:**
  - Success (Thành công): `#1ABD8C` và các sắc độ xanh ngọc.
  - Danger (Lỗi/Xóa): `#E70404` và các sắc độ đỏ.
  - Warning (Cảnh báo): `#F2A853` và các sắc độ vàng cam.

## Typography

Sử dụng phông chữ **Inter** xuyên suốt toàn bộ ứng dụng để đảm bảo tính dễ đọc tuyệt đối trên mọi kích thước màn hình.

## Layout & Grids

Hệ thống lưới (Grid) chuẩn cho giao diện Desktop (1440px):

- **Columns:** 12
- **Type:** Center
- **Column Width:** 88px
- **Gutter:** 16px

## Spacing

Hệ thống khoảng cách (Spacing) bao gồm các mức:
4px, 8px, 16px, 24px, 28px, 32px, 40px, 48px, 56px, 64px, 72px, 80px, 88px, 96px, 104px, 124px.

## Shapes & Corner Radius

Sử dụng bo góc (Border Radius) chuẩn mực để tạo sự mềm mại vừa phải:

- **8px:** Cho các thành phần nhỏ (input, badge).
- **12px:** Cho các thành phần trung bình (button, modal nhỏ).
- **16px:** Cho các thành phần lớn (card, hình ảnh, container).

## Icons

- **Icon Pack:** Sử dụng bộ icon **Icon Park**.
- **Kích thước chuẩn:** 16px, 20px, 24px, 40px.

## Components

- **Button Primary:** Nổi bật với màu nền Primary (`#3337BF`), chữ trắng, bo góc 12px.
- **Card:** Nền trắng nguyên bản, bo góc 16px.

## Do's and Don'ts

- **Do:** Tuân thủ hệ thống lưới 12 cột và khoảng cách gutter 16px trên desktop.
- **Do:** Sử dụng đúng kích thước icon chuẩn để duy trì sự nhất quán.
- **Don't:** Không sử dụng các giá trị spacing nằm ngoài thang đo đã định nghĩa.
