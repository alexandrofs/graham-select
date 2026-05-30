package afsdigital.grahamselect.common.portfolio.application.usecase;

import afsdigital.grahamselect.common.portfolio.application.dto.MonthlyEvolutionDTO;
import afsdigital.grahamselect.common.portfolio.application.dto.PortfolioEvolutionDTO;
import afsdigital.grahamselect.common.portfolio.application.repository.TradePort;
import afsdigital.grahamselect.common.portfolio.domain.entities.Trade;
import afsdigital.grahamselect.common.portfolio.domain.entities.TradeSide;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class GetPortfolioEvolutionUseCase {

    private final TradePort tradePort;

    public PortfolioEvolutionDTO execute(String userId) {
        List<Trade> trades = tradePort.findAllByUserId(userId);

        // Define o intervalo dos últimos 12 meses consecutivos
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(11).withDayOfMonth(1);

        // Preenche o mapa com todos os 12 meses zerados (garante a ordem e continuidade)
        Map<String, BigDecimal> contributionsMap = new HashMap<>();
        Map<String, BigDecimal> dividendsMap = new HashMap<>();
        List<String> monthsInOrder = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        LocalDate currentMonth = startDate;
        while (!currentMonth.isAfter(endDate)) {
            String monthKey = currentMonth.format(formatter);
            contributionsMap.put(monthKey, BigDecimal.ZERO);
            dividendsMap.put(monthKey, BigDecimal.ZERO);
            monthsInOrder.add(monthKey);
            currentMonth = currentMonth.plusMonths(1);
        }

        // Consolida as operações existentes
        for (Trade trade : trades) {
            LocalDate tradeDate = trade.getTradeDate();
            if (tradeDate == null || tradeDate.isBefore(startDate) || tradeDate.isAfter(endDate)) {
                continue; // Fora do intervalo dos últimos 12 meses
            }

            String monthKey = tradeDate.format(formatter);
            BigDecimal value = trade.getQuantity().multiply(trade.getPrice());

            if (TradeSide.COMPRA.name().equals(trade.getSide())) {
                contributionsMap.merge(monthKey, value, BigDecimal::add);
            } else if (TradeSide.DIVIDENDO.name().equals(trade.getSide())) {
                dividendsMap.merge(monthKey, value, BigDecimal::add);
            }
        }

        // Converte para a lista de DTOs na ordem correta
        List<MonthlyEvolutionDTO> monthlyData = new ArrayList<>();
        for (String month : monthsInOrder) {
            monthlyData.add(new MonthlyEvolutionDTO(
                    month,
                    contributionsMap.get(month),
                    dividendsMap.get(month)
            ));
        }

        return new PortfolioEvolutionDTO(monthlyData);
    }
}
