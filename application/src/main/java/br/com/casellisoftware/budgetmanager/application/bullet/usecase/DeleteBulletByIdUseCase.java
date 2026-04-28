package br.com.casellisoftware.budgetmanager.application.bullet.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.DeleteBulletByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletInUseException;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteBulletByIdUseCase implements DeleteBulletByIdBoundary {

    private static final Logger log = LoggerFactory.getLogger(DeleteBulletByIdUseCase.class);

    private final BulletRepository bulletRepository;
    private final WalletRepository walletRepository;
    private final PaymentRepository paymentRepository;
    private final FindWalletDomainByIdBoundary findWalletDomainByIdBoundary;

    public DeleteBulletByIdUseCase(BulletRepository bulletRepository,
                                   WalletRepository walletRepository,
                                   PaymentRepository paymentRepository,
                                   FindWalletDomainByIdBoundary findWalletDomainByIdBoundary) {
        this.bulletRepository = bulletRepository;
        this.walletRepository = walletRepository;
        this.paymentRepository = paymentRepository;
        this.findWalletDomainByIdBoundary = findWalletDomainByIdBoundary;
    }

    @Override
    public void execute(String id) {
        log.info("Deleting bullet id={}", id);

        Bullet bullet = bulletRepository.findById(id)
                .orElseThrow(() -> new BulletNotFoundException(id));

        if (paymentRepository.existsByBulletId(id)) {
            throw new BulletInUseException(id);
        }

        Wallet wallet = findWalletDomainByIdBoundary.findById(bullet.getWalletId());
        Wallet refunded = wallet.credit(bullet.getRemaining());

        walletRepository.save(refunded);
        bulletRepository.deleteById(id);

        log.info("Bullet deleted id={} refundedToWallet={}", id, bullet.getRemaining().amount());
    }
}
