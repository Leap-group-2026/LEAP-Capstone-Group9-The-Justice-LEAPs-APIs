package main.config;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import main.entities.PortfolioSize;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PortfolioSizeTypeHandler extends BaseTypeHandler<PortfolioSize> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, PortfolioSize parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.getValue());
    }

    @Override
    public PortfolioSize getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        if (value == null) {
            return null;
        }
        return PortfolioSize.fromValue(value);
    }

    @Override
    public PortfolioSize getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        if (value == null) {
            return null;
        }
        return PortfolioSize.fromValue(value);
    }

    @Override
    public PortfolioSize getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        if (value == null) {
            return null;
        }
        return PortfolioSize.fromValue(value);
    }
}
