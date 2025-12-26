package model;

public class Student {
    private String roll;
    private String name;
    private String branch;
    private int semester;
    private int year;
    private String subject;

    public Student(String roll, String name, String branch, int semester, int year, String subject) {
        this.roll = roll;
        this.name = name;
        this.branch = branch;
        this.semester = semester;
        this.year = year;
        this.subject = subject;
    }

    public String getRoll() { return roll; }
    public String getName() { return name; }
    public String getBranch() { return branch; }
    public int getSemester() { return semester; }
    public int getYear() { return year; }
    public String getSubject() { return subject; }

    public void setName(String name) { this.name = name; }
    public void setBranch(String branch) { this.branch = branch; }
    public void setSemester(int semester) { this.semester = semester; }
    public void setYear(int year) { this.year = year; }
    public void setSubject(String subject) { this.subject = subject; }

    @Override
    public String toString() { return roll + " - " + name; }
}
