import { MapPin, ChevronLeft, ChevronRight } from "lucide-react";
import { Link } from "react-router-dom";
import type { Event } from "../types";

const featuredEvents: Event[] = [
  {
    id: "1",
    title: "Shawn Mendes",
    category: "Concert",
    date: "2023-05-15",
    location: "Los Angeles, CA",
    price: 150,
    image: "/images/event1.jpg",
    availableTickets: 1200,
    isHot: true,
  },
  {
    id: "2",
    title: "Taylor Swift",
    category: "Concert",
    date: "2023-06-20",
    location: "New York, NY",
    price: 300,
    image: "/images/event2.jpg",
    availableTickets: 450,
    isHot: true,
  },
  {
    id: "3",
    title: "Ariana Grande",
    category: "Concert",
    date: "2023-07-10",
    location: "London, UK",
    price: 200,
    image: "/images/event3.jpg",
    availableTickets: 800,
    isHot: false,
  },
  {
    id: "4",
    title: "Adele",
    category: "Concert",
    date: "2023-08-05",
    location: "Paris, FR",
    price: 250,
    image: "/images/event4.jpg",
    availableTickets: 950,
    isHot: true,
  },
];

export default function FeaturedEvents() {
  const formatMonth = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString("en-US", { month: "short" }).toUpperCase();
  };
  const formatDay = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString("en-US", { day: "2-digit" });
  };

  return (
    <section className="py-16 w-full max-w-7xl mx-auto px-4 sm:px-8 lg:px-12 text-center">
      <h2 className="text-[#161033] text-[32px] font-extrabold mb-2">
        Time is Running Out
      </h2>
      <p className="text-[#554d63] text-[15px] mb-6">
        Don't miss out these hot upcoming concerts.
      </p>

      <div className="flex justify-center gap-2 mb-8">
        <button className="w-8 h-8 rounded-full border border-gray-300 flex items-center justify-center hover:bg-gray-100 transition-colors">
          <ChevronLeft size={16} />
        </button>
        <button className="w-8 h-8 rounded-full border border-gray-300 flex items-center justify-center hover:bg-gray-100 transition-colors">
          <ChevronRight size={16} />
        </button>
      </div>

      <div className="flex justify-center gap-6 overflow-x-auto pb-6 hide-scrollbar">
        {featuredEvents.map((event) => (
          <div
            key={event.id}
            className="flex-none w-[260px] sm:w-[280px] bg-white rounded-[20px] overflow-hidden flex flex-col p-[2px]"
            style={{
              background: "linear-gradient(180deg, #5142f5 0%, #ffffff 100%)",
              boxShadow: "0px 10px 30px rgba(0, 0, 0, 0.08)"
            }}
          >
            <div className="bg-white rounded-[18px] flex flex-col h-full overflow-hidden">
              <div className="relative h-[240px] w-full overflow-hidden bg-gray-200">
                <img
                  src={event.image}
                  alt={event.title}
                  className="w-full h-full object-cover object-center"
                  onError={(e) => {
                    (e.target as HTMLImageElement).src =
                      "https://placehold.co/400x500/e2e8f0/64748b?text=Concert";
                  }}
                />
                <div className="absolute bottom-3 left-3 bg-[#f85c5c] text-white rounded-lg flex flex-col items-center justify-center w-14 h-14 font-bold shadow-lg">
                  <span className="text-[12px] uppercase leading-none">{formatMonth(event.date)}</span>
                  <span className="text-[20px] leading-none mt-1">{formatDay(event.date)}</span>
                </div>
              </div>
              <div className="p-5 flex flex-col items-start text-left flex-1 bg-white">
                <h4 className="font-extrabold text-[#161033] text-[18px] mb-1">
                  {event.title}
                </h4>
                <div className="flex items-center gap-1 text-[#554d63] text-[13px] mb-4">
                  <MapPin size={14} className="text-[#5142f5]" />
                  <span>{event.location}</span>
                </div>
                <Link
                  to={`/events/${event.id}`}
                  className="w-full bg-[#f4f2ff] text-[#7b61ff] font-bold text-[14px] py-2.5 rounded-full text-center hover:bg-[#eae6ff] hover:text-[#5d42f5] transition-all"
                >
                  Book Now
                </Link>
              </div>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
