package com.example.lab5_starter;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements CityDialogFragment.CityDialogListener {

    private Button addCityButton;
    private ListView cityListView;

    private ArrayList<City> cityArrayList;
    private ArrayAdapter<City> cityArrayAdapter;
    private FirebaseFirestore db;
    private CollectionReference citiesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Set views
        addCityButton = findViewById(R.id.buttonAddCity);
        cityListView = findViewById(R.id.listviewCities);

        // City Array
        cityArrayList = new ArrayList<>();
        cityArrayAdapter = new CityArrayAdapter(this, cityArrayList);
        cityListView.setAdapter(cityArrayAdapter);

        db = FirebaseFirestore.getInstance();
        citiesRef = db.collection("cities");

        // Add City
        addCityButton.setOnClickListener(view -> {
            CityDialogFragment dialog = new CityDialogFragment();
            dialog.show(getSupportFragmentManager(), "Add City");
        });

        // Tap to Edit
        cityListView.setOnItemClickListener((adapterView, view, i, l) -> {
            City city = cityArrayAdapter.getItem(i);
            if (city != null) {
                CityDialogFragment dialog = CityDialogFragment.newInstance(city);
                dialog.show(getSupportFragmentManager(), "City Details");
            }
        });

        // Long-press to Delete
        cityListView.setOnItemLongClickListener((parent, view, position, id) -> {
            City city = cityArrayAdapter.getItem(position);
            if (city != null) confirmAndDeleteCity(city, position);
            return true;
        });

        // Firestore sync listener
        citiesRef.addSnapshotListener((QuerySnapshot value, FirebaseFirestoreException error) -> {
            if (error != null) {
                Log.e("Firestore", "Error", error);
                return;
            }
            // rebuild in memory list from snapshot
            cityArrayList.clear();
            if (value != null) {
                for (QueryDocumentSnapshot snapshot : value) {
                    String name = snapshot.getString("name");
                    String province = snapshot.getString("province");
                    if (name != null && province != null) {
                        cityArrayList.add(new City(name, province));
                    }
                }
            }
            cityArrayAdapter.notifyDataSetChanged(); // refresh the UI
        });
    }

    @Override
    public void updateCity(City city, String title, String year) {
        String oldName = city.getName();
        boolean nameChanged =! oldName.equals(title);

        city.setName(title); // local changes
        city.setProvince(year); // local changes
        cityArrayAdapter.notifyDataSetChanged();

        if (nameChanged) {
            citiesRef.document(oldName).delete() // deletes old document
                    .addOnSuccessListener(v -> citiesRef.document(title).set(city));
        } else {
            citiesRef.document(title).set(city); // writes the updated city
        }
    }

    // Delete City , show cuity and province
    private void confirmAndDeleteCity(City city, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete City")
                .setMessage("Delete " + city.getName() + ", " + city.getProvince() + "?")
                .setPositiveButton("Delete", (dialog, which) -> deleteCity(city, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteCity(City city, int position) {
        citiesRef.document(city.getName())
                .delete() // deletes from firestore
                .addOnSuccessListener(aVoid -> {
                    cityArrayList.remove(position); // remove item from in memoru list
                    cityArrayAdapter.notifyDataSetChanged(); // remake with excluded item
                    Toast.makeText(MainActivity.this, "Deleted " + city.getName(), Toast.LENGTH_SHORT).show();
                })
                // pre cautionary step if firestore fails to delete
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("Firestore", "Delete failed", e);
                });
    }
    @Override
    public void addCity(City city){
        cityArrayList.add(city);
        cityArrayAdapter.notifyDataSetChanged();

        DocumentReference docref = citiesRef.document(city.getName());
        docref.set(city); // writes to firestore

    }

    public void addDummyData(){
        City m1 = new City("Edmonton", "AB");
        City m2 = new City("Vancouver", "BC");
        cityArrayList.add(m1);
        cityArrayList.add(m2);
        cityArrayAdapter.notifyDataSetChanged();
    }
}