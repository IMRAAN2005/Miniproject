package main;

import java.util.*;
import model.ExamHall;
import model.Student;
import service.SeatingService;

public class MainApp {
    public static void main(String[] args) {
        // Dataset (Students)
        List<Student> students = new ArrayList<>();
        students.add(new Student("S001", "Imraan", "CSE", 5, 3, "Math"));
        students.add(new Student("S002", "Rahul", "CSE", 5, 3, "Physics"));
        students.add(new Student("S003", "Anjali", "ECE", 5, 3, "Chemistry"));
        students.add(new Student("S004", "Sneha", "EEE", 5, 3, "Biology"));
        students.add(new Student("S005", "Amit", "MECH", 5, 3, "Math"));

        // Exam Hall
        ExamHall hall = new ExamHall("Hall-101", 10, 2);

        // Seating allocation using service
        SeatingService service = new SeatingService();
        service.addHall(hall);
        service.allocateSeating(students, Arrays.asList(hall));

        System.out.println("Seating generated for " + hall.getHallId() + ". Assigned: " + service.getSeatingForHall(hall.getHallId()).size());
    }
}