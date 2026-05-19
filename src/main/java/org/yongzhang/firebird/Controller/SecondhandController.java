package org.yongzhang.firebird.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.yongzhang.firebird.Data.*;
import org.yongzhang.firebird.Mapper.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/secondhand")
public class SecondhandController {

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String NGINX_IMAGE_PATH = "E:/yongzhang/Nginx_server/images/";

    @GetMapping("/items")
    public ItemsResponse getItems(@RequestParam(required = false) Integer page,
                                  @RequestParam(required = false) Integer size,
                                  @RequestParam(required = false) String q,
                                  @RequestParam(required = false) String category) {
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 20 : size;
        int offset = (p - 1) * s;
        List<Item> items = itemMapper.search(q, category, offset, s);
        int total = itemMapper.count(q, category);
        return new ItemsResponse(items, total);
    }

    @GetMapping("/items/pending")
    public List<Item> getPendingItems(@RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!"admin".equals(role)) {
            return new ArrayList<>();
        }
        List<Item> items = itemMapper.getByStatus("pending_review");
        return items != null ? items : new ArrayList<>();
    }

    @PostMapping("/items/{id}/review")
    public Map<String, Object> reviewItem(@PathVariable String id,
                                          @RequestBody Map<String, String> body,
                                          @RequestHeader(value = "X-User-Role", required = false) String role) {
        Map<String, Object> res = new HashMap<>();
        if (!"admin".equals(role)) {
            res.put("success", false);
            res.put("message", "无权限");
            return res;
        }

        String action = body.get("action");
        if (action == null || (!action.equals("approve") && !action.equals("reject"))) {
            res.put("success", false);
            res.put("message", "无效的操作");
            return res;
        }

        Item item = itemMapper.getById(id);
        if (item == null) {
            res.put("success", false);
            res.put("message", "商品不存在");
            return res;
        }

        String newStatus = "approve".equals(action) ? "available" : "rejected";
        itemMapper.updateStatus(id, newStatus);

        res.put("success", true);
        res.put("message", "商品审核" + ("available".equals(newStatus) ? "通过" : "拒绝"));
        return res;
    }

    @GetMapping("/items/my")
    public List<Item> getMyItems(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) return new ArrayList<>();
        List<Item> items = itemMapper.getBySellerId(userId);
        for (Item item : items) {
            List<Order> orders = orderMapper.getOrdersByItemId(item.getId(), "paid");
            item.setOrders(orders);
        }
        return items;
    }

    @GetMapping("/items/{id}")
    public Item getItem(@PathVariable String id) {
        return itemMapper.getById(id);
    }

    @PutMapping("/items/{id}/offline")
    public Map<String, Object> offlineItem(@PathVariable String id,
                                          @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                          @RequestHeader(value = "X-User-Role", required = false) String role) {
        Map<String, Object> res = new HashMap<>();
        if (userId == null) {
            res.put("success", false);
            res.put("message", "未登录");
            return res;
        }
        Item item = itemMapper.getById(id);
        if (item == null) {
            res.put("success", false);
            res.put("message", "商品不存在");
            return res;
        }
        if (!role.equals("admin") && !item.getSellerId().equals(userId)) {
            res.put("success", false);
            res.put("message", "无权限下架此商品");
            return res;
        }
        int updated = itemMapper.offline(id, userId);
        res.put("success", updated > 0);
        res.put("message", updated > 0 ? "下架成功" : "下架失败");
        return res;
    }

    @PostMapping(value = "/items", consumes = {"multipart/form-data"})
    public Map<String, Object> createItem(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                   @RequestHeader(value = "X-User-Username", required = false) String username,
                                   @RequestHeader(value = "X-User-Role", required = false) String role,
                                   @RequestParam String title,
                                   @RequestParam String description,
                                   @RequestParam double price,
                                   @RequestParam(required = false, defaultValue = "灵感") String category,
                                   @RequestParam(required = false) MultipartFile[] images) throws IOException {
        Map<String, Object> res = new HashMap<>();

        if (userId == null) {
            res.put("success", false);
            res.put("message", "未登录");
            return res;
        }

        if ("admin".equals(role)) {
            res.put("success", false);
            res.put("message", "管理员不能发布商品");
            return res;
        }

        User seller = userMapper.getById(userId);
        if (seller == null) {
            res.put("success", false);
            res.put("message", "用户不存在");
            return res;
        }

        if (!"approved".equals(seller.getStatus())) {
            res.put("success", false);
            res.put("message", "您的商家账号尚未通过审核，无法发布商品");
            return res;
        }

        String id = UUID.randomUUID().toString();
        String date = LocalDateTime.now().format(DTF);
        String thumb = null;
        List<String> saved = new ArrayList<>();
        if (images != null && images.length > 0) {
            File nginxDir = new File(NGINX_IMAGE_PATH);
            if (!nginxDir.exists()) nginxDir.mkdirs();
            for (MultipartFile mf : images) {
                if (mf.isEmpty()) continue;
                String fname = UUID.randomUUID().toString() + "_" + mf.getOriginalFilename();
                File out = new File(nginxDir, fname);
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    fos.write(mf.getBytes());
                }
                String url = "http://localhost:80/images/" + fname;
                saved.add(url);
            }
            if (!saved.isEmpty()) thumb = saved.get(0);
        }

        Item item = new Item();
        item.setId(id);
        item.setTitle(title);
        item.setDescription(description);
        item.setPrice(price);
        item.setCategory(category);
        item.setDate(date);
        item.setSellerId(userId);
        item.setSellerName(username);
        item.setThumb(thumb);
        item.setImages(String.join(",", saved));
        item.setStatus("available");

        itemMapper.insert(item);
        res.put("success", true);
        res.put("message", "商品发布成功");
        res.put("data", item);
        return res;
    }

    @PutMapping(value = "/items/{id}", consumes = {"multipart/form-data"})
    public Item updateItem(@PathVariable String id,
                           @RequestHeader(value = "X-User-Id", required = false) Long userId,
                           @RequestParam String title,
                           @RequestParam String description,
                           @RequestParam double price,
                           @RequestParam String category,
                           @RequestParam(required = false) MultipartFile[] images) throws IOException {
        Item existing = itemMapper.getById(id);
        if (existing == null) return null;
        if (existing.getSellerId() != null && userId != null && !existing.getSellerId().equals(userId)) return null;

        List<String> saved = new ArrayList<>();
        String thumb = existing.getThumb();
        if (images != null && images.length > 0) {
            File nginxDir = new File(NGINX_IMAGE_PATH);
            if (!nginxDir.exists()) nginxDir.mkdirs();
            for (MultipartFile mf : images) {
                if (mf.isEmpty()) continue;
                String fname = UUID.randomUUID().toString() + "_" + mf.getOriginalFilename();
                File out = new File(nginxDir, fname);
                try (FileOutputStream fos = new FileOutputStream(out)) { fos.write(mf.getBytes()); }
                String url = "http://localhost:80/images/" + fname;
                saved.add(url);
            }
            if (!saved.isEmpty()) thumb = saved.get(0);
        }

        existing.setTitle(title);
        existing.setDescription(description);
        existing.setPrice(price);
        existing.setCategory(category);
        existing.setThumb(thumb);
        if (!saved.isEmpty()) existing.setImages(String.join(",", saved));
        existing.setStatus("pending_review");

        itemMapper.update(existing);
        return existing;
    }

    @DeleteMapping("/items/{id}")
    public void deleteItem(@PathVariable String id, @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        itemMapper.delete(id, userId);
    }

    @GetMapping("/categories")
    public Map<String, List<String>> getCategories() {
        Map<String, List<String>> result = new HashMap<>();
        result.put("categories", Arrays.asList("灵感", "思考", "模版", "资源"));
        return result;
    }

    @GetMapping("/cart")
    public CartResponse getCart(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        List<CartItem> items = cartMapper.getByUser(userId);
        return new CartResponse(items);
    }

    @PostMapping("/cart")
    public Map<String, Object> addToCart(@RequestBody Map<String, Object> body,
                                         @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        Map<String, Object> res = new HashMap<>();
        if (userId == null) {
            res.put("success", false);
            res.put("message", "未登录");
            return res;
        }

        String itemId = (String) body.get("itemId");
        String title = (String) body.get("title");
        double price = body.get("price") != null ? Double.parseDouble(body.get("price").toString()) : 0;
        String thumb = (String) body.get("thumb");
        int quantity = body.get("quantity") != null ? Integer.parseInt(body.get("quantity").toString()) : 1;

        CartItem cartItem = new CartItem();
        cartItem.setId(UUID.randomUUID().toString());
        cartItem.setItemId(itemId);
        cartItem.setTitle(title);
        cartItem.setPrice(price);
        cartItem.setThumb(thumb);
        cartItem.setQuantity(quantity);
        cartItem.setUserId(userId);

        cartMapper.insert(cartItem);
        res.put("success", true);
        res.put("message", "加入购物车成功");
        return res;
    }

    @PutMapping("/cart/{id}")
    public Map<String, Object> updateCartItem(@PathVariable String id,
                                              @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                              @RequestBody Map<String, Object> body) {
        Map<String, Object> res = new HashMap<>();
        int quantity = body.get("quantity") != null ? Integer.parseInt(body.get("quantity").toString()) : 1;
        cartMapper.updateQuantity(id, userId, quantity);
        res.put("success", true);
        return res;
    }

    @DeleteMapping("/cart/{id}")
    public Map<String, Object> removeFromCart(@PathVariable String id,
                                              @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        Map<String, Object> res = new HashMap<>();
        cartMapper.delete(id, userId);
        res.put("success", true);
        res.put("message", "移除成功");
        return res;
    }

    @DeleteMapping("/cart")
    public Map<String, Object> clearCart(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        Map<String, Object> res = new HashMap<>();
        if (userId == null) {
            res.put("success", false);
            res.put("message", "未登录");
            return res;
        }
        cartMapper.clear(userId);
        res.put("success", true);
        res.put("message", "清空购物车成功");
        return res;
    }

    @PostMapping("/orders")
    public Map<String, Object> createOrder(@RequestBody CreateOrderRequest request,
                                           @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        Map<String, Object> res = new HashMap<>();
        if (userId == null) {
            res.put("success", false);
            res.put("message", "未登录");
            return res;
        }

        List<CartItem> cartItems = cartMapper.getByUser(userId);
        if (cartItems == null || cartItems.isEmpty()) {
            res.put("success", false);
            res.put("message", "购物车为空");
            return res;
        }

        User buyer = userMapper.getById(userId);
        if (buyer.getBalance() == null || buyer.getBalance() < request.getTotalAmount()) {
            res.put("success", false);
            res.put("message", "余额不足");
            return res;
        }

        String orderId = UUID.randomUUID().toString();
        String date = LocalDateTime.now().format(DTF);

        Order order = new Order();
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setTotalAmount(request.getTotalAmount());
        order.setStatus("pending");
        order.setDate(date);
        // shippingAddress available in CreateOrderRequest? if provided, use it
        try {
            order.setShippingAddress(request.getShippingAddress());
        } catch (Throwable ignored) {
            // ignore if not provided
        }

        orderMapper.insert(order);

        for (CartItem item : cartItems) {
            item.setOrderId(orderId);
            orderMapper.insertOrderItem(item);
        }

        cartMapper.clear(userId);

        res.put("success", true);
        res.put("orderId", orderId);
        res.put("message", "订单创建成功");
        return res;
    }

    @GetMapping("/orders")
    public OrdersResponse getOrders(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) return new OrdersResponse(new ArrayList<>());
        List<Order> orders = orderMapper.getByUser(userId);
        return new OrdersResponse(orders);
    }

    @GetMapping("/orders/seller/{sellerId}")
    public List<Order> getOrdersBySeller(@PathVariable Long sellerId) {
        return orderMapper.getOrdersBySellerId(sellerId);
    }

    @GetMapping("/orders/{orderId}")
    public Order getOrder(@PathVariable String orderId) {
        return orderMapper.getByOrderId(orderId);
    }

    @PostMapping("/orders/{orderId}/pay")
    public Map<String, Object> payOrder(@PathVariable String orderId,
                                        @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        Map<String, Object> res = new HashMap<>();
        if (userId == null) {
            res.put("success", false);
            res.put("message", "未登录");
            return res;
        }

        Order order = orderMapper.getByOrderId(orderId);
        if (order == null) {
            res.put("success", false);
            res.put("message", "订单不存在");
            return res;
        }

        User buyer = userMapper.getById(userId);
        if (buyer.getBalance() == null || buyer.getBalance() < order.getTotalAmount()) {
            res.put("success", false);
            res.put("message", "余额不足");
            return res;
        }

        double newBalance = buyer.getBalance() - order.getTotalAmount();
        userMapper.updateBalance(userId, newBalance);

        String payTime = LocalDateTime.now().format(DTF);
        orderMapper.updateStatus(orderId, userId, "paid");

        res.put("success", true);
        res.put("message", "支付成功");
        res.put("balance", newBalance);
        return res;
    }

    @PostMapping("/orders/{orderId}/ship")
    public Map<String, Object> shipOrder(@PathVariable String orderId,
                                          @RequestBody Map<String, String> body) {
        Map<String, Object> res = new HashMap<>();
        String trackingNumber = body.get("trackingNumber");
        if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
            res.put("success", false);
            res.put("message", "物流单号不能为空");
            return res;
        }

        Order order = orderMapper.getByOrderId(orderId);
        if (order == null) {
            res.put("success", false);
            res.put("message", "订单不存在");
            return res;
        }

        orderMapper.updateOrderStatus(orderId, "shipped", trackingNumber);

        res.put("success", true);
        res.put("message", "发货成功");
        return res;
    }

    @PostMapping("/orders/{orderId}/confirm")
    public Map<String, Object> confirmOrder(@PathVariable String orderId,
                                            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        Map<String, Object> res = new HashMap<>();
        if (userId == null) {
            res.put("success", false);
            res.put("message", "未登录");
            return res;
        }

        Order order = orderMapper.getByOrderId(orderId);
        if (order == null) {
            res.put("success", false);
            res.put("message", "订单不存在");
            return res;
        }

        String receiveTime = LocalDateTime.now().format(DTF);
        orderMapper.updateReceiveTime(orderId, receiveTime);

        res.put("success", true);
        res.put("message", "确认收货成功");
        return res;
    }

    @PostMapping("/orders/{orderId}/refund")
    public Map<String, Object> applyRefund(@PathVariable String orderId,
                                           @RequestBody Map<String, String> body,
                                           @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        Map<String, Object> res = new HashMap<>();
        System.out.println("DEBUG: applyRefund called for orderId: " + orderId + ", userId: " + userId);
        
        if (userId == null) {
            res.put("success", false);
            res.put("message", "未登录");
            return res;
        }

        String reason = body.get("reason");
        System.out.println("DEBUG: Refund reason: " + reason);
        
        Order order = orderMapper.getByOrderId(orderId);
        if (order == null) {
            res.put("success", false);
            res.put("message", "订单不存在");
            return res;
        }
        
        System.out.println("DEBUG: Order found, userId: " + order.getUserId() + ", totalAmount: " + order.getTotalAmount());

        String refundTime = LocalDateTime.now().format(DTF);
        int rowsUpdated = orderMapper.updateRefundStatus(orderId, "pending", reason, refundTime);
        System.out.println("DEBUG: updateRefundStatus affected rows: " + rowsUpdated);

        res.put("success", true);
        res.put("message", "退款申请已提交");
        return res;
    }

    @PostMapping("/orders/{orderId}/refund/review")
    public Map<String, Object> reviewRefund(@PathVariable String orderId,
                                             @RequestBody Map<String, String> body) {
        Map<String, Object> res = new HashMap<>();
        String action = body.get("action");
        if (action == null || (!action.equals("approve") && !action.equals("reject"))) {
            res.put("success", false);
            res.put("message", "无效的操作");
            return res;
        }

        Order order = orderMapper.getByOrderId(orderId);
        if (order == null) {
            res.put("success", false);
            res.put("message", "订单不存在");
            return res;
        }

        String refundTime = LocalDateTime.now().format(DTF);

        if ("approve".equals(action)) {
            User buyer = userMapper.getById(order.getUserId());
            double newBalance = (buyer.getBalance() != null ? buyer.getBalance() : 0.0) + order.getTotalAmount();
            userMapper.updateBalance(order.getUserId(), newBalance);

            orderMapper.processRefund(orderId, "cancelled", "refunded", refundTime);

            res.put("success", true);
            res.put("message", "退款已处理，款项已退回买家账户");
        } else {
            orderMapper.updateRefundStatus(orderId, "rejected", order.getReturnReason(), refundTime);

            res.put("success", true);
            res.put("message", "退款申请已拒绝");
        }

        return res;
    }

    @GetMapping("/orders/refunds/pending")
    public List<Order> getPendingRefunds(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) return new ArrayList<>();
        return orderMapper.getPendingRefundsBySellerId(userId);
    }

    @GetMapping("/orders/returns")
    public List<Order> getAllReturns() {
        return orderMapper.getAllRefunds();
    }

    @GetMapping("/orders/returns/seller/{sellerId}")
    public List<Order> getReturnsBySeller(@PathVariable Long sellerId) {
        System.out.println("DEBUG: getReturnsBySeller called with sellerId: " + sellerId);
        List<Order> orders = orderMapper.getPendingRefundsBySellerId(sellerId);
        System.out.println("DEBUG: Returning " + orders.size() + " pending refund orders for seller " + sellerId);
        return orders;
    }

    @PostMapping("/orders/{orderId}/return/approve")
    public Map<String, Object> approveReturn(@PathVariable String orderId,
                                              @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        Map<String, Object> res = new HashMap<>();
        if (userId == null) {
            res.put("success", false);
            res.put("message", "未登录");
            return res;
        }

        Order order = orderMapper.getByOrderId(orderId);
        if (order == null) {
            res.put("success", false);
            res.put("message", "订单不存在");
            return res;
        }

        Long sellerId = orderMapper.getSellerIdByOrderId(orderId);
        if (sellerId == null || !sellerId.equals(userId)) {
            res.put("success", false);
            res.put("message", "无权限审核此退货");
            return res;
        }

        User buyer = userMapper.getById(order.getUserId());
        double newBalance = (buyer.getBalance() != null ? buyer.getBalance() : 0.0) + order.getTotalAmount();
        userMapper.updateBalance(order.getUserId(), newBalance);

        String refundTime = LocalDateTime.now().format(DTF);
        orderMapper.processRefund(orderId, "cancelled", "refunded", refundTime);

        res.put("success", true);
        res.put("message", "退货已通过，款项已退回买家账户");
        return res;
    }

    @PostMapping("/orders/{orderId}/return/reject")
    public Map<String, Object> rejectReturn(@PathVariable String orderId,
                                             @RequestBody Map<String, String> body,
                                             @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        Map<String, Object> res = new HashMap<>();
        if (userId == null) {
            res.put("success", false);
            res.put("message", "未登录");
            return res;
        }

        String reason = body.get("reason");

        Order order = orderMapper.getByOrderId(orderId);
        if (order == null) {
            res.put("success", false);
            res.put("message", "订单不存在");
            return res;
        }

        Long sellerId = orderMapper.getSellerIdByOrderId(orderId);
        if (sellerId == null || !sellerId.equals(userId)) {
            res.put("success", false);
            res.put("message", "无权限审核此退货");
            return res;
        }

        String refundTime = LocalDateTime.now().format(DTF);
        orderMapper.updateRefundStatus(orderId, "rejected", reason != null ? reason : order.getReturnReason(), refundTime);

        res.put("success", true);
        res.put("message", "退货申请已拒绝");
        return res;
    }

    @GetMapping("/messages")
    public MessagesResponse getMessages(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) return new MessagesResponse(new ArrayList<>());
        List<Message> messages = messageMapper.getByUser(userId);
        return new MessagesResponse(messages);
    }

    @GetMapping("/messages/conversations")
    public Object getConversations(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) {
            Map<String, Object> res = new HashMap<>();
            res.put("conversations", new ArrayList<>());
            res.put("status", "ok");
            return res;
        }
        List<Map<String, Object>> conversations = messageMapper.getConversations(userId);
        Map<String, Object> res = new HashMap<>();
        res.put("conversations", conversations);
        res.put("status", "ok");
        return res;
    }

    @GetMapping("/messages/conversation")
    public MessagesResponse getConversationByParams(@RequestParam Long withUserId,
                                                   @RequestParam(required = false) String itemId,
                                                   @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) return new MessagesResponse(new ArrayList<>());
        List<Message> messages = messageMapper.getConversation(userId, withUserId, itemId);
        return new MessagesResponse(messages);
    }

    @GetMapping("/messages/{withUserId}")
    public MessagesResponse getConversation(@PathVariable Long withUserId,
                                             @RequestParam(required = false) String itemId,
                                             @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) return new MessagesResponse(new ArrayList<>());
        List<Message> messages = messageMapper.getConversation(userId, withUserId, itemId);
        return new MessagesResponse(messages);
    }

    @PostMapping("/messages")
    public Map<String, Object> sendMessage(@RequestBody Map<String, Object> body,
                                           @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                           @RequestHeader(value = "X-User-Username", required = false) String username) {
        Map<String, Object> res = new HashMap<>();
        if (userId == null) {
            res.put("success", false);
            res.put("message", "未登录");
            return res;
        }

        Long toUserId = Long.parseLong(body.get("toUserId").toString());
        String content = (String) body.get("content");
        String itemId = body.containsKey("itemId") ? (String) body.get("itemId") : null;
        String itemTitle = body.containsKey("itemTitle") ? (String) body.get("itemTitle") : null;

        Message message = new Message();
        message.setId(UUID.randomUUID().toString());
        message.setFromUserId(userId);
        message.setFromUsername(username);
        message.setToUserId(toUserId);
        message.setContent(content);
        message.setItemId(itemId);
        message.setItemTitle(itemTitle);
        message.setDate(LocalDateTime.now().format(DTF));

        messageMapper.insert(message);

        res.put("success", true);
        res.put("message", message);
        return res;
    }

    @GetMapping("/items/{itemId}/reviews")
    public ReviewsResponse getItemReviews(@PathVariable String itemId) {
        List<Review> reviews = reviewMapper.getByItem(itemId);
        return new ReviewsResponse(reviews);
    }

    @PostMapping("/reviews")
    public Map<String, Object> submitReview(@RequestBody Map<String, Object> body,
                                             @RequestHeader(value = "X-User-Id", required = false) Long userId,
                                             @RequestHeader(value = "X-User-Username", required = false) String username) {
        Map<String, Object> res = new HashMap<>();
        if (userId == null) {
            res.put("success", false);
            res.put("message", "未登录");
            return res;
        }

        String itemId = (String) body.get("itemId");
        int rating = body.get("rating") != null ? Integer.parseInt(body.get("rating").toString()) : 5;
        String comment = body.containsKey("comment") ? (String) body.get("comment") : "";

        Review review = new Review();
        review.setId(UUID.randomUUID().toString());
        review.setItemId(itemId);
        review.setUserId(userId);
        review.setUsername(username);
        review.setRating(rating);
        review.setComment(comment);
        review.setDate(LocalDateTime.now().format(DTF));

        reviewMapper.insert(review);

        res.put("success", true);
        res.put("message", "评价成功");
        return res;
    }
}