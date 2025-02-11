package com.example.backend.controller;

import com.example.backend.dto.OrderStatusDTO;
import com.example.backend.entity.*;
import com.example.backend.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zhouhaoran
 * @date 2024/3/29
 * @project Backend
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    @Autowired
    IOrderService orderService;
    @Autowired
    IOrderDetailService orderDetailService;
    @Autowired
    IMenuItemService menuItemService;
    @Autowired
    IUserService userService;
    @Autowired
    ICouponService couponService;

    @Autowired
    IUserCouponService userCouponService;
    private ResponseEntity<Map<String, Object>> createResponse(HttpStatus status, String message, Object data) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("status", status.value() + " " + status.getReasonPhrase());
        responseBody.put("message", message);
        responseBody.put("data", data);
        return new ResponseEntity<>(responseBody, status);
    }
    /**
     * 创建订单。
     *
     * @param order 包含订单信息的对象，通过请求体传入。订单信息包括用户ID、商品信息、总价格等。
     * @param authentication 当前用户的认证信息，用于权限验证。确保只有已认证的用户才能创建订单。
     * @return 如果认证失败，返回未授权的状态码（401）；否则，返回创建成功的订单信息，包括订单ID、用户ID、状态、总价格、下单时间等。
     */
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Order order, Authentication authentication) {
        // 权限验证：判断用户是否已认证，未认证返回401
        if (authentication == null || !authentication.isAuthenticated()) {
            return createResponse(HttpStatus.UNAUTHORIZED, "用户未认证", null);
        }

        // 初始化订单状态和时间
        order.setTotalPrice(BigDecimal.ZERO);  // 初始金额设置为0
        order.setStatus(OrderStatus.CREATED.toString());  // 订单状态设置为已创建
        order.setUpdateTime(new Timestamp(System.currentTimeMillis()));  // 设置订单更新时间为当前时间

        // 保存订单到数据库
        orderService.save(order);

        // 准备订单创建成功后返回的数据
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", order.getOrderId());
        data.put("userId", order.getUserId());
        data.put("storeId", order.getStoreId());
        data.put("status", order.getStatus());
        data.put("totalPrice", order.getTotalPrice());
        data.put("orderTime", order.getOrderTime());
        data.put("notes", order.getNotes());
        data.put("dineOption", order.getDineOption());
        data.put("updateTime", order.getUpdateTime());

        // 返回订单创建成功的响应，包含订单详细信息
        return createResponse(HttpStatus.OK, "订单创建成功", data);
    }


    /**
     * 根据订单ID获取订单信息。
     *
     * @param orderId 通过路径变量传递的订单ID。
     * @param authentication 当前请求的认证信息，用于权限验证。
     * @return 如果找到相应的订单，返回包含订单信息的ResponseEntity；如果没有找到，返回一个订单未找到的ResponseEntity；如果用户未认证，返回未授权的ResponseEntity。
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable Integer orderId, Authentication authentication) {
        // 检查用户是否认证
        if (authentication == null || !authentication.isAuthenticated()) {
            return createResponse(HttpStatus.UNAUTHORIZED, "用户未认证", null);
        }
        // 根据订单ID获取订单
        Order order = orderService.getById(orderId);
        // 如果订单不存在，返回未找到的响应
        if (order == null) {
            return createResponse(HttpStatus.NOT_FOUND, "订单未找到", null);
        }

        // 构建订单信息的响应体
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", order.getOrderId());
        data.put("userId", order.getUserId());
        data.put("storeId", order.getStoreId());
        data.put("status", order.getStatus());
        data.put("totalPrice", order.getTotalPrice());
        data.put("orderTime", order.getOrderTime());
        data.put("notes", order.getNotes());
        data.put("dineOption", order.getDineOption());
        data.put("updateTime", order.getUpdateTime());

        // 返回订单信息的响应
        return createResponse(HttpStatus.OK, "订单信息获取成功", data);
    }



    /**
     * 获取指定用户的所有订单。
     *
     * @param userId 用户ID，通过URL路径变量传递。表示需要查询订单的用户ID。
     * @param authentication 当前请求的认证信息，用于权限验证。
     * @return 返回一个包含该用户所有订单的响应实体。如果用户没有订单，则返回空列表。
     *         如果用户未进行认证，返回401未授权状态。
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserOrders(@PathVariable Integer userId, Authentication authentication) {
        // 验证用户是否认证
        if (authentication == null || !authentication.isAuthenticated()) {
            return createResponse(HttpStatus.UNAUTHORIZED, "用户未认证", null);
        }
        // 根据用户ID查询订单
        List<Order> orders = orderService.lambdaQuery().eq(Order::getUserId, userId).list();

        // 将订单信息转换为简洁的Map格式
        List<Map<String, Object>> orderDetails = orders.stream().map(order -> {
            Map<String, Object> detail = new HashMap<>();
            detail.put("orderId", order.getOrderId());
            detail.put("userId", order.getUserId());
            detail.put("storeId", order.getStoreId());
            detail.put("status", order.getStatus());
            detail.put("totalPrice", order.getTotalPrice());
            detail.put("orderTime", order.getOrderTime());
            detail.put("notes", order.getNotes());
            detail.put("dineOption", order.getDineOption());
            detail.put("updateTime", order.getUpdateTime());
            return detail;
        }).collect(Collectors.toList());

        // 准备返回的数据结构
        Map<String, Object> data = new HashMap<>();
        data.put("orders", orderDetails);
        // 构造成功返回的响应实体
        return createResponse(HttpStatus.OK, "用户订单列表获取成功", data);
    }


    /**
     * 获取所有订单的详细信息列表。
     *
     * 该接口不接受任何参数，返回所有订单的详细信息集合。订单信息以Map的形式返回，包括订单的各种属性，如订单ID、用户ID、商店ID等。
     *
     * @return ResponseEntity<?> 包含订单详情列表的响应实体，其中?为Map类型，包含"orders"键，其值为订单详情列表。
     */
    @GetMapping
    public ResponseEntity<?> getAllOrders() {
        // 从订单服务获取所有订单列表
        List<Order> orders = orderService.list();

        // 将订单列表映射转换为订单详情列表，每个详情为一个Map，包含订单的各种属性
        List<Map<String, Object>> orderDetails = orders.stream().map(order -> {
            Map<String, Object> detail = new HashMap<>();
            detail.put("orderId", order.getOrderId());
            detail.put("userId", order.getUserId());
            detail.put("storeId", order.getStoreId());
            detail.put("status", order.getStatus());
            detail.put("totalPrice", order.getTotalPrice());
            detail.put("orderTime", order.getOrderTime());
            detail.put("notes", order.getNotes());
            detail.put("dineOption", order.getDineOption());
            detail.put("updateTime", order.getUpdateTime());
            return detail;
        }).collect(Collectors.toList());

        // 将订单详情列表封装到一个大Map中，作为数据部分返回
        Map<String, Object> data = new HashMap<>();
        data.put("orders", orderDetails);

        // 创建并返回一个包含状态码、消息和数据的响应实体
        return createResponse(HttpStatus.OK, "所有订单列表获取成功", data);
    }


    /**
     * 删除订单。
     *
     * @param orderId 订单ID，通过URL路径变量传递。
     * @param authentication 当前请求的认证信息，用于权限验证。
     * @return 如果删除成功，则返回200 OK的响应实体；如果找不到对应的订单，则返回404 Not Found的响应实体。
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<?> deleteOrder(@PathVariable Integer orderId, Authentication authentication) {
        // 验证用户是否认证
        if (authentication == null || !authentication.isAuthenticated()) {
            return createResponse(HttpStatus.UNAUTHORIZED, "用户未认证", null);
        }

        // 尝试删除订单
        boolean removed = orderService.removeById(orderId);
        if (removed) {
            return createResponse(HttpStatus.OK, "订单删除成功", null);
        } else {
            return createResponse(HttpStatus.NOT_FOUND, "订单未找到", null);
        }
    }


    /**
     * 获取订单统计信息。
     *
     * @return 返回包含订单总数、各状态订单数量的统计信息。
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getOrderStats() {
        // 从订单服务获取所有订单列表
        List<Order> orders = orderService.list();

        // 统计各状态订单数量
        Map<String, Long> stats = orders.stream()
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));

        // 添加总订单数
        stats.put("总共", (long) orders.size());

        // 返回统计信息
        return createResponse(HttpStatus.OK, "订单统计信息获取成功", stats);
    }


    /**
     * 确认订单操作。
     *
     * @param orderId 订单ID，路径变量。用于确定要确认的订单。
     * @param userCouponId 优惠券ID，可选查询参数，用于应用优惠券折扣。
     * @param authentication 用户认证信息，用于验证用户是否已认证。
     * @return ResponseEntity<?> 返回HTTP响应实体，包含订单确认结果。
     *         通过不同的HTTP状态码和消息体，告知客户端订单确认的结果。
     */
    @PutMapping("/confirm/{orderId}")
    public ResponseEntity<?> confirmOrder(@PathVariable Integer orderId, @RequestParam(required = false) Integer userCouponId, Authentication authentication) {
        // 验证用户是否已认证，未认证返回401
        if (authentication == null || !authentication.isAuthenticated()) {
            return createResponse(HttpStatus.UNAUTHORIZED, "未认证的用户", null);
        }

        // 根据订单ID获取订单，不存在返回404
        Order order = orderService.getById(orderId);
        if (order == null) {
            return createResponse(HttpStatus.NOT_FOUND, "订单未找到", null);
        }

        // 检查订单状态是否允许更新为进行中，不允许返回400
        if (!OrderStatus.CREATED.toString().equals(order.getStatus())) {
            return createResponse(HttpStatus.BAD_REQUEST, "订单状态不允许此操作", null);
        }

        // 获取用户信息
        User user = userService.getById(order.getUserId());
        if (user == null) {
            return createResponse(HttpStatus.BAD_REQUEST, "用户不存在", null);
        }

        // 计算订单的初始总价格
        BigDecimal totalOrderPrice = calculateTotalPrice(order);

        // 如果提供了优惠券ID，则尝试应用优惠券折扣
        BigDecimal discount = BigDecimal.ZERO;
        if (userCouponId != null) {
            // 验证优惠券有效性并计算折扣
            UserCoupon userCoupon = userCouponService.getById(userCouponId);
            if (userCoupon == null || userCoupon.getIsUsed()) {
                return createResponse(HttpStatus.BAD_REQUEST, "无效的用户优惠券", null);
            }

            Coupon coupon = couponService.getById(userCoupon.getCouponId());
            if (coupon == null || !coupon.getIsActive() || coupon.getExpirationDate().before(new Date())) {
                return createResponse(HttpStatus.BAD_REQUEST, "无效或过期的优惠券", null);
            }

            // 检查订单总价是否满足优惠券的最低消费金额
            if (totalOrderPrice.compareTo(coupon.getMinPurchase()) < 0) {
                return createResponse(HttpStatus.BAD_REQUEST, "订单总价未达到优惠券的最低消费金额", null);
            }

            // 应用优惠券折扣
            discount = coupon.getDiscount();
        }

        // 计算折扣后的订单总价格
        BigDecimal newTotalPrice = totalOrderPrice.subtract(discount);

        // 检查用户余额是否足够支付折扣后的订单总价格
        if (user.getBalance().compareTo(newTotalPrice) < 0) {
            return createResponse(HttpStatus.BAD_REQUEST, "用户余额不足", null);
        }

        // 如果提供了优惠券ID并且用户余额足够，则标记用户优惠券为已使用并记录订单ID
        if (userCouponId != null) {
            UserCoupon userCoupon = userCouponService.getById(userCouponId);
            userCoupon.setIsUsed(true);
            userCoupon.setOrderId(orderId);
            userCouponService.updateById(userCoupon);
        }

        // 更新订单总价格
        order.setTotalPrice(newTotalPrice);

        // 更新订单状态为进行中，减少库存并扣除用户余额（考虑优惠券折扣）
        updateInventory(order, false);
        deductUserBalance(order);
        order.setStatus(OrderStatus.IN_PROGRESS.toString());
        order.setUpdateTime(new Timestamp(System.currentTimeMillis())); // 更新订单的更新时间
        order.setOrderTime(new Timestamp(System.currentTimeMillis()));  // 设置订单时间为当前时间
        orderService.updateById(order);

        // 订单确认成功，返回200
        return createResponse(HttpStatus.OK, "订单确认成功", order);
    }



    /**
     * 完成指定订单的操作。
     *
     * @param orderId 订单的ID，通过URL路径变量传递。
     * @param authentication 当前用户的认证信息，用于权限验证。
     * @return 根据操作结果返回不同的响应实体，包括订单完成成功、未认证、订单不存在或订单状态不允许完成操作的情况。
     */
    @PutMapping("/complete/{orderId}")
    public ResponseEntity<?> completeOrder(@PathVariable Integer orderId, Authentication authentication) {
        // 验证用户是否已认证，未认证返回401
        if (authentication == null || !authentication.isAuthenticated()) {
            return createResponse(HttpStatus.UNAUTHORIZED, "未认证的用户", null);
        }

        // 根据订单ID获取订单，不存在返回404
        Order order = orderService.getById(orderId);
        if (order == null) {
            return createResponse(HttpStatus.NOT_FOUND, "订单未找到", null);
        }

        // 检查订单状态是否允许完成操作，不允许返回400
        if (!OrderStatus.IN_PROGRESS.toString().equals(order.getStatus())) {
            return createResponse(HttpStatus.BAD_REQUEST, "订单状态不允许此操作", null);
        }

        order.setStatus(OrderStatus.COMPLETED.toString());
        order.setUpdateTime(new Timestamp(System.currentTimeMillis())); // 更新订单的更新时间
        orderService.updateById(order);
        // 返回订单完成成功的响应，包含订单信息
        return createResponse(HttpStatus.OK, "订单已完成", order);
    }

    /**
     * 取消订单的处理。
     *
     * @param orderId 通过URL路径变量传递的订单ID，用于标识需要取消的订单。
     * @param authentication 用户的认证信息，用于验证请求者的身份。
     * @return 根据操作的结果返回不同的响应实体，包括订单未找到、用户未认证、订单状态不允许取消或取消成功的情况。
     */
    @PutMapping("/cancel/{orderId}")
    public ResponseEntity<?> cancelOrder(@PathVariable Integer orderId, Authentication authentication) {
        // 检查用户是否已认证，未认证返回401
        if (authentication == null || !authentication.isAuthenticated()) {
            return createResponse(HttpStatus.UNAUTHORIZED, "未认证的用户", null);
        }

        // 根据订单ID获取订单，订单不存在返回404
        Order order = orderService.getById(orderId);
        if (order == null) {
            return createResponse(HttpStatus.NOT_FOUND, "订单未找到", null);
        }

        // 检查订单状态是否允许取消，不允许则返回400
        if (!OrderStatus.CREATED.toString().equals(order.getStatus()) && !OrderStatus.IN_PROGRESS.toString().equals(order.getStatus())) {
            return createResponse(HttpStatus.BAD_REQUEST, "订单状态不允许此操作", null);
        }

        updateInventory(order, true); // 退还库存
        refundUserBalance(order); // 退还用户余额
        resetUserCoupon(orderId); // 重置优惠券状态为未使用
        order.setStatus(OrderStatus.CANCELLED.toString());
        order.setUpdateTime(new Timestamp(System.currentTimeMillis())); // 更新订单的更新时间
        orderService.updateById(order);
        // 返回订单取消成功的响应
        return createResponse(HttpStatus.OK, "订单已取消", order);
    }


    /**
     * 处理订单退款请求。
     *
     * @param orderId  需要退款的订单ID，通过URL路径变量传递。
     * @param authentication  当前请求的认证信息，用于验证用户身份。
     * @return  根据操作结果返回不同的响应实体，包括订单退款成功、订单未找到、用户未认证、订单状态不允许退款等不同的状态码和消息。
     */
    @PutMapping("/refund/{orderId}")
    public ResponseEntity<?> refundOrder(@PathVariable Integer orderId, Authentication authentication) {
        // 检查用户是否已认证，未认证返回401
        if (authentication == null || !authentication.isAuthenticated()) {
            return createResponse(HttpStatus.UNAUTHORIZED, "未认证的用户", null);
        }

        // 根据订单ID查询订单，未找到返回404
        Order order = orderService.getById(orderId);
        if (order == null) {
            return createResponse(HttpStatus.NOT_FOUND, "订单未找到", null);
        }

        // 检查订单状态是否允许退款，不允许返回400
        if (!OrderStatus.COMPLETED.toString().equals(order.getStatus())) {
            return createResponse(HttpStatus.BAD_REQUEST, "订单状态不允许此操作", null);
        }

        updateInventory(order, true); // 退还库存
        refundUserBalance(order); // 实际退款
        resetUserCoupon(orderId); // 重置优惠券状态为未使用
        order.setStatus(OrderStatus.REFUNDED.toString());
        order.setUpdateTime(new Timestamp(System.currentTimeMillis())); // 更新订单的更新时间
        orderService.updateById(order);

        // 退款成功，返回200和订单信息
        return createResponse(HttpStatus.OK, "订单已退款", order);
    }

    /**
     * 更新订单信息。
     *
     * @param orderId 订单ID，通过路径变量传递。
     * @param updatedOrder 包含更新后的订单信息的对象，通过请求体传入。
     * @param authentication 当前用户的认证信息，用于权限验证。
     * @return 返回包含更新后订单信息的ResponseEntity；如果订单未找到，返回一个订单未找到的ResponseEntity；如果用户未认证，返回未授权的ResponseEntity。
     */
    @PutMapping("/{orderId}")
    public ResponseEntity<?> updateOrder(@PathVariable Integer orderId, @RequestBody Order updatedOrder, Authentication authentication) {
        // 检查用户是否认证
        if (authentication == null || !authentication.isAuthenticated()) {
            return createResponse(HttpStatus.UNAUTHORIZED, "用户未认证", null);
        }

        // 根据订单ID获取订单
        Order existingOrder = orderService.getById(orderId);
        // 如果订单不存在，返回未找到的响应
        if (existingOrder == null) {
            return createResponse(HttpStatus.NOT_FOUND, "订单未找到", null);
        }

        // 更新订单信息
        existingOrder.setUserId(updatedOrder.getUserId());
        existingOrder.setStoreId(updatedOrder.getStoreId());
        existingOrder.setStatus(updatedOrder.getStatus());
        existingOrder.setTotalPrice(updatedOrder.getTotalPrice());
        existingOrder.setOrderTime(updatedOrder.getOrderTime());
        existingOrder.setNotes(updatedOrder.getNotes());
        existingOrder.setDineOption(updatedOrder.getDineOption());
        existingOrder.setUpdateTime(new Timestamp(System.currentTimeMillis())); // 更新订单的更新时间


        // 保存更新后的订单到数据库
        orderService.updateById(existingOrder);

        // 根据订单ID重新获取更新后的订单
        Order newOrder = orderService.getById(orderId);


        // 构建更新后订单信息的响应体
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", newOrder.getOrderId());
        data.put("userId", newOrder.getUserId());
        data.put("storeId", newOrder.getStoreId());
        data.put("status", newOrder.getStatus());
        data.put("totalPrice", newOrder.getTotalPrice());
        data.put("orderTime", newOrder.getOrderTime());
        data.put("notes", newOrder.getNotes());
        data.put("dineOption", newOrder.getDineOption());
        data.put("updateTime", newOrder.getUpdateTime());

        // 返回订单信息的响应
        return createResponse(HttpStatus.OK, "订单信息更新成功", data);
    }

    /**
     * 更新库存信息。
     * 该方法根据订单中的商品详情，更新对应商品的库存数量。
     * 如果是退款操作，则增加库存；否则减少库存。
     *
     * @param order 表示一个订单对象，用于获取订单详情和订单ID。
     * @param isRefund 表示操作类型，true代表退款操作，false代表非退款操作（如正常购买）。
     */
    private void updateInventory(Order order, boolean isRefund) {
        // 根据订单ID查询所有的订单详情
        List<OrderDetail> orderDetails = orderDetailService.lambdaQuery().eq(OrderDetail::getOrderId, order.getOrderId()).list();
        for (OrderDetail detail : orderDetails) {
            // 尝试获取订单详情对应的菜单项
            MenuItem item = menuItemService.getById(detail.getItemId());
            if (item != null) {
                // 根据操作类型计算新的库存数量
                int newStock = isRefund ? item.getSizeStock() + detail.getQuantity() : item.getSizeStock() - detail.getQuantity();
                // 更新菜单项的库存数量
                item.setSizeStock(newStock);
                // 保存更新后的菜单项信息
                menuItemService.updateById(item);
            }
        }
    }

    /**
     * 从用户余额中扣除订单费用。
     * @param order 包含订单信息的对象，需要有用户ID和订单商品等信息。
     * 该方法不返回任何值。
     */
    private void deductUserBalance(Order order) {
        // 根据订单中的用户ID获取用户信息
        User user = userService.getById(order.getUserId());
        if (user != null) {
            // 直接使用订单的总价格
            BigDecimal totalPrice = order.getTotalPrice();
            // 从用户余额中扣除订单总价
            user.setBalance(user.getBalance().subtract(totalPrice));
            // 更新用户信息，将扣除余额后的余额保存
            userService.updateById(user);
        }
    }

    /**
     * 重置订单中使用的优惠券状态为未使用。
     * 该方法通过订单ID找出所有已使用的用户优惠券，并将它们的状态重置为未使用，同时清除它们与订单的关联。
     *
     * @param orderId 订单ID，用于获取相关的用户优惠券信息。
     */
    private void resetUserCoupon(Integer orderId) {
        // 查询当前订单中所有已使用的用户优惠券
        List<UserCoupon> userCoupons = userCouponService.lambdaQuery()
                .eq(UserCoupon::getOrderId, orderId)
                .eq(UserCoupon::getIsUsed, true)
                .list();

        // 遍历查询结果，将优惠券状态重置，并清除与订单的关联
        for (UserCoupon userCoupon : userCoupons) {
            userCoupon.setIsUsed(false);
            userCoupon.setOrderId(null); // 清除关联的订单ID
            userCouponService.updateById(userCoupon);
        }
    }


    /**
     * 为用户办理退款手续，将订单金额退还至用户余额。
     * @param order 退款订单，包含用户ID和订单详情。
     */
    private void refundUserBalance(Order order) {
        // 根据订单获取用户信息
        User user = userService.getById(order.getUserId());
        if (user != null) {
            // 计算订单的总价格
            BigDecimal totalPrice = calculateTotalPrice(order);
            // 将订单总价格加到用户余额上，实现退款
            user.setBalance(user.getBalance().add(totalPrice));
            // 更新用户信息
            userService.updateById(user);
        }
    }


    /**
     * 计算订单的总价格。
     *
     * @param order 订单对象，不可为null。
     * @return 订单的总价格，返回一个BigDecimal类型，保证精确度。
     */
    private BigDecimal calculateTotalPrice(Order order) {
        // 通过订单ID查询订单详情列表
        List<OrderDetail> orderDetails = orderDetailService.lambdaQuery().eq(OrderDetail::getOrderId, order.getOrderId()).list();
        // 细节：通过流处理订单详情，计算每项商品的价格乘以数量，然后累加得到订单总价
        return orderDetails.stream()
                .map(detail -> detail.getPrice().multiply(new BigDecimal(detail.getQuantity()))) // 计算商品总价
                .reduce(BigDecimal.ZERO, BigDecimal::add); // 累加所有商品总价
    }

    /**
     * 获取指定用户的已完成订单。
     *
     * @param userId 用户ID，通过路径变量传递。
     * @param authentication 当前请求的认证信息，用于权限验证。
     * @return 返回包含该用户所有已完成订单的响应实体。如果用户没有已完成订单，则返回空列表。
     *         如果用户未进行认证，返回401未授权状态。
     */
    @GetMapping("/user/{userId}/completed")
    public ResponseEntity<Map<String, Object>> getCompletedOrders(@PathVariable Integer userId, Authentication authentication) {
        // 验证用户是否认证
        if (authentication == null || !authentication.isAuthenticated()) {
            return createResponse(HttpStatus.UNAUTHORIZED, "用户未认证", null);
        }

        // 根据用户ID查询已完成订单
        List<Order> completedOrders = orderService.lambdaQuery()
                .eq(Order::getUserId, userId)
                .eq(Order::getStatus, OrderStatus.COMPLETED.toString())
                .list();

        // 将订单信息转换为包含用户信息、订单详情和菜单项信息的Map格式
        List<Map<String, Object>> orderDetails = completedOrders.stream().map(order -> {
            Map<String, Object> detail = new HashMap<>();
            detail.put("orderId", order.getOrderId());
            detail.put("userId", order.getUserId());
            detail.put("storeId", order.getStoreId());
            detail.put("status", order.getStatus());
            detail.put("totalPrice", order.getTotalPrice());
            detail.put("orderTime", order.getOrderTime());
            detail.put("notes", order.getNotes());
            detail.put("dineOption", order.getDineOption());
            detail.put("updateTime", order.getUpdateTime());

            // 获取用户信息
            User user = userService.getById(order.getUserId());
            if (user != null) {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("userId", user.getUserId());
                userInfo.put("name", user.getName());
                userInfo.put("phone", user.getPhone());
                userInfo.put("registrationDate", user.getRegistrationDate());
                userInfo.put("balance", user.getBalance());
                userInfo.put("role", user.getRole());
                detail.put("userInfo", userInfo);
            }

            // 获取订单详情信息
            List<OrderDetail> orderDetailsList = orderDetailService.lambdaQuery().eq(OrderDetail::getOrderId, order.getOrderId()).list();
            List<Map<String, Object>> orderDetailsInfo = orderDetailsList.stream().map(orderDetail -> {
                Map<String, Object> detailInfo = new HashMap<>();
                detailInfo.put("detailId", orderDetail.getDetailId());
                detailInfo.put("orderId", orderDetail.getOrderId());
                detailInfo.put("itemId", orderDetail.getItemId());
                detailInfo.put("quantity", orderDetail.getQuantity());
                detailInfo.put("price", orderDetail.getPrice());
                detailInfo.put("size", orderDetail.getSize());
                detailInfo.put("specialRequests", orderDetail.getSpecialRequests());

                // 获取菜单项信息
                MenuItem menuItem = menuItemService.getById(orderDetail.getItemId());
                if (menuItem != null) {
                    Map<String, Object> menuItemInfo = new HashMap<>();
                    menuItemInfo.put("itemId", menuItem.getItemId());
                    menuItemInfo.put("storeId", menuItem.getStoreId());
                    menuItemInfo.put("name", menuItem.getName());
                    menuItemInfo.put("description", menuItem.getDescription());
                    menuItemInfo.put("imageUrl", menuItem.getImageUrl());
                    menuItemInfo.put("category", menuItem.getCategory());
                    menuItemInfo.put("smallSizePrice", menuItem.getSmallSizePrice());
                    menuItemInfo.put("largeSizePrice", menuItem.getLargeSizePrice());
                    menuItemInfo.put("sizeStock", menuItem.getSizeStock());
                    detailInfo.put("menuItemInfo", menuItemInfo);
                }

                return detailInfo;
            }).collect(Collectors.toList());
            detail.put("orderDetails", orderDetailsInfo);

            return detail;
        }).collect(Collectors.toList());

        // 准备返回的数据结构
        Map<String, Object> data = new HashMap<>();
        data.put("orders", orderDetails);

        // 构造成功返回的响应实体
        return createResponse(HttpStatus.OK, "用户已完成订单列表获取成功", data);
    }


    /**
     * 获取某家店铺的所有进行中订单。
     *
     * @param storeId 店铺ID，通过路径变量传递。
     * @param authentication 当前请求的认证信息，用于权限验证。
     * @return 返回包含该店铺所有进行中订单的响应实体。如果店铺没有进行中订单，则返回空列表。
     *         如果用户未进行认证，返回401未授权状态。
     */
    @GetMapping("/store/{storeId}/in-progress")
    public ResponseEntity<Map<String, Object>> getInProgressOrders(@PathVariable Integer storeId, Authentication authentication) {
        // 验证用户是否认证
        if (authentication == null || !authentication.isAuthenticated()) {
            return createResponse(HttpStatus.UNAUTHORIZED, "用户未认证", null);
        }

        // 根据店铺ID查询进行中订单
        List<Order> inProgressOrders = orderService.lambdaQuery()
                .eq(Order::getStoreId, storeId)
                .eq(Order::getStatus, OrderStatus.IN_PROGRESS.toString())
                .list();

        // 将订单信息转换为包含用户信息、订单详情和菜单项信息的Map格式
        List<Map<String, Object>> orderDetails = inProgressOrders.stream().map(order -> {
            Map<String, Object> detail = new HashMap<>();
            detail.put("orderId", order.getOrderId());
            detail.put("userId", order.getUserId());
            detail.put("storeId", order.getStoreId());
            detail.put("status", order.getStatus());
            detail.put("totalPrice", order.getTotalPrice());
            detail.put("orderTime", order.getOrderTime());
            detail.put("notes", order.getNotes());
            detail.put("dineOption", order.getDineOption());
            detail.put("updateTime", order.getUpdateTime());

            // 获取用户信息
            User user = userService.getById(order.getUserId());
            if (user != null) {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("userId", user.getUserId());
                userInfo.put("name", user.getName());
                userInfo.put("phone", user.getPhone());
                userInfo.put("registrationDate", user.getRegistrationDate());
                userInfo.put("balance", user.getBalance());
                userInfo.put("role", user.getRole());
                detail.put("userInfo", userInfo);
            }

            // 获取订单详情信息
            List<OrderDetail> orderDetailsList = orderDetailService.lambdaQuery().eq(OrderDetail::getOrderId, order.getOrderId()).list();
            List<Map<String, Object>> orderDetailsInfo = orderDetailsList.stream().map(orderDetail -> {
                Map<String, Object> detailInfo = new HashMap<>();
                detailInfo.put("detailId", orderDetail.getDetailId());
                detailInfo.put("orderId", orderDetail.getOrderId());
                detailInfo.put("itemId", orderDetail.getItemId());
                detailInfo.put("quantity", orderDetail.getQuantity());
                detailInfo.put("price", orderDetail.getPrice());
                detailInfo.put("size", orderDetail.getSize());
                detailInfo.put("specialRequests", orderDetail.getSpecialRequests());

                // 获取菜单项信息
                MenuItem menuItem = menuItemService.getById(orderDetail.getItemId());
                if (menuItem != null) {
                    Map<String, Object> menuItemInfo = new HashMap<>();
                    menuItemInfo.put("itemId", menuItem.getItemId());
                    menuItemInfo.put("storeId", menuItem.getStoreId());
                    menuItemInfo.put("name", menuItem.getName());
                    menuItemInfo.put("description", menuItem.getDescription());
                    menuItemInfo.put("imageUrl", menuItem.getImageUrl());
                    menuItemInfo.put("category", menuItem.getCategory());
                    menuItemInfo.put("smallSizePrice", menuItem.getSmallSizePrice());
                    menuItemInfo.put("largeSizePrice", menuItem.getLargeSizePrice());
                    menuItemInfo.put("sizeStock", menuItem.getSizeStock());
                    detailInfo.put("menuItemInfo", menuItemInfo);
                }

                return detailInfo;
            }).collect(Collectors.toList());
            detail.put("orderDetails", orderDetailsInfo);

            return detail;
        }).collect(Collectors.toList());

        // 准备返回的数据结构
        Map<String, Object> data = new HashMap<>();
        data.put("orders", orderDetails);

        // 构造成功返回的响应实体
        return createResponse(HttpStatus.OK, "店铺进行中订单列表获取成功", data);
    }

}
