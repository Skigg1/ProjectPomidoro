package com.example.projectpomidoro;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class TaskCompletionDialog {
    private Context context;
    private DBHelper dbHelper;

    public TaskCompletionDialog(Context context, DBHelper dbHelper) {
        this.context = context;
        this.dbHelper = dbHelper;
    }

    public void showCompletionDialog() {
        // Получаем только активные задачи
        List<Task> activeTasks = dbHelper.getAllActiveTasks();

        if (activeTasks.isEmpty()) {
            Toast.makeText(context, "Нет активных задач для завершения", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Какие задачи вы выполнили?");

        // layout для чекбоксов
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        List<CheckBox> checkBoxes = new ArrayList<>();
        List<Task> tasksToShow = new ArrayList<>();

        // Сортируем задачи по приоритету
        List<Task> highPriority = new ArrayList<>();
        List<Task> mediumPriority = new ArrayList<>();
        List<Task> lowPriority = new ArrayList<>();

        for (Task task : activeTasks) {
            switch (task.getPriority()) {
                case 3: highPriority.add(task); break;
                case 2: mediumPriority.add(task); break;
                case 1: lowPriority.add(task); break;
            }
        }

        tasksToShow.addAll(highPriority);
        tasksToShow.addAll(mediumPriority);
        tasksToShow.addAll(lowPriority);

        // Чекбоксы для каждой задачи
        for (Task task : tasksToShow) {
            CheckBox checkBox = new CheckBox(context);
            checkBox.setText(task.getTitle());
            checkBox.setTextColor(0xFFEDEDED);
            checkBox.setTag(task.getId());

            // Индикатор приоритета
            String priorityText = "";
            switch (task.getPriority()) {
                case 1: priorityText = " (низкая)"; break;
                case 2: priorityText = " (средняя)"; break;
                case 3: priorityText = " (высокая)"; break;
            }
            checkBox.setText(checkBox.getText() + priorityText);

            checkBoxes.add(checkBox);
            layout.addView(checkBox);

            // Разделитель (кроме последнего элемента)
            if (tasksToShow.indexOf(task) < tasksToShow.size() - 1) {
                View separator = new View(context);
                separator.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1));
                separator.setBackgroundColor(0xFF5E5E5E);
                layout.addView(separator);
            }
        }

        builder.setView(layout);

        builder.setPositiveButton("Готово", (dialog, which) -> {
            int completedCount = 0;
            for (CheckBox checkBox : checkBoxes) {
                if (checkBox.isChecked()) {
                    int taskId = (int) checkBox.getTag();
                    dbHelper.deleteTask(taskId);
                    completedCount++;
                }
            }

            if (completedCount > 0) {
                Toast.makeText(context, "Завершено задач: " + completedCount, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Ни одна задача не отмечена как выполненная", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> {
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        // Стилизуем кнопки
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(0xFFD32F2F);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFFEDEDED);
    }
}