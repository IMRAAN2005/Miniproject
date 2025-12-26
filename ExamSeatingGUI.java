package gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import model.ExamHall;
import model.Student;
import service.SeatingService;

/**
 * Professional interactive GUI for university exam seating.
 */
public class ExamSeatingGUI extends JFrame {

    private final SeatingService service = new SeatingService();

    private final DefaultTableModel studentModel = new DefaultTableModel(
            new Object[]{"Roll","Name","Branch","Semester","Year","Subject"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final DefaultTableModel hallModel = new DefaultTableModel(
            new Object[]{"Hall ID","Rows","Cols"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    private JTable studentTable, hallTable;
    private JTextField searchField;
    private JPanel seatingPanel; // holds halls
    private JLabel statusLabel;

    private final Deque<Runnable> undoStack = new ArrayDeque<>();
    private final Deque<Runnable> redoStack = new ArrayDeque<>();

    public ExamSeatingGUI() {
        super("University Exam Seating — Professional");
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1300, 820);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8,8));

        createMenuBar();
        createToolBar();

        // Left: students and halls
        JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        leftSplit.setResizeWeight(0.6);

        studentTable = new JTable(studentModel);
        studentTable.setRowHeight(26);
        JScrollPane stScroll = new JScrollPane(studentTable);
        stScroll.setBorder(BorderFactory.createTitledBorder("Students"));

        hallTable = new JTable(hallModel);
        hallTable.setRowHeight(24);
        JScrollPane hScroll = new JScrollPane(hallTable);
        hScroll.setBorder(BorderFactory.createTitledBorder("Exam Halls"));

        leftSplit.setTopComponent(stScroll);
        leftSplit.setBottomComponent(hScroll);
        leftSplit.setPreferredSize(new Dimension(520, 700));
        add(leftSplit, BorderLayout.WEST);

        // Center seating visualization
        seatingPanel = new JPanel();
        seatingPanel.setLayout(new BoxLayout(seatingPanel, BoxLayout.Y_AXIS));
        JScrollPane center = new JScrollPane(seatingPanel);
        center.setBorder(BorderFactory.createTitledBorder("Seating Plan"));
        add(center, BorderLayout.CENTER);

        // Right: unassigned + guidelines + status
        JPanel right = new JPanel(new BorderLayout(6,6));
        right.setPreferredSize(new Dimension(320,700));

        JList<Student> unassignedList = new JList<>(new DefaultListModel<>());
        JScrollPane unScroll = new JScrollPane(unassignedList);
        unScroll.setBorder(BorderFactory.createTitledBorder("Unassigned Students"));
        right.add(unScroll, BorderLayout.CENTER);

        JTextArea rules = new JTextArea();
        rules.setEditable(false);
        rules.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        rules.setText(buildGuidelinesText());
        rules.setBackground(new Color(250,250,250));
        rules.setBorder(BorderFactory.createTitledBorder("Anti-cheating Guidelines"));
        rules.setRows(8);
        right.add(rules, BorderLayout.NORTH);

        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(new EmptyBorder(6,6,6,6));
        right.add(statusLabel, BorderLayout.SOUTH);

        add(right, BorderLayout.EAST);

        wireActions();
        setVisible(true);
    }

    private void createMenuBar() {
        JMenuBar mb = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem imp = new JMenuItem("Import CSV...");
        JMenuItem exp = new JMenuItem("Export CSV...");
        JMenuItem exit = new JMenuItem("Exit");
        file.add(imp); file.add(exp); file.addSeparator(); file.add(exit);

        imp.addActionListener(e -> importCsv());
        exp.addActionListener(e -> exportCsv());
        exit.addActionListener(e -> System.exit(0));

        JMenu edit = new JMenu("Edit");
        JMenuItem undo = new JMenuItem("Undo");
        JMenuItem redo = new JMenuItem("Redo");
        edit.add(undo); edit.add(redo);
        undo.addActionListener(e -> doUndo());
        redo.addActionListener(e -> doRedo());

        mb.add(file); mb.add(edit);
        setJMenuBar(mb);
    }

    private void createToolBar() {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);

        JButton addStudentBtn = new JButton("➕ Add Student");
        JButton editStudentBtn = new JButton("✏ Edit");
        JButton delStudentBtn = new JButton("🗑 Delete");
        JButton dupStudentBtn = new JButton("⧉ Duplicate");
        JButton addHallBtn = new JButton("🏷 Add Hall");
        JButton genBtn = new JButton("🎯 Generate (Anti-Cheat)");
        JButton exportBtn = new JButton("Export CSV");

        searchField = new JTextField(20);
        searchField.setToolTipText("Search by roll/name/branch/subject...");

        tb.add(addStudentBtn); tb.add(editStudentBtn); tb.add(delStudentBtn); tb.add(dupStudentBtn);
        tb.addSeparator();
        tb.add(addHallBtn);
        tb.addSeparator();
        tb.add(new JLabel("Search: ")); tb.add(searchField);
        tb.addSeparator();
        tb.add(genBtn); tb.add(exportBtn);

        add(tb, BorderLayout.NORTH);

        addStudentBtn.addActionListener(e -> addStudentDialog(null));
        editStudentBtn.addActionListener(e -> editSelectedStudent());
        delStudentBtn.addActionListener(e -> deleteSelectedStudent());
        dupStudentBtn.addActionListener(e -> duplicateSelectedStudent());
        addHallBtn.addActionListener(e -> addHallDialog());
        genBtn.addActionListener(e -> generateSeating());
        exportBtn.addActionListener(e -> exportCsv());
    }

    private void wireActions() {
        studentTable.addMouseListener(new MouseAdapter(){
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) editSelectedStudent();
            }
        });

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void apply() {
                String q = searchField.getText().trim();
                applyFilter(q);
            }
            public void insertUpdate(DocumentEvent e) { apply(); }
            public void removeUpdate(DocumentEvent e) { apply(); }
            public void changedUpdate(DocumentEvent e) { apply(); }
        });
    }

    // ---------------- Student / Hall dialogs ----------------

    private void addStudentDialog(Student prefill) {
        StudentEditorDialog dlg = new StudentEditorDialog(this, prefill);
        dlg.setVisible(true);
        if (!dlg.isOk()) return;
        Student s = dlg.getStudent();

        if (prefill == null) {
            service.addStudent(s);
            studentModel.addRow(new Object[]{ s.getRoll(), s.getName(), s.getBranch(),
                    s.getSemester(), s.getYear(), s.getSubject() });
            undoStack.push(() -> {
                service.removeStudentByRoll(s.getRoll());
                reloadStudentTable();
                statusLabel.setText("Undid add student " + s.getRoll());
            });
            redoStack.clear();
            statusLabel.setText("Added student " + s.getRoll());
        } else {
            Student before = prefill;
            service.updateStudent(s);
            reloadStudentTable();
            undoStack.push(() -> {
                service.updateStudent(before);
                reloadStudentTable();
                statusLabel.setText("Undid edit " + before.getRoll());
            });
            redoStack.clear();
            statusLabel.setText("Updated student " + s.getRoll());
        }
    }

    private void editSelectedStudent() {
        int r = studentTable.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Select a student to edit."); return; }
        int modelRow = studentTable.convertRowIndexToModel(r);
        Student s = tableRowToStudent(modelRow);
        addStudentDialog(s);
    }

    private void deleteSelectedStudent() {
        int r = studentTable.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Select a student to delete."); return; }
        int modelRow = studentTable.convertRowIndexToModel(r);
        Student s = tableRowToStudent(modelRow);
        int ans = JOptionPane.showConfirmDialog(this, "Delete " + s.getRoll() + "?","Confirm",JOptionPane.YES_NO_OPTION);
        if (ans == JOptionPane.YES_OPTION) {
            service.removeStudentByRoll(s.getRoll());
            reloadStudentTable();
            undoStack.push(() -> {
                service.addStudent(s);
                reloadStudentTable();
                statusLabel.setText("Undid delete " + s.getRoll());
            });
            redoStack.clear();
            statusLabel.setText("Deleted " + s.getRoll());
        }
    }

    private void duplicateSelectedStudent() {
        int r = studentTable.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Select a student to duplicate."); return; }
        int modelRow = studentTable.convertRowIndexToModel(r);
        Student s = tableRowToStudent(modelRow);
        Student dup = new Student(s.getRoll() + "_copy", s.getName(), s.getBranch(), s.getSemester(), s.getYear(), s.getSubject());
        service.addStudent(dup);
        studentModel.addRow(new Object[]{dup.getRoll(), dup.getName(), dup.getBranch(), dup.getSemester(), dup.getYear(), dup.getSubject()});
        undoStack.push(() -> {
            service.removeStudentByRoll(dup.getRoll());
            reloadStudentTable();
            statusLabel.setText("Undid duplicate " + dup.getRoll());
        });
        redoStack.clear();
        statusLabel.setText("Duplicated " + s.getRoll());
    }

    private void addHallDialog() {
        JTextField id = new JTextField();
        JTextField rows = new JTextField("5");
        JTextField cols = new JTextField("6");
        Object[] form = {"Hall ID:", id, "Rows:", rows, "Cols:", cols};
        int ans = JOptionPane.showConfirmDialog(this, form, "Add Hall", JOptionPane.OK_CANCEL_OPTION);
        if (ans != JOptionPane.OK_OPTION) return;
        try {
            String hid = id.getText().trim();
            int r = Integer.parseInt(rows.getText().trim());
            int c = Integer.parseInt(cols.getText().trim());
            if (hid.isEmpty()) { JOptionPane.showMessageDialog(this, "Hall ID required"); return; }
            ExamHall h = new ExamHall(hid, r, c);
            service.addHall(h);
            hallModel.addRow(new Object[]{h.getHallId(), h.getRows(), h.getCols()});
            statusLabel.setText("Added hall " + hid);
            undoStack.push(() -> {
                service.removeHallById(hid);
                reloadHallTable();
                statusLabel.setText("Undid add hall " + hid);
            });
            redoStack.clear();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Rows and Cols must be integers");
        }
    }

    // ---------------- Seating ----------------

    private void generateSeating() {
        List<Student> studs = tableAllToStudentList();
        List<ExamHall> halls = tableAllToHallList();
        service.allocateSeating(studs, halls);
        refreshSeatingViewsInteractive();
        statusLabel.setText("Seating generated with anti-cheating rules");
    }

    private void refreshSeatingViewsInteractive() {
        seatingPanel.removeAll();
        for (ExamHall h : tableAllToHallList()) {
            String hid = h.getHallId();
            int rows = h.getRows();
            int cols = h.getCols();
            Student[][] grid = service.getSeatingGrid(hid);

            JPanel hallBox = new JPanel(new BorderLayout(4,4));
            hallBox.setBorder(BorderFactory.createTitledBorder("Hall: " + hid + " (" + rows + "x" + cols + ")"));

            JPanel gridPanel = new JPanel(new GridLayout(rows, cols, 6,6));
            gridPanel.setBackground(Color.WHITE);

            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    Student s = (grid != null && r < grid.length && c < grid[r].length) ? grid[r][c] : null;
                    JButton seat = new JButton();
                    seat.setPreferredSize(new Dimension(110, 36));
                    seat.setFont(new Font("SansSerif", Font.PLAIN, 11));
                    if (s != null) {
                        seat.setText(s.getRoll());
                        seat.setToolTipText("<html><b>" + s.getName() + "</b><br/>" + s.getBranch() + " | " + s.getSubject() + "</html>");
                        seat.setBackground(colorForSubject(s.getSubject()));
                    } else {
                        seat.setText("Empty");
                        seat.setBackground(new Color(230,230,230));
                    }
                    seat.addMouseListener(new SeatMouseAdapter(hid, r, c, seat));
                    gridPanel.add(seat);
                }
            }
            hallBox.add(gridPanel, BorderLayout.CENTER);
            seatingPanel.add(hallBox);
            seatingPanel.add(Box.createVerticalStrut(8));
        }
        seatingPanel.revalidate();
        seatingPanel.repaint();
    }

    // ---------------- Import / Export CSV ----------------

    private void importCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import students/halls CSV");
        int res = chooser.showOpenDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;
        File f = chooser.getSelectedFile();
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            int addedStud = 0, addedHall = 0;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = parseCsvLine(line);
                if (parts.length == 0) continue;
                String type = parts[0].toUpperCase();
                if ("STUDENT".equals(type) && parts.length >= 7) {
                    Student s = new Student(parts[1].trim(), parts[2].trim(), parts[3].trim(),
                            safeParseInt(parts[4], 1), safeParseInt(parts[5], 2025), parts[6].trim());
                    service.addStudent(s);
                    studentModel.addRow(new Object[]{s.getRoll(), s.getName(), s.getBranch(), s.getSemester(), s.getYear(), s.getSubject()});
                    addedStud++;
                } else if ("HALL".equals(type) && parts.length >= 4) {
                    String hid = parts[1].trim();
                    int rows = safeParseInt(parts[2], 5);
                    int cols = safeParseInt(parts[3], 6);
                    if (!hid.isEmpty()) {
                        ExamHall h = new ExamHall(hid, rows, cols);
                        service.addHall(h);
                        hallModel.addRow(new Object[]{hid, rows, cols});
                        addedHall++;
                    }
                }
            }
            statusLabel.setText("Imported " + addedStud + " students, " + addedHall + " halls.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error reading file: " + ex.getMessage());
        }
    }

    private void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export students/halls CSV");
        int res = chooser.showSaveDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;
        File f = chooser.getSelectedFile();
        try (PrintWriter pw = new PrintWriter(f)) {
            for (Student s : tableAllToStudentList()) {
                pw.println(csvJoin("STUDENT", s.getRoll(), s.getName(), s.getBranch(),
                        String.valueOf(s.getSemester()), String.valueOf(s.getYear()), s.getSubject()));
            }
            for (ExamHall h : tableAllToHallList()) {
                pw.println(csvJoin("HALL", h.getHallId(), String.valueOf(h.getRows()), String.valueOf(h.getCols())));
            }
            statusLabel.setText("Exported CSV successfully.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error writing file: " + ex.getMessage());
        }
    }

    // ---------------- Helpers ----------------

    private int safeParseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); }
        catch(Exception e) { return def; }
    }

    private String[] parseCsvLine(String line) {
        return line.split(Pattern.quote(","));
    }

    private String csvJoin(String... fields) {
        return String.join(",", fields);
    }

    private Student tableRowToStudent(int modelRow) {
        return new Student(
                studentModel.getValueAt(modelRow, 0).toString(),
                studentModel.getValueAt(modelRow, 1).toString(),
                studentModel.getValueAt(modelRow, 2).toString(),
                safeParseInt(studentModel.getValueAt(modelRow, 3).toString(),1),
                safeParseInt(studentModel.getValueAt(modelRow, 4).toString(),2025),
                studentModel.getValueAt(modelRow, 5).toString()
        );
    }

    private List<Student> tableAllToStudentList() {
        List<Student> list = new ArrayList<>();
        for (int i = 0; i < studentModel.getRowCount(); i++) list.add(tableRowToStudent(i));
        return list;
    }

    private List<ExamHall> tableAllToHallList() {
        List<ExamHall> list = new ArrayList<>();
        for (int i = 0; i < hallModel.getRowCount(); i++) {
            String hid = hallModel.getValueAt(i,0).toString();
            int r = safeParseInt(hallModel.getValueAt(i,1).toString(),5);
            int c = safeParseInt(hallModel.getValueAt(i,2).toString(),6);
            list.add(new ExamHall(hid, r, c));
        }
        return list;
    }

    private void reloadStudentTable() {
        studentModel.setRowCount(0);
        for (Student s : service.getAllStudents()) {
            studentModel.addRow(new Object[]{s.getRoll(), s.getName(), s.getBranch(), s.getSemester(), s.getYear(), s.getSubject()});
        }
    }

    private void reloadHallTable() {
        hallModel.setRowCount(0);
        for (ExamHall h : service.getAllHalls()) {
            hallModel.addRow(new Object[]{h.getHallId(), h.getRows(), h.getCols()});
        }
    }

    private void doUndo() {
        if (!undoStack.isEmpty()) { Runnable u = undoStack.pop(); u.run(); redoStack.push(u); }
    }

    private void doRedo() {
        if (!redoStack.isEmpty()) { Runnable r = redoStack.pop(); r.run(); undoStack.push(r); }
    }

    private void applyFilter(String q) {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(studentModel);
        studentTable.setRowSorter(sorter);
        if (q.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(q)));
        }
    }

    private Color colorForSubject(String subj) {
        switch(subj.toLowerCase()) {
            case "math": return new Color(200,230,250);
            case "physics": return new Color(250,200,200);
            case "chemistry": return new Color(200,250,200);
            default: return new Color(240,240,240);
        }
    }

    private String buildGuidelinesText() {
        return "1. No two students of same branch adjacent.\n" +
               "2. Rotate subjects in same row.\n" +
               "3. Maintain at least one empty seat between same subject.\n" +
               "4. Ensure visibility for invigilators.\n" +
               "5. Respect exam hall layout rules.";
    }

    // ---------------- Inner classes ----------------

    private class SeatMouseAdapter extends MouseAdapter {
        String hallId; int r,c;
        JButton btn;
        SeatMouseAdapter(String h,int rr,int cc,JButton b){ hallId=h;r=rr;c=cc; btn=b; }
        @Override public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) {
                Student[][] grid = service.getSeatingGrid(hallId);
                Student s = grid[r][c];
                String msg = (s!=null)?("Student: "+s.getRoll()+" - "+s.getName()):"Empty";
                JOptionPane.showMessageDialog(ExamSeatingGUI.this, msg);
            }
        }
    }

    // ---------------- Student Editor Dialog ----------------
    private static class StudentEditorDialog extends JDialog {
        private boolean ok = false;
        private JTextField rollField = new JTextField(15);
        private JTextField nameField = new JTextField(15);
        private JTextField branchField = new JTextField(15);
        private JTextField semesterField = new JTextField(3);
        private JTextField yearField = new JTextField(4);
        private JTextField subjectField = new JTextField(10);

        public StudentEditorDialog(JFrame parent, Student prefill) {
            super(parent, "Student Editor", true);
            JPanel p = new JPanel(new GridLayout(0,2,6,6));
            p.add(new JLabel("Roll:")); p.add(rollField);
            p.add(new JLabel("Name:")); p.add(nameField);
            p.add(new JLabel("Branch:")); p.add(branchField);
            p.add(new JLabel("Semester:")); p.add(semesterField);
            p.add(new JLabel("Year:")); p.add(yearField);
            p.add(new JLabel("Subject:")); p.add(subjectField);
            if (prefill != null) {
                rollField.setText(prefill.getRoll());
                nameField.setText(prefill.getName());
                branchField.setText(prefill.getBranch());
                semesterField.setText(String.valueOf(prefill.getSemester()));
                yearField.setText(String.valueOf(prefill.getYear()));
                subjectField.setText(prefill.getSubject());
            }
            JButton okBtn = new JButton("OK");
            JButton cancelBtn = new JButton("Cancel");
            JPanel btnPanel = new JPanel();
            btnPanel.add(okBtn); btnPanel.add(cancelBtn);
            okBtn.addActionListener(e -> { ok=true; setVisible(false); });
            cancelBtn.addActionListener(e -> { ok=false; setVisible(false); });
            getContentPane().add(p, BorderLayout.CENTER);
            getContentPane().add(btnPanel, BorderLayout.SOUTH);
            pack();
            setLocationRelativeTo(parent);
        }

        public boolean isOk() { return ok; }

        public Student getStudent() {
            return new Student(
                    rollField.getText().trim(),
                    nameField.getText().trim(),
                    branchField.getText().trim(),
                    Integer.parseInt(semesterField.getText().trim()),
                    Integer.parseInt(yearField.getText().trim()),
                    subjectField.getText().trim()
            );
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ExamSeatingGUI::new);
    }
}
