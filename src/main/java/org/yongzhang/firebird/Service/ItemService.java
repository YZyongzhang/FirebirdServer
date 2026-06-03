package org.yongzhang.firebird.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.yongzhang.firebird.Data.Item;
import org.yongzhang.firebird.Data.User;
import org.yongzhang.firebird.Mapper.ItemMapper;
import org.yongzhang.firebird.Mapper.OrderMapper;
import org.yongzhang.firebird.Mapper.UserMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ItemService {

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderMapper orderMapper;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String NGINX_IMAGE_PATH = "E:/yongzhang/Nginx_server/images/";

    public Map<String, Object> searchItems(Integer page, Integer size, String q, String category) {
        Map<String, Object> res = new HashMap<>();
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 20 : size;
        int offset = (p - 1) * s;
        List<Item> items = itemMapper.search(q, category, offset, s);
        int total = itemMapper.count(q, category);
        res.put("items", items);
        res.put("total", total);
        return res;
    }

    public List<Item> getPendingItems() {
        List<Item> items = itemMapper.getByStatus("pending_review");
        return items != null ? items : new ArrayList<>();
    }

    @Transactional
    public Map<String, Object> reviewItem(String id, String action, String role) {
        Map<String, Object> res = new HashMap<>();

        if (!"admin".equals(role)) {
            res.put("success", false);
            res.put("message", "无权限");
            return res;
        }

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

    public List<Item> getMyItems(Long userId) {
        if (userId == null) return new ArrayList<>();
        List<Item> items = itemMapper.getBySellerId(userId);
        for (Item item : items) {
            List<org.yongzhang.firebird.Data.Order> orders = orderMapper.getOrdersByItemId(item.getId(), "paid");
            item.setOrders(orders);
        }
        return items;
    }

    public Item getItemById(String id) {
        return itemMapper.getById(id);
    }

    @Transactional
    public Map<String, Object> offlineItem(String id, Long userId, String role) {
        Map<String, Object> res = new HashMap<>();

        Item item = itemMapper.getById(id);
        if (item == null) {
            res.put("success", false);
            res.put("message", "商品不存在");
            return res;
        }

        if (!"admin".equals(role) && !item.getSellerId().equals(userId)) {
            res.put("success", false);
            res.put("message", "无权限下架此商品");
            return res;
        }

        int updated = itemMapper.offline(id, userId);
        res.put("success", updated > 0);
        res.put("message", updated > 0 ? "下架成功" : "下架失败");
        return res;
    }

    @Transactional
    public Map<String, Object> createItem(Long userId, String username, String role,
                                          String title, String description, double price,
                                          String category, MultipartFile[] images) throws IOException {
        Map<String, Object> res = new HashMap<>();

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

        // 图片上传处理
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

        // 创建商品
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

    @Transactional
    public Item updateItem(String id, Long userId, String title, String description,
                           double price, String category, MultipartFile[] images) throws IOException {
        Item existing = itemMapper.getById(id);
        if (existing == null) return null;
        if (existing.getSellerId() != null && userId != null && !existing.getSellerId().equals(userId)) {
            return null;
        }

        List<String> saved = new ArrayList<>();
        String thumb = existing.getThumb();

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

    @Transactional
    public void deleteItem(String id, Long userId) {
        itemMapper.delete(id, userId);
    }

    public Map<String, List<String>> getCategories() {
        Map<String, List<String>> result = new HashMap<>();
        result.put("categories", List.of("灵感", "思考", "模版", "资源"));
        return result;
    }
}