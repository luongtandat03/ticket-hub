import { useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  UserRound,
  CalendarDays,
  ChevronDown,
  CircleX,
  Map,
} from "lucide-react";
import {
  DatePicker,
  DateField,
  Calendar,
  Autocomplete,
  ListBox,
  EmptyState,
  SearchField,
} from "@heroui/react";

type City = {
  country: string;
  name: string;
};

export default function Hero() {
  const navigate = useNavigate();
  const [searchSingers, setSearchSingers] = useState("");
  const [searchDate, setSearchDate] = useState<any>(null);
  const [searchLocation, setSearchLocation] = useState<string>("");
  const [isLoading, setIsLoading] = useState(false);

  const allCities: City[] = [
    { country: "Vietnam", name: "TP Ho Chi Minh" },
    { country: "Vietnam", name: "Hanoi" },
    { country: "Vietnam", name: "Da Nang" },
    { country: "Vietnam", name: "Da Lat" },
    { country: "Vietnam", name: "Nha Trang" },
    { country: "Vietnam", name: "Can Tho" },
    { country: "Vietnam", name: "Hai Phong" },
  ];

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const params = new URLSearchParams();
    if (searchSingers.trim()) params.append("singer", searchSingers.trim());
    if (searchDate) params.append("date", searchDate.toString());
    if (searchLocation) params.append("location", searchLocation);
    const queryString = params.toString();
    navigate(queryString ? `/concerts?${queryString}` : "/concerts");
  };

  const customFilter = (textValue: string, inputValue: string) => {
    return textValue.toLowerCase().includes(inputValue.toLowerCase());
  };

  return (
    <section className="hero" id="hero">
      <div className="hero-bg" />
      <div className="hero-content">
        <h3 className="hero-title">Book Tickets Of Your Favorite Singers!</h3>

        <p className="hero-subtitle">
          Make Sure Don’t Miss These 5 Up Coming's Concerts!
        </p>

        {/* Search Bar matching screenshot */}
        <form
          className="flex flex-col md:flex-row items-stretch md:items-center gap-3 sm:gap-3.5 w-full max-w-4xl mx-auto my-8 relative z-50"
          onSubmit={handleSearch}
        >
          {/* Singer Input Pill */}
          <div className="hero-search-pill relative flex-[1.2] min-w-[200px] w-full flex items-center px-4 gap-2.5">
            <UserRound
              size={20}
              strokeWidth={2}
              className="text-[#3b334a] flex-shrink-0"
            />
            <input
              type="text"
              placeholder="Type a singer name"
              value={searchSingers}
              onChange={(e) => setSearchSingers(e.target.value)}
              className="flex-1 bg-transparent text-[15 px] text-[#241f30] placeholder:text-[#554d63] placeholder:font-bold font-bold outline-none border-none min-w-0"
            />
            <button
              type="button"
              onClick={() => setSearchSingers("")}
              className={`text-[#8e859b] hover:text-[#332b40] transition-colors p-0.5 ${
                searchSingers
                  ? "opacity-100 cursor-pointer"
                  : "opacity-60 cursor-default"
              }`}
              aria-label="Clear singer name"
            >
              <CircleX size={17} strokeWidth={1.8} />
            </button>
          </div>

          {/* */}
          <div className="hero-search-pill relative flex-1 min-w-[170px] w-full flex items-center px-4">
            <DatePicker
              name="date"
              aria-label="Date"
              className="w-full h-full flex items-center"
              value={searchDate}
              onChange={setSearchDate}
            >
              <DateField.Group
                fullWidth
                className="!bg-transparent !border-0 !shadow-none !p-0 !h-full w-full flex items-center justify-between gap-2"
              >
                <DateField.Prefix className="flex items-center pl-0 pr-1 pointer-events-none">
                  <CalendarDays
                    size={20}
                    strokeWidth={2}
                    className="text-[#3b334a]"
                  />
                </DateField.Prefix>
                <DateField.Input className="!bg-transparent !border-none !p-0 text-[14.5px] text-[#241f30] !font-bold outline-none !shadow-none flex-1">
                  {(segment) => (
                    <DateField.Segment
                      segment={segment}
                      className="!font-bold text-[#241f30] data-[placeholder=true]:text-[#4b4458] data-[placeholder=true]:!font-bold"
                    />
                  )}
                </DateField.Input>
                <DateField.Suffix className="flex items-center pr-0 pl-1">
                  <DatePicker.Trigger className="p-0 border-none bg-transparent hover:bg-transparent shadow-none cursor-pointer text-[#3b334a]">
                    <ChevronDown
                      size={18}
                      strokeWidth={2.2}
                      className="text-[#3b334a]"
                    />
                  </DatePicker.Trigger>
                </DateField.Suffix>
              </DateField.Group>

              <DatePicker.Popover className="bg-[#161033] border border-white/15 rounded-2xl shadow-2xl p-4 text-white backdrop-blur-xl z-[100] w-[290px]">
                <Calendar aria-label="Event date" className="text-white w-full">
                  <Calendar.Header className="flex items-center justify-between pb-3 text-white">
                    <Calendar.YearPickerTrigger className="cursor-pointer text-white font-bold hover:text-purple-300 transition-colors">
                      <Calendar.YearPickerTriggerHeading className="text-sm font-bold text-white" />
                      <Calendar.YearPickerTriggerIndicator />
                    </Calendar.YearPickerTrigger>
                    <div className="flex items-center gap-1">
                      <Calendar.NavButton
                        slot="previous"
                        className="text-white hover:bg-white/10 rounded-lg p-1 cursor-pointer"
                      />
                      <Calendar.NavButton
                        slot="next"
                        className="text-white hover:bg-white/10 rounded-lg p-1 cursor-pointer"
                      />
                    </div>
                  </Calendar.Header>

                  <Calendar.Grid className="w-full">
                    <Calendar.GridHeader>
                      {(day) => (
                        <Calendar.HeaderCell className="text-xs text-[#c3b7e4] font-bold pb-2 text-center">
                          {day}
                        </Calendar.HeaderCell>
                      )}
                    </Calendar.GridHeader>
                    <Calendar.GridBody>
                      {(date) => (
                        <Calendar.Cell
                          date={date}
                          className="text-center text-sm p-1.5 rounded-lg hover:bg-white/15 cursor-pointer font-bold data-[selected=true]:bg-[#4f3df5] data-[selected=true]:text-white"
                        />
                      )}
                    </Calendar.GridBody>
                  </Calendar.Grid>

                  <Calendar.YearPickerGrid className="h-[210px] max-h-[210px] overflow-y-auto overscroll-contain">
                    <Calendar.YearPickerGridBody>
                      {({ year }) => (
                        <Calendar.YearPickerCell
                          year={year}
                          className="py-1 px-2 text-sm rounded-lg hover:bg-white/15 cursor-pointer text-center font-bold data-[selected=true]:bg-[#4f3df5] data-[selected=true]:text-white"
                        />
                      )}
                    </Calendar.YearPickerGridBody>
                  </Calendar.YearPickerGrid>
                </Calendar>
              </DatePicker.Popover>
            </DatePicker>
          </div>

          {/* Location Picker Pill */}
          <div className="hero-search-pill relative flex-1 min-w-[170px] w-full flex items-center">
            <Autocomplete
              aria-label="Select location"
              placeholder="Location"
              value={searchLocation}
              onChange={(key) => setSearchLocation(key as string)}
              className="w-full h-full flex items-center autocomplete-reset"
            >
              <Autocomplete.Trigger className="w-full px-4 !bg-transparent border-none shadow-none outline-none ring-0 flex items-center gap-2 h-full data-[hover=true]:!bg-transparent data-[open=true]:!bg-transparent data-[focus=true]:!bg-transparent data-[has-value=true]:!bg-transparent text-[#241f30]">
                <Map
                  size={20}
                  strokeWidth={2}
                  className="text-[#3b334a] flex-shrink-0"
                />
                <Autocomplete.Value className="text-[14.5px] font-bold !text-[#241f30] flex-1 text-left placeholder:!text-[#554d63]" />
                <Autocomplete.ClearButton className="text-[#8e859b] hover:text-[#332b40]" />
                <Autocomplete.Indicator className="text-[#3b334a]" />
              </Autocomplete.Trigger>
              <Autocomplete.Popover className="bg-[#161033] border border-white/20 rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.4)] p-3 z-[100] mt-2 min-w-[280px]">
                <Autocomplete.Filter filter={customFilter}>
                  <SearchField
                    autoFocus
                    aria-label="Search cities"
                    name="search"
                    className="mb-3"
                  >
                    <SearchField.Group className="bg-[#2a1f54] border border-white/20 rounded-xl px-3 py-2.5 flex items-center text-white shadow-inner transition-colors focus-within:bg-[#342763] focus-within:border-white/30">
                      <SearchField.SearchIcon className="text-white/70 mr-2.5 w-[18px] h-[18px]" />
                      <SearchField.Input
                        className="bg-transparent border-none outline-none text-white text-[14.5px] w-full placeholder:text-white/50"
                        placeholder="Search cities..."
                      />
                      <SearchField.ClearButton className="text-white/60 hover:text-white transition-colors" />
                    </SearchField.Group>
                  </SearchField>
                  <ListBox
                    className="max-h-60 overflow-y-auto hide-scrollbar"
                    renderEmptyState={() => (
                      <EmptyState className="text-white/60 text-sm p-4 text-center">
                        {isLoading ? "Searching..." : "No results found..."}
                      </EmptyState>
                    )}
                  >
                    {allCities.map((city) => (
                      <ListBox.Item
                        key={city.name}
                        value={city.name}
                        textValue={city.name}
                        className="text-[#c3b7e4] hover:text-white hover:bg-white/10 focus:bg-white/10 data-[selected=true]:bg-white/20 rounded-xl px-3.5 py-2.5 cursor-pointer transition-colors"
                      >
                        <div className="flex flex-col">
                          <span className="text-[15px] font-semibold text-white">
                            {city.name}
                          </span>
                          <span className="text-[12.5px] text-[#8e859b] mt-0.5">
                            {city.country}
                          </span>
                        </div>
                        <ListBox.ItemIndicator className="text-[#aa3bff]" />
                      </ListBox.Item>
                    ))}
                  </ListBox>
                </Autocomplete.Filter>
              </Autocomplete.Popover>
            </Autocomplete>
          </div>
          {/* Find Ticket Button */}
          <button
            type="submit"
            className="hero-find-btn px-7 sm:px-8 flex-shrink-0 w-full md:w-auto flex items-center justify-center cursor-pointer select-none"
          >
            Find Ticket
          </button>
        </form>
      </div>
    </section>
  );
}
