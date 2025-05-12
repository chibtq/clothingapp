package com.example.foodappfinal.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.foodappfinal.Adapter.OrderHistoryAdapter;
import com.example.foodappfinal.Model.Order;
import com.example.foodappproject.R;
import com.example.foodappproject.databinding.ActivityOrderHistoryBinding;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class OrderHistoryActivity extends BaseActivity implements OrderHistoryAdapter.OnOrderClickListener {
    private ActivityOrderHistoryBinding binding;
    private List<Order> orderList;
    private List<Order> filteredOrderList;
    private OrderHistoryAdapter orderAdapter;
    private static final String ORDERS_URL = "https://fmheidqpephjpsgjuupf.supabase.co/rest/v1/orders";
    private static final int PAGE_SIZE = 5; // Số lượng đơn hàng mỗi trang
    private int currentPage = 0;
    private int totalPages = 0;
    private int totalOrders = 0;
    private String currentSearchQuery = "";
    private int currentSortCriteria = 0; // 0: Thời gian mới nhất (mặc định)
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private static final int SEARCH_DELAY_MS = 300; // Độ trễ 300ms để debounce

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Thiết lập Toolbar
        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Lịch sử đơn hàng");
        }

        orderList = new ArrayList<>();
        filteredOrderList = new ArrayList<>();
        orderAdapter = new OrderHistoryAdapter(this, new ArrayList<>(), this); // Khởi tạo với danh sách rỗng
        binding.orderRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.orderRecyclerView.setAdapter(orderAdapter);

        // Cài đặt Spinner cho sắp xếp
        ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(this,
                R.array.sort_criteria, android.R.layout.simple_spinner_item);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.sortCriteriaSpinner.setAdapter(sortAdapter);
        binding.sortCriteriaSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSortCriteria = position;
                applySearchAndSort();
                loadFilteredOrdersForPage(0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Tự động tìm kiếm khi nhập với debounce
        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchHandler.removeCallbacksAndMessages(null); // Hủy các tác vụ cũ
            }
            @Override
            public void afterTextChanged(Editable s) {
                searchHandler.postDelayed(() -> {
                    currentSearchQuery = s.toString().trim();
                    applySearchAndSort();
                    loadFilteredOrdersForPage(0);
                }, SEARCH_DELAY_MS);
            }
        });

        // Xử lý nút Previous và Next
        binding.previousButton.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                loadFilteredOrdersForPage(currentPage);
            }
        });

        binding.nextButton.setOnClickListener(v -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                loadFilteredOrdersForPage(currentPage);
            }
        });

        loadOrderHistory();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadOrderHistory() {
        String userId = getCurrentUserId();
        if (userId == null) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Please log in to view order history", Toast.LENGTH_SHORT).show();
                logout();
            });
            return;
        }

        String url = ORDERS_URL + "?user_id=eq." + userId + "&select=*";
        Request request = getRequestBuilder()
                .url(url)
                .get()
                .build();

        getOkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(OrderHistoryActivity.this, "Failed to load order history", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    Gson gson = new Gson();
                    Type orderType = new TypeToken<List<Order>>(){}.getType();
                    List<Order> orders = gson.fromJson(responseData, orderType);

                    runOnUiThread(() -> {
                        orderList.clear();
                        filteredOrderList.clear();
                        if (orders != null && !orders.isEmpty()) {
                            orderList.addAll(orders);
                            filteredOrderList.addAll(orders);
                            totalOrders = orderList.size();
                            totalPages = (int) Math.ceil((double) totalOrders / PAGE_SIZE);
                            binding.paginationContainer.setVisibility(View.VISIBLE);
                        } else {
                            totalOrders = 0;
                            totalPages = 0;
                            binding.emptyTxt.setVisibility(View.VISIBLE);
                            binding.paginationContainer.setVisibility(View.GONE);
                        }
                        applySearchAndSort();
                        loadFilteredOrdersForPage(0);
                    });
                }
            }
        });
    }

    private void applySearchAndSort() {
        filteredOrderList.clear();

        // Lọc danh sách theo từ khóa tìm kiếm (tìm trên tất cả các trường)
        if (!currentSearchQuery.isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            for (Order order : orderList) {
                boolean matches = false;

                // Kiểm tra theo thời gian
                try {
                    String orderDate = sdf.format(order.getCreatedAtAsDate());
                    if (orderDate.contains(currentSearchQuery)) {
                        matches = true;
                    }
                } catch (Exception e) {
                    // Bỏ qua nếu không parse được ngày
                }

                // Kiểm tra theo giá tiền
                if (!matches) {
                    try {
                        if (currentSearchQuery.matches("\\d+(\\.\\d+)?")) { // Chỉ parse nếu là số hợp lệ
                            double searchPrice = Double.parseDouble(currentSearchQuery);
                            if (Math.abs(order.getTotalAmount() - searchPrice) < 0.01) {
                                matches = true;
                            }
                        }
                    } catch (NumberFormatException e) {
                        // Bỏ qua nếu không parse được số
                    }
                }

                // Kiểm tra theo Order ID
                if (!matches) {
                    if (order.getOrderId().toLowerCase().contains(currentSearchQuery.toLowerCase())) {
                        matches = true;
                    }
                }

                if (matches) {
                    filteredOrderList.add(order);
                }
            }
        } else {
            filteredOrderList.addAll(orderList);
        }

        // Sắp xếp danh sách theo tiêu chí
        switch (currentSortCriteria) {
            case 0: // Thời gian mới nhất
                Collections.sort(filteredOrderList, (o1, o2) -> o2.getCreatedAtAsDate().compareTo(o1.getCreatedAtAsDate()));
                break;
            case 1: // Thời gian cũ nhất
                Collections.sort(filteredOrderList, (o1, o2) -> o1.getCreatedAtAsDate().compareTo(o2.getCreatedAtAsDate()));
                break;
            case 2: // Giá tiền cao đến thấp
                Collections.sort(filteredOrderList, (o1, o2) -> Double.compare(o2.getTotalAmount(), o1.getTotalAmount()));
                break;
            case 3: // Giá tiền thấp đến cao
                Collections.sort(filteredOrderList, (o1, o2) -> Double.compare(o1.getTotalAmount(), o2.getTotalAmount()));
                break;
            case 4: // Order ID A-Z
                Collections.sort(filteredOrderList, (o1, o2) -> o1.getOrderId().compareTo(o2.getOrderId()));
                break;
            case 5: // Order ID Z-A
                Collections.sort(filteredOrderList, (o1, o2) -> o2.getOrderId().compareTo(o1.getOrderId()));
                break;
        }

        totalOrders = filteredOrderList.size();
        totalPages = (int) Math.ceil((double) totalOrders / PAGE_SIZE);
        binding.paginationContainer.setVisibility(totalOrders > 0 ? View.VISIBLE : View.GONE);
        binding.emptyTxt.setVisibility(totalOrders == 0 ? View.VISIBLE : View.GONE);
    }

    private void loadFilteredOrdersForPage(int page) {
        currentPage = page;
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, filteredOrderList.size());

        // Tạo danh sách tạm để chứa dữ liệu của trang hiện tại
        List<Order> tempList = new ArrayList<>();
        if (start < filteredOrderList.size()) {
            tempList.addAll(filteredOrderList.subList(start, end));
        }

        // Cập nhật dữ liệu cho adapter mà không sửa đổi filteredOrderList
        runOnUiThread(() -> {
            orderAdapter.updateOrders(tempList);
            binding.emptyTxt.setVisibility(tempList.isEmpty() ? View.VISIBLE : View.GONE);
            updatePaginationControls();
        });
    }

    private void updatePaginationControls() {
        binding.pageIndicator.setText("Trang " + (currentPage + 1) + "/" + totalPages);
        binding.previousButton.setEnabled(currentPage > 0);
        binding.nextButton.setEnabled(currentPage < totalPages - 1);
    }

    @Override
    public void onOrderClicked(String orderId) {
        Intent intent = new Intent(OrderHistoryActivity.this, OrderDetailActivity.class);
        intent.putExtra("order_id", orderId);
        startActivity(intent);
    }
}