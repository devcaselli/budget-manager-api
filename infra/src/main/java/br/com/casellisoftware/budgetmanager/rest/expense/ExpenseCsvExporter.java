package br.com.casellisoftware.budgetmanager.rest.expense;

import br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseResponseDto;

import java.math.BigDecimal;
import java.util.List;

final class ExpenseCsvExporter {

    private static final String HEADER = "id,name,cost,purchaseDate,remaining,walletId,bulletId,creditCardId,installment,installmentNumber";

    private ExpenseCsvExporter() {
    }

    static String export(List<ExpenseResponseDto> expenses) {
        StringBuilder csv = new StringBuilder(HEADER).append('\n');
        expenses.forEach(expense -> csv
                .append(field(expense.id())).append(',')
                .append(field(expense.name())).append(',')
                .append(field(expense.cost())).append(',')
                .append(field(expense.purchaseDate())).append(',')
                .append(field(expense.remaining())).append(',')
                .append(field(expense.walletId())).append(',')
                .append(field(expense.bulletId())).append(',')
                .append(field(expense.creditCardId())).append(',')
                .append(expense.installment()).append(',')
                .append(field(expense.installmentNumber()))
                .append('\n'));
        return csv.toString();
    }

    private static String field(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }

        String text = value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
