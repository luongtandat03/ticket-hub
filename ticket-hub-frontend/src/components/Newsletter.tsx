import { Mail, Send } from 'lucide-react';
import { useState } from 'react';

export default function Newsletter() {
  const [email, setEmail] = useState('');
  const [isSubscribed, setIsSubscribed] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (email) {
      console.log('Subscribe email:', email);
      setIsSubscribed(true);
      setEmail('');
      
      // Reset after 3 seconds
      setTimeout(() => {
        setIsSubscribed(false);
      }, 3000);
    }
  };

  return (
    <section className="newsletter-section">
      <div className="newsletter-content">
        <div className="newsletter-icon">
          <Mail size={48} />
        </div>
        
        <h2 className="newsletter-title">Đăng ký nhận tin</h2>
        <p className="newsletter-description">
          Nhận thông tin về các sự kiện mới nhất và ưu đãi độc quyền
        </p>

        {isSubscribed ? (
          <div className="newsletter-success">
            ✓ Đăng ký thành công! Cảm ơn bạn đã đăng ký.
          </div>
        ) : (
          <form className="newsletter-form" onSubmit={handleSubmit}>
            <div className="newsletter-input-wrapper">
              <Mail className="input-icon" size={20} />
              <input
                type="email"
                placeholder="Nhập địa chỉ email của bạn"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="newsletter-input"
                required
              />
            </div>
            <button type="submit" className="newsletter-button">
              <Send size={20} />
              Đăng ký
            </button>
          </form>
        )}

        <p className="newsletter-privacy">
          Chúng tôi tôn trọng quyền riêng tư của bạn. Thông tin sẽ được bảo mật.
        </p>
      </div>
    </section>
  );
}
