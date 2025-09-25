package com.example.miniproject1;

import android.content.pm.ActivityInfo;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.HashMap;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private SeekBar[] lanes;
    private Button btnStart, btnReset;
    private TextView tvMoney, tvResult;

    private int balance = 100;
    private boolean bettingOpen = true;
    private final HashMap<Integer, Integer> betsMap = new HashMap<>();

    private boolean raceOn = false;
    private boolean winnerAnnounced = false;
    private final int MAX_PROGRESS = 1000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable[] ticks = new Runnable[4]; // 4 lanes
    private final int[] baseSpeed = new int[4];   // tốc độ cơ bản của mỗi con vịt (bước/tick)
    private final Random random = new Random();

    // Cấu hình tốc độ cho mỗi con vịt
    private static final int STEP_MIN = 2, STEP_MAX = 4;
    private static final int DELAY_MIN = 25; // ms - tốc độ nhanh nhất
    private static final int DELAY_MAX = 120; // ms - tốc độ chậm nhất
    private static final float SPEED_CHANGE_PROB = 0.12f; // xác suất thay đổi tốc độ cơ bản mỗi tick


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        lanes = new SeekBar[]{
                findViewById(R.id.seekBar1),
                findViewById(R.id.seekBar2),
                findViewById(R.id.seekBar3),
                findViewById(R.id.seekBar4)
        };
        btnStart = findViewById(R.id.btnStart);
        btnReset = findViewById(R.id.btnReset);
        tvMoney = findViewById(R.id.tvMoney);
        tvResult = findViewById(R.id.tvResult);

        Button btnBet = findViewById(R.id.btnBet);
        Button btnAddFunds = findViewById(R.id.btnAddFunds);

        btnBet.setOnClickListener(v -> showBetDialog());
        btnAddFunds.setOnClickListener(v -> showAddFundsDialog());

        for (SeekBar lane : lanes) {
            lane.setMax(MAX_PROGRESS);
            lane.setOnTouchListener((v, e) -> true);
            lane.setProgress(0);
        }

        tvResult.setVisibility(View.GONE);
        tvResult.setAlpha(0f);

        btnStart.setOnClickListener(v -> startRace());
        btnReset.setOnClickListener(v -> resetRace());

        updateMoneyText();
    }

    private void startRace(){
        if(raceOn) return;
        bettingOpen = false;
        raceOn = true;
        winnerAnnounced = false;
        startDuckAnimations();
        // Khởi tạo tốc độ cơ bản ngẫu nhiên cho mỗi xe
        for(int i = 0; i < lanes.length; i++){
            lanes[i].setProgress(0);
            baseSpeed[i] = randInt(STEP_MIN, STEP_MAX);
        }

        // Khởi động các luồng chạy xe
        for (int i = 0; i < lanes.length; i++){
            final int laneIndex = i;
            ticks[i] = new Runnable() {
                @Override
                public void run() {
                    if(!raceOn) return;

                    if(random.nextFloat() < SPEED_CHANGE_PROB){
                        // thay đổi tốc độ cơ bản ngẫu nhiên trong khoảng STEP_MIN đến STEP_MAX
                        baseSpeed[laneIndex] = randInt(STEP_MIN, STEP_MAX);
                    }

                    int jitter = random.nextInt(3) - 1; // -1, 0, or +1
                    int step = Math.max(1, baseSpeed[laneIndex] + jitter);

                    // Cập nhật tiến độ
                    int newProgress = lanes[laneIndex].getProgress() + step;
                    if(newProgress > MAX_PROGRESS) newProgress = MAX_PROGRESS;
                    lanes[laneIndex].setProgress(newProgress);

                    // Kiểm tra nếu có người chiến thắng
                    if (!winnerAnnounced && newProgress >= MAX_PROGRESS) {
                        winnerAnnounced = true;
                        int winner = laneIndex; // 0..3

                        int payout = settleSingleUserBets(winner); // nhận số tiền thưởng

                        if (payout > 0) {
                            showResult("Vịt " + (winner + 1) + " thắng!  +$" + payout, true);
                        } else {
                            showResult("Vịt " + (winner + 1) + " thắng!  (Bạn không đặt con này)", false);
                        }

                        updateMoneyText();  // cập nhật “Số dư: $…”
                        bettingOpen = true;
                        stopOthers();
                        stopDuckAnimations();
                        return;
                    }
                    // Lên lịch tick tiếp theo với độ trễ ngẫu nhiên
                    handler.postDelayed(this, randInt(DELAY_MIN, DELAY_MAX));
                }
            };
            handler.postDelayed(ticks[i], randInt(DELAY_MIN, DELAY_MAX));
        }
    }

    private void resetRace() {
        if (raceOn) return;
        winnerAnnounced = false;
        stopOthers();
        for (SeekBar sb : lanes) sb.setProgress(0);
        bettingOpen = true;
        updateMoneyText();
        stopDuckAnimations();

        // Ẩn kết quả
        tvResult.setText("");
        tvResult.setVisibility(View.GONE);
        tvResult.setAlpha(0f);
    }

    private void stopOthers(){
        raceOn = false;
        for (Runnable r : ticks) {
            if (r != null) handler.removeCallbacks(r);
        }
    }

    private int randInt(int min, int maxInclusive) {
        return min + random.nextInt(maxInclusive - min + 1);
    }
    private void startDuckAnimations() {
        for (SeekBar sb : lanes) {
            if (sb.getThumb() instanceof AnimationDrawable) {
                AnimationDrawable ad = (AnimationDrawable) sb.getThumb();
                if (!ad.isRunning()) ad.start();
            }
        }
    }

    private void stopDuckAnimations() {
        for (SeekBar sb : lanes) {
            if (sb.getThumb() instanceof AnimationDrawable) {
                AnimationDrawable ad = (AnimationDrawable) sb.getThumb();
                if (ad.isRunning()) ad.stop();
                ad.selectDrawable(0); // về frame “đứng yên”
            }
        }
    }

    private void showBetDialog() {
        if (!bettingOpen) {
            android.widget.Toast.makeText(this, "Đang đua — tạm khoá đặt cược!", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        android.widget.TextView tvBal = new android.widget.TextView(this);
        tvBal.setText("Số dư hiện tại: $" + balance);
        root.addView(tvBal);

        // 4 hàng nhập tiền cho vịt 1..4
        android.widget.EditText[] et = new android.widget.EditText[4];
        for (int i = 0; i < 4; i++) {
            android.widget.LinearLayout row = new android.widget.LinearLayout(this);
            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);

            android.widget.TextView tv = new android.widget.TextView(this);
            tv.setText("Vịt " + (i+1) + " ($): ");

            et[i] = new android.widget.EditText(this);
            et[i].setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            et[i].setHint("0");
            android.widget.LinearLayout.LayoutParams lpLabel =
                    new android.widget.LinearLayout.LayoutParams(0,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            android.widget.LinearLayout.LayoutParams lpInput =
                    new android.widget.LinearLayout.LayoutParams(0,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);

            row.addView(tv);
            row.addView(et[i]);
            root.addView(row);
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Đặt nhiều kèo")
                .setView(root)
                .setNegativeButton("Huỷ", null)
                .setPositiveButton("Xác nhận", (d,w)->{
                    // tính tổng tiền đặt
                    int totalBet = 0;
                    int[] betVal = new int[4];
                    for (int i = 0; i < 4; i++) {
                        int v = 0;
                        try { v = Integer.parseInt(et[i].getText().toString().trim()); } catch (Exception ignored) {}
                        if (v < 0) v = 0;
                        betVal[i] = v;
                        totalBet += v;
                    }

                    if (totalBet <= 0) {
                        android.widget.Toast.makeText(this, "Hãy nhập số tiền > 0 cho ít nhất 1 con.", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (totalBet > balance) {
                        android.widget.Toast.makeText(this, "Vượt số dư! Bạn còn $" + balance, android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // trừ tiền & ghi kèo
                    balance -= totalBet;
                    betsMap.clear();
                    for (int i = 0; i < 4; i++) {
                        if (betVal[i] > 0) betsMap.put(i, betVal[i]); // i = duckIndex
                    }
                    updateMoneyText();
                    android.widget.Toast.makeText(this, "Đã đặt tổng $" + totalBet, android.widget.Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showAddFundsDialog() {
        android.widget.EditText et = new android.widget.EditText(this);
        et.setHint("Nhập số tiền $");
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cộng tiền vào tài khoản")
                .setView(et)
                .setNegativeButton("Huỷ", null)
                .setPositiveButton("Cộng", (d,w)->{
                    int add = 0;
                    try { add = Integer.parseInt(et.getText().toString().trim()); } catch (Exception ignored){}
                    if (add <= 0) {
                        android.widget.Toast.makeText(this, "Nhập số > 0", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    balance += add;
                    updateMoneyText();
                    android.widget.Toast.makeText(this, "Đã cộng $" + add, android.widget.Toast.LENGTH_SHORT).show();
                })
                .show();
    }
    private void updateMoneyText() {
        tvMoney.setText("Số dư: $" + balance);
    }

    //trả số tiền thưởng
    private int settleSingleUserBets(int winnerDuckIndex) {
        if (betsMap.isEmpty()) return 0;
        Integer betOnWinner = betsMap.get(winnerDuckIndex);
        int payout = 0;
        if (betOnWinner != null && betOnWinner > 0) {
            payout = betOnWinner * 2;  // đã trừ khi đặt, thắng nhận 2x
            balance += payout;
        }
        betsMap.clear();
        return payout;
    }

    private void showResult(String msg, boolean isWin) {
        tvResult.setText(msg);
        tvResult.setTextColor(getColor(isWin ? R.color.result_win : R.color.result_lose));
        tvResult.setVisibility(View.VISIBLE);
        tvResult.setAlpha(0f);
        tvResult.animate().alpha(1f).setDuration(250).start();
    }

}