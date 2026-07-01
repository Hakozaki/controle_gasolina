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
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
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
    private static final String KEY_STATIONS = "stations";
    private static final String KEY_REFUELS = "refuels";
    private static final String KEY_DARK_THEME = "dark_theme";
    private static final int PAGE_REGISTER = 0;
    private static final int PAGE_HISTORY = 1;
    private static final int PAGE_VEHICLES = 2;
    private static final int PAGE_STATIONS = 3;
    private static final int PAGE_SETTINGS = 4;
    private static final int LOCATION_PERMISSION_REQUEST = 42;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private final NumberFormat moneyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    private SharedPreferences prefs;
    private Spinner vehicleSpinner;
    private Spinner fuelTypeSpinner;
    private EditText kmInput;
    private AutoCompleteTextView stationInput;
    private EditText litersInput;
    private EditText totalValueInput;
    private TextView locationText;
    private TextView litersSummaryText;
    private TextView totalSummaryText;
    private TextView historyCountText;
    private LinearLayout historyContainer;
    private EditText vehicleNameInput;
    private EditText vehiclePlateInput;
    private LinearLayout vehiclesContainer;
    private EditText stationNameInput;
    private EditText stationNeighborhoodInput;
    private LinearLayout stationsContainer;
    private Location lastLocation;
    private int currentPage = PAGE_REGISTER;
    private int editingVehicleIndex = -1;
    private int editingStationIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        ensureDefaultVehicle();
        ensureDefaultStation();
        setBarsForTheme();
        setContentView(buildContent());
        refreshCurrentPage();
    }

    private View buildContent() {
        switch (currentPage) {
            case PAGE_HISTORY:
                return buildHistoryContent();
            case PAGE_VEHICLES:
                return buildVehiclesContent();
            case PAGE_STATIONS:
                return buildStationsContent();
            case PAGE_SETTINGS:
                return buildSettingsContent();
            case PAGE_REGISTER:
            default:
                return buildRegisterContent();
        }
    }

    private View buildRegisterContent() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(appColor(R.color.screen_background));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        screen.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(18));
        root.setBackgroundColor(appColor(R.color.screen_background));
        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("Controle Gasolina");
        title.setTextColor(appColor(R.color.text_primary));
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Registre o abastecimento e acompanhe o custo por litro.");
        subtitle.setTextColor(appColor(R.color.text_secondary));
        subtitle.setTextSize(14);
        subtitle.setPadding(0, dp(4), 0, dp(16));
        root.addView(subtitle);

        vehicleSpinner = new Spinner(this);
        vehicleSpinner.setBackground(cardBackground(appColor(R.color.card_background), dp(8), appColor(R.color.border)));
        vehicleSpinner.setPadding(dp(8), 0, dp(8), 0);
        root.addView(vehicleSpinner, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(54)));

        LinearLayout summaryRow = new LinearLayout(this);
        summaryRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams summaryRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        summaryRowParams.setMargins(0, dp(16), 0, dp(16));
        root.addView(summaryRow, summaryRowParams);

        litersSummaryText = new TextView(this);
        totalSummaryText = new TextView(this);
        summaryRow.addView(summaryCard(R.drawable.ic_liters, "Litros", "0,0 L", litersSummaryText), weightedCardParams(0));
        summaryRow.addView(summaryCard(R.drawable.ic_money, "Total", "R$ 0,00", totalSummaryText), weightedCardParams(dp(10)));

        LinearLayout form = section();
        root.addView(form);

        LinearLayout formHeader = new LinearLayout(this);
        formHeader.setOrientation(LinearLayout.HORIZONTAL);
        formHeader.setGravity(Gravity.CENTER_VERTICAL);
        form.addView(formHeader);

        TextView formTitle = new TextView(this);
        formTitle.setText("Novo abastecimento");
        formTitle.setTextColor(appColor(R.color.text_primary));
        formTitle.setTextSize(14);
        formTitle.setTypeface(Typeface.DEFAULT_BOLD);
        formHeader.addView(formTitle, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        fuelTypeSpinner = new Spinner(this);
        ArrayAdapter<String> fuelAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Gasolina", "Etanol", "Diesel", "GNV", "Flex/Outro"}
        );
        fuelTypeSpinner.setAdapter(fuelAdapter);
        fuelTypeSpinner.setBackground(cardBackground(appColor(R.color.brand_green_soft), dp(12), appColor(R.color.brand_green_soft)));
        fuelTypeSpinner.setPadding(dp(6), 0, dp(6), 0);
        formHeader.addView(fuelTypeSpinner, new LinearLayout.LayoutParams(dp(118), dp(36)));

        LinearLayout valueRow = new LinearLayout(this);
        valueRow.setOrientation(LinearLayout.HORIZONTAL);
        valueRow.setPadding(0, dp(12), 0, 0);
        form.addView(valueRow);

        litersInput = input("Litros abastecidos", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        valueRow.addView(inputBlock(R.drawable.ic_liters, "Litros", litersInput), weightedCardParams(0));

        totalValueInput = input("Valor pago", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        applyCurrencyMask(totalValueInput);
        valueRow.addView(inputBlock(R.drawable.ic_money, "Valor total", totalValueInput), weightedCardParams(dp(8)));

        kmInput = input("Quilometragem atual (opcional)", InputType.TYPE_CLASS_NUMBER);
        form.addView(spacedField(R.drawable.ic_gauge, "Quilometragem", kmInput));

        stationInput = autoCompleteInput("Selecione o posto", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        form.addView(spacedField(R.drawable.ic_location, "Posto", stationInput));

        locationText = new TextView(this);
        locationText.setText("GPS: aguardando permissao");
        locationText.setTextColor(appColor(R.color.warning_text));
        locationText.setTextSize(12);
        locationText.setBackground(cardBackground(appColor(R.color.warning_background), dp(8), appColor(R.color.warning_background)));
        locationText.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams locationParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        locationParams.setMargins(0, dp(10), 0, dp(10));
        form.addView(locationText, locationParams);

        Button saveButton = new Button(this);
        saveButton.setText("Salvar abastecimento");
        saveButton.setTextColor(color(android.R.color.white));
        saveButton.setTextSize(14);
        saveButton.setTypeface(Typeface.DEFAULT_BOLD);
        saveButton.setBackground(cardBackground(appColor(R.color.brand_green), dp(8), appColor(R.color.brand_green)));
        saveButton.setOnClickListener(v -> saveRefuel());
        form.addView(saveButton, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));

        LinearLayout historyHeader = new LinearLayout(this);
        historyHeader.setGravity(Gravity.CENTER_VERTICAL);
        historyHeader.setPadding(0, dp(18), 0, dp(10));
        root.addView(historyHeader);

        TextView historyTitle = sectionTitle("Historico");
        historyHeader.addView(historyTitle, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        historyCountText = new TextView(this);
        historyCountText.setText("0 registros");
        historyCountText.setTextColor(appColor(R.color.brand_green));
        historyCountText.setTextSize(12);
        historyCountText.setTypeface(Typeface.DEFAULT_BOLD);
        historyHeader.addView(historyCountText);

        historyContainer = new LinearLayout(this);
        historyContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(historyContainer);

        View bottomNav = bottomNavigation();
        screen.addView(bottomNav, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(76)
        ));
        applySystemBarSpacing(screen, bottomNav);

        return screen;
    }

    private View buildHistoryContent() {
        LinearLayout screen = pageShell();
        ScrollView scrollView = scrollArea(screen);
        LinearLayout root = pageRoot(scrollView);

        addPageHeader(root, "Historico", "Consulte abastecimentos, custos e consumo registrados.");

        LinearLayout summaryRow = new LinearLayout(this);
        summaryRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams summaryRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        summaryRowParams.setMargins(0, 0, 0, dp(16));
        root.addView(summaryRow, summaryRowParams);

        litersSummaryText = new TextView(this);
        totalSummaryText = new TextView(this);
        summaryRow.addView(summaryCard(R.drawable.ic_liters, "Litros", "0,0 L", litersSummaryText), weightedCardParams(0));
        summaryRow.addView(summaryCard(R.drawable.ic_money, "Total", "R$ 0,00", totalSummaryText), weightedCardParams(dp(10)));

        LinearLayout historyHeader = new LinearLayout(this);
        historyHeader.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(historyHeader);

        historyHeader.addView(sectionTitle("Registros recentes"), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        historyCountText = new TextView(this);
        historyCountText.setText("0 registros");
        historyCountText.setTextColor(appColor(R.color.brand_green));
        historyCountText.setTextSize(12);
        historyCountText.setTypeface(Typeface.DEFAULT_BOLD);
        historyHeader.addView(historyCountText);

        historyContainer = new LinearLayout(this);
        historyContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams historyParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        historyParams.setMargins(0, dp(10), 0, 0);
        root.addView(historyContainer, historyParams);

        View bottomNav = bottomNavigation();
        screen.addView(bottomNav, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(76)));
        applySystemBarSpacing(screen, bottomNav);
        return screen;
    }

    private View buildVehiclesContent() {
        LinearLayout screen = pageShell();
        ScrollView scrollView = scrollArea(screen);
        LinearLayout root = pageRoot(scrollView);

        addPageHeader(root, "Veiculos", "Cadastre seus veiculos para separar os abastecimentos.");

        LinearLayout form = section();
        root.addView(form);
        form.addView(sectionTitle("Novo veiculo"));

        vehicleNameInput = input("Meu veiculo", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        form.addView(spacedField(R.drawable.ic_car, "Nome do veiculo", vehicleNameInput));

        vehiclePlateInput = input("ABC-1D23", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        form.addView(spacedField(R.drawable.ic_tag, "Placa", vehiclePlateInput));

        Button saveVehicle = primaryButton("Salvar veiculo", 0);
        saveVehicle.setOnClickListener(v -> saveVehicleFromPage());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        buttonParams.setMargins(0, dp(10), 0, 0);
        form.addView(saveVehicle, buttonParams);

        LinearLayout listHeader = new LinearLayout(this);
        listHeader.setGravity(Gravity.CENTER_VERTICAL);
        listHeader.setPadding(0, dp(18), 0, dp(10));
        root.addView(listHeader);
        listHeader.addView(sectionTitle("Veiculos cadastrados"), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        vehiclesContainer = new LinearLayout(this);
        vehiclesContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(vehiclesContainer);

        View bottomNav = bottomNavigation();
        screen.addView(bottomNav, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(76)));
        applySystemBarSpacing(screen, bottomNav);
        return screen;
    }

    private View buildStationsContent() {
        LinearLayout screen = pageShell();
        ScrollView scrollView = scrollArea(screen);
        LinearLayout root = pageRoot(scrollView);

        addPageHeader(root, "Postos", "Organize os postos usados nos seus abastecimentos.");

        LinearLayout form = section();
        root.addView(form);
        form.addView(sectionTitle("Novo posto"));

        stationNameInput = input("Posto Central", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        form.addView(spacedField(R.drawable.ic_fuel, "Nome do posto", stationNameInput));

        stationNeighborhoodInput = input("Centro", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        form.addView(spacedField(R.drawable.ic_location, "Bairro", stationNeighborhoodInput));

        Button saveStation = primaryButton("Salvar posto", 0);
        saveStation.setOnClickListener(v -> saveStationFromPage());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        buttonParams.setMargins(0, dp(10), 0, 0);
        form.addView(saveStation, buttonParams);

        LinearLayout listHeader = new LinearLayout(this);
        listHeader.setGravity(Gravity.CENTER_VERTICAL);
        listHeader.setPadding(0, dp(18), 0, dp(10));
        root.addView(listHeader);
        listHeader.addView(sectionTitle("Postos cadastrados"), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        stationsContainer = new LinearLayout(this);
        stationsContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(stationsContainer);

        View bottomNav = bottomNavigation();
        screen.addView(bottomNav, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(76)));
        applySystemBarSpacing(screen, bottomNav);
        return screen;
    }

    private View buildSettingsContent() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(appColor(R.color.screen_background));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        screen.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(18));
        root.setBackgroundColor(appColor(R.color.screen_background));
        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("Configuracoes");
        title.setTextColor(appColor(R.color.text_primary));
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Ajuste preferencias do aplicativo e escolha como a interface deve aparecer.");
        subtitle.setTextColor(appColor(R.color.text_secondary));
        subtitle.setTextSize(14);
        subtitle.setPadding(0, dp(4), 0, dp(16));
        root.addView(subtitle);

        LinearLayout appearance = section();
        root.addView(appearance);
        appearance.addView(sectionTitle("Aparencia"));
        appearance.addView(settingsRow(R.drawable.ic_settings, "Tema escuro", "Ative para usar fundo escuro e economia visual a noite.", true));

        LinearLayout appSection = section();
        LinearLayout.LayoutParams appParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        appParams.setMargins(0, dp(12), 0, 0);
        appSection.setLayoutParams(appParams);
        root.addView(appSection);
        appSection.addView(sectionTitle("Aplicativo"));
        appSection.addView(settingsRow(R.drawable.ic_clock, "Lembretes", "Alertas de abastecimento em breve.", false));
        appSection.addView(settingsRow(R.drawable.ic_location, "Dados locais", "Registros salvos somente neste aparelho.", false));

        TextView version = new TextView(this);
        version.setText("Controle Gasolina - versao 1.0");
        version.setTextColor(appColor(R.color.text_secondary));
        version.setTextSize(12);
        version.setGravity(Gravity.CENTER_VERTICAL);
        version.setPadding(dp(12), 0, dp(12), 0);
        version.setBackground(cardBackground(appColor(R.color.card_background), dp(8), appColor(R.color.border)));
        LinearLayout.LayoutParams versionParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(42)
        );
        versionParams.setMargins(0, dp(12), 0, 0);
        root.addView(version, versionParams);

        View bottomNav = bottomNavigation();
        screen.addView(bottomNav, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(76)
        ));
        applySystemBarSpacing(screen, bottomNav);

        return screen;
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

    private void saveVehicleFromPage() {
        String name = vehicleNameInput == null ? "" : vehicleNameInput.getText().toString().trim();
        String plate = vehiclePlateInput == null ? "" : vehiclePlateInput.getText().toString().trim();
        String label = plate.isEmpty() ? name : name + " - " + plate;
        if (label.trim().isEmpty()) {
            toast("Informe um nome para o veiculo.");
            return;
        }

        try {
            JSONArray vehicles = getArray(KEY_VEHICLES);
            for (int i = 0; i < vehicles.length(); i++) {
                if (i != editingVehicleIndex && label.equalsIgnoreCase(vehicles.getString(i))) {
                    toast("Esse veiculo ja esta cadastrado.");
                    return;
                }
            }

            if (editingVehicleIndex >= 0 && editingVehicleIndex < vehicles.length()) {
                vehicles.put(editingVehicleIndex, label);
            } else {
                vehicles.put(label);
            }
            putArray(KEY_VEHICLES, vehicles);

            editingVehicleIndex = -1;
            if (vehicleNameInput != null) vehicleNameInput.setText("");
            if (vehiclePlateInput != null) vehiclePlateInput.setText("");
            refreshVehicleList();
            toast("Veiculo salvo.");
        } catch (JSONException e) {
            toast("Nao foi possivel salvar o veiculo.");
        }
    }

    private void saveStationFromPage() {
        String name = stationNameInput == null ? "" : stationNameInput.getText().toString().trim();
        String neighborhood = stationNeighborhoodInput == null ? "" : stationNeighborhoodInput.getText().toString().trim();
        if (name.isEmpty()) {
            toast("Informe o nome do posto.");
            return;
        }

        try {
            JSONArray stations = getArray(KEY_STATIONS);
            for (int i = 0; i < stations.length(); i++) {
                JSONObject station = stations.getJSONObject(i);
                if (i != editingStationIndex && name.equalsIgnoreCase(station.getString("name"))) {
                    toast("Esse posto ja esta cadastrado.");
                    return;
                }
            }

            JSONObject station = new JSONObject();
            station.put("name", name);
            station.put("neighborhood", neighborhood);
            if (editingStationIndex >= 0 && editingStationIndex < stations.length()) {
                stations.put(editingStationIndex, station);
            } else {
                stations.put(station);
            }
            putArray(KEY_STATIONS, stations);

            editingStationIndex = -1;
            stationNameInput.setText("");
            stationNeighborhoodInput.setText("");
            refreshStationList();
            toast("Posto salvo.");
        } catch (JSONException e) {
            toast("Nao foi possivel salvar o posto.");
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

    private void refreshCurrentPage() {
        if (currentPage == PAGE_REGISTER) {
            refreshVehicles();
            refreshStationOptions();
            refreshHistory();
            requestLocationIfPossible();
        } else if (currentPage == PAGE_HISTORY) {
            refreshHistory();
        } else if (currentPage == PAGE_VEHICLES) {
            refreshVehicleList();
        } else if (currentPage == PAGE_STATIONS) {
            refreshStationList();
        }
    }

    private void refreshVehicles() {
        if (vehicleSpinner == null) {
            return;
        }
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

    private void refreshStationOptions() {
        if (stationInput == null) {
            return;
        }
        try {
            JSONArray stations = getArray(KEY_STATIONS);
            List<String> names = new ArrayList<>();
            for (int i = 0; i < stations.length(); i++) {
                String name = stations.getJSONObject(i).optString("name", "");
                if (!name.isEmpty()) {
                    names.add(name);
                }
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names);
            stationInput.setAdapter(adapter);
        } catch (JSONException e) {
            toast("Erro ao carregar postos.");
        }
    }

    private void refreshVehicleList() {
        if (vehiclesContainer == null) {
            return;
        }
        vehiclesContainer.removeAllViews();
        try {
            JSONArray vehicles = getArray(KEY_VEHICLES);
            if (vehicles.length() == 0) {
                vehiclesContainer.addView(emptyText("Nenhum veiculo cadastrado ainda."));
                return;
            }
            for (int i = 0; i < vehicles.length(); i++) {
                String label = vehicles.getString(i);
                final int index = i;
                LinearLayout card = simpleListCard(
                        R.drawable.ic_car,
                        label,
                        i == editingVehicleIndex ? "Editando este veiculo." : "Use o lapis para editar.",
                        v -> selectVehicleForEdit(index),
                        v -> confirmRemoveVehicle(index)
                );
                vehiclesContainer.addView(card);
            }
        } catch (JSONException e) {
            toast("Erro ao carregar veiculos.");
        }
    }

    private void selectVehicleForEdit(int index) {
        try {
            JSONArray vehicles = getArray(KEY_VEHICLES);
            if (index < 0 || index >= vehicles.length()) {
                return;
            }

            String label = vehicles.getString(index);
            int separator = label.lastIndexOf(" - ");
            if (separator >= 0) {
                vehicleNameInput.setText(label.substring(0, separator));
                vehiclePlateInput.setText(label.substring(separator + 3));
            } else {
                vehicleNameInput.setText(label);
                vehiclePlateInput.setText("");
            }
            editingVehicleIndex = index;
            refreshVehicleList();
            toast("Dados do veiculo carregados.");
        } catch (JSONException e) {
            toast("Nao foi possivel carregar o veiculo.");
        }
    }

    private void confirmRemoveVehicle(int index) {
        try {
            JSONArray vehicles = getArray(KEY_VEHICLES);
            if (index < 0 || index >= vehicles.length()) {
                return;
            }
            String label = vehicles.getString(index);
            new AlertDialog.Builder(this)
                    .setTitle("Remover veiculo")
                    .setMessage("Deseja remover \"" + label + "\"?")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Remover", (dialog, which) -> removeVehicle(index))
                    .show();
        } catch (JSONException e) {
            toast("Nao foi possivel carregar o veiculo.");
        }
    }

    private void removeVehicle(int index) {
        try {
            JSONArray vehicles = getArray(KEY_VEHICLES);
            if (index < 0 || index >= vehicles.length()) {
                return;
            }
            vehicles.remove(index);
            putArray(KEY_VEHICLES, vehicles);
            if (editingVehicleIndex == index) {
                editingVehicleIndex = -1;
                if (vehicleNameInput != null) vehicleNameInput.setText("");
                if (vehiclePlateInput != null) vehiclePlateInput.setText("");
            } else if (editingVehicleIndex > index) {
                editingVehicleIndex--;
            }
            refreshVehicleList();
            refreshVehicles();
            toast("Veiculo removido.");
        } catch (JSONException e) {
            toast("Nao foi possivel remover o veiculo.");
        }
    }

    private void refreshStationList() {
        if (stationsContainer == null) {
            return;
        }
        stationsContainer.removeAllViews();
        try {
            JSONArray stations = getArray(KEY_STATIONS);
            if (stations.length() == 0) {
                stationsContainer.addView(emptyText("Nenhum posto cadastrado ainda."));
                return;
            }
            for (int i = 0; i < stations.length(); i++) {
                JSONObject station = stations.getJSONObject(i);
                String neighborhood = station.optString("neighborhood", "");
                String subtitle = neighborhood.isEmpty() ? "Bairro nao informado." : neighborhood;
                if (i == editingStationIndex) {
                    subtitle = "Editando este posto.";
                }
                final int index = i;
                LinearLayout card = simpleListCard(
                        R.drawable.ic_fuel,
                        station.getString("name"),
                        subtitle,
                        v -> selectStationForEdit(index),
                        v -> confirmRemoveStation(index)
                );
                stationsContainer.addView(card);
            }
        } catch (JSONException e) {
            toast("Erro ao carregar postos.");
        }
    }

    private void selectStationForEdit(int index) {
        try {
            JSONArray stations = getArray(KEY_STATIONS);
            if (index < 0 || index >= stations.length()) {
                return;
            }

            JSONObject station = stations.getJSONObject(index);
            stationNameInput.setText(station.optString("name", ""));
            stationNeighborhoodInput.setText(station.optString("neighborhood", ""));
            editingStationIndex = index;
            refreshStationList();
            toast("Dados do posto carregados.");
        } catch (JSONException e) {
            toast("Nao foi possivel carregar o posto.");
        }
    }

    private void confirmRemoveStation(int index) {
        try {
            JSONArray stations = getArray(KEY_STATIONS);
            if (index < 0 || index >= stations.length()) {
                return;
            }
            String name = stations.getJSONObject(index).optString("name", "posto");
            new AlertDialog.Builder(this)
                    .setTitle("Remover posto")
                    .setMessage("Deseja remover \"" + name + "\"?")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Remover", (dialog, which) -> removeStation(index))
                    .show();
        } catch (JSONException e) {
            toast("Nao foi possivel carregar o posto.");
        }
    }

    private void removeStation(int index) {
        try {
            JSONArray stations = getArray(KEY_STATIONS);
            if (index < 0 || index >= stations.length()) {
                return;
            }
            stations.remove(index);
            putArray(KEY_STATIONS, stations);
            if (editingStationIndex == index) {
                editingStationIndex = -1;
                if (stationNameInput != null) stationNameInput.setText("");
                if (stationNeighborhoodInput != null) stationNeighborhoodInput.setText("");
            } else if (editingStationIndex > index) {
                editingStationIndex--;
            }
            refreshStationList();
            toast("Posto removido.");
        } catch (JSONException e) {
            toast("Nao foi possivel remover o posto.");
        }
    }

    private void refreshHistory() {
        if (historyContainer == null || litersSummaryText == null || totalSummaryText == null || historyCountText == null) {
            return;
        }
        historyContainer.removeAllViews();
        try {
            JSONArray refuels = getArray(KEY_REFUELS);
            updateSummary(refuels);
            if (refuels.length() == 0) {
                TextView empty = new TextView(this);
                empty.setText("Nenhum abastecimento cadastrado ainda.");
                empty.setTextColor(appColor(R.color.text_secondary));
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
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(cardBackground(appColor(R.color.card_background), dp(8), appColor(R.color.border)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(params);

        card.addView(iconBox(R.drawable.ic_fuel, dp(34), appColor(R.color.brand_green_soft), appColor(R.color.brand_green)),
                new LinearLayout.LayoutParams(dp(34), dp(34)));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        contentParams.setMargins(dp(10), 0, 0, 0);
        card.addView(content, contentParams);

        TextView title = new TextView(this);
        title.setText(item.getString("vehicle") + " - " + item.getString("fuelType"));
        title.setTextColor(appColor(R.color.text_primary));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextSize(13);
        content.addView(title);

        double liters = item.optDouble("liters", 0);
        double total = item.optDouble("totalValue", 0);
        String odometer = item.optString("odometer", "");
        String km = odometer.isEmpty() ? "" : " | Km " + odometer;
        String unitPrice = liters > 0 ? moneyFormat.format(total / liters) + "/L" : "-";
        String details = dateFormat.format(new Date(item.getLong("timestamp")))
                + km
                + "\n" + item.getString("station")
                + "\n" + String.format(Locale.getDefault(), "%.2f L", liters)
                + " | " + moneyFormat.format(total)
                + " | " + unitPrice;

        if (!item.isNull("latitude") && !item.isNull("longitude")) {
            details += String.format(Locale.getDefault(), "\nGPS: %.5f, %.5f", item.getDouble("latitude"), item.getDouble("longitude"));
        }

        TextView body = new TextView(this);
        body.setText(details);
        body.setTextColor(appColor(R.color.text_secondary));
        body.setTextSize(11);
        body.setPadding(0, dp(4), 0, 0);
        content.addView(body);

        return card;
    }

    private void requestLocationIfPossible() {
        if (locationText == null) {
            return;
        }
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
        locationText.setTextColor(appColor(R.color.brand_green));
        locationText.setBackground(cardBackground(appColor(R.color.brand_green_soft), dp(8), appColor(R.color.brand_green_soft)));
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

    private void ensureDefaultStation() {
        if (!prefs.contains(KEY_STATIONS)) {
            JSONArray stations = new JSONArray();
            JSONObject station = new JSONObject();
            try {
                station.put("name", "Posto Central");
                station.put("neighborhood", "Centro");
                stations.put(station);
                putArray(KEY_STATIONS, stations);
            } catch (JSONException ignored) {
                putArray(KEY_STATIONS, new JSONArray());
            }
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
        input.setTextColor(appColor(R.color.text_primary));
        input.setHintTextColor(appColor(R.color.text_secondary));
        input.setInputType(inputType);
        input.setTextSize(13);
        input.setBackground(cardBackground(appColor(R.color.field_background), dp(8), appColor(R.color.border)));
        input.setPadding(dp(12), 0, dp(12), 0);
        input.setLayoutParams(fieldParams());
        return input;
    }

    private AutoCompleteTextView autoCompleteInput(String hint, int inputType) {
        AutoCompleteTextView input = new AutoCompleteTextView(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setTextColor(appColor(R.color.text_primary));
        input.setHintTextColor(appColor(R.color.text_secondary));
        input.setInputType(inputType);
        input.setTextSize(13);
        input.setThreshold(0);
        input.setBackground(cardBackground(appColor(R.color.field_background), dp(8), appColor(R.color.border)));
        input.setPadding(dp(12), 0, dp(12), 0);
        input.setLayoutParams(fieldParams());
        input.setOnClickListener(v -> input.showDropDown());
        input.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                input.showDropDown();
            }
        });
        return input;
    }

    private void applyCurrencyMask(EditText input) {
        input.addTextChangedListener(new TextWatcher() {
            private boolean updating;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (updating) {
                    return;
                }

                String digits = onlyDigits(editable.toString());
                if (digits.isEmpty()) {
                    return;
                }

                updating = true;
                double value = Double.parseDouble(digits) / 100.0;
                input.setText(moneyFormat.format(value));
                input.setSelection(input.getText().length());
                updating = false;
            }
        });
    }

    private LinearLayout pageShell() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(appColor(R.color.screen_background));
        return screen;
    }

    private ScrollView scrollArea(LinearLayout screen) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        screen.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1
        ));
        return scrollView;
    }

    private LinearLayout pageRoot(ScrollView scrollView) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(18));
        root.setBackgroundColor(appColor(R.color.screen_background));
        scrollView.addView(root);
        return root;
    }

    private void addPageHeader(LinearLayout root, String titleText, String subtitleText) {
        TextView title = new TextView(this);
        title.setText(titleText);
        title.setTextColor(appColor(R.color.text_primary));
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(subtitleText);
        subtitle.setTextColor(appColor(R.color.text_secondary));
        subtitle.setTextSize(14);
        subtitle.setPadding(0, dp(4), 0, dp(16));
        root.addView(subtitle);
    }

    private LinearLayout section() {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setPadding(dp(14), dp(12), dp(14), dp(14));
        section.setBackground(cardBackground(appColor(R.color.card_background), dp(8), appColor(R.color.border)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        section.setLayoutParams(params);
        return section;
    }

    private LinearLayout.LayoutParams fieldParams() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48));
    }

    private GradientDrawable cardBackground(int fillColor, int radius, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private LinearLayout summaryCard(int iconRes, String label, String fallback, TextView valueView) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(cardBackground(appColor(R.color.card_background), dp(8), appColor(R.color.border)));

        LinearLayout labelRow = new LinearLayout(this);
        labelRow.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(labelRow);

        labelRow.addView(iconView(iconRes, appColor(R.color.brand_green), dp(17)), new LinearLayout.LayoutParams(dp(17), dp(17)));

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(appColor(R.color.text_secondary));
        labelView.setTextSize(11);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        labelParams.setMargins(dp(6), 0, 0, 0);
        labelRow.addView(labelView, labelParams);

        valueView.setText(fallback);
        valueView.setTextColor(appColor(R.color.text_primary));
        valueView.setTextSize(18);
        valueView.setTypeface(Typeface.DEFAULT_BOLD);
        valueView.setPadding(0, dp(6), 0, 0);
        card.addView(valueView);

        return card;
    }

    private LinearLayout inputBlock(int iconRes, String label, EditText input) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);

        LinearLayout labelRow = new LinearLayout(this);
        labelRow.setGravity(Gravity.CENTER_VERTICAL);
        block.addView(labelRow);

        labelRow.addView(iconView(iconRes, appColor(R.color.brand_green), dp(15)), new LinearLayout.LayoutParams(dp(15), dp(15)));

        TextView labelView = smallLabel(label);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        labelParams.setMargins(dp(6), 0, 0, 0);
        labelRow.addView(labelView, labelParams);
        block.addView(input, fieldParams());

        return block;
    }

    private LinearLayout spacedField(int iconRes, String label, EditText input) {
        LinearLayout block = inputBlock(iconRes, label, input);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(8), 0, 0);
        block.setLayoutParams(params);
        return block;
    }

    private TextView smallLabel(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(appColor(R.color.text_secondary));
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextSize(11);
        label.setPadding(0, 0, 0, dp(4));
        return label;
    }

    private TextView sectionTitle(String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextColor(appColor(R.color.text_primary));
        title.setTextSize(16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        return title;
    }

    private TextView emptyText(String text) {
        TextView empty = new TextView(this);
        empty.setText(text);
        empty.setTextColor(appColor(R.color.text_secondary));
        empty.setTextSize(15);
        return empty;
    }

    private Button primaryButton(String text, int iconRes) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(color(android.R.color.white));
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setAllCaps(false);
        if (iconRes != 0) {
            button.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0);
            button.setCompoundDrawablePadding(dp(8));
        }
        button.setBackground(cardBackground(appColor(R.color.brand_green), dp(8), appColor(R.color.brand_green)));
        return button;
    }

    private LinearLayout simpleListCard(
            int iconRes,
            String titleText,
            String descriptionText,
            View.OnClickListener editListener,
            View.OnClickListener removeListener
    ) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackground(cardBackground(appColor(R.color.card_background), dp(8), appColor(R.color.border)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(params);

        card.addView(iconBox(iconRes, dp(36), appColor(R.color.brand_green_soft), appColor(R.color.brand_green)),
                new LinearLayout.LayoutParams(dp(36), dp(36)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        copyParams.setMargins(dp(10), 0, 0, 0);
        card.addView(copy, copyParams);

        TextView title = new TextView(this);
        title.setText(titleText);
        title.setTextColor(appColor(R.color.text_primary));
        title.setTextSize(13);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        copy.addView(title);

        TextView description = new TextView(this);
        description.setText(descriptionText);
        description.setTextColor(appColor(R.color.text_secondary));
        description.setTextSize(11);
        description.setPadding(0, dp(3), 0, 0);
        copy.addView(description);

        card.setOnClickListener(editListener);
        card.addView(actionButton(R.drawable.ic_edit, appColor(R.color.field_background), appColor(R.color.border), appColor(R.color.text_secondary), "Editar", editListener),
                new LinearLayout.LayoutParams(dp(36), dp(36)));
        LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        removeParams.setMargins(dp(8), 0, 0, 0);
        card.addView(actionButton(R.drawable.ic_trash, dangerBackgroundColor(), dangerBorderColor(), dangerTextColor(), "Remover", removeListener), removeParams);

        return card;
    }

    private View actionButton(int iconRes, int backgroundColor, int borderColor, int tintColor, String description, View.OnClickListener listener) {
        LinearLayout button = new LinearLayout(this);
        button.setGravity(Gravity.CENTER);
        button.setBackground(cardBackground(backgroundColor, dp(8), borderColor));
        button.setContentDescription(description);
        button.setClickable(true);
        button.setFocusable(true);
        button.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClick(v);
            }
        });
        button.addView(iconView(iconRes, tintColor, dp(17)), new LinearLayout.LayoutParams(dp(17), dp(17)));
        return button;
    }

    private LinearLayout iconBox(int iconRes, int size, int backgroundColor, int tintColor) {
        LinearLayout box = new LinearLayout(this);
        box.setGravity(Gravity.CENTER);
        box.setBackground(cardBackground(backgroundColor, dp(8), backgroundColor));
        box.addView(iconView(iconRes, tintColor, Math.max(dp(18), size - dp(16))),
                new LinearLayout.LayoutParams(Math.max(dp(18), size - dp(16)), Math.max(dp(18), size - dp(16))));
        return box;
    }

    private int dangerBackgroundColor() {
        return isDarkTheme() ? 0xFF241A1A : 0xFFFFF4F2;
    }

    private int dangerBorderColor() {
        return isDarkTheme() ? 0xFF5A2A2A : 0xFFF1C8C1;
    }

    private int dangerTextColor() {
        return isDarkTheme() ? 0xFFFF8A7A : 0xFFC24132;
    }

    private ImageView iconView(int iconRes, int tintColor, int size) {
        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(tintColor);
        icon.setAdjustViewBounds(true);
        icon.setMaxWidth(size);
        icon.setMaxHeight(size);
        return icon;
    }

    private View settingsRow(int iconRes, String titleText, String descriptionText, boolean hasSwitch) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(12), 0, 0);

        LinearLayout iconBox = iconBox(iconRes, dp(38), appColor(R.color.brand_green_soft), appColor(R.color.brand_green));
        row.addView(iconBox, new LinearLayout.LayoutParams(dp(38), dp(38)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        copyParams.setMargins(dp(10), 0, dp(10), 0);
        row.addView(copy, copyParams);

        TextView title = new TextView(this);
        title.setText(titleText);
        title.setTextColor(appColor(R.color.text_primary));
        title.setTextSize(13);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        copy.addView(title);

        TextView description = new TextView(this);
        description.setText(descriptionText);
        description.setTextColor(appColor(R.color.text_secondary));
        description.setTextSize(11);
        copy.addView(description);

        if (hasSwitch) {
            Switch themeSwitch = new Switch(this);
            themeSwitch.setChecked(isDarkTheme());
            themeSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
                prefs.edit().putBoolean(KEY_DARK_THEME, checked).apply();
                setBarsForTheme();
                setContentView(buildContent());
            });
            row.addView(themeSwitch);
        }

        return row;
    }

    private LinearLayout.LayoutParams weightedCardParams(int leftMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        params.setMargins(leftMargin, 0, 0, 0);
        return params;
    }

    private View bottomNavigation() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(16), dp(10), dp(16), dp(14));
        nav.setBackgroundColor(appColor(R.color.screen_background));

        nav.addView(navItem(R.drawable.ic_fuel, "Registro", currentPage == PAGE_REGISTER, PAGE_REGISTER), weightedNavParams(0));
        nav.addView(navItem(R.drawable.ic_clock, "Historico", currentPage == PAGE_HISTORY, PAGE_HISTORY), weightedNavParams(dp(6)));
        nav.addView(navItem(R.drawable.ic_car, "Veiculos", currentPage == PAGE_VEHICLES, PAGE_VEHICLES), weightedNavParams(dp(6)));
        nav.addView(navItem(R.drawable.ic_location, "Postos", currentPage == PAGE_STATIONS, PAGE_STATIONS), weightedNavParams(dp(6)));
        nav.addView(navItem(R.drawable.ic_settings, "Config", currentPage == PAGE_SETTINGS, PAGE_SETTINGS), weightedNavParams(dp(6)));

        return nav;
    }

    private LinearLayout navItem(int iconRes, String text, boolean selected, int page) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setBackground(cardBackground(
                appColor(selected ? R.color.brand_green_soft : R.color.screen_background),
                dp(24),
                appColor(selected ? R.color.brand_green_soft : R.color.screen_background)
        ));

        int tint = appColor(selected ? R.color.brand_green : R.color.text_secondary);
        item.addView(iconView(iconRes, tint, dp(20)), new LinearLayout.LayoutParams(dp(20), dp(20)));

        TextView label = new TextView(this);
        label.setText(text);
        label.setGravity(Gravity.CENTER);
        label.setTextSize(10);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextColor(tint);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        labelParams.setMargins(0, dp(2), 0, 0);
        item.addView(label, labelParams);

        item.setOnClickListener(v -> {
            currentPage = page;
            setContentView(buildContent());
            refreshCurrentPage();
        });
        return item;
    }

    private LinearLayout.LayoutParams weightedNavParams(int leftMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(52), 1);
        params.setMargins(leftMargin, 0, 0, 0);
        return params;
    }

    private void updateSummary(JSONArray refuels) throws JSONException {
        double totalLiters = 0;
        double totalValue = 0;
        for (int i = 0; i < refuels.length(); i++) {
            JSONObject item = refuels.getJSONObject(i);
            totalLiters += item.optDouble("liters", 0);
            totalValue += item.optDouble("totalValue", 0);
        }
        litersSummaryText.setText(String.format(Locale.getDefault(), "%.1f L", totalLiters));
        totalSummaryText.setText(moneyFormat.format(totalValue));
        historyCountText.setText(refuels.length() == 1 ? "1 registro" : refuels.length() + " registros");
    }

    private double parseDecimal(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        try {
            String normalized = onlyNumberSymbols(text);
            if (normalized.contains(",")) {
                normalized = normalized.replace(".", "").replace(",", ".");
            }
            return Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String onlyDigits(String text) {
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (Character.isDigit(character)) {
                digits.append(character);
            }
        }
        return digits.toString();
    }

    private String onlyNumberSymbols(String text) {
        StringBuilder number = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (Character.isDigit(character) || character == ',' || character == '.') {
                number.append(character);
            }
        }
        return number.toString();
    }

    private int color(int id) {
        return getResources().getColor(id, getTheme());
    }

    private int appColor(int id) {
        if (!isDarkTheme()) {
            return color(id);
        }
        if (id == R.color.screen_background) return 0xFF101512;
        if (id == R.color.card_background) return 0xFF19211C;
        if (id == R.color.field_background) return 0xFF121A15;
        if (id == R.color.border) return 0xFF2D3931;
        if (id == R.color.brand_green) return 0xFF58C98D;
        if (id == R.color.brand_green_dark) return 0xFF0B2F22;
        if (id == R.color.brand_green_soft) return 0xFF20392C;
        if (id == R.color.warning_background) return 0xFF3A321C;
        if (id == R.color.warning_text) return 0xFFF1C95A;
        if (id == R.color.text_primary) return 0xFFEAF2EC;
        if (id == R.color.text_secondary) return 0xFFA8B4AB;
        return color(id);
    }

    private boolean isDarkTheme() {
        return prefs != null && prefs.getBoolean(KEY_DARK_THEME, false);
    }

    private void setBarsForTheme() {
        getWindow().setStatusBarColor(appColor(R.color.brand_green_dark));
        getWindow().setNavigationBarColor(appColor(R.color.screen_background));
    }

    private void applySystemBarSpacing(LinearLayout screen, View bottomNav) {
        int screenLeft = screen.getPaddingLeft();
        int screenTop = screen.getPaddingTop();
        int screenRight = screen.getPaddingRight();
        int screenBottom = screen.getPaddingBottom();
        int navLeft = bottomNav.getPaddingLeft();
        int navTop = bottomNav.getPaddingTop();
        int navRight = bottomNav.getPaddingRight();
        int navBottom = bottomNav.getPaddingBottom();
        int navBaseHeight = dp(76);

        screen.setOnApplyWindowInsetsListener((view, insets) -> {
            int statusBarHeight = insets.getSystemWindowInsetTop();
            int navigationBarHeight = insets.getSystemWindowInsetBottom();

            view.setPadding(
                    screenLeft,
                    screenTop + statusBarHeight,
                    screenRight,
                    screenBottom
            );

            bottomNav.setPadding(
                    navLeft,
                    navTop,
                    navRight,
                    navBottom + navigationBarHeight
            );

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) bottomNav.getLayoutParams();
            params.height = navBaseHeight + navigationBarHeight;
            bottomNav.setLayoutParams(params);

            return insets;
        });
        screen.requestApplyInsets();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
