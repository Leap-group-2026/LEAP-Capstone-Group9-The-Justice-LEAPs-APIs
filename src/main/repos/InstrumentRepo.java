package main.repos;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Options;
import main.entities.InstrumentEntity;
import java.util.Optional;
import java.util.List;

@Mapper
public interface InstrumentRepo {
    @Select("SELECT * FROM instruments WHERE instrument_id = #{instrumentId}")
    Optional<InstrumentEntity> findById(Integer instrumentId);

    @Select("SELECT * FROM instruments")
    List<InstrumentEntity> findAll();

    @Insert("INSERT INTO instruments (ticker, asset_type, asset_name, price, currency) " +
            "VALUES (#{ticker}, #{assetType}, #{assetName}, #{price}, #{currency})")
    @Options(useGeneratedKeys = true, keyProperty = "instrumentId")
    void insert(InstrumentEntity instrument);

    @Update("UPDATE instruments SET ticker=#{ticker}, asset_type=#{assetType}, asset_name=#{assetName}, price=#{price}, currency=#{currency} WHERE instrument_id=#{instrumentId}")
    void update(InstrumentEntity instrument);

    @Delete("DELETE FROM instruments WHERE instrument_id = #{instrumentId}")
    void delete(Integer instrumentId);
}