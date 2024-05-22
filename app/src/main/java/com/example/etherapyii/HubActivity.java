package com.example.etherapyii;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainer;
import androidx.fragment.app.FragmentContainerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.jetbrains.annotations.NotNull;

public class HubActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_hub);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set initial fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.hub_container, new ActivitySelectionFragment())
                    .commit();
        }

        //Fragment Navigation
        BottomNavigationView navMenu = findViewById(R.id.nav_menu);
        navMenu.setOnItemSelectedListener(item -> {
            Fragment selectedFragment;
            if (item.getItemId() == R.id.nav_activities) {
                selectedFragment = new ActivitySelectionFragment();
            } else if (item.getItemId() == R.id.nav_data) {
                selectedFragment = new WIPFragment(); // TODO: Change to PersonalDataFragment()
            } else if (item.getItemId() == R.id.nav_toDoList) {
                selectedFragment = new WIPFragment(); // TODO: Change to ToDoListFragment()
            }
            // Default
            else {
                selectedFragment = new WIPFragment(); // TODO: Change to ActivitySelectionFragment()
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.hub_container,selectedFragment).commit();
            return true;
        });




    }
}