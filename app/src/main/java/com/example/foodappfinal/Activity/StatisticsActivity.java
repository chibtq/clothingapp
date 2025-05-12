package com.example.foodappfinal.Activity;

import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.foodappproject.R;
import com.example.foodappproject.databinding.ActivityStatisticsBinding;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class StatisticsActivity extends BaseActivity {
    private static final String TAG = "StatisticsActivity";
    private ActivityStatisticsBinding binding;
    private static final String ORDERS_URL = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/orders";
    private static final String ORDER_ITEMS_URL = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/order_items";
    private static final String PRODUCTS_URL = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/products";
    private static final String USER_PROFILES_URL = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/user_profiles";
    private static final String WISHLIST_URL = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/wishlist";
    private static final String PRODUCT_REVIEWS_URL = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/product_reviews";
    private long startDate = 0;
    private long endDate = 0;
    private String selectedCriteria = "Số lượng đơn hàng";
    private String selectedSortOrder = "Nhiều nhất";
    private int selectedLimit = 5;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStatisticsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupDatePickers();
        setupSpinners();
        setupLimitInput();
        setupRecyclerViews();
        checkAdminRoleAndLoad();
    }

    private void setupToolbar() {
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupDatePickers() {
        Calendar calendar = Calendar.getInstance();

        binding.etStartDate.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(
                    StatisticsActivity.this,
                    (view, year, month, dayOfMonth) -> {
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(year, month, dayOfMonth);
                        startDate = selectedDate.getTimeInMillis();
                        binding.etStartDate.setText(dateFormat.format(new Date(startDate)));
                        Log.d(TAG, "Start date selected: " + dateFormat.format(new Date(startDate)));
                        if (endDate > 0) loadStatistics();
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            dialog.show();
        });

        binding.etEndDate.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(
                    StatisticsActivity.this,
                    (view, year, month, dayOfMonth) -> {
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(year, month, dayOfMonth);
                        endDate = selectedDate.getTimeInMillis();
                        binding.etEndDate.setText(dateFormat.format(new Date(endDate)));
                        Log.d(TAG, "End date selected: " + dateFormat.format(new Date(endDate)));
                        if (startDate > 0) loadStatistics();
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            dialog.show();
        });
    }

    private void setupSpinners() {
        binding.spinnerCriteria.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCriteria = parent.getItemAtPosition(position).toString();
                Log.d(TAG, "Criteria selected: " + selectedCriteria);
                updateMostLeastState();
                updateLimitState();
                loadStatistics();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        binding.spinnerMostLeast.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSortOrder = parent.getItemAtPosition(position).toString();
                Log.d(TAG, "Sort order selected: " + selectedSortOrder);
                loadStatistics();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupLimitInput() {
        binding.etLimit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    selectedLimit = Integer.parseInt(s.toString());
                    if (selectedLimit <= 0) {
                        selectedLimit = 5;
                        binding.etLimit.setText("5");
                    }
                    loadStatistics();
                } catch (NumberFormatException e) {
                    selectedLimit = 5;
                    binding.etLimit.setText("5");
                }
            }
        });
    }

    private void updateMostLeastState() {
        boolean disableMostLeast = selectedCriteria.equals("Số lượng đơn hàng") || selectedCriteria.equals("Doanh thu");
        binding.spinnerMostLeast.setEnabled(!disableMostLeast);
        if (disableMostLeast) selectedSortOrder = "Nhiều nhất";
        Log.d(TAG, "MostLeast spinner enabled: " + !disableMostLeast);
    }

    private void updateLimitState() {
        boolean enableLimit = selectedCriteria.equals("Hàng tồn kho") ||
                selectedCriteria.equals("Sản phẩm bán chạy") ||
                selectedCriteria.equals("Số lượng bình luận") ||
                selectedCriteria.equals("Sản phẩm được yêu thích") ||
                selectedCriteria.equals("Doanh thu theo sản phẩm");
        binding.etLimit.setEnabled(enableLimit);
        if (!enableLimit) binding.etLimit.setText("5");
        Log.d(TAG, "Limit input enabled: " + enableLimit);
    }

    private void setupRecyclerViews() {
        binding.rvGenericTable.setLayoutManager(new LinearLayoutManager(this));
    }

    private void checkAdminRoleAndLoad() {
        String userId = getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show();
            logout();
            return;
        }

        String url = USER_PROFILES_URL + "?user_id=eq." + userId + "&select=role";
        Request request = getRequestBuilder().url(url).get().build();

        getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(StatisticsActivity.this, "Failed to check role", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d(TAG, "checkAdminRoleAndLoad response: " + responseData);
                if (response.isSuccessful()) {
                    JsonArray array = JsonParser.parseString(responseData).getAsJsonArray();
                    if (array.size() > 0 && array.get(0).getAsJsonObject().get("role").getAsString().equals("admin")) {
                        runOnUiThread(StatisticsActivity.this::loadStatistics);
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(StatisticsActivity.this, "Access denied: Admin role required", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }
                }
            }
        });
    }

    private void loadStatistics() {
        String dateFilter = "";
        if ((selectedCriteria.equals("Số lượng đơn hàng") || selectedCriteria.equals("Doanh thu") ||
                selectedCriteria.equals("Người dùng đăng ký") || selectedCriteria.equals("Doanh thu theo sản phẩm")) &&
                startDate > 0 && endDate > 0) {
            String startDateStr = apiDateFormat.format(new Date(startDate));
            String endDateStr = apiDateFormat.format(new Date(endDate));
            dateFilter = "&created_at=gt." + startDateStr + "&created_at=lt." + endDateStr;
            Log.d(TAG, "Date filter applied: " + dateFilter);
        }

        binding.chartGeneric.setVisibility(View.GONE);
        binding.chartGenericLine.setVisibility(View.GONE);
        binding.rvGenericTable.setVisibility(View.GONE);

        switch (selectedCriteria) {
            case "Số lượng đơn hàng":
                binding.tvChartTitle.setText("Số đơn hàng theo ngày");
                loadOrderCount(dateFilter);
                break;
            case "Doanh thu":
                binding.tvChartTitle.setText("Doanh thu theo ngày");
                loadRevenueData(dateFilter);
                break;
            case "Hàng tồn kho":
                binding.tvChartTitle.setText("Số lượng hàng tồn kho");
                loadStockStats();
                break;
            case "Sản phẩm bán chạy":
                binding.tvChartTitle.setText("Sản phẩm bán chạy");
                loadTopProducts(dateFilter);
                break;
            case "Người dùng đăng ký":
                binding.tvChartTitle.setText("Người dùng đăng ký mới");
                loadUserStats(dateFilter);
                break;
            case "Số lượng bình luận":
                binding.tvChartTitle.setText("Số lượng bình luận sản phẩm");
                loadCommentStats();
                break;
            case "Sản phẩm được yêu thích":
                binding.tvChartTitle.setText("Sản phẩm được yêu thích");
                loadWishlistStats();
                break;
            case "Doanh thu theo sản phẩm":
                binding.tvChartTitle.setText("Doanh thu theo sản phẩm");
                loadRevenueByProduct();
                break;
        }
    }

    private void loadOrderCount(String dateFilter) {
        String url = ORDERS_URL + "?select=created_at" + dateFilter;
        getOkHttpClient().newCall(getRequestBuilder().url(url).get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(StatisticsActivity.this, "Failed to load order count", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    JsonArray array = JsonParser.parseString(response.body().string()).getAsJsonArray();
                    Log.d(TAG, "loadOrderCount response: " + array.toString());
                    Map<String, Integer> dailyOrders = new TreeMap<>();
                    if (!dateFilter.isEmpty()) {
                        Calendar start = Calendar.getInstance();
                        start.setTime(new Date(startDate));
                        Calendar end = Calendar.getInstance();
                        end.setTime(new Date(endDate));
                        while (!start.after(end)) {
                            String dayKey = dayFormat.format(start.getTime());
                            dailyOrders.put(dayKey, 0);
                            start.add(Calendar.DAY_OF_MONTH, 1);
                        }
                    }
                    for (JsonElement element : array) {
                        String dateStr = element.getAsJsonObject().get("created_at").getAsString().split("T")[0];
                        try {
                            Date date = apiDateFormat.parse(dateStr);
                            String dayKey = dayFormat.format(date);
                            dailyOrders.merge(dayKey, 1, Integer::sum);
                        } catch (ParseException e) {
                            Log.e(TAG, "Error parsing date: " + dateStr, e);
                        }
                    }
                    List<BarEntry> entries = new ArrayList<>();
                    List<String> labels = new ArrayList<>();
                    int index = 0;
                    for (Map.Entry<String, Integer> entry : dailyOrders.entrySet()) {
                        entries.add(new BarEntry(index, entry.getValue()));
                        labels.add(entry.getKey());
                        index++;
                    }
                    BarDataSet dataSet = new BarDataSet(entries, "Số đơn hàng");
                    dataSet.setColor(Color.parseColor("#4FC3F7"));
                    dataSet.setValueTextSize(14f);
                    dataSet.setValueTextColor(Color.BLACK);
                    BarData barData = new BarData(dataSet);
                    runOnUiThread(() -> {
                        XAxis xAxis = binding.chartGeneric.getXAxis();
                        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
                        xAxis.setGranularity(1f);
                        xAxis.setLabelCount(labels.size());
                        xAxis.setTextSize(14f);
                        xAxis.setTextColor(Color.BLACK);
                        xAxis.setLabelRotationAngle(0);
                        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                        binding.chartGeneric.getAxisLeft().setTextSize(14f);
                        binding.chartGeneric.getAxisLeft().setTextColor(Color.BLACK);
                        binding.chartGeneric.getAxisRight().setDrawAxisLine(false);
                        binding.chartGeneric.getAxisRight().setDrawLabels(false);
                        binding.chartGeneric.setData(barData);
                        binding.chartGeneric.setVisibility(View.VISIBLE);
                        binding.chartGeneric.invalidate();
                    });
                }
            }
        });
    }

    private void loadRevenueData(String dateFilter) {
        String url = ORDERS_URL + "?select=total_amount,created_at" + dateFilter;
        getOkHttpClient().newCall(getRequestBuilder().url(url).get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(StatisticsActivity.this, "Failed to load revenue", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    JsonArray array = JsonParser.parseString(response.body().string()).getAsJsonArray();
                    Log.d(TAG, "loadRevenueData response: " + array.toString());
                    Map<String, Double> dailyRevenue = new TreeMap<>();
                    if (!dateFilter.isEmpty()) {
                        Calendar start = Calendar.getInstance();
                        start.setTime(new Date(startDate));
                        Calendar end = Calendar.getInstance();
                        end.setTime(new Date(endDate));
                        while (!start.after(end)) {
                            String dayKey = dayFormat.format(start.getTime());
                            dailyRevenue.put(dayKey, 0.0);
                            start.add(Calendar.DAY_OF_MONTH, 1);
                        }
                    }
                    for (JsonElement element : array) {
                        String dateStr = element.getAsJsonObject().get("created_at").getAsString().split("T")[0];
                        double amount = element.getAsJsonObject().get("total_amount").getAsDouble();
                        try {
                            Date date = apiDateFormat.parse(dateStr);
                            String dayKey = dayFormat.format(date);
                            dailyRevenue.merge(dayKey, amount, Double::sum);
                        } catch (ParseException e) {
                            Log.e(TAG, "Error parsing date: " + dateStr, e);
                        }
                    }
                    List<Entry> entries = new ArrayList<>();
                    List<String> labels = new ArrayList<>();
                    int index = 0;
                    for (Map.Entry<String, Double> entry : dailyRevenue.entrySet()) {
                        entries.add(new Entry(index, entry.getValue().floatValue()));
                        labels.add(entry.getKey());
                        index++;
                    }
                    LineDataSet dataSet = new LineDataSet(entries, "Doanh thu (₫)");
                    dataSet.setColor(Color.parseColor("#4FC3F7"));
                    dataSet.setLineWidth(2f);
                    dataSet.setValueTextSize(14f);
                    dataSet.setValueTextColor(Color.BLACK);
                    LineData lineData = new LineData(dataSet);
                    runOnUiThread(() -> {
                        XAxis xAxis = binding.chartGenericLine.getXAxis();
                        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
                        xAxis.setGranularity(1f);
                        xAxis.setLabelCount(labels.size());
                        xAxis.setTextSize(14f);
                        xAxis.setTextColor(Color.BLACK);
                        xAxis.setLabelRotationAngle(0);
                        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                        binding.chartGenericLine.getAxisLeft().setTextSize(14f);
                        binding.chartGenericLine.getAxisLeft().setTextColor(Color.BLACK);
                        binding.chartGenericLine.getAxisRight().setDrawAxisLine(false);
                        binding.chartGenericLine.getAxisRight().setDrawLabels(false);
                        binding.chartGenericLine.setData(lineData);
                        binding.chartGenericLine.setVisibility(View.VISIBLE);
                        binding.chartGenericLine.invalidate();
                    });
                }
            }
        });
    }

    private void loadStockStats() {
        String url = PRODUCTS_URL + "?select=name,stock";
        getOkHttpClient().newCall(getRequestBuilder().url(url).get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(StatisticsActivity.this, "Failed to load stock stats", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    JsonArray array = JsonParser.parseString(response.body().string()).getAsJsonArray();
                    Log.d(TAG, "loadStockStats response: " + array.toString());
                    Map<String, Integer> stockDataMap = new HashMap<>();
                    for (JsonElement element : array) {
                        JsonObject product = element.getAsJsonObject();
                        String name = product.get("name").getAsString();
                        int stock = product.get("stock").getAsInt();
                        stockDataMap.put(name, stock);
                    }

                    // Sắp xếp dữ liệu: theo selectedSortOrder, nếu bằng thì theo tên sản phẩm
                    List<Map.Entry<String, Integer>> stockData = new ArrayList<>(stockDataMap.entrySet());
                    stockData.sort((e1, e2) -> {
                        int compareValue = selectedSortOrder.equals("Nhiều nhất") ?
                                e2.getValue().compareTo(e1.getValue()) : // Nhiều nhất: giảm dần
                                e1.getValue().compareTo(e2.getValue()); // Ít nhất: tăng dần
                        if (compareValue == 0) {
                            return e1.getKey().compareTo(e2.getKey());
                        }
                        return compareValue;
                    });

                    // Giới hạn số lượng sản phẩm theo selectedLimit
                    stockData = stockData.stream().limit(selectedLimit).collect(Collectors.toList());

                    List<BarEntry> entries = new ArrayList<>();
                    List<String> labels = new ArrayList<>();
                    for (int i = 0; i < stockData.size(); i++) {
                        entries.add(new BarEntry(i, stockData.get(i).getValue()));
                        labels.add(stockData.get(i).getKey());
                    }

                    BarDataSet dataSet = new BarDataSet(entries, "Số lượng tồn kho");
                    dataSet.setColor(Color.parseColor("#4FC3F7"));
                    dataSet.setValueTextSize(14f);
                    dataSet.setValueTextColor(Color.BLACK);
                    BarData barData = new BarData(dataSet);
                    runOnUiThread(() -> {
                        XAxis xAxis = binding.chartGeneric.getXAxis();
                        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
                        xAxis.setGranularity(1f);
                        xAxis.setLabelCount(labels.size());
                        xAxis.setTextSize(14f);
                        xAxis.setTextColor(Color.BLACK);
                        xAxis.setLabelRotationAngle(45);
                        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                        binding.chartGeneric.getAxisLeft().setTextSize(14f);
                        binding.chartGeneric.getAxisLeft().setTextColor(Color.BLACK);
                        binding.chartGeneric.getAxisRight().setDrawAxisLine(false);
                        binding.chartGeneric.getAxisRight().setDrawLabels(false);
                        binding.chartGeneric.setData(barData);
                        binding.chartGeneric.setVisibility(View.VISIBLE);
                        binding.chartGeneric.invalidate();
                    });
                }
            }
        });
    }

    private void loadTopProducts(String dateFilter) {
        String urlProducts = PRODUCTS_URL + "?select=name";
        String urlOrderItems = ORDER_ITEMS_URL + "?select=product_id(name),quantity" + dateFilter;

        // Lấy danh sách tất cả sản phẩm
        getOkHttpClient().newCall(getRequestBuilder().url(urlProducts).get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(StatisticsActivity.this, "Failed to load products for top products", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    JsonArray productsArray = JsonParser.parseString(response.body().string()).getAsJsonArray();
                    Map<String, Integer> productSales = new HashMap<>();

                    // Khởi tạo tất cả sản phẩm với số lượng bán là 0
                    for (JsonElement element : productsArray) {
                        String prodName = element.getAsJsonObject().get("name").getAsString();
                        productSales.put(prodName, 0);
                    }

                    // Lấy dữ liệu từ order_items
                    getOkHttpClient().newCall(getRequestBuilder().url(urlOrderItems).get().build()).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            runOnUiThread(() -> Toast.makeText(StatisticsActivity.this, "Failed to load top products", Toast.LENGTH_SHORT).show());
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (response.isSuccessful()) {
                                JsonArray array = JsonParser.parseString(response.body().string()).getAsJsonArray();
                                Log.d(TAG, "loadTopProducts response: " + array.toString());

                                // Đếm số lượng bán của từng sản phẩm
                                for (JsonElement element : array) {
                                    JsonObject item = element.getAsJsonObject();
                                    String prodName = item.getAsJsonObject("product_id").get("name").getAsString();
                                    int quantity = item.get("quantity").getAsInt();
                                    productSales.merge(prodName, quantity, Integer::sum);
                                }

                                // Sắp xếp dữ liệu: theo selectedSortOrder, nếu bằng thì theo tên sản phẩm
                                List<Map.Entry<String, Integer>> sortedProducts = new ArrayList<>(productSales.entrySet());
                                sortedProducts.sort((e1, e2) -> {
                                    int compareValue = selectedSortOrder.equals("Nhiều nhất") ?
                                            e2.getValue().compareTo(e1.getValue()) : // Nhiều nhất: giảm dần
                                            e1.getValue().compareTo(e2.getValue()); // Ít nhất: tăng dần
                                    if (compareValue == 0) {
                                        return e1.getKey().compareTo(e2.getKey());
                                    }
                                    return compareValue;
                                });

                                // Lấy đúng số lượng sản phẩm theo selectedLimit
                                sortedProducts = sortedProducts.stream().limit(selectedLimit).collect(Collectors.toList());

                                List<BarEntry> entries = new ArrayList<>();
                                List<String> labels = new ArrayList<>();
                                for (int i = 0; i < sortedProducts.size(); i++) {
                                    entries.add(new BarEntry(i, sortedProducts.get(i).getValue()));
                                    labels.add(sortedProducts.get(i).getKey());
                                }

                                BarDataSet dataSet = new BarDataSet(entries, "Số lượng bán");
                                dataSet.setColor(Color.parseColor("#4FC3F7"));
                                dataSet.setValueTextSize(14f);
                                dataSet.setValueTextColor(Color.BLACK);
                                BarData barData = new BarData(dataSet);
                                runOnUiThread(() -> {
                                    XAxis xAxis = binding.chartGeneric.getXAxis();
                                    xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
                                    xAxis.setGranularity(1f);
                                    xAxis.setLabelCount(labels.size());
                                    xAxis.setTextSize(14f);
                                    xAxis.setTextColor(Color.BLACK);
                                    xAxis.setLabelRotationAngle(45);
                                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                                    binding.chartGeneric.getAxisLeft().setTextSize(14f);
                                    binding.chartGeneric.getAxisLeft().setTextColor(Color.BLACK);
                                    binding.chartGeneric.getAxisRight().setDrawAxisLine(false);
                                    binding.chartGeneric.getAxisRight().setDrawLabels(false);
                                    binding.chartGeneric.setData(barData);
                                    binding.chartGeneric.setVisibility(View.VISIBLE);
                                    binding.chartGeneric.invalidate();
                                });
                            }
                        }
                    });
                }
            }
        });
    }

    private void loadUserStats(String dateFilter) {
        String url = USER_PROFILES_URL + "?select=created_at" + dateFilter;
        getOkHttpClient().newCall(getRequestBuilder().url(url).get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(StatisticsActivity.this, "Failed to load user stats", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    JsonArray array = JsonParser.parseString(response.body().string()).getAsJsonArray();
                    Log.d(TAG, "loadUserStats response: " + array.toString());
                    Map<String, Integer> dailyRegistrations = new TreeMap<>();
                    if (!dateFilter.isEmpty()) {
                        Calendar start = Calendar.getInstance();
                        start.setTime(new Date(startDate));
                        Calendar end = Calendar.getInstance();
                        end.setTime(new Date(endDate));
                        while (!start.after(end)) {
                            String dayKey = dayFormat.format(start.getTime());
                            dailyRegistrations.put(dayKey, 0);
                            start.add(Calendar.DAY_OF_MONTH, 1);
                        }
                    }
                    for (JsonElement element : array) {
                        String dateStr = element.getAsJsonObject().get("created_at").getAsString().split("T")[0];
                        try {
                            Date date = apiDateFormat.parse(dateStr);
                            String dayKey = dayFormat.format(date);
                            dailyRegistrations.merge(dayKey, 1, Integer::sum);
                        } catch (ParseException e) {
                            Log.e(TAG, "Error parsing date: " + dateStr, e);
                        }
                    }
                    List<BarEntry> entries = new ArrayList<>();
                    List<String> labels = new ArrayList<>();
                    int index = 0;
                    for (Map.Entry<String, Integer> entry : dailyRegistrations.entrySet()) {
                        entries.add(new BarEntry(index, entry.getValue()));
                        labels.add(entry.getKey());
                        index++;
                    }
                    BarDataSet dataSet = new BarDataSet(entries, "Số lượng đăng ký");
                    dataSet.setColor(Color.parseColor("#4FC3F7"));
                    dataSet.setValueTextSize(14f);
                    dataSet.setValueTextColor(Color.BLACK);
                    BarData barData = new BarData(dataSet);
                    runOnUiThread(() -> {
                        XAxis xAxis = binding.chartGeneric.getXAxis();
                        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
                        xAxis.setGranularity(1f);
                        xAxis.setLabelCount(labels.size());
                        xAxis.setTextSize(14f);
                        xAxis.setTextColor(Color.BLACK);
                        xAxis.setLabelRotationAngle(0);
                        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                        binding.chartGeneric.getAxisLeft().setTextSize(14f);
                        binding.chartGeneric.getAxisLeft().setTextColor(Color.BLACK);
                        binding.chartGeneric.getAxisRight().setDrawAxisLine(false);
                        binding.chartGeneric.getAxisRight().setDrawLabels(false);
                        binding.chartGeneric.setData(barData);
                        binding.chartGeneric.setVisibility(View.VISIBLE);
                        binding.chartGeneric.invalidate();
                    });
                }
            }
        });
    }

    private void loadCommentStats() {
        String urlProducts = PRODUCTS_URL + "?select=name";
        String urlReviews = PRODUCT_REVIEWS_URL + "?select=product_id(name)";

        // Lấy danh sách tất cả sản phẩm
        getOkHttpClient().newCall(getRequestBuilder().url(urlProducts).get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(StatisticsActivity.this, "Failed to load products for comment stats", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    JsonArray productsArray = JsonParser.parseString(response.body().string()).getAsJsonArray();
                    Map<String, Integer> commentCounts = new HashMap<>();

                    // Khởi tạo tất cả sản phẩm với số lượng bình luận là 0
                    for (JsonElement element : productsArray) {
                        String prodName = element.getAsJsonObject().get("name").getAsString();
                        commentCounts.put(prodName, 0);
                    }

                    // Lấy dữ liệu từ product_reviews
                    getOkHttpClient().newCall(getRequestBuilder().url(urlReviews).get().build()).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            runOnUiThread(() -> Toast.makeText(StatisticsActivity.this, "Failed to load comment stats", Toast.LENGTH_SHORT).show());
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (response.isSuccessful()) {
                                JsonArray array = JsonParser.parseString(response.body().string()).getAsJsonArray();
                                Log.d(TAG, "loadCommentStats response: " + array.toString());

                                // Đếm số lượng bình luận của từng sản phẩm
                                for (JsonElement element : array) {
                                    String prodName = element.getAsJsonObject().getAsJsonObject("product_id").get("name").getAsString();
                                    commentCounts.merge(prodName, 1, Integer::sum);
                                }

                                // Sắp xếp dữ liệu: theo selectedSortOrder, nếu bằng thì theo tên sản phẩm
                                List<Map.Entry<String, Integer>> sortedComments = new ArrayList<>(commentCounts.entrySet());
                                sortedComments.sort((e1, e2) -> {
                                    int compareValue = selectedSortOrder.equals("Nhiều nhất") ?
                                            e2.getValue().compareTo(e1.getValue()) : // Nhiều nhất: giảm dần
                                            e1.getValue().compareTo(e2.getValue()); // Ít nhất: tăng dần
                                    if (compareValue == 0) {
                                        return e1.getKey().compareTo(e2.getKey());
                                    }
                                    return compareValue;
                                });

                                // Lấy đúng số lượng sản phẩm theo selectedLimit
                                sortedComments = sortedComments.stream().limit(selectedLimit).collect(Collectors.toList());

                                List<BarEntry> entries = new ArrayList<>();
                                List<String> labels = new ArrayList<>();
                                for (int i = 0; i < sortedComments.size(); i++) {
                                    entries.add(new BarEntry(i, sortedComments.get(i).getValue()));
                                    labels.add(sortedComments.get(i).getKey());
                                }

                                BarDataSet dataSet = new BarDataSet(entries, "Số lượng bình luận");
                                dataSet.setColor(Color.parseColor("#4FC3F7"));
                                dataSet.setValueTextSize(14f);
                                dataSet.setValueTextColor(Color.BLACK);
                                BarData barData = new BarData(dataSet);
                                runOnUiThread(() -> {
                                    XAxis xAxis = binding.chartGeneric.getXAxis();
                                    xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
                                    xAxis.setGranularity(1f);
                                    xAxis.setLabelCount(labels.size());
                                    xAxis.setTextSize(14f);
                                    xAxis.setTextColor(Color.BLACK);
                                    xAxis.setLabelRotationAngle(45);
                                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                                    binding.chartGeneric.getAxisLeft().setTextSize(14f);
                                    binding.chartGeneric.getAxisLeft().setTextColor(Color.BLACK);
                                    binding.chartGeneric.getAxisRight().setDrawAxisLine(false);
                                    binding.chartGeneric.getAxisRight().setDrawLabels(false);
                                    binding.chartGeneric.setData(barData);
                                    binding.chartGeneric.setVisibility(View.VISIBLE);
                                    binding.chartGeneric.invalidate();
                                });
                            }
                        }
                    });
                }
            }
        });
    }

    private void loadWishlistStats() {
        String urlProducts = PRODUCTS_URL + "?select=name";
        String urlWishlist = WISHLIST_URL + "?select=product_id(name)";

        // Lấy danh sách tất cả sản phẩm
        getOkHttpClient().newCall(getRequestBuilder().url(urlProducts).get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(StatisticsActivity.this, "Failed to load products for wishlist stats", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    JsonArray productsArray = JsonParser.parseString(response.body().string()).getAsJsonArray();
                    Map<String, Integer> wishlistCounts = new HashMap<>();

                    // Khởi tạo tất cả sản phẩm với số lượng yêu thích là 0
                    for (JsonElement element : productsArray) {
                        String prodName = element.getAsJsonObject().get("name").getAsString();
                        wishlistCounts.put(prodName, 0);
                    }

                    // Lấy dữ liệu từ wishlist
                    getOkHttpClient().newCall(getRequestBuilder().url(urlWishlist).get().build()).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            runOnUiThread(() -> Toast.makeText(StatisticsActivity.this, "Failed to load wishlist stats", Toast.LENGTH_SHORT).show());
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (response.isSuccessful()) {
                                JsonArray array = JsonParser.parseString(response.body().string()).getAsJsonArray();
                                Log.d(TAG, "loadWishlistStats response: " + array.toString());

                                // Đếm số lượng yêu thích của từng sản phẩm
                                for (JsonElement element : array) {
                                    String prodName = element.getAsJsonObject().getAsJsonObject("product_id").get("name").getAsString();
                                    wishlistCounts.merge(prodName, 1, Integer::sum);
                                }

                                // Sắp xếp dữ liệu: theo selectedSortOrder, nếu bằng thì theo tên sản phẩm
                                List<Map.Entry<String, Integer>> sortedWishlist = new ArrayList<>(wishlistCounts.entrySet());
                                sortedWishlist.sort((e1, e2) -> {
                                    int compareValue = selectedSortOrder.equals("Nhiều nhất") ?
                                            e2.getValue().compareTo(e1.getValue()) : // Nhiều nhất: giảm dần
                                            e1.getValue().compareTo(e2.getValue()); // Ít nhất: tăng dần
                                    if (compareValue == 0) {
                                        return e1.getKey().compareTo(e2.getKey());
                                    }
                                    return compareValue;
                                });

                                // Lấy đúng số lượng sản phẩm theo selectedLimit
                                sortedWishlist = sortedWishlist.stream().limit(selectedLimit).collect(Collectors.toList());

                                List<BarEntry> entries = new ArrayList<>();
                                List<String> labels = new ArrayList<>();
                                for (int i = 0; i < sortedWishlist.size(); i++) {
                                    entries.add(new BarEntry(i, sortedWishlist.get(i).getValue()));
                                    labels.add(sortedWishlist.get(i).getKey());
                                }

                                BarDataSet dataSet = new BarDataSet(entries, "Số lượng yêu thích");
                                dataSet.setColor(Color.parseColor("#4FC3F7"));
                                dataSet.setValueTextSize(14f);
                                dataSet.setValueTextColor(Color.BLACK);
                                BarData barData = new BarData(dataSet);
                                runOnUiThread(() -> {
                                    XAxis xAxis = binding.chartGeneric.getXAxis();
                                    xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
                                    xAxis.setGranularity(1f);
                                    xAxis.setLabelCount(labels.size());
                                    xAxis.setTextSize(14f);
                                    xAxis.setTextColor(Color.BLACK);
                                    xAxis.setLabelRotationAngle(45);
                                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                                    binding.chartGeneric.getAxisLeft().setTextSize(14f);
                                    binding.chartGeneric.getAxisLeft().setTextColor(Color.BLACK);
                                    binding.chartGeneric.getAxisRight().setDrawAxisLine(false);
                                    binding.chartGeneric.getAxisRight().setDrawLabels(false);
                                    binding.chartGeneric.setData(barData);
                                    binding.chartGeneric.setVisibility(View.VISIBLE);
                                    binding.chartGeneric.invalidate();
                                });
                            }
                        }
                    });
                }
            }
        });
    }

    private void loadRevenueByProduct() {
        String urlProducts = PRODUCTS_URL + "?select=name";
        String urlOrderItems = ORDER_ITEMS_URL + "?select=product_id(name),quantity,price";

        // Lấy danh sách tất cả sản phẩm
        getOkHttpClient().newCall(getRequestBuilder().url(urlProducts).get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(StatisticsActivity.this, "Failed to load products for revenue by product", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    JsonArray productsArray = JsonParser.parseString(response.body().string()).getAsJsonArray();
                    Map<String, Double> productRevenue = new HashMap<>();

                    // Khởi tạo tất cả sản phẩm với doanh thu là 0
                    for (JsonElement element : productsArray) {
                        String prodName = element.getAsJsonObject().get("name").getAsString();
                        productRevenue.put(prodName, 0.0);
                    }

                    // Lấy dữ liệu từ order_items
                    getOkHttpClient().newCall(getRequestBuilder().url(urlOrderItems).get().build()).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            runOnUiThread(() -> Toast.makeText(StatisticsActivity.this, "Failed to load revenue by product", Toast.LENGTH_SHORT).show());
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (response.isSuccessful()) {
                                JsonArray array = JsonParser.parseString(response.body().string()).getAsJsonArray();
                                Log.d(TAG, "loadRevenueByProduct response: " + array.toString());

                                // Tính tổng doanh thu của từng sản phẩm
                                for (JsonElement element : array) {
                                    JsonObject item = element.getAsJsonObject();
                                    String prodName = item.getAsJsonObject("product_id").get("name").getAsString();
                                    int quantity = item.get("quantity").getAsInt();
                                    double price = item.get("price").getAsDouble();
                                    double revenue = quantity * price;
                                    productRevenue.merge(prodName, revenue, Double::sum);
                                }

                                // Sắp xếp dữ liệu: theo selectedSortOrder, nếu bằng thì theo tên sản phẩm
                                List<Map.Entry<String, Double>> sortedRevenue = new ArrayList<>(productRevenue.entrySet());
                                sortedRevenue.sort((e1, e2) -> {
                                    int compareValue = selectedSortOrder.equals("Nhiều nhất") ?
                                            e2.getValue().compareTo(e1.getValue()) : // Nhiều nhất: giảm dần
                                            e1.getValue().compareTo(e2.getValue()); // Ít nhất: tăng dần
                                    if (compareValue == 0) {
                                        return e1.getKey().compareTo(e2.getKey());
                                    }
                                    return compareValue;
                                });

                                // Lấy đúng số lượng sản phẩm theo selectedLimit
                                sortedRevenue = sortedRevenue.stream().limit(selectedLimit).collect(Collectors.toList());

                                List<BarEntry> entries = new ArrayList<>();
                                List<String> labels = new ArrayList<>();
                                for (int i = 0; i < sortedRevenue.size(); i++) {
                                    entries.add(new BarEntry(i, sortedRevenue.get(i).getValue().floatValue()));
                                    labels.add(sortedRevenue.get(i).getKey());
                                }

                                BarDataSet dataSet = new BarDataSet(entries, "Doanh thu (₫)");
                                dataSet.setColor(Color.parseColor("#4FC3F7"));
                                dataSet.setValueTextSize(14f);
                                dataSet.setValueTextColor(Color.BLACK);
                                BarData barData = new BarData(dataSet);
                                runOnUiThread(() -> {
                                    XAxis xAxis = binding.chartGeneric.getXAxis();
                                    xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
                                    xAxis.setGranularity(1f);
                                    xAxis.setLabelCount(labels.size());
                                    xAxis.setTextSize(14f);
                                    xAxis.setTextColor(Color.BLACK);
                                    xAxis.setLabelRotationAngle(45);
                                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                                    binding.chartGeneric.getAxisLeft().setTextSize(14f);
                                    binding.chartGeneric.getAxisLeft().setTextColor(Color.BLACK);
                                    binding.chartGeneric.getAxisRight().setDrawAxisLine(false);
                                    binding.chartGeneric.getAxisRight().setDrawLabels(false);
                                    binding.chartGeneric.setData(barData);
                                    binding.chartGeneric.setVisibility(View.VISIBLE);
                                    binding.chartGeneric.invalidate();
                                });
                            }
                        }
                    });
                }
            }
        });
    }

    private static class StatsAdapter extends RecyclerView.Adapter<StatsAdapter.ViewHolder> {
        private final List<String[]> data;

        StatsAdapter(List<String[]> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String[] row = data.get(position);
            holder.text1.setText(row[0]);
            holder.text2.setText(row[1]);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;

            ViewHolder(View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}