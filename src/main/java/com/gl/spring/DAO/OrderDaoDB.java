package com.gl.spring.DAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gl.spring.entity.Color;
import com.gl.spring.entity.Customer;
import com.gl.spring.entity.Order;
import com.gl.spring.entity.OrderItem;
import com.gl.spring.entity.Product;




@Component
public class OrderDaoDB implements OrderDAO {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	@Autowired
	private JdbcTemplate jdbcTemplate;

//	@Autowired
//	private Product product;

	final String SELECT_ORDER_BY_ID = "SELECT orders.id, member_id, created_date, created_time, status, "
			+ "		payment_type, payment_fee, payment_note, " + "		shipping_type, shipping_fee, shipping_note, "
			+ "        recipient_name, recipient_email, recipient_phone, shipping_address,"
			+ "        orders_items.product_id, products.name as product_name,products.photo_url,"
			+ "        price,quantity,(price*quantity) as amount"
			+ "	FROM orders LEFT JOIN orders_items ON orders.id = orders_items.orders_id  "
			+ "		 LEFT JOIN products ON orders_items.product_id = products.id" + "	WHERE orders.id=?";
	final String INSERT_ORDER = "INSERT INTO orders" + "(id, member_id, created_date, created_time, status,"
			+ "payment_type, payment_fee, shipping_type, shipping_fee,"
			+ "recipient_name, recipient_email, recipient_phone, shipping_address)"
			+ "VALUE(0,?,?,?,0, ?,?,?,?,?,?,?,?)";
	final String INSERT_ORDER_ITEM = "INSERT INTO orders_items"
			+ "(orders_id, product_id, brand, color_name, size, quantity, price)" + "VALUE(?,?,?,?,?,?,?)";
	final String UPDATE_PRODUCT_STOCK = "UPDATE products SET stock=stock-? " + "WHERE id=? AND stock>=?";

	@Override
	@Transactional
	public void insertOrder(Order order) {
		if (order == null)
			throw new IllegalArgumentException("建立訂單不得為null!");			
			for (OrderItem item : order.getOrderItemSet()) {
				int row = 0;
				if (item.getColor() == null) {
					jdbcTemplate.update(new PreparedStatementCreator() {
						public PreparedStatement createPreparedStatement(Connection conn) throws SQLException {
							PreparedStatement pstmtUpdateProducts = conn.prepareStatement(UPDATE_PRODUCT_STOCK);
							pstmtUpdateProducts.setInt(1, item.getQuantity());
							pstmtUpdateProducts.setInt(2, item.getProduct().getId());
							pstmtUpdateProducts.setInt(3, item.getQuantity());

							return pstmtUpdateProducts;
						}
					});
				}
				if (row == 0) {
//				throw new StockShortageException("產品庫存不足",item);
				}
			}

//--------------------------------------------------------------------------------------------------------------------------
			KeyHolder keyHolder = new GeneratedKeyHolder();
			jdbcTemplate.update(new PreparedStatementCreator() {				
				public PreparedStatement createPreparedStatement(Connection conn) throws SQLException {
					PreparedStatement pstmt1 = conn.prepareStatement(INSERT_ORDER, Statement.RETURN_GENERATED_KEYS);
					pstmt1.setString(1, order.getMember().getId());
					pstmt1.setString(2, order.getCreatedDate().toString());
					pstmt1.setString(3, order.getCreatedTime().toString());

					pstmt1.setString(4, order.getPaymentType().name());
					pstmt1.setDouble(5, order.getPaymentType().getFreight());
					pstmt1.setString(6, order.getShippingType().name());
					pstmt1.setDouble(7, order.getShippingType().getFreight());

					pstmt1.setString(8, order.getRecipientName());
					pstmt1.setString(9, order.getRecipientEmail());
					pstmt1.setString(10, order.getRecipientPhone());
					pstmt1.setString(11, order.getShippingAddress());

					return pstmt1;
				}
			}, keyHolder);
//		return keyHolder.getKey().intValue();
// --------------------------------------------------------------------------------------------------------------------------
			for (OrderItem item : order.getOrderItemSet()) {
				Product p = item.getProduct();
				Color color = item.getColor();

				jdbcTemplate.update(new PreparedStatementCreator() {					
					public PreparedStatement createPreparedStatement(Connection conn) throws SQLException {
						PreparedStatement pstmt2 = conn.prepareStatement(INSERT_ORDER_ITEM);

						pstmt2.setInt(1, order.getId());
						pstmt2.setInt(2, p.getId());
						pstmt2.setString(3, item.getBrand() != null ? item.getBrand() : "");
						pstmt2.setString(4, color != null ? color.getColorName() : "");
						pstmt2.setString(5, item.getSize());
						pstmt2.setInt(6, item.getQuantity());
						pstmt2.setDouble(7, item.getPrice());

						return pstmt2;
					}
				});
			}		
	}

	private RowMapper<Order> OrderMapper = new RowMapper<Order>() {
		@Override
		public Order mapRow(ResultSet rs, int rowNum) throws SQLException {

			Order o = new Order();
			o.setId(rs.getInt("id"));

			Customer member = new Customer();
			member.setId(rs.getString("member_id"));
			o.setMember(member);

			o.setCreatedDate(rs.getString("created_date"));
			o.setCreatedTime(rs.getString("created_time"));
			o.setStatus(rs.getInt("status"));

			o.setPaymentType(rs.getString("payment_type"));
			o.setPaymentFee(rs.getDouble("payment_fee"));
			o.setPaymentNote(rs.getString("payment_note"));
			o.setShippingType(rs.getString("shipping_type"));
			o.setShippingFee(rs.getDouble("shipping_fee"));
			o.setShippingNote(rs.getString("shipping_note"));

			o.setRecipientName(rs.getString("recipient_name"));
			o.setRecipientEmail(rs.getString("recipient_email"));
			o.setRecipientPhone(rs.getString("recipient_phone"));
			o.setShippingAddress(rs.getString("shipping_address"));

			OrderItem item = new OrderItem();
			Product p = new Product();
			p.setId(rs.getInt("product_id"));
			p.setName(rs.getString("product_name"));
			p.setPhotoUrl(rs.getString("photo_url"));
			item.setProduct(p);
			item.setPrice(rs.getDouble("price"));
			item.setQuantity(rs.getInt("quantity"));

			o.addOrderItem(item);
			return o;
		}

	};

	@Override
	public Optional<Order> selectOrderById(String orderId) {
		List<Order> list = jdbcTemplate.query(SELECT_ORDER_BY_ID, OrderMapper, orderId);
		if (list.isEmpty()) {
			return Optional.empty();
		} else {
			return Optional.of(list.get(0));
		}
	}

}
