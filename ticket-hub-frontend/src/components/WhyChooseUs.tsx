import { ShieldCheck, Zap, Headphones, Award, CreditCard, Gift } from 'lucide-react';
import type { Feature } from '../types';

const features: Feature[] = [
  {
    icon: <ShieldCheck size={32} />,
    title: 'Vé chính hãng 100%',
    description: 'Cam kết mọi vé đều được cung cấp từ đơn vị tổ chức chính thức',
  },
  {
    icon: <Zap size={32} />,
    title: 'Đặt vé nhanh chóng',
    description: 'Chỉ cần 3 bước đơn giản, bạn đã có vé trong tay',
  },
  {
    icon: <CreditCard size={32} />,
    title: 'Thanh toán an toàn',
    description: 'Hỗ trợ đa dạng phương thức thanh toán, bảo mật tuyệt đối',
  },
  {
    icon: <Headphones size={32} />,
    title: 'Hỗ trợ 24/7',
    description: 'Đội ngũ chăm sóc khách hàng sẵn sàng hỗ trợ mọi lúc',
  },
  {
    icon: <Award size={32} />,
    title: 'Uy tín hàng đầu',
    description: 'Được hàng triệu người dùng tin tưởng và lựa chọn',
  },
  {
    icon: <Gift size={32} />,
    title: 'Ưu đãi hấp dẫn',
    description: 'Nhiều chương trình khuyến mãi và tích điểm đổi quà',
  },
];

export default function WhyChooseUs() {
  return (
    <section className="why-choose-us-section">
      <div className="section-header">
        <h2 className="section-title">Vì sao chọn TicketPro?</h2>
        <p className="section-subtitle">
          Chúng tôi mang đến trải nghiệm đặt vé tốt nhất cho bạn
        </p>
      </div>

      <div className="features-grid">
        {features.map((feature, index) => (
          <div key={index} className="feature-card">
            <div className="feature-icon">{feature.icon}</div>
            <h3 className="feature-title">{feature.title}</h3>
            <p className="feature-description">{feature.description}</p>
          </div>
        ))}
      </div>
    </section>
  );
}
