
import java.io.*;
import java.util.*;

/*
 HotelReservationSystem.java
 Console-based hotel reservation system for CodeAlpha internship Task 4.

 Features:
 - Room categories: Standard, Deluxe, Suite
 - Search availability, Book room (with payment simulation), Cancel booking
 - View all bookings
 - File storage: bookings.txt (simple CSV-like format)
 - Object-oriented: Room, Booking, Hotel
 - Single-file for easy upload to GitHub

 Save this as: HotelReservationSystem.java
*/

class Room {
    enum Category { STANDARD, DELUXE, SUITE }

    private Category category;
    private double pricePerNight;

    Room(Category category, double pricePerNight) {
        this.category = category;
        this.pricePerNight = pricePerNight;
    }

    public Category getCategory() {
        return category;
    }

    public double getPricePerNight() {
        return pricePerNight;
    }

    public static Category fromString(String s) {
        s = s.trim().toUpperCase();
        switch (s) {
            case "STANDARD": return Category.STANDARD;
            case "DELUXE": return Category.DELUXE;
            case "SUITE": return Category.SUITE;
            default: return null;
        }
    }
}

class Booking {
    private String bookingId;
    private String customerName;
    private Room.Category category;
    private int nights;
    private double totalAmount;
    private long timestamp;

    Booking(String bookingId, String customerName, Room.Category category, int nights, double totalAmount, long timestamp) {
        this.bookingId = bookingId;
        this.customerName = customerName;
        this.category = category;
        this.nights = nights;
        this.totalAmount = totalAmount;
        this.timestamp = timestamp;
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public Room.Category getCategory() {
        return category;
    }

    public int getNights() {
        return nights;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("BookingID: %s | Name: %s | Category: %s | Nights: %d | Amount: %.2f",
                bookingId, customerName, category, nights, totalAmount);
    }

    // CSV line for file: bookingId,customerName,category,nights,totalAmount,timestamp
    public String toCSVLine() {
        String safeName = customerName.replace(",", " "); // avoid comma issues
        return String.join(",", bookingId, safeName, category.toString(), String.valueOf(nights),
                String.format(Locale.US, "%.2f", totalAmount), String.valueOf(timestamp));
    }

    public static Booking fromCSVLine(String line) {
        // Expect 6 fields
        String[] parts = line.split(",", 6);
        if (parts.length < 6) return null;
        try {
            String id = parts[0];
            String name = parts[1];
            Room.Category cat = Room.fromString(parts[2]);
            int nights = Integer.parseInt(parts[3]);
            double amount = Double.parseDouble(parts[4]);
            long ts = Long.parseLong(parts[5]);
            return new Booking(id, name, cat, nights, amount, ts);
        } catch (Exception e) {
            return null;
        }
    }
}

class Hotel {
    private Map<Room.Category, Integer> totalRooms;
    private Map<Room.Category, Integer> bookedRooms;
    private Map<Room.Category, Double> priceMap;
    private List<Booking> bookings;
    private final File bookingFile;

    Hotel(File bookingFile) {
        this.bookingFile = bookingFile;
        totalRooms = new EnumMap<>(Room.Category.class);
        bookedRooms = new EnumMap<>(Room.Category.class);
        priceMap = new EnumMap<>(Room.Category.class);
        bookings = new ArrayList<>();

        // Set default inventory counts (you can adjust)
        totalRooms.put(Room.Category.STANDARD, 10);
        totalRooms.put(Room.Category.DELUXE, 6);
        totalRooms.put(Room.Category.SUITE, 3);

        // Initial booked count = 0
        for (Room.Category c : Room.Category.values()) {
            bookedRooms.put(c, 0);
        }

        // Prices per night (example)
        priceMap.put(Room.Category.STANDARD, 1500.0);
        priceMap.put(Room.Category.DELUXE, 3000.0);
        priceMap.put(Room.Category.SUITE, 6000.0);

        // Load bookings from file if exists
        loadBookingsFromFile();
    }

    private void loadBookingsFromFile() {
        if (!bookingFile.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(bookingFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                Booking b = Booking.fromCSVLine(line);
                if (b != null) {
                    bookings.add(b);
                    // mark as booked (increase booked count)
                    Room.Category cat = b.getCategory();
                    bookedRooms.put(cat, bookedRooms.getOrDefault(cat, 0) + 1);
                }
            }
        } catch (IOException e) {
            System.out.println("Warning: Could not read bookings file (" + e.getMessage() + "). Starting fresh.");
        }
    }

    private void saveBookingsToFile() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(bookingFile, false))) {
            for (Booking b : bookings) {
                pw.println(b.toCSVLine());
            }
        } catch (IOException e) {
            System.out.println("Error saving bookings to file: " + e.getMessage());
        }
    }

    public int availableRooms(Room.Category category) {
        return totalRooms.getOrDefault(category, 0) - bookedRooms.getOrDefault(category, 0);
    }

    public void printAvailability() {
        System.out.println("Current Availability:");
        for (Room.Category c : Room.Category.values()) {
            System.out.printf("%-8s : %d available (Price/night: %.2f)\n",
                    c, availableRooms(c), priceMap.get(c));
        }
    }

    public List<Booking> searchBookingsByName(String name) {
        List<Booking> res = new ArrayList<>();
        for (Booking b : bookings) {
            if (b.getCustomerName().equalsIgnoreCase(name)) res.add(b);
        }
        return res;
    }

    public Booking bookRoom(String customerName, Room.Category category, int nights) {
        if (nights <= 0) return null;
        if (availableRooms(category) <= 0) return null;

        double price = priceMap.get(category);
        double total = price * nights;

        // Simulate payment - caller should do actual prompt; here we just finalize after assumed payment
        String bookingId = generateBookingId();
        long ts = System.currentTimeMillis();
        Booking booking = new Booking(bookingId, customerName, category, nights, total, ts);

        bookings.add(booking);
        bookedRooms.put(category, bookedRooms.getOrDefault(category, 0) + 1);
        saveBookingsToFile();
        return booking;
    }

    public boolean cancelBooking(String bookingId) {
        Iterator<Booking> it = bookings.iterator();
        while (it.hasNext()) {
            Booking b = it.next();
            if (b.getBookingId().equalsIgnoreCase(bookingId)) {
                // free a room of that category
                Room.Category c = b.getCategory();
                bookedRooms.put(c, Math.max(0, bookedRooms.getOrDefault(c, 0) - 1));
                it.remove();
                saveBookingsToFile();
                return true;
            }
        }
        return false;
    }

    public List<Booking> viewAllBookings() {
        // return a copy
        return new ArrayList<>(bookings);
    }

    private String generateBookingId() {
        // Example ID: B + last 5 digits of timestamp + random 3 digits
        long ts = System.currentTimeMillis();
        int rnd = new Random().nextInt(900) + 100;
        String id = "B" + (ts % 100000) + rnd;
        return id.toUpperCase();
    }

    public double getPrice(Room.Category cat) {
        return priceMap.getOrDefault(cat, 0.0);
    }
}

public class HotelReservationSystem {
    private static final Scanner sc = new Scanner(System.in);
    private static Hotel hotel;

    public static void main(String[] args) {
        File bookingFile = new File("bookings.txt");
        hotel = new Hotel(bookingFile);

        System.out.println("==== Welcome to CodeAlpha Hotel Reservation System ====");
        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Enter choice: ");
            switch (choice) {
                case 1:
                    hotel.printAvailability();
                    break;
                case 2:
                    handleBooking();
                    break;
                case 3:
                    handleCancellation();
                    break;
                case 4:
                    handleViewBookings();
                    break;
                case 5:
                    handleSearchByName();
                    break;
                case 6:
                    System.out.println("Saving and exiting... Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice. Try again.");
            }
            System.out.println();
        }
        sc.close();
    }

    private static void printMenu() {
        System.out.println("Menu:");
        System.out.println("1. Search / View Room Availability");
        System.out.println("2. Book Room");
        System.out.println("3. Cancel Booking");
        System.out.println("4. View All Bookings");
        System.out.println("5. Search Bookings by Customer Name");
        System.out.println("6. Exit");
    }

    private static void handleBooking() {
        System.out.println("---- Book a Room ----");
        System.out.print("Enter your name: ");
        String name = sc.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("Name cannot be empty.");
            return;
        }

        System.out.println("Choose category: 1. Standard  2. Deluxe  3. Suite");
        int catChoice = readInt("Category (1-3): ");
        Room.Category category;
        switch (catChoice) {
            case 1: category = Room.Category.STANDARD; break;
            case 2: category = Room.Category.DELUXE; break;
            case 3: category = Room.Category.SUITE; break;
            default:
                System.out.println("Invalid category."); return;
        }

        int avail = hotel.availableRooms(category);
        if (avail <= 0) {
            System.out.println("Sorry, no rooms available in selected category.");
            return;
        } else {
            System.out.println("Rooms available: " + avail + ". Price per night: " + hotel.getPrice(category));
        }

        int nights = readInt("Enter number of nights: ");
        if (nights <= 0) {
            System.out.println("Invalid nights.");
            return;
        }

        double total = hotel.getPrice(category) * nights;
        System.out.printf("Total amount to pay (simulated): %.2f\n", total);

        // Payment simulation
        System.out.print("Proceed to payment? (yes/no): ");
        String proceed = sc.nextLine().trim().toLowerCase();
        if (!proceed.equals("yes") && !proceed.equals("y")) {
            System.out.println("Booking cancelled by user (payment not completed).");
            return;
        }

        // Simulate payment success
        System.out.println("Processing payment...");
        try { Thread.sleep(700); } catch (InterruptedException ignored) {}
        System.out.println("Payment Successful!");

        Booking booking = hotel.bookRoom(name, category, nights);
        if (booking != null) {
            System.out.println("Booking Confirmed!");
            System.out.println(booking.toString());
            System.out.println("Save this Booking ID for cancellation: " + booking.getBookingId());
        } else {
            System.out.println("Booking failed. Please try again.");
        }
    }

    private static void handleCancellation() {
        System.out.println("---- Cancel Booking ----");
        System.out.print("Enter Booking ID to cancel: ");
        String id = sc.nextLine().trim();
        if (id.isEmpty()) {
            System.out.println("Booking ID cannot be empty.");
            return;
        }
        boolean ok = hotel.cancelBooking(id);
        if (ok) {
            System.out.println("Booking cancelled successfully.");
        } else {
            System.out.println("Booking ID not found. Please check and try again.");
        }
    }

    private static void handleViewBookings() {
        System.out.println("---- All Bookings ----");
        List<Booking> list = hotel.viewAllBookings();
        if (list.isEmpty()) {
            System.out.println("No bookings found.");
            return;
        }
        for (Booking b : list) {
            System.out.println(b.toString());
        }
    }

    private static void handleSearchByName() {
        System.out.print("Enter customer name to search: ");
        String name = sc.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("Name cannot be empty.");
            return;
        }
        List<Booking> found = hotel.searchBookingsByName(name);
        if (found.isEmpty()) {
            System.out.println("No bookings found for '" + name + "'.");
        } else {
            System.out.println("Found bookings:");
            for (Booking b : found) System.out.println(b.toString());
        }
    }

    // Utility to read integers robustly
    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }
}
