package br.com.casellisoftware.budgetmanager.mappers.expense;

import br.com.casellisoftware.budgetmanager.application.mappers.ExpenseApplicationMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ExpenseApplicationMapperAdapter extends ExpenseApplicationMapper {
}
