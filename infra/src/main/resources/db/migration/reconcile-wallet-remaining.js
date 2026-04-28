// One-shot Mongo shell migration for the wallet-debit rollout.
// Recomputes walletDocument.remainingAmount as budgetAmount - sum(active bullet budgets).

const wallets = db.walletDocument.find();

wallets.forEach(wallet => {
  const allocated = db.bulletDocument.aggregate([
    { $match: { walletId: wallet._id } },
    { $group: { _id: "$walletId", total: { $sum: "$budget" } } }
  ]).toArray()[0]?.total || NumberDecimal("0.00");

  db.walletDocument.updateOne(
    { _id: wallet._id },
    [
      {
        $set: {
          remainingAmount: {
            $subtract: [
              { $toDecimal: "$budgetAmount" },
              { $toDecimal: allocated }
            ]
          },
          remainingCurrency: {
            $ifNull: ["$remainingCurrency", { $ifNull: ["$budgetCurrency", "BRL"] }]
          }
        }
      }
    ]
  );
});
