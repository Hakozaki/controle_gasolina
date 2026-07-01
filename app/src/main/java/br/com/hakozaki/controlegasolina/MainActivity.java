package br.com.hakozaki.controlegasolina;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREFS = "controle_gasolina";
    private static final String KEY_VEHICLES = "vehicles";
    private static final String KEY_REFUELS = "refuels";
    private static final int LOCATION_PERMISSION_REQUEST = 42;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private final NumberFormat moneyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    private SharedPreferences prefs;
    private Spinner vehicleSpinner;
    private Spinner fuelTypeSpinner;
    private EditText kmInput;
    private EditText stationInput;
    private EditText litersInput;
    private EditText totalValueInput;
    private TextView locationText;
    private LinearLayout historyContainer;
    private Location lastLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        ensureDefaultVehicle();
        setContentView(buildContent());
        refreshVehicles();
        refreshHistory();
        requestLocationIfPossible();
    }

    private View buildContent() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(20), dp(18), dp(28));
        root.setBackgroundColor(color(R.color.screen_background));
        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("Controle Gasolina");
        title.setTextColor(color(R.color.text_primary));
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Registre os abastecimentos dos seus veiculos.");
        subtitle.setTextColor(color(R.color.text_secondary));
        subtitle.setTextSize(15);
        subtitle.setPadding(0, dp(4), 0, dp(18));
        root.addView(subtitle);

        LinearLayout vehicleRow = new LinearLayout(this);
        vehicleRow.setOrientation(LinearLayout.HORIZONTAL);
        vehicleRow.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(vehicleRow);

        vehicleSpinner = new Spinner(this);
        vehicleRow.addView(vehicleSpinner, new LinearLayout.LayoutParams(0, dp(48), 1));

        Button addVehicleButton = new Button(this);
        addVehicleButton.setText("+");
        addVehicleButton.setTextSize(22);
        addVehicleButton.setTextColor(color(android.R.color.white));
        addVehicleButton.setBackground(cardBackground(color(R.color.brand_green), dp(8)));
        addVehicleButton.setOnClickListener(v -> showAddVehicleDialog());
        LinearLayout.LayoutParams addVehicleParams = new LinearLayout.LayoutParams(dp(54), dp(48));
        addVehicleParams.setMargins(dp(10), 0, 0, 0);
        vehicleRow.addView(addVehicleButton, addVehicleParams);

        LinearLayout form = section();
        root.addView(form);

        kmInput = input("Quilometragem atual (opcional)", InputType.TYPE_CLASS_NUMBER);
        form.addView(label("Quilometragem"));
        form.addView(kmInput);

        stationInput = input("Posto de combustivel", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        form.addView(label("Posto"));
        form.addView(stationInput);

        litersInput = input("Litros", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        form.addView(label("Litros"));
        form.addView(litersInput);

        fuelTypeSpinner = new Spinner(this);
        ArrayAdapter<String> fuelAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Gasolina", "Etanol", "Diesel", "GNV", "Flex/Outro"}
        );
        fuelTypeSpinner.setAdapter(fuelAdapter);
        form.addView(label("Tipo de combustivel"));
        form.addView(fuelTypeSpinner, fieldParams());

        totalValueInput = input("Valor total em R$", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        form.addView(label("Valor"));
        form.addView(totalValueInput);

        locationText = new TextView(this);
        locationText.setText("GPS: aguardando permissao");
        locationText.setTextColor(color(R.color.text_secondary));
        locationText.setTextSize(14);
        locationText.setPadding(0, dp(10), 0, dp(10));
        form.addView(locationText);

        Button saveButton = new Button(this);
        saveButton.setText("Salvar abastecimento");
        saveButton.setTextColor(color(android.R.color.white));
        saveButton.setTextSize(16);
        saveButton.setTypeface(Typeface.DEFAULT_BOLD);
        saveButton.setBackground(cardBackground(color(R.color.brand_green), dp(8)));
        saveButton.setOnClickListener(v -> saveRefuel());
        form.addView(saveButton, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));

        TextView historyTitle = new TextView(this);
        historyTitle.setText("Historico");
        historyTitle.setTextColor(color(R.color.text_primary));
        historyTitle.setTextSize(20);
        historyTitle.setTypeface(Typeface.DEFAULT_BOLD);
        historyTitle.setPadding(0, dp(22), 0, dp(10));
        root.addView(historyTitle);

        historyContainer = new LinearLayout(this);
        historyContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(historyContainer);

        return scrollView;
    }

    private void showAddVehicleDialog() {
        EditText input = input("Nome ou placa do veiculo", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        input.setPadding(dp(14), 0, dp(14), 0);

        new AlertDialog.Builder(this)
                .setTitle("Novo veiculo")
                .setView(input)
                .setPositiveButton("Cadastrar", (dialog, which) -> addVehicle(input.getText().toString()))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void addVehicle(String rawName) {
        String name = rawName.trim();
        if (name.isEmpty()) {
            toast("Informe um nome para o veiculo.");
            return;
        }

        try {
            JSONArray vehicles = getArray(KEY_VEHICLES);
            for (int i = 0; i < vehicles.length(); i++) {
                if (name.equalsIgnoreCase(vehicles.getString(i))) {
                    toast("Esse veiculo ja esta cadastrado.");
                    return;
                }
            }
            vehicles.put(name);
            putArray(KEY_VEHICLES, vehicles);
            refreshVehicles();
            vehicleSpinner.setSelection(vehicles.length() - 1);
        } catch (JSONException e) {
            toast("Nao foi possivel cadastrar o veiculo.");
        }
    }

    private void saveRefuel() {
        String vehicle = vehicleSpinner.getSelectedItem() == null ? "" : vehicleSpinner.getSelectedItem().toString();
        String station = stationInput.getText().toString().trim();
        String litersText = litersInput.getText().toString().trim();
        String valueText = totalValueInput.getText().toString().trim();

        if (vehicle.isEmpty()) {
            toast("Cadastre ou escolha um veiculo.");
            return;
        }
        if (station.isEmpty()) {
            toast("Informe o posto de combustivel.");
            return;
        }

        double liters = parseDecimal(litersText);
        double totalValue = parseDecimal(valueText);
        if (liters <= 0 || totalValue <= 0) {
            toast("Informe litros e valor validos.");
            return;
        }

        try {
            JSONObject refuel = new JSONObject();
            refuel.put("id", System.currentTimeMillis());
            refuel.put("vehicle", vehicle);
            refuel.put("odometer", kmInput.getText().toString().trim());
            refuel.put("station", station);
            refuel.put("liters", liters);
            refuel.put("fuelType", fuelTypeSpinner.getSelectedItem().toString());
            refuel.put("totalValue", totalValue);
            refuel.put("timestamp", System.currentTimeMillis());

            if (lastLocation != null) {
                refuel.put("latitude", lastLocation.getLatitude());
                refuel.put("longitude", lastLocation.getLongitude());
            } else {
                refuel.put("latitude", JSONObject.NULL);
                refuel.put("longitude", JSONObject.NULL);
            }

            JSONArray refuels = getArray(KEY_REFUELS);
            refuels.put(refuel);
            putArray(KEY_REFUELS, refuels);

            clearForm();
            refreshHistory();
            requestLocationIfPossible();
            toast("Abastecimento salvo.");
        } catch (JSONException e) {
            toast("Nao foi possivel salvar.");
        }
    }

    private void clearForm() {
        kmInput.setText("");
        stationInput.setText("");
        litersInput.setText("");
        totalValueInput.setText("");
    }

    private void refreshVehicles() {
        try {
            JSONArray vehicles = getArray(KEY_VEHICLES);
            List<String> names = new ArrayList<>();
            for (int i = 0; i < vehicles.length(); i++) {
                names.add(vehicles.getString(i));
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names);
            vehicleSpinner.setAdapter(adapter);
        } catch (JSONException e) {
            toast("Erro ao carregar veiculos.");
        }
    }

    private void refreshHistory() {
        historyContainer.removeAllViews();
        try {
            JSONArray refuels = getArray(KEY_REFUELS);
            if (refuels.length() == 0) {
                TextView empty = new TextView(this);
                empty.setText("Nenhum abastecimento cadastrado ainda.");
                empty.setTextColor(color(R.color.text_secondary));
                empty.setTextSize(15);
                historyContainer.addView(empty);
                return;
            }

            for (int i = refuels.length() - 1; i >= 0; i--) {
                JSONObject item = refuels.getJSONObject(i);
                historyContainer.addView(historyCard(item));
            }
        } catch (JSONException e) {
            toast("Erro ao carregar historico.");
        }
    }

    private View historyCard(JSONObject item) throws JSONException {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(cardBackground(color(R.color.card_background), dp(8)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(params);

        TextView title = new TextView(this);
        title.setText(item.getString("vehicle") + " - " + item.getString("fuelType"));
        title.setTextColor(color(R.color.text_primary));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextSize(16);
        card.addView(title);

        double liters = item.getDouble("liters");
        double total = item.getDouble("totalValue");
        String odometer = item.optString("odometer", "");
        String km = odometer.isEmpty() ? "" : " | Km " + odometer;
        String details = dateFormat.format(new Date(item.getLong("timestamp")))
                + km
                + "\n" + item.getString("station")
                + "\n" + String.format(Locale.getDefault(), "%.2f L", liters)
                + " | " + moneyFormat.format(total)
                + " | " + moneyFormat.format(total / liters) + "/L";

        if (!item.isNull("latitude") && !item.isNull("longitude")) {
            details += String.format(Locale.getDefault(), "\nGPS: %.5f, %.5f", item.getDouble("latitude"), item.getDouble("longitude"));
        }

        TextView body = new TextView(this);
        body.setText(details);
        body.setTextColor(color(R.color.text_secondary));
        body.setTextSize(14);
        body.setPadding(0, dp(6), 0, 0);
        card.addView(body);

        return card;
    }

    private void requestLocationIfPossible() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST
            );
            return;
        }

        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (manager == null) {
            locationText.setText("GPS: indisponivel");
            return;
        }

        Location location = newestLocation(manager);
        if (location != null) {
            updateLocation(location);
        } else {
            locationText.setText("GPS: buscando localizacao");
        }

        try {
            String provider = manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    ? LocationManager.GPS_PROVIDER
                    : LocationManager.NETWORK_PROVIDER;
            manager.requestSingleUpdate(provider, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    updateLocation(location);
                }

                @Override
                public void onProviderDisabled(String provider) {
                    locationText.setText("GPS: provedor desativado");
                }

                @Override
                public void onProviderEnabled(String provider) {
                    locationText.setText("GPS: buscando localizacao");
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                    // Mantido para compatibilidade com APIs antigas.
                }
            }, Looper.getMainLooper());
        } catch (SecurityException | IllegalArgumentException ignored) {
            locationText.setText("GPS: nao foi possivel obter localizacao");
        }
    }

    private Location newestLocation(LocationManager manager) {
        Location best = null;
        try {
            for (String provider : manager.getProviders(true)) {
                Location location = manager.getLastKnownLocation(provider);
                if (location != null && (best == null || location.getTime() > best.getTime())) {
                    best = location;
                }
            }
        } catch (SecurityException ignored) {
            return null;
        }
        return best;
    }

    private void updateLocation(Location location) {
        lastLocation = location;
        locationText.setText(String.format(Locale.getDefault(), "GPS: %.5f, %.5f", location.getLatitude(), location.getLongitude()));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationIfPossible();
            } else {
                locationText.setText("GPS: sem permissao");
            }
        }
    }

    private void ensureDefaultVehicle() {
        if (!prefs.contains(KEY_VEHICLES)) {
            JSONArray vehicles = new JSONArray();
            vehicles.put("Meu veiculo");
            putArray(KEY_VEHICLES, vehicles);
        }
    }

    private JSONArray getArray(String key) throws JSONException {
        return new JSONArray(prefs.getString(key, "[]"));
    }

    private void putArray(String key, JSONArray array) {
        prefs.edit().putString(key, array.toString()).apply();
    }

    private EditText input(String hint, int inputType) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setTextColor(color(R.color.text_primary));
        input.setHintTextColor(color(R.color.text_secondary));
        input.setInputType(inputType);
        input.setBackground(cardBackground(color(android.R.color.white), dp(8)));
        input.setPadding(dp(12), 0, dp(12), 0);
        input.setLayoutParams(fieldParams());
        return input;
    }

    private TextView label(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(color(R.color.text_primary));
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextSize(14);
        label.setPadding(0, dp(12), 0, dp(6));
        return label;
    }

    private LinearLayout section() {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setPadding(dp(14), dp(10), dp(14), dp(14));
        section.setBackground(cardBackground(color(R.color.card_background), dp(8)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(16), 0, 0);
        section.setLayoutParams(params);
        return section;
    }

    private LinearLayout.LayoutParams fieldParams() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
    }

    private GradientDrawable cardBackground(int fillColor, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), 0x1F000000);
        return drawable;
    }

    private double parseDecimal(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(text.replace(",", "."));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int color(int id) {
        return getResources().getColor(id, getTheme());
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
