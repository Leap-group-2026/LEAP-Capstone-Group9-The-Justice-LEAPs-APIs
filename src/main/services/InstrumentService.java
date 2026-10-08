package services;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import com.github.pagehelper.PageHelper;

import repos.InstrumentRepo;
import dto.InstrumentWithPrice;
import entities.InstrumentEntity;
import exception.ResourceNotFoundException;
import java.util.List;

@Service
public class InstrumentService {
    private InstrumentRepo repo;
    public InstrumentService(InstrumentRepo repo){
        this.repo = repo;
    }

    public InstrumentEntity saveInstrument(InstrumentEntity entity) {
        if (repo.existsByTicker(entity.getTicker())) {
            throw new IllegalArgumentException("Ticker already exists");
        }
        try {
            repo.insert(entity);
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("Ticker already exists");
        }
        return entity;
    }

    public InstrumentWithPrice getTickerPrice(String ticker) {
        return repo.findByTicker(ticker).orElseThrow(() -> new ResourceNotFoundException("Instrument", ticker));
    }

    public List<InstrumentWithPrice> getAllInstruments(@RequestParam(defaultValue = "1") int pageNum, @RequestParam(defaultValue = "10") int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        return repo.findAll();
    }
}
