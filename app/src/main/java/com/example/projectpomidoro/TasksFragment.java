package com.example.projectpomidoro;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.List;

public class TasksFragment extends Fragment {
    private DBHelper dbHelper;
    private List<Task> taskList;
    private LinearLayout tasksContainer;
    private EditText editTextNewTask;
    private Button buttonAddTask;
    private TextView textViewTasksStatus;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);

        // Инициализация базы данных
        dbHelper = new DBHelper(getContext());

        tasksContainer = view.findViewById(R.id.tasksContainer);
        editTextNewTask = view.findViewById(R.id.editTextNewTask);
        buttonAddTask = view.findViewById(R.id.buttonAddTask);
        textViewTasksStatus = view.findViewById(R.id.textViewTasksStatus);

        // Загрузка задач
        loadTasks();

        // Обработчик добавления новой задачи
        buttonAddTask.setOnClickListener(v -> addNewTask());

        return view;
    }

    private void loadTasks() {
        // Очистка контейнера
        tasksContainer.removeAllViews();

        // Получение задачи из базы данных
        taskList = dbHelper.getAllTasks();

        // Обновление статуса
        if (taskList.isEmpty()) {
            textViewTasksStatus.setText("Нет задач. Добавьте первую!");
        } else {
            int activeCount = 0;
            for (Task task : taskList) {
                if (task.isActive()) activeCount++;
            }
            textViewTasksStatus.setText("Всего задач: " + taskList.size() + " (активных: " + activeCount + ")");
        }

        // View для каждой задачи
        for (Task task : taskList) {
            View taskView = createTaskView(task);
            tasksContainer.addView(taskView);
        }
    }

    private View createTaskView(Task task) {
        // Надуваем макет задачи
        View taskView = LayoutInflater.from(getContext()).inflate(R.layout.task_item, tasksContainer, false);

        // Находим View элементы
        View priorityIndicator = taskView.findViewById(R.id.priorityIndicator);
        TextView textViewTask = taskView.findViewById(R.id.textViewTask);
        CheckBox checkBoxActive = taskView.findViewById(R.id.checkBoxActive);

        // Устанавливаем данные задачи
        textViewTask.setText(task.getTitle());
        checkBoxActive.setChecked(task.isActive());

        // Устанавливаем цвет индикатора приоритета
        priorityIndicator.setBackgroundColor(task.getPriorityColor());

        // Обработчик клика на индикатор приоритета для смены цвета
        priorityIndicator.setOnClickListener(v -> {
            showPriorityDialog(task);
        });

        // Обработчик изменения галочки активации
        checkBoxActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            dbHelper.updateTaskActive(task.getId(), isChecked);
            String status = isChecked ? "активирована" : "деактивирована";
            Toast.makeText(getContext(), "Задача \"" + task.getTitle() + "\" " + status, Toast.LENGTH_SHORT).show();
            loadTasks(); // Обновляем статус
        });

        return taskView;
    }

    private void addNewTask() {
        String taskTitle = editTextNewTask.getText().toString().trim();
        if (taskTitle.isEmpty()) {
            Toast.makeText(getContext(), "Введите текст задачи", Toast.LENGTH_SHORT).show();
            return;
        }

        // Создаем новую задачу с приоритетом по умолчанию (низкий) и активной
        Task newTask = new Task(true, taskTitle, 1);
        long taskId = dbHelper.addTask(newTask);

        if (taskId != -1) {
            // Успешно добавлено
            editTextNewTask.setText("");
            loadTasks(); // Перезагружаем список
            Toast.makeText(getContext(), "Задача добавлена", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Ошибка добавления задачи", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPriorityDialog(Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Выберите важность задачи:");

        // Создаем кастомный layout для диалога
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.priority_dialog, null);
        builder.setView(dialogView);

        // Находим элементы в диалоге
        TextView textTaskTitle = dialogView.findViewById(R.id.textTaskTitle);
        View colorGreen = dialogView.findViewById(R.id.colorGreen);
        View colorYellow = dialogView.findViewById(R.id.colorYellow);
        View colorRed = dialogView.findViewById(R.id.colorRed);
        TextView textGreen = dialogView.findViewById(R.id.textGreen);
        TextView textYellow = dialogView.findViewById(R.id.textYellow);
        TextView textRed = dialogView.findViewById(R.id.textRed);

        // Установка названия задачи
        textTaskTitle.setText("\"" + task.getTitle() + "\"");

        // Обработчики для выбора цвета
        colorGreen.setOnClickListener(v -> {
            updateTaskPriority(task, 1, "Низкая (зеленый)");
            ((AlertDialog) v.getTag()).dismiss();
        });

        colorYellow.setOnClickListener(v -> {
            updateTaskPriority(task, 2, "Средняя (желтый)");
            ((AlertDialog) v.getTag()).dismiss();
        });

        colorRed.setOnClickListener(v -> {
            updateTaskPriority(task, 3, "Высокая (красный)");
            ((AlertDialog) v.getTag()).dismiss();
        });

        AlertDialog dialog = builder.create();

        // Передаем ссылку на диалог в теги кнопок
        colorGreen.setTag(dialog);
        colorYellow.setTag(dialog);
        colorRed.setTag(dialog);

        dialog.show();
    }

    private void updateTaskPriority(Task task, int priority, String priorityName) {
        dbHelper.updateTaskPriority(task.getId(), priority);
        loadTasks();
        Toast.makeText(getContext(), "Важность изменена на: " + priorityName, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}