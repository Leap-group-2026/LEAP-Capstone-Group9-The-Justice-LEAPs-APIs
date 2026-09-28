package main.repos;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Options;
import main.entities.InstrumentEntity;
import main.dto.InstrumentWithPrice;
import java.util.Optional;
import java.util.List;

@Mapper
public interface InstrumentRepo {
    @Select("SELECT i.instrument_id, i.ticker, i.asset_type, i.asset_name, i.currency, " +
            "cp.price, cp.quote_time " +
            "FROM instruments i " +
            "LEFT JOIN current_prices cp USING (instrument_id) " +
            "WHERE i.instrument_id = #{instrumentId}")
    Optional<InstrumentWithPrice> findById(Integer instrumentId);

    @Select("SELECT i.instrument_id, i.ticker, i.asset_type, i.asset_name, i.currency, " +
            "cp.price, cp.quote_time " +
            "FROM instruments i " +
            "LEFT JOIN current_prices cp USING (instrument_id)")
    List<InstrumentWithPrice> findAll();

    // Reference data only, for nested @One mappings whose target property is an InstrumentEntity
    @Select("SELECT instrument_id, ticker, asset_type, asset_name, currency FROM instruments WHERE instrument_id = #{instrumentId}")
    InstrumentEntity findEntityById(Integer instrumentId);

    @Insert("INSERT INTO instruments (ticker, asset_type, asset_name, currency) " +
            "VALUES (#{ticker}, #{assetType}, #{assetName}, #{currency})")
    @Options(useGeneratedKeys = true, keyProperty = "instrumentId")
    void insert(InstrumentEntity instrument);

    @Update("UPDATE instruments SET ticker=#{ticker}, asset_type=#{assetType}, asset_name=#{assetName}, currency=#{currency} WHERE instrument_id=#{instrumentId}")
    void update(InstrumentEntity instrument);

    @Delete("DELETE FROM instruments WHERE instrument_id = #{instrumentId}")
    void delete(Integer instrumentId);

    @Select("SELECT EXISTS(SELECT 1 FROM instruments WHERE ticker = #{ticker})")
    boolean existsByTicker(String ticker);
}