package com.example.miniproject1;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private SeekBar[] lanes;
    private Button btnStart, btnReset, btnBet, btnAddFunds;
    private TextView tvMoney, tvResult;
    private SoundEffects sfx;
    private MediaPlayer raceBgm;
    private Runnable hideResultRunnable;
    // Số dư & kèo (key: duckIndex 0..3, value: amount)
    private int balance = 100;
    private final HashMap<Integer, Integer> betsMap = new HashMap<>();

    // Activity Result: nhận kèo từ BetActivity
    private final ActivityResultLauncher<Intent> betLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Intent data = result.getData();
                            int[] amounts = data.getIntArrayExtra(BetActivity.EXTRA_AMOUNTS); // [a1,a2,a3,a4]
                            if (amounts == null || amounts.length != 4) return;

                            int total = 0;
                            // map key là 0..3 (khớp winner index)
                            for (int i = 0; i < 4; i++) {
                                int add = Math.max(0, amounts[i]);
                                total += add;
                                if (add > 0) {
                                    int old = betsMap.containsKey(i) ? betsMap.get(i) : 0;
                                    betsMap.put(i, old + add);
                                }
                            }

                            balance -= total; // đã trừ tổng khi đặt
                            updateMoneyText();

                            android.widget.Toast.makeText(this,
                                    "Đã đặt tổng $" + total,
                                    android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }
            );


    // Trạng thái race
    private boolean bettingOpen = true; // cho phép đặt kèo khi chưa đua
    private boolean raceOn = false;
    private boolean winnerAnnounced = false;

    // Cấu hình đua
    private static final int MAX_PROGRESS = 1200;
    private static final int STEP_MIN = 2, STEP_MAX = 4;
    private static final int DELAY_MIN = 40;   // ms
    private static final int DELAY_MAX = 120;  // ms
    private static final float SPEED_CHANGE_PROB = 0.12f;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable[] ticks = new Runnable[4];
    private final int[] baseSpeed = new int[4];
    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        sfx = new SoundEffects();
        sfx.load(this, R.raw.race_start);
        sfx.load(this, R.raw.win);
        sfx.load(this, R.raw.lose);
        sfx.load(this, R.raw.click);
        sfx.load(this, R.raw.coin);

        raceBgm = android.media.MediaPlayer.create(this, R.raw.main_bg /*đổi thành nhạc đua riêng nếu có*/);
        raceBgm.setLooping(true);

        // Ánh xạ view
        lanes = new SeekBar[]{
                findViewById(R.id.seekBar1),
                findViewById(R.id.seekBar2),
                findViewById(R.id.seekBar3),
                findViewById(R.id.seekBar4)
        };
        btnStart    = findViewById(R.id.btnStart);
        btnReset    = findViewById(R.id.btnReset);
        btnBet      = findViewById(R.id.btnBet);
        btnAddFunds = findViewById(R.id.btnAddFunds);
        tvMoney     = findViewById(R.id.tvMoney);
        tvResult    = findViewById(R.id.tvResult);

        // Cấu hình lanes
        for (SeekBar lane : lanes) {
            lane.setMax(MAX_PROGRESS);
            lane.setOnTouchListener((v, e) -> true); // chặn kéo tay
            lane.setProgress(0);
        }

        tvResult.setVisibility(View.GONE);
        tvResult.setAlpha(0f);

        // Sự kiện nút
        btnStart.setOnClickListener(v -> {
            // còi xuất phát
            sfx.play(R.raw.race_start, 1f);

            // cho còi kêu trước 100–150ms rồi bật nhạc nền (tránh giựt/đá audio focus)
            handler.postDelayed(this::playRaceMusicFromStart, 120);

            startRace();
        });

        btnReset.setOnClickListener(v -> {
            sfx.play(R.raw.click, 1f);
            resetRace();
        });

        btnBet.setOnClickListener(v -> {
            if (!bettingOpen) {
                Toast.makeText(this, "Đang đua — tạm khoá đặt cược!", Toast.LENGTH_SHORT).show();
                return;
            }
            sfx.play(R.raw.click, 1f);
            Intent i = new Intent(this, BetActivity.class);
            i.putExtra(BetActivity.EXTRA_BALANCE, balance);
            betLauncher.launch(i);
        });
        btnAddFunds.setOnClickListener(v -> {
            sfx.play(R.raw.click, 1f);
            showAddFundsDialog();
        });
        updateMoneyText();
    }

    private void startRace() {
        if (raceOn) return;
        bettingOpen = false;
        raceOn = true;
        winnerAnnounced = false;

        startDuckAnimations();

        // Reset tiến độ & tốc độ cơ bản
        for (int i = 0; i < lanes.length; i++) {
            lanes[i].setProgress(0);
            baseSpeed[i] = randInt(STEP_MIN, STEP_MAX);
        }

        // Tick cho từng lane
        for (int i = 0; i < lanes.length; i++) {
            final int laneIndex = i;
            ticks[i] = new Runnable() {
                @Override
                public void run() {
                    if (!raceOn) return;

                    if (random.nextFloat() < SPEED_CHANGE_PROB) {
                        baseSpeed[laneIndex] = randInt(STEP_MIN, STEP_MAX);
                    }
                    int jitter = random.nextInt(3) - 1; // -1,0,+1
                    int step = Math.max(1, baseSpeed[laneIndex] + jitter);

                    int newProgress = lanes[laneIndex].getProgress() + step;
                    if (newProgress > MAX_PROGRESS) newProgress = MAX_PROGRESS;
                    lanes[laneIndex].setProgress(newProgress);

                    if (!winnerAnnounced && newProgress >= MAX_PROGRESS) {
                        winnerAnnounced = true;
                        int winner = laneIndex; // 0..3

                        int payout = settleSingleUserBets(winner);

                        if (payout > 0) {
                            showResult("Vịt " + (winner + 1) + " thắng!  +$" + payout, true);
                        } else {
                            showResult("Vịt " + (winner + 1) + " thắng!  (Bạn không đặt con này)", false);
                        }

                        updateMoneyText();
                        bettingOpen = true;
                        stopAllTicks();
                        stopDuckAnimations();
                        return;
                    }

                    handler.postDelayed(this, randInt(DELAY_MIN, DELAY_MAX));
                }
            };
            handler.postDelayed(ticks[i], randInt(DELAY_MIN, DELAY_MAX));
        }
    }

    private void resetRace() {
        if (raceOn) return; // đang đua thì không reset
        winnerAnnounced = false;
        stopAllTicks();
        for (SeekBar sb : lanes) sb.setProgress(0);
        bettingOpen = true;
        updateMoneyText();
        stopDuckAnimations();

        if (hideResultRunnable != null) {
            tvResult.removeCallbacks(hideResultRunnable);
            hideResultRunnable = null;
        }

        tvResult.setText("");
        tvResult.setVisibility(View.GONE);
        tvResult.setAlpha(0f);
    }

    private void stopAllTicks() {
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
                ad.selectDrawable(0);
            }
        }
    }

    private void showAddFundsDialog() {
        final android.widget.EditText et = new android.widget.EditText(this);
        et.setHint("Nhập số tiền $");
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cộng tiền vào tài khoản")
                .setView(et)
                .setNegativeButton("Huỷ", null)
                .setPositiveButton("Cộng", (d, w) -> {
                    int add = 0;
                    try { add = Integer.parseInt(et.getText().toString().trim()); } catch (Exception ignored) {}
                    if (add <= 0) {
                        Toast.makeText(this, "Nhập số > 0", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    balance += add;
                    updateMoneyText();
                    Toast.makeText(this, "Đã cộng $" + add, Toast.LENGTH_SHORT).show();
                    sfx.play(R.raw.coin, 0.9f);
                })
                .show();
    }

    private void updateMoneyText() {
        tvMoney.setText("Số dư: $" + balance);
    }

    /** Trả thưởng cho người chơi 1 – theo kèo đang lưu trong betsMap (0..3). */
    private int settleSingleUserBets(int winnerDuckIndex) {
        if (betsMap.isEmpty()) return 0;
        Integer betOnWinner = betsMap.get(winnerDuckIndex);
        int payout = 0;
        if (betOnWinner != null && betOnWinner > 0) {
            payout = betOnWinner * 2; // đã trừ khi đặt; thắng nhận 2x
            balance += payout;
        }
        betsMap.clear();
        return payout;
    }

    private void showResult(String msg, boolean isWin) {
        sfx.play(isWin ? R.raw.win : R.raw.lose, 1f);

        if (raceBgm != null && raceBgm.isPlaying()) raceBgm.pause();

        tvResult.setText(msg);
        try {
            tvResult.setTextColor(getColor(isWin ? R.color.result_win : R.color.result_lose));
        } catch (Exception ignore) {
            // fallback nếu chưa khai báo màu
            tvResult.setTextColor(isWin ? 0xFF2E7D32 : 0xFFC62828);
        }
        tvResult.setVisibility(View.VISIBLE);
        tvResult.setAlpha(0f);
        tvResult.animate().alpha(1f).setDuration(250).start();
        if (hideResultRunnable != null) {
            tvResult.removeCallbacks(hideResultRunnable);
        }

        // Lên lịch ẩn sau 3.5s (3500ms). Đổi 4000 nếu muốn ~4s.
        hideResultRunnable = () -> {
            tvResult.animate()
                    .alpha(0f)
                    .setDuration(400)
                    .withEndAction(() -> tvResult.setVisibility(View.GONE))
                    .start();
        };
        tvResult.postDelayed(hideResultRunnable, 4500);
    }
    private void playRaceMusicFromStart() {
        try {
            if (raceBgm != null) {
                raceBgm.release(); // giải phóng player cũ để tránh IllegalState
            }
        } catch (Exception ignored) {}

        raceBgm = MediaPlayer.create(this, R.raw.main_bg);
        raceBgm.setLooping(true);
        raceBgm.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // đảm bảo dừng nhạc khi app vào nền
        if (raceBgm != null && raceBgm.isPlaying()) raceBgm.pause();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (hideResultRunnable != null) tvResult.removeCallbacks(hideResultRunnable);
        if (sfx != null) sfx.release();
        if (raceBgm != null) { raceBgm.release(); raceBgm = null; }
    }
}
