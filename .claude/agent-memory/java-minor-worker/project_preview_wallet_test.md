---
name: Preview Wallet Subscription Charges Test
description: Test coverage for PREVIEW wallet returning both PRODUCTION + PREVIEW subscriptions (applied 2026-05-07)
type: project
---

**Test Added**: `FindSubscriptionChargesByWalletIdUseCaseTest.execute_whenWalletIsPreview_returnsProductionAndPreviewSubscriptionCharges`

This test validates that when a wallet is in PREVIEW state, the use case returns both PRODUCTION and PREVIEW subscriptions as charges. The test:
1. Builds a Wallet in PREVIEW state (WalletState.PREVIEW)
2. Mocks SubscriptionRepository.findActiveFor(month, SubscriptionState.PREVIEW) to return one PRODUCTION + one PREVIEW subscription
3. Calls useCase.execute(walletId)
4. Asserts both charges appear in the output with correct amounts (80.00 and 20.00)

**Complementary Test**: `execute_whenWalletIsProduction_returnsOnlyProductionSubscriptionCharges`

Negative coverage test that verifies PRODUCTION wallets do NOT receive PREVIEW subscriptions. Mocks repository with only PRODUCTION subs and confirms output has exactly 1 charge.

**Semantics Verified Against**: SubscriptionRepositoryImplTest.findActiveFor_withState_keepsPreviewOutOfProductionAndIncludesItForPreviewWallets confirms that:
- `findActiveFor(month, SubscriptionState.PRODUCTION)` returns only PRODUCTION subs
- `findActiveFor(month, SubscriptionState.PREVIEW)` returns BOTH PRODUCTION and PREVIEW subs

This aligns with WalletSubscriptionSelector.activeForWallet() which delegates to repository based on wallet state.

**Why:** Code review requirement (bullet 2 of Missing Tests) to verify preview-state wallet behavior.
**How to apply:** These tests now cover the PREVIEW wallet path. Future changes to WalletSubscriptionSelector or repository behavior must keep these tests passing.
