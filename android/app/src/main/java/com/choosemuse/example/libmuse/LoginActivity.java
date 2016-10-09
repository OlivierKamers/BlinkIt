package com.choosemuse.example.libmuse;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LoginActivity extends Activity implements View.OnClickListener {

    private ListView listView;
    private List<String> users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        Button addButton = (Button) findViewById(R.id.button);
        addButton.setOnClickListener(this);
        listView = (ListView) findViewById(R.id.listview);
        fillList();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                setActivePlayer(users.get(i));
                Intent intent = new Intent(LoginActivity.this, BlinkItActivity.class);
                startActivity(intent);
            }
        });
    }

    private void setActivePlayer(String name) {
        SharedPreferences preferences = this.getSharedPreferences("active", Context.MODE_PRIVATE);
        preferences.edit().putString("active_player", name).putString("active_phone", preferences.getString(name, "")).apply();

    }

    private void fillList() {
        SharedPreferences preferences = this.getSharedPreferences("com.choosemuse.example.libmuse.users", Context.MODE_PRIVATE);
        users = new ArrayList<>(preferences.getAll().keySet());
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, users);
        listView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fillList();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.button) {
            Intent i = new Intent(this, UserCreateActivity.class);
            startActivity(i);
        }
    }
}
