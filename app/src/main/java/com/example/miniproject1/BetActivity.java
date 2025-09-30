package com.example.miniproject1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class BetActivity extends AppCompatActivity {

    public static final String EXTRA_BALANCE  = "EXTRA_BALANCE";
    public static final String EXTRA_AMOUNTS  = "EXTRA_AMOUNTS"; // int[4] tiền cho vịt 1..4

    private CheckBox cb1, cb2, cb3, cb4;
    private EditText et1, et2, et3, et4;
    private TextView tvBalance;
    private Button btnConfirm, btnCancel;

    private int balance = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bet);

        cb1 = findViewById(R.id.cbLane1);
        cb2 = findViewById(R.id.cbLane2);
        cb3 = findViewById(R.id.cbLane3);
        cb4 = findViewById(R.id.cbLane4);

        et1 = findViewById(R.id.etLane1);
        et2 = findViewById(R.id.etLane2);
        et3 = findViewById(R.id.etLane3);
        et4 = findViewById(R.id.etLane4);

        tvBalance  = findViewById(R.id.tvBalance);
        btnConfirm = findViewById(R.id.btnConfirm);
        btnCancel  = findViewById(R.id.btnCancel);

        balance = getIntent().getIntExtra(EXTRA_BALANCE, 0);
        tvBalance.setText("Balance: $" + balance);

        btnCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        btnConfirm.setOnClickListener(v -> confirmMultiBets());
    }

    private void confirmMultiBets() {
        int[] amounts = new int[4];

        // Parse từng dòng; chỉ chấp nhận số >0 nếu checkbox được tick
        amounts[0] = parseIfChecked(cb1, et1);
        amounts[1] = parseIfChecked(cb2, et2);
        amounts[2] = parseIfChecked(cb3, et3);
        amounts[3] = parseIfChecked(cb4, et4);

        int total = amounts[0] + amounts[1] + amounts[2] + amounts[3];

        if (total <= 0) {
            toast("Chọn ít nhất 1 vịt và nhập số tiền > 0.");
            return;
        }
        if (total > balance) {
            toast("Vượt số dư! Bạn còn $" + balance);
            return;
        }

        Intent result = new Intent();
        result.putExtra(EXTRA_AMOUNTS, amounts); // 4 phần tử cho vịt 1..4
        setResult(RESULT_OK, result);
        finish();
    }

    private int parseIfChecked(CheckBox cb, EditText et) {
        if (!cb.isChecked()) return 0;
        String s = et.getText().toString().trim();
        if (s.isEmpty()) return 0;
        try {
            int v = Integer.parseInt(s);
            return Math.max(v, 0);
        } catch (Exception e) {
            return 0;
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
