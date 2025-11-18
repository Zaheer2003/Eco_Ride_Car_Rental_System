// Save this file as EcoRideModern.java
// Single-file professional CLI EcoRide Car Rental System (Customers, Vehicles, Bookings, Drivers, Invoice)

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern; // Necessary for robust validation
public class EcoRideCarRentalSystem {

    /* ===========================
       GLOBAL CONSTANTS
       =========================== */
    private static final double DRIVER_DAILY_FEE = 2500.0;

    /* ===========================
       ANSI COLORS & UI
       =========================== */
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String CYAN = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";
    private static final String GREEN = "\u001B[32m";
    private static final String GRAY = "\u001B[90m";
    private static final String WHITE = "\u001B[37m";
    private static final String RED = "\u001B[31m";
    private static final Scanner sc = new Scanner(System.in);

    /* ===========================
       ENUMS
       =========================== */
    enum VehicleStatus { AVAILABLE, RESERVED, UNDER_MAINTENANCE }
    enum DriverStatus { AVAILABLE, ASSIGNED, ON_LEAVE }
    enum BookingStatus { RESERVED, COMPLETED, CANCELLED }

    /* ===========================
       MODEL CLASSES
       =========================== */

    // Customer base class (abstract)
    static abstract class Customer {
        private final String customerId;
        protected String name;
        protected String contactNo;
        protected String email;
        protected String drivingLicense;

        public Customer(String customerId) { this.customerId = customerId; }

        public abstract void registerInteractive(App app);

        public void updateDetailsInteractive(App app) {
            System.out.print("New contact number (leave empty to keep): ");
            String contact = sc.nextLine().trim();
            if (!contact.isEmpty() && app.isValidGeneralContact(contact)) this.contactNo = contact;

            System.out.print("New email (leave empty to keep): ");
            String mail = sc.nextLine().trim();
            if (!mail.isEmpty() && app.isValidEmail(mail)) this.email = mail;

            println(GREEN + "Customer details updated." + RESET);
        }

        public String getCustomerId() { return customerId; }
        public String getName() { return name; }
        public String getContactNo() { return contactNo; }
        public String getEmail() { return email; }
        public String getDrivingLicense() { return drivingLicense; }

        public void displayRow() {
            System.out.printf("║ %-8s ║ %-16s ║ %-12s ║ %-22s ║ %-12s ║%n",
                    getCustomerId(), getName(), getContactNo(), getEmail(), getDrivingLicense());
        }
    }

    static class LocalCustomer extends Customer {
        private String nic;

        public LocalCustomer(String id) { super(id); }

        @Override
        public void registerInteractive(App app) {
            System.out.print("Enter name: "); this.name = sc.nextLine();
            this.nic = app.readLineWithValidation("Enter NIC (9/12 alphanumeric): ", app::isValidNIC);
            System.out.print("Enter driving license no: "); this.drivingLicense = sc.nextLine();
            this.contactNo = app.readLineWithValidation("Enter contact number (07xxxxxxxx): ", app::isValidLankaContact);
            this.email = app.readLineWithValidation("Enter email: ", app::isValidEmail);
        }

        public String getNic() { return nic; }
    }

    static class ForeignCustomer extends Customer {
        private String passportNo;

        public ForeignCustomer(String id) { super(id); }

        @Override
        public void registerInteractive(App app) {
            System.out.print("Enter name: "); this.name = sc.nextLine();
            this.passportNo = app.readLineWithValidation("Enter passport no (Min 6 alphanumeric): ", app::isValidPassport);
            System.out.print("Enter driving license no: "); this.drivingLicense = sc.nextLine();
            this.contactNo = app.readLineWithValidation("Enter contact number (General format): ", app::isValidGeneralContact);
            this.email = app.readLineWithValidation("Enter email: ", app::isValidEmail);
        }

        public String getPassportNo() { return passportNo; }
    }

    // PackageInfo (composition inside Vehicle)
    static class PackageInfo {
        private final String categoryName;
        private final double dailyRentalFee;
        private final int freeKmPerDay;
        private final double extraKmCharge;
        private final double taxRate;

        public PackageInfo(String categoryName, double dailyRentalFee, int freeKmPerDay, double extraKmCharge, double taxRate) {
            this.categoryName = categoryName;
            this.dailyRentalFee = dailyRentalFee;
            this.freeKmPerDay = freeKmPerDay;
            this.extraKmCharge = extraKmCharge;
            this.taxRate = taxRate;
        }

        public double calcExtraCharge(double totalKm, int days) {
            double allowed = freeKmPerDay * days;
            return totalKm > allowed ? (totalKm - allowed) * extraKmCharge : 0.0;
        }

        public double calcTax(double amount) { return amount * (taxRate / 100.0); }

        public String getCategoryName() { return categoryName; }
        public double getDailyRentalFee() { return dailyRentalFee; }
        public double getTaxRate() { return taxRate; }
    }

    // Vehicle
    static class Vehicle {
        private final String carId;
        private final String model;
        private final PackageInfo pkg;
        private VehicleStatus status;

        public Vehicle(String carId, String model, PackageInfo pkg) {
            this.carId = carId;
            this.model = model;
            this.pkg = pkg;
            this.status = VehicleStatus.AVAILABLE;
        }

        public String getCarId() { return carId; }
        public String getModel() { return model; }
        public PackageInfo getPkg() { return pkg; }
        public VehicleStatus getStatus() { return status; }
        public void setStatus(VehicleStatus status) { this.status = status; }

        public void displayRow() {
            String statusColor = (status == VehicleStatus.AVAILABLE) ? GREEN : (status == VehicleStatus.RESERVED ? YELLOW : RED);
            // FIX: Changed from single '│' to double '║'
            System.out.printf("║ %-8s ║ %-18s ║ %-18s ║ %-10.2f ║ %s%-12s%s ║%n",
                    carId, model, pkg.getCategoryName(), pkg.getDailyRentalFee(), BOLD + statusColor, status, RESET);
        }
    }

    // Drivers
    static class Driver {
        private final String driverId;
        private final String name;
        private final String licenseNo;
        private final String contactNo;
        private DriverStatus status;

        public Driver(String driverId, String name, String licenseNo, String contactNo) {
            this.driverId = driverId;
            this.name = name;
            this.licenseNo = licenseNo;
            this.contactNo = contactNo;
            this.status = DriverStatus.AVAILABLE;
        }

        public String getDriverId() { return driverId; }
        public String getName() { return name; }
        public DriverStatus getStatus() { return status; }
        public void setStatus(DriverStatus status) { this.status = status; }

        public void displayRow() {
            String statusColor = (status == DriverStatus.AVAILABLE) ? GREEN : (status == DriverStatus.ASSIGNED ? YELLOW : RED);
            // FIX: Changed from single '│' to double '║'
            System.out.printf("║ %-8s ║ %-18s ║ %-14s ║ %-13s ║ %s%-10s%s ║%n",
                    driverId, name, licenseNo, contactNo, BOLD + statusColor, status, RESET);
        }
    }

    // Invoice (composition from booking)
    static class Invoice {
        private final String invoiceId;
        private final String bookingId;
        // Added driverFee
        private double basePrice, extraKmCharge, discount, tax, depositDeducted, finalAmount, driverFee;

        public Invoice(String invoiceId, String bookingId) {
            this.invoiceId = invoiceId;
            this.bookingId = bookingId;
        }

        public void populate(double basePrice, double extraKmCharge, double discount, double tax, double deposit, double finalAmount, double driverFee) {
            this.basePrice = basePrice;
            this.extraKmCharge = extraKmCharge;
            this.discount = discount;
            this.tax = tax;
            this.depositDeducted = deposit;
            this.finalAmount = finalAmount;
            this.driverFee = driverFee; // Assigned new field
        }

        public void display(double totalKmUsed, boolean driverAssigned) {
            System.out.println();
            // This invoice border is kept as a single-line block (not requested to change)
            System.out.println(CYAN + BOLD + "╭───────────────────── FINAL INVOICE ─────────────────────╮" + RESET);
            System.out.printf("│ Invoice ID : %-30s│%n", invoiceId);
            System.out.printf("│ Booking ID : %-30s│%n", bookingId);
            System.out.printf("│ Total KM Used: %-25.0f km │%n", totalKmUsed);
            System.out.println(GRAY + "├───────────────────────────────────────────────────┤" + RESET);
            System.out.printf("│ Base Rental Fee: LKR %-25.2f│%n", basePrice);
            System.out.printf("│ Extra KM Charge: LKR %-25.2f│%n", extraKmCharge);

            // New: Display Driver Service Fee if applicable
            if (driverAssigned) {
                System.out.printf("│ Driver Service Fee: LKR %-24.2f│%n", driverFee);
            }

            System.out.printf("│ 7+ Day Discount (10%%): LKR %-25.2f│%n", discount);
            System.out.printf("│ Tax: LKR %-25.2f│%n", tax);
            System.out.println(GRAY + "├───────────────────────────────────────────────────┤" + RESET);
            System.out.printf("│ Deposit Deducted: LKR (%-25.2f)│%n", depositDeducted);
            System.out.printf(GREEN + BOLD + "│ FINAL PAYABLE AMOUNT: LKR %-25.2f│" + RESET + "%n", finalAmount); // Using printf here for final amount
            System.out.println(CYAN + BOLD + "╰───────────────────────────────────────────────────╯" + RESET);
            System.out.println();
        }
    }

    // Booking
    static class Booking {
        private final String bookingId;
        private final Customer customer;
        private final Vehicle vehicle;
        private final Driver driver;
        private final LocalDate bookingDate;
        private final int rentalDays;
        private final double estimatedKm;
        private double actualKm = 0.0;
        private BookingStatus status;
        private final double deposit = 5000.0;
        private final Invoice invoice;

        public Booking(String bookingId, Customer customer, Vehicle vehicle, Driver driver, LocalDate bookingDate, int rentalDays, double estimatedKm) {
            this.bookingId = bookingId;
            this.customer = customer;
            this.vehicle = vehicle;
            this.driver = driver;
            this.bookingDate = bookingDate;
            this.rentalDays = rentalDays;
            this.estimatedKm = estimatedKm;
            this.status = BookingStatus.RESERVED;
            this.invoice = new Invoice("INV-" + bookingId, bookingId);
        }

        public boolean canBook() { return vehicle.getStatus() == VehicleStatus.AVAILABLE; }

        public void setActualKm(double actualKm) { this.actualKm = actualKm; }

        public boolean canCancel() {
            if (status != BookingStatus.RESERVED) return false;
            long daysUntilBooking = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), bookingDate);
            return daysUntilBooking > 2;
        }

        // Calculates final fee and populates invoice, returns final amount
        public double calculateFinalFee() {
            PackageInfo pkg = vehicle.getPkg();

            double kmToUse = (status == BookingStatus.COMPLETED && actualKm > 0) ? actualKm : estimatedKm;

            double base = pkg.getDailyRentalFee() * rentalDays;

            double extra = pkg.calcExtraCharge(kmToUse, rentalDays);

            double driverCharge = 0.0;
            if (driver != null) {
                driverCharge = EcoRideCarRentalSystem.DRIVER_DAILY_FEE * rentalDays;
            }

            double discount = rentalDays >= 7 ? base * 0.10 : 0.0;

            double taxable = base - discount + extra + driverCharge;

            double tax = pkg.calcTax(taxable);

            double finalAmt = taxable + tax - deposit;

            invoice.populate(base, extra, discount, tax, deposit, finalAmt, driverCharge);
            return finalAmt;
        }


        public void finalizeBooking() {
            vehicle.setStatus(VehicleStatus.RESERVED);
            if (driver != null) driver.setStatus(DriverStatus.ASSIGNED);
        }

        public String getBookingId() { return bookingId; }
        public Customer getCustomer() { return customer; }
        public Vehicle getVehicle() { return vehicle; }
        public Driver getDriver() { return driver; }
        public Invoice getInvoice() { return invoice; }
        public BookingStatus getStatus() { return status; }
        public void setStatus(BookingStatus status) { this.status = status; }
        public double getActualKm() { return actualKm; }
        public LocalDate getBookingDate() { return bookingDate; }


        public void displayRow() {
            String driverId = driver != null ? driver.getDriverId() : "N/A";
            // FIX: Changed from single '│' to double '║'
            System.out.printf("║ %-8s ║ %-8s ║ %-8s ║ %-10s ║ %-6d ║ %-8.1f ║ %-8s ║%n",
                    bookingId, customer.getCustomerId(), vehicle.getCarId(), status, rentalDays, actualKm > 0 ? actualKm : estimatedKm, driverId);
        }
    }

    /* ===========================
       APPLICATION (controller + view)
       =========================== */

    static class App {
        private final Map<String, Customer> customers = new LinkedHashMap<>();
        private final Map<String, Vehicle> vehicles = new LinkedHashMap<>();
        private final Map<String, Driver> drivers = new LinkedHashMap<>();
        private final Map<String, Booking> bookings = new LinkedHashMap<>();

        private int custCounter = 1, vehCounter = 1, drvCounter = 1, bookingCounter = 1;

        // Standard package list (table)
        private final List<PackageInfo> packageOptions = Arrays.asList(
                new PackageInfo("Compact Petrol", 5000, 100, 50, 10),
                new PackageInfo("Hybrid Midsize", 7500, 150, 60, 12),
                new PackageInfo("Electric Premium", 10000, 200, 40, 8),
                new PackageInfo("Luxury SUV", 15000, 250, 75, 15)
        );

        public void start() {
            while (true) {
                clear();
                printDashboard(); // Use the updated, compact dashboard
                int choice = readInt("\n" + GREEN + "➤ Enter your choice (0-13): " + RESET);
                switch (choice) {
                    case 1 -> addCustomer();
                    case 2 -> addVehicle();
                    case 3 -> addDriver();
                    case 4 -> makeBooking();
                    case 5 -> displayCustomers();
                    case 6 -> displayVehicles();
                    case 7 -> displayDrivers();
                    case 8 -> displayBookings();
                    case 9 -> searchBooking();
                    case 10 -> updateCustomerDetails();
                    case 11 -> completeBooking(); // New feature
                    case 12 -> cancelBooking(); // New feature
                    case 13 -> manageAssetStatus(); // New feature
                    case 0 -> exitApp();
                    default -> {
                        printlnErr("Invalid choice. Try again.");
                        pause();
                    }
                }
            }
        }

        private void printDashboard() {
            String time = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
            final String DOUBLE_TOP_BORDER = WHITE + BOLD + "╔═════════════════════════════════════════════════════════════════════════════════╗" + RESET;
            final String DOUBLE_H_DIVIDER = WHITE + "╠═══════════════════════════════╦═════════════════════════════════════════════════╣" + RESET;
            final String DOUBLE_INTERNAL_DIVIDER = WHITE + "╠═══════════════════════════════╬═════════════════════════════════════════════════╣" + RESET;
            final String DOUBLE_FOOTER = WHITE + "╚═══════════════════════════════╩═════════════════════════════════════════════════╝" + RESET;

            // 1. Large Double-Bordered Header Block (81 content width)
            println(DOUBLE_TOP_BORDER);

            // --- ECORIDE Block Logo  ---
            println(WHITE + BOLD + "║" + RESET + "                                                                                 " + RESET + WHITE+"║");
            printEcoRideBlockLogo(); // The LARGER ECORIDE Block Logo
            println(WHITE + BOLD + "║" + RESET + "      " + WHITE + BOLD + "               C A R   R E N T A L   S Y S T E M" + RESET  + WHITE + BOLD + "                           ║" + RESET); // Line 10: Subtitle
            println(WHITE + BOLD + "║                                                                                 " + RESET + WHITE+ "║"); // Blank line for spacing
            // --- End Logo ---

            // 2. Connector to the Menu Section
            println(DOUBLE_H_DIVIDER);

            // 3. Menu/Status Headers
            println(String.format(WHITE+"║ %-20s                 "  +         WHITE+    "    ║ %-26s  "                   + WHITE+"                            ║", CYAN+ BOLD +  "MAIN MENU" + RESET, BOLD  +  "SYSTEM STATUS" + RESET));

            println(DOUBLE_INTERNAL_DIVIDER);

            // Menu Items & Counters
            println(String.format(WHITE+"║ " + CYAN + BOLD + "1. Register Customer" + RESET + WHITE+"          ║ Total Customers: %-15d                ║", customers.size()));
            println(String.format(WHITE+"║ " + CYAN + BOLD + "2. Add New Vehicle" + RESET +WHITE+ "            ║ Vehicles Available: %-9d                   ║", availableVehiclesCount()));
            println(String.format(WHITE+"║ " + CYAN + BOLD + "3. Add New Driver" + RESET + WHITE+"             ║ Drivers Available: %-10d                   ║", availableDriversCount()));
            println(String.format(WHITE+"║ " + CYAN + BOLD + "4. Make Booking" + RESET + WHITE+"               ║ Total Bookings: %-10d                      ║", bookings.size()));
            println(DOUBLE_INTERNAL_DIVIDER);
            println(String.format(WHITE+"║ " + CYAN + BOLD + "5. View Customers" + RESET + WHITE+"             ║ " + RED + BOLD + "3-Day Advance Booking" + RESET + WHITE+"                           ║"));
            println(String.format(WHITE+"║ " + CYAN + BOLD + "6. View Vehicles" + RESET + WHITE+"              ║ " + RED + BOLD + "2-Day Cancel Lockout" + RESET +WHITE+ "                            ║"));
            println(String.format(WHITE+"║ " + CYAN + BOLD + "7. View Drivers" + RESET + WHITE+ "               ║ " + GREEN + BOLD + "7+ Days = 10%% Discount" + RESET + WHITE+"                          ║"));
            println(String.format(WHITE+"║ " + CYAN + BOLD + "8. View Bookings" + RESET + WHITE+"              ║ " + YELLOW + BOLD + "Driver Fee: LKR %.0f/day" + RESET +WHITE+ "                        ║", DRIVER_DAILY_FEE));
            println(String.format(WHITE+"║ " + CYAN + BOLD + "9. Search Booking" + RESET + WHITE+"             ║ " + GRAY + "System Date: %-13s" + RESET + WHITE+"                      ║", time));
            println(DOUBLE_INTERNAL_DIVIDER);
            println(String.format(WHITE+"║ " + YELLOW + BOLD + "10. Update Customer" + RESET + WHITE+"           ║ " + GREEN + BOLD + "11. Complete Booking" + RESET + WHITE+"                            ║"));
            println(String.format(WHITE+"║ " + YELLOW + BOLD + "12. Cancel Booking" + RESET + WHITE+"            ║ " + RED + BOLD + "13. Manage Asset Status" + RESET +WHITE+ "                         ║"));
            println(String.format(WHITE+"║ " + RED + BOLD + "0. Exit Application" + RESET + WHITE+"           ║ %-26s                      ║", " "));

            // 4. Footer
            println(DOUBLE_FOOTER);
        }

        private void printEcoRideBlockLogo() {
            final String E_COLOR = GREEN + BOLD;
            final String R_COLOR = YELLOW + BOLD;
            final String BORDER = WHITE + BOLD + "║" + RESET;
            final String PAD = "  ";
            final String LOGO_LEFT_SPACING = "             ";
            final String LOGO_RIGHT_SPACING = "              ";

            // E (6 wide)
            final String E_BLOCK = E_COLOR + "██████" + RESET;
            final String E_L1 = E_BLOCK;
            final String E_L2 = E_COLOR + "██    " + RESET;
            final String E_L3 = E_BLOCK;
            final String E_L4 = E_COLOR + "██    " + RESET;
            final String E_L5 = E_BLOCK;

            // C (6 wide)
            final String C_L1 = E_COLOR + " ████ " + RESET;
            final String C_L2 = E_COLOR + "██    " + RESET;
            final String C_L3 = E_COLOR + "██    " + RESET;
            final String C_L4 = E_COLOR + "██    " + RESET;
            final String C_L5 = C_L1;

            // O (6 wide)
            final String O_L1 = E_COLOR + " ████ " + RESET;
            final String O_L2 = E_COLOR + "██  ██" + RESET;
            final String O_L3 = O_L2;
            final String O_L4 = O_L2;
            final String O_L5 = O_L1;

            // R (6 wide)
            final String R_L1 = R_COLOR + "██████" + RESET;
            final String R_L2 = R_COLOR + "██  ██" + RESET;
            final String R_L3 = R_COLOR + "██████" + RESET;
            final String R_L4 = R_COLOR + "██ ██ " + RESET;
            final String R_L5 = R_COLOR + "██  ██" + RESET;

            // I (6 wide)
            final String I_L1 = R_COLOR + "██████" + RESET;
            final String I_L2 = R_COLOR + "  ██  " + RESET;
            final String I_L3 = I_L2;
            final String I_L4 = I_L2;
            final String I_L5 = I_L1;

            // D (6 wide)
            final String D_L1 = R_COLOR + "█████ " + RESET;
            final String D_L2 = R_COLOR + "██  ██" + RESET;
            final String D_L3 = D_L2;
            final String D_L4 = D_L2;
            final String D_L5 = D_L1;

            // E2 (second E in RIDE, uses the RIDE color)
            final String E2_L1 = R_COLOR + "██████" + RESET;
            final String E2_L2 = R_COLOR + "██    " + RESET;
            final String E2_L3 = R_COLOR + "██████" + RESET;
            final String E2_L4 = R_COLOR + "██    " + RESET;
            final String E2_L5 = R_COLOR + "██████" + RESET;


            // --- Print Lines ---

            // Line 1: E C O R I D E
            System.out.println(BORDER + LOGO_LEFT_SPACING + E_L1 + PAD + C_L1 + PAD + O_L1 + PAD + R_L1 + PAD + I_L1 + PAD + D_L1 + PAD + E2_L1 + LOGO_RIGHT_SPACING + BORDER);

            // Line 2: E C O R I D E
            System.out.println(BORDER + LOGO_LEFT_SPACING + E_L2 + PAD + C_L2 + PAD + O_L2 + PAD + R_L2 + PAD + I_L2 + PAD + D_L2 + PAD + E2_L2 + LOGO_RIGHT_SPACING + BORDER);

            // Line 3: E C O R I D E
            System.out.println(BORDER + LOGO_LEFT_SPACING + E_L3 + PAD + C_L3 + PAD + O_L3 + PAD + R_L3 + PAD + I_L3 + PAD + D_L3 + PAD + E2_L3 + LOGO_RIGHT_SPACING + BORDER);

            // Line 4: E C O R I D E
            System.out.println(BORDER + LOGO_LEFT_SPACING + E_L4 + PAD + C_L4 + PAD + O_L4 + PAD + R_L4 + PAD + I_L4 + PAD + D_L4 + PAD + E2_L4 + LOGO_RIGHT_SPACING + BORDER);

            // Line 5: E C O R I D E
            System.out.println(BORDER + LOGO_LEFT_SPACING + E_L5 + PAD + C_L5 + PAD + O_L5 + PAD + R_L5 + PAD + I_L5 + PAD + D_L5 + PAD + E2_L5 + LOGO_RIGHT_SPACING + BORDER);
        }

        /* ------------------------------------------------
           VALIDATION HELPERS
           ------------------------------------------------ */

        // Helper to loop until input is valid
        private String readLineWithValidation(String prompt, Validator validator) {
            String input;
            while (true) {
                System.out.print(prompt);
                input = sc.nextLine().trim();
                if (validator.validate(input)) {
                    return input;
                } else {
                    printlnErr("❌ Invalid format or input. Please re-enter.");
                }
            }
        }

        @FunctionalInterface
        private interface Validator {
            boolean validate(String input);
        }

        // 1. Email Validation (Simple standard check)
        private boolean isValidEmail(String email) {
            return Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE).matcher(email).matches();
        }

        // 2. Sri Lankan Contact Validation (07xxxxxxxx)
        private boolean isValidLankaContact(String contact) {
            return contact.matches("07[0-9]{8}");
        }

        // 3. General Contact Validation (Allows +, numbers, and spaces)
        private boolean isValidGeneralContact(String contact) {
            return contact.matches("(\\+?[0-9\\s-]{7,15})");
        }

        // 4. NIC Validation (Old 10-char or New 12-char format)
        private boolean isValidNIC(String nic) {
            return nic.matches("^[0-9]{9}[VvXx]$") || nic.matches("^[0-9]{12}$");
        }

        // 5. Passport Validation (Simple alphanumeric check, min 6 chars)
        private boolean isValidPassport(String passport) {
            return passport.matches("^[A-Z0-9]{6,15}$");
        }


        /* ------------------------------------------------
           CORE CRUD/ACTION METHODS
           ------------------------------------------------ */

        private void addCustomer() {
            println(CYAN + "\n[1. Register Customer]" + RESET);
            String id = String.format("C%03d", custCounter++);
            int type = readInt("Type (1=Local, 2=Foreign): ");
            Customer c = (type == 1) ? new LocalCustomer(id) : new ForeignCustomer(id);
            c.registerInteractive(this);
            customers.put(id, c);
            println(GREEN + "✅ Customer added with ID: " + id + RESET);
            pause();
        }

        private void addVehicle() {
            println(CYAN + "\n[2. Add Vehicle]" + RESET);
            String id = String.format("V%03d", vehCounter++);
            String model = read("Model: ");

            System.out.println("Choose package:");
            for (int i = 0; i < packageOptions.size(); i++) {
                System.out.printf("%d. %-15s (LKR %.0f/day)%n", i + 1, packageOptions.get(i).getCategoryName(), packageOptions.get(i).getDailyRentalFee());
            }
            int p = readInt("Select (1-" + packageOptions.size() + "): ");
            if (p < 1 || p > packageOptions.size()) { printlnErr("Invalid package selected, defaulting to 1."); p = 1; }
            PackageInfo pkg = packageOptions.get(p - 1);

            Vehicle v = new Vehicle(id, model, pkg);
            vehicles.put(id, v);
            println(GREEN + "✅ Vehicle added with ID: " + id + RESET);
            pause();
        }

        private void addDriver() {
            println(CYAN + "\n[3. Add Driver]" + RESET);
            String id = String.format("D%03d", drvCounter++);
            String name = read("Name: ");
            String licenseNo = read("License No: ");
            String contactNo = readLineWithValidation("Contact No (General format): ", this::isValidGeneralContact);

            Driver d = new Driver(id, name, licenseNo, contactNo);
            drivers.put(id, d);
            println(GREEN + "✅ Driver added with ID: " + id + RESET);
            pause();
        }

        private void makeBooking() {
            println(CYAN + "\n[4. Make Booking]" + RESET);

            String custId = read("Enter Customer ID (Cxxx): ").toUpperCase();
            Customer customer = customers.get(custId);
            if (customer == null) { printlnErr("Customer not found."); pause(); return; }

            // 1. Vehicle Selection
            List<Vehicle> availableVehicles = vehicles.values().stream()
                    .filter(v -> v.getStatus() == VehicleStatus.AVAILABLE)
                    .toList();

            if (availableVehicles.isEmpty()) { printlnErr("No vehicles available for booking."); pause(); return; }

            displayVehicles(availableVehicles);
            String carId = read("Enter Car ID to book (Vxxx): ").toUpperCase();
            Vehicle vehicle = vehicles.get(carId);

            if (vehicle == null || vehicle.getStatus() != VehicleStatus.AVAILABLE) {
                printlnErr("Invalid Car ID or vehicle is not available."); pause(); return;
            }

            // 2. Driver Selection (Optional)
            Driver driver = null;
            if (read("Include a Driver? (y/n): ").equalsIgnoreCase("y")) {
                List<Driver> availableDrivers = drivers.values().stream()
                        .filter(d -> d.getStatus() == DriverStatus.AVAILABLE)
                        .toList();

                if (availableDrivers.isEmpty()) {
                    printlnErr("No drivers currently available. Booking without driver.");
                } else {
                    displayDrivers(availableDrivers);
                    String driverId = read("Enter Driver ID (Dxxx) or press ENTER to skip: ").toUpperCase();
                    if (!driverId.isEmpty()) {
                        driver = drivers.get(driverId);
                        if (driver == null || driver.getStatus() != DriverStatus.AVAILABLE) {
                            printlnErr("Invalid Driver ID or driver not available. Booking without driver.");
                            driver = null;
                        }
                    }
                }
            }

            // 3. Date and Duration
            LocalDate bookingDate = LocalDate.now();
            int rentalDays = 0;
            while(true) {
                String dateStr = readLineWithValidation("Enter Booking Date (YYYY-MM-DD - must be at least 3 days from today): ", s -> {
                    try {
                        LocalDate date = LocalDate.parse(s, DateTimeFormatter.ISO_DATE);
                        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), date);
                        return days >= 3;
                    } catch (Exception e) { return false; }
                });
                bookingDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
                rentalDays = readInt("Enter Rental Duration (days): ");
                if (rentalDays > 0) break;
                printlnErr("Duration must be greater than 0.");
            }

            double estimatedKm = readDouble("Enter Estimated Total Kilometers: ");

            // 4. Finalize Booking
            String bookingId = String.format("B%04d", bookingCounter++);
            Booking booking = new Booking(bookingId, customer, vehicle, driver, bookingDate, rentalDays, estimatedKm);

            // Pre-calculate fees for display/confirmation
            double initialFee = booking.calculateFinalFee();
            System.out.printf(YELLOW + "\nInitial Payable Amount (LKR %.2f) including LKR %.2f deposit deduction." + RESET, initialFee, booking.deposit);
            read("Press ENTER to confirm booking...");

            // Apply changes
            booking.finalizeBooking();
            bookings.put(bookingId, booking);

            println(GREEN + "\n✅ Booking successful! ID: " + bookingId + RESET);
            pause();
        }

        private void completeBooking() {
            println(CYAN + "\n[11. Complete Booking]" + RESET);
            String bookingId = read("Enter Booking ID (Bxxxx) to complete: ").toUpperCase();
            Booking booking = bookings.get(bookingId);

            if (booking == null || booking.getStatus() != BookingStatus.RESERVED) {
                printlnErr("Booking not found or not in 'RESERVED' status.");
                pause();
                return;
            }

            double actualKm = readDouble("Enter Actual Total Kilometers Used: ");
            if (actualKm < 0 || actualKm < booking.estimatedKm * 0.5) { // Simple check
                printlnErr("Actual distance seems too low or invalid. Please confirm.");
                if (!read("Proceed anyway? (y/n): ").equalsIgnoreCase("y")) {
                    pause(); return;
                }
            }

            // Update status and calculate final fees
            booking.setActualKm(actualKm);
            booking.setStatus(BookingStatus.COMPLETED);
            double finalPayment = booking.calculateFinalFee(); // Recalculate with actual KM

            // Update asset statuses
            booking.getVehicle().setStatus(VehicleStatus.AVAILABLE);
            if (booking.getDriver() != null) {
                booking.getDriver().setStatus(DriverStatus.AVAILABLE);
            }

            println(GREEN + "✅ Booking " + bookingId + " marked as COMPLETED." + RESET);
            booking.getInvoice().display(actualKm, booking.getDriver() != null); // Display final invoice
            pause();
        }

        private void cancelBooking() {
            println(CYAN + "\n[12. Cancel Booking]" + RESET);
            String bookingId = read("Enter Booking ID (Bxxxx) to cancel: ").toUpperCase();
            Booking booking = bookings.get(bookingId);

            if (booking == null || booking.getStatus() != BookingStatus.RESERVED) {
                printlnErr("Booking not found or is not currently reserved.");
                pause();
                return;
            }

            if (!booking.canCancel()) {
                printlnErr(RED + "❌ Cannot cancel. Cancellation window is closed (less than 2 days to pickup)." + RESET);
                pause();
                return;
            }

            if (read("Are you sure you want to cancel booking " + bookingId + "? (y/n): ").equalsIgnoreCase("y")) {
                booking.setStatus(BookingStatus.CANCELLED);
                // Release assets
                booking.getVehicle().setStatus(VehicleStatus.AVAILABLE);
                if (booking.getDriver() != null) {
                    booking.getDriver().setStatus(DriverStatus.AVAILABLE);
                }
                println(GREEN + "✅ Booking " + bookingId + " successfully CANCELLED." + RESET);
            } else {
                println(YELLOW + "Cancellation aborted." + RESET);
            }
            pause();
        }

        private void manageAssetStatus() {
            println(CYAN + "\n[13. Manage Asset Status]" + RESET);
            int type = readInt("Manage (1=Vehicle, 2=Driver): ");

            if (type == 1) {
                String id = read("Enter Vehicle ID (Vxxx): ").toUpperCase();
                Vehicle v = vehicles.get(id);
                if (v == null) { printlnErr("Vehicle not found."); pause(); return; }

                System.out.printf("Current Status: %s. Set to (1=AVAILABLE, 2=UNDER_MAINTENANCE): ", v.getStatus());
                int statusChoice = readInt("");
                if (statusChoice == 1) v.setStatus(VehicleStatus.AVAILABLE);
                else if (statusChoice == 2) v.setStatus(VehicleStatus.UNDER_MAINTENANCE);
                else { printlnErr("Invalid choice."); pause(); return; }

                println(GREEN + "✅ Vehicle " + id + " status updated to " + v.getStatus() + RESET);

            } else if (type == 2) {
                String id = read("Enter Driver ID (Dxxx): ").toUpperCase();
                Driver d = drivers.get(id);
                if (d == null) { printlnErr("Driver not found."); pause(); return; }

                System.out.printf("Current Status: %s. Set to (1=AVAILABLE, 2=ON_LEAVE): ", d.getStatus());
                int statusChoice = readInt("");
                if (statusChoice == 1) d.setStatus(DriverStatus.AVAILABLE);
                else if (statusChoice == 2) d.setStatus(DriverStatus.ON_LEAVE);
                else { printlnErr("Invalid choice."); pause(); return; }

                println(GREEN + "✅ Driver " + id + " status updated to " + d.getStatus() + RESET);

            } else {
                printlnErr("Invalid type selection.");
            }
            pause();
        }

        private void updateCustomerDetails() {
            println(CYAN + "\n[10. Update Customer Details]" + RESET);
            String custId = read("Enter Customer ID (Cxxx): ").toUpperCase();
            Customer customer = customers.get(custId);
            if (customer == null) {
                printlnErr("Customer not found.");
                pause();
                return;
            }

            customer.updateDetailsInteractive(this);
            pause();
        }

        private void searchBooking() {
            println(CYAN + "\n[9. Search Booking]" + RESET);
            String bookingId = read("Enter Booking ID (Bxxxx): ").toUpperCase();
            Booking booking = bookings.get(bookingId);

            if (booking == null) {
                printlnErr("Booking not found.");
            } else {
                System.out.println(BOLD + "\n--- Booking Details: " + bookingId + " ---" + RESET);
                System.out.printf("Status: %s%n", booking.getStatus());
                System.out.printf("Customer: %s (%s)%n", booking.getCustomer().getName(), booking.getCustomer().getCustomerId());
                System.out.printf("Vehicle: %s (%s)%n", booking.getVehicle().getModel(), booking.getVehicle().getCarId());
                if (booking.getDriver() != null) {
                    System.out.printf("Driver: %s (%s)%n", booking.getDriver().getName(), booking.getDriver().getDriverId());
                } else {
                    System.out.println("Driver: N/A");
                }
                System.out.printf("Rental Period: %s for %d days%n", booking.getBookingDate(), booking.rentalDays);

                // Show calculation based on current status
                if (booking.getStatus() == BookingStatus.RESERVED) {
                    System.out.printf(YELLOW + "Estimated Final Fee (before actual KM): LKR %.2f%n" + RESET, booking.calculateFinalFee());
                } else if (booking.getStatus() == BookingStatus.COMPLETED) {
                    System.out.printf("Actual KM Used: %.1f km%n", booking.getActualKm());
                    booking.getInvoice().display(booking.getActualKm(), booking.getDriver() != null); // Recalculate/Display
                }
            }
            pause();
        }


        /* ------------------------------------------------
           DISPLAY METHODS
           ------------------------------------------------ */

        private void displayCustomers() {
            displayCustomers(customers.values().stream().toList());
        }

        private void displayCustomers(List<Customer> list) {
            println(CYAN + "\n--- Customer List (" + list.size() + ") ---" + RESET);
            if (list.isEmpty()) { printlnErr("No customers registered."); pause(); return; }

            // FIX: Updated all table headers to use double borders (║ and ╦)
            System.out.println(WHITE + BOLD + "╔══════════╦══════════════════╦══════════════╦════════════════════════╦══════════════╗" + RESET);
            System.out.printf("║ %-8s ║ %-16s ║ %-12s ║ %-22s ║ %-12s ║%n", "ID", "Name", "Contact", "Email", "License");
            System.out.println(WHITE + BOLD + "╠══════════╬══════════════════╬══════════════╬════════════════════════╬══════════════╣" + RESET);

            for (Customer c : list) { c.displayRow(); }

            System.out.println(WHITE + BOLD + "╚══════════╩══════════════════╩══════════════╩════════════════════════╩══════════════╝" + RESET);
            pause();
        }

        private void displayVehicles() {
            displayVehicles(vehicles.values().stream().toList());
        }

        private void displayVehicles(List<Vehicle> list) {
            println(CYAN + "\n--- Vehicle List (" + list.size() + ") ---" + RESET);
            if (list.isEmpty()) { printlnErr("No vehicles registered."); pause(); return; }

            // FIX: Updated all table headers to use double borders (║ and ╦)
            System.out.println(WHITE + BOLD + "╔══════════╦══════════════════╦════════════════════╦══════════════╦══════════════╗" + RESET);
            System.out.printf("║ %-8s ║ %-18s ║ %-18s ║ %-10s ║ %-12s ║%n", "ID", "Model", "Package", "Fee/Day", "Status");
            System.out.println(WHITE + BOLD + "╠══════════╬══════════════════╬════════════════════╬══════════════╬══════════════╣" + RESET);

            for (Vehicle v : list) { v.displayRow(); }

            System.out.println(WHITE + BOLD + "╚══════════╩══════════════════╩════════════════════╩══════════════╩══════════════╝" + RESET);
            if (list.size() == vehicles.size()) pause();
        }

        private int availableVehiclesCount() {
            return (int) vehicles.values().stream().filter(v -> v.getStatus() == VehicleStatus.AVAILABLE).count();
        }

        private void displayDrivers() {
            displayDrivers(drivers.values().stream().toList());
        }

        private void displayDrivers(List<Driver> list) {
            println(CYAN + "\n--- Driver List (" + list.size() + ") ---" + RESET);
            if (list.isEmpty()) { printlnErr("No drivers registered."); pause(); return; }

            // FIX: Updated all table headers to use double borders (║ and ╦)
            System.out.println(WHITE + BOLD + "╔══════════╦══════════════════╦════════════════╦═══════════════╦════════════╗" + RESET);
            System.out.printf("║ %-8s ║ %-18s ║ %-14s ║ %-13s ║ %-10s ║%n", "ID", "Name", "License No", "Contact", "Status");
            System.out.println(WHITE + BOLD + "╠══════════╬══════════════════╬════════════════╬═══════════════╬════════════╣" + RESET);

            for (Driver d : list) { d.displayRow(); }

            System.out.println(WHITE + BOLD + "╚══════════╩══════════════════╩════════════════╩═══════════════╩════════════╝" + RESET);
            if (list.size() == drivers.size()) pause();
        }

        private int availableDriversCount() {
            return (int) drivers.values().stream().filter(d -> d.getStatus() == DriverStatus.AVAILABLE).count();
        }

        private void displayBookings() {
            println(CYAN + "\n--- Booking List (" + bookings.size() + ") ---" + RESET);
            if (bookings.isEmpty()) { printlnErr("No bookings registered."); pause(); return; }

            // FIX: Updated all table headers to use double borders (║ and ╦)
            System.out.println(WHITE + BOLD + "╔══════════╦══════════╦══════════╦════════════╦════════╦══════════╦══════════╗" + RESET);
            System.out.printf("║ %-8s ║ %-8s ║ %-8s ║ %-10s ║ %-6s ║ %-8s ║ %-8s ║%n", "Bkg ID", "Cust ID", "Car ID", "Status", "Days", "KM Used", "Driver ID");
            System.out.println(WHITE + BOLD + "╠══════════╬══════════╬══════════╬════════════╬════════╬══════════╬══════════╣" + RESET);

            for (Booking b : bookings.values()) { b.displayRow(); }

            System.out.println(WHITE + BOLD + "╚══════════╩══════════╩══════════╩════════════╩════════╩══════════╩══════════╝" + RESET);
            pause();
        }

        private void exitApp() {
            println(RED + "\nGoodbye! Thank you for using the EcoRide Car Rental System." + RESET);
            System.exit(0);
        }

        /* ------------------------------------------------
           INPUT HELPERS
           ------------------------------------------------ */

        private int readInt(String prompt) {
            while (true) {
                System.out.print(prompt);
                String line = sc.nextLine().trim();
                try { return Integer.parseInt(line); }
                catch (NumberFormatException e) { printlnErr("❌ Please enter a valid whole number."); }
            }
        }

        private double readDouble(String prompt) {
            while (true) {
                System.out.print(prompt);
                String line = sc.nextLine().trim();
                try { return Double.parseDouble(line); }
                catch (NumberFormatException e) { printlnErr("❌ Please enter a valid number (e.g., 500.50 or 1200)."); }
            }
        }

        private String read(String prompt) {
            System.out.print(prompt);
            return sc.nextLine().trim();
        }
    }

    /* ===========================
       Utility printing / control
       =========================== */
    private static void println(String s) { System.out.println(s); }
    private static void printlnErr(String s) { System.out.println(RED + s + RESET); }
    private static void pause() {
        System.out.print(GRAY + "\nPress ENTER to continue..." + RESET);
        sc.nextLine();
    }
    private static void clear() {
        // ANSI escape sequence to clear console - may not work in all environments
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    /* ===========================
       MAIN
       =========================== */
    public static void main(String[] args) {
        new App().start();
    }
}