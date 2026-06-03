package org.yongzhang.firebird.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yongzhang.firebird.Data.CartItem;
import org.yongzhang.firebird.Mapper.CartMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CartService {

    @Autowired
    private CartMapper cartMapper;

    public List<CartItem> getCartItems(Long userId) {
        return cartMapper.getByUser(userId);
    }

    @Transactional
    public Map<String, Object> addToCart(Long userId, String itemId, String title,
                                         double price, String thumb, int quantity) {
        Map<String, Object> res = new HashMap<>();

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

    @Transactional
    public Map<String, Object> updateCartItem(String id, Long userId, int quantity) {
        Map<String, Object> res = new HashMap<>();
        cartMapper.updateQuantity(id, userId, quantity);
        res.put("success", true);
        return res;
    }

    @Transactional
    public Map<String, Object> removeFromCart(String id, Long userId) {
        Map<String, Object> res = new HashMap<>();
        cartMapper.delete(id, userId);
        res.put("success", true);
        res.put("message", "移除成功");
        return res;
    }

    @Transactional
    public Map<String, Object> clearCart(Long userId) {
        Map<String, Object> res = new HashMap<>();
        cartMapper.clear(userId);
        res.put("success", true);
        res.put("message", "清空购物车成功");
        return res;
    }
}