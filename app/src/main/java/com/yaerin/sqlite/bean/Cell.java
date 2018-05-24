package com.yaerin.sqlite.bean;

public class Cell {
    public int col;
    public int row;
    public String oldVal;
    public String newVal;

    public Cell(int col, int row, String oldVal, String newVal) {
        this.col = col;
        this.row = row;
        this.oldVal = oldVal;
        this.newVal = newVal;
    }
}
