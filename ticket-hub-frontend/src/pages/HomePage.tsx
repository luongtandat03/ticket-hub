import {
  Hero,
  TrendingSingers,
  Benefits,
  FeaturedEvents,
  WhyChooseUs,
  Newsletter,
} from "../components";

export default function HomePage() {
  return (
    <div className="home-page">
      <Hero />
      <TrendingSingers />
      <Benefits />
      <FeaturedEvents />
      <WhyChooseUs />
      <Newsletter />
    </div>
  );
}
