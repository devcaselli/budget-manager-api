package br.com.casellisoftware.budgetmanager.rest.subscription.mappers;

import br.com.casellisoftware.budgetmanager.application.subscription.boundary.PatchSubscriptionInput;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionInput;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionOutput;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionVersionOutput;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.rest.subscription.dtos.PagedSubscriptionResponseDto;
import br.com.casellisoftware.budgetmanager.rest.subscription.dtos.SubscriptionPatchRequestDto;
import br.com.casellisoftware.budgetmanager.rest.subscription.dtos.SubscriptionRequestDto;
import br.com.casellisoftware.budgetmanager.rest.subscription.dtos.SubscriptionResponseDto;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionRestMapperTest {

    private final SubscriptionRestMapper mapper = Mappers.getMapper(SubscriptionRestMapper.class);

    @Test
    void toInput_copiesAllFields() {
        SubscriptionRequestDto request = new SubscriptionRequestDto("Netflix", new BigDecimal("55.90"), "BRL", "cc-1");

        SubscriptionInput input = mapper.toInput(request);

        assertThat(input).isEqualTo(new SubscriptionInput("Netflix", new BigDecimal("55.90"), "BRL", null, null, br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum.NONE, "cc-1"));
    }

    @Test
    void toPatchInput_copiesIdAndPatchFields() {
        SubscriptionPatchRequestDto request = new SubscriptionPatchRequestDto("Netflix Premium", new BigDecimal("60.00"), "cc-2");

        PatchSubscriptionInput input = mapper.toPatchInput("subscription-1", request);

        assertThat(input).isEqualTo(new PatchSubscriptionInput("subscription-1", "Netflix Premium", new BigDecimal("60.00"), "cc-2"));
    }

    @Test
    void toResponse_copiesAllFieldsAndVersions() {
        SubscriptionOutput output = output("subscription-1", "Netflix");

        SubscriptionResponseDto response = mapper.toResponse(output);

        assertThat(response.id()).isEqualTo("subscription-1");
        assertThat(response.description()).isEqualTo("Netflix");
        assertThat(response.currency()).isEqualTo("BRL");
        assertThat(response.startMonth()).isEqualTo(YearMonth.of(2026, 5));
        assertThat(response.endMonth()).isNull();
        assertThat(response.versions()).hasSize(1);
        assertThat(response.versions().getFirst().effectiveMonth()).isEqualTo(YearMonth.of(2026, 5));
        assertThat(response.versions().getFirst().amount()).isEqualByComparingTo("55.90");
    }

    @Test
    void toPagedResponse_fromPageResult_mapsPaginationAndContent() {
        PageResult<SubscriptionOutput> page = new PageResult<>(
                List.of(output("subscription-1", "Netflix"), output("subscription-2", "Spotify")),
                0,
                20,
                2,
                1
        );

        PagedSubscriptionResponseDto response = mapper.toPagedResponse(page);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().getFirst().id()).isEqualTo("subscription-1");
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(1);
    }

    @Test
    void toPagedResponse_fromList_mapsSingleSyntheticPage() {
        PagedSubscriptionResponseDto response = mapper.toPagedResponse(List.of(output("subscription-1", "Netflix")));

        assertThat(response.content()).hasSize(1);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(1);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
    }

    private static SubscriptionOutput output(String id, String description) {
        return new SubscriptionOutput(
                id,
                description,
                "BRL",
                YearMonth.of(2026, 5),
                null,
                List.of(new SubscriptionVersionOutput(YearMonth.of(2026, 5), new BigDecimal("55.90")))
        );
    }
}
