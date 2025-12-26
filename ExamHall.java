package model;

public class ExamHall {
    private String hallId;
    private int rows;
    private int cols;

    public ExamHall(String hallId, int rows, int cols) {
        this.hallId = hallId;
        this.rows = rows;
        this.cols = cols;
    }

    public String getHallId() { return hallId; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }

    public void setRows(int rows) { this.rows = rows; }
    public void setCols(int cols) { this.cols = cols; }

    @Override
    public String toString() { return hallId + " (" + rows + "x" + cols + ")"; }
}
