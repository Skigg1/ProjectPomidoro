package com.example.projectpomidoro;

import android.graphics.Color;

public class Task {
    private int id;
    private boolean isActive;
    private String title;
    private int priority; // 1 - зеленый, 2 - желтый, 3 - красный

    // Конструкторы
    public Task() {}

    public Task(boolean isActive, String title, int priority) {
        this.isActive = isActive;
        this.title = title;
        this.priority = priority;
    }

    public Task(int id, boolean isActive, String title, int priority) {
        this.id = id;
        this.isActive = isActive;
        this.title = title;
        this.priority = priority;
    }

    // Геттеры и сеттеры
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    // Получение цвета приоритета
    public int getPriorityColor() {
        switch (priority) {
            case 1: return Color.parseColor("#4CAF50"); // Зеленый
            case 2: return Color.parseColor("#FFC107"); // Желтый
            case 3: return Color.parseColor("#F44336"); // Красный
            default: return Color.parseColor("#9E9E9E"); // Серый
        }
    }

    // Получение названия приоритета
    public String getPriorityName() {
        switch (priority) {
            case 1: return "Низкий";
            case 2: return "Средний";
            case 3: return "Высокий";
            default: return "Не указан";
        }
    }

    @Override
    public String toString() {
        return title + " (" + getPriorityName() + ")";
    }
}
