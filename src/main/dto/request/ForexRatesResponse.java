package main.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ForexRatesResponse extends java.util.ArrayList<ForexRatesResponse.ForexRate> {
    
    public static class ForexRate {
        @JsonProperty("date")
        private String date;
        
        @JsonProperty("base")
        private String base;
        
        @JsonProperty("quote")
        private String quote;
        
        @JsonProperty("rate")
        private double rate;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public String getBase() { return base; }
        public void setBase(String base) { this.base = base; }
        
        public String getQuote() { return quote; }
        public void setQuote(String quote) { this.quote = quote; }
        
        public double getRate() { return rate; }
        public void setRate(double rate) { this.rate = rate; }
    }
}

