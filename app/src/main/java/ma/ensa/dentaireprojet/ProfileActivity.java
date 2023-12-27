package ma.ensa.dentaireprojet;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        int userId = getIntent().getIntExtra("id", -1);
        Log.d("uu", String.valueOf(userId));

        getUserData(userId);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Gérer les événements de clic sur les éléments de la barre de navigation
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_item_1:
                        Intent profileIntent1 = new Intent(ProfileActivity.this, ProfileActivity.class);
                        startActivity(profileIntent1);
                        return true;
                    case R.id.menu_item_2:
                        Intent profileIntent2 = new Intent(ProfileActivity.this, MainActivity.class);
                        startActivity(profileIntent2);
                        return true;
                    case R.id.menu_item_3:
                        Intent profileIntent3 = new Intent(ProfileActivity.this, LoginActivity.class);
                        startActivity(profileIntent3);
                        return true;
                }
                return false;
            }
        });
    }

    private void getUserData(int userId) {
        String apiUrl = "http://192.168.1.141:8082/api/users/" + userId;
        RequestQueue requestQueue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                apiUrl,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("Response", "API Response: " + response.toString());
                        try {
                            // Récupérer les informations de l'utilisateur depuis la réponse JSON
                            String userName = response.getString("userName");
                            String firstName = response.getString("firstName");
                            String lastName = response.getString("lastName");
                            String image = response.getString("image");

                            // Mettre à jour les boutons avec les données de l'utilisateur
                            updateButtonContent(userName, firstName, lastName, image);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Gérer les erreurs lors de la récupération des données de l'utilisateur
                        Log.e("VolleyError", "Error fetching user data: " + error.getMessage());
                        // Afficher un message Toast pour indiquer l'erreur
                        Toast.makeText(ProfileActivity.this, "Error fetching user data", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        requestQueue.add(jsonObjectRequest);
    }

    private void updateButtonContent(String userName, String firstName, String lastName, String image) {
        // Mettre à jour le texte des boutons avec les données de l'utilisateur
        ImageView imageView = findViewById(R.id.imageView2);
        Picasso.with(this).load(image).into(imageView);

        TextView userText = findViewById(R.id.textView);
        userText.setText(lastName + " " + firstName);

        AppCompatButton usernameButton = findViewById(R.id.button);
        usernameButton.setText("Username: " + userName);

        AppCompatButton firstnameButton = findViewById(R.id.button2);
        firstnameButton.setText("Firstname: " + firstName);

        AppCompatButton lastnameButton = findViewById(R.id.button3);
        lastnameButton.setText("Lastname: " + lastName);

    }
}