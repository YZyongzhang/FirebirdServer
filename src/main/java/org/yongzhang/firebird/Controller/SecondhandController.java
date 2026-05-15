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

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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

    // GET /items/{id}
    @GetMapping("/items/{id}")
    public Item getItem(@PathVariable String id) {
        return itemMapper.getById(id);
    }

    // POST /items (multipart) - requires X-User-Id header for seller
    @PostMapping(value = "/items", consumes = {"multipart/form-data"})
    public Item createItem(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                           @RequestHeader(value = "X-User-Username", required = false) String username,
                           @RequestParam String title,
                           @RequestParam String description,
                           @RequestParam double price,
                           @RequestParam String category,
                           @RequestParam(required = false) MultipartFile[] images) throws IOException {
        String id = UUID.randomUUID().toString();
        String date = LocalDateTime.now().format(DTF);
        String thumb = null;
        List<String> saved = new ArrayList<>();
        if (images != null && images.length > 0) {
            File uploadDir = new File("src/main/resources/static/uploads");
            if (!uploadDir.exists()) uploadDir.mkdirs();
            for (MultipartFile mf : images) {
                if (mf.isEmpty()) continue;
                String fname = UUID.randomUUID().toString() + "_" + mf.getOriginalFilename();
                File out = new File(uploadDir, fname);
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    fos.write(mf.getBytes());
                }
                String url = "/uploads/" + fname;
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

        itemMapper.insert(item);
        return item;
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
            File uploadDir = new File("src/main/resources/static/uploads");
            if (!uploadDir.exists()) uploadDir.mkdirs();
            for (MultipartFile mf : images) {
                if (mf.isEmpty()) continue;
                String fname = UUID.randomUUID().toString() + "_" + mf.getOriginalFilename();
                File out = new File(uploadDir, fname);
                try (FileOutputStream fos = new FileOutputStream(out)) { fos.write(mf.getBytes()); }
                String url = "/uploads/" + fname;
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
    public void addToCart(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                          @RequestBody Map<String, Object> payload) {
        String itemId = String.valueOf(payload.get("itemId"));
        int quantity = ((Number) payload.getOrDefault("quantity", 1)).intValue();
        Item it = itemMapper.getById(itemId);
        if (it == null) return;
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
        // naive: no items inserted (would require reading cart items by ids). For demo, just return order id
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

