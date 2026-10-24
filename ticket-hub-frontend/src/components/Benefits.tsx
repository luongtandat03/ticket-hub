import { ShieldCheck, MonitorSmartphone, Tag, Receipt } from "lucide-react";

const benefits = [
  {
    icon: <ShieldCheck size={36} className="text-[#4f3df5]" />,
    title: "Information Security",
    description: "We use the best encryption to protect your data.",
    bg: "bg-blue-50",
  },
  {
    icon: <MonitorSmartphone size={36} className="text-[#ff6b9d]" />,
    title: "Online Booking",
    description: "Book your tickets easily from anywhere, anytime.",
    bg: "bg-pink-50",
  },
  {
    icon: <Receipt size={36} className="text-[#ffb72b]" />,
    title: "Refundable Ticket",
    description: "Change of plans? We offer easy ticket refunds.",
    bg: "bg-orange-50",
  },
  {
    icon: <Tag size={36} className="text-[#00c897]" />,
    title: "Cheapest Ticket",
    description: "Get the best deals and cheapest prices guaranteed.",
    bg: "bg-green-50",
  },
];

export default function Benefits() {
  return (
    <section className="py-16 w-full max-w-7xl mx-auto px-4 sm:px-8 lg:px-12 text-center">
      <h2 className="text-[#161033] text-[32px] font-extrabold mb-2">
        Our Benefit
      </h2>
      <p className="text-[#554d63] text-[15px] mb-12">
        We provide you with the best experience and services.
      </p>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
        {benefits.map((benefit, index) => (
          <div key={index} className="flex flex-col items-center">
            <div
              className={`w-20 h-20 rounded-full flex items-center justify-center mb-5 ${benefit.bg}`}
            >
              {benefit.icon}
            </div>
            <h3 className="text-[#161033] font-bold text-[18px] mb-2">
              {benefit.title}
            </h3>
            <p className="text-[#554d63] text-[14px] leading-relaxed max-w-[200px]">
              {benefit.description}
            </p>
          </div>
        ))}
      </div>
    </section>
  );
}
