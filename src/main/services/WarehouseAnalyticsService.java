package services;

import dto.response.TopBoughtStockResponse;
import dto.response.ActiveAccountAnalyticsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
public class WarehouseAnalyticsService {
    private static final String TOP_BOUGHT_STOCKS_SQL = """
        SELECT ticker, SUM(quantity) AS total_volume
        FROM dw.ribbit_trade_activity
        WHERE status = 'FILLED'
          AND side = 'BUY'
        GROUP BY ticker
        ORDER BY total_volume DESC
        LIMIT 20
        """;

    private final String warehouseUrl;
    private final String warehouseUsername;
    private final String warehousePassword;

    public WarehouseAnalyticsService(
        @Value("${warehouse.datasource.url}") String warehouseUrl,
        @Value("${warehouse.datasource.username}") String warehouseUsername,
        @Value("${warehouse.datasource.password}") String warehousePassword
    ) {
        this.warehouseUrl = warehouseUrl;
        this.warehouseUsername = warehouseUsername;
        this.warehousePassword = warehousePassword;
    }

    public List<TopBoughtStockResponse> getTopBoughtStocks() {
        List<TopBoughtStockResponse> stocks = new ArrayList<>();

        try (
            Connection connection = DriverManager.getConnection(
                warehouseUrl,
                warehouseUsername,
                warehousePassword
            );
            PreparedStatement statement = connection.prepareStatement(TOP_BOUGHT_STOCKS_SQL);
            ResultSet results = statement.executeQuery()
        ) {
            while (results.next()) {
                stocks.add(new TopBoughtStockResponse(
                    results.getString("ticker"),
                    results.getLong("total_volume")
                ));
            }
            return stocks;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load warehouse analytics", exception);
        }
    }

    public List<ActiveAccountAnalyticsResponse> getMostActiveAccounts() {
        String sql = """
            SELECT account_id,
                   COUNT(*) AS order_count,
                   COALESCE(SUM(quantity) FILTER (WHERE status = 'FILLED'), 0) AS filled_quantity,
                   COALESCE(SUM(total_price) FILTER (WHERE status = 'FILLED'), 0) AS total_traded_value,
                   COUNT(*) FILTER (WHERE side = 'BUY') AS buy_count,
                   COUNT(*) FILTER (WHERE side = 'SELL') AS sell_count
            FROM dw.ribbit_trade_activity
            GROUP BY account_id
            ORDER BY total_traded_value DESC, filled_quantity DESC, account_id
            """;
        List<ActiveAccountAnalyticsResponse> accounts = new ArrayList<>();

        try (
            Connection connection = DriverManager.getConnection(warehouseUrl, warehouseUsername, warehousePassword);
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet results = statement.executeQuery()
        ) {
            while (results.next()) {
                accounts.add(new ActiveAccountAnalyticsResponse(
                    results.getInt("account_id"),
                    results.getLong("order_count"),
                    results.getLong("filled_quantity"),
                    results.getBigDecimal("total_traded_value"),
                    results.getLong("buy_count"),
                    results.getLong("sell_count")
                ));
            }
            return accounts;
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to load active account analytics", exception);
        }
    }
}
