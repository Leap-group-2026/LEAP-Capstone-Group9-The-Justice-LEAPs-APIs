package main.repos;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import main.entities.CurrentPriceEntity;
import java.math.BigDecimal;
import java.util.Optional;

// Written only by the price refresher. Everything else reads prices via InstrumentRepo's join.
@Mapper
public interface CurrentPriceRepo {
    @Select("SELECT instrument_id, price, quote_time, retrieved_at FROM current_prices WHERE instrument_id = #{instrumentId}")
    Optional<CurrentPriceEntity> findByInstrumentId(Integer instrumentId);

    @Select("SELECT cp.price " +
            "FROM current_prices cp " +
            "INNER JOIN instruments i ON cp.instrument_id = i.instrument_id " +
            "WHERE i.ticker = #{ticker}")
    Optional<BigDecimal> findPriceByTicker(String ticker);

    @Insert("INSERT INTO current_prices (instrument_id, price, quote_time, retrieved_at) " +
            "VALUES (#{instrumentId}, #{price}, #{quoteTime}, #{retrievedAt}) " +
            "ON CONFLICT (instrument_id) DO UPDATE SET " +
            "price = EXCLUDED.price, " +
            "quote_time = EXCLUDED.quote_time, " +
            "retrieved_at = EXCLUDED.retrieved_at")
    void upsert(CurrentPriceEntity currentPrice);
}
