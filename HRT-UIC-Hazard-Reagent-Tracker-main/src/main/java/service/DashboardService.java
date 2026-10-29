package service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import model.Chemical;
import model.ChemicalRequest;
import model.RequestItem;
import model.Transaction;

/** Aggregates dashboard data from the models already loaded by the application. */
public class DashboardService {
    public Map<String, Integer> inventoryHealth(List<Chemical> list) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("OK", 0);
        counts.put("Low Stock", 0);
        counts.put("Expiring Soon", 0);
        counts.put("Expired", 0);
        if (list == null) return counts;
        for (Chemical chemical : list) {
            if (chemical == null) continue;
            String status = chemical.isExpired() ? "Expired" : chemical.isExpiringSoon() ? "Expiring Soon"
                    : chemical.isLowStock() ? "Low Stock" : "OK";
            counts.put(status, counts.get(status) + 1);
        }
        return counts;
    }

    public Map<String, Integer> hazardCounts(List<Chemical> list) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (list == null) return counts;
        for (Chemical chemical : list) {
            if (chemical == null || chemical.getHazardType() == null) continue;
            for (String hazard : chemical.getHazardType().split(",")) {
                hazard = hazard.trim();
                if (!hazard.isEmpty()) counts.put(hazard, counts.getOrDefault(hazard, 0) + 1);
            }
        }
        return counts;
    }

    /** Counts statuses; filter APPROVED requests by requestDate for the monthly-approval proxy. */
    public Map<String, Integer> requestStatusCounts(List<ChemicalRequest> list) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put(ChemicalRequest.PENDING, 0);
        counts.put(ChemicalRequest.APPROVED, 0);
        counts.put(ChemicalRequest.REJECTED, 0);
        counts.put(ChemicalRequest.CANCELLED, 0);
        if (list == null) return counts;
        for (ChemicalRequest request : list) {
            if (request != null && counts.containsKey(request.getStatus()))
                counts.put(request.getStatus(), counts.get(request.getStatus()) + 1);
        }
        return counts;
    }

    public Map<String, Double> topRequestedChemicals(List<ChemicalRequest> list, int limit) {
        Map<String, Double> totals = new LinkedHashMap<>();
        if (list != null) for (ChemicalRequest request : list) {
            if (request == null || request.getItems() == null) continue;
            Set<String> countedInRequest = new HashSet<>();
            for (RequestItem item : request.getItems()) {
                if (item != null && item.getChemicalName() != null && countedInRequest.add(item.getChemicalName()))
                    totals.put(item.getChemicalName(), totals.getOrDefault(item.getChemicalName(), 0.0) + 1.0);
            }
        }
        Map<String, Double> result = new LinkedHashMap<>();
        totals.entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(Math.max(0, limit)).forEach(entry -> result.put(entry.getKey(), entry.getValue()));
        return result;
    }

    public Map<String, Integer> requestsPerMonth(List<ChemicalRequest> list, int months) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (months <= 0) return counts;
        YearMonth current = YearMonth.now();
        for (int i = months - 1; i >= 0; i--) counts.put(current.minusMonths(i).toString(), 0);
        if (list == null) return counts;
        for (ChemicalRequest request : list) {
            if (request == null || request.getRequestDate() == null) continue;
            String month = YearMonth.from(request.getRequestDate()).toString();
            if (counts.containsKey(month)) counts.put(month, counts.get(month) + 1);
        }
        return counts;
    }

    public List<Chemical> lowestStock(List<Chemical> list, int limit) {
        if (list == null || limit <= 0) return new ArrayList<>();
        return list.stream().filter(chemical -> chemical != null)
                .sorted(Comparator.comparingDouble(chemical -> chemical.getMinimumStock() > 0
                        ? chemical.getQuantity() / chemical.getMinimumStock() : Double.POSITIVE_INFINITY))
                .limit(limit).collect(Collectors.toList());
    }

    public Map<String, double[]> movementByWeek(List<Transaction> list, int weeks) {
        Map<String, double[]> movement = new LinkedHashMap<>();
        if (weeks <= 0) return movement;
        LocalDate thisWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        for (int i = weeks - 1; i >= 0; i--) movement.put(thisWeek.minusWeeks(i).toString(), new double[2]);
        if (list == null) return movement;
        for (Transaction transaction : list) {
            if (transaction == null || transaction.getDate() == null) continue;
            LocalDate week = transaction.getDate().toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            double[] totals = movement.get(week.toString());
            if (totals != null && "ADD".equalsIgnoreCase(transaction.getType())) totals[0] += transaction.getQuantity();
            else if (totals != null && "RELEASE".equalsIgnoreCase(transaction.getType())) totals[1] += transaction.getQuantity();
        }
        return movement;
    }
}
