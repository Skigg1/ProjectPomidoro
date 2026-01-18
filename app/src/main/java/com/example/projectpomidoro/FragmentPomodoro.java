package com.example.projectpomidoro;

import android.content.pm.PackageManager;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.Random;

public class FragmentPomodoro extends Fragment {

    private static final String CHANNEL_ID = "pomodoro_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final int NOTIFICATION_PERMISSION_CODE = 1001;

    // Тайминги Pomodoro (в миллисекундах)
    private static final long WORK_TIME = 25 * 60 * 1000; // 25 минут
    private static final long BREAK_TIME = 5 * 60 * 1000; // 5 минут

    private static final int DIRECTION_TOP = 0;
    private static final int DIRECTION_BOTTOM = 1;
    private static final int DIRECTION_LEFT = 2;
    private static final int DIRECTION_RIGHT = 3;

    private TextView textViewTimer, textViewStatus, textViewSessionCount;
    private ImageButton imageButtonStart;
    private ImageView imageTomatoColor, imageTomatoBW;
    private Button buttonStop;

    private CountDownTimer countDownTimer;
    private long timeLeftInMillis;
    private boolean timerRunning;
    private boolean isWorkTime = true;
    private int sessionCount = 0;
    private int currentFillDirection;
    private Random random;
    private ClipDrawable clipDrawable;

    private NotificationManager notificationManager;
    private DBHelper dbHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pomodoro, container, false);

        initViews(view);
        createNotificationChannel();
        random = new Random();

        // Инициализация базы данных
        dbHelper = new DBHelper(getContext());

        imageButtonStart.setOnClickListener(v -> checkPermissionsAndStartTimer());
        buttonStop.setOnClickListener(v -> stopTimer());

        return view;
    }

    private void initViews(View view) {
        textViewTimer = view.findViewById(R.id.textViewTimer);
        textViewStatus = view.findViewById(R.id.textViewStatus);
        textViewSessionCount = view.findViewById(R.id.textViewSessionCount);
        imageButtonStart = view.findViewById(R.id.imageButtonStart);
        imageTomatoColor = view.findViewById(R.id.imageTomatoColor);
        imageTomatoBW = view.findViewById(R.id.imageTomatoBW);
        buttonStop = view.findViewById(R.id.buttonStop);

        Drawable drawable = imageTomatoColor.getDrawable();
        if (drawable instanceof ClipDrawable) {
            clipDrawable = (ClipDrawable) drawable;
            // Изначально показываем полностью цветной помидор
            clipDrawable.setLevel(10000);
        } else {
            imageTomatoColor.setImageResource(R.drawable.pomidor);
        }

        // Скрываем черно-белый помидор изначально
        imageTomatoBW.setVisibility(View.GONE);

        notificationManager = requireContext().getSystemService(NotificationManager.class);
    }

    private void checkPermissionsAndStartTimer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    android.Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);

                Toast.makeText(requireContext(), "Разрешите уведомления для работы таймера", Toast.LENGTH_LONG).show();
                return;
            }
        }
        startTimer();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startTimer();
                Toast.makeText(requireContext(), "Разрешение получено! Таймер запущен", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(),
                        "Без разрешения уведомления не будут работать, но таймер запустится",
                        Toast.LENGTH_LONG).show();
                startTimer();
            }
        }
    }

    private void startTimer() {
        if (!timerRunning) {
            timeLeftInMillis = isWorkTime ? WORK_TIME : BREAK_TIME;

            currentFillDirection = random.nextInt(4);

            imageTomatoBW.setVisibility(View.VISIBLE);

            setupClipDirection();

            updateTomatoProgress(0.0f);

            countDownTimer = new CountDownTimer(timeLeftInMillis, 50) {
                @Override
                public void onTick(long millisUntilFinished) {
                    timeLeftInMillis = millisUntilFinished;
                    updateTimer();

                    float progress = 1.0f - ((float) timeLeftInMillis / (isWorkTime ? WORK_TIME : BREAK_TIME));
                    updateTomatoProgress(progress);
                }

                @Override
                public void onFinish() {
                    timerRunning = false;
                    sessionCompleted();
                }
            }.start();

            timerRunning = true;
            updateButtons();
            textViewStatus.setText(isWorkTime ? "Время работать!" : "Время отдыхать!");
        }
    }

    private void setupClipDirection() {
        // Вместо создания нового ClipDrawable, используем существующий из XML
        Drawable drawable = imageTomatoColor.getDrawable();
        if (drawable instanceof ClipDrawable) {
            clipDrawable = (ClipDrawable) drawable;
        } else {
            int orientation = ClipDrawable.HORIZONTAL;
            int gravity = 0;

            switch (currentFillDirection) {
                case DIRECTION_TOP:
                    orientation = ClipDrawable.VERTICAL;
                    gravity = android.view.Gravity.BOTTOM;
                    break;
                case DIRECTION_BOTTOM:
                    orientation = ClipDrawable.VERTICAL;
                    gravity = android.view.Gravity.TOP;
                    break;
                case DIRECTION_LEFT:
                    orientation = ClipDrawable.HORIZONTAL;
                    gravity = android.view.Gravity.RIGHT;
                    break;
                case DIRECTION_RIGHT:
                    orientation = ClipDrawable.HORIZONTAL;
                    gravity = android.view.Gravity.LEFT;
                    break;
            }

            clipDrawable = new ClipDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.pomidor),
                    gravity,
                    orientation
            );
            imageTomatoColor.setImageDrawable(clipDrawable);
        }

        // Начинаем с полностью скрытого (серый будет виден)
        clipDrawable.setLevel(0);
    }

    private void stopTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        timerRunning = false;

        // Показываем диалог выбора выполненных задач
        showTaskCompletionDialog();

        // Возвращаем обычное цветное изображение
        imageTomatoColor.setImageResource(R.drawable.pomidor);
        // Скрываем черно-белый помидор
        imageTomatoBW.setVisibility(View.GONE);

        resetTimer();
        updateButtons();
    }

    private void sessionCompleted() {
        vibrate();

        if (hasNotificationPermission()) {
            sendNotification();
        } else {
            String message = isWorkTime ?
                    "25 минут работы завершены! Отдохните 5 минут!" :
                    "5 минут отдыха завершены! Приступайте к работе!";
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        }

        isWorkTime = !isWorkTime;

        if (!isWorkTime) {
            sessionCount++;
            textViewSessionCount.setText("Сессий завершено: " + sessionCount);
        }


        if (clipDrawable != null) {
            clipDrawable.setLevel(10000);
        }


        if (!isWorkTime) {
            showTaskCompletionDialog();
        }


        new android.os.Handler().postDelayed(() -> {
            startTimer();
        }, 2000); // 2 секунды задержки
    }

    // Отображение диалога выбора выполненных задач
    private void showTaskCompletionDialog() {
        TaskCompletionDialog completionDialog = new TaskCompletionDialog(getContext(), dbHelper);
        completionDialog.showCompletionDialog();
    }

    // Анимация заполнения
    private void updateTomatoProgress(float progress) {
        if (clipDrawable != null) {
            // ClipDrawable level: 0 -> полностью скрыто (серый), 10000 -> полностью видно (цветной)
            int level = (int) (progress * 10000);
            clipDrawable.setLevel(level);

            // Отладка
            Log.d("Pomodoro", "Progress: " + progress + ", Level: " + level);
        } else {
            Log.e("Pomodoro", "ClipDrawable is null!");
        }
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(requireContext(),
                    android.Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void updateTimer() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;

        String timeLeftFormatted = String.format("%02d:%02d", minutes, seconds);
        textViewTimer.setText(timeLeftFormatted);

        if (timeLeftInMillis < 10000) {
            textViewTimer.setTextColor(Color.RED);
        } else {
            textViewTimer.setTextColor(Color.parseColor("#D32F2F"));
        }
    }

    private void resetTimer() {
        timeLeftInMillis = isWorkTime ? WORK_TIME : BREAK_TIME;
        updateTimer();
        textViewStatus.setText("Таймер остановлен");
        textViewTimer.setTextColor(Color.parseColor("#D32F2F"));
    }

    private void updateButtons() {
        if (timerRunning) {
            imageButtonStart.setVisibility(View.GONE);
            buttonStop.setVisibility(View.VISIBLE);
        } else {
            imageButtonStart.setVisibility(View.VISIBLE);
            buttonStop.setVisibility(View.GONE);
            // При остановке показываем обычный цветной помидор
            imageTomatoColor.setImageResource(R.drawable.pomidor);
            imageTomatoBW.setVisibility(View.GONE);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Pomodoro Timer";
            String description = "Канал для уведомлений Pomodoro таймера";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});

            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendNotification() {
        String title = isWorkTime ? "Время отдыхать!" : "Время работать!";
        String message = isWorkTime ?
                "25 минут работы завершены. Отдохните 5 минут!" :
                "5 минут отдыха завершены. Приступайте к работе!";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(500);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}