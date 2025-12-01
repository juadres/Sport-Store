package com.ecommerce.tiendaspring.services;

import com.ecommerce.tiendaspring.models.Usuario;
import com.ecommerce.tiendaspring.models.Venta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class ExternalAPIService {
    
    private final String WEATHER_API_KEY = "d6523603474772a8863a62048827d6d2";
    
    public String getWeatherRecommendation() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openweathermap.org/data/2.5/weather?q=Bogota,co&appid=" + WEATHER_API_KEY + "&units=metric&lang=es"))
                .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200 && response.body().contains("\"temp\"")) {
                String body = response.body();
                int tempStart = body.indexOf("\"temp\":") + 7;
                int tempEnd = body.indexOf(",", tempStart);
                String tempStr = body.substring(tempStart, tempEnd);
                
                double temperature = Double.parseDouble(tempStr);
                
                if (temperature > 25) {
                    return "🌞 ¡Hace calor! Perfecto para deportes acuáticos y ropa ligera";
                } else if (temperature > 18) {
                    return "🌤️ ¡Clima ideal! Excelente para running y deportes al aire libre";
                } else {
                    return "⛅ ¡Buen día para deportes! Nuestras sudaderas te mantendrán cómodo";
                }
            }
        } catch (Exception e) {
            System.out.println("Error clima: " + e.getMessage());
        }
        
        return "🏃‍♂️ ¡Día perfecto para hacer deporte! Encuentra tu equipamiento ideal";
    }
    
    public Map<String, String> getCurrencyConversions(BigDecimal copAmount) {
        Map<String, String> conversions = new HashMap<>();
        
        try {
            BigDecimal usdRate = new BigDecimal("0.00025");
            BigDecimal eurRate = new BigDecimal("0.00022");
            
            conversions.put("USD", "$" + copAmount.multiply(usdRate).setScale(2, java.math.RoundingMode.HALF_UP));
            conversions.put("EUR", "€" + copAmount.multiply(eurRate).setScale(2, java.math.RoundingMode.HALF_UP));
            conversions.put("COP", "$" + copAmount.setScale(0, java.math.RoundingMode.HALF_UP) + " COP");
            
        } catch (Exception e) {
            conversions.put("USD", "$" + copAmount.divide(new BigDecimal("4000"), 2, java.math.RoundingMode.HALF_UP));
            conversions.put("EUR", "€" + copAmount.divide(new BigDecimal("4500"), 2, java.math.RoundingMode.HALF_UP));
            conversions.put("COP", "$" + copAmount.setScale(0, java.math.RoundingMode.HALF_UP) + " COP");
        }
        
        return conversions;
    }
}