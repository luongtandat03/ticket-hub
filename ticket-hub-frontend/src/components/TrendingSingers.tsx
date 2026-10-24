import { Link } from "react-router-dom";

interface Singer {
  id: string;
  name: string;
  title: string;
  image: string;
}

const trendingSingers: Singer[] = [
  {
    id: "1",
    name: "Celine Dion",
    title: "Singer",
    image: "/images/celine.jpg",
  },
  {
    id: "2",
    name: "Taylor Swift",
    title: "Singer",
    image: "/images/taylor.jpg",
  },
  {
    id: "3",
    name: "Justin Bieber",
    title: "Singer",
    image: "/images/justin.jpg",
  },
  {
    id: "4",
    name: "Adele",
    title: "Singer",
    image: "/images/adele.jpg",
  },
  {
    id: "5",
    name: "Ed Sheeran",
    title: "Singer",
    image: "/images/ed.jpg",
  },
];

export default function TrendingSingers() {
  return (
    <section className="relative z-30 -mt-36 w-full max-w-7xl mx-auto px-4 sm:px-8 lg:px-12 mb-16">
      <div className="flex justify-center gap-4 sm:gap-6 overflow-x-auto pb-6 hide-scrollbar">
        {trendingSingers.map((singer) => (
          <div
            key={singer.id}
            className="flex-none w-[160px] sm:w-[180px] bg-white rounded-[20px] overflow-hidden flex flex-col p-[2px]"
            style={{
              background: "linear-gradient(180deg, #5142f5 0%, #ffffff 100%)",
              boxShadow: "0px 10px 30px rgba(0, 0, 0, 0.08)"
            }}
          >
            <div className="bg-white rounded-[18px] flex flex-col h-full overflow-hidden">
              <div className="h-[200px] w-full overflow-hidden bg-gray-200">
                <img
                  src={singer.image}
                  alt={singer.name}
                  className="w-full h-full object-cover object-center"
                  onError={(e) => {
                    (e.target as HTMLImageElement).src =
                      "https://placehold.co/400x500/e2e8f0/64748b?text=Singer";
                  }}
                />
              </div>
              <div className="p-4 flex flex-col items-center text-center flex-1 bg-white">
                <h4 className="font-extrabold text-[#161033] text-[15px] mb-0.5">
                  {singer.name}
                </h4>
                <p className="text-[#f85c5c] font-semibold text-[12px] mb-3">
                  {singer.title}
                </p>
                <Link
                  to={`/singers/${singer.id}`}
                  className="w-full text-[#7b61ff] font-bold text-[13px] hover:text-[#5d42f5] transition-all"
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
