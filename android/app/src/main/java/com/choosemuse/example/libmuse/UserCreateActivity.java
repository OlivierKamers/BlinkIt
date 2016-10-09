package com.choosemuse.example.libmuse;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class UserCreateActivity extends Activity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_create);
        Button cancelButton = (Button) findViewById(R.id.buttonCancel);
        if (cancelButton != null)
            cancelButton.setOnClickListener(this);
        Button createButton = (Button) findViewById(R.id.buttonCreate);
        if (createButton != null)
            createButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.buttonCreate) {
            // Create user
            SharedPreferences preferences = this.getSharedPreferences("com.choosemuse.example.libmuse.users", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(((EditText) findViewById(R.id.txtName)).getText().toString(), ((EditText) findViewById(R.id.txtPhone)).getText().toString());
            editor.apply();
            finish();
        } else if (view.getId() == R.id.buttonCancel) {
            // Cancel creating user and go to user list view
            Log.d("foobar", "not yet implemented");
        }
    }
}
