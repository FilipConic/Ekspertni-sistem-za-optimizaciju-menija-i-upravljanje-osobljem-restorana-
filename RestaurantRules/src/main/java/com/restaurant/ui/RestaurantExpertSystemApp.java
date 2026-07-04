package com.restaurant.ui;

import com.restaurant.model.*;
import com.restaurant.model.MenuItem;
import com.restaurant.repository.MenuItemRepository;
import com.restaurant.repository.ShiftRepository;
import com.restaurant.repository.WaiterRepository;
import com.restaurant.service.DroolsService;
import com.restaurant.rules.backward.BackwardChainingEngine;
import org.kie.api.runtime.KieSession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.kie.api.runtime.rule.FactHandle;

public class RestaurantExpertSystemApp extends JFrame {

    private static final Color ACCENT_COLOR = new Color(0xD3, 0x54, 0x00);
    private static final Color DANGER_COLOR = new Color(0xC0, 0x39, 0x2B);
    private static final Color BACKGROUND_COLOR = new Color(0xFA, 0xFA, 0xFA);
    private static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 15);
    private static final Font FIELD_FONT = new Font("Segoe UI", Font.PLAIN, 15);
    private static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 15);
    private static final Font TAB_FONT = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font OUTPUT_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 14);
    private static final Font HINT_FONT = new Font("Segoe UI", Font.ITALIC, 13);

    private final MenuItemRepository menuItemRepository;
    private final WaiterRepository waiterRepository;
    private final ShiftRepository shiftRepository;

    private final KieSession session;
    private final List<Recommendation> recommendations = new ArrayList<>();
    private final JTextArea output = new JTextArea();

    private final Map<Integer, Waiter> waitersById = new LinkedHashMap<>();
    private final Map<Integer, Shift> shiftsById = new LinkedHashMap<>();
    private final Map<Integer, MenuItem> menuItemsById = new LinkedHashMap<>();

    private final DefaultListModel<Waiter> waiterListModel = new DefaultListModel<>();
    private final DefaultComboBoxModel<Waiter> waiterComboModel = new DefaultComboBoxModel<>();
    private final DefaultComboBoxModel<MenuItem> menuItemComboModel = new DefaultComboBoxModel<>();

    private static final ListCellRenderer<Object> WAITER_RENDERER = (list, value, index, isSelected, cellHasFocus) -> {
        String text = value == null ? "" : ((Waiter) value).getName() + "  (id=" + ((Waiter) value).getId() + ")";
        JLabel label = new JLabel(text);
        label.setFont(FIELD_FONT);
        label.setBorder(new EmptyBorder(6, 10, 6, 10));
        label.setOpaque(true);
        if (isSelected) {
            label.setBackground(list.getSelectionBackground());
            label.setForeground(list.getSelectionForeground());
        } else {
            label.setBackground(list.getBackground());
            label.setForeground(list.getForeground());
        }
        return label;
    };

    private static final ListCellRenderer<Object> MENU_ITEM_RENDERER = (list, value, index, isSelected, cellHasFocus) -> {
        String text = value == null ? "" : ((MenuItem) value).getName() + "  (id=" + ((MenuItem) value).getId() + ")";
        JLabel label = new JLabel(text);
        label.setFont(FIELD_FONT);
        label.setBorder(new EmptyBorder(6, 10, 6, 10));
        label.setOpaque(true);
        if (isSelected) {
            label.setBackground(list.getSelectionBackground());
            label.setForeground(list.getSelectionForeground());
        } else {
            label.setBackground(list.getBackground());
            label.setForeground(list.getForeground());
        }
        return label;
    };

    public RestaurantExpertSystemApp() throws SQLException {
        super("Restaurant Expert System");

        Connection connection = connectSQL();
        this.menuItemRepository = new MenuItemRepository(connection);
        this.waiterRepository = new WaiterRepository(connection);
        this.shiftRepository = new ShiftRepository(connection);

        DroolsService droolsService = new DroolsService();
        this.session = droolsService.newPersistentSession(recommendations);

        loadPersistedData();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BACKGROUND_COLOR);
        add(buildTabs(), BorderLayout.WEST);
        add(buildOutputPanel(), BorderLayout.CENTER);
        setSize(1150, 700);
        setLocationRelativeTo(null);

        log("Loaded from database: " + waitersById.size() + " waiter(s), "
                + menuItemsById.size() + " menu item(s), " + shiftsById.size() + " shift(s).");
    }

    private static Connection connectSQL() throws SQLException {
        var connection = DriverManager.getConnection("jdbc:sqlite:main.db");
        try (var stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return connection;
    }

    private void loadPersistedData() throws SQLException {
        for (Waiter w : waiterRepository.getAll()) {
            waitersById.put(w.getId(), w);
            session.insert(w);
        }
        for (MenuItem m : menuItemRepository.getAll()) {
            menuItemsById.put(m.getId(), m);
            session.insert(m);
        }
        for (Shift s : shiftRepository.getAll()) {
            shiftsById.put(s.getId(), s);
            session.insert(s);
        }
        refreshWaiterWidgets();
        refreshMenuItemWidgets();
    }

    private void refreshWaiterWidgets() {
        waiterListModel.clear();
        waiterComboModel.removeAllElements();
        for (Waiter w : waitersById.values()) {
            waiterListModel.addElement(w);
            waiterComboModel.addElement(w);
        }
    }

    private void refreshMenuItemWidgets() {
        menuItemComboModel.removeAllElements();
        for (MenuItem m : menuItemsById.values()) {
            menuItemComboModel.addElement(m);
        }
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(TAB_FONT);
        tabs.addTab("Waiter", buildWaiterForm());
        tabs.addTab("Shift", buildShiftForm());
        tabs.addTab("Menu Item", buildMenuItemForm());
        tabs.addTab("Forward Chaining", buildForwardPanel());
        tabs.addTab("Backward Chaining", buildBackwardPanel());
        tabs.addTab("CEP", buildCepPanel());
        tabs.setPreferredSize(new Dimension(460, 700));
        return tabs;
    }

    private JScrollPane buildOutputPanel() {
        output.setEditable(false);
        output.setFont(OUTPUT_FONT);
        output.setBackground(Color.WHITE);
        output.setBorder(new EmptyBorder(12, 14, 12, 14));
        JScrollPane scroll = new JScrollPane(output);
        scroll.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(0xDD, 0xDD, 0xDD)));
        return scroll;
    }

    private void log(String msg) {
        output.append(msg + "\n");
        output.setCaretPosition(output.getDocument().getLength());
    }

    private JPanel tabPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(BACKGROUND_COLOR);
        return panel;
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(LABEL_FONT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(new EmptyBorder(10, 0, 4, 0));
        return label;
    }

    private JTextField createTextField(String initial) {
        JTextField field = new JTextField(initial);
        field.setFont(FIELD_FONT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xCC, 0xCC, 0xCC)),
                new EmptyBorder(6, 10, 6, 10)));
        return field;
    }

    private JTextField createTextField() {
        return createTextField("");
    }

    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setForeground(Color.WHITE);
        button.setBackground(ACCENT_COLOR);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(12, 26, 12, 26));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        return button;
    }

    private void addFieldRow(JPanel panel, String labelText, JComponent field) {
        panel.add(createLabel(labelText));
        panel.add(field);
    }

    private JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setForeground(ACCENT_COLOR);
        button.setBackground(Color.WHITE);
        button.setOpaque(true);
        button.setBorderPainted(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_COLOR, 2),
                new EmptyBorder(10, 24, 10, 24)));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JButton createDangerButton(String text) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setForeground(Color.WHITE);
        button.setBackground(DANGER_COLOR);
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(10, 22, 10, 22));
        return button;
    }

    private JTextField createIdField() {
        JTextField field = createTextField();
        field.setMaximumSize(new Dimension(110, 36));
        return field;
    }

    private int retractMatchingFacts(Predicate<Object> predicate) {
        List<FactHandle> toDelete = new ArrayList<>(session.getFactHandles(predicate::test));
        for (FactHandle handle : toDelete) {
            session.delete(handle);
        }
        return toDelete.size();
    }

    private boolean confirmDelete(String description) {
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete " + description + "? This cannot be undone.",
                "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }

    private JPanel buttonRow(JComponent... components) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setOpaque(false);
        for (int i = 0; i < components.length; i++) {
            row.add(components[i]);
            if (i < components.length - 1) row.add(Box.createHorizontalStrut(12));
        }
        return row;
    }

    private void printTable(String title, String[] headers, List<Object[]> rows) {
        log("");
        log("=== " + title + " (" + rows.size() + ") ===");
        if (rows.isEmpty()) {
            log(" (no records)");
            return;
        }
        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) widths[i] = headers[i].length();
        for (Object[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                widths[i] = Math.max(widths[i], String.valueOf(row[i]).length());
            }
        }
        log(formatTableRow(headers, widths));
        log(formatTableSeparator(widths));
        for (Object[] row : rows) {
            String[] cells = new String[row.length];
            for (int i = 0; i < row.length; i++) cells[i] = String.valueOf(row[i]);
            log(formatTableRow(cells, widths));
        }
    }

    private String formatTableRow(String[] cells, int[] widths) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            sb.append(String.format("%-" + widths[i] + "s", cells[i]));
            if (i < cells.length - 1) sb.append(" | ");
        }
        return sb.toString();
    }

    private String formatTableSeparator(int[] widths) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < widths.length; i++) {
            sb.append("-".repeat(widths[i]));
            if (i < widths.length - 1) sb.append("-+-");
        }
        return sb.toString();
    }

    private JPanel buildWaiterForm() {
        JTextField name = createTextField();
        JTextField rating = createTextField();
        JTextField tables = createTextField();
        JTextField orderValue = createTextField();

        JPanel panel = tabPanel();
        addFieldRow(panel, "Name", name);
        addFieldRow(panel, "Guest Rating (0-5)", rating);
        addFieldRow(panel, "Avg Tables per Shift", tables);
        addFieldRow(panel, "Avg Order Value", orderValue);
        panel.add(Box.createVerticalStrut(16));

        JButton add = createButton("Add Waiter");
        add.addActionListener(e -> {
            try {
                Waiter w = new Waiter();
                w.setName(name.getText().trim());
                w.setGuestRating(Double.parseDouble(rating.getText().trim()));
                w.setAvgTablesPerShift(Double.parseDouble(tables.getText().trim()));
                w.setAvgOrderValue(Double.parseDouble(orderValue.getText().trim()));

                waiterRepository.save(w);
                waitersById.put(w.getId(), w);
                refreshWaiterWidgets();

                session.insert(w);
                log("[+] Saved waiter: " + w.getName() + " (id=" + w.getId() + ")");
            } catch (Exception ex) {
                showError(ex);
            }
        });

        JButton showAll = createSecondaryButton("Show All Waiters");
        showAll.addActionListener(e -> {
            try {
                List<Waiter> all = waiterRepository.getAll();
                List<Object[]> rows = new ArrayList<>();
                for (Waiter w : all) {
                    rows.add(new Object[]{w.getId(), w.getName(), w.getAvgTablesPerShift(),
                            w.getAvgOrderValue(), w.getGuestRating()});
                }
                printTable("WAITERS", new String[]{"ID", "Name", "AvgTables", "AvgOrderValue", "GuestRating"}, rows);
            } catch (Exception ex) {
                showError(ex);
            }
        });

        panel.add(buttonRow(add, showAll));

        panel.add(Box.createVerticalStrut(20));
        panel.add(createLabel("Delete by ID"));
        JTextField deleteId = createIdField();
        JButton delete = createDangerButton("Delete Waiter");
        delete.addActionListener(e -> {
            try {
                int id = Integer.parseInt(deleteId.getText().trim());
                Waiter existing = waitersById.get(id);
                String label = "waiter id=" + id + (existing != null ? " (" + existing.getName() + ")" : "");
                if (!confirmDelete(label)) return;

                waiterRepository.delete(id);
                int retracted = retractMatchingFacts(o -> o instanceof Waiter && ((Waiter) o).getId() == id);
                retracted += retractMatchingFacts(o -> o instanceof WaiterFact && ((WaiterFact) o).getWaiterId() == id);
                waitersById.remove(id);
                refreshWaiterWidgets();

                log("[-] Deleted " + label + " (" + retracted + " fact(s) retracted from session)");
            } catch (Exception ex) {
                showError(ex);
            }
        });
        panel.add(buttonRow(deleteId, delete));

        return panel;
    }
    private JPanel buildShiftForm() {
        JTextField date = createTextField(LocalDate.now().toString());
        JTextField guests = createTextField();
        JTextField earnings = createTextField();

        JList<Waiter> waiterJList = new JList<>(waiterListModel);
        waiterJList.setFont(FIELD_FONT);
        waiterJList.setCellRenderer(WAITER_RENDERER);
        waiterJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        waiterJList.setVisibleRowCount(6);
        JScrollPane waiterScroll = new JScrollPane(waiterJList);
        waiterScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        waiterScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));

        JPanel panel = tabPanel();
        addFieldRow(panel, "Date (YYYY-MM-DD)", date);
        addFieldRow(panel, "Guest Count", guests);
        addFieldRow(panel, "Total Earnings", earnings);
        panel.add(createLabel("Waiters on Shift (Ctrl/Shift for multiple)"));
        panel.add(waiterScroll);
        panel.add(Box.createVerticalStrut(16));

        JButton add = createButton("Add Shift");
        add.addActionListener(e -> {
            try {
                List<Integer> waiterIds = waiterJList.getSelectedValuesList()
                        .stream().map(Waiter::getId).collect(Collectors.toList());

                Shift s = new Shift();
                s.setDate(LocalDate.parse(date.getText().trim()));
                s.setGuestCount(Integer.parseInt(guests.getText().trim()));
                s.setTotalEarnings(Double.parseDouble(earnings.getText().trim()));
                s.setWaiterIds(waiterIds);

                shiftRepository.save(s);
                for (int waiterId : waiterIds) {
                    shiftRepository.connectWaiter(s.getId(), waiterId);
                }
                shiftsById.put(s.getId(), s);

                session.insert(s);
                log("[+] Saved shift id=" + s.getId() + " with " + waiterIds.size() + " waiter(s).");
            } catch (Exception ex) {
                showError(ex);
            }
        });

        JButton showAll = createSecondaryButton("Show All Shifts");
        showAll.addActionListener(e -> {
            try {
                List<Shift> all = shiftRepository.getAll();
                List<Object[]> rows = new ArrayList<>();
                for (Shift s : all) {
                    rows.add(new Object[]{s.getId(), s.getDate(), s.getWaiterIds(),
                            s.getGuestCount(), s.getTotalEarnings()});
                }
                printTable("SHIFTS", new String[]{"ID", "Date", "WaiterIds", "Guests", "TotalEarnings"}, rows);
            } catch (Exception ex) {
                showError(ex);
            }
        });

        panel.add(buttonRow(add, showAll));

        panel.add(Box.createVerticalStrut(20));
        panel.add(createLabel("Delete by ID"));
        JTextField deleteId = createIdField();
        JButton delete = createDangerButton("Delete Shift");
        delete.addActionListener(e -> {
            try {
                int id = Integer.parseInt(deleteId.getText().trim());
                Shift existing = shiftsById.get(id);
                String label = "shift id=" + id;
                if (!confirmDelete(label)) return;

                shiftRepository.delete(id);
                int retracted = retractMatchingFacts(o -> o instanceof Shift && ((Shift) o).getId() == id);
                if (existing != null) {
                    long sameDateCount = shiftsById.values().stream()
                            .filter(s -> s.getId() != id && existing.getDate().equals(s.getDate()))
                            .count();
                    if (sameDateCount == 0) {
                        retracted += retractMatchingFacts(o -> o instanceof ShiftFact
                                && existing.getDate().equals(((ShiftFact) o).getDate()));
                    }
                }
                shiftsById.remove(id);

                log("[-] Deleted " + label + " (" + retracted + " fact(s) retracted from session)");
            } catch (Exception ex) {
                showError(ex);
            }
        });
        panel.add(buttonRow(deleteId, delete));

        return panel;
    }

    private JPanel buildMenuItemForm() {
        JTextField name = createTextField();
        JTextField price = createTextField();
        JTextField cost = createTextField();
        JTextField sales = createTextField();
        JTextField category = createTextField();

        JPanel panel = tabPanel();
        addFieldRow(panel, "Item Name", name);
        addFieldRow(panel, "Price", price);
        addFieldRow(panel, "Preparation Cost", cost);
        addFieldRow(panel, "Weekly Sales", sales);
        addFieldRow(panel, "Category", category);
        panel.add(Box.createVerticalStrut(16));

        JButton add = createButton("Add Menu Item");
        add.addActionListener(e -> {
            try {
                MenuItem m = new MenuItem();
                m.setName(name.getText().trim());
                m.setPrice(Double.parseDouble(price.getText().trim()));
                m.setPreparationCost(Double.parseDouble(cost.getText().trim()));
                m.setWeeklySales(Integer.parseInt(sales.getText().trim()));
                m.setCategory(category.getText().trim());

                menuItemRepository.save(m);
                menuItemsById.put(m.getId(), m);
                refreshMenuItemWidgets();

                session.insert(m);
                log("[+] Saved menu item: " + m.getName() + " (id=" + m.getId() + ")");
            } catch (Exception ex) {
                showError(ex);
            }
        });

        JButton showAll = createSecondaryButton("Show All Menu Items");
        showAll.addActionListener(e -> {
            try {
                List<MenuItem> all = menuItemRepository.getAll();
                List<Object[]> rows = new ArrayList<>();
                for (MenuItem m : all) {
                    rows.add(new Object[]{m.getId(), m.getName(), m.getCategory(),
                            m.getPrice(), m.getPreparationCost(), m.getWeeklySales()});
                }
                printTable("MENU ITEMS", new String[]{"ID", "Name", "Category", "Price", "PrepCost", "WeeklySales"}, rows);
            } catch (Exception ex) {
                showError(ex);
            }
        });

        panel.add(buttonRow(add, showAll));

        panel.add(Box.createVerticalStrut(20));
        panel.add(createLabel("Delete by ID"));
        JTextField deleteId = createIdField();
        JButton delete = createDangerButton("Delete Menu Item");
        delete.addActionListener(e -> {
            try {
                int id = Integer.parseInt(deleteId.getText().trim());
                MenuItem existing = menuItemsById.get(id);
                String label = "menu item id=" + id + (existing != null ? " (" + existing.getName() + ")" : "");
                if (!confirmDelete(label)) return;

                menuItemRepository.delete(id);
                int retracted = retractMatchingFacts(o -> o instanceof MenuItem && ((MenuItem) o).getId() == id);
                if (existing != null) {
                    retracted += retractMatchingFacts(o -> o instanceof MenuItemFact
                            && existing.getName().equals(((MenuItemFact) o).getMenuItemName()));
                }
                menuItemsById.remove(id);
                refreshMenuItemWidgets();

                log("[-] Deleted " + label + " (" + retracted + " fact(s) retracted from session)");
            } catch (Exception ex) {
                showError(ex);
            }
        });
        panel.add(buttonRow(deleteId, delete));

        return panel;
    }

    private JPanel buildForwardPanel() {
        JPanel panel = tabPanel();
        JButton run = createButton("Run Forward Chaining");
        run.addActionListener(e -> {
            recommendations.clear();
            int fired = session.fireAllRules();
            log("=== FORWARD CHAINING (" + fired + " rule(s) fired) ===");
            for (Recommendation r : recommendations) {
                log(" - [" + r.getType() + "] " + r.getSubject() + ": " + r.getExplanation());
            }
            if (recommendations.isEmpty()) log(" (no new recommendations)");
        });
        panel.add(run);
        return panel;
    }

    private JPanel buildBackwardPanel() {
        JPanel panel = tabPanel();

        JComboBox<String> goal = new JComboBox<>(new String[]{
                "Should this waiter be fired?",
                "Should this waiter receive a reward?",
                "Should this menu item be promoted?",
                "Should this menu item be removed?"
        });
        goal.setFont(FIELD_FONT);
        goal.setAlignmentX(Component.LEFT_ALIGNMENT);
        goal.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JComboBox<Waiter> waiterCombo = new JComboBox<>(waiterComboModel);
        waiterCombo.setRenderer(WAITER_RENDERER);
        waiterCombo.setFont(FIELD_FONT);
        waiterCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        waiterCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JComboBox<MenuItem> itemCombo = new JComboBox<>(menuItemComboModel);
        itemCombo.setRenderer(MENU_ITEM_RENDERER);
        itemCombo.setFont(FIELD_FONT);
        itemCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        itemCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JPanel waiterInputPanel = new JPanel();
        waiterInputPanel.setLayout(new BoxLayout(waiterInputPanel, BoxLayout.Y_AXIS));
        waiterInputPanel.setOpaque(false);
        waiterInputPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        waiterInputPanel.add(createLabel("Waiter"));
        waiterInputPanel.add(waiterCombo);

        JPanel itemInputPanel = new JPanel();
        itemInputPanel.setLayout(new BoxLayout(itemInputPanel, BoxLayout.Y_AXIS));
        itemInputPanel.setOpaque(false);
        itemInputPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        itemInputPanel.add(createLabel("Menu item"));
        itemInputPanel.add(itemCombo);

        panel.add(createLabel("Goal"));
        panel.add(goal);
        panel.add(waiterInputPanel);
        panel.add(itemInputPanel);
        panel.add(Box.createVerticalStrut(16));

        Runnable updateVisibleInput = () -> {
            boolean isWaiterGoal = goal.getSelectedIndex() <= 1;
            waiterInputPanel.setVisible(isWaiterGoal);
            itemInputPanel.setVisible(!isWaiterGoal);
            panel.revalidate();
            panel.repaint();
        };
        goal.addActionListener(e -> updateVisibleInput.run());
        updateVisibleInput.run();

        JButton ask = createButton("Ask");
        ask.addActionListener(e -> {
            BackwardChainingEngine engine = new BackwardChainingEngine(session);
            boolean result;
            log("=== BACKWARD CHAINING ===");
            switch (goal.getSelectedIndex()) {
                case 0: {
                    Waiter selected = (Waiter) waiterCombo.getSelectedItem();
                    if (selected == null) {
                        log("Add at least one waiter first.");
                        return;
                    }
                    result = engine.shouldFireWaiter(selected.getId());
                    break;
                }
                case 1: {
                    Waiter selected = (Waiter) waiterCombo.getSelectedItem();
                    if (selected == null) {
                        log("Add at least one waiter first.");
                        return;
                    }
                    result = engine.shouldRewardWaiter(selected.getId());
                    break;
                }
                case 2: {
                    MenuItem selected = (MenuItem) itemCombo.getSelectedItem();
                    if (selected == null) {
                        log("Add at least one menu item first.");
                        return;
                    }
                    result = engine.shouldPromoteItem(selected.getName());
                    break;
                }
                case 3: {
                    MenuItem selected = (MenuItem) itemCombo.getSelectedItem();
                    if (selected == null) {
                        log("Add at least one menu item first.");
                        return;
                    }
                    result = engine.shouldRemoveItem(selected.getName());
                    break;
                }
                default:
                    throw new IllegalStateException("Unknown goal index: " + goal.getSelectedIndex());
            }
            for (String step : engine.getTrace()) log(step);
            log("ANSWER: " + (result ? "YES" : "NO"));
            log("");
        });
        panel.add(ask);

        return panel;
    }

    private JPanel buildCepPanel() {
        JTextField shiftId = createTextField();
        JTextField guestCount = createTextField();
        JTextField waiterCount = createTextField();

        JPanel panel = tabPanel();
        addFieldRow(panel, "Shift ID (integer)", shiftId);
        addFieldRow(panel, "Current Guest Count", guestCount);
        addFieldRow(panel, "Active Waiters", waiterCount);
        panel.add(Box.createVerticalStrut(16));

        JButton send = createCepSendButton(shiftId, guestCount, waiterCount);
        panel.add(send);

        panel.add(Box.createVerticalStrut(14));
        JLabel hint = new JLabel("<html>Click a few times in a row to simulate a sustained<br>"
                + "high guest-to-waiter ratio (window: 20s).<br>"
                + "Shift ID here is just an event-stream tag - it doesn't<br>"
                + "need to match a real saved shift.</html>");
        hint.setFont(HINT_FONT);
        hint.setForeground(new Color(0x66, 0x66, 0x66));
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(hint);

        return panel;
    }

    private JButton createCepSendButton(JTextField shiftId, JTextField guestCount, JTextField waiterCount) {
        JButton send = createButton("Send Snapshot (CEP Event)");
        send.addActionListener(e -> {
            try {
                StaffingSnapshotEvent event = new StaffingSnapshotEvent(
                        Integer.parseInt(shiftId.getText().trim()),
                        Integer.parseInt(guestCount.getText().trim()),
                        Integer.parseInt(waiterCount.getText().trim()));
                session.insert(event);
                int fired = session.fireAllRules();
                log("[CEP] " + event + " (" + fired + " rule(s) fired)");
                for (Recommendation r : recommendations) {
                    if (r.getType() == Recommendation.Type.SURGE_PRICING) {
                        log(" -> " + r.getExplanation());
                    }
                }
            } catch (Exception ex) {
                showError(ex);
            }
        });
        return send;
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new RestaurantExpertSystemApp().setVisible(true);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null,
                        "Could not connect to database: " + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
