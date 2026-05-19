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

    // GET /items
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

    // GET /categories - get available categories
    @GetMapping("/categories")
    public Map<String, List<String>> getCategories() {
        Map<String, List<String>> result = new HashMap<>();
        result.put("categories", Arrays.asList("灵感", "思考", "模版", "资源"));
        return result;
    }

    // GET /items/my - get my published items
    @GetMapping("/items/my")
    public List<Item> getMyItems(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) return new ArrayList<>();
        List<Item> items = itemMapper.getBySellerId(userId);
        // 为每个商品加载待发货的订单
        for (Item item : items) {
            List<Order> orders = orderMapper.getOrdersByItemId(item.getId(), "paid");
            item.setOrders(orders);
        }
        return items;
    }

    // GET /items/{id}
    @GetMapping("/items/{id}")
    public Item getItem(@PathVariable String id) {
        return itemMapper.getById(id);
    }

    // PUT /items/{id}/offline - take item offline
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

    // POST /items (multipart) - requires X-User-Id header for seller
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
        res.put("data", item);
        return res;
    }

    // PUT /items/{id}
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

        itemMapper.update(existing);
        return existing;
    }

    // DELETE /items/{id}
    @DeleteMapping("/items/{id}")
    public void deleteItem(@PathVariable String id, @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        itemMapper.delete(id, userId);
    }

    // GET /categories
    @GetMapping("/categories")
    public CategoriesResponse getCategories() {
        System.out.println("Fetching categories...");
        List<String> cats = itemMapper.categories();
        return new CategoriesResponse(cats);
    }

    // Cart endpoints
    @GetMapping("/cart")
    public CartResponse getCart(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        List<CartItem> items = cartMapper.getByUser(userId);
        return new CartResponse(items);
    }

    @PostMapping("/cart")
    public Map<String, Object> addToCart(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                          @RequestHeader(value = "X-User-Role", required = false) String role,
                          @RequestBody Map<String, Object> payload) {
        Map<String, Object> res = new HashMap<>();
        
        if (userId == null) {
            res.put("success", false);
            res.put("message", "未登录");
            return res;
        }
        
        if ("seller".equals(role)) {
            res.put("success", false);
            res.put("message", "商家用户不能购买商品");
            return res;
        }
        
        String itemId = String.valueOf(payload.get("itemId"));
        int quantity = ((Number) payload.getOrDefault("quantity", 1)).intValue();
        Item it = itemMapper.getById(itemId);
        if (it == null) {
            res.put("success", false);
            res.put("message", "商品不存在");
            return res;
        }
        CartItem existing = null;
        // naive: always insert new cart item
        CartItem ci = new CartItem();
        ci.setId(UUID.randomUUID().toString());
        ci.setItemId(itemId);
        ci.setTitle(it.getTitle());
        ci.setPrice(it.getPrice());
        ci.setThumb(it.getThumb());
        ci.setQuantity(quantity);
        ci.setUserId(userId);
        cartMapper.insert(ci);
        res.put("success", true);
        return res;
    }

    @PutMapping("/cart/{id}")
    public void updateCartItem(@PathVariable String id, @RequestHeader(value = "X-User-Id", required = false) Long userId,
                               @RequestBody Map<String, Object> payload) {
        int quantity = ((Number) payload.getOrDefault("quantity", 1)).intValue();
        cartMapper.updateQuantity(id, userId, quantity);
    }

    @DeleteMapping("/cart/{id}")
    public void removeFromCart(@PathVariable String id, @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        cartMapper.delete(id, userId);
    }

    @DeleteMapping("/cart")
    public void clearCart(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        cartMapper.clear(userId);
    }

    // Messages
    @GetMapping("/messages/conversations")
    public Object getConversations(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        List<Map<String, Object>> conversations = messageMapper.getConversations(userId);
        Map<String, Object> res = new HashMap<>();
        res.put("conversations", conversations);
        res.put("status", "ok");
        return res;
    }

    @GetMapping("/messages/item/{itemId}")
    public Object getMessagesByItem(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                    @PathVariable String itemId) {
        List<Message> msgs = messageMapper.getMessagesByItem(userId, itemId);
        Map<String, Object> res = new HashMap<>();
        res.put("messages", msgs);
        res.put("status", "ok");
        return res;
    }

    @GetMapping("/messages/conversation")
    public Object getConversation(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                  @RequestParam Long withUserId,
                                  @RequestParam String itemId) {
        List<Message> msgs = messageMapper.getConversation(userId, withUserId, itemId);
        messageMapper.markAsRead(userId, withUserId, itemId);
        Map<String, Object> res = new HashMap<>();
        res.put("messages", msgs);
        res.put("status", "ok");
        return res;
    }

    @PostMapping("/messages")
    public Map<String, Object> sendMessage(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                           @RequestHeader(value = "X-User-Username", required = false) String username,
                                           @RequestBody Map<String, Object> payload) {
        Long toUserId = ((Number) payload.get("toUserId")).longValue();
        String content = String.valueOf(payload.get("content"));
        String itemId = String.valueOf(payload.get("itemId"));
        String itemTitle = String.valueOf(payload.get("itemTitle"));
        
        Message m = new Message();
        m.setId(UUID.randomUUID().toString());
        m.setFromUserId(userId);
        m.setFromUsername(username);
        m.setToUserId(toUserId);
        m.setContent(content);
        m.setDate(LocalDateTime.now().format(DTF));
        m.setItemId(itemId);
        m.setItemTitle(itemTitle);
        m.setIsRead(0);
        messageMapper.insert(m);
        
        Map<String, Object> res = new HashMap<>();
        res.put("status", "ok");
        res.put("message", m);
        return res;
    }
    
    // GET /messages/sellers - admin can get all sellers for messaging
    @GetMapping("/messages/sellers")
    public Object getSellers(@RequestHeader(value = "X-User-Role", required = false) String role) {
        Map<String, Object> res = new HashMap<>();
        if (!"admin".equals(role)) {
            res.put("success", false);
            res.put("message", "无权限");
            return res;
        }
        List<User> sellers = userMapper.getSellers();
        res.put("success", true);
        res.put("sellers", sellers);
        return res;
    }

    @GetMapping("/messages/unread-count")
    public UnreadCountResponse getUnreadCount(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        int c = messageMapper.countUnread(userId);
        return new UnreadCountResponse(c);
    }

    // Orders
    @GetMapping("/orders")
    public OrdersResponse getOrders(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        List<Order> orders = orderMapper.getByUser(userId);
        return new OrdersResponse(orders);
    }

    @PostMapping("/orders")
    public Map<String, Object> createOrder(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                           @RequestBody CreateOrderRequest req) {
        String orderId = UUID.randomUUID().toString();
        Order order = new Order();
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setTotalAmount(req.getTotalAmount());
        order.setStatus("created");
        order.setDate(LocalDateTime.now().format(DTF));
        orderMapper.insert(order);
        
        // 从购物车读取商品并插入订单商品表
        List<CartItem> cartItems = cartMapper.getByUser(userId);
        for (CartItem item : cartItems) {
            if (item.getItemId() == null || item.getItemId().isEmpty()) {
                continue; // 跳过无效的购物车商品
            }
            CartItem orderItem = new CartItem();
            orderItem.setId(UUID.randomUUID().toString());
            orderItem.setOrderId(orderId);
            orderItem.setItemId(item.getItemId());
            orderItem.setTitle(item.getTitle());
            orderItem.setPrice(item.getPrice());
            orderItem.setQuantity(item.getQuantity());
            orderMapper.insertOrderItem(orderItem);
        }
        
        // 清空购物车
        cartMapper.clear(userId);
        
        Map<String, Object> res = new HashMap<>();
        res.put("orderId", orderId);
        res.put("status", "created");
        return res;
    }

    @PostMapping("/orders/{orderId}/pay")
    public Map<String, Object> payOrder(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                        @PathVariable String orderId,
                                        @RequestBody Map<String, Object> payload) {
        // paymentMethod ignored in demo
        orderMapper.updateStatus(orderId, userId, "paid");
        Map<String, Object> r = new HashMap<>();
        r.put("success", true);
        r.put("orderId", orderId);
        r.put("status", "paid");
        return r;
    }

    @PutMapping("/orders/{orderId}/cancel")
    public void cancelOrder(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                            @PathVariable String orderId) {
        orderMapper.updateStatus(orderId, userId, "cancelled");
    }

    @PutMapping("/orders/{orderId}/ship")
    public Map<String, Object> shipOrder(@PathVariable String orderId,
                                         @RequestBody Map<String, Object> payload) {
        String trackingNumber = String.valueOf(payload.getOrDefault("trackingNumber", ""));
        orderMapper.updateOrderStatus(orderId, "shipped", trackingNumber);
        Map<String, Object> r = new HashMap<>();
        r.put("success", true);
        r.put("orderId", orderId);
        r.put("status", "shipped");
        r.put("trackingNumber", trackingNumber);
        return r;
    }

    // 获取商家的所有订单（管理员查看）
    @GetMapping("/orders/seller/{sellerId}")
    public List<Order> getOrdersBySeller(@PathVariable Long sellerId) {
        return orderMapper.getOrdersBySellerId(sellerId);
    }

    @PutMapping("/orders/{orderId}/confirm")
    public void confirmOrder(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                             @PathVariable String orderId) {
        orderMapper.updateStatus(orderId, userId, "completed");
    }

    @PutMapping("/orders/{orderId}/return")
    public Map<String, Object> returnOrder(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                           @PathVariable String orderId,
                                           @RequestBody Map<String, Object> payload) {
        String reason = String.valueOf(payload.getOrDefault("reason", ""));
        orderMapper.updateStatus(orderId, userId, "cancelled");
        Map<String, Object> r = new HashMap<>();
        r.put("success", true);
        r.put("message", "退货申请已提交，原因：" + reason);
        return r;
    }

    // Reviews
    @GetMapping("/items/{itemId}/reviews")
    public ReviewsResponse getItemReviews(@PathVariable String itemId) {
        List<Review> reviews = reviewMapper.getByItem(itemId);
        return new ReviewsResponse(reviews);
    }

    @PostMapping("/items/{itemId}/reviews")
    public Review submitReview(@PathVariable String itemId,
                               @RequestHeader(value = "X-User-Id", required = false) Long userId,
                               @RequestHeader(value = "X-User-Username", required = false) String username,
                               @RequestBody Map<String, Object> payload) {
        Review r = new Review();
        r.setId(UUID.randomUUID().toString());
        r.setItemId(itemId);
        r.setUserId(userId);
        r.setUsername(username);
        r.setRating(((Number) payload.getOrDefault("rating", 5)).intValue());
        r.setComment(String.valueOf(payload.getOrDefault("comment", "")));
        r.setDate(LocalDateTime.now().format(DTF));
        reviewMapper.insert(r);
        return r;
    }

}

