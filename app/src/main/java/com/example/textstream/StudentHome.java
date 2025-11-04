package com.example.textstream;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class StudentHome extends AppCompatActivity {

    private SearchView searchView;
    private RecyclerView recyclerView;
    private BlockAdapter blockAdapter;
    private List<BlockItem> blockList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student_home);
        searchView = findViewById(R.id.searchView);
        recyclerView = findViewById(R.id.recyclerView);

        // Set up block list and RecyclerView
        blockList = new ArrayList<>();
        blockList.add(new BlockItem("Operating Systems","Open Text Book", subjects.class, getString(R.string.os_obj), "os_notes", getString(R.string.os_book)));
        blockList.add(new BlockItem("Computer Networks","Open Text Book", subjects.class, getString(R.string.cn_obj), "cn_notes", getString(R.string.cn_book)));
        blockList.add(new BlockItem("Graph Theory","Open Text Book", subjects.class, getString(R.string.gt_obj), "gt_notes", getString(R.string.gt_book)));
        blockList.add(new BlockItem("Web Technologies","Open Text Book", subjects.class, getString(R.string.wt_obj), "wt_notes", getString(R.string.wt_book)));
        blockList.add(new BlockItem("Compiler Engineering","Open Text Book", subjects.class, getString(R.string.ce_obj), "ce_notes", getString(R.string.ce_book)));
        blockList.add(new BlockItem("Advances in Databases","Open Text Book", subjects.class, getString(R.string.adb_obj), "adb_notes", getString(R.string.adb_book)));
        blockList.add(new BlockItem("Digital Logic and Design","Open Text Book", subjects.class, getString(R.string.dld_obj), "dld_notes", getString(R.string.dld_book)));
        blockList.add(new BlockItem("Programming and Data Structures","Open Text Book", subjects.class, getString(R.string.pds_obj), "pds_notes", getString(R.string.pds_book)));
        blockList.add(new BlockItem("Database Management Systems","Open Text Book", subjects.class, getString(R.string.dbms_obj), "dbms_notes", getString(R.string.dbms_book)));
        blockList.add(new BlockItem("OOPS and ADS","Open Text Book", subjects.class, getString(R.string.oops_obj), "oops_notes", getString(R.string.oops_book)));


        blockAdapter = new BlockAdapter(blockList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(blockAdapter);

        // Set up SearchView listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                blockAdapter.filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                blockAdapter.filter(newText);
                return false;
            }
        });
        Button setReminderButton = findViewById(R.id.setReminderButton);
        setReminderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Redirect to ReminderActivity
                Intent intent = new Intent(StudentHome.this, ReminderActivity.class);
                startActivity(intent);
            }
        });
        Button submitAssignmentButton = findViewById(R.id.submitAssignmentButton);
        submitAssignmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StudentHome.this, SubmitAssignmentActivity.class);
                startActivity(intent);
            }
        });

    }
}