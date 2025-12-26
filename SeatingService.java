package service;

import java.util.*;
import model.ExamHall;
import model.Student;

public class SeatingService {
    private final List<Student> students = new ArrayList<>();
    private final List<ExamHall> halls = new ArrayList<>();
    private final Map<String, Student[][]> seatingMap = new HashMap<>();

    // ---------------- Student operations ----------------
    public void addStudent(Student s) { students.add(s); }

    public void removeStudentByRoll(String roll) {
        students.removeIf(s -> s.getRoll().equals(roll));
    }

    public void updateStudent(Student s) {
        for (int i = 0; i < students.size(); i++)
            if (students.get(i).getRoll().equals(s.getRoll())) { students.set(i, s); break; }
    }

    public List<Student> getStudents() { return new ArrayList<>(students); }

    // Added for GUI compatibility
    public List<Student> getAllStudents() { return new ArrayList<>(students); }

    // ---------------- Hall operations ----------------
    public void addHall(ExamHall h) {
        halls.add(h);
        seatingMap.put(h.getHallId(), new Student[h.getRows()][h.getCols()]);
    }

    public void removeHallById(String id) {
        halls.removeIf(h -> h.getHallId().equals(id));
        seatingMap.remove(id);
    }

    public List<ExamHall> getHalls() { return new ArrayList<>(halls); }

    // Added for GUI compatibility
    public List<ExamHall> getAllHalls() { return new ArrayList<>(halls); }

    // ---------------- Seating ----------------
    public void allocateSeating(List<Student> studs, List<ExamHall> halls) {
        seatingMap.clear();
        int idx = 0;
        for (ExamHall h : halls) {
            Student[][] grid = new Student[h.getRows()][h.getCols()];
            for (int r = 0; r < h.getRows(); r++) {
                for (int c = 0; c < h.getCols(); c++) {
                    if (idx < studs.size()) grid[r][c] = studs.get(idx++);
                }
            }
            seatingMap.put(h.getHallId(), grid);
        }
    }

    public Student[][] getSeatingGrid(String hallId) { return seatingMap.get(hallId); }
    public void setSeating(String hallId, Student[][] grid) { seatingMap.put(hallId, grid); }

    public List<Student> getSeatingForHall(String hallId) {
        Student[][] grid = seatingMap.get(hallId);
        List<Student> lst = new ArrayList<>();
        if (grid != null)
            for (Student[] row : grid)
                for (Student s : row)
                    if (s != null) lst.add(s);
        return lst;
    }
}
