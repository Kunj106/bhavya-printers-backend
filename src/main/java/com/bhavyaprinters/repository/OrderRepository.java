package com.bhavyaprinters.repository;

import com.bhavyaprinters.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByOrderByCreatedAtDesc();
    List<Order> findByBankIdOrderByCreatedAtDesc(Long bankId);
    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);

    @Query(value = """
        SELECT
            MONTH(created_at)  AS month,
            YEAR(created_at)   AS year,
            SUM(total)         AS total_revenue,
            COUNT(*)           AS order_count,
            SUM(gst_amount)    AS gst_collected
        FROM orders
        GROUP BY YEAR(created_at), MONTH(created_at)
        ORDER BY YEAR(created_at) DESC, MONTH(created_at) DESC
        LIMIT 24
        """, nativeQuery = true)
    List<Object[]> findMonthlyRevenue();

    @Query(value = """
        SELECT
            bank_id,
            bank_name,
            branch_name,
            COUNT(*)   AS order_count,
            SUM(total) AS total_spend
        FROM orders
        WHERE bank_id IS NOT NULL
        GROUP BY bank_id, bank_name, branch_name
        ORDER BY total_spend DESC
        LIMIT 10
        """, nativeQuery = true)
    List<Object[]> findTopBanks();

    @Query(value = """
        SELECT
            MONTH(created_at)                                                         AS month,
            YEAR(created_at)                                                          AS year,
            SUM(subtotal)                                                             AS taxable_amount,
            SUM(CASE WHEN gst_rate = 12 THEN gst_amount ELSE 0 END)                  AS gst12_amount,
            SUM(CASE WHEN gst_rate = 18 THEN gst_amount ELSE 0 END)                  AS gst18_amount,
            SUM(gst_amount)                                                           AS total_gst,
            COUNT(*)                                                                  AS order_count
        FROM orders
        GROUP BY YEAR(created_at), MONTH(created_at)
        ORDER BY YEAR(created_at) DESC, MONTH(created_at) DESC
        LIMIT 24
        """, nativeQuery = true)
    List<Object[]> findMonthlyGst();

    @Query(value = """
        SELECT
            COUNT(*)                                                                               AS total_orders,
            COALESCE(SUM(total), 0)                                                                AS total_revenue,
            SUM(CASE WHEN status = 'Delivered' THEN 1 ELSE 0 END)                                 AS delivered_orders,
            COALESCE(SUM(CASE WHEN MONTH(created_at) = MONTH(NOW())
                               AND YEAR(created_at)  = YEAR(NOW())
                              THEN total ELSE 0 END), 0)                                           AS this_month_revenue,
            SUM(CASE WHEN status = 'Pending' THEN 1 ELSE 0 END)                                   AS pending_orders,
            SUM(CASE WHEN MONTH(created_at) = MONTH(NOW())
                      AND YEAR(created_at)  = YEAR(NOW())
                     THEN 1 ELSE 0 END)                                                            AS this_month_orders
        FROM orders
        """, nativeQuery = true)
    List<Object[]> findDashboardOrderStats();

}
